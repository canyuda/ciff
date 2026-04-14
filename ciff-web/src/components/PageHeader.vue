<template>
  <div class="page-header">
    <div class="page-header__left">
      <h2 class="page-header__title">{{ title }}</h2>
      <p v-if="description" class="page-header__desc">{{ description }}</p>
    </div>
    <div class="page-header__actions">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--ciff-space-4);
  margin-bottom: var(--ciff-space-6);
}

.page-header__title {
  font-family: var(--ciff-font-heading);
  font-size: var(--ciff-text-2xl);
  font-weight: var(--ciff-font-bold);
  color: var(--ciff-text-primary);
  line-height: var(--ciff-leading-tight);
  margin: 0;
}

.page-header__desc {
  font-size: var(--ciff-text-sm);
  color: var(--ciff-text-secondary);
  margin-top: var(--ciff-space-1);
  margin-bottom: 0;
  line-height: var(--ciff-leading-normal);
}

.page-header__actions {
  display: flex;
  align-items: center;
  gap: var(--ciff-space-2);
  flex-shrink: 0;
}
</style>
