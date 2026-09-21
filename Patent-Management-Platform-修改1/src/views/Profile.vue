<template>
  <div class="page-container profile-page">
    <div class="profile-background profile-background-left"></div>
    <div class="profile-background profile-background-right"></div>

    <div class="page-content profile-content">
      <aside class="panel-glow profile-sidebar">
        <div class="profile-avatar-block">
          <el-upload
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :http-request="handleAvatarUpload"
            :before-upload="beforeAvatarUpload"
            :disabled="!isEditing || avatarUploading"
            accept="image/png,image/jpeg,image/webp,image/gif"
          >
            <div class="avatar-frame" :class="{ editable: isEditing }">
              <img v-if="currentAvatarUrl" :src="currentAvatarUrl" class="avatar-image" alt="avatar" />
              <div v-else class="avatar-placeholder">{{ userInitial }}</div>
              <div class="avatar-overlay">
                <el-icon v-if="avatarUploading" class="is-loading"><Loading /></el-icon>
                <template v-else>
                  <el-icon><Camera /></el-icon>
                  <span>{{ isEditing ? '上传头像' : '查看头像' }}</span>
                </template>
              </div>
            </div>
          </el-upload>
          <p class="avatar-tip">支持 PNG、JPG、WEBP、GIF，单张不超过 5MB</p>
        </div>

        <div class="profile-identity">
          <span class="identity-tag">个人资料</span>
          <h2>{{ displayName }}</h2>
          <p>@{{ userInfo?.user?.username || 'unknown' }}</p>
          <div class="identity-badge">
            <span class="status-dot"></span>
            普通用户
          </div>
        </div>

        <div class="profile-stats">
          <div class="stat-card">
            <strong>{{ userInfo?.profile?.realName ? '已实名' : '未实名' }}</strong>
            <span>认证状态</span>
          </div>
          <div class="stat-card">
            <strong>{{ userInfo?.user?.phone ? '已绑定' : '未绑定' }}</strong>
            <span>手机绑定</span>
          </div>
          <div class="stat-card">
            <strong>{{ userInfo?.user?.email ? '已绑定' : '未绑定' }}</strong>
            <span>邮箱绑定</span>
          </div>
        </div>
      </aside>

      <section class="panel-glow profile-main">
        <div class="profile-main-header">
          <div>
            <span class="section-tag">资料管理</span>
            <h1>修改个人资料</h1>
            <p>统一管理头像、联系方式与实名信息，保存后立即同步到当前账号。</p>
          </div>
          <div class="header-actions">
            <el-button
              class="ghost-button"
              round
              @click="toggleEdit"
            >
              <el-icon><component :is="isEditing ? Close : Edit" /></el-icon>
              {{ isEditing ? '取消编辑' : '编辑资料' }}
            </el-button>
          </div>
        </div>

        <el-form
          ref="editFormRef"
          :model="editForm"
          :rules="editRules"
          label-position="top"
          class="profile-form"
          :disabled="!isEditing || loading"
        >
          <div class="form-grid">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="editForm.username" disabled class="glass-input">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="editForm.nickname" class="glass-input" placeholder="设置一个公开昵称">
                <template #prefix><el-icon><Postcard /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
              <el-input v-model="editForm.phone" class="glass-input" placeholder="绑定手机号">
                <template #prefix><el-icon><Iphone /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="电子邮箱" prop="email">
              <el-input v-model="editForm.email" class="glass-input" placeholder="绑定邮箱">
                <template #prefix><el-icon><Message /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="editForm.realName" class="glass-input" placeholder="用于实名信息留档">
                <template #prefix><el-icon><CollectionTag /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="身份证号" prop="idNumber">
              <el-input v-model="editForm.idNumber" class="glass-input" placeholder="请输入实名证件号">
                <template #prefix><el-icon><CreditCard /></el-icon></template>
              </el-input>
            </el-form-item>
          </div>

          <div class="profile-footer">
            <div class="save-tip">
              <el-icon><Upload /></el-icon>
              <span>头像上传成功后会自动回填，点击“保存修改”后正式生效。</span>
            </div>
            <div v-if="isEditing" class="footer-actions">
              <el-button class="ghost-button" @click="resetForm">重置修改</el-button>
              <el-button type="primary" class="primary-button" :loading="loading" @click="submitForm">
                保存修改
              </el-button>
            </div>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadRawFile, UploadRequestOptions } from 'element-plus'
import { profileApi } from '@/api'
import type { FullUserInfo, UpdateUserProfileParams } from '@/types'
import {
  Camera,
  Close,
  CollectionTag,
  CreditCard,
  Edit,
  Iphone,
  Loading,
  Message,
  Postcard,
  Upload,
  User
} from '@element-plus/icons-vue'

type EditableProfileForm = {
  username: string
  nickname: string
  avatarUrl: string | null
  realName: string
  idNumber: string
  phone: string
  email: string
}

const userInfo = ref<FullUserInfo | null>(null)
const editFormRef = ref<FormInstance>()
const loading = ref(false)
const avatarUploading = ref(false)
const isEditing = ref(false)

const editForm = reactive<EditableProfileForm>({
  username: '',
  nickname: '',
  avatarUrl: null,
  realName: '',
  idNumber: '',
  phone: '',
  email: ''
})

const editRules: FormRules<EditableProfileForm> = {
  nickname: [{ min: 0, max: 64, message: '昵称长度不能超过 64 个字符', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  email: [{ pattern: /^$|^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '请输入正确的邮箱格式', trigger: 'blur' }],
  realName: [{ min: 0, max: 64, message: '真实姓名长度不能超过 64 个字符', trigger: 'blur' }],
  idNumber: [{ pattern: /^$|(^\d{15}$)|(^\d{17}(\d|X|x)$)$/, message: '请输入正确的身份证号', trigger: 'blur' }]
}

const displayName = computed(() => editForm.nickname || userInfo.value?.profile?.nickname || userInfo.value?.user?.username || '未登录用户')
const currentAvatarUrl = computed(() => editForm.avatarUrl || userInfo.value?.profile?.avatarUrl || null)
const userInitial = computed(() => {
  const source = displayName.value.trim()
  return source ? source.charAt(0).toUpperCase() : 'U'
})

const normalizeNullable = (value: string | null | undefined) => {
  if (value == null) {
    return null
  }
  const normalized = String(value).trim()
  return normalized === '' ? null : normalized
}

const getProfileErrorMessage = (error: any, fallback: string) =>
  error?.response?.data?.message
  || error?.message
  || fallback

const syncForm = (data: FullUserInfo | null) => {
  editForm.username = data?.user?.username || ''
  editForm.nickname = data?.profile?.nickname || ''
  editForm.avatarUrl = data?.profile?.avatarUrl || null
  editForm.realName = data?.profile?.realName || ''
  editForm.idNumber = data?.profile?.idNumber || ''
  editForm.phone = data?.user?.phone || ''
  editForm.email = data?.user?.email || ''
}

const loadUserInfo = async () => {
  loading.value = true
  try {
    const data = await profileApi.getCurrentUserInfo()
    userInfo.value = data
    syncForm(data)
  } catch (error: any) {
    ElMessage.error(getProfileErrorMessage(error, '获取个人资料失败'))
  } finally {
    loading.value = false
  }
}

const toggleEdit = () => {
  if (isEditing.value) {
    syncForm(userInfo.value)
    isEditing.value = false
    return
  }
  isEditing.value = true
}

const resetForm = () => {
  syncForm(userInfo.value)
}

const beforeAvatarUpload = (file: UploadRawFile) => {
  const isAllowedType = ['image/png', 'image/jpeg', 'image/webp', 'image/gif'].includes(file.type)
  const isAllowedSize = file.size / 1024 / 1024 <= 5

  if (!isAllowedType) {
    ElMessage.error('仅支持 PNG、JPG、WEBP、GIF 图片')
    return false
  }
  if (!isAllowedSize) {
    ElMessage.error('头像文件不能超过 5MB')
    return false
  }
  return true
}

const handleAvatarUpload = async (options: UploadRequestOptions) => {
  if (!isEditing.value) {
    ElMessage.warning('请先进入编辑状态再上传头像')
    options.onError?.(new Error('not editing') as any)
    return
  }

  avatarUploading.value = true
  try {
    const file = options.file as File
    const response = await profileApi.uploadAvatar(file)
    editForm.avatarUrl = response.url
    ElMessage.success('头像上传成功')
    options.onSuccess?.(response as any)
  } catch (error: any) {
    ElMessage.error(getProfileErrorMessage(error, '头像上传失败，请稍后重试'))
    options.onError?.(error)
  } finally {
    avatarUploading.value = false
  }
}

const submitForm = async () => {
  if (!editFormRef.value) return

  try {
    await editFormRef.value.validate()

    const payload = {
      nickname: normalizeNullable(editForm.nickname),
      avatarUrl: editForm.avatarUrl ?? undefined,
      realName: normalizeNullable(editForm.realName),
      idNumber: normalizeNullable(editForm.idNumber),
      phone: normalizeNullable(editForm.phone),
      email: normalizeNullable(editForm.email)
    }

    loading.value = true
    await profileApi.updateUserProfile(payload as UpdateUserProfileParams)
    ElMessage.success('资料更新成功')
    isEditing.value = false
    await loadUserInfo()
  } catch (error: any) {
    if (error?.message !== 'error fields') {
      ElMessage.error(getProfileErrorMessage(error, '保存资料失败，请稍后重试'))
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadUserInfo()
})
</script>

<style scoped>
.profile-page {
  --profile-accent: #2f7bf6;
  --profile-panel-bg: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(243, 248, 255, 0.9));
  --profile-panel-border: rgba(61, 125, 255, 0.14);
  --profile-panel-shadow: 0 24px 48px rgba(67, 94, 153, 0.14);
  --profile-avatar-frame-bg: rgba(255, 255, 255, 0.76);
  --profile-avatar-frame-border: rgba(61, 125, 255, 0.16);
  --profile-avatar-overlay: linear-gradient(180deg, rgba(255, 255, 255, 0), rgba(15, 45, 83, 0.72));
  --profile-avatar-text: #f4f8ff;
  --profile-tip: #6c7f93;
  --profile-tag-bg: rgba(61, 125, 255, 0.1);
  --profile-tag-border: rgba(61, 125, 255, 0.18);
  --profile-tag-text: #2b528a;
  --profile-heading-bg: linear-gradient(180deg, rgba(233, 242, 255, 0.96), rgba(224, 236, 252, 0.88));
  --profile-heading-text: #12355c;
  --profile-heading-shadow: 0 10px 24px rgba(67, 94, 153, 0.12);
  --profile-subline-bg: rgba(233, 242, 255, 0.92);
  --profile-subline-text: #34587f;
  --profile-input-bg: rgba(255, 255, 255, 0.9);
  --profile-input-border: rgba(61, 125, 255, 0.18);
  --profile-disabled-bg: rgba(237, 244, 255, 0.92);
  --profile-badge-bg: rgba(61, 125, 255, 0.12);
  --profile-badge-border: rgba(61, 125, 255, 0.16);
  --profile-badge-text: #183a62;
  --profile-stat-bg: rgba(255, 255, 255, 0.74);
  --profile-stat-border: rgba(61, 125, 255, 0.12);
  --profile-divider: rgba(61, 125, 255, 0.12);
  --profile-save-tip: #5f7898;
  position: relative;
  overflow: hidden;
}

.profile-content {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
  gap: 24px;
  padding-top: 40px;
  padding-bottom: 48px;
}

.profile-background {
  position: absolute;
  border-radius: 999px;
  filter: blur(80px);
  opacity: 0.34;
  pointer-events: none;
}

.profile-background-left {
  top: 60px;
  left: -120px;
  width: 320px;
  height: 320px;
  background: rgba(47, 107, 255, 0.45);
}

.profile-background-right {
  right: -120px;
  bottom: 40px;
  width: 340px;
  height: 340px;
  background: rgba(34, 199, 222, 0.4);
}

.profile-sidebar,
.profile-main {
  padding: 28px;
}

.profile-sidebar {
  display: flex;
  flex-direction: column;
  gap: 24px;
  background: var(--profile-panel-bg) !important;
  border: 1px solid var(--profile-panel-border) !important;
  box-shadow: var(--profile-panel-shadow);
}

.profile-avatar-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.avatar-uploader {
  width: 100%;
  display: flex;
  justify-content: center;
}

.avatar-frame {
  position: relative;
  width: 164px;
  height: 164px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid var(--profile-avatar-frame-border);
  box-shadow: 0 18px 36px rgba(67, 94, 153, 0.16);
  background: var(--profile-avatar-frame-bg);
}

.avatar-frame.editable {
  cursor: pointer;
}

.avatar-image,
.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-image {
  object-fit: cover;
}

.avatar-placeholder {
  background: linear-gradient(145deg, rgba(62, 117, 255, 0.95), rgba(31, 184, 207, 0.7));
  color: #fff;
  font-size: 62px;
  font-weight: 700;
}

.avatar-overlay {
  position: absolute;
  inset: auto 0 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--profile-avatar-overlay);
  color: var(--profile-avatar-text);
  font-size: 13px;
}

.avatar-tip {
  margin: 0;
  color: var(--profile-tip);
  font-size: 13px;
  text-align: center;
}

.profile-identity {
  text-align: center;
}

.section-tag,
.identity-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 7px 14px;
  background: var(--profile-tag-bg);
  border: 1px solid var(--profile-tag-border);
  color: var(--profile-tag-text);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.profile-identity h2,
.profile-main-header h1 {
  margin: 14px 0 8px;
  display: inline-block;
  padding: 6px 14px;
  border-radius: 14px;
  background: var(--profile-heading-bg);
  color: var(--profile-heading-text);
  font-weight: 800;
  letter-spacing: 0.02em;
  box-shadow: var(--profile-heading-shadow);
  text-shadow: none;
}

.profile-identity p,
.profile-main-header p {
  margin: 0;
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--profile-subline-bg);
  color: var(--profile-subline-text);
  font-weight: 600;
  line-height: 1.7;
}

.profile-form :deep(.el-input__wrapper),
.profile-form :deep(.el-textarea__inner) {
  background: var(--profile-input-bg) !important;
  box-shadow: 0 0 0 1px var(--profile-input-border) inset !important;
  color: var(--text-primary) !important;
}

.profile-form :deep(.el-input__inner),
.profile-form :deep(.el-textarea__inner) {
  color: var(--text-primary) !important;
}

.profile-form :deep(.el-input__inner::placeholder),
.profile-form :deep(.el-textarea__inner::placeholder) {
  color: var(--profile-tip) !important;
}

.profile-form :deep(.el-input.is-disabled .el-input__wrapper) {
  background: var(--profile-disabled-bg) !important;
  box-shadow: 0 0 0 1px var(--profile-input-border) inset !important;
  cursor: not-allowed;
}

.profile-form :deep(.el-input.is-disabled .el-input__inner) {
  color: var(--text-primary) !important;
  -webkit-text-fill-color: var(--text-primary) !important;
  opacity: 1 !important;
  font-weight: 700;
}

.identity-badge {
  width: fit-content;
  margin: 16px auto 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  padding: 8px 14px;
  background: var(--profile-badge-bg);
  color: var(--profile-badge-text);
  border: 1px solid var(--profile-badge-border);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #35d49b;
  box-shadow: 0 0 10px rgba(53, 212, 155, 0.8);
}

.profile-stats {
  display: grid;
  gap: 14px;
}

.stat-card {
  border-radius: 20px;
  padding: 18px 20px;
  background: var(--profile-stat-bg);
  border: 1px solid var(--profile-stat-border);
}

.stat-card strong {
  display: block;
  color: var(--text-primary);
  font-size: 18px;
}

.stat-card span {
  display: block;
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 13px;
}

.profile-main {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.profile-main-header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 20px;
}

.profile-footer {
  margin-top: 14px;
  padding-top: 24px;
  border-top: 1px solid var(--profile-divider);
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
}

.save-tip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--profile-save-tip);
  font-size: 14px;
}

.footer-actions {
  display: flex;
  gap: 12px;
}

.ghost-button,
.primary-button {
  min-width: 120px;
  border-radius: 999px;
}

[data-theme="dark"] .profile-page {
  --profile-panel-bg: linear-gradient(180deg, rgba(13, 33, 61, 0.94), rgba(10, 28, 54, 0.9));
  --profile-panel-border: rgba(120, 183, 255, 0.18);
  --profile-panel-shadow: 0 24px 48px rgba(7, 18, 38, 0.28);
  --profile-avatar-frame-bg: rgba(255, 255, 255, 0.1);
  --profile-avatar-frame-border: rgba(255, 255, 255, 0.24);
  --profile-avatar-overlay: linear-gradient(180deg, rgba(7, 16, 34, 0), rgba(7, 16, 34, 0.9));
  --profile-avatar-text: #f4f8ff;
  --profile-tip: rgba(229, 240, 255, 0.8);
  --profile-tag-bg: rgba(115, 183, 255, 0.12);
  --profile-tag-border: rgba(115, 183, 255, 0.2);
  --profile-tag-text: #d8edff;
  --profile-heading-bg: linear-gradient(180deg, rgba(6, 20, 44, 0.24), rgba(6, 20, 44, 0.14));
  --profile-heading-text: #fdfefe;
  --profile-heading-shadow: 0 10px 24px rgba(8, 22, 52, 0.18);
  --profile-subline-bg: rgba(7, 24, 51, 0.16);
  --profile-subline-text: rgba(247, 251, 255, 0.98);
  --profile-input-bg: rgba(255, 255, 255, 0.12);
  --profile-input-border: rgba(140, 196, 255, 0.18);
  --profile-disabled-bg: rgba(255, 255, 255, 0.16);
  --profile-badge-bg: rgba(78, 150, 255, 0.18);
  --profile-badge-border: rgba(120, 183, 255, 0.18);
  --profile-badge-text: #eef7ff;
  --profile-stat-bg: rgba(255, 255, 255, 0.1);
  --profile-stat-border: rgba(120, 183, 255, 0.16);
  --profile-divider: rgba(255, 255, 255, 0.1);
  --profile-save-tip: rgba(224, 238, 255, 0.74);
}

@media (max-width: 1100px) {
  .profile-content {
    grid-template-columns: 1fr;
  }

  .profile-main-header,
  .profile-footer {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 768px) {
  .profile-content {
    padding-top: 14px;
    gap: 12px;
  }

  .profile-sidebar,
  .profile-main {
    padding: 18px;
    border-radius: 24px;
  }

  .avatar-frame {
    width: 124px;
    height: 124px;
  }

  .avatar-placeholder {
    font-size: 46px;
  }

  .profile-identity h2 {
    font-size: 28px;
  }

  .profile-main-header h1 {
    font-size: 24px;
    margin-top: 10px;
  }

  .profile-main-header p {
    font-size: 13px;
  }

  .profile-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .stat-card {
    padding: 14px 12px;
    text-align: center;
    min-height: 84px;
  }

  .stat-card strong {
    font-size: 16px;
  }

  .stat-card span {
    font-size: 12px;
  }

  .header-actions,
  .save-tip,
  .profile-footer {
    width: 100%;
  }

  .save-tip {
    align-items: flex-start;
    font-size: 13px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .profile-main-header,
  .profile-footer {
    gap: 14px;
  }

  .footer-actions {
    width: 100%;
    flex-direction: column;
  }

  .ghost-button,
  .primary-button {
    width: 100%;
  }
}
@media (max-width: 480px) {
  .profile-content {
    padding-top: 12px;
  }

  .profile-sidebar,
  .profile-main {
    padding: 16px 14px;
  }

  .profile-stats {
    grid-template-columns: 1fr;
  }

  .profile-main-header h1 {
    font-size: 22px;
  }

  .profile-identity h2 {
    font-size: 24px;
  }
}
</style>

