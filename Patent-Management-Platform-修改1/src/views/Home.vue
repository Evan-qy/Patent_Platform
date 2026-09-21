<template>
  <div class="home-page">
    <HomeSkeleton v-if="loading" />
    <template v-else>
      <button class="ai-float-button" :class="{ active: showAIChat }" @click="toggleAIChat">
        <el-icon><ChatDotRound /></el-icon>
        <span>AI 咨询</span>
      </button>

      <div v-if="showAIChat" class="ai-chat-overlay" @click="closeAIChat">
        <div class="ai-chat-container" @click.stop :style="aiChatContainerStyle">
          <AIChat />
          <button class="close-button" @click="closeAIChat">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </div>

      <header class="home-header">
        <div class="container header-stack">
          <div class="header-top">
            <button class="logo" @click="router.push('/')">
              <el-icon class="logo-icon"><Cpu /></el-icon>
              <span>{{ PLATFORM_NAME }}</span>
            </button>
            <div class="header-actions">
              <template v-if="!isLogin">
                <el-button @click="goToLogin">登录</el-button>
                <el-button type="primary" @click="goToRegister">注册</el-button>
              </template>
              <template v-else>
                <el-button @click="goToProfile">{{ userDisplayName }}</el-button>
                <el-button type="primary" plain @click="logout">退出登录</el-button>
              </template>
            </div>
          </div>

          <nav class="nav-menu">
            <button
              v-for="item in visibleNavItems"
              :key="item.key"
              class="nav-link"
              @click="handleNavSelect(item.key)"
            >
              {{ item.label }}
            </button>
            <button v-if="isMobile" class="nav-link more-link" @click="showMoreMenu = !showMoreMenu">更多</button>
          </nav>

          <div v-if="isMobile && showMoreMenu" class="mobile-more-panel">
            <button
              v-for="item in overflowNavItems"
              :key="item.key"
              class="nav-link more-panel-link"
              @click="handleMoreNav(item.key)"
            >
              {{ item.label }}
            </button>
          </div>
        </div>
      </header>

      <main>
        <section class="hero">
          <div class="container hero-grid">
            <div class="hero-copy">
              <span class="eyebrow">高校知识产权运营服务平台</span>
              <h1>{{ homeContent.heroTitle }}</h1>
              <p class="slogan">{{ homeContent.brandSlogan }}</p>
              <p class="desc">{{ homeContent.heroDescription }}</p>

              <div class="hero-actions">
                <el-button type="primary" size="large" @click="goToPatentSearch">立即检索专利</el-button>
                <el-button size="large" @click="goToRequirementPlaza">查看需求广场</el-button>
                <el-button v-if="!isMobile" size="large" @click="goToRequirementPublish">发布技术需求</el-button>
              </div>

              <div v-if="!isMobile" class="highlight-list">
                <span v-for="(item, index) in heroHighlights" :key="index">{{ item }}</span>
              </div>
            </div>

            <div class="hero-side">
              <section class="quick-search-card">
                <div>
                  <span class="mini-label">快速入口</span>
                  <h2>从一个关键词开始找成果</h2>
                  <p>输入技术方向、应用场景或公开号，直接进入专利数据库检索。</p>
                </div>
                <el-input
                  v-model="quickSearchKeyword"
                  class="quick-search-input"
                  placeholder="如：储能材料 / CN 公开号"
                  @keyup.enter="submitQuickSearch"
                />
                <el-button type="primary" size="large" @click="submitQuickSearch">搜索专利</el-button>
              </section>

              <section class="presence-card">
                <div class="presence-head">
                  <span class="presence-pill">
                    <span class="presence-dot"></span>
                    在线 {{ presence.onlineUsers }} 人
                  </span>
                </div>
                <div class="presence-stats">
                  <article>
                    <strong>{{ presence.onlineUsers }}</strong>
                    <span>总在线</span>
                  </article>
                  <article>
                    <strong>{{ presence.authenticatedUsers }}</strong>
                    <span>已登录</span>
                  </article>
                  <article>
                    <strong>{{ presence.guestUsers }}</strong>
                    <span>访客</span>
                  </article>
                </div>
                <div v-if="heroPresenceUsers.length" class="presence-users">
                  <span v-for="user in heroPresenceUsers" :key="`${user.type}-${user.displayName}-${user.page}`">
                    {{ user.displayName }} · {{ formatPresencePage(user.page) }}
                  </span>
                </div>
              </section>
            </div>
          </div>
        </section>

        <section class="section metric-section">
          <div class="container">
            <div class="metric-list">
              <article v-for="(item, index) in heroMetrics" :key="index" class="metric-card">
                <strong>{{ item.value }}</strong>
                <span>{{ item.label }}</span>
                <p>{{ item.detail }}</p>
              </article>
            </div>
          </div>
        </section>

        <section class="section">
          <div class="container">
            <div class="section-head">
              <h2>{{ homeContent.coreFeaturesTitle }}</h2>
              <p>{{ homeContent.coreFeaturesIntro }}</p>
            </div>
            <div class="feature-grid">
              <FeatureCard
                v-for="(item, index) in renderedFeatures"
                :key="`${item.to}-${index}`"
                :icon="item.iconComponent"
                :title="item.title"
                :description="item.description"
                :detail="item.detail"
                :more-label="item.moreLabel"
                :to="item.to"
                @click="handleFeatureClick"
              />
            </div>
          </div>
        </section>

        <section class="section alt">
          <div class="container">
            <div class="section-head">
              <h2>平台优势</h2>
              <p>围绕专利检索、价值评估、需求匹配和成果转化，提供稳定可落地的业务链路。</p>
            </div>
            <div class="card-grid">
              <article v-for="(item, index) in renderedAdvantages" :key="index" class="info-card">
                <el-icon class="card-icon"><component :is="item.iconComponent" /></el-icon>
                <h3>{{ item.title }}</h3>
                <p>{{ item.description }}</p>
              </article>
            </div>
          </div>
        </section>

        <section class="section">
          <div class="container">
            <div class="section-head">
              <h2>{{ homeContent.articlesSectionTitle }}</h2>
              <p>{{ homeContent.articlesSectionSubtitle }}</p>
            </div>
            <div v-if="hasCmsArticles" class="article-section-bar">
              <span>后台文章已接入</span>
              <el-button text type="primary" @click="openFirstArticle">查看最新文章</el-button>
            </div>
            <div class="article-grid">
              <article v-for="item in articleCards" :key="item.key" class="article-card" @click="openArticle(item)">
                <div class="article-cover" :style="{ backgroundImage: `url(${item.image})` }"></div>
                <div class="article-body">
                  <span class="article-status">{{ item.status }}</span>
                  <h3>{{ item.title }}</h3>
                  <p>{{ item.description }}</p>
                  <div class="article-meta">
                    <span><el-icon><OfficeBuilding /></el-icon>{{ item.company }}</span>
                    <span><el-icon><Calendar /></el-icon>{{ formatCaseTime(item.time) }}</span>
                  </div>
                </div>
              </article>
            </div>
          </div>
        </section>

        <section class="section">
          <div class="container">
            <div class="section-head">
              <h2>服务保障</h2>
              <p>平台整合数据、专家和业务流程，支持从咨询到落地的持续跟进。</p>
            </div>
            <div class="card-grid">
              <article v-for="item in renderedPromises" :key="item.title" class="info-card">
                <el-icon class="card-icon"><component :is="item.iconComponent" /></el-icon>
                <h3>{{ item.title }}</h3>
                <p>{{ item.text }}</p>
              </article>
            </div>
          </div>
        </section>
      </main>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw, onMounted, onUnmounted, reactive, ref, type Component } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, ChatDotRound, Close, Cpu, DataAnalysis, DocumentChecked, EditPen, House, Lightning, Lock, Message, Money, OfficeBuilding, Service, Star, TrendCharts, User, UserFilled } from '@element-plus/icons-vue'
import { authApi, presenceApi } from '@/api'
import { cmsApi } from '@/api/cms'
import { PLATFORM_NAME } from '@/constants/brand'
import HomeSkeleton from '@/components/HomeSkeleton.vue'
import FeatureCard from '@/components/FeatureCard.vue'
import AIChat from '@/components/AIChat.vue'
import type { PresenceStatus } from '@/types'
import type { CmsArticle, HomeAdvantageItem, HomeContentPayload, HomeFeatureItem, HomeMetricItem, HomeServicePromiseItem } from '@/types/cms'

const createClientId = (prefix: string) =>
  globalThis.crypto?.randomUUID
    ? `${prefix}-${globalThis.crypto.randomUUID()}`
    : `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`

type RenderFeature = HomeFeatureItem & { iconComponent: Component }
type RenderAdvantage = HomeAdvantageItem & { iconComponent: Component }
type RenderPromise = HomeServicePromiseItem & { iconComponent: Component }
type ArticleCard = { key: string; slug?: string; title: string; status: string; description: string; company: string; time?: string | null; image: string; fullStory?: string }

const DEFAULT_CASES: ArticleCard[] = [
  { key: '1', title: '高分子复合材料抗病毒成果转化', status: '成果转化', description: '完成专利转让与作价入股，形成校企协同转化样板。', company: '河北凯尔威生物技术有限公司', time: '2020-08-01', image: 'https://picsum.photos/seed/transformation-case-1/640/360', fullStory: '该案例展示了从成果形成到企业承接的完整链路。' },
  { key: '2', title: '海水提钾技术转让落地', status: '成果转化', description: '完成多项专利打包转让，并推进试车和产业化。', company: '厦门东方银祥有限公司', time: '2022-05-01', image: 'https://picsum.photos/seed/transformation-case-2/640/360', fullStory: '该项目覆盖技术中试和产业化验证。' },
  { key: '3', title: '电解液联产装置专利许可', status: '专利许可', description: '通过专利许可推进大型化工装置应用。', company: '陕西北元化工集团股份有限公司', time: '2022-10-01', image: 'https://picsum.photos/seed/transformation-case-3/640/360', fullStory: '案例重点在于许可模式设计与产业化衔接。' }
]

const createPresenceState = (): PresenceStatus => ({
  onlineUsers: 1,
  authenticatedUsers: 0,
  guestUsers: 1,
  heartbeatWindowSeconds: 120,
  activeUsers: []
})

const router = useRouter()
const loading = ref(true)
const isLogin = ref(Boolean(localStorage.getItem('token')))
const isMobile = ref(window.innerWidth <= 768)
const showAIChat = ref(false)
const showMoreMenu = ref(false)
const articles = ref<CmsArticle[]>([])
const quickSearchKeyword = ref('')
const userInfo = reactive({ username: '', nickname: '', userId: 0 })
const presence = ref<PresenceStatus>(createPresenceState())
const presenceClientId = createClientId('home')
let presenceTimer: number | undefined

const iconMap: Record<string, Component> = {
  DocumentChecked: markRaw(DocumentChecked),
  User: markRaw(User),
  UserFilled: markRaw(UserFilled),
  EditPen: markRaw(EditPen),
  DataAnalysis: markRaw(DataAnalysis),
  TrendCharts: markRaw(TrendCharts),
  Star: markRaw(Star),
  Lightning: markRaw(Lightning),
  Lock: markRaw(Lock),
  Service: markRaw(Service),
  Money: markRaw(Money),
  House: markRaw(House),
  Cpu: markRaw(Cpu),
  Message: markRaw(Message)
}

const createDefaultHomeContent = (): HomeContentPayload => ({
  heroTitle: PLATFORM_NAME,
  brandSlogan: '让专利检索、需求对接与成果转化形成一条高效协同链路。',
  heroDescription: '面向高校、科研团队与合作企业，提供专利检索、需求发布、价值评估、智能匹配与成果转化的一体化运营服务。',
  coreFeaturesTitle: '核心功能',
  coreFeaturesIntro: '聚焦知识产权运营全流程，把检索、评估、撮合和落地入口集中到一个工作台。',
  heroMetrics: [
    { value: '120+', label: '服务高校', detail: '覆盖重点院校、科研团队与成果运营机构' },
    { value: '3200+', label: '入库专利成果', detail: '持续沉淀专利、需求与评估数据' },
    { value: '40%', label: '匹配效率提升', detail: '缩短检索、评估与转化推进周期' }
  ],
  heroHighlights: ['专利检索', '价值评估', '成果转化', 'AI 辅助分析'],
  features: [
    { icon: 'DocumentChecked', title: '专利检索', description: '通过数据库检索与 AI 智能搜索定位目标专利成果。', detail: '支持关键字、技术方向和公开号入口。', moreLabel: '查看能力', to: '/patent' },
    { icon: 'Message', title: '需求广场', description: '集中浏览平台已公开的技术需求与合作机会。', detail: '按方向和主题查看潜在合作项目。', moreLabel: '进入广场', to: '/requirement' },
    { icon: 'EditPen', title: '需求发布', description: '标准化录入技术需求，沉淀合作线索与转化机会。', detail: '适合高校、团队和企业发布问题。', moreLabel: '立即发布', to: '/demand' },
    { icon: 'TrendCharts', title: '价值评估', description: '固定模型下完成专利价值评估，快速形成报告结论。', detail: '从技术、市场、法律和经济维度分析。', moreLabel: '查看能力', to: '/evaluation' }
  ],
  advantages: [
    { icon: 'Star', title: '资源沉淀', description: '整合高校专利、需求与专家资源，形成统一运营入口。' },
    { icon: 'Lightning', title: 'AI 协同', description: '基于 AI 的检索、评估与问答流程，提升成果转化效率。' },
    { icon: 'Lock', title: '稳定可靠', description: '提供可审计、可追踪的数据与咨询链路。' }
  ],
  mobileOperationTips: [],
  mobileServicePromises: [
    { icon: 'Lightning', title: '快速响应', text: '需求提交后可快速推送对应成果与团队。' },
    { icon: 'Money', title: '链路完整', text: '评估、转化、交易链路一站打通。' },
    { icon: 'Service', title: '持续跟进', text: '提供专家与项目顾问协同支持。' }
  ],
  caseKpis: [],
  articlesSectionTitle: '转化案例与平台动态',
  articlesSectionSubtitle: '优先展示 CMS 文章，暂无内容时使用平台内置案例。'
})

const homeContent = reactive<HomeContentPayload>(createDefaultHomeContent())
const navItems = [
  { key: 'home', label: '首页' },
  { key: 'patent', label: '专利检索' },
  { key: 'requirement', label: '需求广场' },
  { key: 'demand', label: '需求发布' },
  { key: 'patent-manage', label: '专利管理' },
  { key: 'evaluation', label: '价值评估' },
  { key: 'transformation', label: '成果转化' }
]

const visibleNavItems = computed(() => (isMobile.value ? navItems.slice(0, 3) : navItems))
const overflowNavItems = computed(() => (isMobile.value ? navItems.slice(3) : []))
const userDisplayName = computed(() => userInfo.nickname || userInfo.username || '用户')
const heroPresenceUsers = computed(() => presence.value.activeUsers.slice(0, 4))
const aiChatContainerStyle = computed(() => (isMobile.value ? { width: '100vw', height: '100dvh' } : { width: '420px', height: 'min(780px, calc(100dvh - 40px))' }))

const resolveIcon = (iconKey: string | undefined, fallbackKey: string) => iconMap[iconKey || ''] || iconMap[fallbackKey] || iconMap.Cpu
const heroMetrics = computed(() => (homeContent.heroMetrics.length ? homeContent.heroMetrics : createDefaultHomeContent().heroMetrics).map((item: HomeMetricItem) => ({ value: item.value || '-', label: item.label || '未命名指标', detail: item.detail || '' })))
const heroHighlights = computed(() => (homeContent.heroHighlights.length ? homeContent.heroHighlights : createDefaultHomeContent().heroHighlights).filter(Boolean))
const renderedFeatures = computed<RenderFeature[]>(() => {
  const fallback = createDefaultHomeContent().features
  const source = homeContent.features.length ? homeContent.features : fallback
  return source.map((item, index) => ({
    ...item,
    title: item.title || fallback[index]?.title || `功能 ${index + 1}`,
    description: item.description || fallback[index]?.description || '',
    detail: item.detail || fallback[index]?.detail || '',
    moreLabel: item.moreLabel || fallback[index]?.moreLabel || '查看详情',
    to: item.to || fallback[index]?.to || '',
    iconComponent: resolveIcon(item.icon, fallback[index]?.icon || 'Cpu')
  }))
})
const renderedAdvantages = computed<RenderAdvantage[]>(() => {
  const fallback = createDefaultHomeContent().advantages
  const source = homeContent.advantages.length ? homeContent.advantages : fallback
  return source.map((item, index) => ({
    ...item,
    title: item.title || fallback[index]?.title || `优势 ${index + 1}`,
    description: item.description || fallback[index]?.description || '',
    iconComponent: resolveIcon(item.icon, fallback[index]?.icon || 'Star')
  }))
})
const renderedPromises = computed<RenderPromise[]>(() => {
  const fallback = createDefaultHomeContent().mobileServicePromises
  const source = homeContent.mobileServicePromises.length ? homeContent.mobileServicePromises : fallback
  return source.map((item, index) => ({
    ...item,
    title: item.title || fallback[index]?.title || `服务 ${index + 1}`,
    text: item.text || fallback[index]?.text || '',
    iconComponent: resolveIcon(item.icon, fallback[index]?.icon || 'Service')
  }))
})
const cmsArticleCards = computed(() =>
  articles.value
    .filter(item => item.status === 'PUBLISHED' && !(item.slug || '').startsWith('codex-') && !(item.title || '').includes('????'))
    .sort((a, b) => {
      const left = new Date(a.publishedAt || a.updatedAt || a.createdAt || 0).getTime()
      const right = new Date(b.publishedAt || b.updatedAt || b.createdAt || 0).getTime()
      return right - left || (a.sortOrder || 0) - (b.sortOrder || 0)
    })
)
const hasCmsArticles = computed(() => cmsArticleCards.value.length > 0)
const articleCards = computed<ArticleCard[]>(() => {
  const visible = cmsArticleCards.value
  if (visible.length) {
    return visible.slice(0, 6).map((item, index) => ({
      key: `${item.id}`,
      slug: item.slug,
      title: item.title,
      status: '平台动态',
      description: item.summary || '点击查看文章详情。',
      company: '平台文章',
      time: item.publishedAt || item.updatedAt || item.createdAt,
      image: item.coverUrl || `https://picsum.photos/seed/home-article-${index + 1}/640/360`
    }))
  }
  return DEFAULT_CASES
})

const formatPresencePage = (page: string) => !page || page === '/' ? '首页' : page.startsWith('/login') ? '登录页' : page.startsWith('/register') ? '注册页' : page.startsWith('/patent') ? '专利检索' : page.startsWith('/requirement') ? '需求广场' : page.startsWith('/demand') ? '需求发布' : page.startsWith('/profile') ? '个人中心' : page.replace(/^\//, '')
const updateViewport = () => { isMobile.value = window.innerWidth <= 768; if (!isMobile.value) showMoreMenu.value = false }
const toggleAIChat = () => { showAIChat.value = !showAIChat.value }
const closeAIChat = () => { showAIChat.value = false }
const requireLoginAndGo = (target: string) => { if (isLogin.value) { router.push(target); return } ElMessage.warning('请先登录后再使用该功能'); router.push(`/login?redirect=${encodeURIComponent(target)}`) }
const handleNavSelect = (key: string) => { if (key === 'home') router.push('/'); else if (key === 'patent') router.push('/patent'); else if (key === 'patent-manage') requireLoginAndGo('/patent/manage'); else if (key === 'requirement') router.push('/requirement'); else if (key === 'demand') requireLoginAndGo('/demand'); else if (key === 'evaluation') requireLoginAndGo('/evaluation'); else if (key === 'transformation') requireLoginAndGo('/transformation') }
const handleMoreNav = (key: string) => { showMoreMenu.value = false; handleNavSelect(key) }
const handleFeatureClick = (to: string) => { if (!to) return; if (to === '/patent' || to === '/requirement') { router.push(to); return } requireLoginAndGo(to) }
const goToPatentSearch = () => router.push('/patent')
const goToRequirementPlaza = () => router.push('/requirement')
const goToRequirementPublish = () => requireLoginAndGo('/demand')
const submitQuickSearch = () => {
  const keyword = quickSearchKeyword.value.trim()
  if (!keyword) {
    router.push('/patent')
    return
  }
  router.push(`/patent?q=${encodeURIComponent(keyword)}`)
}
const goToLogin = () => router.push('/login')
const goToRegister = () => router.push('/register')
const goToProfile = () => router.push('/profile')
const openArticle = (item: ArticleCard) => { if (item.slug) { router.push(`/articles/${item.slug}`); return } if (item.fullStory) { void ElMessageBox.alert(item.fullStory, item.title, { confirmButtonText: '关闭', customClass: 'home-case-story-dialog', dangerouslyUseHTMLString: false }); return } ElMessage.info('当前内容暂无详情页') }
const openFirstArticle = () => { const first = articleCards.value[0]; if (first) openArticle(first) }
const formatCaseTime = (value?: string | null) => (value ? new Date(value).toLocaleDateString('zh-CN') : '最近更新')
const logout = () => { localStorage.removeItem('token'); isLogin.value = false; Object.assign(userInfo, { username: '', nickname: '', userId: 0 }); router.push('/') }
const loadHomeContent = async () => { try { Object.assign(homeContent, createDefaultHomeContent(), await cmsApi.getHomeContent()) } catch (error) { console.error('加载首页 CMS 失败', error) } }
const loadArticles = async () => { try { articles.value = await cmsApi.getArticles() } catch (error) { console.error('加载文章失败', error) } }
const loadCurrentUser = async () => { if (!localStorage.getItem('token')) return; try { const current = await authApi.getCurrentUser(); Object.assign(userInfo, current?.user || current || {}) } catch (error: any) { if (error?.response?.data?.code === 401 || error?.response?.status === 401) { localStorage.removeItem('token'); isLogin.value = false } } }
const syncPresence = async () => { try { const status = await presenceApi.heartbeat({ clientId: presenceClientId, page: router.currentRoute.value.fullPath || '/' }); presence.value = { ...createPresenceState(), ...status, onlineUsers: Math.max(1, status.onlineUsers || 1), authenticatedUsers: Math.max(0, status.authenticatedUsers || 0), guestUsers: Math.max(0, status.guestUsers || 0), activeUsers: status.activeUsers || [] } } catch (error) { console.error('同步在线状态失败', error) } }
const startPresenceHeartbeat = () => { void syncPresence(); presenceTimer = window.setInterval(() => { void syncPresence() }, 30000) }
const stopPresenceHeartbeat = () => { if (presenceTimer) { window.clearInterval(presenceTimer); presenceTimer = undefined } }

onMounted(async () => {
  window.addEventListener('resize', updateViewport)
  startPresenceHeartbeat()
  try {
    await Promise.all([loadHomeContent(), loadArticles(), loadCurrentUser()])
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', updateViewport)
  stopPresenceHeartbeat()
})
</script>

<style scoped>
.home-page { min-height: 100vh; background: linear-gradient(180deg, #f7fbff 0%, #eef6f3 52%, #f8faf6 100%); color: var(--text-primary); }
.container { width: min(calc(100% - 32px), 1180px); margin: 0 auto; }
.home-header { position: sticky; top: 0; z-index: 20; background: rgba(246, 251, 255, 0.94); backdrop-filter: blur(16px); border-bottom: 1px solid rgba(16, 38, 63, 0.08); }
.header-stack { display: grid; gap: 10px; padding: 12px 0; }
.header-top { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.logo { display: inline-flex; align-items: center; gap: 10px; border: none; background: none; font-size: 18px; font-weight: 700; color: inherit; cursor: pointer; }
.logo-icon { color: #409eff; font-size: 26px; }
.nav-menu { display: flex; gap: 6px; overflow-x: auto; padding-bottom: 4px; }
.nav-link { border: none; background: none; padding: 8px 12px; border-radius: 999px; color: #4e6686; font-weight: 600; font-size: 14px; cursor: pointer; white-space: nowrap; }
.nav-link:hover { background: rgba(64, 158, 255, 0.1); color: #2376db; }
.mobile-more-panel { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.more-panel-link { background: rgba(64, 158, 255, 0.08); }
.header-actions { display: flex; gap: 10px; }
.hero { padding: 20px 0 0; }
.hero .container { width: min(calc(100% - 32px), 1180px); }
.hero-grid,
.feature-grid,
.card-grid,
.article-grid,
.metric-list { display: grid; gap: 18px; }
.hero-grid { grid-template-columns: minmax(0, 1.1fr) minmax(340px, 0.9fr); align-items: stretch; padding: 48px clamp(24px, 6vw, 72px); border-radius: 28px; background: linear-gradient(135deg, #173b63 0%, #246160 58%, #8a6a28 100%); box-shadow: 0 26px 70px rgba(20, 50, 70, 0.18); }
.eyebrow { display: inline-flex; padding: 8px 14px; border-radius: 999px; background: rgba(255, 255, 255, 0.12); color: rgba(255, 255, 255, 0.9); font-size: 12px; font-weight: 700; }
.hero-copy h1 { margin: 18px 0 10px; font-size: clamp(42px, 5vw, 64px); line-height: 1.04; color: #fff; }
.slogan { margin: 0 0 14px; color: #d7f1ff; font-size: 22px; font-weight: 700; }
.desc, .section-head p, .info-card p, .article-body p { margin: 0; color: #5b708c; line-height: 1.8; }
.hero-copy .desc { max-width: 640px; color: rgba(232, 244, 255, 0.82); font-size: 16px; }
.hero-actions, .highlight-list { display: flex; flex-wrap: wrap; gap: 10px; }
.hero-actions { margin-top: 22px; }
.highlight-list { margin-top: 18px; }
.highlight-list span { padding: 8px 12px; border-radius: 999px; background: rgba(255, 255, 255, 0.12); color: rgba(255, 255, 255, 0.92); font-weight: 600; }
.hero-side { display: grid; gap: 14px; align-content: start; }
.quick-search-card,
.presence-card,
.metric-card { padding: 18px; border-radius: 18px; background: rgba(255, 255, 255, 0.1); border: 1px solid rgba(255, 255, 255, 0.16); backdrop-filter: blur(12px); color: #fff; }
.quick-search-card { display: grid; gap: 14px; background: rgba(255, 255, 255, 0.14); }
.mini-label { display: inline-flex; color: #d7f1ff; font-size: 12px; font-weight: 800; }
.quick-search-card h2 { margin: 6px 0 8px; font-size: 24px; line-height: 1.35; }
.quick-search-card p { margin: 0; color: rgba(238, 247, 255, 0.78); line-height: 1.7; }
.quick-search-input :deep(.el-input__wrapper) { min-height: 46px; background: rgba(255, 255, 255, 0.94); box-shadow: none; }
.presence-pill { display: inline-flex; align-items: center; gap: 8px; padding: 8px 14px; border-radius: 999px; background: rgba(255, 255, 255, 0.12); border: 1px solid rgba(255, 255, 255, 0.18); }
.presence-dot { width: 8px; height: 8px; border-radius: 50%; background: #4af0b2; box-shadow: 0 0 0 4px rgba(74, 240, 178, 0.16); }
.presence-stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin-top: 14px; }
.presence-stats article { text-align: center; padding: 10px; border-radius: 16px; background: rgba(255, 255, 255, 0.08); }
.presence-stats strong { display: block; font-size: 24px; }
.presence-stats span { font-size: 12px; color: rgba(240, 247, 255, 0.78); }
.presence-users { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
.presence-users span { padding: 8px 10px; border-radius: 999px; background: rgba(255, 255, 255, 0.12); font-size: 12px; font-weight: 600; }
.metric-section { padding-top: 20px; }
.metric-list { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.metric-card { background: rgba(255, 255, 255, 0.86); color: #12355c; box-shadow: 0 18px 36px rgba(19, 43, 74, 0.08); }
.metric-card strong { display: block; font-size: 34px; color: #12355c; }
.metric-card span { display: block; margin-top: 4px; font-size: 16px; font-weight: 700; color: #12355c; }
.metric-card p { margin-top: 10px; color: #5b708c; }
.section { padding-top: 28px; }
.section-head { max-width: 760px; margin: 0 auto 22px; text-align: center; }
.section-head h2 { margin: 0 0 10px; font-size: clamp(28px, 4vw, 40px); }
.feature-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.card-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.info-card { padding: 26px 24px; border-radius: 18px; background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 250, 247, 0.94)); border: 1px solid rgba(48, 112, 92, 0.16); box-shadow: 0 20px 44px rgba(19, 43, 74, 0.08); }
.card-icon { display: inline-flex; align-items: center; justify-content: center; width: 56px; height: 56px; border-radius: 16px; margin-bottom: 18px; font-size: 28px; color: #1f8f70; background: linear-gradient(135deg, rgba(47, 128, 237, 0.1), rgba(230, 166, 64, 0.16)); }
.info-card h3 { margin: 0 0 10px; color: #12355c; font-size: 24px; }
.article-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.article-section-bar { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin: -8px 0 16px; padding: 12px 16px; border-radius: 14px; background: rgba(31, 143, 112, 0.08); color: #1d6f5a; font-weight: 800; }
.article-card { border-radius: 18px; overflow: hidden; cursor: pointer; background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 250, 247, 0.94)); box-shadow: 0 20px 44px rgba(19, 43, 74, 0.08); display: flex; flex-direction: column; transition: transform .2s ease, box-shadow .2s ease; }
.article-card:hover { transform: translateY(-3px); box-shadow: 0 26px 54px rgba(19, 43, 74, 0.14); }
.article-cover { height: 200px; background-size: cover; background-position: center; background-color: #173b63; }
.article-body { flex: 1; display: flex; flex-direction: column; padding: 20px; }
.article-status { display: inline-flex; padding: 6px 10px; border-radius: 999px; background: rgba(31, 143, 112, 0.12); color: #1d6f5a; font-weight: 700; font-size: 12px; }
.article-body h3 { margin: 12px 0 8px; color: #12355c; font-size: clamp(17px, 1.1vw, 20px); line-height: 1.45; }
.article-meta { margin-top: auto; padding-top: 14px; display: flex; flex-direction: column; gap: 8px; color: #6c7f93; }
.article-meta span { display: inline-flex; align-items: center; gap: 8px; }
.ai-float-button { position: fixed; right: 20px; bottom: 20px; z-index: 1000; background: linear-gradient(135deg, #409eff 0%, #67c23a 100%); color: #fff; padding: 12px 16px; border-radius: 50px; box-shadow: 0 4px 20px rgba(64, 158, 255, 0.4); cursor: pointer; display: flex; align-items: center; gap: 8px; border: none; }
.ai-chat-overlay { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.5); z-index: 2000; display: flex; justify-content: flex-end; }
.ai-chat-container { background: #fff; position: absolute; top: 20px; right: 20px; border-radius: 24px; overflow: hidden; box-shadow: -2px 0 20px rgba(0, 0, 0, 0.1); }
.close-button { position: absolute; top: 16px; right: 16px; width: 32px; height: 32px; border-radius: 50%; border: none; background: #f5f7fa; display: flex; align-items: center; justify-content: center; cursor: pointer; z-index: 20; }

@media (min-width: 769px) {
  .hero .container {
    width: min(calc(100% - 10px), 1880px);
  }
}

@media (max-width: 900px) {
  .hero-grid,
  .feature-grid,
  .card-grid,
  .article-grid,
  .metric-list {
    grid-template-columns: 1fr;
  }

  .hero-grid {
    padding: 32px 20px;
  }
}

@media (max-width: 768px) {
  .container { width: min(calc(100% - 20px), 1180px); }
  .header-top { flex-direction: column; align-items: stretch; }
  .logo { justify-content: center; }
  .header-actions { display: grid; grid-template-columns: 1fr 1fr; }
  .header-actions :deep(.el-button) { width: 100%; margin: 0; }
  .hero-grid { padding: 24px 18px; gap: 14px; }
  .hero-copy h1 { font-size: clamp(34px, 13vw, 44px); }
  .slogan { font-size: 18px; }
  .hero-copy .desc { font-size: 14px; }
  .hero-actions { display: grid; grid-template-columns: 1fr; }
  .hero-actions :deep(.el-button) { width: 100%; margin: 0; }
  .quick-search-card :deep(.el-button) { width: 100%; margin: 0; }
  .presence-users { overflow-x: auto; flex-wrap: nowrap; padding-bottom: 4px; scrollbar-width: none; }
  .presence-users::-webkit-scrollbar { display: none; }
  .presence-users span { flex: 0 0 auto; }
  .metric-section { padding-top: 16px; }
  .metric-list { gap: 14px; }
  .ai-float-button { bottom: 92px; }
  .ai-chat-overlay { align-items: flex-end; }
  .ai-chat-container { inset: auto 0 0 0; width: 100% !important; height: min(100dvh - 8px, 100%); border-radius: 20px 20px 0 0; }
  .article-section-bar { align-items: flex-start; flex-direction: column; }
}

@media (max-width: 480px) {
  .container { width: min(calc(100% - 16px), 1180px); }
  .hero-grid { padding: 20px 12px; }
  .hero-copy h1 { font-size: clamp(30px, 12vw, 38px); }
  .slogan { font-size: 16px; }
  .presence-stats { grid-template-columns: 1fr; }
  .article-cover { height: 168px; }
}
</style>

<style>
.home-case-story-dialog.el-message-box { max-width: min(560px, 92vw); }
.home-case-story-dialog .el-message-box__message { white-space: pre-wrap; max-height: min(70vh, 520px); overflow-y: auto; padding-right: 6px; line-height: 1.75; text-align: left; }
</style>
