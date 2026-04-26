<template>
  <div class="page-container">
    <PageHeader title="供应商管理" description="配置模型供应商及其接入参数，支持 OpenAI、Claude 等">
      <el-button type="primary" @click="dialogRef?.open()">
        <el-icon><Plus /></el-icon>新增供应商
      </el-button>
    </PageHeader>

    <div class="ciff-card">
      <CiffTable ref="tableRef" :columns="columns" :api="fetchProviders">
        <template #type="{ row }">
          <el-tag size="small" effect="dark" :color="typeColorMap[row.type]" style="border: none">
            {{ providerNameMap[row.type] || row.type }}
          </el-tag>
        </template>

        <template #authType="{ row }">
          <el-tag size="small" type="info">{{ authTypeMap[row.authType] || row.authType }}</el-tag>
        </template>

        <template #status="{ row }">
          <el-tag v-if="row.status === 'active'" type="success" size="small">启用</el-tag>
          <el-tag v-else type="info" size="small">禁用</el-tag>
        </template>

        <template #health="{ row }">
          <div class="health-cell">
            <span class="health-dot" :class="healthDotClass(row.health?.status)"></span>
            <span
              v-if="row.health?.status === 'UP' && row.health?.lastLatencyMs != null"
              :class="['health-label', row.health.lastLatencyMs < 2000 ? 'health-low' : 'health-high']"
            >
              {{ row.health.lastLatencyMs }} ms
            </span>
            <span v-else-if="row.health?.status === 'DOWN'" class="health-label health-down">异常</span>
            <span v-else class="health-label health-unknown">未知</span>
          </div>
        </template>

        <template #modelCount="{ row }">
          <span>{{ Array.isArray(row.models) ? row.models.length : 0 }}</span>
        </template>

        <template #actions="{ row }">
          <div style="display: flex; gap: 8px">
            <el-button link type="primary" @click="handleOpenModels(row)">模型管理</el-button>
            <el-button link type="primary" @click="openEditDialog(row.id)">编辑</el-button>
            <el-dropdown trigger="click">
              <el-button link type="primary">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleTest(row)">连通测试</el-dropdown-item>
                  <el-dropdown-item @click="handleDelete(row.id)" style="color: var(--el-color-danger)">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </CiffTable>
    </div>

    <CiffFormDialog
      ref="dialogRef"
      title="供应商"
      width="640px"
      :rules="rules"
      :submit-handler="handleSubmit"
    >
      <template #default="{ data, isEdit }">
        <el-form-item label="名称" prop="name">
          <el-input v-model="data.name" placeholder="例如：OpenAI Production" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select
            v-model="data.type"
            placeholder="请选择供应商类型"
            style="width: 100%"
            :disabled="isEdit"
            @change="(val: string) => { data.authType = resolveAuthType(val); data.apiBaseUrl = '' }"
          >
            <el-option v-for="t in providerTypes" :key="t" :label="providerNameMap[t] || t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="认证方式" prop="authType">
          <el-input :model-value="authTypeMap[data.authType] || data.authType || ''" disabled style="width: 100%" />
        </el-form-item>
        <el-form-item label="Base URL" prop="apiBaseUrl">
          <el-input v-model="data.apiBaseUrl" :placeholder="baseUrlPlaceholder[data.type] || 'https://'">
            <template #append>
              <el-button :disabled="!data.type" @click="data.apiBaseUrl = baseUrlPlaceholder[data.type] || ''">
                填入
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input
            v-model="data.apiKey"
            type="password"
            show-password
            :placeholder="isEdit ? '留空表示不修改' : 'sk-...'"
          />
        </el-form-item>
      </template>
    </CiffFormDialog>

    <el-dialog v-model="modelDialogVisible" :title="`${currentProvider?.name || ''} - 模型管理`" width="840px" destroy-on-close>
      <div style="display: flex; justify-content: flex-end; margin-bottom: 12px">
        <el-button type="primary" @click="openAddModelDialog">
          <el-icon><Plus /></el-icon>新增模型
        </el-button>
      </div>
      <CiffTable ref="modelTableRef" :columns="modelColumns" :api="fetchModelsForProvider">
        <template #status="{ row }">
          <el-tag v-if="row.status === 'active'" type="success" size="small">启用</el-tag>
          <el-tag v-else type="info" size="small">禁用</el-tag>
        </template>
        <template #actions="{ row }">
          <div style="display: flex; gap: 8px">
            <el-button link type="primary" @click="openEditModelDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDeleteModel(row.id)">删除</el-button>
          </div>
        </template>
      </CiffTable>
    </el-dialog>

    <CiffFormDialog
      ref="modelFormDialogRef"
      title="模型"
      width="560px"
      :rules="modelRules"
      :submit-handler="handleModelSubmit"
    >
      <template #default="{ data, isEdit: _isEdit }">
        <el-form-item label="供应商" prop="providerId">
          <el-select v-model="data.providerId" placeholder="请选择供应商" style="width: 100%" disabled>
            <el-option :label="currentProvider?.name" :value="currentProvider?.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="data.name" placeholder="例如：gpt-4o" />
        </el-form-item>
        <el-form-item label="显示名称" prop="displayName">
          <el-input v-model="data.displayName" placeholder="例如：GPT-4o" />
        </el-form-item>
        <el-form-item label="Max Tokens" prop="maxTokens">
          <el-input-number v-model="data.maxTokens" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="默认参数" prop="defaultParamsText">
          <el-input
            v-model="data.defaultParamsText"
            type="textarea"
            :rows="4"
            placeholder='{"temperature":0.7,"topP":1}'
          />
        </el-form-item>
      </template>
    </CiffFormDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import CiffTable from '@/components/CiffTable.vue'
import CiffFormDialog from '@/components/CiffFormDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess } from '@/utils/notify'
import {
  getProviders,
  getProviderById,
  createProvider,
  updateProvider,
  deleteProvider,
  testProvider,
  getProviderTypes,
  type Provider,
  type ProviderCreateRequest,
  type ProviderUpdateRequest,
} from '@/api/provider'
import {
  getModels,
  createModel,
  updateModel,
  deleteModel,
  type ModelVO,
  type ModelCreateRequest,
  type ModelUpdateRequest,
} from '@/api/model'
import type { TableColumn, PageParams } from '@/types/common'
import type { FormRules } from 'element-plus'

// ---- Types ----

interface TableRef {
  refresh: () => void
}

interface DialogRef {
  open: (data?: Partial<Provider>) => void
}

interface ModelTableRef {
  refresh: () => void
}

interface ModelFormDialogRef {
  open: (data?: Partial<ModelForm>) => void
}

interface ModelForm {
  id?: number
  providerId: number
  name: string
  displayName?: string
  maxTokens?: number
  defaultParamsText?: string
}

// ---- Constants ----

const providerTypes = ref<string[]>([])
const providerNameMap = reactive<Record<string, string>>({})

async function loadProviderTypes() {
  try {
    const types = await getProviderTypes()
    providerTypes.value = types.map(t => t.type)
    types.forEach(t => {
      providerNameMap[t.type] = t.displayName
    })
  } catch {
    providerTypes.value = []
  }
}

onMounted(() => {
  loadProviderTypes()
})

const typeColorMap: Record<string, string> = {
  openai:    'rgb(16, 163, 127)',
  claude:    'rgb(204, 120, 50)',
  gemini:    'rgb(66, 133, 244)',
  ollama:    'rgb(118, 100, 236)',
  deepseek:  'rgb(68, 122, 255)',
  qwen:      'rgb(255, 106, 0)',
  zhipu:     'rgb(54, 179, 126)',
  kimi:      'rgb(30, 150, 219)',
  wenxin:    'rgb(36, 104, 242)',
  doubao:    'rgb(248, 100, 42)',
  hunyuan:   'rgb(0, 170, 255)',
  yi:        'rgb(70, 100, 255)',
  minimax:   'rgb(140, 80, 255)',
  spark:     'rgb(228, 56, 80)',
}

const authTypeMap: Record<string, string> = {
  bearer: 'Bearer Token',
  api_key_header: 'API Key Header',
  url: 'URL 参数',
  jwt: 'JWT',
  dual_key: '双密钥',
  none: '无认证',
}

const baseUrlPlaceholder: Record<string, string> = {
  openai:    'https://api.openai.com/v1',
  claude:    'https://api.anthropic.com',
  gemini:    'https://generativelanguage.googleapis.com/v1beta/openai/',
  ollama:    'http://localhost:11434',
  deepseek:  'https://api.deepseek.com',
  qwen:      'https://dashscope.aliyuncs.com/compatible-mode/v1',
  zhipu:     'https://open.bigmodel.cn/api/paas/v4',
  kimi:      'https://api.moonshot.cn/v1',
  wenxin:    'https://qianfan.baidubce.com/v2',
  doubao:    'https://ark.cn-beijing.volces.com/api/v3',
  hunyuan:   'https://hunyuan.tencentcloudapi.com',
  yi:        'https://api.lingyiwanwu.com/v1',
  minimax:   'https://api.minimax.chat',
  spark:     'https://spark-api-open.xf-yun.com/v1',
}

// ---- Table config ----

const tableRef = ref<TableRef | null>(null)
const dialogRef = ref<DialogRef | null>(null)

const modelDialogVisible = ref(false)
const currentProvider = ref<Provider | null>(null)
const modelTableRef = ref<ModelTableRef | null>(null)
const modelFormDialogRef = ref<ModelFormDialogRef | null>(null)

const columns: TableColumn[] = [
  { label: '名称', prop: 'name', minWidth: 160 },
  { label: '类型', slot: 'type', width: 110, align: 'center' },
  { label: '认证方式', slot: 'authType', width: 140, align: 'center' },
  { label: 'Base URL', prop: 'apiBaseUrl', minWidth: 240 },
  { label: '状态', slot: 'status', width: 80, align: 'center' },
  { label: '健康状态', slot: 'health', width: 130, align: 'center' },
  { label: '模型数', slot: 'modelCount', width: 80, align: 'center' },
  { label: '创建时间', prop: 'createTime', width: 170 },
  { label: '操作', slot: 'actions', minWidth: 240, fixed: 'right' },
]

const modelColumns: TableColumn[] = [
  { label: '模型名称', prop: 'name', minWidth: 140 },
  { label: '显示名称', prop: 'displayName', minWidth: 140 },
  { label: 'Max Tokens', prop: 'maxTokens', width: 120, align: 'center' },
  { label: '状态', slot: 'status', width: 80, align: 'center' },
  { label: '创建时间', prop: 'createTime', width: 170 },
  { label: '操作', slot: 'actions', width: 130, fixed: 'right' },
]

function healthDotClass(status?: string) {
  if (status === 'UP') return 'health-dot--up'
  if (status === 'DOWN') return 'health-dot--down'
  return 'health-dot--unknown'
}

function resolveAuthType(type?: string) {
  if (type === 'claude') return 'api_key_header'
  if (type === 'ollama') return 'none'
  return 'bearer'
}

async function openEditDialog(id?: number) {
  if (!id) return
  const detail = await getProviderById(id)
  dialogRef.value?.open(detail)
}

async function fetchProviders(params: PageParams) {
  return getProviders({
    page: params.page,
    pageSize: params.pageSize,
  })
}

function handleOpenModels(row: Provider) {
  currentProvider.value = row
  modelDialogVisible.value = true
}

async function fetchModelsForProvider(params: PageParams) {
  if (!currentProvider.value?.id) {
    return { list: [], total: 0 }
  }
  return getModels({
    page: params.page,
    pageSize: params.pageSize,
    providerId: currentProvider.value.id,
  })
}

function openAddModelDialog() {
  if (!currentProvider.value?.id) return
  modelFormDialogRef.value?.open({ providerId: currentProvider.value.id })
}

function openEditModelDialog(row: ModelVO) {
  modelFormDialogRef.value?.open({
    id: row.id,
    providerId: row.providerId,
    name: row.name,
    displayName: row.displayName,
    maxTokens: row.maxTokens,
    defaultParamsText: row.defaultParams ? JSON.stringify(row.defaultParams, null, 2) : '',
  })
}

// ---- Form config ----

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  authType: [{ required: true, message: '请选择认证方式', trigger: 'change' }],
  apiBaseUrl: [{ required: true, message: '请输入 Base URL', trigger: 'blur' }],
}

const modelRules: FormRules = {
  providerId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
}

async function handleSubmit(form: Provider) {
  const authType = resolveAuthType(form.type)
  if (form.id) {
    const payload: ProviderUpdateRequest = {
      name: form.name,
      type: form.type,
      authType,
      apiBaseUrl: form.apiBaseUrl,
    }
    if (form.apiKey && form.apiKey.trim()) {
      payload.apiKey = form.apiKey.trim()
    }
    await updateProvider(form.id, payload)
    notifySuccess('更新成功')
  } else {
    const payload: ProviderCreateRequest = {
      name: form.name,
      type: form.type,
      authType,
      apiBaseUrl: form.apiBaseUrl,
      apiKey: form.apiKey && form.apiKey.trim() ? form.apiKey.trim() : undefined,
    }
    await createProvider(payload)
    notifySuccess('创建成功')
  }
  tableRef.value?.refresh()
}

// ---- Delete ----

const { confirm } = useConfirm()

async function handleDelete(id?: number) {
  if (!id) return
  await confirm('确定要删除该供应商吗？删除后不可恢复。', async () => {
    await deleteProvider(id)
  })
  tableRef.value?.refresh()
}

async function handleTest(row: Provider) {
  if (!row.id) return
  row.health = await testProvider(row.id)
  notifySuccess('连通性测试完成')
}

async function handleModelSubmit(form: ModelForm) {
  const defaultParams = form.defaultParamsText?.trim() || undefined

  if (form.id) {
    const payload: ModelUpdateRequest = {
      name: form.name,
      displayName: form.displayName,
      maxTokens: form.maxTokens,
      defaultParams,
    }
    await updateModel(form.id, payload)
    notifySuccess('更新成功')
  } else {
    const payload: ModelCreateRequest = {
      providerId: form.providerId,
      name: form.name,
      displayName: form.displayName,
      maxTokens: form.maxTokens,
      defaultParams,
    }
    await createModel(payload)
    notifySuccess('创建成功')
  }
  modelTableRef.value?.refresh()
  tableRef.value?.refresh()
}

async function handleDeleteModel(id?: number) {
  if (!id) return
  await confirm('确定要删除该模型吗？删除后不可恢复。', async () => {
    await deleteModel(id)
  })
  modelTableRef.value?.refresh()
  tableRef.value?.refresh()
}
</script>

<style scoped>
.page-container {
  max-width: 1600px;
}

.health-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.health-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.health-dot--up {
  background-color: #67c23a;
}

.health-dot--down {
  background-color: #f56c6c;
}

.health-dot--unknown {
  background-color: #909399;
}

.health-label {
  font-size: 12px;
  line-height: 1;
}

.health-low {
  color: #67c23a;
}

.health-high {
  color: #e6a23c;
}

.health-down {
  color: #f56c6c;
}

.health-unknown {
  color: #909399;
}
</style>