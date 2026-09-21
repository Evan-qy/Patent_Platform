<template>
  <div class="auth-container">
    <div class="bg-shape shape-1"></div>
    <div class="bg-shape shape-2"></div>

    <div class="register-layout">
      <section class="register-intro panel-glow" :class="{ collapsed: collapseIntro }">
        <div class="intro-badge">{{ PLATFORM_NAME }}</div>
        <h1>创建账号，立即开始使用</h1>
        <p>{{ collapseIntro ? '注册后即可使用专利检索、需求发布和评估分析。' : '注册后即可使用专利检索、需求发布、评估分析和成果转化等核心功能。' }}</p>
        <el-button v-if="collapseIntro" text type="primary" class="intro-toggle" @click="showFullIntro = !showFullIntro">
          {{ showFullIntro ? '收起说明' : '展开说明' }}
        </el-button>
        <div v-show="!collapseIntro || showFullIntro" class="intro-points">
          <div class="intro-point">
            <strong>手机优先</strong>
            <span>表单、按钮和错误提示都按小屏重新排布，减少输入负担。</span>
          </div>
          <div class="intro-point">
            <strong>快速完成</strong>
            <span>只保留关键字段，让注册流程在手机上更短、更清晰。</span>
          </div>
          <div class="intro-point">
            <strong>安全校验</strong>
            <span>密码强度和两次输入一致性会实时提醒，不需要来回提交试错。</span>
          </div>
        </div>
      </section>

      <section class="auth-card glass-card">
        <div class="auth-header">
          <div class="logo-icon-wrapper">
            <el-icon class="logo-icon"><User /></el-icon>
          </div>
          <h2 class="auth-title">创建您的平台账号</h2>
          <p class="auth-subtitle">填写必要信息后即可进入平台继续操作。</p>
        </div>

        <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top" class="register-form" @submit.prevent="handleRegister">
          <el-form-item label="用户名" prop="username" class="glass-form-item">
            <el-input
              v-model="registerForm.username"
              placeholder="3-20 位，支持字母、数字、下划线"
              size="large"
              class="glass-input"
              @keyup.enter="handleRegister"
            >
              <template #prefix>
                <el-icon class="input-icon"><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password" class="glass-form-item">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="6-20 位，需包含字母和数字"
              show-password
              size="large"
              class="glass-input"
              @keyup.enter="handleRegister"
            >
              <template #prefix>
                <el-icon class="input-icon"><Lock /></el-icon>
              </template>
            </el-input>
            <div v-if="registerForm.password" class="password-strength">
              <div class="strength-bar-bg">
                <div class="strength-bar" :style="{ width: `${strengthWidth}%`, background: strengthColor }"></div>
              </div>
              <span class="strength-text" :style="{ color: strengthColor }">{{ strengthText }}</span>
            </div>
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword" class="glass-form-item">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              show-password
              size="large"
              class="glass-input"
              @keyup.enter="handleRegister"
            >
              <template #prefix>
                <el-icon class="input-icon"><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="手机号" prop="phone" class="glass-form-item">
            <el-input v-model="registerForm.phone" placeholder="手机号可选填" size="large" class="glass-input">
              <template #prefix>
                <el-icon class="input-icon"><Iphone /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading" @click="handleRegister" size="large" class="register-button apple-btn">
              立即注册
            </el-button>
          </el-form-item>

          <div class="register-footer">
            <span class="has-account">已有账号？</span>
            <el-link type="primary" @click="goToLogin" class="login-link">立即登录</el-link>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Iphone } from '@element-plus/icons-vue'
import { authApi } from '@/api'
import type { RegisterParams } from '@/types/auth'
import type { FormInstance, FormRules } from 'element-plus'
import { PLATFORM_NAME } from '@/constants/brand'

const router = useRouter()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const strengthWidth = ref(0)
const strengthText = ref('')
const showFullIntro = ref(false)
const collapseIntro = ref(window.innerWidth <= 390)
const handleResize = () => {
  collapseIntro.value = window.innerWidth <= 390
  if (!collapseIntro.value) {
    showFullIntro.value = true
  }
}

const strengthColor = computed(() => {
  if (strengthWidth.value < 40) return '#ff6b6b'
  if (strengthWidth.value < 80) return '#ff9f43'
  return '#12bf7f'
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: '',
  nickname: ''
})

const checkPasswordStrength = (password: string) => {
  let score = 0
  if (password.length >= 8) score += 25
  else if (password.length >= 6) score += 10
  if (/[a-zA-Z]/.test(password)) score += 25
  if (/\d/.test(password)) score += 25
  if (/[^a-zA-Z0-9]/.test(password)) score += 25

  strengthWidth.value = Math.min(score, 100)
  if (score < 50) strengthText.value = '较弱'
  else if (score < 80) strengthText.value = '中等'
  else strengthText.value = '较强'
}

watch(() => registerForm.password, (value) => {
  if (value) checkPasswordStrength(value)
  else {
    strengthWidth.value = 0
    strengthText.value = ''
  }
})

const validateUsername = (_rule: any, value: string, callback: any) => {
  const reg = /^[a-zA-Z0-9_]{3,20}$/
  if (value && !reg.test(value)) {
    callback(new Error('用户名仅支持字母、数字和下划线，长度 3 到 20 位'))
    return
  }
  callback()
}

const validatePasswordStrength = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback()
    return
  }
  const hasLetter = /[a-zA-Z]/.test(value)
  const hasNumber = /\d/.test(value)
  if (!hasLetter || !hasNumber) {
    callback(new Error('密码需同时包含字母和数字'))
    return
  }
  callback()
}

const registerRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { validator: validateUsername, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 到 20 个字符之间', trigger: 'blur' },
    { validator: validatePasswordStrength, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' },
    { required: false }
  ]
})

const handleRegister = async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      loading.value = true
      const registerData: RegisterParams = {
        username: registerForm.username,
        password: registerForm.password,
        phone: registerForm.phone || undefined,
        email: registerForm.email || undefined,
        nickname: registerForm.nickname || undefined
      }

      await authApi.register(registerData)
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error: any) {
      ElMessage.error(error?.message || '注册失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

const goToLogin = () => {
  router.push('/login')
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.auth-container { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--auth-shell-bg); padding: 20px; position: relative; overflow: hidden; }
.bg-shape { position: absolute; border-radius: 50%; filter: blur(90px); z-index: 0; opacity: 0.32; }
.shape-1 { width: 420px; height: 420px; background: #007aff; top: -110px; left: -110px; animation: float 12s infinite ease-in-out; }
.shape-2 { width: 340px; height: 340px; background: #48c6ef; bottom: -60px; right: -40px; animation: float 16s infinite ease-in-out reverse; }
@keyframes float { 0% { transform: translate(0, 0); } 50% { transform: translate(36px, 54px); } 100% { transform: translate(0, 0); } }
.register-layout { width: min(100%, 1040px); display: grid; grid-template-columns: minmax(280px, 0.95fr) minmax(0, 1fr); gap: 20px; position: relative; z-index: 1; }
.register-intro, .auth-card { border-radius: 26px; }
.register-intro { padding: 34px 30px; background: linear-gradient(180deg, rgba(18, 59, 99, 0.94), rgba(17, 110, 159, 0.9)); color: #fff; box-shadow: 0 24px 56px rgba(10, 31, 57, 0.18); display: flex; flex-direction: column; gap: 18px; }
.intro-badge { display: inline-flex; width: fit-content; padding: 7px 12px; border-radius: 999px; background: rgba(255,255,255,.12); border: 1px solid rgba(255,255,255,.14); font-size: 12px; font-weight: 700; }
.register-intro h1 { margin: 0; font-size: clamp(28px, 4vw, 38px); line-height: 1.12; }
.register-intro p { margin: 0; color: rgba(238, 246, 255, 0.84); line-height: 1.75; }
.intro-toggle { width: fit-content; padding-left: 0; color: #bfe7ff; }
.intro-points { display: grid; gap: 12px; margin-top: auto; }
.intro-point { padding: 16px; border-radius: 18px; background: rgba(255,255,255,.1); border: 1px solid rgba(255,255,255,.12); }
.intro-point strong { display: block; margin-bottom: 6px; font-size: 15px; }
.intro-point span { color: rgba(239, 247, 255, 0.8); font-size: 13px; line-height: 1.6; }
.auth-card { width: 100%; padding: 34px 30px; background: var(--auth-card-bg) !important; backdrop-filter: blur(24px) !important; border: 1px solid var(--auth-card-border) !important; box-shadow: var(--auth-card-shadow) !important; animation: slideUp 0.6s cubic-bezier(0.25, 0.8, 0.25, 1); }
@keyframes slideUp { from { opacity: 0; transform: translateY(40px); } to { opacity: 1; transform: translateY(0); } }
.auth-header { text-align: center; margin-bottom: 28px; }
.logo-icon-wrapper { width: 60px; height: 60px; background: linear-gradient(135deg, #007aff 0%, #1fb6ff 100%); border-radius: 16px; display: flex; align-items: center; justify-content: center; margin: 0 auto 18px; box-shadow: 0 10px 24px rgba(0, 122, 255, 0.24); }
.logo-icon { font-size: 30px; color: white; }
.auth-title { font-size: 28px; font-weight: 700; color: var(--text-primary); margin: 0 0 8px; }
.auth-subtitle { color: var(--text-secondary); font-size: 14px; margin: 0; }
.register-form { width: 100%; }
.glass-form-item { margin-bottom: 22px; }
:deep(.el-form-item__label) { color: var(--text-secondary); font-weight: 600; margin-bottom: 6px; }
:deep(.el-input__wrapper) { background: rgba(255, 255, 255, 0.88) !important; box-shadow: 0 0 0 1px rgba(61, 125, 255, 0.16) inset !important; border-radius: 12px !important; min-height: 48px; padding-left: 12px; transition: all 0.3s ease; }
:deep(.el-input__wrapper:hover), :deep(.el-input__wrapper.is-focus) { background: rgba(255, 255, 255, 1) !important; box-shadow: 0 0 0 2px rgba(61, 125, 255, 0.3) inset !important; }
.input-icon { font-size: 18px; color: #5f7896; }
.password-strength { margin-top: 10px; display: flex; align-items: center; gap: 10px; }
.strength-bar-bg { flex: 1; height: 6px; background: rgba(30, 51, 73, 0.1); border-radius: 999px; overflow: hidden; }
.strength-bar { height: 100%; border-radius: 999px; transition: width .25s ease, background .25s ease; }
.strength-text { min-width: 36px; font-size: 12px; font-weight: 700; }
.register-button { width: 100%; height: 50px; font-size: 16px; letter-spacing: .5px; background: linear-gradient(135deg, #007aff 0%, #3bb0ff 100%); border: none; border-radius: 999px; }
.register-footer { text-align: center; margin-top: 22px; color: var(--text-secondary); }
.has-account { margin-right: 8px; }
.login-link { font-weight: 600; color: #2376db; }

@media (min-width: 769px) {
  .register-layout {
    width: min(100%, 960px);
    grid-template-columns: minmax(300px, 0.82fr) minmax(0, 1fr);
    gap: 18px;
  }

  .register-intro {
    padding: 28px 24px;
    gap: 16px;
    border-radius: 22px;
    box-shadow: 0 18px 38px rgba(10, 31, 57, 0.14);
  }

  .register-intro h1 {
    font-size: clamp(24px, 2.6vw, 32px);
  }

  .intro-point {
    padding: 14px;
    border-radius: 16px;
  }

  .auth-card {
    padding: 30px 28px;
    border-radius: 22px;
    backdrop-filter: blur(12px) !important;
    box-shadow: 0 16px 34px rgba(16, 38, 63, 0.12) !important;
  }

  .auth-header {
    margin-bottom: 24px;
  }

  .logo-icon-wrapper {
    width: 54px;
    height: 54px;
    margin-bottom: 14px;
  }

  .auth-title {
    font-size: 26px;
  }

  .auth-subtitle {
    font-size: 13px;
  }

  .glass-form-item {
    margin-bottom: 18px;
  }
}

@media (max-width: 768px) {
  .auth-container { padding: 14px; align-items: stretch; }
  .register-layout { grid-template-columns: 1fr; gap: 14px; }
  .register-intro, .auth-card { padding: 24px 18px; border-radius: 22px; }
  .register-intro { gap: 14px; }
  .intro-points { gap: 10px; }
}

@media (max-width: 480px) {
  .auth-container { padding: 10px; }
  .register-intro { padding: 18px 14px; }
  .auth-card { padding: 22px 14px; }
  .register-intro h1 { font-size: 24px; }
  .auth-title { font-size: 24px; }
  .logo-icon-wrapper { width: 52px; height: 52px; margin-bottom: 14px; }
  .password-strength { align-items: flex-start; flex-direction: column; gap: 8px; }
  .strength-bar-bg { width: 100%; }
}

@media (max-width: 390px) {
  .register-intro.collapsed {
    gap: 10px;
  }
}
</style>
