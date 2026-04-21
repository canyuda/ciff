# Phase 5 执行计划：Workflow 工作流

> **目标**：JSON 配置的工作流——线性步骤 + 条件分支。
> **涉及模块**：`ciff-workflow` / `ciff-app`（聚合层扩展） / `ciff-web`
>
> **设计决策**：V1 不做可视化拖拽编辑器，工作流通过 JSON 定义步骤序列。每个步骤有输入（引用前序步骤输出或全局变量）和输出（写入上下文）。

---

## 一、前置 Phase 状态检查

| Phase | 状态 | 证据 |
|-------|------|------|
| Phase 0 | 完成 | UserContext / Flyway / 枚举 / 双数据源就绪 |
| Phase 1 | 完成 | `mvn test -pl ciff-provider,ciff-common` 全部通过 |
| Phase 2 | 完成 | `mvn test -pl ciff-agent,ciff-mcp,ciff-app` 全部通过 |
| Phase 3 | **未完成** | 知识库 CRUD 未开发，PGVector 未接入 |
| Phase 4 | 完成 | Chat 对话引擎（后端+前端+测试）全部完成 |

> **⚠️ 重要风险**：Phase 3（知识库）未完成。Phase 5.2 工作流执行引擎的前置依赖包含 Phase 3.3（PGVector 向量检索）。
>
> **影响范围**：工作流步骤类型中的 `knowledge_retrieval`（知识库检索）无法完整实现，因为缺少 embedding 生成和向量检索能力。
>
> **建议方案**：
> 1. **方案 A（推荐）**：Phase 5 先实现 `llm` / `tool` / `condition` 三种步骤类型，`knowledge_retrieval` 步骤类型标记为 TODO，等 Phase 3 完成后补齐。
> 2. **方案 B**：先完成 Phase 3，再启动 Phase 5。
> 3. **方案 C**：Phase 5 中 `knowledge_retrieval` 降级为基于 MySQL `t_knowledge_document` 的全文检索（精确匹配），不做语义检索。

---

## 二、状态快照

### 已就绪的基础设施

| 组件 | 状态 | 说明 |
|------|------|------|
| 数据库表 `t_workflow` | 已就绪 | `V1__init_schema.sql` 中已定义（id/user_id/name/description/definition/status/deleted） |
| `ciff-workflow` 模块 | 空壳 | `pom.xml` 已配置，依赖 ciff-common/provider/agent/knowledge，但无源代码 |
| 前端工作流页面 | 不存在 | `views/` 下无 workflow 目录或文件 |

### 需要新建的文件清单

**后端**（`ciff-workflow` 模块）：
```
ciff-workflow/src/main/java/com/ciff/workflow/
  entity/WorkflowPO.java
  mapper/WorkflowMapper.java
  dto/WorkflowCreateRequest.java
  dto/WorkflowUpdateRequest.java
  dto/WorkflowVO.java
  dto/WorkflowPageQuery.java
  service/WorkflowService.java
  service/impl/WorkflowServiceImpl.java
  convertor/WorkflowConvertor.java
  controller/WorkflowController.java
  engine/WorkflowEngine.java
  engine/WorkflowContext.java
  engine/step/StepExecutor.java
  engine/step/LlmStepExecutor.java
  engine/step/ToolStepExecutor.java
  engine/step/ConditionStepExecutor.java
  engine/dto/StepDefinition.java
  engine/dto/ConditionRule.java
  engine/dto/WorkflowExecutionResult.java
  exception/WorkflowException.java
  exception/InvalidWorkflowDefinitionException.java

ciff-app/src/main/java/com/ciff/app/controller/
  AppWorkflowController.java
```

**前端**（`ciff-web`）：
```
ciff-web/src/
  api/workflow.ts
  views/workflow/WorkflowList.vue
  views/workflow/components/WorkflowEditor.vue
  views/workflow/components/WorkflowExecutionDialog.vue
```

**测试**：
```
ciff-workflow/src/test/java/com/ciff/workflow/
  controller/WorkflowControllerTest.java
  service/WorkflowServiceTest.java
  mapper/WorkflowMapperTest.java
  engine/WorkflowEngineTest.java
  engine/WorkflowConditionTest.java
  engine/WorkflowExecutionTest.java
  engine/WorkflowUserContextTest.java

ciff-web/src/views/workflow/__tests__/
  WorkflowList.spec.ts
```

---

## 三、执行步骤

### Step 1：后端 — Workflow CRUD 基础层

| 项 | 内容 |
|----|------|
| Task | 实现 `t_workflow` 的 Entity / Mapper / DTO / Convertor / Service / Controller |
| Output | `ciff-workflow` 模块完整 CRUD 代码 |
| Verification | `mvn test -pl ciff-workflow` 中 Mapper 集成测试 + Service 单元测试通过 |

**DTO 设计**：

```java
// WorkflowCreateRequest
public class WorkflowCreateRequest {
    @NotBlank private String name;
    private String description;
    @NotNull  private WorkflowDefinition definition;  // JSON 结构
    private String status = "draft";  // active / inactive / draft
}

// WorkflowDefinition (JSON 根结构)
public class WorkflowDefinition {
    private List<StepDefinition> steps;
    private Map<String, Object> inputs;  // 全局输入变量定义
}

// StepDefinition
public class StepDefinition {
    @NotBlank private String id;           // 步骤唯一标识
    @NotBlank private String type;         // llm / tool / condition / knowledge_retrieval
    @NotBlank private String name;
    private Map<String, Object> config;    // 步骤配置（根据 type 变化）
    private List<String> dependsOn;        // 依赖的前序步骤 id
    private Map<String, String> outputs;   // 输出变量映射
}

// ConditionRule (condition 类型专用)
public class ConditionRule {
    private String operator;    // eq / contains / gt / default
    private String field;       // 引用变量路径，如 "${step1.output.result}"
    private Object value;       // 对比值
    private String nextStepId;  // 命中时跳转的步骤
}
```

**数据库映射**：
- `definition` 字段存 JSON，`WorkflowPO` 中用 `WorkflowDefinition` 对象 + `@TableField(typeHandler = JacksonTypeHandler.class)`
- 遵循 `SoftDeletableEntity` 基类（含 `deleted` 逻辑删除字段）

**Service 层校验**：
- `name` 非空且唯一（同一 user_id 下）
- `definition.steps` 非空
- 每个 step 必须有 `id` 和 `type`
- step `type` 必须是允许的枚举值（`llm`, `tool`, `condition`；V1 暂不支持 `knowledge_retrieval`）
- step `id` 在整个 steps 列表中唯一

---

### Step 2：后端 — 工作流执行引擎

| 项 | 内容 |
|----|------|
| Task | 实现工作流 JSON 解析 + 按序执行 + 上下文变量传递 + 条件分支 |
| Output | `WorkflowEngine` 及各类 `StepExecutor` |
| Verification | `WorkflowEngineTest` / `WorkflowConditionTest` / `WorkflowExecutionTest` 通过 |

**引擎架构**：

```
WorkflowEngine.execute(definition, inputs)
    │
    ├──→ 1. 拓扑排序（按 dependsOn 构建 DAG，检测循环引用）
    │
    ├──→ 2. 初始化 WorkflowContext（存储变量）
    │
    ├──→ 3. 按序遍历步骤
    │       │
    │       ├──→ llm 步骤 → LlmStepExecutor
    │       │              ├──→ 组装 prompt（支持变量插值 ${stepId.output.xxx}）
    │       │              ├──→ 调用 ProviderFacade 获取 LLM 配置 → ChatClient
    │       │              └──→ 将回复写入 context
    │       │
    │       ├──→ tool 步骤 → ToolStepExecutor
    │       │              ├──→ 解析 tool 调用参数（变量插值）
    │       │              ├──→ 调用 ToolService 执行
    │       │              └──→ 将结果写入 context
    │       │
    │       └──→ condition 步骤 → ConditionStepExecutor
    │                      ├──→ 获取判断字段值（从 context）
    │                      ├──→ 按规则顺序匹配（eq / contains / gt / default）
    │                      └──→ 返回命中规则的 nextStepId
    │
    └──→ 4. 返回 WorkflowExecutionResult（每步骤输出 + 整体结果）
```

**变量插值语法**：
- `${stepId.output.xxx}` — 引用某步骤的输出字段
- `${inputs.xxx}` — 引用全局输入变量
- 支持字符串模板：`"请将以下内容翻译：${step1.output.text}"`

**步骤类型配置示例**：

```json
// llm 步骤
{
  "id": "translate",
  "type": "llm",
  "name": "翻译",
  "config": {
    "modelId": 1,
    "systemPrompt": "你是一个翻译助手",
    "userPrompt": "请将以下内容翻译成英文：${inputs.text}"
  },
  "outputs": { "result": "translatedText" }
}

// tool 步骤
{
  "id": "weather",
  "type": "tool",
  "name": "查询天气",
  "config": {
    "toolId": 3,
    "params": { "city": "${inputs.city}" }
  },
  "outputs": { "result": "weatherInfo" }
}

// condition 步骤
{
  "id": "check_sentiment",
  "type": "condition",
  "name": "情感判断",
  "config": {
    "field": "${step1.output.sentiment}",
    "rules": [
      { "operator": "eq", "value": "positive", "nextStepId": "positive_reply" },
      { "operator": "eq", "value": "negative", "nextStepId": "negative_reply" },
      { "operator": "default", "nextStepId": "neutral_reply" }
    ]
  }
}
```

**循环引用检测**：
- 在引擎执行前，先构建步骤依赖图
- DFS 检测环，发现环时抛出 `InvalidWorkflowDefinitionException`

---

### Step 3：后端 — ciff-app 聚合层 + 联调测试

| 项 | 内容 |
|----|------|
| Task | 创建 `AppWorkflowController`，补齐测试用例 |
| Output | `ciff-app` 新增聚合接口 + 完整测试套件 |
| Verification | `mvn test -pl ciff-workflow,ciff-app` 全部通过 |

**聚合层职责**：
- `AppWorkflowController` 暴露 `/api/v1/app/workflows` 接口
- 创建/更新时校验 `modelId`（llm 步骤中引用的模型）是否存在 —— 复用 `ProviderFacade`
- 执行工作流时校验 `toolId`（tool 步骤中引用的工具）是否存在 —— 复用 `ToolService`

**测试用例清单**：

| 编号 | 类型 | 用例 | 覆盖 |
|------|------|------|------|
| T5.1 | Controller 切片 | `WorkflowControllerTest`：CRUD / definition JSON 校验 / 分页 | ciff-workflow |
| T5.2 | Service 单元 | `WorkflowServiceTest`：definition 格式校验 / 循环引用检测 / 步骤类型校验 / 重复 name | ciff-workflow |
| T5.3 | Mapper 集成 | `WorkflowMapperTest`：CRUD + `UserContext` user_id 隔离 | ciff-workflow |
| T5.4 | 单元 | `WorkflowEngineTest`：单步 LLM 调用 / 单步工具调用 / 线性多步骤 / 上下文传递 / 变量插值 | ciff-workflow |
| T5.5 | 单元 | `WorkflowConditionTest`：等于 / 包含 / 大于 / 默认分支 / 条件嵌套 / 无匹配分支 | ciff-workflow |
| T5.6 | 集成 | `WorkflowExecutionTest`：完整 JSON 工作流端到端执行（3 步骤 + 1 条件分支） | ciff-workflow |
| T5.7 | Controller 切片 | `AppWorkflowControllerTest`：聚合层 model_id / tool_id 外键校验 | ciff-app |
| T5.9 | 单元 | `WorkflowUserContextTest`：工作流异步步骤 userId 透传正确 | ciff-workflow |

---

### Step 4：前端 — API 层 + 工作流管理页面

| 项 | 内容 |
|----|------|
| Task | 创建 `api/workflow.ts` + `WorkflowList.vue` + JSON 编辑器组件 |
| Output | `ciff-web` 工作流管理页面 |
| Verification | Vitest 通过 + 手动联调：创建 → 编辑 → 执行 |

**API 层**（`api/workflow.ts`）：
- 端点：`GET/POST/PUT/DELETE /api/v1/app/workflows`
- 新增：`POST /api/v1/app/workflows/{id}/execute`（执行工作流）
- 类型：`WorkflowVO` / `WorkflowCreateRequest` / `WorkflowUpdateRequest` / `WorkflowExecutionRequest` / `WorkflowExecutionResult`

**页面结构**：

```
WorkflowList.vue
├── PageHeader（标题 + "创建工作流"按钮）
├── CiffTable（列表：名称/描述/状态/操作）
│   └── 操作列：编辑 / 执行 / 删除
└── CiffFormDialog
    ├── 基础信息：名称 / 描述 / 状态
    └── 步骤配置：JSON 编辑器（ Monaco Editor 或 Codemirror 轻量版）
```

**JSON 编辑器选型**：
- V1 不做可视化拖拽，使用 JSON 编辑器
- 推荐 `json-editor-vue3` 或原生 `textarea` + `JSON.parse` 实时校验
- 必须提供：语法高亮、格式错误提示、一键格式化

**执行弹窗**（`WorkflowExecutionDialog.vue`）：
- 输入全局变量（根据 definition.inputs 动态生成表单）
- 点击"执行"后显示步骤执行进度（轮询或一次性返回）
- 显示每步骤输出结果

---

### Step 5：前端 — 联调与测试

| 项 | 内容 |
|----|------|
| Task | 完整联调 + 前端单元测试 |
| Output | `WorkflowList.spec.ts` |
| Verification | Vitest 通过 + 手动配置并执行 3 个典型工作流 |

**联调场景**：
1. 简单 2 步骤：LLM 翻译 → 输出结果
2. 条件分支：LLM 情感分析 → condition（positive/negative/neutral）→ 不同回复
3. 工具调用：LLM 提取城市名 → 天气工具查询 → LLM 生成回复

---

## 四、依赖关系

```
Step 1 (Workflow CRUD)
    │
    ├──→ Step 2 (工作流执行引擎)
    │       │
    │       ├──→ Step 3 (聚合层 + 联调测试)
    │       │       │
    │       │       └──→ Step 5 (前端联调)
    │       │
    │       └──→ Step 5 (前端可与后端测试并行)
    │
    └──→ Step 4 (前端页面)
            │
            └──→ Step 5 (联调)
```

**并行机会**：
- Step 1 完成后，Step 2 和 Step 4 可并行开发
- Step 3 的测试用例编写可与 Step 2 开发并行（TDD）
- Step 4 前端页面开发可与 Step 2 引擎开发并行

---

## 五、验收标准（来自主计划）

1. `mvn test -pl ciff-workflow,ciff-app` 全部通过
2. definition JSON 校验拦截非法配置（缺少 type / 未知步骤类型 / 循环引用 / 步骤 id 重复）
3. 条件分支：等于 / 包含 / 大于 三种判断正常，未命中走默认分支
4. 不同工作流实例上下文变量不串扰，步骤输出正确映射为后续步骤输入
5. 前端 Vitest 通过；JSON 编辑器格式错误有明确提示
6. 手动配置并执行 3 个典型工作流，结果与预期一致
7. 5 步骤以内工作流执行 P95 < 15s（含 LLM 调用）；定义查询 P95 < 200ms

---

## 六、技术决策记录

| 决策 | 选择 | 原因 |
|------|------|------|
| 步骤类型 | `llm` / `tool` / `condition` | V1 范围，`knowledge_retrieval` 等 Phase 3 完成后扩展 |
| 工作流定义格式 | JSON（非 YAML） | 前端可直接用 JSON 编辑器，前后端解析成本低 |
| 执行方式 | 同步执行（非异步 Job） | V1 工作流为短流程（< 15s），同步更可控 |
| 变量引用语法 | `${stepId.output.field}` | 显式、可读、易于解析 |
| LLM 调用方式 | 复用 Phase 4 `ChatService` 非流式接口 | 工作流不需要 SSE 流式，复用现有能力 |
| 条件分支匹配 | 顺序匹配，第一个命中即跳转 | 简单直观，默认分支放最后 |
| 编辑器 | JSON 文本编辑器（非可视化拖拽） | V1 明确排除可视化编辑器，降低复杂度 |

---

## 七、风险与降级

| 风险 | 影响 | 降级方案 |
|------|------|----------|
| Phase 3 未完成，缺少 knowledge_retrieval | 工作流步骤类型不完整 | 标记为 TODO，V1 只支持 llm/tool/condition |
| 工作流步骤过多导致执行超时 | 用户体验差 | 引擎层面设置最大步骤数（如 20 步）和总超时（60s） |
| JSON 编辑器对用户不友好 | 配置门槛高 | 提供 3 个示例模板（翻译/分类/工具链），用户可复制修改 |
| 变量插值语法错误 | 执行失败 | 引擎执行前校验所有 `${}` 引用是否合法，前置拦截 |
| 循环引用检测遗漏 | 死循环 | 拓扑排序 + DFS 双重检测，发现环立即报错 |
| LLM 步骤超时阻塞整个工作流 | 执行卡住 | 每步骤独立超时（如 30s），超时后标记失败但继续后续步骤（如配置了降级） |

---

## 八、Phase 3 依赖处理建议

如果用户选择**方案 A**（推荐）：在 Phase 5 执行期间，`knowledge_retrieval` 步骤类型相关代码不实现，但预留扩展点：

1. `StepType` 枚举中不定义 `KNOWLEDGE_RETRIEVAL`，校验器拒绝该类型
2. `WorkflowEngine` 中 `StepExecutor` 为接口模式，后续新增 `KnowledgeRetrievalStepExecutor` 即可扩展
3. 前端 JSON 编辑器中不提示 `knowledge_retrieval` 类型，但在文档中标注"即将支持"

等 Phase 3 完成后，只需：
- 在 `StepType` 枚举中增加 `KNOWLEDGE_RETRIEVAL`
- 新增 `KnowledgeRetrievalStepExecutor` 实现
- 前端更新类型提示

无需改动引擎核心逻辑。

---

## 九、完成总结（待执行后填写）

**计划完成日期**: 待定

**交付内容**:
- `ciff-workflow`: Workflow CRUD + 执行引擎（llm / tool / condition）
- `ciff-app`: AppWorkflowController 聚合接口
- `ciff-web`: WorkflowList.vue + JSON 编辑器 + 执行弹窗
- 测试: 9 个后端测试 + 1 个前端测试