<template>
  <div class="page-header">
    <div class="page-header__left">
      <div class="page-header__breadcrumbs" v-if="breadcrumbs && breadcrumbs.length > 0">
        <span
          v-for="(crumb, index) in breadcrumbs"
          :key="index"
          :class="['breadcrumb-item', { 'breadcrumb-item--last': index === breadcrumbs.length - 1 }]"
        >
          {{ crumb }}
          <span v-if="index < breadcrumbs.length - 1" class="breadcrumb-separator">/</span>
        </span>
      </div>
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
  breadcrumbs?: string[]
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--ciff-space-4);
  margin-bottom: var(--ciff-space-6);
  padding-bottom: var(--ciff-space-4);
  border-bottom: 1px solid var(--ciff-border-light);
}

.page-header__breadcrumbs {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: var(--ciff-space-2);
}

.breadcrumb-item {
  font-size: var(--ciff-text-xs);
  color: var(--ciff-text-tertiary);
  font-weight: var(--ciff-font-medium);
}

.breadcrumb-item--last {
  color: var(--ciff-primary-500);
}

.breadcrumb-separator {
  margin-left: 6px;
  color: var(--ciff-border-dark);
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
