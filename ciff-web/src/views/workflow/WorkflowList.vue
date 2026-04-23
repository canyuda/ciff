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
          <el-input
            v-model="data.definitionJson"
            type="textarea"
            :rows="16"
            placeholder="JSON 工作流定义"
            @blur="validateJson(data)"
          />
          <div v-if="data._jsonError" class="json-error">{{ data._jsonError }}</div>
        </el-form-item>
      </template>
    </CiffFormDialog>

    <el-dialog v-model="executeVisible" title="执行工作流" width="600px" destroy-on-close>
      <!-- Input params form (before execution) -->
      <div v-if="!executeResult && !executing" class="execute-inputs">
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

      <!-- Executing spinner -->
      <div v-else-if="executing" style="text-align: center; padding: 40px">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>正在执行工作流...</p>
      </div>

      <!-- Execution result -->
      <div v-else class="execute-result">
        <el-alert
          :title="executeResult.success ? '执行成功' : '执行失败'"
          :type="executeResult.success ? 'success' : 'error'"
          :description="executeResult.error"
          show-icon
          :closable="false"
          style="margin-bottom: 16px"
        />

        <!-- Final output (highlighted) -->
        <div v-if="executeResult.finalOutputs && Object.keys(executeResult.finalOutputs).length > 0" class="final-output">
          <h4 style="margin: 0 0 8px">最终结果</h4>
          <pre class="step-output final-output-content">{{ formatFinalOutput(executeResult.finalOutputs) }}</pre>
        </div>

        <!-- Step details (collapsible) -->
        <el-collapse style="margin-top: 12px">
          <el-collapse-item title="步骤执行详情">
            <div v-for="(step, key) in executeResult.stepResults" :key="key" class="step-result">
              <div class="step-header">
                <el-tag size="small" :type="step.success ? 'success' : 'danger'">
                  {{ step.stepName }}
                </el-tag>
                <span class="step-type">{{ step.type }}</span>
              </div>
              <pre v-if="step.outputs" class="step-output">{{ JSON.stringify(step.outputs, null, 2) }}</pre>
              <div v-if="step.error" class="step-error">{{ step.error }}</div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <template #footer>
        <el-button @click="executeVisible = false">关闭</el-button>
        <el-button
          v-if="!executeResult && !executing"
          type="primary"
          :loading="executing"
          @click="handleExecute"
        >执行</el-button>
        <el-button
          v-else-if="executeResult"
          type="primary"
          @click="resetExecute"
        >重新执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Plus, Loading } from '@element-plus/icons-vue'
import CiffTable from '@/components/CiffTable.vue'
import CiffFormDialog from '@/components/CiffFormDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess } from '@/utils/notify'
import {
  getWorkflows,
  getWorkflowById,
  createWorkflow,
  updateWorkflow,
  deleteWorkflow,
  executeWorkflow,
} from '@/api/workflow'
import type { TableColumn, PageParams } from '@/types/common'
import type { FormRules } from 'element-plus'
import type { WorkflowVO, WorkflowExecutionResult } from '@/api/workflow'

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
  [key: string]: unknown
}

const tableRef = ref<TableRef | null>(null)
const dialogRef = ref<DialogRef | null>(null)
const executeVisible = ref(false)
const executing = ref(false)
const executeResult = ref<WorkflowExecutionResult | null>(null)
const currentWorkflowId = ref<number | null>(null)
const currentWorkflowDef = ref<Record<string, unknown> | null>(null)
const executeInputValues = ref<Record<string, string>>({})

interface InputField {
  key: string
  label: string
  description?: string
}

const executeInputFields = computed<InputField[]>(() => {
  const inputs = currentWorkflowDef.value?.inputs
  if (!inputs || typeof inputs !== 'object') return []
  return Object.entries(inputs).map(([key, val]) => ({
    key,
    label: (val as any)?.description || key,
    description: (val as any)?.type ? `类型: ${(val as any).type}` : undefined,
  }))
})

const columns: TableColumn[] = [
  { label: '名称', prop: 'name', minWidth: 140 },
  { label: '描述', prop: 'description', minWidth: 200 },
  { label: '状态', slot: 'status', width: 80, align: 'center' },
  { label: '创建时间', prop: 'createTime', width: 170 },
  { label: '操作', slot: 'actions', minWidth: 180, fixed: 'right' },
]

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

async function fetchWorkflows(params: PageParams) {
  return getWorkflows({ page: params.page, pageSize: params.pageSize })
}

function validateJson(data: WorkflowForm) {
  if (!data.definitionJson) {
    data._jsonError = ''
    return
  }
  try {
    JSON.parse(data.definitionJson)
    data._jsonError = ''
  } catch (e) {
    data._jsonError = 'JSON 格式错误: ' + (e as Error).message
  }
}

function openCreateDialog() {
  dialogRef.value?.open({
    status: 'draft',
    definitionJson: JSON.stringify({
      steps: [
        { id: 'step1', type: 'llm', name: '步骤1', config: {}, outputs: {} },
      ],
      inputs: {},
    }, null, 2),
  } as Partial<WorkflowForm>)
}

async function openEditDialog(id?: number) {
  if (!id) return
  const detail = await getWorkflowById(id)
  dialogRef.value?.open({
    id: detail.id,
    name: detail.name,
    description: detail.description,
    status: detail.status,
    definitionJson: detail.definition ? JSON.stringify(detail.definition, null, 2) : '',
  })
}

function openExecuteDialog(row: WorkflowVO) {
  currentWorkflowId.value = row.id ?? null
  currentWorkflowDef.value = (row.definition as Record<string, unknown>) ?? null
  executeInputValues.value = {}
  executeResult.value = null
  executing.value = false
  executeVisible.value = true
}

function resetExecute() {
  executeResult.value = null
  executeInputValues.value = {}
}

async function handleExecute() {
  if (!currentWorkflowId.value) return
  executing.value = true
  executeResult.value = null
  try {
    // convert string inputs to actual values (try number, fallback string)
    const inputs: Record<string, unknown> = {}
    for (const [key, val] of Object.entries(executeInputValues.value)) {
      if (val === '') continue
      const num = Number(val)
      inputs[key] = isNaN(num) ? val : num
    }
    executeResult.value = await executeWorkflow(currentWorkflowId.value, inputs)
  } finally {
    executing.value = false
  }
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

function formatFinalOutput(outputs: Record<string, unknown>): string {
  // if there's a "finalReply" key (from LLM steps), show it directly
  const reply = outputs.finalReply ?? outputs.result
  if (typeof reply === 'string') return reply
  return JSON.stringify(outputs, null, 2)
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

.execute-result {
  max-height: 500px;
  overflow-y: auto;
}

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
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
  margin: 0;
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
}

.final-output-content {
  background: #fff;
  border: none;
  padding: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.6;
}
</style>
