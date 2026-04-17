# Ciff 核心数据表设计

## 表清单

### 用户
- `t_user` — 用户账号

### 模型管理
- `t_provider` — 模型供应商（OpenAI / Claude / Ollama...）
- `t_model` — 具体模型（gpt-4o / claude-sonnet...）
- `t_provider_health` — 供应商健康状态（连续失败次数、延迟、探测时间）

### Agent
- `t_agent` — Agent 配置（Prompt、模型参数、类型）
- `t_agent_tool` — Agent 与工具的绑定关系（中间表）
- `t_agent_knowledge` — Agent 与知识库的绑定关系（中间表）

### MCP 工具
- `t_tool` — 工具定义（名称、描述、接口地址、参数 Schema）

### 工作流
- `t_workflow` — JSON 工作流定义

### 知识库
- `t_knowledge` — 知识库
- `t_knowledge_document` — 文档元数据（文件名、状态、分块数）
- `t_knowledge_chunk` — 向量分块（PGVector，content + embedding）

### 对话
- `t_conversation` — 会话
- `t_chat_message` — 消息（用户消息 / AI 回复 / 工具调用结果）

### API 发布
- `t_api_key` — API 调用密钥

## 表关系

```
t_user 1─────N t_agent
t_user 1─────N t_workflow
t_user 1─────N t_knowledge
t_user 1─────N t_conversation
t_user 1─────N t_api_key

t_provider 1─────N t_model
t_provider 1─────1 t_provider_health
t_model 1─────N t_agent

t_agent N─────N t_tool          (via t_agent_tool)
t_agent N─────N t_knowledge     (via t_agent_knowledge)
t_agent 1─────0..1 t_workflow   (optional)
t_agent 1─────N t_api_key

t_knowledge 1─────N t_knowledge_document
t_knowledge_document 1─────N t_knowledge_chunk   (PGVector)

t_agent 1─────N t_conversation
t_conversation 1─────N t_chat_message
```

## 数据库分布

| 数据库 | 表 | 用途 |
|--------|---|------|
| MySQL 8.x | t_user, t_provider, t_model, t_provider_health, t_agent, t_agent_tool, t_agent_knowledge, t_tool, t_workflow, t_knowledge, t_knowledge_document, t_conversation, t_chat_message, t_api_key | 业务数据（共 14 张） |
| PGVector | t_knowledge_chunk | 向量索引（1 张，含 embedding 字段） |
