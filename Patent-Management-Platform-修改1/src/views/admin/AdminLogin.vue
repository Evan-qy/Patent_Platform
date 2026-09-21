<template>
  <div class="admin-login-page">
    <el-card class="admin-login-card">
      <template #header>
        <strong>后台登录</strong>
      </template>

      <el-form :model="form" label-position="top" @submit.prevent="login">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入后台账号" />
        </el-form-item>

        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>

        <el-button type="primary" :loading="loading" style="width: 100%" @click="login">登录后台</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import { getApiErrorMessage } from '@/lib/api-errors'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const login = async () => {
  loading.value = true
  try {
    const response = await adminApi.login(form)
    localStorage.setItem('adminToken', response.token)
    localStorage.setItem('adminProfile', JSON.stringify(response.admin))
    ElMessage.success('后台登录成功')
    router.push((route.query.redirect as string) || '/admin/home')
  } catch (error: any) {
    ElMessage.error(getApiErrorMessage(error, '后台登录失败'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eff6ff, #f7fbff);
}

.admin-login-card {
  width: min(420px, 92vw);
}
</style>
