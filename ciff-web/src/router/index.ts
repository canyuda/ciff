import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
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
      path: '/model',
      name: 'model',
      component: () => import('@/views/model/ModelList.vue'),
    },
    {
      path: '/tool',
      name: 'tool',
      component: () => import('@/views/tool/ToolList.vue'),
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
  ],
})

export default router
