<template>
  <div class="page-container">
    <PageHeader title="Agent 管理" description="创建和管理 AI Agent 智能助手">
      <el-button type="default">导入</el-button>
      <el-button type="primary">创建 Agent</el-button>
    </PageHeader>

    <div class="ciff-card">
      <p class="placeholder-text">Agent 列表</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
</script>

<style scoped>
.page-container {
  max-width: var(--ciff-content-max-width);
}

.placeholder-text {
  color: var(--ciff-text-tertiary);
  font-size: var(--ciff-text-sm);
  margin: 0;
  padding: var(--ciff-space-8) 0;
  text-align: center;
}
</style>
