<template>
  <div class="page-container">
    <PageHeader title="模型提供商管理" description="配置模型供应商及其接入参数，支持 OpenAI、Claude、Gemini、Ollama 等">
      <el-button type="primary" @click="dialogRef?.open()">
        <el-icon><Plus /></el-icon>新增提供商
      </el-button>
    </PageHeader>

    <div class="ciff-card">
      <CiffTable ref="tableRef" :columns="columns" :api="fetchProviders">
        <template #type="{ row }">
          <el-tag size="small" effect="dark" :color="typeColorMap[row.type]" style="border: none">{{ providerNameMap[row.type] || row.type }}</el-tag>
        </template>

        <template #status="{ row }">
          <el-switch
            v-model="row.enabled"
            style="--el-switch-on-color: #10B981; --el-switch-off-color: #F59E0B"
            active-text="启用"
            inactive-text="禁用"
            inline-prompt
          />
        </template>

        <template #actions="{ row }">
          <div style="display: flex; gap: 8px">
            <el-button type="primary" size="small" @click="dialogRef?.open(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </div>
        </template>
      </CiffTable>
    </div>

    <CiffFormDialog
      ref="dialogRef"
      title="提供商"
      :rules="rules"
      @submit="handleSubmit"
    >
      <template #default="{ data }">
        <el-form-item label="名称" prop="name">
          <el-input v-model="data.name" placeholder="例如：OpenAI Production" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="data.type" placeholder="请选择供应商类型" style="width: 100%" @change="data.baseUrl = ''">
            <el-option v-for="t in providerTypes" :key="t" :label="providerNameMap[t] || t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="data.baseUrl" :placeholder="baseUrlPlaceholder[data.type as string] || 'https://'">
            <template #append>
              <el-button :disabled="!data.type" @click="data.baseUrl = baseUrlPlaceholder[data.type as string] || ''">填入</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="data.apiKey" type="password" show-password placeholder="sk-..." />
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
import type { TableColumn } from '@/types/common'
import type { FormRules } from 'element-plus'

// ---- Types ----

interface Provider {
  id: number
  name: string
  type: string
  baseUrl: string
  apiKey: string
  enabled: boolean
  createdAt: string
  [key: string]: unknown
}

// ---- Mock data ----

let nextId = 8

const mockData: Provider[] = [
  {
    id: 1,
    name: 'OpenAI Production',
    type: 'openai',
    baseUrl: 'https://api.openai.com',
    apiKey: 'sk-proj-xxxx...a1b2',
    enabled: true,
    createdAt: '2025-01-15 10:30:00',
  },
  {
    id: 2,
    name: 'Claude API',
    type: 'claude',
    baseUrl: 'https://api.anthropic.com',
    apiKey: 'sk-ant-xxxx...c3d4',
    enabled: true,
    createdAt: '2025-02-03 14:20:00',
  },
  {
    id: 3,
    name: 'Gemini Pro',
    type: 'gemini',
    baseUrl: 'https://generativelanguage.googleapis.com',
    apiKey: 'AIza-xxxx...e5f6',
    enabled: false,
    createdAt: '2025-02-20 09:15:00',
  },
  {
    id: 4,
    name: 'Ollama Local',
    type: 'ollama',
    baseUrl: 'http://localhost:11434',
    apiKey: '',
    enabled: true,
    createdAt: '2025-03-10 16:45:00',
  },
  {
    id: 5,
    name: 'DeepSeek V3',
    type: 'deepseek',
    baseUrl: 'https://api.deepseek.com',
    apiKey: 'sk-xxxx...h1i2',
    enabled: true,
    createdAt: '2025-03-15 08:30:00',
  },
  {
    id: 6,
    name: '通义千问生产',
    type: 'qwen',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    apiKey: 'sk-xxxx...j3k4',
    enabled: true,
    createdAt: '2025-03-20 14:00:00',
  },
  {
    id: 7,
    name: 'Kimi 长文本',
    type: 'kimi',
    baseUrl: 'https://api.moonshot.cn/v1',
    apiKey: 'sk-xxxx...l5m6',
    enabled: false,
    createdAt: '2025-04-01 10:20:00',
  },
]

// Supported providers (English identifiers, stored in DB as t_provider.type):
// OpenAI-compatible: openai, gemini, ollama, deepseek, qwen, zhipu, kimi,
//                    wenxin, doubao, hunyuan, yi, minimax, spark
// Non-OpenAI:        claude (Anthropic Messages API, /v1/messages)
const providerTypes = [
  'openai', 'claude', 'gemini', 'ollama',
  'deepseek', 'qwen', 'zhipu', 'kimi',
  'wenxin', 'doubao', 'hunyuan', 'yi', 'minimax', 'spark',
]

// Display name mapping (Chinese names for domestic providers only)
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

const tableRef = ref()
const dialogRef = ref()

const columns: TableColumn[] = [
  { label: '名称', prop: 'name', minWidth: 160 },
  { label: '类型', slot: 'type', width: 110, align: 'center' },
  { label: 'Base URL', prop: 'baseUrl', minWidth: 240 },
  { label: '状态', slot: 'status', width: 90, align: 'center' },
  { label: '创建时间', prop: 'createdAt', width: 170 },
  { label: '操作', slot: 'actions', width: 130, fixed: 'right' },
]

function fetchProviders(params: { page: number; pageSize: number }) {
  const start = (params.page - 1) * params.pageSize
  const end = start + params.pageSize
  return Promise.resolve({
    list: mockData.slice(start, end),
    total: mockData.length,
  })
}

// ---- Form config ----

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  baseUrl: [{ required: true, message: '请输入 Base URL', trigger: 'blur' }],
}

async function handleSubmit(data: Provider, done: () => void) {
  if (data.id) {
    const idx = mockData.findIndex((item) => item.id === data.id)
    if (idx !== -1) mockData[idx] = { ...mockData[idx], ...data }
    notifySuccess('更新成功')
  } else {
    mockData.unshift({
      ...data,
      id: nextId++,
      enabled: true,
      createdAt: new Date().toLocaleString('zh-CN'),
    })
    notifySuccess('创建成功')
  }
  done()
  tableRef.value?.refresh()
}

// ---- Delete ----

const { confirm } = useConfirm()

async function handleDelete(id: number) {
  await confirm('确定要删除该提供商吗？删除后不可恢复。', () => {
    const idx = mockData.findIndex((item) => item.id === id)
    if (idx !== -1) mockData.splice(idx, 1)
    return Promise.resolve()
  })
  tableRef.value?.refresh()
}
</script>

<style scoped>
.page-container {
  max-width: var(--ciff-content-max-width);
}
</style>
