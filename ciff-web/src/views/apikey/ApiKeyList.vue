<template>
  <div class="page-container">
    <PageHeader title="API Key 管理" description="管理外部接口访问密钥">
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>创建 API Key
      </el-button>
    </PageHeader>

    <div class="ciff-card">
      <el-table :data="keys" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="keyPrefix" label="Key 前缀" min-width="180">
          <template #default="{ row }">
            <code class="key-prefix">{{ row.keyPrefix }}****</code>
          </template>
        </el-table-column>
        <el-table-column prop="agentName" label="关联 Agent" min-width="120" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'active'" type="success" size="small">启用</el-tag>
            <el-tag v-else type="danger" size="small">已吊销</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expiresAt" label="过期时间" width="170">
          <template #default="{ row }">
            <span v-if="row.expiresAt">{{ row.expiresAt }}</span>
            <span v-else class="text-muted">永不过期</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-popconfirm
              v-if="row.status === 'active'"
              title="确定要吊销该 API Key 吗？吊销后不可恢复。"
              @confirm="handleRevoke(row.id)"
            >
              <template #reference>
                <el-button link type="danger" size="small">吊销</el-button>
              </template>
            </el-popconfirm>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Create Dialog -->
    <el-dialog v-model="dialogVisible" title="创建 API Key" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="API Key 名称" />
        </el-form-item>
        <el-form-item label="关联 Agent" prop="agentId">
          <el-select v-model="form.agentId" placeholder="请选择 Agent" style="width: 100%">
            <el-option
              v-for="agent in agents"
              :key="agent.id"
              :label="agent.name"
              :value="agent.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="form.expiresAt"
            type="datetime"
            placeholder="可选，留空表示永不过期"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- Show Key Dialog -->
    <el-dialog v-model="showKeyVisible" title="API Key 已创建" width="520px" :close-on-click-modal="false">
      <el-alert
        type="warning"
        title="请立即复制 API Key，关闭后将无法再次查看。"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />
      <div class="raw-key-box">
        <code class="raw-key">{{ createdKey }}</code>
        <el-button size="small" @click="copyKey">复制</el-button>
      </div>
      <template #footer>
        <el-button type="primary" @click="showKeyVisible = false">我已复制</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { listApiKeys, createApiKey, revokeApiKey, type ApiKeyVO } from '@/api/apiKey'
import { getAgents, type AgentVO } from '@/api/agent'

const keys = ref<ApiKeyVO[]>([])
const agents = ref<AgentVO[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const creating = ref(false)
const showKeyVisible = ref(false)
const createdKey = ref('')
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  agentId: null as number | null,
  expiresAt: null as string | null,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  agentId: [{ required: true, message: '请选择 Agent', trigger: 'change' }],
}

async function fetchKeys() {
  loading.value = true
  try {
    keys.value = await listApiKeys()
  } finally {
    loading.value = false
  }
}

async function fetchAgents() {
  try {
    const res = await getAgents({ page: 1, pageSize: 100 })
    agents.value = res.list || []
  } catch {
    // ignore
  }
}

function openCreateDialog() {
  form.name = ''
  form.agentId = null
  form.expiresAt = null
  dialogVisible.value = true
}

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  creating.value = true
  try {
    const res = await createApiKey({
      name: form.name,
      agentId: form.agentId!,
      expiresAt: form.expiresAt,
    })
    createdKey.value = res.rawKey || ''
    dialogVisible.value = false
    showKeyVisible.value = true
    await fetchKeys()
  } finally {
    creating.value = false
  }
}

async function handleRevoke(id: number) {
  await revokeApiKey(id)
  ElMessage.success('已吊销')
  await fetchKeys()
}

function copyKey() {
  navigator.clipboard.writeText(createdKey.value)
  ElMessage.success('已复制到剪贴板')
}

onMounted(() => {
  fetchKeys()
  fetchAgents()
})
</script>

<style scoped>
.key-prefix {
  font-family: var(--ciff-font-mono);
  font-size: var(--ciff-text-sm);
  background: var(--ciff-neutral-100);
  padding: 2px 8px;
  border-radius: var(--ciff-radius-base);
}

.text-muted {
  color: var(--ciff-neutral-400);
  font-size: var(--ciff-text-sm);
}

.raw-key-box {
  display: flex;
  align-items: center;
  gap: var(--ciff-space-3);
  padding: var(--ciff-space-3);
  background: var(--ciff-neutral-100);
  border-radius: var(--ciff-radius-md);
  border: 1px solid var(--ciff-border-light);
}

.raw-key {
  flex: 1;
  font-family: var(--ciff-font-mono);
  font-size: var(--ciff-text-sm);
  word-break: break-all;
}
</style>
