# 接口规范

## 文档

- 用 Swagger 生成接口文档
- 使用接口`/v3/api-docs`导出

## 路径
RESTful 风格：/api/v1/{资源复数名}
GET    /api/v1/providers          # 列表（分页）
POST   /api/v1/providers          # 创建
GET    /api/v1/providers/{id}     # 详情
PUT    /api/v1/providers/{id}     # 更新
DELETE /api/v1/providers/{id}     # 删除
POST   /api/v1/providers/{id}/test-connection  # 非 CRUD 操作用动词

## 入参校验
所有创建和更新接口必须校验入参。使用 `@Valid` + JSR-303 注解（`@NotBlank`、`@NotNull`、`@Size` 等），校验注解加在 Request DTO 上，Controller 方法参数前加 `@Valid`。

## 统一响应
所有接口返回 Result<T>：
{ "code": 200, "message": "success", "data": {...} }

## 分页
请求：page（从 1 开始）、pageSize（默认 20，最大 100）
响应：Result<PageResult<T>>，PageResult 包含 list、total、page、pageSize

## 空值
- 列表字段空时返回 []，不返回 null
- 字符串字段空时返回 ""，不返回 null
- 对象不存在时返回 null

## 错误码
四位数字，按模块分段：
1000-1999 通用
000-2999 Provider
3000-3999 Agent
4000-4999 Chat
5000-5999 MCP
000-6999 Workflow
7000-7999 Knowledge
