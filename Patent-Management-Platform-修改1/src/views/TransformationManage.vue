<template>
  <div class="transformation-page">
    <el-card class="panel" shadow="never">
      <div class="page-header">
        <div>
          <h2>成果转化管理</h2>
          <p>外部专利优先使用 `datasetId + recordId` 关联，旧记录仍兼容分类和公开号。</p>
        </div>
        <el-button type="primary" @click="openCreateDialog">新增成果</el-button>
      </div>

      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="数据集">
          <el-select v-model="searchForm.datasetId" clearable filterable placeholder="全部数据集" style="width: 260px">
            <el-option v-for="item in datasets" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.patentCategory" clearable placeholder="全部分类" style="width: 220px">
            <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="公开号">
          <el-input v-model="searchForm.patentPublicNum" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" clearable placeholder="全部状态" style="width: 180px">
            <el-option label="In Progress" value="In Progress" />
            <el-option label="Success" value="Success" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTransformations">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="panel" shadow="never">
      <el-table :data="transformationList" v-loading="loading" style="width: 100%">
        <el-table-column prop="patentCategory" label="分类" width="180" show-overflow-tooltip />
        <el-table-column prop="patentPublicNum" label="公开号" width="180" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="效益" width="160">
          <template #default="{ row }">
            {{ row.benefitAmount ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="transformationDate" label="转化日期" width="140" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewTransformation(row)">查看</el-button>
            <el-button link type="primary" @click="editTransformation(row)">编辑</el-button>
            <el-button link type="danger" @click="deleteTransformation(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="920px" @closed="resetDialog">
      <el-form :model="dialogSearchForm" inline class="search-form">
        <el-form-item label="数据集">
          <el-select v-model="dialogSearchForm.datasetId" filterable placeholder="请选择数据集" style="width: 280px">
            <el-option v-for="item in datasets" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词 / 公开号">
          <el-input v-model="dialogSearchForm.query" style="width: 240px" @keyup.enter="searchDialogPatents" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="dialogSearching" @click="searchDialogPatents">搜索专利</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="dialogPatents" v-loading="dialogSearching" max-height="240" style="width: 100%">
        <el-table-column prop="publicNum" label="公开号" width="180" />
        <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
        <el-table-column prop="applicant" label="申请人" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="choosePatent(row)">选中</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-alert
        v-if="selectedPatent"
        type="success"
        :closable="false"
        show-icon
        class="selected-alert"
        :title="selectedPatent.title || selectedPatent.publicNum"
        :description="`分类：${selectedPatent.category || '-'} | 公开号：${selectedPatent.publicNum || '-'} | 数据集：${selectedPatent.datasetName || '-'}`"
      />

      <el-form ref="formRef" :model="transformationForm" :rules="formRules" label-width="100px" class="detail-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="transformationForm.status" style="width: 100%">
                <el-option label="In Progress" value="In Progress" />
                <el-option label="Success" value="Success" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="转化日期">
              <el-date-picker v-model="transformationForm.transformationDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="专家用户ID">
              <el-input-number v-model="transformationForm.expertUserId" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="需求ID">
              <el-input-number v-model="transformationForm.requirementId" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="合作方ID">
              <el-input-number v-model="transformationForm.partnerOrgId" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="效益金额">
          <el-input-number v-model="transformationForm.benefitAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="transformationForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="saveTransformation">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="成果详情" width="760px">
      <el-descriptions v-if="currentTransformation" :column="2" border>
        <el-descriptions-item label="分类">{{ currentTransformation.patentCategory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公开号">{{ currentTransformation.patentPublicNum || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数据集ID">{{ currentTransformation.datasetId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="记录ID">{{ currentTransformation.recordId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ currentTransformation.status || '-' }}</el-descriptions-item>
        <el-descriptions-item label="转化日期">{{ currentTransformation.transformationDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="效益金额">{{ currentTransformation.benefitAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="专家用户ID">{{ currentTransformation.expertUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentTransformation.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { patentApi, transformationApi } from '@/api'
import { normalizeDatasetPatent, normalizePatentCategory } from '@/constants/domain'
import type { CreateTransformationParams, PatentBase, PatentCategoryOption, PatentDataset, TransformationResult } from '@/types'

const loading = ref(false)
const dialogSearching = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const dialogTitle = ref('新增成果')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const datasets = ref<PatentDataset[]>([])
const categories = ref<PatentCategoryOption[]>([])
const transformationList = ref<TransformationResult[]>([])
const dialogPatents = ref<PatentBase[]>([])
const selectedPatent = ref<PatentBase | null>(null)
const currentTransformation = ref<TransformationResult | null>(null)

const searchForm = reactive({
  datasetId: undefined as number | undefined,
  patentCategory: '',
  patentPublicNum: '',
  status: ''
})

const dialogSearchForm = reactive({
  datasetId: undefined as number | undefined,
  query: ''
})

const transformationForm = reactive<CreateTransformationParams>({
  patentSource: 'EXTERNAL',
  status: 'In Progress',
  description: '',
  transformationDate: '',
  benefitAmount: undefined,
  expertUserId: undefined,
  requirementId: undefined,
  partnerOrgId: undefined
})

const formRules: FormRules = {
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const loadMeta = async () => {
  try {
    const [datasetList, categoryList] = await Promise.all([
      patentApi.getPatentDatasets(),
      patentApi.getPatentCategories()
    ])
    datasets.value = datasetList
    categories.value = categoryList
  } catch (error: any) {
    ElMessage.error(error.message || '加载元数据失败')
  }
}

const loadTransformations = async () => {
  loading.value = true
  try {
    transformationList.value = await transformationApi.getAllTransformations({
      patentSource: 'EXTERNAL',
      datasetId: searchForm.datasetId,
      patentCategory: normalizePatentCategory(searchForm.patentCategory) || undefined,
      patentPublicNum: searchForm.patentPublicNum.trim() || undefined,
      status: searchForm.status || undefined
    })
  } catch (error: any) {
    transformationList.value = []
    ElMessage.error(error.message || '加载转化结果失败')
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.datasetId = undefined
  searchForm.patentCategory = ''
  searchForm.patentPublicNum = ''
  searchForm.status = ''
  void loadTransformations()
}

const resetDialog = () => {
  editingId.value = null
  dialogPatents.value = []
  selectedPatent.value = null
  dialogSearchForm.datasetId = undefined
  dialogSearchForm.query = ''
  Object.assign(transformationForm, {
    patentSource: 'EXTERNAL',
    datasetId: undefined,
    recordId: undefined,
    patentCategory: undefined,
    patentPublicNum: undefined,
    userPatentId: undefined,
    expertUserId: undefined,
    requirementId: undefined,
    partnerOrgId: undefined,
    description: '',
    transformationDate: '',
    status: 'In Progress',
    benefitAmount: undefined
  })
  formRef.value?.clearValidate()
}

const openCreateDialog = () => {
  dialogTitle.value = '新增成果'
  resetDialog()
  dialogVisible.value = true
}

const searchDialogPatents = async () => {
  if (!dialogSearchForm.datasetId) {
    ElMessage.warning('请先选择数据集')
    return
  }
  if (!dialogSearchForm.query.trim()) {
    ElMessage.warning('请输入关键词或公开号')
    return
  }
  dialogSearching.value = true
  try {
    const response = await patentApi.searchPatentDataset(dialogSearchForm.datasetId, {
      query: dialogSearchForm.query.trim(),
      page: 0,
      size: 20
    })
    const dataset = datasets.value.find(item => item.id === dialogSearchForm.datasetId)
    dialogPatents.value = (response.content || []).map(item => {
      const patent = normalizeDatasetPatent(item)
      return {
        ...patent,
        datasetId: patent.datasetId || dialogSearchForm.datasetId,
        datasetName: patent.datasetName || dataset?.name || '',
        category: normalizePatentCategory(patent.category || dataset?.category || '')
      }
    })
  } catch (error: any) {
    dialogPatents.value = []
    ElMessage.error(error.message || '搜索专利失败')
  } finally {
    dialogSearching.value = false
  }
}

const choosePatent = (patent: PatentBase) => {
  selectedPatent.value = patent
  transformationForm.datasetId = patent.datasetId
  transformationForm.recordId = patent.recordId
  transformationForm.patentCategory = normalizePatentCategory(patent.category)
  transformationForm.patentPublicNum = patent.publicNum
  if (!transformationForm.description) {
    transformationForm.description = patent.title
  }
}

const saveTransformation = async () => {
  await formRef.value?.validate()
  if (!transformationForm.datasetId || !transformationForm.recordId) {
    ElMessage.warning('请先选择数据集中的专利')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await transformationApi.updateTransformation(editingId.value, { ...transformationForm })
    } else {
      await transformationApi.createTransformation({ ...transformationForm })
    }
    dialogVisible.value = false
    await loadTransformations()
    ElMessage.success('保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const viewTransformation = (row: TransformationResult) => {
  currentTransformation.value = row
  detailDialogVisible.value = true
}

const editTransformation = (row: TransformationResult) => {
  dialogTitle.value = '编辑成果'
  resetDialog()
  editingId.value = row.id
  dialogVisible.value = true
  dialogSearchForm.datasetId = row.datasetId
  dialogSearchForm.query = row.patentPublicNum || ''
  Object.assign(transformationForm, {
    patentSource: row.patentSource || 'EXTERNAL',
    datasetId: row.datasetId,
    recordId: row.recordId,
    patentCategory: normalizePatentCategory(row.patentCategory),
    patentPublicNum: row.patentPublicNum,
    userPatentId: row.userPatentId,
    expertUserId: row.expertUserId,
    requirementId: row.requirementId,
    partnerOrgId: row.partnerOrgId,
    description: row.description,
    transformationDate: row.transformationDate,
    status: row.status,
    benefitAmount: row.benefitAmount
  })
  selectedPatent.value = {
    datasetId: row.datasetId,
    recordId: row.recordId,
    category: normalizePatentCategory(row.patentCategory),
    publicNum: row.patentPublicNum,
    title: row.description || row.patentPublicNum || '',
    abstractText: '',
    applicant: '',
    inventor: '',
    ipc: ''
  }
}

const deleteTransformation = async (row: TransformationResult) => {
  try {
    await ElMessageBox.confirm(`确认删除成果记录 ${row.patentPublicNum || row.id} 吗？`, '删除确认', { type: 'warning' })
    await transformationApi.deleteTransformation(row.id)
    await loadTransformations()
    ElMessage.success('删除成功')
  } catch {
    // user canceled
  }
}

onMounted(() => {
  void Promise.all([loadMeta(), loadTransformations()])
})
</script>

<style scoped>
.transformation-page {
  padding: 24px;
  display: grid;
  gap: 20px;
}

.panel {
  border-radius: 20px;
}

.page-header,
.search-form {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
}

.page-header h2 {
  margin: 0 0 8px;
}

.page-header p {
  margin: 0;
  color: var(--el-text-color-secondary);
}

.detail-form,
.selected-alert {
  margin-top: 16px;
}
</style>
