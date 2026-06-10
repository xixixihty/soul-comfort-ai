<template>
  <router-view v-if="$route.meta.guest" />
  <div v-else class="app-layout" :class="{ 'app-layout--dark': isDark }">
    <aside class="sidebar">
      <div class="sidebar-logo">
        <el-icon :size="28" color="#d4a373"><StarFilled /></el-icon>
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
          <el-icon :size="16"><UserFilled /></el-icon>
          <span class="user-nickname">{{ authStore.nickname }}</span>
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
import { StarFilled, ChatDotRound, List, Notebook, Calendar, UserFilled, SwitchButton, Sunny, Moon } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from './stores/auth'
import CheckinDialog from './components/CheckinDialog.vue'

const authStore = useAuthStore()
const route = useRoute()

const isDark = ref(false)
const checkinDialogRef = ref(null)

function toggleDark() {
  isDark.value = !isDark.value
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  document.documentElement.classList.toggle('dark', isDark.value)
}

onMounted(() => {
  const saved = localStorage.getItem('theme')
  if (saved === 'dark') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }
})

watch(() => route.path, () => {
  if (authStore.isLoggedIn && route.path === '/') {
    setTimeout(() => checkinDialogRef.value?.tryShow(), 500)
  }
})

watch(() => authStore.isLoggedIn, (v) => {
  if (v) setTimeout(() => checkinDialogRef.value?.tryShow(), 800)
})

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
  display: flex;
  height: 100vh;
  background: var(--bg-primary);
  transition: background 0.3s;
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
:root {
  --bg-primary: #faf5ed;
  --bg-sidebar: linear-gradient(180deg, #fff8f0 0%, #fffdf9 100%);
  --bg-hover: #faf0e2;
  --bg-active: #f5e6d5;
  --bg-card: #faf7f2;
  --bg-input: #faf7f2;
  --bg-bubble-user: linear-gradient(135deg, #a0c4ff, #bdd4ff);
  --bg-bubble-assistant: linear-gradient(135deg, #fff8f0, #ffecd2);
  --bg-chat-area: #fffdf9;
  --bg-chat-header: #fefbf6;
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
  --gradient-chat: linear-gradient(180deg, #fff8f0, #fffdf9);
  --error-color: #e88b7a;
}

html.dark {
  --bg-primary: #1a1a2e;
  --bg-sidebar: linear-gradient(180deg, #16213e 0%, #1a1a2e 100%);
  --bg-hover: #2d2d44;
  --bg-active: #3a3a5c;
  --bg-card: #252540;
  --bg-input: #252540;
  --bg-bubble-user: linear-gradient(135deg, #3a3a7c, #4a4a9c);
  --bg-bubble-assistant: linear-gradient(135deg, #2d2d44, #35355a);
  --bg-chat-area: #1a1a2e;
  --bg-chat-header: #1e1e3a;
  --bg-conv-item: #252540;
  --bg-conv-item-hover: #2d2d44;
  --bg-quote: #2d2d44;
  --bg-note: #252540;
  --bg-note-hover: #2d2d44;
  --bg-tag: #252540;
  --bg-tag-hover: #2d2d44;
  --bg-tag-active: #3a3a5c;
  --text-primary: #e8d5c4;
  --text-secondary: #b8a898;
  --text-muted: #6a6a80;
  --border-color: #2d2d44;
  --accent-color: #c9a87c;
  --accent-gradient: linear-gradient(135deg, #c9895a, #b8956a);
  --accent-gradient-hover: linear-gradient(135deg, #b8794a, #a8855a);
  --accent-gradient-disabled: linear-gradient(135deg, #98693a, #88754a);
  --shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  --chart-bg: #252540;
  --stat-bg: linear-gradient(135deg, #252540, #2d2d44);
  --gradient-primary: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #1a1a2e 100%);
  --gradient-chat: linear-gradient(180deg, #16213e, #1a1a2e);
  --error-color: #e88b7a;
}

html.dark .el-input__wrapper,
html.dark .el-textarea__inner {
  background-color: var(--bg-input) !important;
  box-shadow: none !important;
  border-color: var(--border-color) !important;
  color: var(--text-primary) !important;
}

html.dark .el-button--text {
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

html.dark ::-webkit-scrollbar-track {
  background: var(--bg-sidebar);
}

html.dark ::-webkit-scrollbar-thumb {
  background: var(--border-color);
}

html.dark ::-webkit-scrollbar-thumb:hover {
  background: var(--text-muted);
}
</style>