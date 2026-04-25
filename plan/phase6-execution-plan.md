# Phase 6 执行计划：API 发布 + 用户认证 + 部署

> 创建时间：2026-04-25
> 技术决策：采用 Sa-Token + JWT 替代 Spring Security（轻量、API 简洁、内置 Redis 集成）

## Goal

系统可对外发布 API，支持 JWT 登录鉴权，可 Docker 单机一键部署。

## Prerequisites

- Phase 0-5 全部完成 ✅
- `t_user` 表已存在（bcrypt 密码字段）
- `t_api_key` 表已存在（key_hash + key_prefix）
- `UserIdInterceptor` + `UserContext` 已在 ciff-common 中（临时方案待替换）
- 前端 `request.ts` 硬编码 `X-User-Id: 1`（待替换为 Sa-Token JWT）

## 现有基础审计

| 组件 | 状态 | 说明 |
|------|------|------|
| `t_user` DDL | ✅ 已有 | username / password(bcrypt) / role(admin,user) |
| `t_api_key` DDL | ✅ 已有 | key_hash / key_prefix / permissions(JSON) / expires_at |
| `UserContext` ThreadLocal | ✅ 已有 | set/get/clear |
| `UserIdInterceptor` | ✅ 已有 | 读 X-User-Id Header，Phase 6 后改为从 Sa-Token 读取 |
| 前端 `request.ts` | ⚠️ 硬编码 | `X-User-Id: 1`，需改为 `Authorization: Bearer {token}` |
| 前端路由守卫 | ❌ 缺失 | router/index.ts 无 beforeEnter/beforeEach 守卫 |
| Sa-Token 依赖 | ❌ 缺失 | pom.xml 无 sa-token 相关依赖 |
| Docker 文件 | ❌ 缺失 | 无 Dockerfile / docker-compose.yml |
| Nginx 配置 | ❌ 缺失 | 参照 rules/04-deployment.md |
| 外部对话接口 | ❌ 缺失 | 无 /api/v1/external/chat |

## 技术方案决策记录

### 认证框架：Sa-Token + JWT（替代 Spring Security）

**决策理由**：
- Spring Security 过重，FilterChain 体系复杂，本项目不需要 OAuth2/ACL 等高级特性
- Sa-Token API 简洁：`StpUtil.login(id)` 签发、`StpUtil.getLoginIdAsLong()` 取用户
- 内置 Redis 集成（`sa-token-redis-jackson`），Token 自动持久化，支持登出/踢人
- 整合 JWT（`sa-token-jwt`）：Token 格式为 JWT（可解析），同时 Redis 存一份（可吊销）
- 密码加密仍用 bcrypt（`BCryptPasswordEncoder`）

**依赖清单**：
```xml
sa-token-spring-boot3-starter 1.39.0
sa-token-jwt 1.39.0
sa-token-redis-jackson 1.39.0
spring-security-crypto（仅用 BCryptPasswordEncoder，不引入完整 Spring Security）
```

**认证流程**：
```
登录 → StpUtil.login(userId) → 自动签发 JWT Token + 存 Redis → 返回给前端
请求 → SaInterceptor 拦截 → Sa-Token 自动验证 JWT 签名 + 查 Redis → 放行
登出 → StpUtil.logout() → Redis 删除 → Token 立即失效
```

**UserContext 兼容方案**：
- `UserIdInterceptor` 保留，内部改为从 `StpUtil.getLoginIdAsLong()` 读取 userId 设置到 UserContext
- 业务层代码（UserContext.getUserId()）无需任何改动

---

## Steps

### Step 1：Sa-Token + JWT 认证基础设施 ✅ 2026-04-25

**Task**：引入 Sa-Token 依赖，配置 JWT 模式 + Redis 持久化，实现鉴权拦截。

**Output**：
- `ciff-common/pom.xml` 新增依赖：`sa-token-spring-boot3-starter`、`sa-token-jwt`、`spring-security-crypto`（仅 BCryptPasswordEncoder）
- `ciff-app/pom.xml` 新增依赖：`sa-token-redis-jackson`
- `SaTokenConfig.java`（ciff-app）：注册 SaInterceptor，配置公开路径（`/api/auth/**`、`/api/v1/external/**`、`/doc.html`）+ 受保护路径
- `application.yml` 新增 Sa-Token 配置项：`sa-token.token-name`、`sa-token.timeout`、`sa-token.jwt-secret-key`
- 修改 `UserIdInterceptor`：内部改为 `StpUtil.getLoginIdAsLong()` 设置 UserContext
- Flyway 迁移：向 `t_user` 插入默认管理员账户（admin / bcrypt 加密初始密码）

**Verification**：
- `mvn compile` 通过
- `SaTokenConfigTest`：拦截器路径匹配正确、公开路径不拦截
- `UserIdInterceptorTest`：从 Sa-Token 读取 userId 设置到 UserContext

**Dependencies**：无前置

---

### Step 2：用户登录鉴权接口 ✅ 2026-04-25

**Task**：实现登录、注册、获取当前用户信息、登出接口。

**Output**：
- `ciff-app` 新增 `AuthController`：`POST /api/auth/login`、`POST /api/auth/register`、`GET /api/auth/me`、`POST /api/auth/logout`
- `AuthService`：验证用户名密码 → `StpUtil.login(userId)` → 返回 Token
- `PasswordService`：bcrypt 加密/校验（`BCryptPasswordEncoder`）
- `UserService`：用户 CRUD（查用户、创建用户）
- 前端 `request.ts`：移除硬编码 `X-User-Id`，改为从 localStorage 读取 Token 设置 `Authorization: Bearer {token}`；401 响应自动跳转登录页

**Verification**：
- `AuthControllerTest`：登录成功/失败、注册校验、获取用户信息、登出
- `PasswordServiceTest`：bcrypt 加密匹配/不匹配

**Dependencies**：Step 1

---

### Step 3：API Key 管理 + 外部对话接口 ✅ 2026-04-25

**Task**：实现 API Key 生成/撤销/验证，以及通过 API Key 调用的外部对话接口。

**Output**：
- `ciff-app` 新增 `ApiKeyController`：`POST /api/keys`（生成）、`GET /api/keys`（列表）、`DELETE /api/keys/{id}`（撤销）
- `ApiKeyService`：生成随机 Key → SHA256 存储 + 前 8 位前缀 → 返回明文（仅此一次）
- `ApiKeyAuthFilter`（Sa-Token 自定义 Filter）：识别 `X-API-Key` Header 或 `?api_key=` 参数，验证 Key → `StpUtil.login(userId)` 临时登录
- `ExternalChatController`：`POST /api/v1/external/chat`（非流式）、`GET /api/v1/external/chat/stream`（SSE 流式）
- SaTokenConfig 中 `/api/v1/external/**` 路径使用 API Key 认证

**Verification**：
- `ApiKeyControllerTest`：生成（返回明文）/ 列表（脱敏）/ 撤销
- `ExternalApiTest`：合法 Key 调用成功 / 无效 Key 401 / 过期 Key 403 / 无 Key 401
- `ApiKeyServiceTest`：Key 哈希生成、前缀提取、过期判定

**Dependencies**：Step 1（SaTokenConfig）

---

### Step 4：前端登录页面 + 路由守卫 ✅ 2026-04-25

**Task**：实现登录页面、Token 管理、路由守卫拦截未登录访问。

**Output**：
- `LoginView.vue`：用户名/密码表单、登录调用、Token 存储（localStorage）
- `router/index.ts`：全局 `beforeEach` 守卫，未登录跳转 `/login`，`/login` 页面无需认证
- `auth.ts`（utils）：`getToken()` / `setToken()` / `removeToken()` / `isAuthenticated()`
- `request.ts` 完善：401 拦截 → 清除 Token → 跳转登录页
- 登出功能：头部区域用户名 + 登出按钮

**Verification**：
- `LoginView.spec.ts`：表单校验、登录成功跳转、Token 存储、错误提示
- 手动验证：未登录访问任意页面 → 跳转登录 → 登录后正常使用 → 登出清除

**Dependencies**：Step 2

---

### Step 5：前端 API Key 管理页面 ✅ 2026-04-25

**Task**：API Key 列表、生成弹窗（仅显示一次明文）、撤销确认。

**Output**：
- `ApiKeyList.vue`：列表（名称/前缀/关联Agent/状态/创建时间）+ 生成弹窗 + 撤销确认
- 路由：`/api-keys`
- 侧边栏菜单新增入口
- 生成时弹窗提示"请立即复制，后续无法再查看明文"

**Verification**：
- `ApiKeyList.spec.ts`：列表渲染、生成弹窗交互、Key 显示/隐藏、撤销确认
- 手动验证：生成 Key → 复制 → 列表只显示前缀 → 撤销成功

**Dependencies**：Step 3 + Step 4

---

### Step 6：Docker Compose 部署配置 ⏸️ 暂停（待用户确认后继续）

**Task**：创建完整的 Docker 单机部署方案。

**Output**：
- `deploy/docker-compose.yml`：MySQL 8 / Redis 7 / PostgreSQL + PGVector / ciff-app / Nginx
- `deploy/Dockerfile`：多阶段构建（Maven build + JRE 17 运行）
- `deploy/nginx/nginx.conf`：`/api/*` 反向代理后端、静态资源托管、SSE `proxy_buffering off`、`/api/v1/external/**` 路径
- `deploy/.env.example`：环境变量模板（数据库密码、JWT 密钥等）
- `deploy/scripts/verify-deploy.sh`：健康检查脚本
- Nginx 配置参照 `rules/04-deployment.md`

**Verification**：
- `docker-compose up -d` 全部服务健康
- `/api/*` 正常转发
- SSE 流式正常（`proxy_buffering off` 生效）
- 静态资源正常加载
- `scripts/verify-deploy.sh` 通过

**Dependencies**：Step 1-3（后端代码完成）

---

### Step 7：前端全局 UI 打磨 ✅ 2026-04-25

**Task**：优化全局用户体验——响应式布局、空状态、加载骨架屏、全局错误提示。

**Output**：
- 各列表页空状态组件统一
- 加载状态骨架屏
- 全局错误提示优化（网络错误/业务错误/认证错误）
- 响应式布局适配（侧边栏折叠）
- 整体视觉一致性检查

**Verification**：
- 手动检查各页面空状态展示
- 慢网络下骨架屏正常显示
- 错误场景（网络断开、Token 过期）提示友好

**Dependencies**：Step 4-5

---

### Step 8：测试补齐 + 验收 ✅ 2026-04-25

**Task**：补齐所有 Phase 6 测试用例，执行完整验收。

**Output**：
- 补齐测试用例 T6.1-T6.10
- 全量测试执行
- 验收清单逐项确认

**Verification**：
- `mvn test -pl ciff-app,ciff-common` 全部通过
- `cd ciff-web && npm run test` 全部通过
- Docker 部署验收脚本通过

**Dependencies**：Step 1-7

---

## Dependencies DAG

```
Step 1 (Sa-Token + JWT 基础设施)
├── Step 2 (登录接口) ──► Step 4 (前端登录页)
│                            ├── Step 5 (API Key 页面)
│                            └── Step 7 (UI 打磨)
└── Step 3 (API Key + 外部接口) ──► Step 5
                                       │
Step 6 (Docker 部署) ◄── Step 1-3 ─────┘
                                       │
Step 8 (测试验收) ◄── Step 1-7 ────────┘
```

## Parallel Opportunities

| 并行组 | 可同时进行的步骤 |
|--------|----------------|
| 并行组 A | Step 1（Sa-Token 基础设施） |
| 并行组 B | Step 1 完成后：Step 2（登录接口）+ Step 3（API Key）可并行 |
| 并行组 C | Step 2 完成后：Step 4（前端登录）可开始；Step 3 完成后 Step 5 可与 Step 4 并行 |
| 并行组 D | Step 6（Docker）可在 Step 1-3 完成后与 Step 4-5 并行 |
| 并行组 E | Step 7（UI 打磨）与 Step 6 可并行 |

## Test Gate

| 时机 | 范围 |
|------|------|
| Step 1 完成后 | `mvn compile` + `SaTokenConfigTest` |
| Step 2 完成后 | `mvn test -pl ciff-app -Dtest="AuthControllerTest,PasswordServiceTest"` |
| Step 3 完成后 | `mvn test -pl ciff-app -Dtest="ApiKeyControllerTest,ExternalApiTest,ApiKeyServiceTest"` |
| Step 4 完成后 | `cd ciff-web && npm run test` |
| Step 8 验收 | 全量 `mvn test` + `npm run test` + Docker 验收脚本 |

## Acceptance Criteria（验收标准）

- [ ] API Key：无效/过期 Key 返回 401/403；Key 仅生成时显示一次明文，数据库存 SHA256 + 前缀
- [ ] JWT（Sa-Token 签发）：合法 Token 可访问受保护接口；缺失/过期/篡改被拦截；过期时间可配置
- [ ] 密码 bcrypt 加密存储，不出现明文
- [ ] `docker-compose up -d` 一键启动全部服务健康；SSE 接口 `proxy_buffering off` 生效
- [ ] 前端 Vitest 通过；路由守卫未登录时正确拦截
- [ ] Docker 全量启动时间 < 2 分钟；登录 P95 < 200ms；外部 API（含 Key 校验）P95 < 500ms
- [ ] 无安全漏洞：JWT 密钥不硬编码、API Key 不明文存储、密码不日志输出
- [ ] Sa-Token Redis 集成正常：登出后 Token 立即失效
