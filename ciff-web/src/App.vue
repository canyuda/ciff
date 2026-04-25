<template>
  <div v-if="isLoginPage" class="app-login">
    <router-view />
  </div>
  <div v-else class="app-layout">
    <Sidebar />
    <div class="app-layout__main">
      <TopBar />
      <div class="app-layout__content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from '@/layouts/AppLayout.vue'
import TopBar from '@/components/TopBar.vue'

const route = useRoute()
const isLoginPage = computed(() => route.path === '/login')
</script>

<style scoped>
.app-login {
  height: 100vh;
}

.app-layout {
  display: flex;
  height: 100vh;
  background: var(--ciff-bg-page);
}

.app-layout__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-layout__content {
  flex: 1;
  overflow-y: auto;
  background: var(--ciff-bg-secondary);
  padding: var(--ciff-page-padding);
}
</style>
