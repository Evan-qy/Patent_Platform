<template>
  <el-config-provider :locale="locale">
    <ThemeSwitcher />
    <router-view v-slot="{ Component, route }">
      <transition :name="getTransitionName(route)" mode="out-in" appear>
        <keep-alive :include="cachedViews">
          <component :is="Component" :key="route.path" v-if="route.meta.keepAlive !== false" />
        </keep-alive>
      </transition>
    </router-view>

    <div class="global-loading" v-if="isLoading">
      <div class="loading-spinner"></div>
    </div>

    <section v-if="showMobileTabbar" ref="mobileTabbarRef" class="mobile-tabbar-shell">
      <div class="mobile-tabbar">
        <button
          v-for="item in tabItems"
          :key="item.key"
          type="button"
          class="mobile-tab"
          :class="{ active: activeNav === item.key, primary: item.primary }"
          @click="handleNavSelect(item.key)"
        >
          <span class="mobile-tab-icon"><el-icon><component :is="item.icon" /></el-icon></span>
          <span class="mobile-tab-label">{{ item.label }}</span>
        </button>
      </div>
    </section>
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import type { Component } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'
import { useRoute, useRouter } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { ElMessage } from 'element-plus'
import ThemeSwitcher from './components/ThemeSwitcher.vue'
import { DataAnalysis, Document, EditPen, House, UserFilled } from '@element-plus/icons-vue'

const locale = zhCn
const router = useRouter()
const route = useRoute()

const showMobileTabbar = computed(() => {
  if (route.path.startsWith('/admin') || route.path.startsWith('/articles/')) return false
  if (route.path === '/login' || route.path === '/register' || route.path === '/admin/login') return false
  return true
})

const activeNav = ref('home')
const mobileTabbarRef = ref<HTMLElement | null>(null)
let mobileTabbarResizeObserver: ResizeObserver | null = null

const tabItems: Array<{ key: string; label: string; icon: Component; primary?: boolean }> = [
  { key: 'home', label: '首页', icon: House },
  { key: 'patent', label: '检索', icon: Document },
  { key: 'demand', label: '发布', icon: EditPen, primary: true },
  { key: 'evaluation', label: '评估', icon: DataAnalysis },
  { key: 'profile', label: '我的', icon: UserFilled }
]

function updateShellClasses(path: string): void {
  const body = document.body
  body.classList.toggle('front-theme-shell', !path.startsWith('/admin'))
  body.classList.toggle('admin-theme-shell', path.startsWith('/admin'))
  body.classList.toggle('screen-theme-shell', path === '/data-screen')
}

const updateActiveNav = (path: string) => {
  if (path === '/' || path.startsWith('/requirement')) activeNav.value = 'home'
  else if (path.includes('/patent')) activeNav.value = 'patent'
  else if (path.includes('/demand')) activeNav.value = 'demand'
  else if (path.includes('/evaluation') || path.includes('/valuation')) activeNav.value = 'evaluation'
  else if (path.includes('/profile')) activeNav.value = 'profile'
  else activeNav.value = 'home'
}

watch(() => route.path, (path) => {
  updateShellClasses(path)
  updateActiveNav(path)
  void nextTick(() => {
    updateMobileTabbarMetrics()
  })
}, { immediate: true })

watch(showMobileTabbar, () => {
  void nextTick(() => {
    updateMobileTabbarOffset()
  })
}, { immediate: true })

const handleNavSelect = (index: string) => {
  const isLogin = !!localStorage.getItem('token')

  if (index === 'home') {
    router.push('/')
    return
  }

  if (index === 'patent') {
    router.push('/patent')
    return
  }

  if (index === 'demand') {
    if (isLogin) router.push('/demand')
    else {
      ElMessage.warning('请先登录后再发布需求')
      router.push('/login?redirect=/demand')
    }
    return
  }

  if (index === 'evaluation') {
    if (isLogin) router.push('/evaluation')
    else {
      ElMessage.warning('请先登录后再使用价值评估')
      router.push('/login?redirect=/evaluation')
    }
    return
  }

  if (index === 'profile') {
    if (isLogin) router.push('/profile')
    else {
      ElMessage.warning('请先登录后查看个人中心')
      router.push('/login?redirect=/profile')
    }
  }
}

interface AppRouteMeta {
  title?: string
  requiresAuth?: boolean
  transition?: string
  keepAlive?: boolean
}

declare module 'vue-router' {
  interface RouteMeta extends AppRouteMeta {}
}

const cachedViews = ref<string[]>([])
const isLoading = ref(false)

const getTransitionName = (currentRoute: RouteLocationNormalized): string => {
  if (!currentRoute?.meta) return 'fade'
  if (typeof currentRoute.meta.transition !== 'string') return 'fade'
  return currentRoute.meta.transition || 'fade'
}

const handleRouteChange = (): void => {
  isLoading.value = true
  setTimeout(() => {
    isLoading.value = false
  }, 300)
}

const updateMobileTabbarOffset = (): void => {
  const viewport = window.visualViewport
  const root = document.documentElement

  if (!viewport) {
    root.style.setProperty('--mobile-tabbar-offset', '0px')
    updateMobileTabbarMetrics()
    return
  }

  const offset = Math.max(0, window.innerHeight - viewport.height - viewport.offsetTop)
  root.style.setProperty('--mobile-tabbar-offset', `${offset}px`)
  updateMobileTabbarMetrics(offset)
}

const updateMobileTabbarMetrics = (
  offset = Number.parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--mobile-tabbar-offset')) || 0
): void => {
  const root = document.documentElement

  if (!showMobileTabbar.value) {
    root.style.setProperty('--mobile-tabbar-height', '0px')
    root.style.setProperty('--mobile-tabbar-safe-area', '0px')
    return
  }

  const measuredHeight = mobileTabbarRef.value?.offsetHeight ?? 78
  const safeArea = Math.max(measuredHeight + offset + 18, 96)

  root.style.setProperty('--mobile-tabbar-height', `${measuredHeight}px`)
  root.style.setProperty('--mobile-tabbar-safe-area', `${safeArea}px`)
}

onMounted(() => {
  updateShellClasses(route.path)
  updateActiveNav(route.path)
  window.addEventListener('route-change', handleRouteChange)
  window.addEventListener('resize', updateMobileTabbarOffset)
  window.visualViewport?.addEventListener('resize', updateMobileTabbarOffset)
  window.visualViewport?.addEventListener('scroll', updateMobileTabbarOffset)
  updateMobileTabbarOffset()

  if (typeof ResizeObserver !== 'undefined' && mobileTabbarRef.value) {
    mobileTabbarResizeObserver = new ResizeObserver(() => {
      updateMobileTabbarMetrics()
    })
    mobileTabbarResizeObserver.observe(mobileTabbarRef.value)
  }
})

onUnmounted(() => {
  document.body.classList.remove('front-theme-shell', 'admin-theme-shell', 'screen-theme-shell')
  window.removeEventListener('route-change', handleRouteChange)
  window.removeEventListener('resize', updateMobileTabbarOffset)
  window.visualViewport?.removeEventListener('resize', updateMobileTabbarOffset)
  window.visualViewport?.removeEventListener('scroll', updateMobileTabbarOffset)
  mobileTabbarResizeObserver?.disconnect()
})

defineExpose({
  setLoading: (status: boolean): void => {
    isLoading.value = status
  },
  addCachedView: (viewName: string): void => {
    if (!cachedViews.value.includes(viewName)) {
      cachedViews.value.push(viewName)
    }
  }
})
</script>

<style>
:root {
  --mobile-tabbar-height: 78px;
  --mobile-tabbar-safe-area: 96px;
}

.global-loading {
  position: fixed;
  inset: 0;
  background-color: var(--bg-overlay);
  backdrop-filter: blur(5px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border-light);
  border-top: 4px solid var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.mobile-tabbar-shell {
  display: none;
  position: fixed !important;
  left: 0;
  right: 0;
  bottom: var(--mobile-tabbar-offset, 0px);
  z-index: 1300;
  padding: 0 12px calc(10px + env(safe-area-inset-bottom));
  pointer-events: none;
}

.mobile-tabbar {
  width: min(100%, 560px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
  border-radius: 28px;
  background: rgba(8, 24, 46, 0.88);
  border: 1px solid rgba(134, 196, 255, 0.16);
  box-shadow: 0 22px 44px rgba(4, 14, 32, 0.24);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  pointer-events: auto;
}

@media (max-width: 768px) {
  .page-container,
  .home-page,
  .auth-container,
  .profile-container,
  .evaluation-container,
  .expert-manage-container,
  .transformation-manage-container,
  .valuation-manage-container,
  .data-screen-container,
  .expert-page,
  .evaluation-page,
  .demand-publish-page,
  .profile-page {
    box-sizing: border-box;
    padding-bottom: var(--mobile-tabbar-safe-area) !important;
  }

  .mobile-tabbar-shell {
    display: block !important;
  }
}

.mobile-tab {
  min-height: 58px;
  border: none;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 18px;
  padding: 8px 4px;
  color: rgba(231, 242, 255, 0.78);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  transition: transform .2s ease, background .2s ease, color .2s ease, box-shadow .2s ease;
}

.mobile-tab-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 14px;
  font-size: 18px;
}

.mobile-tab.active {
  color: #ffffff;
  background: linear-gradient(135deg, rgba(31, 113, 241, 0.82), rgba(24, 186, 204, 0.76));
  box-shadow: 0 12px 24px rgba(19, 91, 188, 0.24);
}

.mobile-tab.primary {
  transform: translateY(-8px);
  background: linear-gradient(135deg, rgba(58, 126, 250, 0.96), rgba(35, 204, 172, 0.92));
  color: #fff;
  box-shadow: 0 16px 30px rgba(23, 94, 194, 0.3);
}

.mobile-tab.primary .mobile-tab-icon {
  background: rgba(255, 255, 255, 0.18);
}

[data-theme="dark"] .mobile-tabbar {
  background: rgba(6, 19, 38, 0.94);
}

@media (max-width: 480px) {
  .mobile-tabbar-shell {
    padding: 0 10px calc(8px + env(safe-area-inset-bottom));
  }

  .mobile-tabbar {
    gap: 6px;
    padding: 8px;
    border-radius: 24px;
  }

  .mobile-tab {
    min-height: 54px;
    font-size: 10px;
  }

  .mobile-tab-icon {
    width: 24px;
    height: 24px;
    font-size: 16px;
  }
}
</style>
