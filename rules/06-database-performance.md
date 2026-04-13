# 数据库性能规范

## 通用字段约定

### 每张表必须包含

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT AUTO_INCREMENT | 主键，不使用 UUID（索引效率低） |
| `deleted` | TINYINT NOT NULL DEFAULT 0 | 逻辑删除：0=正常，1=已删除。禁止物理删除 |
| `create_time` | DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 硬规矩

1. **主键用自增 BIGINT，禁止 UUID** — UUID 无序导致 B+ 树频繁页分裂，写入性能差
2. **能 NOT NULL 就 NOT NULL** — 业务必填字段加 NOT NULL，空值用 `''` 或 `0`。**例外**：可选外键字段（如 `workflow_id`）允许 NULL，不要用 0 代替（0 是合法 ID，语义不清且导致误命中索引）
3. **金额和 Token 用量用 BIGINT 存最小精度** — 如金额存分（1.23 元 → 123），Token 数直接存整数。不用 DECIMAL（索引和计算开销大）
4. **枚举字段用 VARCHAR(32)，禁止 MySQL ENUM** — ENUM 加值要改表结构（DDL），VARCHAR 只需改应用代码

### 类型选用规则

| 场景 | 类型 | 禁止 |
|------|------|------|
| 主键 / 外键 | BIGINT AUTO_INCREMENT | INT（未来不够）、UUID（索引性能差） |
| 状态 / 枚举 | VARCHAR(32) | ENUM（加值需 DDL）、TINYINT（可读性差） |
| 短文本（名称、标题） | VARCHAR(n)，n 按实际最大长度 +20% | VARCHAR(255) 无脑默认 |
| 长文本（Prompt、消息） | TEXT | |
| JSON 配置 | JSON | VARCHAR 存 JSON 字符串 |
| 金额 | BIGINT（存分） | DECIMAL、FLOAT、DOUBLE |
| Token 用量 | BIGINT | DECIMAL、FLOAT |
| 布尔 | TINYINT (0/1) | BIT / BOOLEAN |
| 时间 | DATETIME | TIMESTAMP（2038 问题、时区坑） |

---

## 索引设计原则

### 必须建索引的场景

```sql
-- 1. 主键（自动创建）
PRIMARY KEY (id)

-- 2. 外键字段：每个 FK 必须有索引
ALTER TABLE t_agent ADD INDEX idx_agent_user_id (user_id);
ALTER TABLE t_agent ADD INDEX idx_agent_model_id (model_id);

-- 3. 唯一性约束字段（用 UNIQUE INDEX，不要只在代码层校验）
ALTER TABLE t_user ADD UNIQUE INDEX uk_user_username (username);
ALTER TABLE t_api_key ADD UNIQUE INDEX uk_api_key_prefix (key_prefix);

-- 4. 高频查询 WHERE 条件字段
ALTER TABLE t_conversation ADD INDEX idx_conv_user_status (user_id, status);
```

### 索引硬规矩

1. **逻辑删除字段必须加入索引** — 所有带 `deleted` 的查询条件都需要命中索引
2. **组合索引等值列在前，范围列在后** — `WHERE a = ? AND b > ?` → INDEX(a, b)，反过来 `b` 的范围会阻断 `a` 的等值匹配
3. **多对多关联表两个方向都要索引** — `t_agent_tool` 中按 agent_id 查和按 tool_id 查都需要索引
4. **唯一约束用 UNIQUE INDEX，不要只在代码层校验** — 并发下代码校验不可靠，数据库层兜底
5. **禁止在大文本字段建索引** — TEXT / JSON 字段不建普通索引，需要检索时用全文索引或检索引擎

### 索引命名规范

| 索引类型 | 前缀 | 示例 |
|----------|------|------|
| 普通索引 | `idx_` | `idx_agent_user_id` |
| 唯一索引 | `uk_` | `uk_user_username` |
| 联合索引 | `idx_` | `idx_conv_user_status` |

### 禁止建索引的场景

- 区分度低的字段（`status` 只有 0/1/2 几个值）
- 频繁更新的字段（索引维护成本 > 查询收益）
- 数据量 < 1000 行的小表（全表扫描更快）

### 联合索引：遵循最左前缀原则

```sql
-- 查询场景：WHERE user_id = ? AND status = ? ORDER BY create_time DESC
-- 联合索引把等值条件放前面，范围/排序放后面
ADD INDEX idx_conv_user_status_created (user_id, status, create_time);

-- 能命中：
WHERE user_id = 1
WHERE user_id = 1 AND status = 1
WHERE user_id = 1 AND status = 1 ORDER BY create_time DESC

-- 不能命中：
WHERE status = 1                    -- 跳过了 user_id
WHERE user_id = 1 ORDER BY status   -- 中间断层
```

---

## 大表预判与应对

### 增长预估（50 用户 / 年）

| 表 | 月增量 | 年增量 | 风险等级 |
|----|--------|--------|---------|
| t_chat_message | ~5 万 | ~60 万 | **高** |
| t_conversation | ~2000 | ~2.4 万 | 低 |
| t_knowledge_chunk (PGVector) | ~1 万 | ~12 万 | **中** |
| 其余表 | < 1000 | < 1.2 万 | 无 |

### t_chat_message（增长最快）

**V1 应对：索引 + 定期归档**

```sql
-- 必须的索引
ADD INDEX idx_msg_conv_id (conversation_id);                    -- 按会话查消息
ADD INDEX idx_msg_conv_created (conversation_id, create_time);   -- 分页加载历史消息

-- 归档策略：90 天前的消息迁移到归档表
-- 归档表结构相同，表名 t_chat_message_archive
-- 查询时 UNION ALL 两张表（由 Service 层处理）
CREATE TABLE t_chat_message_archive LIKE t_chat_message;
```

### t_knowledge_chunk（PGVector）

**PGVector 硬规矩：**

1. **必须建 HNSW 索引** — 无索引时向量检索是全量暴力计算，万级数据单次查询秒级
2. **检索时必须加 LIMIT** — 禁止不加 LIMIT 的向量查询，全量排序会拖垮数据库

```sql
-- 1. 建表后立即创建 HNSW 索引
CREATE INDEX idx_chunk_embedding ON t_knowledge_chunk
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 2. 查询时设置 ef_search（精度 vs 速度权衡）
SET LOCAL hnsw.ef_search = 40;   -- 默认 40，50 用户够用
-- 数据量到 50 万以上时调到 100

-- 3. 检索必须加 LIMIT，示例：
SELECT content, 1 - (embedding <=> $1) AS similarity
FROM t_knowledge_chunk
WHERE knowledge_id = $2
ORDER BY embedding <=> $1
LIMIT 5;                          -- 禁止省略 LIMIT
```

### t_conversation

```sql
-- 按用户查会话列表（高频）
ADD INDEX idx_conv_user_updated (user_id, update_time DESC);
```

---

## 分页查询规范

### 禁止深度 OFFSET 分页

```sql
-- ❌ 禁止：深度 OFFSET 性能随页数线性恶化
SELECT * FROM t_chat_message
WHERE conversation_id = 1
ORDER BY create_time DESC
LIMIT 10 OFFSET 100000;   -- 扫描 100010 行，只返回 10 行

-- ✅ 游标分页（seek method）：用上一页最后一条记录的 ID 做游标
SELECT * FROM t_chat_message
WHERE conversation_id = 1 AND id < #{lastId}
ORDER BY id DESC
LIMIT 10;                  -- 命中索引，只扫描 10 行
```

### MyBatis-Plus 分页配置

```java
// ciff-common: MybatisPlusConfig.java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件：限制单页最大 100 条，防止一次查太多
        interceptor.addInnerInterceptor(
            new PaginationInnerInterceptor(DbType.MYSQL, 100, true)
        );
        return interceptor;
    }
}
```

### 对话历史消息加载策略

```sql
-- 对话页面：加载最近 N 条消息，向上滚动时加载更多
-- 第一屏：最新消息
SELECT id, role, content, create_time
FROM t_chat_message
WHERE conversation_id = #{convId}
ORDER BY id DESC
LIMIT 20;

-- 向上滚动加载更多：传入当前最早消息的 ID
SELECT id, role, content, create_time
FROM t_chat_message
WHERE conversation_id = #{convId} AND id < #{oldestMsgId}
ORDER BY id DESC
LIMIT 20;
```

---

## 建表模板

```sql
-- MySQL 建表模板，AI 建表时严格遵循此格式

CREATE TABLE t_{module}_{entity} (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    -- ... 业务字段 ...
    -- 注意：所有字段 NOT NULL，空值用 '' 或 0 代替
    -- 注意：枚举用 VARCHAR(32)，不用 ENUM
    -- 注意：金额/Token 用 BIGINT 存最小精度

    deleted         TINYINT    NOT NULL DEFAULT 0   COMMENT '0=normal, 1=deleted',
    create_time      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',

    -- 索引
    INDEX idx_{table}_{column} ({column}),
    UNIQUE INDEX uk_{table}_{column} ({column})
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='{table description}';
```

```sql
-- PGVector 建表模板

CREATE TABLE t_knowledge_chunk (
    id              BIGSERIAL PRIMARY KEY,
    -- ... 业务字段 ...
    embedding       VECTOR(1536) NOT NULL,
    create_time      TIMESTAMP DEFAULT NOW()
);

-- 建表后立即创建 HNSW 索引
CREATE INDEX idx_chunk_embedding ON t_knowledge_chunk
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
```

---

## 全表索引清单

```sql
-- t_user
UNIQUE INDEX uk_user_username (username)

-- t_provider
INDEX idx_provider_status (status)

-- t_model
INDEX idx_model_provider_id (provider_id)

-- t_agent
INDEX idx_agent_user_id (user_id)
INDEX idx_agent_model_id (model_id)
INDEX idx_agent_workflow_id (workflow_id)
INDEX idx_agent_status (status)

-- t_agent_tool
INDEX idx_at_agent_id (agent_id)
INDEX idx_at_tool_id (tool_id)
UNIQUE INDEX uk_at_agent_tool (agent_id, tool_id)

-- t_agent_knowledge
INDEX idx_ak_agent_id (agent_id)
INDEX idx_ak_knowledge_id (knowledge_id)
UNIQUE INDEX uk_ak_agent_knowledge (agent_id, knowledge_id)

-- t_tool
INDEX idx_tool_status (status)

-- t_workflow
INDEX idx_workflow_user_id (user_id)

-- t_knowledge
INDEX idx_knowledge_user_id (user_id)

-- t_knowledge_document
INDEX idx_kd_knowledge_id (knowledge_id)
INDEX idx_kd_status (status)

-- t_conversation
INDEX idx_conv_user_updated (user_id, update_time DESC)
INDEX idx_conv_agent_id (agent_id)

-- t_chat_message
INDEX idx_msg_conv_created (conversation_id, create_time)

-- t_api_key
UNIQUE INDEX uk_api_key_prefix (key_prefix)
INDEX idx_api_key_user_id (user_id)
INDEX idx_api_key_agent_id (agent_id)
INDEX idx_api_key_status (status)
```

---

## SQL 编写规范

1. **禁止 SELECT *** — 明确列出所需字段，避免新增字段导致不必要的 IO 和网络开销
2. **禁止在 WHERE 条件中对索引列使用函数或运算** — `WHERE YEAR(create_time) = 2026` 会导致全表扫描，改为 `WHERE create_time >= '2026-01-01' AND create_time < '2027-01-01'`
3. **禁止隐式类型转换** — 字段是 VARCHAR 则条件必须传字符串，`WHERE phone = 13800138000` 会导致索引失效
4. **LIKE 禁止前导通配符** — `LIKE '%abc'` 走全表扫描，必须用 `LIKE 'abc%'` 或全文索引
5. **IN 列表不超过 500 个** — 超过时用临时表 JOIN 或分批查询
6. **禁止在代码中拼接 SQL** — 必须用 MyBatis-Plus 的 QueryWrapper 或 XML 参数绑定，防止 SQL 注入
7. **子查询优先改写为 JOIN** — MySQL 对子查询优化有限，JOIN 执行计划更可控
8. **UPDATE/DELETE 必须带 WHERE** — 禁止无条件更新/删除，即使是全表操作也要显式写 WHERE 1=1

---

## 事务规范

9. **事务中禁止 RPC / 外部调用** — 事务内只做数据库操作，外部调用失败会导致长事务和连接泄漏
10. **事务粒度最小化** — 事务包裹的代码只包含必须原子性的 DML，查询和计算放在事务外
11. **禁止嵌套事务** — Spring `@Transactional` 默认 REQUIRED，嵌套调用共享同一事务，内层 rollback 会影响外层。需要独立事务时用 `REQUIRES_NEW`
12. **读多写少的查询不加 `@Transactional`** — 只读查询用 `@Transactional(readOnly = true)`，避免持有写锁

---

## DDL 与变更管理

13. **加列只能 APPEND 到表末尾** — 禁止 `AFTER` / `FIRST`，MySQL 不支持在线 DDL 重排列，会导致表重建锁表
14. **大表 DDL 在低峰期执行** — 超过 10 万行的表，ALTER 期间用 `pt-online-schema-change` 或业务低谷时段执行
15. **禁止使用存储过程、触发器、视图** — 业务逻辑全部在应用层，数据库只负责存储，便于调试和版本管理
16. **禁止使用外键约束** — 外键影响插入/删除性能，且 DDL 变更困难。关联完整性由应用层保证
17. **单表字段数不超过 30** — 超过考虑垂直拆分，将低频字段或大字段拆到扩展表

---

## 数据安全

18. **敏感字段加密存储** — 密码用 BCrypt 哈希，API Key 等凭证用 AES 加密，禁止明文
19. **禁止在日志中打印完整 SQL 参数值中的敏感字段** — 密码、Token 等脱敏后记录
20. **逻辑删除的数据不参与唯一约束校验** — `uk_username` 需改为联合唯一索引 `uk_username_deleted`，或 deleted 值使用 ID 而非固定值 1
