<template>
  <div class="page-container">
    <PageHeader title="知识库管理" description="创建和管理知识库，上传文档并进行向量索引">
      <el-button type="primary" @click="dialogRef?.open()">
        <el-icon><Plus /></el-icon>创建知识库
      </el-button>
    </PageHeader>

    <div class="ciff-card">
      <CiffTable ref="tableRef" :columns="columns" :api="fetchList">
        <template #status="{ row }">
          <el-tag v-if="row.status === 'active'" type="success" size="small">启用</el-tag>
          <el-tag v-else-if="row.status === 'inactive'" type="info" size="small">停用</el-tag>
          <el-tag v-else size="small">{{ row.status }}</el-tag>
        </template>

        <template #docCount="{ row }">
          <el-button link type="primary" @click="openDocPanel(row)">
            {{ row.documentCount ?? 0 }}
          </el-button>
        </template>

        <template #actions="{ row }">
          <div style="display: flex; gap: 8px">
            <el-button link type="primary" @click="openEditDialog(row.id)">编辑</el-button>
            <el-dropdown trigger="click">
              <el-button link type="primary">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push({ path: '/knowledge-documents', query: { knowledgeId: row.id } })">文档管理</el-dropdown-item>
                  <el-dropdown-item @click="$router.push({ path: '/recall-test', query: { knowledgeId: row.id } })">召回测试</el-dropdown-item>
                  <el-dropdown-item @click="handleRebuild(row.id)">重建索引</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </div>
        </template>
      </CiffTable>
    </div>

    <!-- Create / Edit Dialog -->
    <CiffFormDialog
      ref="dialogRef"
      title="知识库"
      width="520px"
      :rules="rules"
      :submit-handler="handleSubmit"
    >
      <template #default="{ data, isEdit }">
        <el-form-item label="名称" prop="name">
          <el-input v-model="data.name" placeholder="例如：产品文档库" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="data.description"
            type="textarea"
            :rows="3"
            placeholder="知识库用途描述"
          />
        </el-form-item>
        <el-form-item label="Embedding 模型" prop="embeddingModel">
          <el-select
            v-model="data.embeddingModel"
            placeholder="请选择 Embedding 模型"
            style="width: 100%"
            :disabled="isEdit"
          >
            <el-option label="text-embedding-v3" value="text-embedding-v3" />
          </el-select>
        </el-form-item>
        <el-form-item label="分块大小" prop="chunkSize">
          <el-input-number
            v-model="data.chunkSize"
            :min="128"
            :max="2048"
            :step="64"
            placeholder="默认 700"
            style="width: 100%"
          />
        </el-form-item>
      </template>
    </CiffFormDialog>

    <!-- Document Management Drawer -->
    <el-drawer
      v-model="docDrawerVisible"
      :title="`文档管理 - ${currentKnowledge?.name ?? ''}`"
      size="520px"
      destroy-on-close
    >
      <div class="doc-drawer__upload">
        <el-upload
          :auto-upload="false"
          :limit="1"
          accept=".txt"
          :on-change="handleFileChange"
          :file-list="uploadFileList"
        >
          <el-button type="primary">选择 TXT 文件</el-button>
        </el-upload>
        <el-button
          type="success"
          :loading="uploading"
          :disabled="!pendingFile"
          @click="handleUpload"
          style="margin-left: 12px"
        >
          上传并处理
        </el-button>
      </div>

      <el-table :data="documents" v-loading="docsLoading" style="width: 100%">
        <el-table-column prop="fileName" label="文件名" min-width="160" show-overflow-tooltip />
        <el-table-column label="大小" width="100" align="center">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="分块数" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'ready'" type="success" size="small">就绪</el-tag>
            <el-tag v-else-if="row.status === 'processing'" type="warning" size="small">处理中</el-tag>
            <el-tag v-else-if="row.status === 'failed'" type="danger" size="small">失败</el-tag>
            <el-tag v-else size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'failed' || row.status === 'uploading'"
              link type="warning"
              @click="handleProcess(row.id)"
            >处理</el-button>
            <el-button link type="danger" @click="handleDeleteDoc(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import type { UploadFile } from 'element-plus'
import CiffTable from '@/components/CiffTable.vue'
import CiffFormDialog from '@/components/CiffFormDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess } from '@/utils/notify'
import {
  getKnowledgeList,
  getKnowledgeById,
  createKnowledge,
  updateKnowledge,
  deleteKnowledge,
  uploadDocument,
  listDocuments,
  deleteDocument,
  processDocument,
  rebuildVectors,
  type KnowledgeVO,
  type KnowledgeCreateRequest,
  type KnowledgeUpdateRequest,
  type DocumentVO,
} from '@/api/knowledge'
import type { TableColumn, PageParams } from '@/types/common'
import type { FormRules } from 'element-plus'

interface TableRef {
  refresh: () => void
}

interface DialogRef {
  open: (data?: Partial<KnowledgeForm>) => void
}

interface KnowledgeForm {
  id?: number
  name: string
  description?: string
  chunkSize?: number
  embeddingModel: string
}

const tableRef = ref<TableRef | null>(null)
const dialogRef = ref<DialogRef | null>(null)

const columns: TableColumn[] = [
  { label: '名称', prop: 'name', minWidth: 160 },
  { label: '状态', slot: 'status', width: 80, align: 'center' },
  { label: '文档数', slot: 'docCount', width: 80, align: 'center' },
  { label: '分块大小', prop: 'chunkSize', width: 90, align: 'center' },
  { label: 'Embedding 模型', prop: 'embeddingModel', minWidth: 140 },
  { label: '创建时间', prop: 'createTime', width: 170 },
  { label: '操作', slot: 'actions', minWidth: 180, fixed: 'right' },
]

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  embeddingModel: [{ required: true, message: '请选择 Embedding 模型', trigger: 'change' }],
}

// Document drawer state
const docDrawerVisible = ref(false)
const currentKnowledge = ref<KnowledgeVO | null>(null)
const documents = ref<DocumentVO[]>([])
const docsLoading = ref(false)
const uploading = ref(false)
const pendingFile = ref<File | null>(null)
const uploadFileList = ref<UploadFile[]>([])

async function fetchList(params: PageParams) {
  return getKnowledgeList({ page: params.page, pageSize: params.pageSize })
}

async function openEditDialog(id?: number) {
  if (!id) return
  const detail = await getKnowledgeById(id)
  const formData: KnowledgeForm = {
    id: detail.id,
    name: detail.name,
    description: detail.description,
    chunkSize: detail.chunkSize,
    embeddingModel: detail.embeddingModel ?? '',
  }
  dialogRef.value?.open(formData)
}

async function handleSubmit(form: KnowledgeForm) {
  if (form.id) {
    const payload: KnowledgeUpdateRequest = {
      name: form.name,
      description: form.description,
      chunkSize: form.chunkSize,
      embeddingModel: form.embeddingModel,
    }
    await updateKnowledge(form.id, payload)
    notifySuccess('更新成功')
  } else {
    const payload: KnowledgeCreateRequest = {
      name: form.name,
      description: form.description,
      chunkSize: form.chunkSize,
      embeddingModel: form.embeddingModel,
    }
    await createKnowledge(payload)
    notifySuccess('创建成功')
  }
  tableRef.value?.refresh()
}

const { confirm } = useConfirm()

async function handleDelete(id?: number) {
  if (!id) return
  await confirm('确定要删除该知识库吗？关联的文档和向量数据将一并删除。', async () => {
    await deleteKnowledge(id)
  })
  tableRef.value?.refresh()
}

async function handleRebuild(id?: number) {
  if (!id) return
  await confirm('确定要重建该知识库的向量索引吗？', async () => {
    await rebuildVectors(id)
    notifySuccess('已触发重建')
  })
}

// Document management
function openDocPanel(knowledge: KnowledgeVO) {
  currentKnowledge.value = knowledge
  docDrawerVisible.value = true
  loadDocuments(knowledge.id!)
}

async function loadDocuments(knowledgeId: number) {
  docsLoading.value = true
  try {
    documents.value = await listDocuments(knowledgeId)
  } catch {
    documents.value = []
  } finally {
    docsLoading.value = false
  }
}

function handleFileChange(file: UploadFile) {
  pendingFile.value = file.raw ?? null
  uploadFileList.value = [file]
}

async function handleUpload() {
  if (!pendingFile.value || !currentKnowledge.value?.id) return
  uploading.value = true
  try {
    await uploadDocument(currentKnowledge.value.id, pendingFile.value)
    notifySuccess('文档已上传')
    pendingFile.value = null
    uploadFileList.value = []
    loadDocuments(currentKnowledge.value.id)
  } finally {
    uploading.value = false
  }
}

async function handleProcess(docId?: number) {
  if (!docId) return
  await processDocument(docId)
  notifySuccess('已触发处理')
  if (currentKnowledge.value?.id) {
    loadDocuments(currentKnowledge.value.id)
  }
}

async function handleDeleteDoc(docId?: number) {
  if (!docId) return
  await confirm('确定要删除该文档吗？', async () => {
    await deleteDocument(docId)
  })
  if (currentKnowledge.value?.id) {
    loadDocuments(currentKnowledge.value.id)
  }
}

function formatSize(bytes?: number): string {
  if (!bytes) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<style scoped>
.page-container {
  max-width: 1600px;
}

.doc-drawer__upload {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
</style>
