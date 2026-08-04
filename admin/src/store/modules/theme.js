import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  // 主题模式：'light' 或 'dark'
  const themeMode = ref(localStorage.getItem('theme-mode') || 'light')
  
  // 切换主题
  function toggleTheme() {
    themeMode.value = themeMode.value === 'light' ? 'dark' : 'light'
    localStorage.setItem('theme-mode', themeMode.value)
    applyTheme()
  }
  
  // 应用主题到 DOM（class 名与 Element Plus 暗色变量 html.dark 一致）
  function applyTheme() {
    const root = document.documentElement
    if (themeMode.value === 'dark') {
      root.classList.add('dark')
      root.classList.remove('light')
      root.style.colorScheme = 'dark'
    } else {
      root.classList.add('light')
      root.classList.remove('dark')
      root.style.colorScheme = 'light'
    }
  }
  
  // 初始化主题
  function initTheme() {
    applyTheme()
  }
  
  return {
    themeMode,
    toggleTheme,
    initTheme
  }
})
