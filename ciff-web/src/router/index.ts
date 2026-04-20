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
  ],
})

export default router
