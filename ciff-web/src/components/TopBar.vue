<template>
  <header class="topbar">
    <!-- Breadcrumb -->
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">Home</el-breadcrumb-item>
      <el-breadcrumb-item v-if="currentLabel">{{ currentLabel }}</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- Right side: actions + user -->
    <div class="topbar__right">
      <!-- Quick actions -->
      <div class="topbar__actions">
        <el-tooltip content="新对话" placement="bottom">
          <el-button text circle size="small" @click="goToChat">
            <el-icon><ChatDotRound /></el-icon>
          </el-button>
        </el-tooltip>
      </div>

      <!-- User dropdown -->
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-trigger">
          <el-avatar :size="30" class="user-trigger__avatar">
            {{ userInitial }}
          </el-avatar>
          <span class="user-trigger__name">{{ username }}</span>
          <el-icon class="user-trigger__arrow"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile" disabled>
              <el-icon><User /></el-icon>
              个人资料
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon style="color: var(--ciff-danger)"><SwitchButton /></el-icon>
              <span style="color: var(--ciff-danger)">退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { SwitchButton, ArrowDown, User, ChatDotRound } from '@element-plus/icons-vue'
import { getUser, removeToken } from '@/utils/auth'
import { logout } from '@/api/auth'

const route = useRoute()
const router = useRouter()

const breadcrumbMap: Record<string, string> = {
  '/provider': '供应商管理',
  '/tool': '工具管理',
  '/knowledge': '知识库管理',
  '/knowledge-documents': '文档管理',
  '/recall-test': '召回测试',
  '/agent': 'Agent 管理',
  '/chat': '对话',
  '/workflow': '工作流管理',
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

function goToChat() {
  router.push('/chat')
}

async function handleCommand(command: string) {
  if (command === 'logout') {
    try {
      await logout()
    } catch {
      // ignore
    }
    removeToken()
    router.push('/login')
  }
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

.topbar__right {
  display: flex;
  align-items: center;
  gap: var(--ciff-space-2);
}

.topbar__actions {
  display: flex;
  align-items: center;
  gap: var(--ciff-space-1);
  padding-right: var(--ciff-space-2);
  border-right: 1px solid var(--ciff-border-light);
  margin-right: var(--ciff-space-1);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: var(--ciff-space-2);
  padding: 4px 8px 4px 4px;
  border-radius: var(--ciff-radius-lg);
  cursor: pointer;
  transition: all var(--ciff-duration-fast) var(--ciff-ease-default);
}

.user-trigger:hover {
  background: var(--ciff-neutral-50);
}

.user-trigger__avatar {
  background: var(--ciff-btn-gradient);
  color: #fff;
  font-size: var(--ciff-text-xs);
  font-weight: var(--ciff-font-semibold);
}

.user-trigger__name {
  font-size: var(--ciff-text-sm);
  color: var(--ciff-text-primary);
  font-weight: var(--ciff-font-medium);
}

.user-trigger__arrow {
  font-size: 12px;
  color: var(--ciff-text-tertiary);
}
</style>
