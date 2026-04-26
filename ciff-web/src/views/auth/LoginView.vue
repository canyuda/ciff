<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <img src="@/assets/logo.svg" alt="Ciff" class="login-logo" />
        <h1 class="login-title">Ciff</h1>
        <p class="login-subtitle">AI Agent Platform</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            :prefix-icon="Lock"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-divider">
        <span>or</span>
      </div>

      <a href="/api/auth/github" class="github-btn">
        <svg viewBox="0 0 16 16" width="20" height="20" fill="currentColor">
          <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/>
        </svg>
        GitHub 登录
      </a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { login } from '@/api/auth'
import { setToken, setUser } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 64, message: '用户名长度 2-64 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 128, message: '密码长度 6-128 个字符', trigger: 'blur' },
  ],
}

// Handle GitHub OAuth callback (token in query params)
onMounted(() => {
  const token = route.query.token as string
  if (token) {
    const user = {
      id: 0,
      username: (route.query.username as string) || 'User',
      role: (route.query.role as string) || 'user',
    }
    setToken(token)
    setUser(user)
    ElMessage.success('登录成功')
    router.push('/')
  }
})

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    setToken(res.token)
    setUser(res.user)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e: any) {
    // error handled by request interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ciff-bg-page);
}

.login-card {
  width: 400px;
  padding: var(--ciff-space-8);
  background: var(--ciff-bg-card);
  border-radius: var(--ciff-radius-xl);
  border: 1px solid var(--ciff-border-light);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.login-header {
  text-align: center;
  margin-bottom: var(--ciff-space-6);
}

.login-logo {
  width: 48px;
  height: 48px;
  margin-bottom: var(--ciff-space-2);
}

.login-title {
  font-family: var(--ciff-font-heading);
  font-size: 28px;
  font-weight: var(--ciff-font-extrabold);
  background: linear-gradient(135deg, var(--ciff-primary-400), var(--ciff-primary-300));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
}

.login-subtitle {
  font-size: var(--ciff-text-sm);
  color: var(--ciff-neutral-500);
  margin-top: var(--ciff-space-1);
}

.login-form {
  margin-top: var(--ciff-space-4);
}

.login-btn {
  width: 100%;
}

.login-divider {
  display: flex;
  align-items: center;
  margin: var(--ciff-space-5) 0;
  color: var(--ciff-neutral-400);
  font-size: var(--ciff-text-sm);
}

.login-divider::before,
.login-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--ciff-border-light);
}

.login-divider span {
  padding: 0 var(--ciff-space-3);
}

.github-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--ciff-space-2);
  width: 100%;
  padding: var(--ciff-space-2-5) var(--ciff-space-4);
  border: 1px solid var(--ciff-border-light);
  border-radius: var(--ciff-radius-md);
  background: #24292f;
  color: #fff;
  font-size: var(--ciff-text-sm);
  font-weight: var(--ciff-font-medium);
  text-decoration: none;
  transition: opacity 0.2s;
}

.github-btn:hover {
  opacity: 0.85;
  color: #fff;
}
</style>
