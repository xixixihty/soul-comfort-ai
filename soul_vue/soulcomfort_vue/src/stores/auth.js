import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('soul_token')
    if (token && !config.url.includes('/login') && !config.url.includes('/register')) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('soul_token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('soul_user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const nickname = computed(() => userInfo.value?.nickname || '朋友')
  const username = computed(() => userInfo.value?.username || '')
  const avatarUrl = computed(() => userInfo.value?.avatarUrl || '')

  function setAuth(authToken, user) {
    token.value = authToken
    userInfo.value = user
    localStorage.setItem('soul_token', authToken)
    localStorage.setItem('soul_user', JSON.stringify(user))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('soul_token')
    localStorage.removeItem('soul_user')
    window.location.hash = '#/login'
  }

  async function login(username, password) {
    const res = await api.post('/auth/login', { username, password })
    if (res.data.code !== 0) {
      throw new Error(res.data.message || '登录失败')
    }
    setAuth(res.data.data.token, res.data.data.user)
    return res.data.data
  }

  async function register(username, password, nickname) {
    const res = await api.post('/auth/register', { username, password, nickname })
    if (res.data.code !== 0) {
      throw new Error(res.data.message || '注册失败')
    }
    setAuth(res.data.data.token, res.data.data.user)
    return res.data.data
  }

  async function fetchUserInfo() {
    try {
      const res = await api.get('/auth/me', {
        headers: { Authorization: `Bearer ${token.value}` }
      })
      if (res.data.code === 0) {
        userInfo.value = res.data.data
        localStorage.setItem('soul_user', JSON.stringify(res.data.data))
      }
    } catch {
      logout()
    }
  }

  /** 上传头像：文件交给后端存阿里云 OSS，返回的图片地址写回本地用户信息 */
  async function uploadAvatar(file) {
    const form = new FormData()
    form.append('file', file)
    const res = await axios.post('/api/user/avatar', form, {
      headers: {
        Authorization: `Bearer ${token.value}`,
        'Content-Type': 'multipart/form-data'
      },
      timeout: 30000
    })
    if (res.data.code !== 0) {
      throw new Error(res.data.message || '头像上传失败')
    }
    const url = res.data.data?.avatarUrl || ''
    userInfo.value = { ...(userInfo.value || {}), avatarUrl: url }
    localStorage.setItem('soul_user', JSON.stringify(userInfo.value))
    return url
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    nickname,
    username,
    avatarUrl,
    setAuth,
    logout,
    login,
    register,
    fetchUserInfo,
    uploadAvatar
  }
})