<template>
  <div class="page-container">
    <PageHeader title="模型管理" description="配置各供应商下的可用模型及默认参数">
      <el-button type="primary" @click="dialogRef?.open()">
        <el-icon><Plus /></el-icon>新增模型
      </el-button>
    </PageHeader>

    <div class="ciff-card">
      <CiffTable ref="tableRef" :columns="columns" :api="fetchModels">
        <template #status="{ row }">
          <el-tag v-if="row.status === 'active'" type="success" size="small">启用</el-tag>
          <el-tag v-else type="info" size="small">禁用</el-tag>
        </template>

        <template #actions="{ row }">
          <div style="display: flex; gap: 8px">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </div>
        </template>
      </CiffTable>
    </div>

    <CiffFormDialog
      ref="dialogRef"
      title="模型"
      width="560px"
      :rules="rules"
      :submit-handler="handleSubmit"
    >
      <template #default="{ data, isEdit }">
        <el-form-item label="供应商" prop="providerId">
          <el-select v-model="data.providerId" placeholder="请选择供应商" style="width: 100%" :disabled="isEdit">
            <el-option
              v-for="p in providerOptions"
              :key="p.id"
              :label="p.name"
              :value="p.id!"
            />
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
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import CiffTable from '@/components/CiffTable.vue'
import CiffFormDialog from '@/components/CiffFormDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess } from '@/utils/notify'
import {
  getModels,
  createModel,
  updateModel,
  deleteModel,
  type ModelVO,
  type ModelCreateRequest,
  type ModelUpdateRequest,
} from '@/api/model'
import { getProviderList, type ProviderListItem } from '@/api/provider'
import type { TableColumn, PageParams } from '@/types/common'
import type { FormRules } from 'element-plus'

interface TableRef {
  refresh: () => void
}

interface DialogRef {
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

const tableRef = ref<TableRef | null>(null)
const dialogRef = ref<DialogRef | null>(null)
const providerOptions = ref<ProviderListItem[]>([])

const columns: TableColumn[] = [
  { label: '模型名称', prop: 'name', minWidth: 140 },
  { label: '显示名称', prop: 'displayName', minWidth: 140 },
  { label: '供应商', prop: 'providerName', minWidth: 140 },
  { label: 'Max Tokens', prop: 'maxTokens', width: 120, align: 'center' },
  { label: '状态', slot: 'status', width: 80, align: 'center' },
  { label: '创建时间', prop: 'createTime', width: 170 },
  { label: '操作', slot: 'actions', width: 130, fixed: 'right' },
]

async function fetchModels(params: PageParams) {
  return getModels({
    page: params.page,
    pageSize: params.pageSize,
  })
}

async function loadProviders() {
  try {
    providerOptions.value = await getProviderList()
  } catch {
    providerOptions.value = []
  }
}

function openDialog(row?: ModelVO) {
  if (!row) {
    dialogRef.value?.open()
    return
  }
  const form: Partial<ModelForm> = {
    id: row.id,
    providerId: row.providerId,
    name: row.name,
    displayName: row.displayName,
    maxTokens: row.maxTokens,
    defaultParamsText: row.defaultParams ? JSON.stringify(row.defaultParams, null, 2) : '',
  }
  dialogRef.value?.open(form)
}

onMounted(() => {
  loadProviders()
})

const rules: FormRules = {
  providerId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
}

async function handleSubmit(form: ModelForm) {
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
  tableRef.value?.refresh()
}

const { confirm } = useConfirm()

async function handleDelete(id?: number) {
  if (!id) return
  await confirm('确定要删除该模型吗？删除后不可恢复。', async () => {
    await deleteModel(id)
  })
  tableRef.value?.refresh()
}
</script>

<style scoped>
.page-container {
  max-width: var(--ciff-content-max-width);
}
</style>