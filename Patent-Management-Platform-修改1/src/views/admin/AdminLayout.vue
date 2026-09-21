<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="admin-brand">网站后台</div>
      <el-menu :default-active="activePath" class="admin-menu" @select="handleMenuSelect">
        <el-menu-item index="/admin/home">首页配置</el-menu-item>
        <el-menu-item index="/admin/articles">文章管理</el-menu-item>
        <el-menu-item index="/admin/patent-sources">专利数据源</el-menu-item>
        <el-menu-item index="/admin/patent-datasets">专利数据集</el-menu-item>
        <el-menu-item index="/admin/audit-logs">审计日志</el-menu-item>
        <el-menu-item index="/admin/sql">SQL 控制台</el-menu-item>
      </el-menu>
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <span>{{ adminName }}</span>
        <div class="header-actions">
          <el-button link @click="goSite">前台首页</el-button>
          <el-button link type="danger" @click="logout">退出登录</el-button>
        </div>
      </header>

      <section class="admin-content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()

const activePath = computed(() => route.path)
const adminName = computed(() => {
  try {
    const raw = localStorage.getItem('adminProfile')
    const profile = raw ? JSON.parse(raw) : null
    return profile?.displayName || profile?.username || '管理员'
  } catch {
    return '管理员'
  }
})

const handleMenuSelect = (index: string) => {
  if (index && index !== route.path) {
    router.push(index)
  }
}

const logout = () => {
  localStorage.removeItem('adminToken')
  localStorage.removeItem('adminProfile')
  router.push('/admin/login')
}

const goSite = () => {
  router.push('/')
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px 1fr;
  background: var(--bg-app);
}

.admin-sidebar {
  border-right: 1px solid var(--border-color);
  background: rgba(255, 255, 255, 0.9);
  padding: 20px 14px;
}

.admin-brand {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 20px;
}

.admin-main {
  display: flex;
  flex-direction: column;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid var(--border-color);
  background: rgba(255, 255, 255, 0.82);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.admin-content {
  padding: 24px;
}
</style>
