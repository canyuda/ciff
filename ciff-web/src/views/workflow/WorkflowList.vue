<template>
  <div class="page-container">
    <PageHeader title="工作流管理" description="JSON 配置的工作流，支持 LLM 调用、工具执行和条件分支">
      <el-button type="primary" @click="openCreateDialog()">
        <el-icon><Plus /></el-icon>创建工作流
      </el-button>
    </PageHeader>

    <div class="ciff-card">
      <CiffTable ref="tableRef" :columns="columns" :api="fetchWorkflows">
        <template #status="{ row }">
          <el-tag v-if="row.status === 'active'" type="success" size="small">启用</el-tag>
          <el-tag v-else-if="row.status === 'draft'" type="info" size="small">草稿</el-tag>
          <el-tag v-else type="danger" size="small">停用</el-tag>
        </template>

        <template #actions="{ row }">
          <div style="display: flex; gap: 8px">
            <el-button link type="primary" @click="openEditDialog(row.id)">编辑</el-button>
            <el-button link type="success" @click="openExecuteDialog(row)">执行</el-button>
            <el-button link type="warning" @click="openTaskListDialog(row)">历史任务</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </div>
        </template>
      </CiffTable>
    </div>

    <CiffFormDialog
      ref="dialogRef"
      title="工作流"
      width="800px"
      :rules="rules"
      :submit-handler="handleSubmit"
    >
      <template #default="{ data, isEdit }">
        <el-form-item label="名称" prop="name">
          <el-input v-model="data.name" placeholder="工作流名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="data.description" type="textarea" :rows="2" placeholder="工作流功能描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="data.status" placeholder="请选择状态">
            <el-option label="启用" value="active" />
            <el-option label="停用" value="inactive" />
            <el-option label="草稿" value="draft" />
          </el-select>
        </el-form-item>
        <el-form-item label="步骤定义" prop="definitionJson">
          <div class="definition-header">
            <div v-if="data._jsonError" class="json-error">{{ data._jsonError }}</div>
            <el-switch
              v-model="data._showChart"
              active-text="流程图"
              inactive-text="JSON"
              style="margin-left: auto"
            />
          </div>
          <el-input
            v-if="!data._showChart"
            v-model="data.definitionJson"
            type="textarea"
            :rows="16"
            placeholder="JSON 工作流定义"
            @input="onDefinitionInput(data)"
          />
          <div v-else class="mermaid-container">
            <div v-if="data._jsonError" class="mermaid-error">JSON 格式错误，无法生成流程图</div>
            <div v-else ref="mermaidContainerRef" class="mermaid-chart" v-html="mermaidSvg"></div>
          </div>
        </el-form-item>
      </template>
    </CiffFormDialog>

    <!-- Execute dialog -->
    <el-dialog v-model="executeVisible" title="执行工作流" width="600px" destroy-on-close>
      <div v-if="!submittedTask" class="execute-inputs">
        <div v-if="executeInputFields.length > 0">
          <p style="color: #666; margin-bottom: 12px">请输入工作流参数：</p>
          <el-form label-width="120px" label-position="right">
            <el-form-item
              v-for="field in executeInputFields"
              :key="field.key"
              :label="field.label"
            >
              <el-input
                v-model="executeInputValues[field.key]"
                :placeholder="field.description || field.key"
              />
            </el-form-item>
          </el-form>
        </div>
        <p v-else style="color: #999">该工作流无需输入参数，点击"执行"直接运行。</p>
      </div>

      <div v-else style="text-align: center; padding: 20px">
        <el-result
          icon="success"
          title="任务已提交"
          :sub-title="'任务ID: ' + submittedTask.taskId"
        />
      </div>

      <template #footer>
        <el-button @click="executeVisible = false">关闭</el-button>
        <el-button
          v-if="!submittedTask"
          type="primary"
          :loading="submitting"
          @click="handleSubmitExecute"
        >执行</el-button>
        <el-button
          v-else
          type="primary"
          @click="goToTaskDetail(submittedTask)"
        >查看任务详情</el-button>
      </template>
    </el-dialog>

    <!-- Task list dialog -->
    <el-dialog v-model="taskListVisible" title="历史任务" width="700px" destroy-on-close>
      <div v-if="taskList.length === 0" style="text-align: center; padding: 40px; color: #999">
        暂无执行记录
      </div>
      <div v-else class="task-list">
        <div v-for="task in taskList" :key="task.taskId" class="task-item" @click="openTaskDetailDialog(task)">
          <div class="task-item-main">
            <el-tag :type="statusTagType(task.status)" size="small">{{ statusLabel(task.status) }}</el-tag>
            <span class="task-item-id">{{ task.taskId.substring(0, 8) }}</span>
            <span v-if="task.currentStepName" class="task-item-step">
              {{ task.completedSteps }}/{{ task.totalSteps }} {{ task.currentStepName }}
            </span>
          </div>
          <div class="task-item-time">
            {{ task.startTime }}
          </div>
          <el-button link type="primary" size="small" @click.stop="openTaskDetailDialog(task)">详情</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- Task detail dialog -->
    <el-dialog v-model="taskDetailVisible" :title="'任务详情 — ' + (taskDetail?.taskId?.substring(0, 8) || '')" width="700px" destroy-on-close>
      <div v-if="taskDetailLoading" style="text-align: center; padding: 40px">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      </div>
      <div v-else-if="taskDetail" class="task-detail">
        <div class="task-detail-header">
          <el-tag :type="statusTagType(taskDetail.status)" size="default">{{ statusLabel(taskDetail.status) }}</el-tag>
          <span class="task-detail-progress">
            进度: {{ taskDetail.completedSteps }}/{{ taskDetail.totalSteps }}
          </span>
          <span v-if="taskDetail.currentStepName" class="task-detail-step">
            当前: {{ taskDetail.currentStepName }}
          </span>
        </div>

        <div v-if="taskDetail.inputs && Object.keys(taskDetail.inputs).length > 0" class="task-detail-section">
          <h4>执行参数</h4>
          <pre class="step-output">{{ JSON.stringify(taskDetail.inputs, null, 2) }}</pre>
        </div>

        <div v-if="isTaskRunning" class="task-detail-refresh">
          <el-switch v-model="autoRefresh" active-text="自动刷新" />
          <span v-if="autoRefresh" style="margin-left: 8px; color: #999; font-size: 12px">
            间隔 <el-input-number v-model="refreshInterval" :min="1" :max="30" size="small" style="width: 80px; margin: 0 4px" /> 秒
          </span>
        </div>

        <div v-if="taskDetail.finalOutputs && Object.keys(taskDetail.finalOutputs).length > 0" class="final-output">
          <h4 style="margin: 0 0 8px; color: #333">最终结果</h4>
          <pre class="step-output final-output-content">{{ formatFinalOutput(taskDetail.finalOutputs) }}</pre>
        </div>

        <div v-if="taskDetail.stepResults && taskDetail.stepResults.length > 0" class="task-detail-section">
          <h4>步骤执行详情</h4>
          <div v-for="step in taskDetail.stepResults" :key="step.stepId" class="step-result">
            <div class="step-header">
              <el-tag size="small" :type="step.success ? 'success' : 'danger'">
                {{ step.stepName }}
              </el-tag>
              <span class="step-type">{{ step.type }}</span>
            </div>
            <pre v-if="step.outputs" class="step-output">{{ formatStepOutput(step) }}</pre>
            <div v-if="step.error" class="step-error">{{ step.error }}</div>
          </div>
        </div>

        <div v-if="taskDetail.error" class="task-detail-section">
          <el-alert type="error" :title="taskDetail.error" :closable="false" />
        </div>

        <div class="task-detail-time">
          <span>开始: {{ taskDetail.startTime }}</span>
          <span v-if="taskDetail.endTime"> | 结束: {{ taskDetail.endTime }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted, nextTick } from 'vue'
import { Plus, Loading } from '@element-plus/icons-vue'
import CiffTable from '@/components/CiffTable.vue'
import CiffFormDialog from '@/components/CiffFormDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess } from '@/utils/notify'
import { toMermaid } from '@/utils/workflow-mermaid'
import {
  getWorkflows,
  getWorkflowById,
  createWorkflow,
  updateWorkflow,
  deleteWorkflow,
  submitWorkflow,
  getWorkflowTasks,
  getWorkflowTaskDetail,
} from '@/api/workflow'
import type { TableColumn, PageParams } from '@/types/common'
import type { FormRules, FormItemRule } from 'element-plus'
import type { WorkflowVO, StepResult, WorkflowTask, WorkflowTaskDetail, TaskStatus, WorkflowDefinition } from '@/api/workflow'
import mermaid from 'mermaid'

mermaid.initialize({ startOnLoad: false, theme: 'default' })

interface TableRef {
  refresh: () => void
}

interface DialogRef {
  open: (data?: Partial<WorkflowForm>) => void
}

interface WorkflowForm {
  id?: number
  name: string
  description?: string
  status: string
  definitionJson: string
  _jsonError?: string
  _showChart?: boolean
  [key: string]: unknown
}

interface InputField {
  key: string
  label: string
  description?: string
}

// workflow CRUD
const tableRef = ref<TableRef | null>(null)
const dialogRef = ref<DialogRef | null>(null)
const columns: TableColumn[] = [
  { label: '名称', prop: 'name', minWidth: 140 },
  { label: '描述', prop: 'description', minWidth: 200 },
  { label: '状态', slot: 'status', width: 80, align: 'center' },
  { label: '创建时间', prop: 'createTime', width: 170 },
  { label: '操作', slot: 'actions', minWidth: 240, fixed: 'right' },
]
const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  definitionJson: [{
    validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
      if (!value) {
        callback(new Error('步骤定义不能为空'))
        return
      }
      try {
        const parsed = JSON.parse(value)
        if (!parsed.steps || !Array.isArray(parsed.steps) || parsed.steps.length === 0) {
          callback(new Error('steps 不能为空'))
          return
        }
        callback()
      } catch (e) {
        callback(new Error('JSON 格式错误: ' + (e as Error).message))
      }
    },
    trigger: 'blur',
  }],
}

async function fetchWorkflows(params: PageParams) {
  return getWorkflows({ page: params.page, pageSize: params.pageSize })
}

// mermaid rendering
const mermaidContainerRef = ref<HTMLElement | null>(null)
const mermaidSvg = ref('')

async function renderMermaid(jsonStr: string): Promise<boolean> {
  if (!jsonStr) {
    mermaidSvg.value = ''
    return false
  }
  try {
    const definition: WorkflowDefinition = JSON.parse(jsonStr)
    const graphDef = toMermaid(definition)
    const { svg } = await mermaid.render('workflow-chart', graphDef)
    mermaidSvg.value = svg
    return true
  } catch {
    mermaidSvg.value = ''
    return false
  }
}

function onDefinitionInput(data: WorkflowForm) {
  if (!data.definitionJson) {
    data._jsonError = ''
  } else {
    try {
      JSON.parse(data.definitionJson)
      data._jsonError = ''
    } catch (e) {
      data._jsonError = 'JSON 格式错误: ' + (e as Error).message
    }
  }
  renderMermaid(data.definitionJson).then((ok) => {
    if (!ok) data._showChart = false
  })
}

async function openCreateDialog() {
  const json = JSON.stringify({
    steps: [
      { id: 'step1', type: 'llm', name: '步骤1', config: {}, outputs: {} },
    ],
    inputs: {},
  }, null, 2)
  const showChart = await renderMermaid(json)
  dialogRef.value?.open({
    status: 'draft',
    definitionJson: json,
    _showChart: showChart,
  } as Partial<WorkflowForm>)
}

async function openEditDialog(id?: number) {
  if (!id) return
  const detail = await getWorkflowById(id)
  const json = detail.definition ? JSON.stringify(detail.definition, null, 2) : ''
  const showChart = await renderMermaid(json)
  dialogRef.value?.open({
    id: detail.id,
    name: detail.name,
    description: detail.description,
    status: detail.status,
    definitionJson: json,
    _showChart: showChart,
  })
}

const { confirm } = useConfirm()

async function handleDelete(id?: number) {
  if (!id) return
  await confirm('确定要删除该工作流吗？删除后不可恢复。', async () => {
    await deleteWorkflow(id)
  })
  tableRef.value?.refresh()
}

async function handleSubmit(form: WorkflowForm) {
  let definition
  if (form.definitionJson) {
    try {
      definition = JSON.parse(form.definitionJson)
    } catch {
      return
    }
  }

  if (form.id) {
    const req: Record<string, unknown> = {
      name: form.name,
      description: form.description,
      status: form.status,
    }
    if (definition) req.definition = definition
    await updateWorkflow(form.id, req)
    notifySuccess('更新成功')
  } else {
    await createWorkflow({
      name: form.name,
      description: form.description,
      definition,
      status: form.status,
    })
    notifySuccess('创建成功')
  }
  tableRef.value?.refresh()
}

// execute workflow
const executeVisible = ref(false)
const submitting = ref(false)
const submittedTask = ref<WorkflowTask | null>(null)
const currentWorkflowId = ref<number | null>(null)
const currentWorkflowDef = ref<Record<string, unknown> | null>(null)
const executeInputValues = ref<Record<string, string>>({})

const executeInputFields = computed<InputField[]>(() => {
  const inputs = currentWorkflowDef.value?.inputs
  if (!inputs || typeof inputs !== 'object') return []
  return Object.entries(inputs).map(([key, val]) => ({
    key,
    label: (val as any)?.description || key,
    description: (val as any)?.type ? `类型: ${(val as any).type}` : undefined,
  }))
})

function openExecuteDialog(row: WorkflowVO) {
  currentWorkflowId.value = row.id ?? null
  currentWorkflowDef.value = (row.definition as Record<string, unknown>) ?? null
  executeInputValues.value = {}
  submittedTask.value = null
  submitting.value = false
  executeVisible.value = true
}

async function handleSubmitExecute() {
  if (!currentWorkflowId.value) return
  submitting.value = true
  try {
    const inputs: Record<string, unknown> = {}
    for (const [key, val] of Object.entries(executeInputValues.value)) {
      if (val === '') continue
      const num = Number(val)
      inputs[key] = isNaN(num) ? val : num
    }
    submittedTask.value = await submitWorkflow(currentWorkflowId.value, inputs)
    notifySuccess('任务已提交')
  } finally {
    submitting.value = false
  }
}

function goToTaskDetail(task: WorkflowTask) {
  executeVisible.value = false
  openTaskDetailDialog(task)
}

// task list
const taskListVisible = ref(false)
const taskList = ref<WorkflowTask[]>([])
const taskListWorkflowId = ref<number | null>(null)

async function openTaskListDialog(row: WorkflowVO) {
  taskListWorkflowId.value = row.id ?? null
  taskList.value = []
  taskListVisible.value = true
  taskList.value = await getWorkflowTasks(row.id!)
}

// task detail
const taskDetailVisible = ref(false)
const taskDetailLoading = ref(false)
const taskDetail = ref<WorkflowTaskDetail | null>(null)
const autoRefresh = ref(false)
const refreshInterval = ref(3)
let refreshTimer: ReturnType<typeof setInterval> | null = null

const isTaskRunning = computed(() => {
  if (!taskDetail.value) return false
  return taskDetail.value.status === 'STARTED' || taskDetail.value.status === 'RUNNING'
})

async function openTaskDetailDialog(task: WorkflowTask) {
  if (!taskListWorkflowId.value) return
  taskDetail.value = null
  autoRefresh.value = false
  taskDetailVisible.value = true
  await loadTaskDetail(task.taskId)
  // auto-enable refresh for running tasks
  if (taskDetail.value && (taskDetail.value.status === 'STARTED' || taskDetail.value.status === 'RUNNING')) {
    autoRefresh.value = true
  }
}

async function loadTaskDetail(taskId: string) {
  if (!taskListWorkflowId.value) return
  taskDetailLoading.value = true
  try {
    taskDetail.value = await getWorkflowTaskDetail(taskListWorkflowId.value, taskId)
  } finally {
    taskDetailLoading.value = false
  }
}

watch(autoRefresh, (on) => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  if (on && taskDetail.value) {
    refreshTimer = setInterval(async () => {
      if (taskDetail.value && isTaskRunning.value) {
        await loadTaskDetail(taskDetail.value.taskId)
      } else {
        autoRefresh.value = false
      }
    }, refreshInterval.value * 1000)
  }
})

watch(refreshInterval, () => {
  if (autoRefresh.value && taskDetail.value) {
    // restart timer with new interval
    autoRefresh.value = false
    setTimeout(() => { autoRefresh.value = true }, 50)
  }
})

watch(taskDetailVisible, (visible) => {
  if (!visible) {
    autoRefresh.value = false
  }
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})

// status helpers
function statusLabel(status: TaskStatus): string {
  const map: Record<TaskStatus, string> = {
    STARTED: '已启动',
    RUNNING: '执行中',
    SUCCESS: '成功',
    FAILED: '失败',
    TIMEOUT: '超时',
  }
  return map[status] || status
}

function statusTagType(status: TaskStatus): string {
  const map: Record<TaskStatus, string> = {
    STARTED: 'info',
    RUNNING: 'warning',
    SUCCESS: 'success',
    FAILED: 'danger',
    TIMEOUT: 'danger',
  }
  return map[status] || 'info'
}

// formatting helpers
function formatFinalOutput(outputs: Record<string, unknown>): string {
  const reply = outputs.finalReply ?? outputs.result ?? outputs.rawContent
  if (typeof reply === 'string') return reply
  return JSON.stringify(outputs, null, 2)
}

function formatStepOutput(step: StepResult): string {
  const outputs = step.outputs
  if (!outputs) return ''

  if (step.type === 'llm' && outputs.rawContent && typeof outputs.rawContent === 'string') {
    return outputs.rawContent
  }

  if (step.type === 'tool') {
    const data = outputs.weatherData ?? outputs.result
    if (data && typeof data === 'object') {
      return JSON.stringify(data, null, 2)
    }
  }

  const display: Record<string, unknown> = {}
  for (const [k, v] of Object.entries(outputs)) {
    if (k !== 'rawContent' && k !== 'result') {
      display[k] = v
    }
  }
  return JSON.stringify(display, null, 2)
}
</script>

<style scoped>
.page-container {
  max-width: 1600px;
}

.json-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}

.definition-header {
  display: flex;
  align-items: center;
  width: 100%;
  margin-bottom: 4px;
}

.mermaid-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 16px;
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  overflow: auto;
}

.mermaid-chart {
  width: 100%;
  overflow: auto;
}

.mermaid-chart :deep(svg) {
  max-width: 100%;
  height: auto;
}

.mermaid-error {
  color: #f56c6c;
  font-size: 14px;
}

/* task list */
.task-list {
  max-height: 500px;
  overflow-y: auto;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.task-item:hover {
  background: #f5f7fa;
}

.task-item-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.task-item-id {
  color: #606266;
  font-size: 13px;
  font-family: monospace;
}

.task-item-step {
  color: #909399;
  font-size: 12px;
}

.task-item-time {
  color: #909399;
  font-size: 12px;
}

/* task detail */
.task-detail {
  max-height: 600px;
  overflow-y: auto;
}

.task-detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.task-detail-progress {
  color: #606266;
  font-size: 14px;
}

.task-detail-step {
  color: #909399;
  font-size: 13px;
}

.task-detail-refresh {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #fdf6ec;
  border-radius: 4px;
}

.task-detail-section {
  margin-bottom: 16px;
}

.task-detail-section h4 {
  margin: 0 0 8px;
  color: #333;
  font-size: 14px;
}

.task-detail-time {
  color: #909399;
  font-size: 12px;
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
}

/* step results */
.step-result {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 8px;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.step-type {
  color: #909399;
  font-size: 12px;
}

.step-output {
  background: #f5f7fa;
  color: #333;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.step-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}

.final-output {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 16px;
}

.final-output-content {
  background: #fff;
  color: #333;
  border: none;
  padding: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.6;
}
</style>
