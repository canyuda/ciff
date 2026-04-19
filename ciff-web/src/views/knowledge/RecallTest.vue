<template>
  <div class="page-container">
    <PageHeader title="召回测试" description="测试向量召回和 Rerank 精排效果" />

    <!-- Operation Area -->
    <div class="ciff-card operation-area">
      <el-form :model="form" label-position="top" class="recall-form">
        <!-- Row 1: knowledge base, rerank switch, confidence -->
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="知识库" required>
              <el-select
                v-model="form.knowledgeIds"
                multiple
                placeholder="至少选择1个知识库"
                style="width: 100%"
              >
                <el-option
                  v-for="item in knowledgeOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="开启精排">
              <el-switch v-model="form.enableRerank" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.enableRerank" :span="8">
            <el-form-item label="置信度过滤上限">
              <el-input-number
                v-model="form.confidence"
                :min="0"
                :max="1"
                :step="0.1"
                :precision="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- Row 2: query text + recall button -->
        <el-row :gutter="24">
          <el-col :span="20">
            <el-form-item label="查询文本" required>
              <el-input
                v-model="form.query"
                type="textarea"
                :rows="3"
                placeholder="输入查询文本..."
              />
            </el-form-item>
          </el-col>
          <el-col :span="4" class="recall-btn-col">
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleSearch">
                召回
              </el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <!-- Result Area -->
    <div v-if="results.length > 0" class="result-area">
      <div class="result-header">
        <span class="result-count">共召回 {{ results.length }} 条结果</span>
        <el-button link type="primary" @click="results = []">清空</el-button>
      </div>

      <div
        v-for="(item, index) in results"
        :key="index"
        class="result-card ciff-card"
      >
        <!-- Upper: content -->
        <div class="result-content">{{ item.content }}</div>

        <!-- Lower: metadata -->
        <div class="result-meta">
          <span class="meta-item">模型:{{ item.embedModel || '-' }}</span>
          <span class="meta-item">所属知识库:{{ item.knowledgeName || '-' }}</span>
          <span class="meta-item">所属文档:{{ item.documentName || '-' }}</span>
          <span class="meta-item">分块序号:{{ item.chunkIndex ?? '-' }}</span>
          <span class="meta-item">向量召回分数:{{ formatScore(item.similarity) }}</span>
          <span class="meta-item">
            精排分数:{{ item.relevanceScore != null ? formatScore(item.relevanceScore) : '无' }}
          </span>
        </div>
      </div>
    </div>

    <el-empty v-else-if="searched" description="暂无召回结果" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { getKnowledgeList, searchKnowledge, type KnowledgeVO, type SearchResultVO } from '@/api/knowledge'

const route = useRoute()

const knowledgeOptions = ref<KnowledgeVO[]>([])
const loading = ref(false)
const searched = ref(false)
const results = ref<SearchResultVO[]>([])

const form = ref({
  knowledgeIds: [] as number[],
  query: '',
  enableRerank: true,
  confidence: 0.3,
})

onMounted(() => {
  loadKnowledgeOptions()
  const qKnowledgeId = route.query.knowledgeId
  if (qKnowledgeId) {
    form.value.knowledgeIds = [Number(qKnowledgeId)]
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

async function handleSearch() {
  if (form.value.knowledgeIds.length === 0) {
    ElMessage.warning('请选择至少1个知识库')
    return
  }
  if (!form.value.query.trim()) {
    ElMessage.warning('请输入查询文本')
    return
  }

  loading.value = true
  searched.value = true
  try {
    results.value = await searchKnowledge({
      query: form.value.query.trim(),
      knowledgeIds: form.value.knowledgeIds,
      enableRerank: form.value.enableRerank,
      confidence: form.value.confidence,
      limit: 10,
    })
  } catch {
    results.value = []
  } finally {
    loading.value = false
  }
}

function formatScore(score?: number): string {
  if (score == null) return '-'
  return score.toFixed(4)
}
</script>

<style scoped>
.page-container {
  max-width: 1200px;
}

.operation-area {
  margin-bottom: 20px;
  padding: 20px 24px;
}

.recall-form {
  margin-bottom: 0;
}

.recall-btn-col {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 0 4px;
}

.result-count {
  font-size: 14px;
  color: var(--ciff-text-secondary);
}

.result-card {
  margin-bottom: 12px;
  padding: 16px 20px;
}

.result-content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--ciff-text-primary);
  margin-bottom: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 20px;
  padding-top: 12px;
  border-top: 1px solid var(--ciff-border-light);
}

.meta-item {
  font-size: 13px;
  color: var(--ciff-neutral-500);
  font-family: var(--ciff-font-mono), monospace;
}
</style>
