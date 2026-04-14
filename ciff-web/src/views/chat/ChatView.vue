<template>
  <div class="page-container">
    <PageHeader title="对话" description="与 Agent 进行实时对话交互" />

    <div class="ciff-card chat-card">
      <p class="placeholder-text">对话界面</p>
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

.chat-card {
  min-height: calc(100vh - var(--ciff-header-height) - var(--ciff-page-padding) * 2 - 80px);
}

.placeholder-text {
  color: var(--ciff-text-tertiary);
  font-size: var(--ciff-text-sm);
  margin: 0;
  padding: var(--ciff-space-16) 0;
  text-align: center;
}
</style>
