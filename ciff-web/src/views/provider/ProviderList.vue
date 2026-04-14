<template>
  <div class="provider-list">
    <h2>模型提供商管理</h2>
    <el-tag v-if="status === 'connected'" type="success">
      后端已连接：Ciff is running
    </el-tag>
    <el-tag v-else-if="status === 'disconnected'" type="danger">
      后端未连接
    </el-tag>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getHealth } from '@/api/health'

const status = ref<'connected' | 'disconnected' | 'pending'>('pending')

onMounted(async () => {
  try {
    await getHealth()
    status.value = 'connected'
  } catch {
    status.value = 'disconnected'
  }
})
</script>
