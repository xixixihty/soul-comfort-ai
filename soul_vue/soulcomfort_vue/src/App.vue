<template>
  <div class="bg-layer" aria-hidden="true"></div>
  <router-view v-if="$route.meta.guest" />
  <div v-else class="app-layout" :class="{ 'app-layout--dark': isDark }">
    <aside class="sidebar">
      <div class="sidebar-logo">
        <LogoMark :size="28" />
        <span class="logo-text">甜弈</span>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item" active-class="nav-item--active">
          <el-icon :size="20"><ChatDotRound /></el-icon>
          <span>聊天</span>
        </router-link>
        <router-link to="/conversations" class="nav-item" active-class="nav-item--active">
          <el-icon :size="20"><List /></el-icon>
          <span>对话记录</span>
        </router-link>
        <router-link to="/diary" class="nav-item" active-class="nav-item--active">
          <el-icon :size="20"><Notebook /></el-icon>
          <span>心情日记</span>
        </router-link>
        <router-link to="/checkin-history" class="nav-item" active-class="nav-item--active">
          <el-icon :size="20"><Calendar /></el-icon>
          <span>签到记录</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="sidebar-user" v-if="authStore.isLoggedIn">
          <div class="sidebar-avatar" @click="triggerAvatarUpload" title="点击更换头像">
            <img v-if="authStore.avatarUrl" :src="authStore.avatarUrl" alt="头像" />
            <el-icon v-else :size="18"><UserFilled /></el-icon>
          </div>
          <span class="user-nickname">{{ authStore.nickname }}</span>
          <input
            ref="avatarInput"
            type="file"
            accept="image/*"
            class="avatar-input-hidden"
            @change="handleAvatarChange"
          />
          <el-button
            :icon="isDark ? Sunny : Moon"
            circle
            text
            size="small"
            class="theme-btn"
            @click="toggleDark"
            :title="isDark ? '切换亮色模式' : '切换暗黑模式'"
          />
          <el-button
            :icon="SwitchButton"
            circle
            text
            size="small"
            class="logout-btn"
            @click="handleLogout"
            title="退出登录"
          />
        </div>
        <span v-else class="footer-text">SoulComfort AI</span>
      </div>
    </aside>

    <main class="main-content">
      <CheckinDialog ref="checkinDialogRef" />
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound, List, Notebook, Calendar, UserFilled, SwitchButton, Sunny, Moon } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from './stores/auth'
import CheckinDialog from './components/CheckinDialog.vue'
import LogoMark from './components/LogoMark.vue'
import { applyBgNeutral, resetBg } from './composables/useBgLayer.js'
import { useTheme } from './composables/useTheme.js'

const authStore = useAuthStore()
const route = useRoute()

const checkinDialogRef = ref(null)

/* 主题：改用共享 hook（登录页/欢迎页也可切换），localStorage 为唯一事实源 */
const { isDark, initTheme, toggleTheme } = useTheme()

function toggleDark() {
  toggleTheme()
}

onMounted(() => {
  initTheme()
})

/* 背景区域随路由/登录态同步：
   登录页等嘉宾路由 → 恢复默认铺满；非聊天页 → 背景填满主内容区（侧边栏不占用背景区域）；
   聊天页由 ChatView 自行管理展开/收拢联动，此处不干预 */
function syncBgWithRoute() {
  if (route.meta.guest || !authStore.isLoggedIn) {
    resetBg()
    return
  }
  if (route.path !== '/') {
    applyBgNeutral()
  }
}

watch(() => route.path, () => {
  if (authStore.isLoggedIn && route.path === '/') {
    setTimeout(() => checkinDialogRef.value?.tryShow(), 500)
  }
  syncBgWithRoute()
}, { immediate: true })

watch(() => authStore.isLoggedIn, () => {
  syncBgWithRoute()
}, { immediate: true })

const avatarInput = ref(null)

function triggerAvatarUpload() {
  avatarInput.value?.click()
}

/** 头像上传：文件交后端存阿里云 OSS，成功后将返回地址写入本地用户信息并即时更新侧边栏 */
async function handleAvatarChange(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  try {
    await authStore.uploadAvatar(file)
    ElMessage.success('头像更新成功')
  } catch (err) {
    ElMessage.error(err.message || '头像上传失败')
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    authStore.logout()
  } catch {}
}
</script>

<style scoped>
.app-layout {
  position: relative;
  display: flex;
  height: 100vh;
  background: transparent;
  transition: background 0.3s;
}

/* 全局背景层：布局锚点 + 可读性遮罩
   面板联动由 JS 设置 --bg-scale/--bg-x/--bg-y 驱动父层 transform；
   背景图移至 ::before 独立呼吸，两层互不干扰 */
.bg-layer {
  position: fixed;
  inset: 0;
  z-index: 0;
  transform-origin: 52% 48%;
  transform: translate(var(--bg-x, 0%), var(--bg-y, 0%)) scale(var(--bg-scale, 1));
  /* 呼吸感缓动：前段缓慢吸气、末端轻微过冲回稳，像轻轻吐纳 */
  transition: transform 0.6s cubic-bezier(0.33, 1.15, 0.45, 1);
}

/* 背景图层：插画原图自带浅色留白边（左右/上下约6%），inset 必须大于留白宽度，
   否则留白边露进视口形成"亮带/缝隙"；同时保证呼吸放大、面板联动缩放时不露底 */
.bg-layer::before {
  content: '';
  position: absolute;
  inset: -8%;
  background-image: url('/image/bankground.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  transform-origin: 52% 48%;
  animation: bg-breathe 14s ease-in-out infinite;
  will-change: transform;
}

@keyframes bg-breathe {
  0%, 100% { transform: scale(1) translateY(0); }        /* 呼气末尾复位 */
  30%       { transform: scale(1.02) translateY(-0.6%); } /* 吸气：轻轻放大上移 */
  65%       { transform: scale(1.008) translateY(0.15%); } /* 屏息：缓慢回稳 */
}

/* ===== 夜间模式背景适配 =====
   bug 根因：暗色下仍用同张高亮插画 + 深色遮罩，「高饱和泼墨粒子透过遮罩变成脏色斑块、
   整体灰蒙浑浊」；且纯 scale 呼吸（1→1.02）被压暗后肉眼几乎不可见。
   修复：① 用 filter 把插画压成沉稳的暗调色板（去饱和+压暗），消除脏色；
         ② 夜间呼吸改为「亮度脉动 + 轻微缩放」双维动画，让呼吸感在暗色下清晰可感知。 */
html.dark .bg-layer::before {
  filter: brightness(0.42) saturate(0.45);
  animation-name: bg-breathe-dark;
}

@keyframes bg-breathe-dark {
  0%, 100% { transform: scale(1) translateY(0); filter: brightness(0.42) saturate(0.45); }       /* 呼气：最暗，缓慢回位 */
  30%       { transform: scale(1.012) translateY(-0.5%); filter: brightness(0.52) saturate(0.55); } /* 吸气：微亮 + 轻放大 */
  65%       { transform: scale(1.004) translateY(0.1%); filter: brightness(0.46) saturate(0.48); }  /* 屏息：缓慢回落 */
}

@media (prefers-reduced-motion: reduce) {
  .bg-layer {
    transition: none;
  }
  .bg-layer::before {
    animation: none;
  }
}

.bg-layer::after {
  content: '';
  position: absolute;
  /* 遮罩必须与背景图层 ::before 同范围（inset:-8%）：
     否则背景层被 transform 缩放/平移时，::before 溢出而 ::after 未覆盖的环形区域
     会露出"无遮罩原图亮带"（左/上/下边缘），夜间尤其突兀 */
  inset: -8%;
  background: var(--bg-veil);
}

.sidebar {
  width: 200px;
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 24px 20px 20px;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 1px;
}

.sidebar-nav {
  flex: 1;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 12px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 15px;
  transition: all 0.2s;
}

.nav-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.nav-item--active {
  background: var(--bg-active);
  color: var(--text-primary);
  font-weight: 500;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
}

/* 用户头像：点击触发上传（OSS 图片地址） */
.sidebar-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--bg-hover);
  border: 2px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  transition: border-color 0.2s, transform 0.2s;
}

.sidebar-avatar:hover {
  border-color: var(--accent-color);
  transform: scale(1.05);
}

.sidebar-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-input-hidden {
  display: none;
}

.user-nickname {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.theme-btn {
  color: var(--text-muted);
}

.theme-btn:hover {
  color: var(--accent-color);
}

.logout-btn {
  color: var(--text-muted);
}

.logout-btn:hover {
  color: var(--error-color);
}

.footer-text {
  font-size: 12px;
  color: var(--text-muted);
}

.main-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>

<style>
/* 背景层缩放时四周露出底色，与主题统一 */
body {
  background-color: var(--bg-primary);
}

:root {
  --bg-primary: #faf5ed;
  --bg-veil: linear-gradient(180deg, rgba(253, 246, 238, 0.72) 0%, rgba(253, 246, 238, 0.56) 45%, rgba(252, 238, 226, 0.5) 100%);
  --bg-sidebar: linear-gradient(180deg, #fff8f0 0%, #fffdf9 100%);
  --bg-hover: #faf0e2;
  --bg-active: #f5e6d5;
  --bg-card: rgba(250, 247, 242, 0.92);
  --bg-input: #faf7f2;
  --bg-bubble-user: linear-gradient(135deg, #f5b48c, #f7c8a8);
  --bg-bubble-assistant: linear-gradient(135deg, #fff8f0, #ffecd2);
  --bg-chat-area: rgba(255, 253, 249, 0.72);
  --bg-chat-header: rgba(254, 251, 246, 0.85);
  --bg-conv-item: #faf7f2;
  --bg-conv-item-hover: #f5ede0;
  --bg-quote: #fdf2e9;
  --bg-note: #f5f0eb;
  --bg-note-hover: #f0e6dc;
  --bg-tag: #fff;
  --bg-tag-hover: #f5f0eb;
  --bg-tag-active: #f0e6dc;
  --text-primary: #5c3d2e;
  --text-secondary: #8b6b5a;
  --text-muted: #c8b8a8;
  --border-color: #f0e6dc;
  --accent-color: #d4a373;
  --accent-gradient: linear-gradient(135deg, #fa9e6c, #d4a373);
  --accent-gradient-hover: linear-gradient(135deg, #f08a4a, #c98d5a);
  --accent-gradient-disabled: linear-gradient(135deg, #e07a3a, #b87d4a);
  --shadow: 0 2px 8px rgba(180, 120, 80, 0.08);
  --chart-bg: #faf7f2;
  --stat-bg: linear-gradient(135deg, #faf7f2, #f5ede0);
  --gradient-primary: linear-gradient(135deg, #faf5ed 0%, #fff8f0 50%, #fdf8f0 100%);
  --gradient-chat: linear-gradient(180deg, rgba(255, 248, 240, 0.85), rgba(255, 253, 249, 0.7));
  --error-color: #e88b7a;
}

html.dark {
  --bg-primary: #211a15;
  --bg-veil: linear-gradient(180deg, rgba(33, 26, 21, 0.5) 0%, rgba(33, 26, 21, 0.38) 100%);
  --bg-sidebar: linear-gradient(180deg, #261e18 0%, #211a15 100%);
  --bg-hover: #32271f;
  --bg-active: #3f3228;
  --bg-card: rgba(42, 33, 27, 0.92);
  --bg-input: #2a211b;
  --bg-bubble-user: linear-gradient(135deg, #a0673f, #c08a5e);
  --bg-bubble-assistant: linear-gradient(135deg, #2f251d, #3a2e24);
  --bg-chat-area: rgba(33, 26, 21, 0.72);
  --bg-chat-header: rgba(37, 29, 23, 0.85);
  --bg-conv-item: #2a211b;
  --bg-conv-item-hover: #32271f;
  --bg-quote: #3a2e24;
  --bg-note: #2a211b;
  --bg-note-hover: #32271f;
  --bg-tag: #2a211b;
  --bg-tag-hover: #32271f;
  --bg-tag-active: #4a3a2c;
  --text-primary: #f0e0ce;
  --text-secondary: #c7b2a0;
  --text-muted: #84705f;
  --border-color: #362a1f;
  --accent-color: #d4a373;
  --accent-gradient: linear-gradient(135deg, #e08a5a, #c98d5a);
  --accent-gradient-hover: linear-gradient(135deg, #d07a4a, #b87d4a);
  --accent-gradient-disabled: linear-gradient(135deg, #9c6a3a, #8a6a48);
  --shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
  --chart-bg: #2a211b;
  --stat-bg: linear-gradient(135deg, #2a211b, #32271f);
  --gradient-primary: linear-gradient(135deg, #211a15 0%, #261e18 50%, #211a15 100%);
  --gradient-chat: linear-gradient(180deg, rgba(37, 29, 23, 0.85), rgba(33, 26, 21, 0.7));
  --error-color: #e88b7a;
}

html.dark .app-layout .el-input__wrapper,
html.dark .app-layout .el-textarea__inner {
  background-color: var(--bg-input) !important;
  box-shadow: none !important;
  border-color: var(--border-color) !important;
  color: var(--text-primary) !important;
}

html.dark .app-layout .el-button--text {
  color: var(--text-secondary) !important;
}

html.dark .el-tag {
  background-color: var(--bg-hover) !important;
  border-color: var(--border-color) !important;
  color: var(--text-secondary) !important;
}

html.dark .el-dialog__body,
html.dark .el-dialog__header {
  background-color: var(--bg-card) !important;
}

html.dark .el-dialog__title {
  color: var(--text-primary) !important;
}

html.dark .el-option {
  background-color: var(--bg-card) !important;
  color: var(--text-primary) !important;
}

html.dark .el-option:hover {
  background-color: var(--bg-hover) !important;
}

html.dark .el-pagination button,
html.dark .el-pagination .el-pager li {
  color: var(--text-secondary) !important;
}

html.dark .el-pagination .el-pager li.is-active {
  background-color: var(--bg-active) !important;
  color: var(--text-primary) !important;
}

html.dark .el-divider__text {
  background-color: var(--bg-primary) !important;
  color: var(--text-muted) !important;
}

html.dark .app-layout ::-webkit-scrollbar-track {
  background: var(--bg-sidebar);
}

html.dark .app-layout ::-webkit-scrollbar-thumb {
  background: var(--border-color);
}

html.dark .app-layout ::-webkit-scrollbar-thumb:hover {
  background: var(--text-muted);
}
</style>