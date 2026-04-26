<template>
  <div class="login-page">
    <!-- Left: Brand showcase -->
    <div class="login-brand">
      <div class="brand-bg">
        <div class="brand-bg__orb brand-bg__orb--1" />
        <div class="brand-bg__orb brand-bg__orb--2" />
        <div class="brand-bg__orb brand-bg__orb--3" />
      </div>
      <div class="brand-content">
        <div class="brand-logo">
          <img src="@/assets/logo.svg" alt="Ciff" />
        </div>
        <h1 class="brand-title">
          Ciff
        </h1>
        <p class="brand-subtitle">AI Agent Platform</p>
        <p class="brand-desc">
          为团队打造的 AI Agent 开发与运行平台<br>
          简化流程，释放智能潜能
        </p>
        <div class="brand-features">
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon :size="18"><ChatDotRound /></el-icon>
            </div>
            <span>智能对话</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon :size="18"><Tools /></el-icon>
            </div>
            <span>工具调用</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon :size="18"><Share /></el-icon>
            </div>
            <span>工作流编排</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Right: Login form -->
    <div class="login-form-section">
      <div class="login-card">
        <div class="login-header">
          <h2 class="login-title">欢迎回来</h2>
          <p class="login-desc">登录您的 Ciff 账户</p>
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
          <span>或使用以下方式</span>
        </div>

        <a href="/api/auth/github" class="github-btn">
          <svg viewBox="0 0 16 16" width="18" height="18" fill="currentColor">
            <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/>
          </svg>
          GitHub 登录
        </a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock, ChatDotRound, Tools, Share } from '@element-plus/icons-vue'
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
  background: var(--ciff-bg-page);
}

/* ===== Left brand section ===== */
.login-brand {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: linear-gradient(135deg, var(--ciff-neutral-900) 0%, #1a1f3c 50%, var(--ciff-primary-900) 100%);
  overflow: hidden;
}

.brand-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.brand-bg__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
}

.brand-bg__orb--1 {
  width: 400px;
  height: 400px;
  background: var(--ciff-primary-500);
  top: -100px;
  right: -100px;
}

.brand-bg__orb--2 {
  width: 300px;
  height: 300px;
  background: var(--ciff-accent-400);
  bottom: 10%;
  left: -80px;
  opacity: 0.2;
}

.brand-bg__orb--3 {
  width: 200px;
  height: 200px;
  background: var(--ciff-primary-300);
  top: 40%;
  right: 20%;
  opacity: 0.15;
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: var(--ciff-space-8);
  max-width: 480px;
}

.brand-logo {
  width: 64px;
  height: 64px;
  margin: 0 auto var(--ciff-space-4);
  padding: 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--ciff-radius-xl);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.brand-logo img {
  width: 100%;
  height: 100%;
}

.brand-title {
  font-family: var(--ciff-font-heading);
  font-size: 48px;
  font-weight: var(--ciff-font-extrabold);
  background: linear-gradient(135deg, #fff 0%, var(--ciff-primary-300) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  line-height: 1.1;
}

.brand-subtitle {
  font-size: var(--ciff-text-lg);
  color: rgba(255, 255, 255, 0.6);
  margin-top: var(--ciff-space-2);
  letter-spacing: 0.05em;
}

.brand-desc {
  font-size: var(--ciff-text-sm);
  color: rgba(255, 255, 255, 0.45);
  margin-top: var(--ciff-space-4);
  line-height: 1.7;
}

.brand-features {
  display: flex;
  justify-content: center;
  gap: var(--ciff-space-6);
  margin-top: var(--ciff-space-8);
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ciff-space-2);
  color: rgba(255, 255, 255, 0.6);
  font-size: var(--ciff-text-xs);
}

.feature-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  border-radius: var(--ciff-radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: var(--ciff-primary-300);
  transition: all var(--ciff-duration-normal) var(--ciff-ease-default);
}

.feature-item:hover .feature-icon {
  background: rgba(255, 255, 255, 0.12);
  transform: translateY(-2px);
}

/* ===== Right form section ===== */
.login-form-section {
  width: 460px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--ciff-space-8);
  background: var(--ciff-bg-page);
}

.login-card {
  width: 100%;
  max-width: 380px;
}

.login-header {
  margin-bottom: var(--ciff-space-6);
}

.login-title {
  font-family: var(--ciff-font-heading);
  font-size: var(--ciff-text-2xl);
  font-weight: var(--ciff-font-bold);
  color: var(--ciff-text-primary);
  margin: 0;
}

.login-desc {
  font-size: var(--ciff-text-sm);
  color: var(--ciff-text-secondary);
  margin-top: var(--ciff-space-1);
}

.login-form :deep(.el-form-item__label) {
  font-weight: var(--ciff-font-medium);
  font-size: var(--ciff-text-sm);
  padding-bottom: 6px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: var(--ciff-text-base);
  margin-top: var(--ciff-space-2);
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
  background: var(--ciff-border);
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
  padding: 11px var(--ciff-space-4);
  border: 1px solid var(--ciff-border);
  border-radius: var(--ciff-radius-md);
  background: var(--ciff-bg-card);
  color: var(--ciff-text-primary);
  font-size: var(--ciff-text-sm);
  font-weight: var(--ciff-font-medium);
  text-decoration: none;
  transition: all var(--ciff-duration-normal) var(--ciff-ease-default);
}

.github-btn:hover {
  border-color: var(--ciff-neutral-400);
  background: var(--ciff-neutral-50);
  color: var(--ciff-text-primary);
  transform: translateY(-1px);
  box-shadow: var(--ciff-shadow-sm);
}

/* ===== Responsive ===== */
@media (max-width: 900px) {
  .login-brand {
    display: none;
  }

  .login-form-section {
    width: 100%;
    padding: var(--ciff-space-6);
  }
}

@media (max-width: 480px) {
  .login-form-section {
    padding: var(--ciff-space-4);
  }

  .login-card {
    max-width: 100%;
  }
}
</style>
