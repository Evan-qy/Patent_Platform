<template>
  <div class="page-container patent-manage-page">
    <div class="glass-header">
      <div class="header-title-block">
        <h2 class="glass-page-title text-gradient-cyan">我的专利</h2>
        <p class="header-subtitle">用户自有专利仍按原逻辑管理，分类选项来自后台启用数据集。</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" class="btn-primary" @click="openCreateDialog">新增专利</el-button>
        <el-button class="btn-secondary" @click="loadUserPatents">刷新</el-button>
      </div>
    </div>

    <div class="page-content">
      <section class="panel-glow search-panel">
        <div class="panel-head">
          <h3>专利筛选</h3>
          <p>按标题、公开号与分类快速定位已录入的专利记录。</p>
        </div>
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.query" class="glass-input" placeholder="标题 / 公开号 / 申请人" @keyup.enter="searchUserPatents" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" class="glass-input" clearable placeholder="全部分类" style="width: 220px">
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" class="btn-primary" @click="searchUserPatents">查询</el-button>
          <el-button class="btn-secondary" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      </section>

      <section class="panel-glow table-panel">
        <div class="panel-head">
          <h3>专利列表</h3>
          <p>统一展示当前用户可管理的专利信息，并支持查看、编辑、删除。</p>
        </div>
      <el-table :data="userPatents" v-loading="loading" class="glass-table" style="width: 100%">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="180" show-overflow-tooltip />
        <el-table-column prop="publicNum" label="公开号" width="180" show-overflow-tooltip />
        <el-table-column prop="applicant" label="申请人" min-width="180" show-overflow-tooltip />
        <el-table-column prop="visibility" label="可见性" width="120">
          <template #default="{ row }">
            <el-tag :type="row.visibility === 'PUBLIC' ? 'success' : 'warning'">
              {{ row.visibility === 'PUBLIC' ? '公开' : '私有' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewPatent(row)">查看</el-button>
            <el-button link type="primary" @click="editPatent(row)">编辑</el-button>
            <el-button link type="danger" @click="deletePatent(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      </section>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" @closed="resetForm">
      <el-form ref="formRef" :model="patentForm" :rules="patentRules" label-width="100px">
        <el-form-item label="分类" prop="category">
          <el-select v-model="patentForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="patentForm.title" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="公开号">
              <el-input v-model="patentForm.publicNum" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="IPC">
              <el-input v-model="patentForm.ipc" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="申请人">
              <el-input v-model="patentForm.applicant" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发明人">
              <el-input v-model="patentForm.inventor" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="可见性">
          <el-select v-model="patentForm.visibility" style="width: 100%">
            <el-option label="公开" value="PUBLIC" />
            <el-option label="私有" value="PRIVATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="patentForm.abstractText" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPatent">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { patentApi } from '@/api'
import type { CreateUserPatentParams, PatentCategoryOption, UpdateUserPatentParams, UserPatent } from '@/types'

interface PatentFormState extends CreateUserPatentParams {
  id?: number
}

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增专利')
const formRef = ref<FormInstance>()

const categoryOptions = ref<PatentCategoryOption[]>([])
const userPatents = ref<UserPatent[]>([])

const searchForm = reactive({
  query: '',
  category: ''
})

const patentForm = reactive<PatentFormState>({
  category: '',
  title: '',
  publicNum: '',
  abstractText: '',
  ipc: '',
  cpc: '',
  applicant: '',
  inventor: '',
  visibility: 'PUBLIC'
})

const patentRules: FormRules = {
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

const loadCategoryOptions = async () => {
  try {
    categoryOptions.value = await patentApi.getPatentCategories()
  } catch (error: any) {
    categoryOptions.value = []
    ElMessage.error(error.message || '加载分类失败')
  }
}

const loadUserPatents = async () => {
  loading.value = true
  try {
    userPatents.value = await patentApi.getUserPatentsList({
      owner: 'me',
      query: searchForm.query.trim() || undefined,
      category: searchForm.category || undefined
    })
  } catch (error: any) {
    userPatents.value = []
    ElMessage.error(error.message || '加载专利列表失败')
  } finally {
    loading.value = false
  }
}

const searchUserPatents = () => {
  void loadUserPatents()
}

const resetSearch = () => {
  searchForm.query = ''
  searchForm.category = ''
  void loadUserPatents()
}

const resetForm = () => {
  Object.assign(patentForm, {
    id: undefined,
    category: '',
    title: '',
    publicNum: '',
    abstractText: '',
    ipc: '',
    cpc: '',
    applicant: '',
    inventor: '',
    visibility: 'PUBLIC'
  })
  formRef.value?.clearValidate()
}

const openCreateDialog = () => {
  dialogTitle.value = '新增专利'
  resetForm()
  dialogVisible.value = true
}

const editPatent = (patent: UserPatent) => {
  dialogTitle.value = '编辑专利'
  Object.assign(patentForm, patent)
  dialogVisible.value = true
}

const submitPatent = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: CreateUserPatentParams | UpdateUserPatentParams = {
      category: patentForm.category,
      title: patentForm.title,
      publicNum: patentForm.publicNum,
      abstractText: patentForm.abstractText,
      ipc: patentForm.ipc,
      cpc: patentForm.cpc,
      applicant: patentForm.applicant,
      inventor: patentForm.inventor,
      visibility: patentForm.visibility
    }

    if (patentForm.id) {
      await patentApi.updateUserPatent(patentForm.id, payload)
    } else {
      await patentApi.createUserPatent(payload as CreateUserPatentParams)
    }

    dialogVisible.value = false
    await loadUserPatents()
    ElMessage.success('保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const viewPatent = (patent: UserPatent) => {
  ElMessageBox.alert(
    [
      `标题：${patent.title}`,
      `分类：${patent.category}`,
      `公开号：${patent.publicNum || '-'}`,
      `申请人：${patent.applicant || '-'}`,
      `发明人：${patent.inventor || '-'}`,
      `IPC：${patent.ipc || '-'}`,
      `摘要：${patent.abstractText || '-'}`
    ].join('<br/>'),
    '专利详情',
    { dangerouslyUseHTMLString: true }
  )
}

const deletePatent = async (patent: UserPatent) => {
  try {
    await ElMessageBox.confirm(`确认删除专利“${patent.title}”吗？`, '删除确认', { type: 'warning' })
    await patentApi.deleteUserPatent(patent.id)
    await loadUserPatents()
    ElMessage.success('删除成功')
  } catch {
    // user canceled
  }
}

onMounted(() => {
  void Promise.all([loadCategoryOptions(), loadUserPatents()])
})
</script>

<style scoped>
.patent-manage-page {
  --pm-subtitle: #5b708c;
  --pm-panel-bg: linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(243, 248, 255, 0.94));
  --pm-panel-border: rgba(61, 125, 255, 0.14);
  --pm-panel-title: #12355c;
  --pm-panel-text: #5f7898;
  --pm-label: #34587f;
  --pm-input-bg: rgba(255, 255, 255, 0.92);
  --pm-input-border: rgba(61, 125, 255, 0.18);
  --pm-input-text: #10263f;
  --pm-placeholder: #6c7f93;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header-title-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-subtitle {
  margin: 0;
  color: var(--pm-subtitle);
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.page-content {
  padding: 24px;
  display: grid;
  gap: 20px;
}

.panel-glow {
  padding: 24px;
  border-radius: 16px;
  background: var(--pm-panel-bg) !important;
  border: 1px solid var(--pm-panel-border) !important;
}

.panel-head {
  margin-bottom: 16px;
}

.panel-head h3 {
  margin: 0 0 6px;
  color: var(--pm-panel-title);
}

.panel-head p {
  margin: 0;
  color: var(--pm-panel-text);
  font-size: 13px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.search-actions {
  margin-left: auto;
}

.search-form :deep(.el-form-item__label) {
  color: var(--pm-label);
  font-weight: 600;
}

.glass-input :deep(.el-input__wrapper),
.glass-input :deep(.el-select__wrapper) {
  background: var(--pm-input-bg) !important;
  box-shadow: 0 0 0 1px var(--pm-input-border) inset !important;
}

.glass-input :deep(.el-input__inner),
.glass-input :deep(.el-select__selected-item),
.glass-input :deep(.el-select__placeholder) {
  color: var(--pm-input-text) !important;
}

.glass-input :deep(.el-input__inner::placeholder) {
  color: var(--pm-placeholder) !important;
}

.glass-table {
  --el-table-border-color: rgba(61, 125, 255, 0.2);
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: var(--bg-card-header);
  --el-table-row-hover-bg-color: rgba(61, 125, 255, 0.1);
  --el-text-color-regular: var(--text-secondary);
}

:deep(.el-table) {
  --el-table-header-bg-color: transparent;
  --el-table-bg-color: transparent;
  --el-table-text-color: var(--text-primary);
  --el-table-header-text-color: var(--text-secondary);
  --el-table-border-color: var(--border-color);
}

:deep(.el-table .el-table__cell) {
  background: transparent;
}

:deep(.el-table .el-table__cell .cell) {
  color: var(--text-primary);
}

[data-theme="dark"] .patent-manage-page {
  --pm-subtitle: rgba(214, 232, 255, 0.78);
  --pm-panel-bg: linear-gradient(145deg, rgba(8, 26, 50, 0.88), rgba(11, 38, 72, 0.8));
  --pm-panel-border: rgba(134, 196, 255, 0.14);
  --pm-panel-title: #d8efff;
  --pm-panel-text: rgba(214, 232, 255, 0.8);
  --pm-label: #d9edff;
  --pm-input-bg: rgba(255, 255, 255, 0.08);
  --pm-input-border: rgba(134, 196, 255, 0.18);
  --pm-input-text: #f2f9ff;
  --pm-placeholder: rgba(214, 232, 255, 0.68);
}

.glass-table :deep(.el-table__cell .cell) {
  color: var(--text-primary);
}

/* Keep table header/body contrast stable in both themes. */
.glass-table :deep(th.el-table__cell) {
  background: var(--bg-card-header) !important;
  color: var(--text-primary) !important;
}

.glass-table :deep(td.el-table__cell),
.glass-table :deep(.el-table__body tr.hover-row > td.el-table__cell) {
  background: transparent !important;
}

::deep(.el-dialog) {
  border-radius: 20px;
}

::deep(.el-dialog__title),
::deep(.el-dialog__body),
::deep(.el-form-item__label) {
  color: var(--text-primary);
}

@media (max-width: 768px) {
  .glass-header {
    gap: 14px;
    padding: 18px 16px;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions :deep(.el-button) {
    width: 100%;
    margin: 0;
  }

  .page-content {
    padding: 14px;
  }

  .panel-glow {
    padding: 18px;
    border-radius: 18px;
  }

  .search-form {
    flex-direction: column;
    gap: 8px;
  }

  .search-form :deep(.el-form-item) {
    margin-right: 0;
  }

  .search-actions {
    margin-left: 0;
  }

  .search-actions :deep(.el-button) {
    width: 100%;
    margin: 0;
  }
}

@media (max-width: 480px) {
  .glass-header,
  .page-content {
    padding-left: 12px;
    padding-right: 12px;
  }
}
</style>
