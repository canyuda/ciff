<template>
  <div class="ciff-table">
    <el-table
      v-loading="loading"
      :data="rows"
      style="width: 100%"
      :empty-text="undefined"
    >
      <el-table-column
        v-for="col in columns"
        :key="col.prop ?? col.slot"
        :label="col.label"
        :prop="col.prop"
        :width="col.width"
        :min-width="col.minWidth"
        :align="col.align"
        :fixed="col.fixed"
      >
        <template v-if="col.slot" #default="scope">
          <slot :name="col.slot" v-bind="scope" />
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无数据" :image-size="80" />
      </template>
    </el-table>

    <div v-if="showPagination && total > 0" class="ciff-table__pagination">
      <span class="ciff-table__total">总条数 {{ total }}</span>
      <el-select v-model="currentPageSize" class="ciff-table__sizes">
        <el-option v-for="s in [10, 20, 50]" :key="s" :label="`每页条数: ${s}`" :value="s" />
      </el-select>
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="currentPageSize"
        :total="total"
        layout="prev, pager, next"
        background
      />
    </div>
  </div>
</template>

<script setup lang="ts" generic="T extends object">
import { ref, watch } from 'vue'
import type { TableColumn, PageParams } from '@/types/common'

const props = withDefaults(
  defineProps<{
    columns: TableColumn[]
    api: (params: PageParams) => Promise<{ list: T[]; total: number }>
    showPagination?: boolean
    immediate?: boolean
  }>(),
  {
    showPagination: true,
    immediate: true,
  },
)

const rows = ref<T[]>([]) as { value: T[] }
const total = ref(0)
const loading = ref(false)
const currentPage = ref(1)
const currentPageSize = ref(10)

async function fetchData() {
  loading.value = true
  try {
    const result = await props.api({
      page: currentPage.value,
      pageSize: currentPageSize.value,
    })
    rows.value = result.list
    total.value = result.total
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function refresh() {
  if (currentPage.value !== 1) {
    currentPage.value = 1
    return
  }
  fetchData()
}

watch([currentPage, currentPageSize], () => {
  fetchData()
})

if (props.immediate) {
  fetchData()
}

defineExpose({ refresh, fetchData })
</script>

<style scoped>
.ciff-table__pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--ciff-space-4);
  padding-top: var(--ciff-space-4);
}

.ciff-table__total {
  color: var(--el-text-color-regular);
  font-size: 13px;
  white-space: nowrap;
}

.ciff-table__sizes {
  width: 150px;
}
</style>
