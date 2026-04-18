<template>
  <div class="page-container">
    <PageHeader title="Agent 管理" description="创建和管理 AI Agent 智能助手，配置模型、工具和提示词">
      <el-button type="primary" @click="dialogRef?.open()">
        <el-icon><Plus /></el-icon>创建 Agent
      </el-button>
    </PageHeader>

    <div class="ciff-card">
      <CiffTable ref="tableRef" :columns="columns" :api="fetchAgents">
        <template #type="{ row }">
          <el-tag v-if="row.type === 'chatbot'" type="primary" size="small" effect="plain">Chatbot</el-tag>
          <el-tag v-else-if="row.type === 'agent'" type="success" size="small" effect="plain">Agent</el-tag>
          <el-tag v-else-if="row.type === 'workflow'" type="warning" size="small" effect="plain">Workflow</el-tag>
          <el-tag v-else size="small">{{ row.type }}</el-tag>
        </template>

        <template #status="{ row }">
          <el-tag v-if="row.status === 'active'" type="success" size="small">启用</el-tag>
          <el-tag v-else-if="row.status === 'draft'" type="info" size="small">草稿</el-tag>
          <el-tag v-else type="danger" size="small">停用</el-tag>
        </template>

        <template #tools="{ row }">
          <span>{{ row.tools?.length ?? 0 }}</span>
        </template>

        <template #actions="{ row }">
          <div style="display: flex; gap: 8px">
            <el-button link type="primary" @click="dialogRef?.open(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </div>
        </template>
      </CiffTable>
    </div>

    <CiffFormDialog
      ref="dialogRef"
      title="Agent"
      width="720px"
      :rules="rules"
      :submit-handler="handleSubmit"
    >
      <template #default="{ data, isEdit }">
        <el-form-item label="名称" prop="name">
          <el-input v-model="data.name" placeholder="例如：客服助手" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="data.type" placeholder="请选择 Agent 类型" style="width: 100%" :disabled="isEdit">
            <el-option label="Chatbot（纯对话）" value="chatbot" />
            <el-option label="Agent（工具调用）" value="agent" />
            <el-option label="Workflow（工作流）" value="workflow" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型" prop="modelId">
          <el-select v-model="data.modelId" placeholder="请选择模型" style="width: 100%">
            <el-option
              v-for="m in modelOptions"
              :key="m.id"
              :label="m.displayName || m.name"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="系统提示词" prop="systemPrompt">
          <el-input
            v-model="data.systemPrompt"
            type="textarea"
            :rows="4"
            placeholder="You are a helpful assistant..."
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="data.description" placeholder="Agent 功能描述" />
        </el-form-item>
        <el-form-item label="绑定工具" prop="toolIds">
          <el-select v-model="data.toolIds" multiple placeholder="选择工具" style="width: 100%">
            <el-option
              v-for="t in toolOptions"
              :key="t.id"
              :label="t.name"
              :value="t.id"
            >
              <span>{{ t.name }}</span>
              <el-tag size="small" style="margin-left: 8px" :type="t.type === 'mcp' ? 'success' : 'primary'">
                {{ t.type?.toUpperCase() }}
              </el-tag>
            </el-option>
          </el-select>
        </el-form-item>
      </template>
    </CiffFormDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import CiffTable from '@/components/CiffTable.vue'
import CiffFormDialog from '@/components/CiffFormDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess } from '@/utils/notify'
import {
  getAgents,
  createAgent,
  updateAgent,
  deleteAgent,
  type AgentVO,
  type AgentCreateRequest,
  type AgentUpdateRequest,
} from '@/api/agent'
import { getModels, type ModelVO } from '@/api/model'
import { getTools, type ToolVO } from '@/api/tool'
import type { TableColumn, PageParams } from '@/types/common'
import type { FormRules } from 'element-plus'

interface TableRef {
  refresh: () => void
}

interface DialogRef {
  open: (data?: Partial<AgentForm>) => void
}

interface AgentForm {
  id?: number
  name: string
  description?: string
  type: string
  modelId?: number
  systemPrompt: string
  toolIds?: number[]
}

const tableRef = ref<TableRef | null>(null)
const dialogRef = ref<DialogRef | null>(null)
const modelOptions = ref<ModelVO[]>([])
const toolOptions = ref<ToolVO[]>([])

const columns: TableColumn[] = [
  { label: '名称', prop: 'name', minWidth: 140 },
  { label: '类型', slot: 'type', width: 110, align: 'center' },
  { label: '模型', prop: 'modelName', minWidth: 120 },
  { label: '工具数', slot: 'tools', width: 80, align: 'center' },
  { label: '状态', slot: 'status', width: 80, align: 'center' },
  { label: '创建时间', prop: 'createTime', width: 170 },
  { label: '操作', slot: 'actions', minWidth: 130, fixed: 'right' },
]

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  modelId: [{ required: true, message: '请选择模型', trigger: 'change' }],
  systemPrompt: [{ required: true, message: '请输入系统提示词', trigger: 'blur' }],
}

onMounted(async () => {
  await loadOptions()
})

async function loadOptions() {
  try {
    const [modelsRes, toolsRes] = await Promise.all([
      getModels({ page: 1, pageSize: 100 }),
      getTools({ page: 1, pageSize: 100 }),
    ])
    modelOptions.value = modelsRes.list
    toolOptions.value = toolsRes.list
  } catch {
    // options load failure should not block page
  }
}

async function fetchAgents(params: PageParams) {
  return getAgents({ page: params.page, pageSize: params.pageSize })
}

async function handleSubmit(form: AgentForm) {
  if (form.id) {
    const payload: AgentUpdateRequest = {
      name: form.name,
      description: form.description,
      type: form.type,
      modelId: form.modelId,
      systemPrompt: form.systemPrompt,
      toolIds: form.toolIds,
    }
    await updateAgent(form.id, payload)
    notifySuccess('更新成功')
  } else {
    const payload: AgentCreateRequest = {
      name: form.name,
      description: form.description,
      type: form.type,
      modelId: form.modelId!,
      systemPrompt: form.systemPrompt,
      toolIds: form.toolIds,
    }
    await createAgent(payload)
    notifySuccess('创建成功')
  }
  tableRef.value?.refresh()
}

const { confirm } = useConfirm()

async function handleDelete(id?: number) {
  if (!id) return
  await confirm('确定要删除该 Agent 吗？删除后不可恢复。', async () => {
    await deleteAgent(id)
  })
  tableRef.value?.refresh()
}
</script>

<style scoped>
.page-container {
  max-width: 1600px;
}
</style>
