<template>
  <div class="auth-page">
    <ThemeToggle />
    <div class="auth-card">
      <header class="brand">
        <div class="logo-box">
          <div class="logo-ring"></div>
          <LogoMark :size="64" />
        </div>
        <h1 class="brand-name">甜弈</h1>
        <p class="brand-slogan">心灵治愈助手</p>
      </header>

      <div class="auth-tabs">
        <button
          class="tab-btn"
          :class="{ 'tab-btn--active': activeTab === 'login' }"
          @click="activeTab = 'login'"
        >
          <span>登录</span>
        </button>
        <button
          class="tab-btn"
          :class="{ 'tab-btn--active': activeTab === 'register' }"
          @click="activeTab = 'register'"
        >
          <span>注册</span>
        </button>
      </div>

      <p class="greeting">{{ greetingText }}</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
        class="auth-form"
      >
        <el-form-item prop="username" v-if="activeTab === 'register'">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="username" v-if="activeTab === 'login'">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword" v-if="activeTab === 'register'">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请确认密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item prop="nickname" v-if="activeTab === 'register'">
          <el-input
            v-model="form.nickname"
            placeholder="请输入昵称（选填）"
            :prefix-icon="Sunny"
            size="large"
          />
        </el-form-item>

        <el-form-item class="submit-item">
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            native-type="submit"
          >
            {{ activeTab === 'login' ? '进入甜弈' : '开始旅程' }}
          </el-button>
        </el-form-item>
      </el-form>

      <footer class="page-footer">
        <p class="footer-links">
          <router-link to="/about">关于甜弈</router-link>
          <span class="footer-dot">·</span>
          <router-link to="/services">服务与功能</router-link>
          <span class="footer-dot">·</span>
          <router-link to="/contact">联系开发者</router-link>
        </p>
        <p class="footer-brand">甜弈 · 心灵治愈助手</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Sunny } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import LogoMark from '../components/LogoMark.vue'
import ThemeToggle from '../components/ThemeToggle.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activeTab = ref('login')
const loading = ref(false)
const formRef = ref(null)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h >= 5 && h < 9) return '早安，新的一天从好心情开始'
  if (h >= 9 && h < 12) return '上午好，来和甜弈聊聊吧'
  if (h >= 12 && h < 14) return '午安，记得好好吃饭哦'
  if (h >= 14 && h < 18) return '下午好，今天过得怎么样？'
  if (h >= 18 && h < 22) return '晚上好，让甜弈陪你放松一下'
  return '夜深了，有什么心事都可以和我说'
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  nickname: [
    { max: 20, message: '昵称不能超过 20 个字符', trigger: 'blur' }
  ]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    if (activeTab.value === 'login') {
      await authStore.login(form.username, form.password)
      ElMessage.success('欢迎回来')
    } else {
      await authStore.register(form.username, form.password, form.nickname || form.username)
      ElMessage.success('注册成功')
    }

    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  position: relative;
  z-index: 1;
  overflow: hidden;
}

.auth-card {
  position: relative;
  width: 440px;
  max-width: 100%;
  padding: 24px 8px;
  background: transparent;
  border: none;
  box-shadow: none;
  animation: cardAppear 0.7s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes cardAppear {
  from {
    opacity: 0;
    transform: translateY(28px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.brand {
  text-align: center;
  margin-bottom: 22px;
}

.logo-box {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  margin-bottom: 14px;
}

/* 月晕：logo 背后一圈暖色柔光，按「吸气 4s → 屏息 6s → 呼气 4s」的节奏呼吸 */
.logo-box::before {
  content: '';
  position: absolute;
  inset: -22px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(232,169,193, 0.55) 0%, rgba(232,169,193, 0) 70%);
  pointer-events: none;
  animation: glowBreath 14s linear infinite;
}

@keyframes glowBreath {
  0% { opacity: 0.55; transform: scale(0.94); }
  28.5% { opacity: 1; transform: scale(1.06); }
  71.5% { opacity: 0.92; transform: scale(1.04); }
  100% { opacity: 0.55; transform: scale(0.94); }
}

.logo-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid rgba(232,169,193, 0.45);
  animation: ringPulse 14s linear infinite;
}

@keyframes ringPulse {
  0% { transform: scale(1); opacity: 0.55; }
  28.5% { transform: scale(1.1); opacity: 0.95; }
  71.5% { transform: scale(1.08); opacity: 0.8; }
  100% { transform: scale(1); opacity: 0.55; }
}

.brand-name {
  font-size: 36px;
  font-weight: 800;
  letter-spacing: 10px;
  margin: 0 0 8px;
  color: #a65b80;
  background: linear-gradient(120deg, #c2708f 0%, #e8a9c1 55%, #7fb2cb 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 1px 18px rgba(246,213,226, 0.65);
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.brand-slogan {
  font-size: 13px;
  letter-spacing: 3px;
  margin: 0;
  color: #9a7590;
  text-shadow: 0 1px 10px rgba(255, 255, 255, 0.75);
}

.auth-tabs {
  display: flex;
  gap: 8px;
  padding: 6px;
  margin-bottom: 16px;
  border-radius: 14px;
  background: rgba(232,169,193, 0.12);
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 16px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s;
}

.tab-btn:hover {
  background: rgba(232,169,193, 0.14);
  color: var(--accent-color);
}

.tab-btn--active {
  background: rgba(194,112,143, 0.82);
  backdrop-filter: blur(16px) saturate(1.4);
  -webkit-backdrop-filter: blur(16px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.45);
  color: #fff;
  font-weight: 600;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  box-shadow:
    0 4px 12px rgba(176,95,131, 0.4),
    inset 0 1px 2px rgba(255, 255, 255, 0.35);
}

.greeting {
  text-align: center;
  font-size: 14px;
  line-height: 1.7;
  letter-spacing: 1px;
  margin: 0 0 16px;
  padding: 0 12px;
  color: #5d3a54;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  text-shadow: 0 1px 8px rgba(255, 255, 255, 0.7);
}

.auth-form :deep(.el-input__wrapper) {
  --el-input-focus-border-color: #c2708f;
  --el-input-hover-border-color: #d98bae;
  /* 必须用实心色：Chrome 自动填充会给内层 input 铺不透明白底，
     若外层是半透明底就会露出"双层白"，两者取同一实心色即可融为一体 */
  background: #f9eaf1;
  border-radius: 12px;
  box-shadow:
    0 2px 8px rgba(165,105,140, 0.12),
    inset 0 0 0 1px rgba(232,169,193, 0.45);
}

.auth-form :deep(.el-input__wrapper:hover) {
  box-shadow:
    0 2px 10px rgba(194,112,143, 0.22),
    inset 0 0 0 1px rgba(194,112,143, 0.55);
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px #c2708f inset,
    0 2px 12px rgba(194,112,143, 0.3);
}

.auth-form :deep(.el-input__inner) {
  caret-color: #c2708f;
}

.auth-form :deep(.el-input__prefix) {
  color: #b98aa6;
}

/* 覆盖浏览器自动填充的蓝色高亮：填充色与 wrapper 实心底色保持一致 */
.auth-form :deep(.el-input__wrapper input:-webkit-autofill),
.auth-form :deep(.el-input__wrapper input:-webkit-autofill:hover),
.auth-form :deep(.el-input__wrapper input:-webkit-autofill:focus) {
  -webkit-text-fill-color: #4a3243;
  -webkit-box-shadow: 0 0 0 1000px #f9eaf1 inset !important;
  transition: background-color 9999s ease-out 0s;
}

.submit-item {
  margin-top: 8px;
  margin-bottom: 0;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  letter-spacing: 2px;
  border-radius: 12px;
  background: rgba(194,112,143, 0.74);
  backdrop-filter: blur(20px) saturate(1.4);
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.45);
  color: #fff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  box-shadow:
    0 4px 14px rgba(176,95,131, 0.4),
    inset 0 1px 2px rgba(255, 255, 255, 0.35);
  transition: all 0.3s;
}

.submit-btn:hover {
  background: rgba(176,95,131, 0.88);
  border-color: rgba(255, 255, 255, 0.6);
  transform: translateY(-2px);
  box-shadow:
    0 6px 18px rgba(176,95,131, 0.5),
    inset 0 1px 2px rgba(255, 255, 255, 0.4);
}

.page-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 12px;
  letter-spacing: 1px;
}

.footer-links {
  margin: 0 0 6px;
}

.footer-links a {
  color: #b05f83;
  text-decoration: none;
  letter-spacing: 1px;
  transition: color 0.2s;
}

.footer-links a:hover {
  color: #8e4a6b;
  text-decoration: underline;
}

.footer-dot {
  margin: 0 8px;
  color: #c9a8c0;
}

.footer-brand {
  margin: 0;
  color: #a08a9e;
}

/* 暗色主题适配 */
html.dark .brand-name {
  color: #f4e7f0;
  background: linear-gradient(120deg, #f6d5e2 0%, #efb7cf 55%, #a9cbd9 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 1px 18px rgba(232,169,193, 0.35);
}

html.dark .brand-slogan {
  color: #c9b4c4;
}

html.dark .greeting {
  color: #d8c2d4;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.4);
}

html.dark .auth-form :deep(.el-input__wrapper) {
  background: #2c2330;
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.25),
    inset 0 0 0 1px rgba(232,169,193, 0.42);
}

html.dark .auth-form :deep(.el-input__wrapper:hover) {
  box-shadow:
    0 2px 10px rgba(232,169,193, 0.2),
    inset 0 0 0 1px rgba(232,169,193, 0.48);
}

html.dark .auth-form :deep(.el-input__inner) {
  color: #f4e7f0;
}

html.dark .auth-form :deep(.el-input__wrapper input:-webkit-autofill),
html.dark .auth-form :deep(.el-input__wrapper input:-webkit-autofill:hover),
html.dark .auth-form :deep(.el-input__wrapper input:-webkit-autofill:focus) {
  -webkit-text-fill-color: #f4e7f0;
  -webkit-box-shadow: 0 0 0 1000px #2c2330 inset !important;
}

html.dark .auth-tabs {
  background: rgba(232,169,193, 0.14);
}

/* 暗色下半透明粉底会被深底"吃掉"显得灰淡：登录页按钮改用实色保持醒目 */
html.dark .tab-btn--active {
  background: #c2708f;
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.45),
    inset 0 1px 2px rgba(255, 255, 255, 0.28);
}

html.dark .submit-btn {
  background: #c2708f;
  box-shadow:
    0 4px 14px rgba(0, 0, 0, 0.45),
    inset 0 1px 2px rgba(255, 255, 255, 0.28);
}

html.dark .submit-btn:hover {
  background: #d089a8;
  box-shadow:
    0 6px 18px rgba(0, 0, 0, 0.55),
    inset 0 1px 2px rgba(255, 255, 255, 0.3);
}

html.dark .page-footer .footer-brand {
  color: #7f6b7f;
}

html.dark .footer-links a {
  color: #e8a9c1;
}

html.dark .footer-links a:hover {
  color: #f6d5e2;
}

html.dark .footer-dot {
  color: #6b5568;
}

@media (max-width: 768px) {
  .auth-page {
    padding: 24px 20px;
  }

  .auth-card {
    padding: 24px 4px;
  }

  .brand-name {
    font-size: 28px;
  }
}

/* 用户系统开启“减少动效”时，关闭月晕呼吸动画 */
@media (prefers-reduced-motion: reduce) {
  .logo-box::before,
  .logo-ring {
    animation: none;
  }
}
</style>