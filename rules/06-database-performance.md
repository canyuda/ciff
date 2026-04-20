# 数据库性能规范

## 通用字段约定

### 每张表必须包含

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT AUTO_INCREMENT | 主键，不使用 UUID |
| `deleted` | TINYINT NOT NULL DEFAULT 0 | 逻辑删除：0=正常，1=已删除 |
| `create_time` | DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 硬规矩

1. **主键用自增 BIGINT，禁止 UUID** — UUID 无序导致 B+ 树频繁页分裂
2. **能 NOT NULL 就 NOT NULL** — 业务必填字段加 NOT NULL，空值用 `''` 或 `0`。**例外**：可选外键字段允许 NULL
3. **金额和 Token 用量用 BIGINT 存最小精度** — 不用 DECIMAL
4. **枚举字段用 VARCHAR(32)，禁止 MySQL ENUM** — ENUM 加值要改表结构

### 类型选用规则

| 场景 | 类型 | 禁止 |
|------|------|------|
| 主键 / 外键 | BIGINT AUTO_INCREMENT | INT、UUID |
| 状态 / 枚举 | VARCHAR(32) | ENUM、TINYINT |
| 短文本 | VARCHAR(n)，n 按实际最大长度 +20% | VARCHAR(255) 无脑默认 |
| 长文本（Prompt、消息） | TEXT | |
| JSON 配置 | JSON | VARCHAR 存 JSON 字符串 |
| 金额 | BIGINT（存分） | DECIMAL、FLOAT、DOUBLE |
| Token 用量 | BIGINT | DECIMAL、FLOAT |
| 布尔 | TINYINT (0/1) | BIT / BOOLEAN |
| 时间 | DATETIME | TIMESTAMP |

---

## 索引设计原则

### 必须建索引的场景

- 主键（自动创建）
- 外键字段：每个 FK 必须有索引
- 唯一性约束字段（用 UNIQUE INDEX）
- 高频查询 WHERE 条件字段

### 索引硬规矩

1. **逻辑删除字段必须加入索引**
2. **组合索引等值列在前，范围列在后** — `WHERE a = ? AND b > ?` → INDEX(a, b)
3. **多对多关联表两个方向都要索引**
4. **唯一约束用 UNIQUE INDEX，不要只在代码层校验**
5. **禁止在大文本字段建索引**

### 索引命名规范

| 索引类型 | 前缀 | 示例 |
|----------|------|------|
| 普通索引 | `idx_` | `idx_agent_user_id` |
| 唯一索引 | `uk_` | `uk_user_username` |

### 禁止建索引的场景

- 区分度低的字段
- 频繁更新的字段
- 数据量 < 1000 行的小表

---

## 大表预判与应对

| 表 | 月增量 | 风险等级 |
|----|--------|---------|
| t_chat_message | ~5 万 | **高** |
| t_knowledge_chunk (PGVector) | ~1 万 | **中** |
| 其余表 | < 1000 | 无 |

### t_chat_message

- 必须索引：`idx_msg_conv_created (conversation_id, create_time)`
- 归档策略：90 天前的消息迁移到归档表 `t_chat_message_archive`

### t_knowledge_chunk（PGVector）

- 必须建 HNSW 索引 — 无索引时向量检索是全量暴力计算
- 检索时必须加 LIMIT — 禁止不加 LIMIT 的向量查询
- 建表后立即创建 HNSW 索引，`ef_search` 默认 40

---

## 分页查询规范

- **禁止深度 OFFSET 分页** — 用游标分页（seek method）：`WHERE id < #{lastId} LIMIT 10`
- MyBatis-Plus 分页插件限制单页最大 100 条

---

## 建表模板

见 `docs/rules-snippets/06-create-table-templates.sql`

---

## 全表索引清单

见 `docs/rules-snippets/06-full-index-list.sql`

---

## SQL 编写规范

1. **禁止 SELECT ***
2. **禁止在 WHERE 条件中对索引列使用函数或运算**
3. **禁止隐式类型转换**
4. **LIKE 禁止前导通配符**
5. **IN 列表不超过 500 个**
6. **禁止在代码中拼接 SQL** — 必须用参数绑定
7. **子查询优先改写为 JOIN**
8. **UPDATE/DELETE 必须带 WHERE**

---

## 事务规范

9. **事务中禁止 RPC / 外部调用**
10. **事务粒度最小化**
11. **禁止嵌套事务** — 需要独立事务时用 `REQUIRES_NEW`
12. **读多写少的查询不加 `@Transactional`** — 只读查询用 `@Transactional(readOnly = true)`

---

## DDL 与变更管理

13. **加列只能 APPEND 到表末尾** — 禁止 `AFTER` / `FIRST`
14. **大表 DDL 在低峰期执行** — 超过 10 万行用 `pt-online-schema-change`
15. **禁止使用存储过程、触发器、视图**
16. **禁止使用外键约束** — 关联完整性由应用层保证
17. **单表字段数不超过 30**

---

## 数据安全

18. **敏感字段加密存储** — 密码用 BCrypt，API Key 用 AES
19. **禁止在日志中打印完整 SQL 参数值中的敏感字段**
20. **逻辑删除的数据不参与唯一约束校验** — `uk_username` 需改为 `uk_username_deleted`
