<template>
  <div class="bg-layer" :class="{ 'bg-layer--breathing': bgBreathing }" aria-hidden="true"></div>
  <router-view v-if="$route.meta.guest || $route.meta.standalone" />
  <div v-else class="app-layout" :class="{ 'app-layout--dark': isDark }">
    <aside class="sidebar">
      <div class="sidebar-logo">
        <LogoMark :size="28" :mood="careTrend" />
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
          <div class="sidebar-avatar" @click="profileVisible = true" title="个人中心">
            <img v-if="authStore.avatarUrl" :src="authStore.avatarUrl" alt="头像" />
            <el-icon v-else :size="18"><UserFilled /></el-icon>
          </div>
          <span class="user-nickname" @click="profileVisible = true" title="个人中心">{{ authStore.nickname }}</span>
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
      <ProfileDialog v-model="profileVisible" />
      <CarePill />
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound, List, Notebook, Calendar, UserFilled, SwitchButton, Sunny, Moon } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from './stores/auth'
import CheckinDialog from './components/CheckinDialog.vue'
import ProfileDialog from './components/ProfileDialog.vue'
import CarePill from './components/CarePill.vue'
import LogoMark from './components/LogoMark.vue'
import { applyBgNeutral, resetBg } from './composables/useBgLayer.js'
import { useTheme } from './composables/useTheme.js'
import { useCareReminder } from './composables/useCareReminder.js'

const authStore = useAuthStore()
const route = useRoute()

/* 主动关怀（情绪灯塔）：登录态变化与回到聊天页时刷新趋势/关怀卡 */
const { careTrend, refreshCare } = useCareReminder()

const checkinDialogRef = ref(null)

/* 主题：改用共享 hook（登录页/欢迎页也可切换），localStorage 为唯一事实源 */
const { isDark, initTheme, toggleTheme } = useTheme()

function toggleDark() {
  toggleTheme()
}

onMounted(() => {
  initTheme()
  document.addEventListener('click', onDocumentClick)
})
onUnmounted(() => document.removeEventListener('click', onDocumentClick))

/* 背景呼吸效果：默认关闭，点击页面空白处开启/停止（点到交互元素上不算） */
const bgBreathing = ref(false)

function onDocumentClick(e) {
  const t = e.target
  if (
    t &&
    typeof t.closest === 'function' &&
    t.closest(
      'a, button, input, textarea, select, [role="button"], [contenteditable="true"], .sidebar-avatar, .user-nickname, .el-overlay, .el-dialog, .el-message, .el-message-box, .el-popper, .el-image-viewer__wrapper'
    )
  ) {
    return
  }
  bgBreathing.value = !bgBreathing.value
}

/* 背景区域随路由/登录态同步：
   登录页等嘉宾路由 → 恢复默认铺满；非聊天页 → 背景填满主内容区（侧边栏不占用背景区域）；
   聊天页由 ChatView 自行管理展开/收拢联动，此处不干预 */
function syncBgWithRoute() {
  if (route.meta.guest || route.meta.standalone || !authStore.isLoggedIn) {
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
    refreshCare()
  }
  syncBgWithRoute()
}, { immediate: true })

watch(() => authStore.isLoggedIn, () => {
  syncBgWithRoute()
  if (authStore.isLoggedIn) refreshCare()
}, { immediate: true })

const profileVisible = ref(false)

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
  will-change: transform;
}

/* 呼吸效果默认关闭：仅当点击空白处开启（bg-layer--breathing）后播放 */
.bg-layer--breathing::before {
  animation: bg-breathe 14s ease-in-out infinite;
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
}

html.dark .bg-layer--breathing::before {
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
  .bg-layer::before,
  .bg-layer--breathing::before {
    animation: none !important;
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

.user-nickname {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.2s;
}

.user-nickname:hover {
  color: var(--accent-color);
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
  --bg-primary: #f4e8ec;
  --bg-veil: linear-gradient(180deg, rgba(246,237,239, 0.6) 0%, rgba(246,237,239, 0.42) 45%, rgba(250,240,245, 0.36) 100%);
  --bg-sidebar: linear-gradient(180deg, #f9eef3 0%, #fdf7fa 100%);
  --bg-hover: #f0d9e4;
  --bg-active: #e9cbda;
  --bg-card: rgba(252, 248, 251, 0.94);
  --bg-input: #f9eaf1;
  --bg-bubble-user: linear-gradient(135deg, #e193b4, #eeb7cd);
  --bg-bubble-assistant: linear-gradient(135deg, #ffffff, #dfeaf1);
  --bg-chat-area: rgba(255, 252, 253, 0.74);
  --bg-chat-header: rgba(250, 241, 246, 0.88);
  --bg-conv-item: #f9eef3;
  --bg-conv-item-hover: #f0d9e4;
  --bg-quote: #dfeaf1;
  --bg-note: #f3e6ec;
  --bg-note-hover: #e9cbda;
  --bg-tag: #fff;
  --bg-tag-hover: #f0d9e4;
  --bg-tag-active: #e9cbda;
  --text-primary: #422f3f;
  --text-secondary: #6f5369;
  --text-muted: #ab8fa6;
  --border-color: #e3c9d6;
  /* 品牌三色：粉 #E8A9C1 / 蓝 #4F8FAA / 白 #F6EDEF */
  --accent-pink: #e8a9c1;
  --accent-pink-deep: #b05f83;
  --accent-pink-soft: #f2c6d8;
  --accent-blue: #4f8faa;
  --accent-blue-deep: #3c7690;
  --accent-blue-soft: #a9cbd9;
  --accent-color: #b05f83;
  --accent-gradient: linear-gradient(135deg, #de93b4, #4f8faa);
  --accent-gradient-hover: linear-gradient(135deg, #d07fa4, #3c7690);
  --accent-gradient-disabled: linear-gradient(135deg, #eecddd, #b8d2dd);
  --shadow: 0 2px 8px rgba(150, 90, 120, 0.14);
  --chart-bg: #f9eef3;
  --stat-bg: linear-gradient(135deg, #f9eef3, #eed8e2);
  --gradient-primary: linear-gradient(135deg, #f4e8ec 0%, #f9eef3 50%, #e6f0f5 100%);
  --gradient-chat: linear-gradient(180deg, rgba(250,241,246, 0.85), rgba(255, 252, 253, 0.72));
  --error-color: #d05f72;

  /* Element Plus 主题跟随品牌色：交互主色用蓝，避免默认科技蓝串色 */
  --el-color-primary: #4f8faa;
  --el-color-primary-light-3: #7fb2cb;
  --el-color-primary-light-5: #a9cbd9;
  --el-color-primary-light-7: #cfe1e9;
  --el-color-primary-light-8: #e0ebf0;
  --el-color-primary-light-9: #eff5f8;
  --el-color-primary-dark-2: #3c7690;
  --el-color-danger: #de7f8e;
  --el-color-danger-light-3: #e8a1ac;
  --el-color-danger-light-5: #f0bec6;
  --el-color-danger-light-7: #f7d9dd;
  --el-color-danger-light-8: #fae7ea;
  --el-color-danger-light-9: #fdf3f4;
  --el-color-danger-dark-2: #c96373;
}

html.dark {
  --bg-primary: #221b23;
  --bg-veil: linear-gradient(180deg, rgba(34,27,35, 0.5) 0%, rgba(34,27,35, 0.38) 100%);
  --bg-sidebar: linear-gradient(180deg, #2a2130 0%, #221b23 100%);
  --bg-hover: #3a2d40;
  --bg-active: #4a3750;
  --bg-card: rgba(44,35,48, 0.92);
  --bg-input: #2c2330;
  --bg-bubble-user: linear-gradient(135deg, #b06a8c, #c98aab);
  --bg-bubble-assistant: linear-gradient(135deg, #2a2e3a, #33404f);
  --bg-chat-area: rgba(34,27,35, 0.72);
  --bg-chat-header: rgba(39,31,43, 0.85);
  --bg-conv-item: #2c2330;
  --bg-conv-item-hover: #3a2d40;
  --bg-quote: #2e3140;
  --bg-note: #2c2330;
  --bg-note-hover: #3a2d40;
  --bg-tag: #2c2330;
  --bg-tag-hover: #3a2d40;
  --bg-tag-active: #4a3848;
  --text-primary: #f4e7f0;
  --text-secondary: #d3bccd;
  --text-muted: #9a829a;
  --border-color: #463751;
  --accent-pink: #e8a9c1;
  --accent-pink-deep: #da9cba;
  --accent-pink-soft: #7a4466;
  --accent-blue: #7fb2cb;
  --accent-blue-deep: #5d95b2;
  --accent-blue-soft: #2e3140;
  --accent-color: #e8a9c1;
  --accent-gradient: linear-gradient(135deg, #c2708f, #4f8faa);
  --accent-gradient-hover: linear-gradient(135deg, #d08fae, #5d95b2);
  --accent-gradient-disabled: linear-gradient(135deg, #7a4466, #355666);
  --shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
  --chart-bg: #2c2330;
  --stat-bg: linear-gradient(135deg, #2c2330, #342838);
  --gradient-primary: linear-gradient(135deg, #221b23 0%, #2a2130 50%, #221b23 100%);
  --gradient-chat: linear-gradient(180deg, rgba(39,31,43, 0.85), rgba(34,27,35, 0.7));
  --error-color: #e8999f;

  /* 暗色下 Element 主色提一档亮度保证对比度 */
  --el-color-primary: #6fa3bc;
  --el-color-primary-light-3: #8db8cc;
  --el-color-primary-light-5: #5d8ba1;
  --el-color-primary-light-7: #3c5b6a;
  --el-color-primary-light-8: #314956;
  --el-color-primary-light-9: #283945;
  --el-color-primary-dark-2: #5d8ba1;
  --el-color-danger: #e8999f;
  --el-color-danger-light-3: #c97f85;
  --el-color-danger-light-5: #8a5a5f;
  --el-color-danger-light-7: #55393d;
  --el-color-danger-light-8: #463033;
  --el-color-danger-light-9: #3a282a;
  --el-color-danger-dark-2: #c97f85;
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

/* 暗色下整个对话框统一用卡片底色：
   只给 header/body 上色会露出 .el-dialog 自身的白底（内边距/页脚处形成白框） */
html.dark .el-dialog {
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