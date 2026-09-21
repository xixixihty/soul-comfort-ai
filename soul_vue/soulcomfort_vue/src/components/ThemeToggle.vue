<template>
  <button
    class="theme-toggle"
    :title="isDark ? '切换到亮色模式' : '切换到夜间模式'"
    aria-label="切换主题"
    @click="toggleTheme"
  >
    <el-icon :size="18"><Sunny v-if="isDark" /><Moon v-else /></el-icon>
  </button>
</template>

<script setup>
import { onMounted } from 'vue'
import { Sunny, Moon } from '@element-plus/icons-vue'
import { useTheme } from '../composables/useTheme.js'

const { isDark, initTheme, toggleTheme } = useTheme()

onMounted(initTheme)
</script>

<style scoped>
.theme-toggle {
  position: fixed;
  top: 18px;
  right: 20px;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 1px solid rgba(212, 163, 115, 0.35);
  background: rgba(255, 251, 244, 0.55);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: #8a5b3a;
  cursor: pointer;
  transition: all 0.25s;
}

.theme-toggle:hover {
  background: rgba(255, 138, 61, 0.7);
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 130, 40, 0.3);
}

html.dark .theme-toggle {
  background: rgba(42, 33, 27, 0.6);
  border-color: rgba(212, 163, 115, 0.3);
  color: #f0c89a;
}

html.dark .theme-toggle:hover {
  background: rgba(255, 122, 41, 0.75);
  color: #fff;
}

@media (max-width: 768px) {
  .theme-toggle {
    top: 12px;
    right: 14px;
    width: 34px;
    height: 34px;
  }
}
</style>