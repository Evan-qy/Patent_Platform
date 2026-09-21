<template>
  <div class="auth-container">
    <div class="bg-shape shape-1"></div>
    <div class="bg-shape shape-2"></div>
    <div class="bg-shape shape-3"></div>

    <div class="auth-shell">
      <div class="auth-card panel-glow">
        <div class="auth-header">
          <div class="brand-row">
            <div class="brand-badge">{{ PLATFORM_NAME }}</div>
            <div class="presence-pill">
              <span class="presence-dot"></span>
              在线 {{ presence.onlineUsers }} 人
            </div>
          </div>

          <div class="presence-panel">
            <div class="presence-stats">
              <div class="presence-stat">
                <strong>{{ presence.onlineUsers }}</strong>
                <span>当前在线</span>
              </div>
              <div class="presence-stat">
                <strong>{{ presence.authenticatedUsers }}</strong>
                <span>已登录</span>
              </div>
              <div class="presence-stat">
                <strong>{{ presence.guestUsers }}</strong>
                <span>访客</span>
              </div>
            </div>
            <div v-if="compactPresenceUsers.length" class="presence-list">
              <span
                v-for="user in compactPresenceUsers"
                :key="`${user.type}-${user.displayName}-${user.page}`"
                class="presence-user-chip"
              >
                {{ user.displayName }} · {{ formatPresencePage(user.page) }}
              </span>
            </div>
          </div>

          <div class="logo-icon-wrapper">
            <el-icon class="logo-icon"><CollectionTag /></el-icon>
          </div>
          <h1 class="auth-title">欢迎回来</h1>
          <p class="auth-subtitle">登录后继续进行专利检索、需求对接与成果转化。</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="账号" prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名、邮箱或手机号"
              size="large"
              clearable
              class="glass-input"
              autocomplete="username"
              autocapitalize="off"
              autocorrect="off"
              spellcheck="false"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon class="input-icon"><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              clearable
              show-password
              class="glass-input"
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon class="input-icon"><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="验证码" prop="captchaAnswer">
            <div class="captcha-row">
              <el-input
                v-model="loginForm.captchaAnswer"
                placeholder="请输入计算结果"
                size="large"
                clearable
                class="glass-input captcha-input"
                inputmode="numeric"
                @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <el-icon class="input-icon"><Key /></el-icon>
                </template>
              </el-input>
              <button type="button" class="captcha-card" :disabled="captchaLoading" @click="refreshCaptcha">
                <span class="captcha-label">验证码</span>
                <strong>{{ captchaQuestion }}</strong>
                <small>{{ captchaLoading ? '刷新中...' : '点击刷新' }}</small>
              </button>
            </div>
          </el-form-item>

          <div class="form-actions">
            <el-checkbox v-model="rememberMe" class="remember-me">记住账号</el-checkbox>
            <span class="help-text">忘记密码请联系管理员</span>
          </div>

          <el-form-item>
            <el-button
              type="primary"
              native-type="submit"
              :loading="loading"
              size="large"
              class="login-button btn-primary"
              style="width: 100%"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form-item>

          <div class="login-footer">
            <span class="no-account">还没有账号？</span>
            <el-link type="primary" @click="goToRegister" class="register-link">立即注册</el-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CollectionTag, Key, Lock, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { authApi, presenceApi } from '@/api'
import { getApiErrorMessage, isServiceUnavailableError } from '@/lib/api-errors'
import type { LoginCaptcha, LoginParams, PresenceStatus, PresenceUserSummary } from '@/types'
import { PLATFORM_NAME } from '@/constants/brand'

const createClientId = (prefix: string) => {
  const webCrypto = globalThis.crypto
  if (webCrypto && typeof webCrypto.randomUUID === 'function') {
    return `${prefix}-${webCrypto.randomUUID()}`
  }
  if (webCrypto && typeof webCrypto.getRandomValues === 'function') {
    const bytes = new Uint8Array(16)
    webCrypto.getRandomValues(bytes)
    const id = Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
    return `${prefix}-${id}`
  }
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

const createPresenceState = (): PresenceStatus => ({
  onlineUsers: 1,
  authenticatedUsers: 0,
  guestUsers: 1,
  heartbeatWindowSeconds: 120,
  activeUsers: []
})

const router = useRouter()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const captchaLoading = ref(false)
const rememberMe = ref(false)
const captchaQuestion = ref('加载中...')
const presence = ref<PresenceStatus>(createPresenceState())
const presenceClientId = createClientId('login')
let presenceTimer: number | undefined

const compactPresenceUsers = computed(() => presence.value.activeUsers.slice(0, 4))

const loginForm = reactive<LoginParams>({
  username: '',
  password: '',
  captchaId: '',
  captchaAnswer: ''
})

const loginRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 50, message: '账号长度需在 3 到 50 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 到 20 个字符之间', trigger: 'blur' }
  ],
  captchaAnswer: [
    { required: true, message: '请输入验证码结果', trigger: 'blur' }
  ]
})

const formatPresencePage = (page: string) => {
  if (!page || page === '/') return '首页'
  if (page.startsWith('/login')) return '登录页'
  if (page.startsWith('/register')) return '注册页'
  if (page.startsWith('/patent')) return '专利检索'
  if (page.startsWith('/requirement')) return '需求广场'
  return page.replace(/^\//, '')
}

const normalizePresence = (status?: Partial<PresenceStatus>) => ({
  ...createPresenceState(),
  ...status,
  onlineUsers: Math.max(1, status?.onlineUsers || 1),
  authenticatedUsers: Math.max(0, status?.authenticatedUsers || 0),
  guestUsers: Math.max(0, status?.guestUsers || 0),
  activeUsers: (status?.activeUsers || []) as PresenceUserSummary[]
})

const refreshCaptcha = async () => {
  try {
    captchaLoading.value = true
    const captcha = await authApi.getCaptcha() as LoginCaptcha
    loginForm.captchaId = captcha.captchaId
    loginForm.captchaAnswer = ''
    captchaQuestion.value = captcha.question
  } catch (error: any) {
    captchaQuestion.value = '加载失败'
    ElMessage.error(error?.message || '验证码加载失败')
  } finally {
    captchaLoading.value = false
  }
}

const syncPresence = async () => {
  try {
    const response = await presenceApi.heartbeat({
      clientId: presenceClientId,
      page: router.currentRoute.value.fullPath || '/login'
    })
    presence.value = normalizePresence(response)
  } catch {
    presence.value = normalizePresence(presence.value)
  }
}

const startPresenceHeartbeat = () => {
  void syncPresence()
  presenceTimer = window.setInterval(() => {
    void syncPresence()
  }, 30000)
}

const stopPresenceHeartbeat = () => {
  if (presenceTimer) {
    window.clearInterval(presenceTimer)
    presenceTimer = undefined
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      loading.value = true
      const response = await authApi.login(loginForm)
      const token = typeof response === 'string' ? response : (response as any)?.token
      if (!token || typeof token !== 'string') {
        throw new Error('登录返回格式异常')
      }

      localStorage.setItem('token', token)
      if (rememberMe.value) {
        localStorage.setItem('rememberedUsername', loginForm.username)
      } else {
        localStorage.removeItem('rememberedUsername')
      }

      await syncPresence()
      ElMessage.success('登录成功')
      const redirect = router.currentRoute.value.query.redirect as string
      router.push(redirect || '/')
    } catch (error: any) {
      ElMessage.error(error?.message || '登录失败，请检查账号、密码和验证码')
      await refreshCaptcha()
    } finally {
      loading.value = false
    }
  })
}

const goToRegister = () => {
  router.push('/register')
}

onMounted(async () => {
  const rememberedUsername = localStorage.getItem('rememberedUsername')
  if (rememberedUsername) {
    loginForm.username = rememberedUsername
    rememberMe.value = true
  }
  await refreshCaptcha()
  startPresenceHeartbeat()
})

onBeforeUnmount(() => {
  stopPresenceHeartbeat()
})
</script>

<style scoped>
.auth-container { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--auth-shell-bg); padding: 20px; position: relative; overflow: hidden; }
.bg-shape { position: absolute; border-radius: 50%; filter: blur(100px); z-index: 0; opacity: 0.28; }
.shape-1 { width: 500px; height: 500px; background: var(--brand-bright-blue); top: -100px; left: -100px; animation: float 15s infinite ease-in-out; }
.shape-2 { width: 400px; height: 400px; background: var(--brand-cyan); bottom: -50px; right: -50px; animation: float 20s infinite ease-in-out reverse; }
.shape-3 { width: 300px; height: 300px; background: var(--brand-teal-blue); top: 40%; left: 40%; animation: pulse 10s infinite ease-in-out; }
.auth-shell { width: 100%; max-width: 500px; position: relative; z-index: 2; }
@keyframes float { 0% { transform: translate(0, 0) rotate(0deg); } 50% { transform: translate(50px, 50px) rotate(10deg); } 100% { transform: translate(0, 0) rotate(0deg); } }
@keyframes pulse { 0% { transform: scale(1); opacity: 0.3; } 50% { transform: scale(1.2); opacity: 0.5; } 100% { transform: scale(1); opacity: 0.3; } }
.auth-card { width: 100%; padding: 34px 30px; z-index: 1; background: var(--auth-card-bg) !important; backdrop-filter: blur(16px) !important; border: 1px solid var(--auth-card-border) !important; box-shadow: var(--auth-card-shadow) !important; animation: slideUp 0.6s cubic-bezier(0.25, 0.8, 0.25, 1); border-radius: 24px; }
@keyframes slideUp { from { opacity: 0; transform: translateY(40px); } to { opacity: 1; transform: translateY(0); } }
.auth-header { text-align: center; margin-bottom: 26px; }
.brand-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.brand-badge { display: inline-flex; align-items: center; justify-content: center; max-width: min(100%, 260px); font-size: 11px; font-weight: 700; letter-spacing: 0.4px; line-height: 1.35; padding: 6px 12px; border-radius: 999px; color: #3a5f9b; background: rgba(61, 125, 255, 0.1); border: 1px solid rgba(61, 125, 255, 0.2); white-space: normal; word-break: break-word; text-align: left; }
.presence-pill { display: inline-flex; align-items: center; gap: 8px; padding: 6px 12px; border-radius: 999px; background: rgba(18, 191, 127, 0.1); color: #127f59; border: 1px solid rgba(18, 191, 127, 0.18); font-size: 12px; font-weight: 700; white-space: nowrap; }
.presence-dot { width: 8px; height: 8px; border-radius: 50%; background: #12bf7f; box-shadow: 0 0 0 4px rgba(18, 191, 127, 0.14); }
.presence-panel { margin-bottom: 18px; padding: 14px; border-radius: 18px; background: linear-gradient(180deg, rgba(244, 249, 255, 0.96), rgba(233, 242, 255, 0.94)); border: 1px solid rgba(61, 125, 255, 0.12); box-shadow: 0 16px 30px rgba(61, 125, 255, 0.08); text-align: left; }
.presence-stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.presence-stat { padding: 10px 8px; border-radius: 14px; background: rgba(255, 255, 255, 0.74); text-align: center; }
.presence-stat strong { display: block; font-size: 22px; color: #173b63; }
.presence-stat span { font-size: 12px; color: #6a86b0; }
.presence-list { margin-top: 12px; display: flex; gap: 8px; overflow-x: auto; padding-bottom: 2px; scrollbar-width: none; }
.presence-list::-webkit-scrollbar { display: none; }
.presence-user-chip { display: inline-flex; flex: 0 0 auto; align-items: center; padding: 7px 10px; border-radius: 999px; background: rgba(61, 125, 255, 0.1); color: #31598e; font-size: 12px; font-weight: 600; }
.logo-icon-wrapper { width: 58px; height: 58px; background: linear-gradient(135deg, #5d97ff 0%, #66c8ff 100%); border-radius: 14px; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; box-shadow: 0 10px 22px rgba(61, 125, 255, 0.3); }
.logo-icon { font-size: 32px; color: white; }
.auth-title { font-size: 30px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; letter-spacing: -0.5px; }
.auth-subtitle { color: var(--text-secondary); font-size: 14px; line-height: 1.7; margin: 0; }
.login-form { width: 100%; }
:deep(.el-input__wrapper) { background-color: rgba(255, 255, 255, 0.9) !important; box-shadow: 0 0 0 1px rgba(61, 125, 255, 0.18) inset !important; border-radius: 12px; padding-left: 12px; transition: all 0.3s ease; }
:deep(.el-input__wrapper:hover), :deep(.el-input__wrapper.is-focus) { background-color: rgba(255, 255, 255, 1) !important; box-shadow: 0 0 0 2px rgba(61, 125, 255, 0.52) inset !important; }
:deep(.el-input__inner) { color: var(--text-primary) !important; height: 48px; }
.input-icon { color: #6a86b0; font-size: 18px; opacity: 0.8; transition: all 0.3s ease; }
:deep(.el-input__wrapper.is-focus) .input-icon { opacity: 1; color: #3d7dff; transform: scale(1.1); }
:deep(.el-form-item__label) { color: var(--text-secondary); font-weight: 600; margin-bottom: 6px; }
.captcha-row { display: grid; grid-template-columns: minmax(0, 1fr) 148px; gap: 12px; align-items: stretch; width: 100%; }
.captcha-card { border: 1px solid rgba(61, 125, 255, 0.18); border-radius: 14px; background: linear-gradient(180deg, rgba(244, 249, 255, 0.96), rgba(233, 242, 255, 0.94)); color: #173b63; display: flex; flex-direction: column; justify-content: center; gap: 4px; padding: 10px 12px; cursor: pointer; transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease; text-align: left; }
.captcha-card:hover:not(:disabled) { transform: translateY(-1px); border-color: rgba(61, 125, 255, 0.32); box-shadow: 0 12px 24px rgba(61, 125, 255, 0.14); }
.captcha-card:disabled { cursor: wait; opacity: 0.72; }
.captcha-label { font-size: 11px; font-weight: 700; color: #6080a8; text-transform: uppercase; letter-spacing: 0.08em; }
.captcha-card strong { font-size: 18px; line-height: 1.2; }
.captcha-card small { color: #6a86b0; font-size: 11px; }
.form-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; gap: 12px; }
.help-text { color: var(--text-secondary); font-size: 13px; }
:deep(.el-checkbox__label) { color: var(--text-secondary); }
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) { background-color: var(--brand-bright-blue); border-color: var(--brand-bright-blue); }
.login-button { height: 48px; font-size: 16px; letter-spacing: 1px; border: none; border-radius: 999px; background: linear-gradient(135deg, #3d7dff 0%, #64b8ff 100%); box-shadow: 0 12px 24px rgba(61, 125, 255, 0.3); }
.login-footer { text-align: center; margin-top: 24px; color: var(--text-secondary); }
.no-account { margin-right: 8px; }
.register-link { font-weight: 600; color: #3d7dff; text-decoration: underline; text-underline-offset: 2px; }

@media (min-width: 769px) {
  .auth-container {
    padding: 36px 24px;
  }

  .auth-shell {
    max-width: 440px;
  }

  .auth-card {
    padding: 32px 32px 28px;
    border-radius: 20px;
    backdrop-filter: blur(10px) !important;
    box-shadow: 0 16px 36px rgba(16, 38, 63, 0.12) !important;
  }

  .brand-row {
    margin-bottom: 10px;
  }

  .presence-panel {
    padding: 12px;
    border-radius: 16px;
    box-shadow: none;
  }

  .presence-stats {
    gap: 8px;
  }

  .presence-stat {
    padding: 8px 6px;
  }

  .presence-stat strong {
    font-size: 20px;
  }

  .presence-list {
    margin-top: 10px;
  }

  .logo-icon-wrapper {
    width: 54px;
    height: 54px;
    margin-bottom: 14px;
  }

  .auth-title {
    font-size: 28px;
  }

  .auth-subtitle {
    font-size: 13px;
    line-height: 1.6;
  }

  .form-actions {
    margin-bottom: 20px;
  }
}

@media (max-width: 768px) {
  .auth-container { align-items: stretch; padding: 14px; }
  .auth-shell { max-width: none; display: flex; align-items: center; }
  .auth-card { padding: 28px 18px; border-radius: 20px; margin: auto 0; }
  .brand-row { flex-direction: column; align-items: stretch; }
  .presence-pill { justify-content: center; }
  .presence-panel { padding: 12px; }
  .presence-list { margin-top: 10px; }
  .auth-title { font-size: 26px; }
  .auth-subtitle { font-size: 13px; }
  .captcha-row { grid-template-columns: 1fr; }
  .captcha-card { min-height: 84px; text-align: center; align-items: center; }
  .form-actions { flex-direction: column; align-items: stretch; }
  .help-text { text-align: center; }
}

@media (max-width: 480px) {
  .auth-container { padding: 10px; }
  .auth-card { padding: 24px 14px; }
  .presence-stats { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .presence-stat { padding: 10px 6px; }
  .presence-stat strong { font-size: 20px; }
  .logo-icon-wrapper { width: 52px; height: 52px; }
  .auth-title { font-size: 24px; }
}

@media (max-width: 390px) {
  .presence-panel { padding: 10px; }
  .presence-stats { gap: 8px; }
  .presence-user-chip { font-size: 11px; }
  .auth-subtitle { font-size: 12px; }
}
</style>
