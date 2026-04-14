<template>
  <el-container class="app-layout">
    <el-aside width="200px">
      <div class="logo">
        <img src="@/assets/logo.svg" alt="Ciff" class="logo-icon" />
        <span>Ciff</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
      >
        <el-menu-item index="/provider">
          <span>模型提供商管理</span>
        </el-menu-item>
        <el-menu-item index="/agent">
          <span>Agent 管理</span>
        </el-menu-item>
        <el-menu-item index="/chat">
          <span>对话</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

.el-aside {
  border-right: 1px solid #e4e7ed;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 20px;
  font-weight: bold;
  border-bottom: 1px solid #e4e7ed;
}

.logo-icon {
  width: 28px;
  height: 28px;
}

.el-menu {
  border-right: none;
}
</style>
