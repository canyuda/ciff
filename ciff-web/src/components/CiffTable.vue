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
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="currentPageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
      />
    </div>
  </div>
</template>

<script setup lang="ts" generic="T extends Record<string, unknown>">
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
  currentPage.value = 1
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
  justify-content: flex-end;
  padding-top: var(--ciff-space-4);
}
</style>
