# 部署架构（Docker Compose 单机）

## 组件与职责

```
┌──────────────────────────────────────────────────┐
│                 宿主机 (单机)                      │
│                                                  │
│  ┌────────────┐                                  │
│  │   Nginx     │  :80/:443                        │
│  │  反向代理    │  TLS / 路由分发 / SSE 透传        │
│  └─────┬──────┘                                  │
│     ┌──┴──┐                                      │
│     ▼     ▼                                      │
│  /api/*   /*.html/js/css                          │
│     │     │                                      │
│     ▼     ▼                                      │
│  ┌──────────┐  ┌───────────┐                     │
│  │ciff-server│  │ ciff-web  │                     │
│  │ :8080     │  │ Nginx托管  │                     │
│  └─┬──┬──┬──┘  └───────────┘                     │
│    │  │  │                                      │
│    ▼  ▼  ▼                                      │
│  MySQL  Redis  PGVector                          │
│  :3306  :6379  :5432                             │
└──────────────────────────────────────────────────┘
         │
         ▼
   外部 LLM API (OpenAI / Claude / Gemini / Ollama)
```

| 组件 | 职责 |
|------|------|
| Nginx | TLS 终止、`/api/*` 路由到 ciff-server、其余路由到前端静态资源、SSE 长连接透传（proxy_buffering off） |
| ciff-web (Nginx) | 托管 Vue 构建产物，gzip 压缩，静态资源缓存 |
| ciff-server | Spring Boot 应用，所有业务逻辑，SSE 流式对话 |
| MySQL | Agent 配置、对话记录、用户、工具、工作流等关系数据 |
| Redis | 缓存（Agent/模型配置）、JWT 会话、Redis Stream（文档处理任务队列） |
| PostgreSQL + pgvector | 知识库向量存储与相似度检索 |

## docker-compose.yml

```yaml
version: "3.8"

services:
  nginx:
    image: nginx:1.25-alpine
    ports: ["80:80", "443:443"]
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      ciff-server: { condition: service_healthy }
      ciff-web: { condition: service_healthy }
    restart: always

  ciff-web:
    image: ciff-web:latest
    build: { context: ../ciff-web, dockerfile: Dockerfile }
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:80"]
      interval: 10s, retries: 3
    restart: always

  ciff-server:
    image: ciff-server:latest
    build: { context: ../ciff-server, dockerfile: Dockerfile }
    environment:
      JAVA_OPTS: "-Xms512m -Xmx512m"
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/ciff?useSSL=false&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      PGVECTOR_HOST: pgvector
      PGVECTOR_PORT: 5432
      PGVECTOR_USERNAME: ${PG_USERNAME}
      PGVECTOR_PASSWORD: ${PG_PASSWORD}
    depends_on:
      mysql: { condition: service_healthy }
      redis: { condition: service_healthy }
      pgvector: { condition: service_healthy }
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health/liveness"]
      interval: 10s, retries: 5, start_period: 30s
    restart: always

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: ciff
      MYSQL_USER: ${DB_USERNAME}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
      - ./mysql/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
      - ./mysql/my.cnf:/etc/mysql/conf.d/my.cnf:ro
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s, retries: 5, start_period: 30s
    restart: always

  redis:
    image: redis:7-alpine
    command: redis-server /usr/local/etc/redis/redis.conf
    volumes:
      - ./redis/redis.conf:/usr/local/etc/redis/redis.conf:ro
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s, retries: 3
    restart: always

  pgvector:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: ciff_vector
      POSTGRES_USER: ${PG_USERNAME}
      POSTGRES_PASSWORD: ${PG_PASSWORD}
    volumes:
      - pgvector-data:/var/lib/postgresql/data
      - ./pgvector/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${PG_USERNAME}"]
      interval: 10s, retries: 5, start_period: 20s
    restart: always

volumes:
  mysql-data:
  redis-data:
  pgvector-data:
```

## Nginx 关键配置

```nginx
# SSE streaming - critical
location /sse/ {
    proxy_pass http://ciff_server;
    proxy_http_version 1.1;
    proxy_set_header Connection '';
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 200s;
    chunked_transfer_encoding on;
}
```

## deploy 目录结构

```
deploy/
├── docker-compose.yml
├── .env                          # git ignored, credentials only
├── nginx/
│   ├── nginx.conf
│   └── ssl/                      # TLS certs (git ignored)
├── mysql/
│   ├── my.cnf                    # max_connections=50, innodb_buffer_pool_size=256M
│   └── init.sql                  # DDL init script
├── pgvector/
│   └── init.sql                  # CREATE EXTENSION vector;
└── redis/
    └── redis.conf                # maxmemory 128mb, maxmemory-policy allkeys-lru
```

## 备份策略

```bash
# cron daily
BACKUP_DIR=/data/ciff-backup/$(date +%Y%m%d)
docker exec ciff-mysql-1 mysqldump -u root -p${DB_ROOT_PASSWORD} ciff > $BACKUP_DIR/ciff.sql
docker exec ciff-pgvector-1 pg_dump -U ${PG_USERNAME} ciff_vector > $BACKUP_DIR/ciff_vector.sql
find /data/ciff-backup -mtime +7 -delete
```

## 关键配置参数

| 配置 | 值 | 理由 |
|------|---|------|
| Nginx proxy_buffering | off (SSE) | 不缓冲，token 到了立即推 |
| Nginx proxy_read_timeout | 200s (SSE) | 覆盖 SseEmitter 的 180s 超时 |
| JVM -Xmx | 512m | 50 人足够，含 SSE 连接开销 |
| MySQL innodb_buffer_pool | 256M | QPS 极低，不需要更多 |
| Redis maxmemory | 128mb | 纯缓存 + Stream，LRU 淘汰 |
