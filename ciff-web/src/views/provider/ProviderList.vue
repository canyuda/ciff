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
            @change="data.apiBaseUrl = ''"
          >
            <el-option v-for="t in providerTypes" :key="t" :label="providerNameMap[t] || t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="认证方式" prop="authType">
          <el-select v-model="data.authType" placeholder="请选择认证方式" style="width: 100%">
            <el-option v-for="t in authTypes" :key="t" :label="authTypeMap[t] || t" :value="t" />
          </el-select>
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
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import CiffTable from '@/components/CiffTable.vue'
import CiffFormDialog from '@/components/CiffFormDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess } from '@/utils/notify'
import {
  getProviders,
  createProvider,
  updateProvider,
  deleteProvider,
  type Provider,
  type ProviderCreateRequest,
  type ProviderUpdateRequest,
} from '@/api/provider'
import type { TableColumn, PageParams } from '@/types/common'
import type { FormRules } from 'element-plus'

// ---- Types ----

interface TableRef {
  refresh: () => void
}

interface DialogRef {
  open: (data?: Partial<Provider>) => void
}

// ---- Constants ----

const providerTypes = [
  'openai', 'claude', 'gemini', 'ollama',
  'deepseek', 'qwen', 'zhipu', 'kimi',
  'wenxin', 'doubao', 'hunyuan', 'yi', 'minimax', 'spark',
]

const providerNameMap: Record<string, string> = {
  openai: 'OpenAI',
  claude: 'Claude',
  gemini: 'Gemini',
  ollama: 'Ollama',
  deepseek: 'DeepSeek',
  qwen: '通义千问',
  zhipu: '智谱',
  kimi: 'Kimi',
  wenxin: '文心一言',
  doubao: '豆包',
  hunyuan: '混元',
  yi: '零一万物',
  minimax: 'MiniMax',
  spark: '讯飞星火',
}

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

const authTypes = ['bearer', 'api_key_header', 'url', 'jwt', 'dual_key']

const authTypeMap: Record<string, string> = {
  bearer: 'Bearer Token',
  api_key_header: 'API Key Header',
  url: 'URL 参数',
  jwt: 'JWT',
  dual_key: '双密钥',
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

const columns: TableColumn[] = [
  { label: '名称', prop: 'name', minWidth: 160 },
  { label: '类型', slot: 'type', width: 110, align: 'center' },
  { label: '认证方式', slot: 'authType', width: 140, align: 'center' },
  { label: 'Base URL', prop: 'apiBaseUrl', minWidth: 240 },
  { label: '状态', slot: 'status', width: 80, align: 'center' },
  { label: '创建时间', prop: 'createTime', width: 170 },
  { label: '操作', slot: 'actions', width: 130, fixed: 'right' },
]

async function fetchProviders(params: PageParams) {
  return getProviders({
    page: params.page,
    pageSize: params.pageSize,
  })
}

// ---- Form config ----

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  authType: [{ required: true, message: '请选择认证方式', trigger: 'change' }],
  apiBaseUrl: [{ required: true, message: '请输入 Base URL', trigger: 'blur' }],
}

async function handleSubmit(form: Provider) {
  if (form.id) {
    const payload: ProviderUpdateRequest = {
      name: form.name,
      type: form.type,
      authType: form.authType,
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
      authType: form.authType,
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
</script>

<style scoped>
.page-container {
  max-width: var(--ciff-content-max-width);
}
</style>