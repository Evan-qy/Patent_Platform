<template>
  <div class="page-container demand-publish-page">
    <div class="glass-header">
      <h2 class="glass-page-title text-gradient-cyan">发布需求</h2>
    </div>

    <div class="page-content">
      <div class="panel-glow publish-panel">
        <div class="panel-header">
          <h3 class="panel-title">填写需求信息</h3>
          <p class="panel-subtitle">请按标准技术方向描述需求，方便平台完成专利与专家的精准匹配。</p>
          <div class="required-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>* 为必填项</span>
          </div>
        </div>

        <el-form
          ref="publishFormRef"
          :model="publishForm"
          :rules="rules"
          label-position="top"
          class="publish-form"
          status-icon
        >
          <div class="form-section-card">
            <div class="section-title">
              <el-icon><EditPen /></el-icon>
              <span>基础信息</span>
            </div>
            <el-form-item label="需求标题 *" prop="title">
              <el-input
                v-model="publishForm.title"
                placeholder="例如：高性能锂电池正极材料研发"
                class="glass-input"
              >
                <template #prefix>
                  <el-icon><CollectionTag /></el-icon>
                </template>
              </el-input>
              <p class="field-help">建议包含技术对象、目标指标或应用场景</p>
            </el-form-item>

            <el-form-item label="技术方向 *" prop="field">
              <el-select
                v-model="publishForm.field"
                placeholder="请选择九大信息产业方向"
                style="width: 100%"
                popper-class="glass-dropdown"
              >
                <template #prefix>
                  <el-icon><Grid /></el-icon>
                </template>
                <el-option
                  v-for="item in TECH_DIRECTION_OPTIONS"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
              <p class="field-help">技术方向按九大信息产业分类，用于统一筛选与匹配。</p>
            </el-form-item>
          </div>

          <div class="form-section-card split-grid">
            <el-form-item prop="budget">
              <template #label>
                <div class="custom-label">
                  <span>预算范围 *</span>
                  <el-tooltip content="预算单位为人民币（元）" placement="top">
                    <el-icon><InfoFilled /></el-icon>
                  </el-tooltip>
                </div>
              </template>
              <el-input-number
                v-model="publishForm.budget"
                :min="0"
                :step="1000"
                style="width: 100%"
                placeholder="请输入预算金额（元）"
                class="glass-input-number"
              />
            </el-form-item>

            <el-form-item label="截止日期 *" prop="deadline">
              <el-date-picker
                v-model="publishForm.deadline"
                type="date"
                placeholder="请选择截止日期"
                style="width: 100%"
                :disabled-date="disabledDate"
                popper-class="glass-dropdown"
              >
                <template #prefix>
                  <el-icon><Calendar /></el-icon>
                </template>
              </el-date-picker>
            </el-form-item>
          </div>

          <div class="form-section-card">
            <div class="section-title">
              <el-icon><Document /></el-icon>
              <span>需求说明</span>
            </div>
            <el-form-item label="需求详情 *" prop="description">
              <el-input
                v-model="publishForm.description"
                type="textarea"
                :rows="7"
                placeholder="请描述：1) 背景与痛点 2) 关键技术指标 3) 期望交付结果 4) 应用场景"
                class="glass-input"
              />
              <p class="field-help">描述越具体，匹配的专家与成果会越精准。</p>
            </el-form-item>
          </div>

          <div class="form-section-card split-grid">
            <el-form-item label="联系人 *" prop="contactName">
              <el-input
                v-model="publishForm.contactName"
                placeholder="请输入联系人姓名"
                class="glass-input"
              >
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="联系电话 *" prop="contactPhone">
              <el-input
                v-model="publishForm.contactPhone"
                placeholder="请输入联系电话"
                class="glass-input"
              >
                <template #prefix>
                  <el-icon><Phone /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </div>

          <el-form-item class="action-row">
            <el-button
              type="primary"
              size="large"
              @click="submitForm(publishFormRef)"
              :loading="loading"
              class="btn-primary publish-btn"
            >
              立即发布需求
            </el-button>
            <el-button text @click="resetForm(publishFormRef)" class="reset-link">重新填写</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { InfoFilled, EditPen, CollectionTag, Grid, Document, User, Phone, Calendar } from '@element-plus/icons-vue'
import { requirementApi } from '@/api'
import { TECH_DIRECTION_OPTIONS } from '@/constants/domain'
import type { CreateRequirementParams } from '@/types'

const router = useRouter()
const publishFormRef = ref<FormInstance>()
const loading = ref(false)

const publishForm = reactive({
  title: '',
  field: '',
  budget: undefined as number | undefined,
  deadline: '',
  description: '',
  contactName: '',
  contactPhone: ''
})

const hasFormContent = computed(() => {
  return Boolean(
    publishForm.title ||
    publishForm.field ||
    publishForm.description ||
    publishForm.contactName ||
    publishForm.contactPhone ||
    publishForm.deadline ||
    publishForm.budget
  )
})

const rules = reactive<FormRules>({
  title: [
    { required: true, message: '请输入需求标题', trigger: 'blur' },
    { min: 5, max: 100, message: '长度在 5 到 100 个字符', trigger: 'blur' }
  ],
  field: [{ required: true, message: '请选择技术方向', trigger: 'change' }],
  budget: [{ required: true, message: '请输入预算范围', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择截止日期', trigger: 'change' }],
  description: [{ required: true, message: '请输入需求详情', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
})

const disabledDate = (time: Date) => time.getTime() < Date.now()

const submitForm = async (formEl: FormInstance | undefined) => {
  if (!formEl) return

  await formEl.validate(async valid => {
    if (!valid) return

    loading.value = true
    try {
      const params: CreateRequirementParams = {
        title: publishForm.title.trim(),
        techDirection: publishForm.field,
        description: publishForm.description.trim(),
        cooperationMode: '技术开发',
        keywords: '',
        contactInfo: `${publishForm.contactName.trim()} ${publishForm.contactPhone.trim()}`,
        budget: publishForm.budget,
        deadline: publishForm.deadline
      }

      await requirementApi.createRequirement(params)
      ElMessage.success('需求发布成功')
      router.push('/requirement')
    } catch (error: any) {
      ElMessage.error(error.message || '发布失败，请重试')
    } finally {
      loading.value = false
    }
  })
}

const resetForm = (formEl: FormInstance | undefined) => {
  if (!formEl || !hasFormContent.value) return

  ElMessageBox.confirm('确认清空已填写内容吗？此操作不可撤销。', '清空确认', {
    confirmButtonText: '确认清空',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    formEl.resetFields()
    publishForm.budget = undefined
  }).catch(() => {})
}
</script>

<style scoped>
.demand-publish-page {
  --demand-page-bg:
    radial-gradient(circle at top left, rgba(69, 134, 255, 0.16), transparent 28%),
    radial-gradient(circle at top right, rgba(26, 202, 221, 0.12), transparent 24%),
    linear-gradient(180deg, rgba(245, 250, 255, 0.98), rgba(235, 244, 255, 0.96));
  --demand-panel-bg: linear-gradient(160deg, rgba(255, 255, 255, 0.92), rgba(244, 249, 255, 0.9));
  --demand-panel-border: rgba(61, 125, 255, 0.14);
  --demand-panel-shadow: 0 30px 60px rgba(67, 94, 153, 0.16), inset 0 1px 0 rgba(255, 255, 255, 0.8);
  --demand-title: #12355c;
  --demand-subtitle: #5b708c;
  --demand-section-bg: rgba(255, 255, 255, 0.72);
  --demand-section-border: rgba(61, 125, 255, 0.12);
  --demand-section-title: #12355c;
  --demand-help: #6c7f93;
  --demand-label: #34587f;
  --demand-input-bg: rgba(255, 255, 255, 0.9);
  --demand-input-border: rgba(61, 125, 255, 0.18);
  min-height: 100vh;
  background: var(--demand-page-bg);
}

.demand-publish-page .page-content {
  position: relative;
  padding-top: 28px;
  padding-bottom: 48px;
}

.publish-panel {
  padding: 36px;
  max-width: 900px;
  margin: 0 auto;
  border-radius: 20px;
  background: var(--demand-panel-bg) !important;
  border: 1px solid var(--demand-panel-border) !important;
  box-shadow: var(--demand-panel-shadow);
}

.panel-header {
  text-align: center;
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(61, 125, 255, 0.15);
}

.panel-title { font-size: 28px; font-weight: 700; color: var(--demand-title); margin-bottom: 10px; }

.panel-subtitle { color: var(--demand-subtitle); font-size: 14px; margin-bottom: 12px; }

.required-tip { display: inline-flex; align-items: center; gap: 6px; color: var(--demand-title); font-size: 13px; background: rgba(61, 125, 255, 0.1); border: 1px solid rgba(61, 125, 255, 0.16); padding: 6px 12px; border-radius: 999px; }

.publish-form {
  max-width: 700px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-section-card { background: var(--demand-section-bg); border: 1px solid var(--demand-section-border); border-radius: 14px; padding: 18px 18px 10px; backdrop-filter: blur(10px); box-shadow: 0 10px 24px rgba(67, 94, 153, 0.1); }

.section-title { display: flex; align-items: center; gap: 8px; color: var(--demand-section-title); font-size: 15px; font-weight: 700; margin-bottom: 10px; }

.split-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 18px;
}

.custom-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.field-help { margin: 6px 0 0; font-size: 12px; color: var(--demand-help); }

.action-row {
  margin-top: 8px;
}

.publish-btn {
  min-width: 180px;
}

.reset-link { margin-left: 14px; color: var(--demand-subtitle); }

.reset-link:hover {
  color: #ffd3d0;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-textarea__inner) {
  background-color: var(--demand-input-bg) !important;
  box-shadow: 0 0 0 1px var(--demand-input-border) inset !important;
  color: var(--text-primary) !important;
  border-radius: 10px;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover),
:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused),
:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px rgba(116, 196, 255, 0.4) inset !important;
}

:deep(.el-form-item__label) { color: var(--demand-label); font-weight: 600; margin-bottom: 6px !important; }

:deep(.glass-input-number .el-input-number__decrease),
:deep(.glass-input-number .el-input-number__increase) {
  background: rgba(61, 125, 255, 0.14);
  color: var(--text-primary);
  border-color: rgba(61, 125, 255, 0.18);
}

:deep(.el-input__inner),
:deep(.el-textarea__inner),
:deep(.el-select__selected-item),
:deep(.el-input-number .el-input__inner),
:deep(.el-date-editor input) {
  color: var(--text-primary) !important;
}

:deep(.el-input__inner::placeholder),
:deep(.el-textarea__inner::placeholder),
:deep(.el-select__placeholder),
:deep(.el-input-number .el-input__inner::placeholder) {
  color: var(--demand-help) !important;
}

:deep(.el-input-number),
:deep(.el-date-editor.el-input__wrapper) {
  width: 100%;
}

:deep(.el-input-number .el-input__wrapper),
:deep(.el-date-editor.el-input__wrapper) {
  background-color: var(--demand-input-bg) !important;
  box-shadow: 0 0 0 1px var(--demand-input-border) inset !important;
  color: var(--text-primary) !important;
}
[data-theme="dark"] .demand-publish-page {
  --demand-page-bg:
    radial-gradient(circle at top left, rgba(69, 134, 255, 0.2), transparent 28%),
    radial-gradient(circle at top right, rgba(26, 202, 221, 0.18), transparent 24%),
    linear-gradient(135deg, #08172d 0%, #10284c 42%, #0f5470 100%);
  --demand-panel-bg: linear-gradient(160deg, rgba(10, 28, 53, 0.94), rgba(15, 43, 79, 0.88));
  --demand-panel-border: rgba(134, 196, 255, 0.14);
  --demand-panel-shadow: 0 30px 60px rgba(3, 11, 28, 0.32), inset 0 1px 0 rgba(255, 255, 255, 0.06);
  --demand-title: #f6fbff;
  --demand-subtitle: rgba(214, 232, 255, 0.78);
  --demand-section-bg: rgba(255, 255, 255, 0.045);
  --demand-section-border: rgba(134, 196, 255, 0.12);
  --demand-section-title: #dff2ff;
  --demand-help: rgba(214, 232, 255, 0.72);
  --demand-label: #d9edff;
  --demand-input-bg: rgba(255, 255, 255, 0.1);
  --demand-input-border: rgba(134, 196, 255, 0.18);
}

@media (max-width: 900px) {
  .publish-panel {
    padding: 20px;
  }

  .split-grid {
    grid-template-columns: 1fr;
  }

  .publish-form {
    max-width: 100%;
  }

  .publish-btn {
    width: 100%;
  }

  .action-row {
    display: flex;
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }

  .reset-link {
    margin-left: 0;
    text-align: center;
  }
}
@media (max-width: 768px) {
  .demand-publish-page .page-content {
    padding-top: 14px;
    padding-bottom: 12px;
  }

  .publish-panel {
    padding: 18px 14px;
    border-radius: 24px;
    box-shadow: 0 20px 34px rgba(10, 28, 54, 0.14);
  }

  .panel-header {
    margin-bottom: 18px;
    padding-bottom: 14px;
    text-align: left;
  }

  .panel-title {
    font-size: 22px;
  }

  .form-section-card {
    padding: 16px 14px 8px;
    border-radius: 18px;
    background: rgba(255, 255, 255, 0.82);
  }

  .required-tip {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .publish-panel {
    padding: 16px 12px;
  }

  .panel-title {
    font-size: 20px;
  }

  .panel-subtitle {
    font-size: 13px;
    line-height: 1.7;
  }
}
</style>
