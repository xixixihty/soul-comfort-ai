<template>
  <div class="guest-nav">
    <button type="button" class="nav-capsule" @click="go('/about')">
      关于甜弈
      <span class="nav-arrow" aria-hidden="true">→</span>
    </button>
    <button type="button" class="nav-capsule" @click="go('/services')">
      服务与功能
      <span class="nav-arrow" aria-hidden="true">→</span>
    </button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function go(path) {
  router.push(path)
}
</script>

<style scoped>
.guest-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  animation: navIn 0.9s cubic-bezier(0.22, 1, 0.36, 1) 0.9s both;
}

@keyframes navIn {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.nav-capsule {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: 1px solid rgba(212, 163, 115, 0.4);
  border-radius: 999px;
  background: rgba(255, 251, 244, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: #7c5c42;
  font-size: 13px;
  letter-spacing: 1px;
  cursor: pointer;
  transition: all 0.25s;
}

.nav-arrow {
  font-size: 12px;
  transition: transform 0.25s;
}

.nav-capsule:hover {
  background: rgba(255, 138, 61, 0.78);
  border-color: rgba(255, 255, 255, 0.5);
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 130, 40, 0.3);
}

.nav-capsule:hover .nav-arrow {
  transform: translateX(3px);
}

html.dark .nav-capsule {
  background: rgba(42, 33, 27, 0.6);
  border-color: rgba(212, 163, 115, 0.28);
  color: #d9c3a9;
}

html.dark .nav-capsule:hover {
  color: #fff;
}
</style>