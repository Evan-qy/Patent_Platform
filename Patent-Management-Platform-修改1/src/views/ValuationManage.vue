<template>
  <div class="valuation-page">
    <el-card class="panel filter-panel" shadow="never">
      <div class="page-header">
        <div>
          <h2>价值评估报告中心</h2>
          <p>按数据集、分类和公开号筛选历史报告，也可以直接新建一份新的评估。</p>
        </div>
        <el-button type="primary" class="header-button" @click="openGenerateDialog">新建评估</el-button>
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
          <el-input v-model="searchForm.patentPublicNum" placeholder="输入公开号" />
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="searchValuations">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="panel list-panel" shadow="never">
      <div class="desktop-table">
        <el-table :data="valuationList" v-loading="loading" style="width: 100%">
          <el-table-column prop="patentPublicNum" label="公开号" width="180" show-overflow-tooltip />
          <el-table-column prop="reportTitle" label="报告标题" min-width="260" show-overflow-tooltip />
          <el-table-column prop="patentCategory" label="分类" width="160" show-overflow-tooltip />
          <el-table-column label="预测价值" width="170">
            <template #default="{ row }">
              {{ formatCurrency(row.predictedValue || row.valuationAmount || 0) }}
            </template>
          </el-table-column>
          <el-table-column prop="modelVersion" label="模型版本" width="120" />
          <el-table-column prop="valuationDate" label="评估日期" width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewReport(row)">查看</el-button>
              <el-button link type="success" @click="regenerateFromRow(row)">重新评估</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="mobile-report-list" v-loading="loading">
        <article
          v-for="row in valuationList"
          :key="row.id || `${row.datasetId}-${row.recordId}-${row.patentPublicNum}`"
          class="mobile-report-card"
        >
          <div class="mobile-report-head">
            <div class="report-head-main">
              <strong>{{ row.reportTitle || row.patentPublicNum || '评估报告' }}</strong>
              <p>{{ row.patentCategory || '未标注分类' }}</p>
            </div>
            <span class="report-amount">{{ formatCurrency(row.predictedValue || row.valuationAmount || 0) }}</span>
          </div>

          <div class="mobile-report-meta">
            <span>公开号：{{ row.patentPublicNum || '-' }}</span>
            <span>模型：{{ row.modelVersion || '-' }}</span>
            <span>评估日期：{{ row.valuationDate || row.createdAt || '-' }}</span>
          </div>

          <div class="mobile-report-actions">
            <el-button size="small" type="primary" plain @click="viewReport(row)">查看</el-button>
            <el-button size="small" type="success" plain @click="regenerateFromRow(row)">重新评估</el-button>
          </div>
        </article>

        <div v-if="!loading && !valuationList.length" class="empty-state">
          当前没有匹配的评估报告，可以先新建一份。
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="generateDialogVisible"
      title="新建价值评估"
      :width="dialogWidth"
      :fullscreen="isMobile"
      class="valuation-dialog"
    >
      <el-form :model="generateForm" inline class="search-form dialog-form">
        <el-form-item label="数据集">
          <el-select v-model="generateForm.datasetId" filterable placeholder="请选择数据集" style="width: 280px">
            <el-option v-for="item in datasets" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词 / 公开号">
          <el-input v-model="generateForm.query" style="width: 260px" @keyup.enter="searchGeneratePatents" />
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" :loading="generateSearching" @click="searchGeneratePatents">搜索专利</el-button>
        </el-form-item>
      </el-form>

      <div class="desktop-table">
        <el-table :data="generateResults" v-loading="generateSearching" max-height="320" style="width: 100%">
          <el-table-column prop="publicNum" label="公开号" width="180" />
          <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
          <el-table-column prop="applicant" label="申请人" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="chooseGeneratePatent(row)">选中</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="mobile-report-list compact" v-loading="generateSearching">
        <article
          v-for="row in generateResults"
          :key="row.recordId || row.publicNum || row.title"
          class="mobile-report-card selectable"
        >
          <div class="mobile-report-head">
            <div class="report-head-main">
              <strong>{{ row.title || '未提供标题' }}</strong>
              <p>{{ row.publicNum || '未提供公开号' }}</p>
            </div>
          </div>

          <div class="mobile-report-meta">
            <span>申请人：{{ row.applicant || '-' }}</span>
            <span>数据集：{{ row.datasetName || '-' }}</span>
          </div>

          <div class="mobile-report-actions">
            <el-button size="small" type="primary" plain @click="chooseGeneratePatent(row)">选中</el-button>
          </div>
        </article>
      </div>

      <el-alert
        v-if="selectedGeneratePatent"
        type="success"
        :closable="false"
        show-icon
        class="selected-alert"
        :title="selectedGeneratePatent.title || selectedGeneratePatent.publicNum"
        :description="`数据集：${selectedGeneratePatent.datasetName || '-'} | 分类：${selectedGeneratePatent.category || '-'} | 公开号：${selectedGeneratePatent.publicNum || '-'}`"
      />

      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="generateReport">生成报告</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailDialogVisible"
      title="评估报告详情"
      :width="detailDialogWidth"
      :fullscreen="isMobile"
      class="valuation-dialog"
    >
      <div v-if="currentReport" class="detail-shell">
        <div class="detail-summary">
          <div class="summary-card">
            <span>预测价值</span>
            <strong>{{ formatCurrency(currentReport.predictedValue || currentReport.valuationAmount || 0) }}</strong>
          </div>
          <div class="summary-card">
            <span>模型版本</span>
            <strong>{{ currentReport.modelVersion || '-' }}</strong>
          </div>
          <div class="summary-card">
            <span>评估日期</span>
            <strong>{{ currentReport.valuationDate || currentReport.createdAt || '-' }}</strong>
          </div>
        </div>

        <el-descriptions :column="isMobile ? 1 : 2" border>
          <el-descriptions-item label="报告标题" :span="isMobile ? 1 : 2">{{ currentReport.reportTitle }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ currentReport.patentCategory || '-' }}</el-descriptions-item>
          <el-descriptions-item label="公开号">{{ currentReport.patentPublicNum || '-' }}</el-descriptions-item>
          <el-descriptions-item label="数据集 ID">{{ currentReport.datasetId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="记录 ID">{{ currentReport.recordId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="专利标题" :span="isMobile ? 1 : 2">{{ currentReport.patentInfo?.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="摘要" :span="isMobile ? 1 : 2">{{ currentReport.patentInfo?.abstract || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { patentApi, valuationApi } from '@/api'
import { formatCurrency, normalizeDatasetPatent, normalizePatentCategory } from '@/constants/domain'
import type { PatentBase, PatentCategoryOption, PatentDataset, QueryValuationParams, ValuationReport } from '@/types'

const route = useRoute()

const loading = ref(false)
const generateSearching = ref(false)
const generating = ref(false)
const detailDialogVisible = ref(false)
const generateDialogVisible = ref(false)
const viewportWidth = ref(window.innerWidth)

const datasets = ref<PatentDataset[]>([])
const categories = ref<PatentCategoryOption[]>([])
const valuationList = ref<ValuationReport[]>([])
const generateResults = ref<PatentBase[]>([])
const selectedGeneratePatent = ref<PatentBase | null>(null)
const currentReport = ref<ValuationReport | null>(null)

const searchForm = reactive({
  datasetId: undefined as number | undefined,
  patentCategory: '',
  patentPublicNum: ''
})

const generateForm = reactive({
  datasetId: undefined as number | undefined,
  query: ''
})

const isMobile = computed(() => viewportWidth.value <= 768)
const dialogWidth = computed(() => (isMobile.value ? '100%' : '920px'))
const detailDialogWidth = computed(() => (isMobile.value ? '100%' : '860px'))

const handleWindowResize = () => {
  viewportWidth.value = window.innerWidth
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

const buildQuery = (): QueryValuationParams => ({
  patentSource: 'EXTERNAL',
  datasetId: searchForm.datasetId,
  patentCategory: normalizePatentCategory(searchForm.patentCategory) || undefined,
  patentPublicNum: searchForm.patentPublicNum.trim() || undefined,
  recordId: typeof route.query.recordId === 'string' ? route.query.recordId : undefined
})

const searchValuations = async () => {
  loading.value = true
  try {
    valuationList.value = await valuationApi.getValuationReports(buildQuery())
  } catch (error: any) {
    valuationList.value = []
    ElMessage.error(error.message || '加载评估报告失败')
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.datasetId = undefined
  searchForm.patentCategory = ''
  searchForm.patentPublicNum = ''
  void searchValuations()
}

const openGenerateDialog = () => {
  generateDialogVisible.value = true
  generateForm.datasetId = searchForm.datasetId
  generateForm.query = searchForm.patentPublicNum
  generateResults.value = []
  selectedGeneratePatent.value = null
}

const searchGeneratePatents = async () => {
  if (!generateForm.datasetId) {
    ElMessage.warning('请先选择数据集')
    return
  }
  if (!generateForm.query.trim()) {
    ElMessage.warning('请输入关键词或公开号')
    return
  }

  generateSearching.value = true
  try {
    const response = await patentApi.searchPatentDataset(generateForm.datasetId, {
      query: generateForm.query.trim(),
      page: 0,
      size: 20
    })
    const dataset = datasets.value.find(item => item.id === generateForm.datasetId)
    generateResults.value = (response.content || []).map(item => {
      const patent = normalizeDatasetPatent(item)
      return {
        ...patent,
        datasetId: patent.datasetId || generateForm.datasetId,
        datasetName: patent.datasetName || dataset?.name || '',
        category: normalizePatentCategory(patent.category || dataset?.category || '')
      }
    })
  } catch (error: any) {
    generateResults.value = []
    ElMessage.error(error.message || '搜索专利失败')
  } finally {
    generateSearching.value = false
  }
}

const chooseGeneratePatent = (patent: PatentBase) => {
  selectedGeneratePatent.value = { ...patent }
  ElMessage.success('已选中目标专利')
}

const generateReport = async () => {
  if (!selectedGeneratePatent.value?.datasetId || !selectedGeneratePatent.value.recordId) {
    ElMessage.warning('请先选中专利')
    return
  }

  generating.value = true
  try {
    const result = await valuationApi.generateValuationReport({
      patentSource: 'EXTERNAL',
      datasetId: selectedGeneratePatent.value.datasetId,
      recordId: selectedGeneratePatent.value.recordId,
      patentCategory: normalizePatentCategory(selectedGeneratePatent.value.category),
      patentPublicNum: selectedGeneratePatent.value.publicNum,
      modelVersion: 'v2.0'
    })
    generateDialogVisible.value = false
    currentReport.value = result
    detailDialogVisible.value = true
    await searchValuations()
    ElMessage.success('评估报告已生成')
  } catch (error: any) {
    ElMessage.error(error.message || '生成评估失败')
  } finally {
    generating.value = false
  }
}

const viewReport = (row: ValuationReport) => {
  currentReport.value = row
  detailDialogVisible.value = true
}

const regenerateFromRow = (row: ValuationReport) => {
  openGenerateDialog()
  generateForm.datasetId = row.datasetId
  generateForm.query = row.patentPublicNum || ''
}

onMounted(() => {
  window.addEventListener('resize', handleWindowResize)

  if (typeof route.query.datasetId === 'string') {
    const datasetId = Number(route.query.datasetId)
    if (!Number.isNaN(datasetId)) {
      searchForm.datasetId = datasetId
    }
  }
  if (typeof route.query.patentCategory === 'string') {
    searchForm.patentCategory = normalizePatentCategory(route.query.patentCategory)
  }
  if (typeof route.query.patentPublicNum === 'string') {
    searchForm.patentPublicNum = route.query.patentPublicNum
  }

  void Promise.all([loadMeta(), searchValuations()])
})

onUnmounted(() => {
  window.removeEventListener('resize', handleWindowResize)
})
</script>

<style scoped>
.valuation-page {
  padding: 24px;
  display: grid;
  gap: 20px;
}

.panel {
  border-radius: 24px;
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
  line-height: 1.6;
}

.desktop-table {
  display: block;
}

.mobile-report-list {
  display: none;
}

.selected-alert {
  margin-top: 16px;
}

.detail-shell {
  display: grid;
  gap: 16px;
}

.detail-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  padding: 16px;
  border-radius: 18px;
  background: rgba(247, 250, 255, 0.96);
  border: 1px solid var(--el-border-color-light);
}

.summary-card span {
  display: block;
  margin-bottom: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.summary-card strong {
  font-size: 16px;
  line-height: 1.5;
  word-break: break-word;
}

@media (min-width: 769px) {
  .valuation-page {
    max-width: 1260px;
    margin: 0 auto;
    padding: 28px 32px 40px;
  }

  .panel {
    border-radius: 18px;
    box-shadow: 0 12px 28px rgba(16, 38, 63, 0.08);
  }
}

@media (max-width: 768px) {
  .valuation-page {
    padding: 14px;
    gap: 14px;
  }

  .page-header,
  .search-form {
    flex-direction: column;
    align-items: stretch;
  }

  .search-form :deep(.el-form-item),
  .search-form :deep(.el-select),
  .search-form :deep(.el-input) {
    width: 100% !important;
  }

  .search-form :deep(.el-button),
  .header-button {
    width: 100%;
    margin: 0;
  }

  .desktop-table {
    display: none;
  }

  .mobile-report-list {
    display: grid;
    gap: 12px;
  }

  .mobile-report-list.compact {
    margin-top: 12px;
  }

  .mobile-report-card {
    padding: 16px;
    border-radius: 20px;
    border: 1px solid rgba(75, 122, 191, 0.16);
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 248, 255, 0.96));
    box-shadow: 0 12px 28px rgba(28, 52, 88, 0.08);
    display: grid;
    gap: 12px;
  }

  .mobile-report-head {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
  }

  .report-head-main {
    min-width: 0;
  }

  .report-head-main strong {
    display: block;
    font-size: 15px;
    line-height: 1.6;
    word-break: break-word;
  }

  .report-head-main p {
    margin: 6px 0 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.5;
  }

  .report-amount {
    flex: 0 0 auto;
    text-align: right;
    color: #245dc7;
    font-size: 14px;
    font-weight: 700;
    line-height: 1.5;
  }

  .mobile-report-meta {
    display: grid;
    gap: 6px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.5;
  }

  .mobile-report-actions {
    display: flex;
    gap: 8px;
  }

  .mobile-report-actions :deep(.el-button) {
    flex: 1 1 0;
    margin: 0;
  }

  .empty-state {
    padding: 24px 16px;
    text-align: center;
    border-radius: 18px;
    background: rgba(247, 250, 255, 0.94);
    border: 1px dashed rgba(75, 122, 191, 0.22);
    color: var(--el-text-color-secondary);
  }

  .detail-summary {
    grid-template-columns: 1fr;
  }

  :deep(.el-dialog__body) {
    padding-top: 16px;
  }
}
</style>
