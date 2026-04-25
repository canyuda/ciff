<template>
  <div class="empty-state">
    <el-icon :size="48" class="empty-state__icon">
      <component :is="icon" />
    </el-icon>
    <p class="empty-state__title">{{ title }}</p>
    <p v-if="description" class="empty-state__desc">{{ description }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { FolderOpened } from '@element-plus/icons-vue'

withDefaults(defineProps<{
  title?: string
  description?: string
  icon?: any
}>(), {
  title: 'No data',
  icon: FolderOpened,
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--ciff-space-10) var(--ciff-space-4);
  color: var(--ciff-neutral-400);
}

.empty-state__icon {
  margin-bottom: var(--ciff-space-3);
  color: var(--ciff-neutral-300);
}

.empty-state__title {
  font-size: var(--ciff-text-base);
  font-weight: var(--ciff-font-medium);
  color: var(--ciff-neutral-500);
  margin: 0;
}

.empty-state__desc {
  font-size: var(--ciff-text-sm);
  color: var(--ciff-neutral-400);
  margin: var(--ciff-space-1) 0 0;
}
</style>
