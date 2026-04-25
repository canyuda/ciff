import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      redirect: '/provider',
    },
    {
      path: '/provider',
      name: 'provider',
      component: () => import('@/views/provider/ProviderList.vue'),
    },
    {
      path: '/tool',
      name: 'tool',
      component: () => import('@/views/tool/ToolList.vue'),
    },
    {
      path: '/knowledge',
      name: 'knowledge',
      component: () => import('@/views/knowledge/KnowledgeList.vue'),
    },
    {
      path: '/knowledge-documents',
      name: 'knowledge-documents',
      component: () => import('@/views/knowledge/KnowledgeDocManage.vue'),
    },
    {
      path: '/recall-test',
      name: 'recall-test',
      component: () => import('@/views/knowledge/RecallTest.vue'),
    },
    {
      path: '/agent',
      name: 'agent',
      component: () => import('@/views/agent/AgentList.vue'),
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/views/chat/ChatView.vue'),
    },
    {
      path: '/workflow',
      name: 'workflow',
      component: () => import('@/views/workflow/WorkflowList.vue'),
    },
    {
      path: '/api-keys',
      name: 'api-keys',
      component: () => import('@/views/apikey/ApiKeyList.vue'),
    },
  ],
})

router.beforeEach((to, _from, next) => {
  if (to.meta.public || isAuthenticated()) {
    next()
  } else {
    next({ path: '/login', query: { redirect: to.fullPath } })
  }
})

export default router
