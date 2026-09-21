/* 主题状态共享 hook：亮/暗模式切换。
   状态以 localStorage('theme') 为唯一事实源，App.vue、登录页、欢迎页共用；
   同屏只存在一个使用方（guest 路由 与 app-layout 互斥），各自挂载时 initTheme 保持一致 */

import { ref } from 'vue'

export function useTheme() {
  const isDark = ref(false)

  function initTheme() {
    const saved = localStorage.getItem('theme')
    if (saved === 'dark') {
      isDark.value = true
      document.documentElement.classList.add('dark')
    } else {
      isDark.value = false
      document.documentElement.classList.remove('dark')
    }
  }

  function toggleTheme() {
    isDark.value = !isDark.value
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
    document.documentElement.classList.toggle('dark', isDark.value)
  }

  return { isDark, initTheme, toggleTheme }
}