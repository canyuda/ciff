<template>
  <aside class="sidebar" :class="{ 'sidebar--collapsed': collapsed }">
    <!-- Logo -->
    <div class="sidebar__logo">
      <div class="sidebar__logo-icon">
        <img src="@/assets/logo.svg" alt="Ciff" />
      </div>
      <transition name="fade-text">
        <div v-if="!collapsed" class="sidebar__logo-text">
          <span class="sidebar__brand">Ciff</span>
          <span class="sidebar__subtitle">AI Agent Platform</span>
        </div>
      </transition>
    </div>

    <!-- Navigation -->
    <nav class="sidebar__nav">
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        class="sidebar__nav-item"
        :class="{ 'sidebar__nav-item--active': route.path === item.path }"
      >
        <el-icon :size="18">
          <component :is="item.icon" />
        </el-icon>
        <transition name="fade-text">
          <span v-if="!collapsed" class="sidebar__nav-label">{{ item.label }}</span>
        </transition>
      </router-link>
    </nav>

    <!-- Bottom -->
    <div class="sidebar__footer">
      <button class="sidebar__toggle" @click="collapsed = !collapsed">
        <el-icon :size="16">
          <Fold v-if="!collapsed" />
          <Expand v-else />
        </el-icon>
        <transition name="fade-text">
          <span v-if="!collapsed" class="sidebar__toggle-label">收起</span>
        </transition>
      </button>
      <transition name="fade-text">
        <span v-if="!collapsed" class="sidebar__version">v0.1.0</span>
      </transition>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { Setting, Document, Tools, User, ChatDotRound, Collection, Fold, Expand } from '@element-plus/icons-vue'

const route = useRoute()
const collapsed = ref(false)

const menuItems = [
  { path: '/provider', label: '供应商管理', icon: Setting },
  { path: '/model', label: '模型管理', icon: Document },
  { path: '/tool', label: '工具管理', icon: Tools },
  { path: '/knowledge', label: '知识库管理', icon: Collection },
  { path: '/agent', label: 'Agent 管理', icon: User },
  { path: '/chat', label: '对话', icon: ChatDotRound },
]
</script>

<style scoped>
.sidebar {
  width: var(--ciff-sidebar-width);
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--ciff-sidebar-bg);
  border-right: 1px solid var(--ciff-sidebar-border);
  transition: width var(--ciff-duration-normal) var(--ciff-ease-default);
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
}

.sidebar::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 1px;
  height: 100%;
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(99, 102, 241, 0.12) 30%,
    rgba(99, 102, 241, 0.12) 70%,
    transparent
  );
}

.sidebar--collapsed {
  width: var(--ciff-sidebar-collapsed);
}

/* ---- Logo ---- */
.sidebar__logo {
  display: flex;
  align-items: center;
  gap: var(--ciff-space-3);
  padding: var(--ciff-space-4) var(--ciff-space-4);
  height: var(--ciff-sidebar-logo-height);
  border-bottom: 1px solid var(--ciff-sidebar-border);
}

.sidebar__logo-icon {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar__logo-icon img {
  width: 100%;
  height: 100%;
}

.sidebar__logo-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  white-space: nowrap;
}

.sidebar__brand {
  font-family: var(--ciff-font-heading);
  font-size: 22px;
  font-weight: var(--ciff-font-extrabold);
  background: linear-gradient(135deg, var(--ciff-primary-400), var(--ciff-primary-300));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
}

.sidebar__subtitle {
  font-size: var(--ciff-text-xs);
  color: var(--ciff-neutral-500);
  letter-spacing: 0.02em;
  line-height: 1.4;
  margin-top: 1px;
}

/* ---- Navigation ---- */
.sidebar__nav {
  flex: 1;
  padding: var(--ciff-space-2) var(--ciff-space-2);
  display: flex;
  flex-direction: column;
  gap: var(--ciff-space-0-5);
}

.sidebar__nav-item {
  display: flex;
  align-items: center;
  gap: var(--ciff-space-3);
  padding: var(--ciff-space-2-5) var(--ciff-space-3);
  border-radius: var(--ciff-radius-md);
  color: var(--ciff-sidebar-text);
  text-decoration: none;
  font-size: var(--ciff-text-sm);
  font-weight: var(--ciff-font-medium);
  transition: var(--ciff-transition-colors);
  position: relative;
  white-space: nowrap;
  overflow: hidden;
}

.sidebar__nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: var(--ciff-sidebar-text-hover);
}

.sidebar__nav-item:hover .el-icon {
  color: var(--ciff-sidebar-text-hover);
}

/* Active indicator — left border */
.sidebar__nav-item--active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  border-radius: 0 3px 3px 0;
  background: var(--ciff-primary-400);
  box-shadow: 0 0 8px rgba(99, 102, 241, 0.5);
}

.sidebar__nav-item--active {
  background: rgba(99, 102, 241, 0.1);
  color: var(--ciff-sidebar-text-active);
}

.sidebar__nav-item--active .el-icon {
  color: var(--ciff-sidebar-icon-active);
}

.sidebar__nav-item .el-icon {
  flex-shrink: 0;
  color: var(--ciff-sidebar-icon);
  transition: color var(--ciff-duration-normal) var(--ciff-ease-default);
}

.sidebar__nav-label {
  min-width: 0;
}

/* ---- Footer ---- */
.sidebar__footer {
  padding: var(--ciff-space-3) var(--ciff-space-3);
  border-top: 1px solid var(--ciff-sidebar-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar__toggle {
  display: flex;
  align-items: center;
  gap: var(--ciff-space-2);
  padding: var(--ciff-space-1-5) var(--ciff-space-2);
  border: none;
  border-radius: var(--ciff-radius-base);
  background: transparent;
  color: var(--ciff-neutral-500);
  cursor: pointer;
  font-size: var(--ciff-text-xs);
  font-family: var(--ciff-font-body);
  transition: var(--ciff-transition-colors);
}

.sidebar__toggle:hover {
  background: rgba(255, 255, 255, 0.06);
  color: var(--ciff-sidebar-text-hover);
}

.sidebar__version {
  font-size: 11px;
  color: var(--ciff-neutral-600);
  font-family: var(--ciff-font-mono);
}

/* ---- Transitions ---- */
.fade-text-enter-active {
  transition: opacity var(--ciff-duration-normal) var(--ciff-ease-default) var(--ciff-duration-fast);
}

.fade-text-leave-active {
  transition: opacity var(--ciff-duration-fast) var(--ciff-ease-default);
}

.fade-text-enter-from,
.fade-text-leave-to {
  opacity: 0;
}
</style>
