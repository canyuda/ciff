<template>
  <div class="page-container">
    <PageHeader title="文档管理" description="管理所有知识库文档，支持上传、编辑和重建索引">
      <el-button type="primary" @click="uploadVisible = true">
        <el-icon><Plus /></el-icon>上传文档
      </el-button>
    </PageHeader>

    <!-- Filter -->
    <div class="ciff-card filter-bar">
      <el-select
        v-model="filterKnowledgeId"
        placeholder="选择知识库"
        clearable
        style="width: 240px"
        @change="handleFilterChange"
      >
        <el-option
          v-for="item in knowledgeOptions"
          :key="item.id"
          :label="item.name"
          :value="item.id"
        />
      </el-select>
      <el-input
        v-model="filterFileName"
        placeholder="文档名"
        clearable
        style="width: 240px; margin-left: 12px"
        @keyup.enter="handleFilterChange"
      />
      <el-button type="primary" @click="handleFilterChange" style="margin-left: 12px">
        查询
      </el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <div class="ciff-card">
      <CiffTable ref="tableRef" :columns="columns" :api="fetchList">
        <template #knowledgeName="{ row }">
          <span>{{ row.knowledgeName || '-' }}</span>
        </template>
        <template #fileSize="{ row }">
          {{ formatSize(row.fileSize) }}
        </template>
        <template #status="{ row }">
          <el-tag v-if="row.status === 'ready'" type="success" size="small">就绪</el-tag>
          <el-tag v-else-if="row.status === 'processing'" type="warning" size="small">处理中</el-tag>
          <el-tag v-else-if="row.status === 'failed'" type="danger" size="small">失败</el-tag>
          <el-tag v-else size="small">{{ row.status }}</el-tag>
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" @click="openChunkDialog(row)">查看分块</el-button>
          <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button
            v-if="row.status === 'failed'"
            link type="warning"
            @click="handleRebuild(row)"
          >重建索引</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </CiffTable>
    </div>

    <!-- Upload Dialog -->
    <el-dialog v-model="uploadVisible" title="上传文档" width="520px" destroy-on-close>
      <el-form>
        <el-form-item label="知识库" required>
          <el-select v-model="uploadKnowledgeId" placeholder="选择知识库" style="width: 100%">
            <el-option
              v-for="item in knowledgeOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".txt"
            :on-change="handleFileChange"
            :on-remove="() => pendingFile = null"
          >
            <el-button type="primary">选择 TXT 文件</el-button>
            <template #tip>
              <div class="el-upload__tip">仅支持 .txt 文件，大小不超过 5MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="uploading"
          :disabled="!pendingFile || !uploadKnowledgeId"
          @click="handleUpload"
        >
          上传并处理
        </el-button>
      </template>
    </el-dialog>

    <!-- Edit Dialog -->
    <el-dialog v-model="editVisible" title="编辑文档" width="420px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef">
        <el-form-item label="文件名" prop="fileName">
          <el-input v-model="editForm.fileName" placeholder="请输入文件名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Chunk Dialog -->
    <el-dialog v-model="chunkDialogVisible" :title="`分块列表 - ${currentDocName}`" width="880px" destroy-on-close>
      <el-table :data="chunkList" v-loading="chunkLoading" style="width: 100%" max-height="500">
        <el-table-column label="序号" width="70" align="center">
          <template #default="{ row }">
            {{ (row.chunkIndex ?? 0) + 1 }}
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            {{ truncateContent(row.content) }}
          </template>
        </el-table-column>
        <el-table-column label="内容长度" width="100" align="center">
          <template #default="{ row }">
            {{ row.content?.length ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openContentDialog(row.content)">查看内容</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- Content Dialog -->
    <el-dialog v-model="contentDialogVisible" title="分块内容" width="720px" destroy-on-close>
      <div style="white-space: pre-wrap; line-height: 1.8; max-height: 500px; overflow-y: auto; padding: 8px;">
        {{ currentContent }}
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import type { UploadFile, FormInstance, FormRules } from 'element-plus'
import CiffTable from '@/components/CiffTable.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess } from '@/utils/notify'
import {
  getAllDocuments,
  uploadDocument,
  deleteDocument,
  updateDocument,
  rebuildVectors,
  getKnowledgeList,
  getChunksByDocumentId,
  type DocumentVO,
  type KnowledgeVO,
  type ChunkVO,
} from '@/api/knowledge'
import type { TableColumn, PageParams } from '@/types/common'

const route = useRoute()

const filterKnowledgeId = ref<number | undefined>(undefined)
const filterFileName = ref('')
const knowledgeOptions = ref<KnowledgeVO[]>([])
const uploadVisible = ref(false)
const uploading = ref(false)
const uploadKnowledgeId = ref<number | undefined>(undefined)
const pendingFile = ref<File | null>(null)
const uploadRef = ref<any>(null)

const editVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = ref({ id: 0, fileName: '' })
const editRules: FormRules = {
  fileName: [{ required: true, message: '请输入文件名', trigger: 'blur' }],
}

const tableRef = ref<any>(null)

// Chunk dialog state
const chunkDialogVisible = ref(false)
const chunkLoading = ref(false)
const chunkList = ref<ChunkVO[]>([])
const currentDocName = ref('')
const currentKnowledgeId = ref<number | undefined>(undefined)

// Content dialog state
const contentDialogVisible = ref(false)
const currentContent = ref('')

const columns: TableColumn[] = [
  { label: '文件名', prop: 'fileName', minWidth: 180, showOverflowTooltip: true },
  { label: '所属知识库', slot: 'knowledgeName', minWidth: 140 },
  { label: '大小', slot: 'fileSize', width: 100, align: 'center' },
  { label: '分块数', prop: 'chunkCount', width: 80, align: 'center' },
  { label: '状态', slot: 'status', width: 100, align: 'center' },
  { label: '上传时间', prop: 'createTime', width: 170 },
  { label: '操作', slot: 'actions', minWidth: 220, fixed: 'right' },
]

const { confirm } = useConfirm()

onMounted(() => {
  loadKnowledgeOptions()
  // Read filter from URL query
  const qKnowledgeId = route.query.knowledgeId
  if (qKnowledgeId) {
    filterKnowledgeId.value = Number(qKnowledgeId)
  }
  const qFileName = route.query.fileName
  if (qFileName) {
    filterFileName.value = String(qFileName)
  }
})

async function loadKnowledgeOptions() {
  try {
    const result = await getKnowledgeList({ page: 1, pageSize: 1000 })
    knowledgeOptions.value = result.list
  } catch {
    knowledgeOptions.value = []
  }
}

async function fetchList(params: PageParams) {
  return getAllDocuments({
    page: params.page,
    pageSize: params.pageSize,
    knowledgeId: filterKnowledgeId.value,
    fileName: filterFileName.value || undefined,
  })
}

function handleFilterChange() {
  tableRef.value?.refresh()
}

function resetFilter() {
  filterKnowledgeId.value = undefined
  filterFileName.value = ''
  tableRef.value?.refresh()
}

function handleFileChange(file: UploadFile) {
  pendingFile.value = file.raw ?? null
}

async function handleUpload() {
  if (!pendingFile.value || !uploadKnowledgeId.value) return
  uploading.value = true
  try {
    await uploadDocument(uploadKnowledgeId.value, pendingFile.value)
    notifySuccess('文档已上传')
    pendingFile.value = null
    uploadVisible.value = false
    uploadKnowledgeId.value = undefined
    uploadRef.value?.clearFiles?.()
    tableRef.value?.refresh()
  } finally {
    uploading.value = false
  }
}

function openEditDialog(row: DocumentVO) {
  editForm.value = { id: row.id!, fileName: row.fileName || '' }
  editVisible.value = true
}

async function handleEditSubmit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  editLoading.value = true
  try {
    await updateDocument(editForm.value.id, editForm.value.fileName)
    notifySuccess('更新成功')
    editVisible.value = false
    tableRef.value?.refresh()
  } finally {
    editLoading.value = false
  }
}

async function handleDelete(id?: number) {
  if (!id) return
  await confirm('确定要删除该文档吗？关联的向量数据将一并删除。', async () => {
    await deleteDocument(id)
  })
  tableRef.value?.refresh()
}

async function handleRebuild(row: DocumentVO) {
  const docId = row.id
  const knowledgeId = row.knowledgeId
  if (!docId || !knowledgeId) return
  await confirm('确定要重建该文档的向量索引吗？', async () => {
    await rebuildVectors(knowledgeId, { documentId: docId })
  })
  notifySuccess('已触发重建')
  tableRef.value?.refresh()
}

function formatSize(bytes?: number): string {
  if (!bytes) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

// Chunk dialog methods
function openChunkDialog(row: DocumentVO) {
  currentDocName.value = row.fileName || ''
  currentKnowledgeId.value = row.knowledgeId
  chunkDialogVisible.value = true
  loadChunks(row.id!)
}

async function loadChunks(documentId: number) {
  chunkLoading.value = true
  try {
    chunkList.value = await getChunksByDocumentId(documentId)
  } catch {
    chunkList.value = []
  } finally {
    chunkLoading.value = false
  }
}

function truncateContent(content?: string): string {
  if (!content) return '-'
  if (content.length <= 100) return content
  return content.substring(0, 100) + '...'
}

function openContentDialog(content?: string) {
  currentContent.value = content || ''
  contentDialogVisible.value = true
}
</script>

<style scoped>
.page-container {
  max-width: 1600px;
}

.filter-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px 20px;
}
</style>
