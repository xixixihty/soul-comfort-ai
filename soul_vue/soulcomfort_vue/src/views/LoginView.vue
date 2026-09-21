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
        <p class="brand-slogan">你的私人心灵治愈助手</p>
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

      <!-- 呼吸倒计时：独立居中一行（表单上方），跟随月晕「吸气→屏息→呼气」节奏 -->
      <div class="breath-coach-row" v-if="!reduced" aria-live="polite">
        <div class="breath-coach">
          <span class="coach-dot" :data-phase="phaseKey"></span>
          <span class="coach-text">{{
            phaseKey === 'inhale' ? '跟着月晕吸气' : phaseKey === 'hold' ? '轻轻屏住呼吸' : '缓缓呼出'
          }}</span>
          <span class="coach-count" :class="`coach-count--${phaseKey}`">{{ breathCount }}<small>s</small></span>
        </div>
      </div>

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

      <footer class="page-footer">甜弈 · 心灵治愈助手</footer>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
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

/* 呼吸倒计时：与 logo 月晕一致的节奏（吸气 4s → 屏息 6s → 呼气 4s） */
const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
const phaseKey = ref('inhale')
const breathCount = ref(4)
const BREATH_CYCLE = [
  { key: 'inhale', seconds: 4 },
  { key: 'hold', seconds: 6 },
  { key: 'exhale', seconds: 4 }
]
let breathTimers = []

function runBreathPhase(idx) {
  const phase = BREATH_CYCLE[idx]
  phaseKey.value = phase.key
  let remain = phase.seconds
  breathCount.value = remain
  const interval = setInterval(() => {
    remain -= 1
    breathCount.value = remain
    if (remain <= 0) {
      clearInterval(interval)
      runBreathPhase((idx + 1) % BREATH_CYCLE.length)
    }
  }, 1000)
  breathTimers.push(interval)
}

onMounted(() => {
  if (!reduced) runBreathPhase(0)
})
onUnmounted(() => breathTimers.forEach((t) => clearInterval(t)))

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

/* 月晕：logo 背后一圈暖色柔光，按「吸气 4s → 屏息 6s → 呼气 4s」的节奏呼吸（与呼吸倒计时同步） */
.logo-box::before {
  content: '';
  position: absolute;
  inset: -22px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 200, 130, 0.55) 0%, rgba(255, 200, 130, 0) 70%);
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
  border: 2px solid rgba(212, 163, 115, 0.45);
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
  color: #8a4b2e;
  background: linear-gradient(120deg, #8a4b2e 0%, #c97f4a 55%, #d4a373 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 1px 18px rgba(255, 224, 186, 0.65);
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.brand-slogan {
  font-size: 13px;
  letter-spacing: 3px;
  margin: 0;
  color: #9a6f4c;
  text-shadow: 0 1px 10px rgba(255, 255, 255, 0.75);
}

.auth-tabs {
  display: flex;
  gap: 8px;
  padding: 6px;
  margin-bottom: 16px;
  border-radius: 14px;
  background: rgba(212, 163, 115, 0.12);
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
  background: rgba(212, 163, 115, 0.14);
  color: var(--accent-color);
}

.tab-btn--active {
  background: rgba(255, 138, 61, 0.68);
  backdrop-filter: blur(16px) saturate(1.4);
  -webkit-backdrop-filter: blur(16px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.55);
  color: #fff;
  font-weight: 600;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  box-shadow:
    0 4px 12px rgba(255, 130, 40, 0.35),
    inset 0 1px 2px rgba(255, 255, 255, 0.35);
}

.greeting {
  text-align: center;
  font-size: 14px;
  line-height: 1.7;
  letter-spacing: 1px;
  margin: 0 0 16px;
  padding: 0 12px;
  color: #6b4a33;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  text-shadow: 0 1px 8px rgba(255, 255, 255, 0.7);
}

/* 呼吸倒计时独立行：横贯卡片居中，作为表单上方的氛围元素（不再与按钮挤一行） */
.breath-coach-row {
  display: flex;
  justify-content: center;
  margin: 0 0 18px;
}

.breath-coach {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px 6px 8px;
  flex-shrink: 0;
  border-radius: 999px;
  background: rgba(255, 246, 236, 0.6);
  border: 1px solid rgba(212, 163, 115, 0.28);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  box-shadow: 0 2px 10px rgba(160, 110, 60, 0.1);
}

.coach-dot {
  width: 10px;
  height: 10px;
  flex: none;
  border-radius: 50%;
  background: #ff9d5c;
  box-shadow: 0 0 0 3px rgba(255, 157, 92, 0.18);
  transition: background 0.4s, box-shadow 0.4s;
}

.coach-dot[data-phase='hold'] {
  background: #e8b873;
  box-shadow: 0 0 0 3px rgba(232, 184, 115, 0.22);
}

.coach-dot[data-phase='exhale'] {
  background: #c97f4a;
  box-shadow: 0 0 0 3px rgba(201, 127, 74, 0.22);
}

.coach-text {
  font-size: 13px;
  letter-spacing: 1px;
  color: #7a5b42;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  white-space: nowrap;
}

.coach-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 30px;
  padding: 0 5px;
  border-radius: 999px;
  background: rgba(255, 157, 92, 0.16);
  color: #b06a35;
  font-size: 15px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  transition: background 0.4s, color 0.4s;
}

.coach-count--hold {
  background: rgba(232, 184, 115, 0.18);
  color: #a8762f;
}

.coach-count--exhale {
  background: rgba(201, 127, 74, 0.2);
  color: #93552b;
}

.coach-count small {
  font-size: 10px;
  font-weight: 500;
  margin-left: 1px;
}

.auth-form :deep(.el-input__wrapper) {
  --el-input-focus-border-color: #d4a373;
  --el-input-hover-border-color: #e0b790;
  background: rgba(255, 247, 238, 0.68);
  border-radius: 12px;
  box-shadow:
    0 2px 8px rgba(160, 110, 60, 0.1),
    inset 0 0 0 1px rgba(212, 163, 115, 0.32);
}

.auth-form :deep(.el-input__wrapper:hover) {
  box-shadow:
    0 2px 10px rgba(212, 163, 115, 0.22),
    inset 0 0 0 1px rgba(212, 163, 115, 0.5);
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px #d4a373 inset,
    0 2px 12px rgba(212, 163, 115, 0.3);
}

.auth-form :deep(.el-input__inner) {
  caret-color: #d4a373;
}

.auth-form :deep(.el-input__prefix) {
  color: #c9a88c;
}

/* 覆盖浏览器自动填充的蓝色高亮 */
.auth-form :deep(.el-input__wrapper input:-webkit-autofill),
.auth-form :deep(.el-input__wrapper input:-webkit-autofill:hover),
.auth-form :deep(.el-input__wrapper input:-webkit-autofill:focus) {
  -webkit-text-fill-color: #5c3d2e;
  -webkit-box-shadow: 0 0 0 1000px #fff7ee inset !important;
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
  background: rgba(255, 138, 61, 0.6);
  backdrop-filter: blur(20px) saturate(1.4);
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.5);
  color: #fff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  box-shadow:
    0 4px 14px rgba(255, 130, 40, 0.35),
    inset 0 1px 2px rgba(255, 255, 255, 0.35);
  transition: all 0.3s;
}

.submit-btn:hover {
  background: rgba(255, 122, 41, 0.75);
  border-color: rgba(255, 255, 255, 0.65);
  transform: translateY(-2px);
  box-shadow:
    0 6px 18px rgba(255, 130, 40, 0.45),
    inset 0 1px 2px rgba(255, 255, 255, 0.4);
}

.page-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 12px;
  color: #9a8771;
  letter-spacing: 1px;
}

/* 暗色主题适配 */
html.dark .brand-name {
  color: #f0e0ce;
  background: linear-gradient(120deg, #f0c89a 0%, #e3a878 55%, #d4a373 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 1px 18px rgba(255, 210, 160, 0.35);
}

html.dark .brand-slogan {
  color: #c7b2a0;
}

html.dark .breath-coach {
  background: rgba(40, 32, 24, 0.55);
  border-color: rgba(212, 163, 115, 0.3);
}

html.dark .coach-text {
  color: #d9c3a9;
}

html.dark .coach-count {
  color: #f0c89a;
}

html.dark .coach-count--hold {
  color: #e8c483;
}

html.dark .coach-count--exhale {
  color: #d9a06b;
}

html.dark .greeting {
  color: #cbb49c;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.4);
}

html.dark .auth-form :deep(.el-input__wrapper) {
  background: rgba(40, 32, 24, 0.6);
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.25),
    inset 0 0 0 1px rgba(212, 163, 115, 0.3);
}

html.dark .auth-form :deep(.el-input__wrapper:hover) {
  box-shadow:
    0 2px 10px rgba(212, 163, 115, 0.2),
    inset 0 0 0 1px rgba(212, 163, 115, 0.48);
}

html.dark .auth-form :deep(.el-input__inner) {
  color: #f0e0ce;
}

html.dark .auth-form :deep(.el-input__wrapper input:-webkit-autofill),
html.dark .auth-form :deep(.el-input__wrapper input:-webkit-autofill:hover),
html.dark .auth-form :deep(.el-input__wrapper input:-webkit-autofill:focus) {
  -webkit-text-fill-color: #f0e0ce;
  -webkit-box-shadow: 0 0 0 1000px #2a211b inset !important;
}

html.dark .auth-tabs {
  background: rgba(212, 163, 115, 0.14);
}

html.dark .page-footer {
  color: #84705f;
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

  .coach-text {
    font-size: 12px;
  }
}

/* 用户系统开启“减少动效”时，关闭呼吸动画与倒计时 */
@media (prefers-reduced-motion: reduce) {
  .logo-box::before,
  .logo-ring {
    animation: none;
  }
}
</style>