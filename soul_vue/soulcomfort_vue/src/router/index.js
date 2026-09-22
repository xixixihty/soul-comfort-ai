import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import ChatView from '../views/ChatView.vue'

const routes = [
  {
    path: '/welcome',
    name: 'Welcome',
    component: () => import('../views/WelcomeView.vue'),
    meta: { guest: true, title: '走向甜弈 - 心灵治愈助手' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { guest: true, title: '登录 - 甜弈' }
  },
  {
    path: '/',
    name: 'Chat',
    component: ChatView,
    meta: { requiresAuth: true, title: '心灵对话 - 甜弈' }
  },
  {
    path: '/conversations',
    name: 'Conversations',
    component: () => import('../views/ConversationView.vue'),
    meta: { requiresAuth: true, title: '对话记录 - 甜弈' }
  },
  {
    path: '/diary',
    name: 'DiaryList',
    component: () => import('../views/DiaryView.vue'),
    meta: { requiresAuth: true, title: '心情日记 - 甜弈' }
  },
  {
    path: '/diary/write',
    name: 'DiaryWrite',
    component: () => import('../views/DiaryWriteView.vue'),
    meta: { requiresAuth: true, title: '写日记 - 甜弈' }
  },
  {
    path: '/diary/:id/edit',
    name: 'DiaryEdit',
    component: () => import('../views/DiaryWriteView.vue'),
    meta: { requiresAuth: true, title: '编辑日记 - 甜弈' }
  },
  {
    path: '/checkin-history',
    name: 'CheckinHistory',
    component: () => import('../views/CheckinHistoryView.vue'),
    meta: { requiresAuth: true, title: '签到记录 - 甜弈' }
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('../views/AboutView.vue'),
    meta: { standalone: true, title: '关于甜弈' }
  },
  {
    path: '/services',
    name: 'Services',
    component: () => import('../views/ServicesView.vue'),
    meta: { standalone: true, title: '服务与功能 - 甜弈' }
  },
  {
    path: '/contact',
    name: 'Contact',
    component: () => import('../views/ContactView.vue'),
    meta: { standalone: true, title: '联系开发者 - 甜弈' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.title) {
    document.title = to.meta.title
  } else {
    document.title = '甜弈 - 心灵治愈助手'
  }

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.guest && authStore.isLoggedIn) {
    next({ name: 'Chat' })
  } else {
    next()
  }
})

export default router