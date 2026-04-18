<template>
  <div class="page-container">
    <PageHeader title="工具管理" description="配置 Agent 可调用的外部工具，支持 API 和 MCP 类型">
      <el-button type="primary" @click="dialogRef?.open()">
        <el-icon><Plus /></el-icon>新增工具
      </el-button>
    </PageHeader>

    <div class="ciff-card">
      <CiffTable ref="tableRef" :columns="columns" :api="fetchTools">
        <template #type="{ row }">
          <el-tag v-if="row.type === 'api'" type="primary" size="small" effect="dark">API</el-tag>
          <el-tag v-else type="success" size="small" effect="dark">MCP</el-tag>
        </template>

        <template #status="{ row }">
          <el-tag v-if="row.status === 'enabled'" type="success" size="small">启用</el-tag>
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
      title="工具"
      width="600px"
      :rules="rules"
      :submit-handler="handleSubmit"
    >
      <template #default="{ data }">
        <el-form-item label="名称" prop="name">
          <el-input v-model="data.name" placeholder="例如：weather-api" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="data.type" placeholder="请选择工具类型" style="width: 100%">
            <el-option label="API" value="api" />
            <el-option label="MCP" value="mcp" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="data.description" type="textarea" :rows="2" placeholder="工具功能描述" />
        </el-form-item>
        <el-form-item label="端点地址" prop="endpoint">
          <el-input v-model="data.endpoint" placeholder="API URL 或 MCP Server 地址" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="data.status" style="width: 100%">
            <el-option label="启用" value="enabled" />
            <el-option label="禁用" value="disabled" />
          </el-select>
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
  getTools,
  createTool,
  updateTool,
  deleteTool,
  type ToolVO,
  type ToolCreateRequest,
  type ToolUpdateRequest,
} from '@/api/tool'
import type { TableColumn, PageParams } from '@/types/common'
import type { FormRules } from 'element-plus'

interface TableRef {
  refresh: () => void
}

interface DialogRef {
  open: (data?: Partial<ToolVO>) => void
}

const tableRef = ref<TableRef | null>(null)
const dialogRef = ref<DialogRef | null>(null)

const columns: TableColumn[] = [
  { label: '名称', prop: 'name', minWidth: 140 },
  { label: '类型', slot: 'type', width: 100, align: 'center' },
  { label: '描述', prop: 'description', minWidth: 200 },
  { label: '端点地址', prop: 'endpoint', minWidth: 240 },
  { label: '状态', slot: 'status', width: 80, align: 'center' },
  { label: '创建时间', prop: 'createTime', width: 170 },
  { label: '操作', slot: 'actions', minWidth: 130, fixed: 'right' },
]

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  endpoint: [{ required: true, message: '请输入端点地址', trigger: 'blur' }],
}

async function fetchTools(params: PageParams) {
  return getTools({ page: params.page, pageSize: params.pageSize })
}

async function handleSubmit(form: ToolVO) {
  if (form.id) {
    const payload: ToolUpdateRequest = {
      name: form.name,
      description: form.description,
      type: form.type,
      endpoint: form.endpoint,
      status: form.status,
    }
    await updateTool(form.id, payload)
    notifySuccess('更新成功')
  } else {
    const payload: ToolCreateRequest = {
      name: form.name,
      description: form.description,
      type: form.type,
      endpoint: form.endpoint,
    }
    await createTool(payload)
    notifySuccess('创建成功')
  }
  tableRef.value?.refresh()
}

const { confirm } = useConfirm()

async function handleDelete(id?: number) {
  if (!id) return
  await confirm('确定要删除该工具吗？删除后不可恢复。', async () => {
    await deleteTool(id)
  })
  tableRef.value?.refresh()
}
</script>

<style scoped>
.page-container {
  max-width: 1600px;
}
</style>
