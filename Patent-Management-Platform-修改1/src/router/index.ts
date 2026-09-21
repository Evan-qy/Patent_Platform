import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { PLATFORM_NAME } from '@/constants/brand'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/data-screen',
    name: 'DataScreen',
    component: () => import('@/views/DataScreen.vue'),
    meta: { title: '数据大屏', requiresAuth: false, transition: 'fade' }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页', requiresAuth: false, transition: 'fade' }
  },
  {
    path: '/articles/:slug',
    name: 'ArticleDetail',
    component: () => import('@/views/ArticleDetail.vue'),
    meta: { title: '文章详情', requiresAuth: false, transition: 'fade' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false, transition: 'slide-right' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', requiresAuth: false, transition: 'slide-right' }
  },
  {
    path: '/demand',
    name: 'Demand',
    component: () => import('@/views/DemandPublish.vue'),
    meta: { title: '需求发布', requiresAuth: true, transition: 'fade' }
  },
  {
    path: '/evaluation',
    name: 'Evaluation',
    component: () => import('@/views/Evaluation.vue'),
    meta: { title: '价值评估', requiresAuth: true, transition: 'fade' }
  },
  {
    path: '/patent',
    name: 'PatentSearch',
    component: () => import('@/views/PatentSearch.vue'),
    meta: { title: '专利检索', requiresAuth: false, transition: 'fade' }
  },
  {
    path: '/patent/manage',
    name: 'PatentManage',
    component: () => import('@/views/PatentManage.vue'),
    meta: { title: '专利管理', requiresAuth: true, transition: 'fade' }
  },
  {
    path: '/expert',
    name: 'Expert',
    component: () => import('@/views/ExpertManage.vue'),
    meta: { title: '专家资源', requiresAuth: true, transition: 'fade' }
  },
  {
    path: '/requirement',
    name: 'Requirement',
    component: () => import('@/views/RequirementManage.vue'),
    meta: { title: '需求广场', requiresAuth: false, transition: 'fade' }
  },
  {
    path: '/transformation',
    name: 'Transformation',
    component: () => import('@/views/TransformationManage.vue'),
    meta: { title: '成果转化', requiresAuth: true, transition: 'fade' }
  },
  {
    path: '/valuation',
    name: 'Valuation',
    component: () => import('@/views/ValuationManage.vue'),
    meta: { title: '评估报告', requiresAuth: true, transition: 'fade' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人中心', requiresAuth: true, transition: 'fade' }
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/AdminLogin.vue'),
    meta: { title: '后台登录', requiresAdminAuth: false, transition: 'fade' }
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAdminAuth: true, transition: 'fade' },
    children: [
      { path: '', redirect: '/admin/home' },
      { path: 'home', name: 'AdminHome', component: () => import('@/views/admin/AdminHomeContent.vue'), meta: { title: '首页配置', requiresAdminAuth: true } },
      { path: 'articles', name: 'AdminArticles', component: () => import('@/views/admin/AdminArticles.vue'), meta: { title: '文章管理', requiresAdminAuth: true } },
      { path: 'articles/new', name: 'AdminArticleCreate', component: () => import('@/views/admin/AdminArticleEditor.vue'), meta: { title: '新建文章', requiresAdminAuth: true } },
      { path: 'articles/:id', name: 'AdminArticleEdit', component: () => import('@/views/admin/AdminArticleEditor.vue'), meta: { title: '编辑文章', requiresAdminAuth: true } },
      { path: 'patent-sources', name: 'AdminPatentSources', component: () => import('@/views/admin/AdminPatentSources.vue'), meta: { title: '专利数据源', requiresAdminAuth: true } },
      { path: 'patent-datasets', name: 'AdminPatentDatasets', component: () => import('@/views/admin/AdminPatentDatasets.vue'), meta: { title: '专利数据集', requiresAdminAuth: true } },
      { path: 'sql', name: 'AdminSql', component: () => import('@/views/admin/AdminSqlConsole.vue'), meta: { title: 'SQL 控制台', requiresAdminAuth: true } }
    ]
  },
  {
    path: '/admin/audit-logs',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAdminAuth: true, transition: 'fade' },
    children: [
      { path: '', name: 'AdminAuditLogs', component: () => import('@/views/admin/AdminAuditLogs.vue'), meta: { title: '审计日志', requiresAdminAuth: true } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition ?? { top: 0 }
  }
})

router.beforeEach((to, _from, next) => {
  document.title = to.meta.title ? `${String(to.meta.title)} - ${PLATFORM_NAME}` : PLATFORM_NAME

  const userToken = localStorage.getItem('token')
  const adminToken = localStorage.getItem('adminToken')

  if (to.path === '/admin/login' && adminToken) {
    next('/admin/home')
    return
  }

  if ((to.path === '/login' || to.path === '/register') && userToken) {
    next('/')
    return
  }

  if (to.meta.requiresAdminAuth && !adminToken) {
    ElMessage.warning('请先登录后台')
    next({ path: '/admin/login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAuth && !userToken) {
    ElMessage.warning('请先登录')
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  next()
})

export default router
