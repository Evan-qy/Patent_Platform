<template>
  <div class="page-container requirement-page">
    <div class="glass-header">
      <div class="header-copy">
        <h2 class="glass-page-title text-gradient-cyan">需求广场</h2>
        <p class="header-subtitle">公开浏览平台上的技术需求与合作机会，移动端优先呈现关键内容。</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" class="btn-primary" @click="goToDemandPublish">发布需求</el-button>
      </div>
    </div>

    <section class="hero-strip panel-glow">
      <div class="hero-main">
        <span class="hero-tag">公开合作需求</span>
        <h3>先浏览方向，再进入对接</h3>
        <p>支持按关键词、技术方向和合作方式筛选，帮助快速找到适合跟进的需求。</p>
      </div>
      <div class="hero-stats">
        <article>
          <strong>{{ total }}</strong>
          <span>公开需求</span>
        </article>
        <article>
          <strong>{{ TECH_DIRECTION_OPTIONS.length }}</strong>
          <span>技术方向</span>
        </article>
      </div>
    </section>

    <section class="search-panel panel-glow">
      <div class="search-panel-head">
        <div>
          <h3>筛选条件</h3>
          <p>按关键词、技术方向、合作方式和状态筛选需求。</p>
        </div>
      </div>

      <el-form :model="searchForm" class="search-form" @submit.prevent="searchRequirements">
        <el-form-item class="search-keyword">
          <el-input
            v-model="searchForm.query"
            class="glass-input"
            placeholder="搜索需求标题、关键词或描述"
            clearable
            @keyup.enter="searchRequirements"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <div class="filter-grid">
          <el-form-item>
            <el-select v-model="searchForm.techDirection" class="glass-input" placeholder="技术方向" clearable>
              <el-option v-for="item in TECH_DIRECTION_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-select v-model="searchForm.cooperationMode" class="glass-input" placeholder="鍚堜綔鏂瑰紡" clearable>
              <el-option v-for="item in COOPERATION_MODE_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-select v-model="searchForm.status" class="glass-input" placeholder="状态" clearable>
              <el-option label="待处理" value="PENDING" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="已失败" value="FAILED" />
            </el-select>
          </el-form-item>
        </div>

        <div class="search-actions">
          <el-button type="primary" class="btn-primary" @click="searchRequirements">
            <el-icon><Search /></el-icon>
              开始筛选
          </el-button>
          <el-button class="btn-secondary" @click="resetSearch">清空条件</el-button>
        </div>
      </el-form>
    </section>

    <section class="result-panel panel-glow">
      <div class="result-head">
        <div>
          <h3>需求列表</h3>
          <p>{{ totalText }}</p>
        </div>
      </div>

      <div v-if="!isMobile" class="table-shell">
        <el-table
          :data="displayRequirementList"
          v-loading="loading"
          class="glass-table"
          style="width: 100%"
          :header-cell-style="{ background: 'var(--bg-card-header)', color: 'var(--brand-cyan)', borderBottom: '1px solid var(--border-color)' }"
          :row-style="{ background: 'transparent', color: 'var(--text-primary)' }"
        >
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">
              <button type="button" class="table-link" @click="viewRequirement(row)">{{ row.title }}</button>
            </template>
          </el-table-column>
          <el-table-column prop="techDirection" label="技术方向" width="200" show-overflow-tooltip />
          <el-table-column prop="cooperationMode" label="合作方式" width="140" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="getRequirementStatusTagType(row.status)" effect="light" round>
                {{ getRequirementStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdDate" label="发布时间" width="150" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="viewRequirement(row)">查看详情</el-button>
                <el-button link type="success" @click="startMatching(row)">智能匹配</el-button>
              </div>
            </template>
          </el-table-column>
          <template #empty>
            <div class="empty-state">
              <el-icon><FolderOpened /></el-icon>
              <h4>当前没有匹配的公开需求</h4>
              <p>可以调整关键词或直接发布新需求，平台会继续协助匹配。</p>
              <el-button type="primary" class="btn-primary" @click="goToDemandPublish">去发布需求</el-button>
            </div>
          </template>
        </el-table>
      </div>

      <div v-else class="mobile-list" v-loading="loading">
        <article v-for="item in displayRequirementList" :key="item.id" class="requirement-card glass-effect">
          <div class="requirement-card-head">
            <span class="requirement-id">#{{ item.id }}</span>
            <el-tag :type="getRequirementStatusTagType(item.status)" effect="light" round>
              {{ getRequirementStatusText(item.status) }}
            </el-tag>
          </div>
          <h3>{{ item.title }}</h3>
          <p class="requirement-desc">{{ item.description || '暂无补充描述，点击后查看完整内容。' }}</p>
          <div class="chip-row">
            <span v-if="item.techDirection" class="chip">{{ item.techDirection }}</span>
            <span v-if="item.cooperationMode" class="chip">{{ item.cooperationMode }}</span>
            <span class="chip chip-muted">{{ item.createdDate }}</span>
          </div>
          <div class="card-actions">
            <el-button type="primary" class="btn-primary is-soft" @click="viewRequirement(item)">查看详情</el-button>
            <el-button class="btn-secondary" @click="startMatching(item)">智能匹配</el-button>
          </div>
        </article>

        <div v-if="!displayRequirementList.length && !loading" class="empty-state">
          <el-icon><FolderOpened /></el-icon>
          <h4>当前没有匹配的公开需求</h4>
          <p>可以调整关键词或直接发布新需求，平台会继续协助匹配。</p>
          <el-button type="primary" class="btn-primary" @click="goToDemandPublish">去发布需求</el-button>
        </div>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          :layout="isMobile ? 'prev, pager, next' : 'total, sizes, prev, pager, next, jumper'"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </section>

    <el-dialog
      v-model="detailDialogVisible"
      :title="dialogTitle"
      :width="isMobile ? '94%' : '62%'"
      class="glass-dialog"
      :before-close="closeDetailDialog"
    >
      <el-descriptions :column="isMobile ? 1 : 2" border class="glass-descriptions">
        <el-descriptions-item label="ID">{{ currentRequirement.id }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ currentRequirement.title }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRequirement.techDirection" label="技术方向">{{ currentRequirement.techDirection }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRequirement.cooperationMode" label="合作方式">{{ currentRequirement.cooperationMode }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRequirement.contactInfo" label="联系电话">{{ currentRequirement.contactInfo }}</el-descriptions-item>
        <el-descriptions-item v-if="hasRequirementBudget(currentRequirement)" label="预算">{{ formatRequirementBudget(currentRequirement.budget) }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRequirement.deadline" label="截止日期">{{ currentRequirement.deadline }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRequirement.keywords" label="关键词" :span="isMobile ? 1 : 2">{{ currentRequirement.keywords }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRequirement.description" label="详情" :span="isMobile ? 1 : 2">{{ currentRequirement.description }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getRequirementStatusTagType(currentRequirement.status)" effect="light" round>
            {{ getRequirementStatusText(currentRequirement.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ currentRequirement.createdDate }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog
      v-model="matchingDialogVisible"
      :title="`闇€姹傚尮閰嶇粨鏋?- ${currentRequirement.title || ''}`"
      :width="isMobile ? '96%' : '82%'"
      class="glass-dialog"
      :before-close="closeMatchingDialog"
    >
      <el-tabs v-model="activeTab" class="glass-tabs">
        <el-tab-pane label="匹配专利" name="patents">
          <div class="match-content">
            <div v-if="!isMobile" class="table-shell">
              <el-table :data="matchedPatents" v-loading="patentLoading" class="glass-table" style="width: 100%">
                <el-table-column label="专利类别" width="120">
                  <template #default="{ row }">
                    <el-tag :type="getPatentCategoryTagType(row.category)" effect="light" round>
                      {{ getPatentCategoryText(row.category) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="publicNum" label="公开号" width="160" />
                <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
                <el-table-column prop="applicant" label="申请人" width="180" show-overflow-tooltip />
                <el-table-column prop="inventor" label="发明人" width="150" show-overflow-tooltip />
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="viewPatentDetail(row)">查看</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div v-else class="match-mobile-list" v-loading="patentLoading">
              <article v-for="row in matchedPatents" :key="`${row.category}-${row.publicNum}`" class="match-card">
                <div class="chip-row">
                  <span class="chip">{{ getPatentCategoryText(row.category) }}</span>
                  <span class="chip chip-muted">{{ row.publicNum }}</span>
                </div>
                <h4>{{ row.title }}</h4>
                <p>{{ row.applicant || '未知申请人' }}</p>
                <el-button type="primary" class="btn-primary is-soft" @click="viewPatentDetail(row)">查看详情</el-button>
              </article>
            </div>
            <div class="match-action-row">
              <el-button type="primary" class="btn-primary" @click="persistPatentMatches">保存专利匹配结果</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="匹配专家" name="experts">
          <div class="match-content">
            <div v-if="!isMobile" class="table-shell">
              <el-table :data="matchedExperts" v-loading="expertLoading" class="glass-table" style="width: 100%">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="name" label="姓名" width="120" />
                <el-table-column prop="field" label="领域" min-width="220" show-overflow-tooltip />
                <el-table-column label="操作" width="120">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="viewExpertDetail(row)">查看说明</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div v-else class="match-mobile-list" v-loading="expertLoading">
              <article v-for="row in matchedExperts" :key="row.id" class="match-card">
                <div class="chip-row">
                  <span class="chip">专家 #{{ row.id }}</span>
                </div>
                <h4>{{ row.name }}</h4>
                <p>{{ row.field || '未标注领域' }}</p>
                <el-button class="btn-secondary" @click="viewExpertDetail(row)">查看说明</el-button>
              </article>
            </div>
            <div class="match-action-row">
              <el-button type="primary" class="btn-primary" @click="persistExpertMatches">保存专家匹配结果</el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog
      v-model="patentDetailDialogVisible"
      title="专利详情"
      :width="isMobile ? '96%' : '72%'"
      class="glass-dialog"
      :before-close="closePatentDetailDialog"
    >
      <el-descriptions v-loading="patentDetailLoading" :column="isMobile ? 1 : 2" border class="glass-descriptions">
        <el-descriptions-item label="公开号">{{ currentPatent.publicNum }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ currentPatent.title }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentPatent.applicant || '未提供' }}</el-descriptions-item>
        <el-descriptions-item label="发明人">{{ currentPatent.inventor || '未提供' }}</el-descriptions-item>
        <el-descriptions-item label="IPC 分类">{{ currentPatent.ipc || '未提供' }}</el-descriptions-item>
        <el-descriptions-item label="法律状态">{{ currentPatent.legalStatus || '未提供' }}</el-descriptions-item>
        <el-descriptions-item v-if="currentPatent.abstractText" label="摘要" :span="isMobile ? 1 : 2">
          <div class="details-text">{{ currentPatent.abstractText }}</div>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentPatent.patentDetails" label="详情" :span="isMobile ? 1 : 2">
          <div class="details-text">{{ currentPatent.patentDetails }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { FolderOpened, Plus, Search } from '@element-plus/icons-vue'
import { patentApi, requirementApi } from '@/api'
import {
  COOPERATION_MODE_OPTIONS,
  TECH_DIRECTION_OPTIONS,
  getPatentCategoryTagType,
  getPatentCategoryText,
  getRequirementStatusTagType,
  getRequirementStatusText,
  normalizeAiPatent
} from '@/constants/domain'
import type { MatchedExpert, MatchedPatent, PatentBase, Requirement, RequirementStatus } from '@/types'

type PatentMatchRow = MatchedPatent & Partial<PatentBase>

const router = useRouter()
const isMobile = ref(window.innerWidth <= 768)

const searchForm = reactive({
  query: '',
  techDirection: '',
  cooperationMode: '',
  status: '' as RequirementStatus | ''
})

const requirementList = ref<Requirement[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailDialogVisible = ref(false)
const dialogTitle = ref('')
const currentRequirement = ref<Requirement>({} as Requirement)
const matchingDialogVisible = ref(false)
const activeTab = ref('patents')
const matchedPatents = ref<PatentMatchRow[]>([])
const matchedExperts = ref<MatchedExpert[]>([])
const patentLoading = ref(false)
const expertLoading = ref(false)
const patentDetailDialogVisible = ref(false)
const patentDetailLoading = ref(false)
const currentPatent = ref<PatentBase>({} as PatentBase)

const displayRequirementList = computed(() => {
  const query = searchForm.query.trim().toLowerCase()
  return requirementList.value.filter(item => {
    const queryMatch =
      !query ||
      (item.title || '').toLowerCase().includes(query) ||
      (item.description || '').toLowerCase().includes(query) ||
      (item.keywords || '').toLowerCase().includes(query)
    const techMatch = !searchForm.techDirection || item.techDirection === searchForm.techDirection
    const coopMatch = !searchForm.cooperationMode || item.cooperationMode === searchForm.cooperationMode
    const statusMatch = !searchForm.status || item.status === searchForm.status
    return queryMatch && techMatch && coopMatch && statusMatch
  })
})

const totalText = computed(() => {
  if (!displayRequirementList.value.length) return '当前没有符合条件的需求'
  return `已筛出 ${displayRequirementList.value.length} 条需求，可继续查看详情或做智能匹配`
})

const hasRequirementBudget = (requirement: Requirement) => {
  const budget = requirement.budget
  return budget !== undefined && budget !== null && String(budget).trim() !== ''
}

const formatRequirementBudget = (budget?: number | string) => {
  if (budget === undefined || budget === null || String(budget).trim() === '') {
    return '未填写'
  }
  const amount = Number(budget)
  if (!Number.isFinite(amount)) {
    return String(budget)
  }
  return `${amount.toLocaleString('zh-CN', { maximumFractionDigits: 2 })} 万元`
}

const updateViewport = () => {
  isMobile.value = window.innerWidth <= 768
}

const goToDemandPublish = () => {
  const hasToken = Boolean(localStorage.getItem('token'))
  if (!hasToken) {
    ElMessage.warning('鍙戝竷闇€姹傚墠璇峰厛鐧诲綍')
    router.push('/login?redirect=/demand')
    return
  }
  router.push('/demand')
}

const searchRequirements = async () => {
  loading.value = true
  try {
    const response = await requirementApi.getRequirements({
      category: searchForm.techDirection || undefined,
      query: searchForm.query || undefined,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    requirementList.value = (response?.content || []).sort((a, b) => Number(a.id) - Number(b.id))
    total.value = response?.totalElements || 0
  } catch (error: any) {
    ElMessage.error(error?.message || '获取需求列表失败')
    requirementList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.query = ''
  searchForm.techDirection = ''
  searchForm.cooperationMode = ''
  searchForm.status = ''
  currentPage.value = 1
  void searchRequirements()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  void searchRequirements()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  void searchRequirements()
}

const viewRequirement = (row: Requirement) => {
  currentRequirement.value = row
  dialogTitle.value = '需求详情'
  detailDialogVisible.value = true
}

const startMatching = async (row: Requirement) => {
  currentRequirement.value = row
  activeTab.value = 'patents'
  matchingDialogVisible.value = true
  await loadMatchResults(row)
}

const loadMatchResults = async (requirement: Requirement) => {
  patentLoading.value = true
  matchedPatents.value = []
  try {
    const response = await requirementApi.aiMatchPatents({
      requirement: [requirement.title, requirement.description].filter(Boolean).join(' '),
      sessionId: `requirement_${requirement.id}`
    })
    matchedPatents.value = response.patents.map(item => {
      const patent = normalizeAiPatent(item)
      return {
        datasetId: patent.datasetId,
        datasetCode: patent.datasetCode,
        datasetName: patent.datasetName,
        tableName: patent.tableName,
        recordId: patent.recordId,
        category: patent.category,
        publicNum: patent.publicNum,
        title: patent.title,
        applicant: patent.applicant || '',
        inventor: patent.inventor || '',
        abstractText: patent.abstractText,
        ipc: patent.ipc,
        legalStatus: patent.legalStatus
      }
    })
  } catch (error: any) {
    ElMessage.error(error?.message || '鑾峰彇涓撳埄鍖归厤缁撴灉澶辫触')
  } finally {
    patentLoading.value = false
  }

  expertLoading.value = true
  try {
    matchedExperts.value = await requirementApi.matchExpertsForRequirement(requirement.id)
  } catch (error: any) {
    ElMessage.error(error?.message || '鑾峰彇涓撳鍖归厤缁撴灉澶辫触')
  } finally {
    expertLoading.value = false
  }
}

const viewPatentDetail = async (row: PatentMatchRow) => {
  patentDetailLoading.value = true
  patentDetailDialogVisible.value = true
  currentPatent.value = {
    publicNum: row.publicNum,
    title: row.title,
    abstractText: row.abstractText || '',
    applicant: row.applicant || '',
    inventor: row.inventor || '',
    ipc: row.ipc || '',
    legalStatus: row.legalStatus || ''
  } as PatentBase

  try {
    if (row.datasetId && row.recordId) {
      currentPatent.value = await patentApi.getPatentDetail(row.datasetId, row.recordId)
    } else {
      currentPatent.value = await patentApi.getPatent(row.category, row.publicNum)
    }
  } catch {
    currentPatent.value = {
      ...currentPatent.value,
      patentDetails: row.abstractText || ''
    } as PatentBase
  } finally {
    patentDetailLoading.value = false
  }
}

const viewExpertDetail = (row: MatchedExpert) => {
  ElMessage.info(`专家 ${row.name} 当前仅提供基础信息展示，后续可补充详情页。`)
}

const persistPatentMatches = async () => {
  try {
    const items = matchedPatents.value.map(patent => ({
      patentCategory: patent.category,
      patentPublicNum: patent.publicNum,
      datasetId: patent.datasetId,
      recordId: patent.recordId
    }))
    await requirementApi.persistPatentMatches(currentRequirement.value.id, { items })
    ElMessage.success('专利匹配结果已保存')
  } catch (error: any) {
    ElMessage.error(error?.message || '淇濆瓨涓撳埄鍖归厤缁撴灉澶辫触')
  }
}

const persistExpertMatches = async () => {
  try {
    const items = matchedExperts.value.map(expert => ({ expertId: expert.id }))
    await requirementApi.persistExpertMatches(currentRequirement.value.id, { items })
    ElMessage.success('专家匹配结果已保存')
  } catch (error: any) {
    ElMessage.error(error?.message || '淇濆瓨涓撳鍖归厤缁撴灉澶辫触')
  }
}

const closeDetailDialog = () => {
  detailDialogVisible.value = false
  currentRequirement.value = {} as Requirement
}

const closeMatchingDialog = () => {
  matchingDialogVisible.value = false
  matchedPatents.value = []
  matchedExperts.value = []
}

const closePatentDetailDialog = () => {
  patentDetailDialogVisible.value = false
  currentPatent.value = {} as PatentBase
}

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
  void searchRequirements()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateViewport)
})
</script>

<style scoped>
.requirement-page {
  --req-panel-bg: linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(243, 248, 255, 0.94));
  --req-panel-border: rgba(61, 125, 255, 0.14);
  --req-title: #12355c;
  --req-subtitle: #5f7898;
  --req-chip-bg: rgba(61, 125, 255, 0.08);
  --req-chip-text: #34587f;
}

.header-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.header-subtitle {
  margin: 0;
  color: var(--req-subtitle);
  font-size: 13px;
  line-height: 1.7;
}

.hero-strip,
.search-panel,
.result-panel {
  padding: 24px;
  border-radius: 18px;
  background: var(--req-panel-bg) !important;
  border: 1px solid var(--req-panel-border) !important;
}

.hero-strip {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(240px, 0.8fr);
  gap: 18px;
  margin-bottom: 20px;
}

.hero-tag {
  display: inline-flex;
  width: fit-content;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(61, 125, 255, 0.1);
  color: #2f66b3;
  font-size: 12px;
  font-weight: 700;
}

.hero-main h3,
.search-panel-head h3,
.result-head h3 {
  margin: 12px 0 8px;
  color: var(--req-title);
}

.hero-main p,
.search-panel-head p,
.result-head p {
  margin: 0;
  color: var(--req-subtitle);
  line-height: 1.7;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.hero-stats article,
.match-card,
.requirement-card {
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(61, 125, 255, 0.12);
}

.hero-stats strong {
  display: block;
  font-size: 28px;
  color: var(--req-title);
}

.hero-stats span {
  color: var(--req-subtitle);
  font-size: 13px;
}

.search-panel-head {
  margin-bottom: 16px;
}

.search-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.search-keyword {
  margin-bottom: 0;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.search-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.result-head {
  margin-bottom: 18px;
}

.table-link {
  border: none;
  background: transparent;
  padding: 0;
  color: #2f7bf6;
  cursor: pointer;
  font-weight: 600;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.mobile-list,
.match-mobile-list {
  display: grid;
  gap: 14px;
}

.requirement-card-head,
.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.requirement-id {
  color: #2f66b3;
  font-weight: 700;
}

.requirement-card h3,
.match-card h4 {
  margin: 12px 0 8px;
  color: var(--req-title);
  line-height: 1.55;
}

.requirement-desc,
.match-card p {
  margin: 0;
  color: var(--req-subtitle);
  line-height: 1.7;
}

.chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--req-chip-bg);
  color: var(--req-chip-text);
  font-size: 12px;
  font-weight: 600;
}

.chip-muted {
  background: rgba(17, 48, 83, 0.06);
}

.card-actions,
.match-action-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.empty-state {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 10px;
  color: var(--req-subtitle);
}

.empty-state :deep(.el-icon) {
  font-size: 28px;
  color: #4776c3;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.details-text {
  white-space: pre-wrap;
  line-height: 1.75;
}

@media (max-width: 900px) {
  .hero-strip {
    grid-template-columns: 1fr;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-strip,
  .search-panel,
  .result-panel {
    padding: 18px;
    border-radius: 18px;
  }

  .hero-stats,
  .search-actions,
  .card-actions,
  .match-action-row {
    grid-template-columns: 1fr;
  }

  .search-actions,
  .card-actions,
  .match-action-row {
    display: grid;
  }

  .search-actions :deep(.el-button),
  .card-actions :deep(.el-button),
  .match-action-row :deep(.el-button),
  .header-actions :deep(.el-button) {
    width: 100%;
    margin: 0;
  }

  .pagination-wrapper {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .hero-strip,
  .search-panel,
  .result-panel {
    padding: 16px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }
}
</style>
