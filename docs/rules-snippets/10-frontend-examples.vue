<!-- 标准列表页 -->
<template>
  <div class="page-container">
    <PageHeader title="xxx管理" description="xxx">
      <el-button type="primary" @click="dialogRef?.open()">新增</el-button>
    </PageHeader>
    <div class="ciff-card">
      <CiffTable ref="tableRef" :columns="columns" :api="fetchList">
        <template #actions="{ row }">
          <el-button link type="primary" @click="dialogRef?.open(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </CiffTable>
    </div>
    <CiffFormDialog ref="dialogRef" title="xxx" :rules="rules" @submit="handleSubmit">
      <template #default="{ data }">
        <!-- 表单项 -->
      </template>
    </CiffFormDialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CiffTable from '@/components/CiffTable.vue'
import CiffFormDialog from '@/components/CiffFormDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useConfirm } from '@/composables/useConfirm'
import type { TableColumn } from '@/types/common'
import type { FormRules } from 'element-plus'

const tableRef = ref()
const dialogRef = ref()
const { confirm } = useConfirm()

const columns: TableColumn[] = [ /* ... */ ]
const rules: FormRules = { /* ... */ }

async function fetchList(params: { page: number; pageSize: number }) {
  /* 调 API，返回 { list, total } */
}

async function handleSubmit(data: any, done: () => void) {
  /* 调新增/编辑 API -> done() -> refresh() */
}

async function handleDelete(id: number) {
  await confirm('确定删除？', () => /* 调删除 API */)
  tableRef.value?.refresh()
}
</script>

<style scoped>
.page-container { max-width: var(--ciff-content-max-width) }
</style>
