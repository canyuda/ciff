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

完整配置见 `docs/rules-snippets/04-docker-compose.yml`。

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
