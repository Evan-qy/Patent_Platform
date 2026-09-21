<template>
  <button
    class="theme-switcher glass-effect"
    type="button"
    @click="toggleTheme"
    :aria-label="isDark ? '切换到浅色模式' : '切换到深色模式'"
    :title="isDark ? '切换到浅色模式' : '切换到深色模式'"
  >
    <el-icon class="icon" v-if="isDark"><Moon /></el-icon>
    <el-icon class="icon" v-else><Sunny /></el-icon>
  </button>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Moon, Sunny } from '@element-plus/icons-vue'

const isDark = ref(false)

const applyTheme = (theme: 'light' | 'dark') => {
  isDark.value = theme === 'dark'
  document.documentElement.setAttribute('data-theme', theme)
  document.documentElement.style.colorScheme = theme
  document.body.setAttribute('data-theme', theme)
  localStorage.setItem('theme', theme)
}

const toggleTheme = () => {
  applyTheme(isDark.value ? 'light' : 'dark')
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme') as 'light' | 'dark' | null
  if (savedTheme) {
    applyTheme(savedTheme)
    return
  }

  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
  applyTheme(prefersDark ? 'dark' : 'light')
})
</script>

<style scoped>
.theme-switcher {
  position: fixed;
  bottom: 92px;
  right: 24px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 900;
  font-size: 22px;
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border-light);
  color: var(--text-primary);
  background: var(--glass-effect);
  backdrop-filter: var(--backdrop-blur);
}

.theme-switcher:hover {
  transform: scale(1.1) rotate(15deg);
  box-shadow: var(--shadow-lg);
}

.icon {
  line-height: 1;
}

@media (max-width: 768px) {
  .theme-switcher {
    right: 14px;
    top: calc(74px + env(safe-area-inset-top));
    bottom: auto;
    width: 40px;
    height: 40px;
    font-size: 18px;
  }
}

@media (max-width: 390px) {
  .theme-switcher {
    right: 12px;
    top: calc(70px + env(safe-area-inset-top));
    bottom: auto;
    width: 38px;
    height: 38px;
  }
}
</style>
