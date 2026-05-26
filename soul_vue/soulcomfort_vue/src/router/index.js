import { createRouter, createWebHashHistory } from 'vue-router'
import ChatView from '../views/ChatView.vue'

const routes = [
  {
    path: '/',
    name: 'Chat',
    component: ChatView
  },
  {
    path: '/conversations',
    name: 'Conversations',
    component: () => import('../views/ConversationView.vue')
  },
  {
    path: '/diary',
    name: 'DiaryList',
    component: () => import('../views/DiaryView.vue')
  },
  {
    path: '/diary/write',
    name: 'DiaryWrite',
    component: () => import('../views/DiaryWriteView.vue')
  },
  {
    path: '/diary/:id/edit',
    name: 'DiaryEdit',
    component: () => import('../views/DiaryWriteView.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router