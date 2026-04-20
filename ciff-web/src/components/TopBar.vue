<template>
  <header class="topbar">
    <!-- Breadcrumb -->
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item v-if="currentLabel">{{ currentLabel }}</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- User -->
    <div class="topbar__user">
      <el-avatar :size="28" class="topbar__avatar">A</el-avatar>
      <span class="topbar__username">Admin</span>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const breadcrumbMap: Record<string, string> = {
  '/provider': '供应商管理',
  '/tool': '工具管理',
  '/knowledge': '知识库管理',
  '/knowledge-documents': '文档管理',
  '/recall-test': '召回测试',
  '/agent': 'Agent 管理',
  '/chat': '对话',
}

const currentLabel = computed(() => {
  if (breadcrumbMap[route.path]) {
    return breadcrumbMap[route.path]
  }
  for (const [path, label] of Object.entries(breadcrumbMap)) {
    if (route.path.startsWith(path + '/')) {
      return label
    }
  }
  return ''
})
</script>

<style scoped>
.topbar {
  height: var(--ciff-header-height);
  padding: 0 var(--ciff-page-padding);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--ciff-bg-card);
  border-bottom: 1px solid var(--ciff-border-light);
  flex-shrink: 0;
}

.topbar__user {
  display: flex;
  align-items: center;
  gap: var(--ciff-space-2);
}

.topbar__avatar {
  background: var(--ciff-btn-gradient);
  color: #fff;
  font-size: var(--ciff-text-xs);
  font-weight: var(--ciff-font-semibold);
}

.topbar__username {
  font-size: var(--ciff-text-sm);
  color: var(--ciff-text-primary);
  font-weight: var(--ciff-font-medium);
}
</style>
