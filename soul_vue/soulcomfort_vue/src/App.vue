<template>
  <div class="app-layout">
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
      </nav>

      <div class="sidebar-footer">
        <span class="footer-text">SoulComfort AI</span>
      </div>
    </aside>

    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { StarFilled, ChatDotRound, List, Notebook } from '@element-plus/icons-vue'
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  background: #faf5ed;
}

.sidebar {
  width: 200px;
  background: linear-gradient(180deg, #fff8f0 0%, #fffdf9 100%);
  border-right: 1px solid #f0e6dc;
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
  color: #5c3d2e;
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
  color: #8b6b5a;
  text-decoration: none;
  font-size: 15px;
  transition: all 0.2s;
}

.nav-item:hover {
  background: #faf0e2;
  color: #5c3d2e;
}

.nav-item--active {
  background: #f5e6d5;
  color: #5c3d2e;
  font-weight: 500;
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid #f0e6dc;
}

.footer-text {
  font-size: 12px;
  color: #c4a882;
}

.main-content {
  flex: 1;
  overflow: hidden;
}
</style>