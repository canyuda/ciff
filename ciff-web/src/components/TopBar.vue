<template>
  <header class="topbar">
    <!-- Breadcrumb -->
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">Home</el-breadcrumb-item>
      <el-breadcrumb-item v-if="currentLabel">{{ currentLabel }}</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- User -->
    <div class="topbar__user">
      <el-avatar :size="28" class="topbar__avatar">
        {{ userInitial }}
      </el-avatar>
      <span class="topbar__username">{{ username }}</span>
      <el-button text size="small" @click="handleLogout">
        <el-icon><SwitchButton /></el-icon>
      </el-button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { SwitchButton } from '@element-plus/icons-vue'
import { getUser, removeToken } from '@/utils/auth'
import { logout } from '@/api/auth'

const route = useRoute()
const router = useRouter()

const breadcrumbMap: Record<string, string> = {
  '/provider': 'Providers',
  '/tool': 'Tools',
  '/knowledge': 'Knowledge',
  '/knowledge-documents': 'Documents',
  '/recall-test': 'Recall Test',
  '/agent': 'Agents',
  '/chat': 'Chat',
  '/workflow': 'Workflows',
  '/api-keys': 'API Key',
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

const currentUser = computed(() => getUser())
const username = computed(() => currentUser.value?.username || 'User')
const userInitial = computed(() => username.value.charAt(0).toUpperCase())

async function handleLogout() {
  try {
    await logout()
  } catch {
    // ignore
  }
  removeToken()
  router.push('/login')
}
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
