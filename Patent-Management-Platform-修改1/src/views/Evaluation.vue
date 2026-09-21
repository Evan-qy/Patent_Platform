<template>
  <div class="evaluation-page">
    <el-card class="panel" shadow="never">
      <el-steps :active="activeStep" align-center finish-status="success" :simple="isMobile">
        <el-step title="选择专利" />
        <el-step title="确认权重" />
        <el-step title="生成报告" />
      </el-steps>

      <section v-if="activeStep === 0" class="step-section">
        <div class="section-head">
          <h3>先从启用的数据集中选一件专利</h3>
          <p>支持按关键词、公开号或标题检索。选中目标后，再进入权重确认和报告生成。</p>
        </div>

        <el-form :model="searchForm" inline class="search-form">
          <el-form-item label="数据集">
            <el-select v-model="searchForm.datasetId" filterable placeholder="请选择数据集" style="width: 320px">
              <el-option v-for="item in datasets" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词 / 公开号">
            <el-input
              v-model="searchForm.query"
              placeholder="支持标题、摘要、公开号等关键词"
              style="width: 320px"
              @keyup.enter="searchDatasetPatents"
            />
          </el-form-item>
          <el-form-item class="search-actions">
            <el-button type="primary" :loading="searching" @click="searchDatasetPatents">搜索</el-button>
          </el-form-item>
        </el-form>

        <div class="desktop-table">
          <el-table :data="searchResults" v-loading="searching" max-height="360" style="width: 100%">
            <el-table-column prop="publicNum" label="公开号" width="180" show-overflow-tooltip />
            <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
            <el-table-column prop="applicant" label="申请人" min-width="180" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="selectPatent(row)">选中</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="mobile-result-list" v-loading="searching">
          <article
            v-for="row in searchResults"
            :key="row.recordId || row.publicNum || row.title"
            class="mobile-result-card"
          >
            <div class="mobile-result-head">
              <div>
                <strong>{{ row.title || '未提供标题' }}</strong>
                <p>{{ row.publicNum || '未提供公开号' }}</p>
              </div>
              <span>{{ row.category || '未标注分类' }}</span>
            </div>
            <div class="mobile-result-meta">
              <span>申请人：{{ row.applicant || '-' }}</span>
              <span>数据集：{{ row.datasetName || '-' }}</span>
            </div>
            <div class="mobile-result-actions">
              <el-button size="small" type="primary" plain @click="selectPatent(row)">选中</el-button>
            </div>
          </article>

          <div v-if="!searching && !searchResults.length" class="empty-state">
            还没有搜索结果，先输入关键词或公开号开始检索。
          </div>
        </div>

        <el-alert
          v-if="selectedPatent"
          type="success"
          :closable="false"
          show-icon
          class="selected-alert"
          :title="selectedPatent.title || selectedPatent.publicNum"
          :description="`数据集：${selectedPatent.datasetName || '-'} | 分类：${selectedPatent.category || '-'} | 公开号：${selectedPatent.publicNum || '-'}`"
        />

        <div class="actions">
          <el-button type="primary" :disabled="!selectedPatent" @click="activeStep = 1">下一步</el-button>
        </div>
      </section>

      <section v-else-if="activeStep === 1" class="step-section">
        <div class="section-head">
          <h3>确认评估权重</h3>
          <p>总权重需要等于 100%。当前模型版本固定为 <code>v2.0</code>。</p>
        </div>

        <div class="weight-grid">
          <div v-for="key in dimensionKeys" :key="key" class="weight-card">
            <strong>{{ VALUATION_DIMENSION_LABELS[key] }}</strong>
            <el-slider v-model="weights[key]" :min="0" :max="100" />
            <span>{{ weights[key] }}%</span>
          </div>
        </div>

        <el-alert
          :title="`当前总权重：${totalWeight}%`"
          :type="totalWeight === 100 ? 'success' : 'warning'"
          :closable="false"
          show-icon
        />

        <div class="actions">
          <el-button @click="activeStep = 0">上一步</el-button>
          <el-button type="primary" :disabled="totalWeight !== 100" @click="activeStep = 2">下一步</el-button>
        </div>
      </section>

      <section v-else class="step-section">
        <div class="section-head">
          <h3>生成评估报告</h3>
          <p>系统会基于你选中的数据集记录生成报告，并同步保存到报告中心。</p>
        </div>

        <el-descriptions :column="isMobile ? 1 : 2" border>
          <el-descriptions-item label="数据集">{{ selectedPatent?.datasetName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ selectedPatent?.category || '-' }}</el-descriptions-item>
          <el-descriptions-item label="公开号">{{ selectedPatent?.publicNum || '-' }}</el-descriptions-item>
          <el-descriptions-item label="模型版本">v2.0</el-descriptions-item>
          <el-descriptions-item label="标题" :span="isMobile ? 1 : 2">{{ selectedPatent?.title || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="actions">
          <el-button @click="activeStep = 1">上一步</el-button>
          <el-button type="primary" :loading="generating" @click="generateEvaluation">生成报告</el-button>
        </div>
      </section>
    </el-card>

    <el-card v-if="report" class="panel report-panel" shadow="never">
      <div class="section-head summary-head">
        <div>
          <h3>{{ report.reportTitle }}</h3>
          <p>{{ report.reportTime || report.valuationDate || report.createdAt }}</p>
        </div>
        <div class="amount-block">
          <span>预测价值</span>
          <strong>{{ formatWan(report.predictedValue || report.valuationAmount || 0) }}</strong>
        </div>
      </div>

      <el-row :gutter="16">
        <el-col v-for="key in dimensionKeys" :key="key" :span="isMobile ? 24 : 6">
          <el-card shadow="never" class="mini-card">
            <strong>{{ report.dimensionScores?.[key]?.score ?? 0 }}</strong>
            <div>{{ VALUATION_DIMENSION_LABELS[key] }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="summary-card">
        <h4>结论摘要</h4>
        <p>{{ report.conclusion?.summary || '-' }}</p>
      </el-card>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { patentApi, valuationApi } from '@/api'
import { formatWan, normalizeDatasetPatent, normalizePatentCategory, VALUATION_DIMENSION_LABELS } from '@/constants/domain'
import type {
  GenerateValuationParams,
  PatentBase,
  PatentDataset,
  ValuationDimensionKey,
  ValuationReport
} from '@/types'

const router = useRouter()

const dimensionKeys: ValuationDimensionKey[] = ['technologicalInnovation', 'marketPotential', 'legalStatus', 'economicValue']

const activeStep = ref(0)
const searching = ref(false)
const generating = ref(false)
const viewportWidth = ref(window.innerWidth)

const datasets = ref<PatentDataset[]>([])
const searchResults = ref<PatentBase[]>([])
const selectedPatent = ref<PatentBase | null>(null)
const report = ref<ValuationReport | null>(null)

const searchForm = reactive({
  datasetId: undefined as number | undefined,
  query: ''
})

const weights = reactive<Record<ValuationDimensionKey, number>>({
  technologicalInnovation: 30,
  marketPotential: 25,
  legalStatus: 20,
  economicValue: 25
})

const isMobile = computed(() => viewportWidth.value <= 768)
const totalWeight = computed(() => dimensionKeys.reduce((sum, key) => sum + weights[key], 0))

const handleWindowResize = () => {
  viewportWidth.value = window.innerWidth
}

const loadDatasets = async () => {
  try {
    datasets.value = await patentApi.getPatentDatasets()
  } catch (error: any) {
    datasets.value = []
    ElMessage.error(error.message || '加载数据集失败')
  }
}

const searchDatasetPatents = async () => {
  if (!searchForm.datasetId) {
    ElMessage.warning('请先选择数据集')
    return
  }
  if (!searchForm.query.trim()) {
    ElMessage.warning('请输入关键词或公开号')
    return
  }

  searching.value = true
  try {
    const response = await patentApi.searchPatentDataset(searchForm.datasetId, {
      query: searchForm.query.trim(),
      page: 0,
      size: 20
    })
    const dataset = datasets.value.find(item => item.id === searchForm.datasetId)
    searchResults.value = (response.content || []).map(item => {
      const patent = normalizeDatasetPatent(item)
      return {
        ...patent,
        datasetId: patent.datasetId || searchForm.datasetId,
        datasetName: patent.datasetName || dataset?.name || '',
        category: normalizePatentCategory(patent.category || dataset?.category || '')
      }
    })
    if (!searchResults.value.length) {
      ElMessage.info('当前数据集下没有找到匹配专利')
    }
  } catch (error: any) {
    searchResults.value = []
    ElMessage.error(error.message || '搜索失败')
  } finally {
    searching.value = false
  }
}

const selectPatent = (patent: PatentBase) => {
  selectedPatent.value = { ...patent }
  ElMessage.success('已选中目标专利')
}

const generateEvaluation = async () => {
  if (!selectedPatent.value?.datasetId || !selectedPatent.value.recordId) {
    ElMessage.warning('请先选择数据集中的专利')
    return
  }

  generating.value = true
  try {
    const params: GenerateValuationParams = {
      patentSource: 'EXTERNAL',
      datasetId: selectedPatent.value.datasetId,
      recordId: selectedPatent.value.recordId,
      patentCategory: normalizePatentCategory(selectedPatent.value.category),
      patentPublicNum: selectedPatent.value.publicNum,
      modelVersion: 'v2.0',
      weights: { ...weights }
    }
    report.value = await valuationApi.generateValuationReport(params)
    ElMessage.success('评估完成')
    router.push({
      path: '/valuation',
      query: {
        datasetId: String(selectedPatent.value.datasetId),
        recordId: selectedPatent.value.recordId,
        patentPublicNum: selectedPatent.value.publicNum || '',
        patentCategory: normalizePatentCategory(selectedPatent.value.category)
      }
    })
  } catch (error: any) {
    ElMessage.error(error.message || '生成评估失败')
  } finally {
    generating.value = false
  }
}

onMounted(() => {
  window.addEventListener('resize', handleWindowResize)
  void loadDatasets()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleWindowResize)
})
</script>

<style scoped>
.evaluation-page {
  padding: 24px;
  display: grid;
  gap: 20px;
}

.panel {
  border-radius: 24px;
}

.step-section {
  margin-top: 24px;
  display: grid;
  gap: 20px;
}

.section-head h3 {
  margin: 0 0 8px;
}

.section-head p {
  margin: 0;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}

.desktop-table {
  display: block;
}

.mobile-result-list {
  display: none;
}

.search-form,
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.actions {
  justify-content: flex-end;
}

.selected-alert {
  margin-top: 8px;
}

.weight-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.weight-card,
.mini-card,
.summary-card {
  border-radius: 18px;
}

.weight-card {
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
}

.summary-head {
  justify-content: space-between;
  align-items: flex-start;
}

.amount-block {
  text-align: right;
}

.amount-block span {
  display: block;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.amount-block strong {
  display: block;
  font-size: 28px;
}

@media (min-width: 769px) {
  .evaluation-page {
    max-width: 1240px;
    margin: 0 auto;
    padding: 28px 32px 40px;
  }

  .panel,
  .mini-card,
  .summary-card {
    border-radius: 18px;
    box-shadow: 0 12px 28px rgba(16, 38, 63, 0.08);
  }

  .weight-card {
    border-radius: 16px;
    background: rgba(255, 255, 255, 0.86);
  }
}

@media (max-width: 900px) {
  .weight-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .evaluation-page {
    padding: 14px;
    gap: 14px;
  }

  .step-section {
    margin-top: 18px;
    gap: 16px;
  }

  .search-form,
  .actions {
    flex-direction: column;
    align-items: stretch;
  }

  .search-form :deep(.el-form-item),
  .search-form :deep(.el-select),
  .search-form :deep(.el-input) {
    width: 100% !important;
  }

  .actions :deep(.el-button) {
    width: 100%;
    margin: 0;
  }

  .desktop-table {
    display: none;
  }

  .mobile-result-list {
    display: grid;
    gap: 12px;
  }

  .mobile-result-card {
    padding: 16px;
    border-radius: 20px;
    border: 1px solid rgba(75, 122, 191, 0.16);
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 248, 255, 0.96));
    box-shadow: 0 12px 28px rgba(28, 52, 88, 0.08);
    display: grid;
    gap: 12px;
  }

  .mobile-result-head {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    align-items: flex-start;
  }

  .mobile-result-head strong {
    display: block;
    font-size: 15px;
    line-height: 1.6;
    word-break: break-word;
  }

  .mobile-result-head p {
    margin: 6px 0 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .mobile-result-head span {
    flex: 0 0 auto;
    color: #245dc7;
    font-size: 12px;
    font-weight: 600;
  }

  .mobile-result-meta {
    display: grid;
    gap: 6px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.5;
  }

  .mobile-result-actions :deep(.el-button) {
    width: 100%;
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

  .weight-grid {
    grid-template-columns: 1fr;
  }

  .summary-head {
    gap: 12px;
  }

  .amount-block {
    text-align: left;
  }

  .amount-block strong {
    font-size: 24px;
  }
}
</style>
