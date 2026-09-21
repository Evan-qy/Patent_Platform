<template>
  <div class="page-container patent-search-page">
    <div class="glass-header">
      <div class="header-left">
        <h2 class="glass-page-title text-gradient-cyan">专利检索</h2>
        <p class="header-subtitle">数据库检索与 AI 智能搜索同时展示，统一在一个结果框中查看命中结果。</p>
      </div>
      <div class="header-actions">
        <el-button
          type="primary"
          round
          class="btn-primary"
          :class="{ active: isAIChatVisible }"
          @click="toggleAIChat"
        >
          <el-icon style="margin-right: 4px"><ChatDotRound /></el-icon>
          {{ aiToggleLabel }}
        </el-button>
      </div>
    </div>

    <div class="main-content" :class="{ 'with-chat': isDesktopAIChatVisible }">
      <div class="content-column" :style="{ width: isDesktopAIChatVisible ? `calc(100% - ${aiChatWidth}px - 24px)` : '100%' }">
        <section class="search-focus panel-glow" aria-label="检索模式切换">
          <div class="search-focus-head">
            <div class="mode-tabs" role="tablist" aria-label="移动端检索模式">
              <button type="button" class="mode-tab" role="tab" :aria-selected="mobileSearchMode === 'es'" :class="{ active: mobileSearchMode === 'es' }" data-testid="mode-es" @click="mobileSearchMode = 'es'">数据库检索</button>
              <button type="button" class="mode-tab" role="tab" :aria-selected="mobileSearchMode === 'ai'" :class="{ active: mobileSearchMode === 'ai' }" data-testid="mode-ai" @click="mobileSearchMode = 'ai'">AI 智能搜索</button>
            </div>
          </div>
        </section>

        <section class="search-grid">
          <article class="search-card panel-glow" :class="{ collapsed: isMobile && mobileSearchMode !== 'es' }">
            <div class="card-head">
              <div>
                <h3>数据库检索</h3>
                <p>直接从 ES 索引的专利数据库中按关键词、技术主题、应用场景或公开号检索。</p>
              </div>
              <el-tag type="primary" effect="plain">ES</el-tag>
            </div>

            <div class="search-body">
              <el-input
                v-model="searchQuery"
                class="glass-input primary-search-input"
                maxlength="120"
                show-word-limit
                placeholder="输入专利关键词、技术主题、应用场景或公开号"
                aria-label="数据库检索关键词"
                data-testid="es-search-input"
                @keyup.enter="searchPatentsByES"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <div class="search-note">适合已知方向、公开号或希望直接浏览数据库结果时使用。</div>
              <div class="search-actions">
                <el-button type="primary" class="btn-primary" :loading="esLoading" :disabled="!canSearchES" data-testid="es-search-submit" @click="searchPatentsByES">开始数据库检索</el-button>
                <el-button class="btn-secondary" data-testid="es-search-reset" @click="resetES">清空</el-button>
              </div>
            </div>
          </article>

          <article class="search-card panel-glow" :class="{ collapsed: isMobile && mobileSearchMode !== 'ai' }">
            <div class="card-head">
              <div>
                <h3>AI 智能搜索</h3>
                <p>先理解需求，再回查命中的专利结果，更适合场景描述式搜索。</p>
              </div>
              <el-tag type="success" effect="plain">AI</el-tag>
            </div>

            <div class="search-body">
              <el-input
                v-model="aiRequirement"
                type="textarea"
                :rows="5"
                resize="none"
                maxlength="500"
                show-word-limit
                class="glass-input ai-search-input"
                placeholder="输入技术需求、应用场景、目标对象或想解决的问题"
                aria-label="AI 智能搜索需求描述"
                data-testid="ai-search-input"
                @keyup.ctrl.enter="searchPatentsByAI"
              />
              <div class="search-note">适合不知道精确关键词，但能描述业务目标、技术问题或应用场景时使用。</div>
              <div class="search-actions">
                <el-button type="primary" class="btn-primary" :loading="aiLoading" :disabled="!canSearchAI" data-testid="ai-search-submit" @click="searchPatentsByAI">
                  <el-icon style="margin-right: 4px"><MagicStick /></el-icon>
                  开始 AI 智能搜索
                </el-button>
                <el-button class="btn-secondary" data-testid="ai-search-reset" @click="resetAI">清空</el-button>
              </div>
            </div>
          </article>
        </section>

        <section class="result-card panel-glow mt-lg">
          <div class="sr-only" role="status" aria-live="polite" aria-atomic="true" data-testid="search-status">{{ searchStatusText }}</div>
          <div class="section-header">
            <div>
              <h3>检索结果</h3>
              <p>数据库检索结果与 AI 智能搜索结果统一汇总在这里，便于连续比对与查看详情。</p>
            </div>
          </div>

          <div class="result-switcher" role="tablist" aria-label="检索结果类型">
            <button type="button" class="result-switch" role="tab" :aria-selected="activeResultTab === 'es'" :class="{ active: activeResultTab === 'es' }" data-testid="result-tab-es" @click="activeResultTab = 'es'">
              <span>数据库检索</span>
              <strong>{{ total }}</strong>
            </button>
            <button type="button" class="result-switch" role="tab" :aria-selected="activeResultTab === 'ai'" :class="{ active: activeResultTab === 'ai' }" data-testid="result-tab-ai" @click="activeResultTab = 'ai'">
              <span>AI 智能搜索</span>
              <strong>{{ aiResult?.patents.length ?? 0 }}</strong>
            </button>
          </div>

          <div v-show="activeResultTab === 'es'" class="result-section" role="tabpanel" data-testid="es-results-panel">
            <div class="subsection-header unified-subsection-header">
              <div>
                <h4>数据库检索结果</h4>
                <p>当前显示 ES 检索命中的数据库结果，支持分页浏览。</p>
              </div>
              <el-tag type="primary" effect="plain">共 {{ total }} 条</el-tag>
            </div>

            <div v-if="!isMobile" class="patent-table-wrapper">
              <el-table
                :data="esResults"
                v-loading="esLoading"
                class="glass-table"
                style="width: 100%"
                :header-cell-style="{ background: 'var(--bg-card-header)', color: 'var(--brand-cyan)', borderBottom: '1px solid var(--border-color)' }"
                :row-style="{ background: 'transparent', color: 'var(--text-primary)' }"
              >
                <el-table-column prop="publicNum" label="公开号" min-width="160" />
                <el-table-column prop="title" label="标题" min-width="320" show-overflow-tooltip>
                  <template #default="{ row }">
                    <button type="button" class="patent-title-link" data-testid="patent-title-link" @click="viewPatent(row)">{{ row.title || '未提供标题' }}</button>
                  </template>
                </el-table-column>
                <el-table-column prop="datasetName" label="数据集" width="180" show-overflow-tooltip />
                <el-table-column prop="applicant" label="申请人" width="180" show-overflow-tooltip />
                <el-table-column prop="inventor" label="发明人" width="160" show-overflow-tooltip />
                <el-table-column prop="appliDate" label="申请日" width="120" show-overflow-tooltip />
                <el-table-column prop="legalStatus" label="法律状态" width="150" show-overflow-tooltip />
                <el-table-column label="操作" width="100" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="viewPatent(row)">
                      <el-icon style="margin-right: 2px"><View /></el-icon>
                      查看
                    </el-button>
                  </template>
                </el-table-column>
                <template #empty>
                  <div class="empty-state">
                    <el-icon><FolderOpened /></el-icon>
                    <h4>暂无数据库检索结果</h4>
                    <p>可以尝试更换关键词、技术方向或直接输入公开号。</p>
                  </div>
                </template>
              </el-table>
            </div>

            <div v-else class="mobile-patent-list" v-loading="esLoading">
              <div v-if="esResults.length" class="patent-cards">
                <button v-for="patent in esResults" :key="getPatentKey(patent)" type="button" class="patent-card glass-effect" data-testid="patent-title-link" @click="viewPatent(patent)">
                  <div class="card-header">
                    <span class="card-public-num">{{ patent.publicNum || '未提供公开号' }}</span>
                    <span class="card-dataset">{{ patent.datasetName || '未命名数据集' }}</span>
                  </div>
                  <h3 class="card-title">{{ patent.title || '未提供标题' }}</h3>
                  <div class="card-info">
                    <div class="info-item">
                      <el-icon><User /></el-icon>
                      <span>{{ patent.applicant || '未知申请人' }}</span>
                    </div>
                    <div class="card-meta-line">
                      <span>{{ patent.inventor || '未提供发明人' }}</span>
                      <span>{{ patent.appliDate || patent.publicDate || '未提供日期' }}</span>
                    </div>
                  </div>
                </button>
              </div>
              <div v-else class="empty-state">
                <el-icon><FolderOpened /></el-icon>
                <h4>暂无数据库检索结果</h4>
                <p>可以尝试更换关键词、技术方向或直接输入公开号。</p>
              </div>
            </div>

            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                background
                :layout="isMobile ? 'total, prev, pager, next' : 'total, sizes, prev, pager, next, jumper'"
                :total="total"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
              />
            </div>
          </div>

          <el-divider v-show="false" />

          <div v-show="activeResultTab === 'ai'" class="result-section" role="tabpanel" data-testid="ai-results-panel">
            <div class="subsection-header unified-subsection-header">
              <div>
                <h4>AI 智能搜索结果</h4>
                <p>先展示 AI 的理解与提取结果，再展示实际命中的专利列表。</p>
              </div>
              <el-tag type="success" effect="plain">命中 {{ aiResult?.patents.length ?? 0 }} 条</el-tag>
            </div>

            <div v-if="aiResult" class="ai-analysis">
              <p class="analysis-text">{{ aiResult.aiAnalysis || 'AI 未返回额外分析文本。' }}</p>

              <div v-if="aiResult.extractedPublicNums.length" class="analysis-block">
                <h4>AI 提取出的公开号</h4>
                <div class="chip-list">
                  <el-tag v-for="publicNum in aiResult.extractedPublicNums" :key="publicNum" effect="plain">{{ publicNum }}</el-tag>
                </div>
              </div>

              <div v-if="aiResult.notFoundPublicNums.length" class="analysis-block">
                <h4>未在索引中命中的公开号</h4>
                <div class="chip-list">
                  <el-tag v-for="publicNum in aiResult.notFoundPublicNums" :key="publicNum" type="warning" effect="plain">{{ publicNum }}</el-tag>
                </div>
              </div>
            </div>

            <div v-if="!isMobile" class="patent-table-wrapper">
              <el-table
                :data="aiResult?.patents || []"
                v-loading="aiLoading"
                class="glass-table"
                style="width: 100%"
                :header-cell-style="{ background: 'var(--bg-card-header)', color: 'var(--brand-cyan)', borderBottom: '1px solid var(--border-color)' }"
                :row-style="{ background: 'transparent', color: 'var(--text-primary)' }"
              >
                <el-table-column prop="publicNum" label="公开号" min-width="160" />
                <el-table-column prop="title" label="标题" min-width="320" show-overflow-tooltip>
                  <template #default="{ row }">
                    <button type="button" class="patent-title-link" data-testid="ai-patent-title-link" @click="viewPatent(row)">{{ row.title || '未提供标题' }}</button>
                  </template>
                </el-table-column>
                <el-table-column prop="datasetName" label="数据集" width="180" show-overflow-tooltip />
                <el-table-column prop="applicant" label="申请人" width="180" show-overflow-tooltip />
                <el-table-column prop="inventor" label="发明人" width="160" show-overflow-tooltip />
                <el-table-column prop="legalStatus" label="法律状态" width="150" show-overflow-tooltip />
                <el-table-column label="操作" width="100" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="viewPatent(row)">查看</el-button>
                  </template>
                </el-table-column>
                <template #empty>
                  <div class="empty-state">
                    <el-icon><FolderOpened /></el-icon>
                    <h4>暂无 AI 命中结果</h4>
                    <p>可以把需求描述得更具体一些，例如目标场景、关键部件或约束条件。</p>
                  </div>
                </template>
              </el-table>
            </div>

            <div v-else class="mobile-patent-list" v-loading="aiLoading">
              <div v-if="(aiResult?.patents || []).length" class="patent-cards">
                <button v-for="patent in aiResult?.patents || []" :key="getPatentKey(patent)" type="button" class="patent-card glass-effect" data-testid="ai-patent-title-link" @click="viewPatent(patent)">
                  <div class="card-header">
                    <span class="card-public-num">{{ patent.publicNum || '未提供公开号' }}</span>
                    <span class="card-dataset">{{ patent.datasetName || '未命名数据集' }}</span>
                  </div>
                  <h3 class="card-title">{{ patent.title || '未提供标题' }}</h3>
                  <div class="card-info">
                    <div class="info-item">
                      <el-icon><User /></el-icon>
                      <span>{{ patent.applicant || '未知申请人' }}</span>
                    </div>
                    <div class="card-meta-line">
                      <span>{{ patent.inventor || '未提供发明人' }}</span>
                      <span>{{ patent.legalStatus || patent.latestLegalStatus || '未提供法律状态' }}</span>
                    </div>
                  </div>
                </button>
              </div>
              <div v-else class="empty-state">
                <el-icon><FolderOpened /></el-icon>
                <h4>暂无 AI 命中结果</h4>
                <p>可以把需求描述得更具体一些，例如目标场景、关键部件或约束条件。</p>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div v-if="isDesktopAIChatVisible" class="ai-chat-section glass-effect" :style="{ width: `${aiChatWidth}px`, height: `${aiChatHeight}px` }">
        <div class="resize-handle" @mousedown="startResize($event, 'width')"></div>
        <div class="corner-resize-handle" @mousedown="startResize($event, 'both')"></div>
        <AIChat />
      </div>
    </div>

    <transition name="mobile-chat-fade">
      <div v-if="isMobileAIChatVisible" class="mobile-ai-chat-shell">
        <div class="mobile-ai-chat-mask" @click="closeAIChat"></div>
        <div class="mobile-ai-chat-panel glass-effect">
          <div class="mobile-ai-chat-header">
            <div>
              <strong>AI 咨询</strong>
              <p>移动端以抽屉形式打开，按钮状态与面板显示保持一致。</p>
            </div>
            <el-button text type="primary" @click="closeAIChat">关闭</el-button>
          </div>
          <AIChat />
        </div>
      </div>
    </transition>

    <el-dialog v-model="detailDialogVisible" width="78%" :title="dialogTitle" :before-close="closeDetailDialog" class="glass-dialog patent-detail-dialog">
      <div class="patent-detail-shell">
        <section class="patent-hero">
          <div class="patent-hero-main">
            <p class="patent-public-num">{{ currentPatent.publicNum || '未提供公开号' }}</p>
            <h2>{{ currentPatent.title || '未提供标题' }}</h2>
            <p class="patent-meta-line">
              {{ currentPatent.datasetName || '未提供数据集' }}
              <span v-if="currentPatent.category"> · {{ currentPatent.category }}</span>
            </p>
          </div>
          <div v-if="patentStatusChips.length" class="patent-status-chips">
            <span v-for="chip in patentStatusChips" :key="chip" class="status-chip">{{ chip }}</span>
          </div>
        </section>

        <section class="detail-section">
          <div class="detail-section-head">
            <h3>核心信息</h3>
            <p>按阅读优先级重组字段，减少大表格带来的扫读成本。</p>
          </div>
          <div class="detail-grid">
            <article v-for="item in overviewItems" :key="item.label" class="detail-card">
              <span class="detail-label">{{ item.label }}</span>
              <div class="detail-value" :class="{ empty: !item.value }">{{ item.value || '未提供' }}</div>
            </article>
          </div>
        </section>

        <section v-if="classificationGroups.length" class="detail-section">
          <div class="detail-section-head">
            <h3>分类与标识</h3>
            <p>将 IPC、CPC、NEC 拆分为标签，便于快速识别技术归属。</p>
          </div>
          <div class="classification-stack">
            <div v-for="group in classificationGroups" :key="group.label" class="classification-row">
              <span class="classification-label">{{ group.label }}</span>
              <div class="classification-tags">
                <span v-for="value in group.values" :key="`${group.label}-${value}`" class="classification-tag">{{ value }}</span>
              </div>
            </div>
          </div>
        </section>

        <section v-if="currentPatent.abstractText" class="detail-section">
          <div class="detail-section-head">
            <h3>摘要</h3>
            <p>保留原文段落结构，并增强文字与背景对比度。</p>
          </div>
          <div class="rich-text-block">{{ currentPatent.abstractText }}</div>
        </section>

        <section v-if="legalStatusTimeline.length" class="detail-section">
          <div class="detail-section-head">
            <h3>法律状态时间线</h3>
            <p>从原始法律状态公告文本中提取关键节点。</p>
          </div>
          <div class="timeline-list">
            <article v-for="item in legalStatusTimeline" :key="`${item.date}-${item.status}-${item.description}`" class="timeline-item">
              <div class="timeline-date">{{ item.date || '未标注日期' }}</div>
              <div class="timeline-content">
                <h4>{{ item.status || '状态更新' }}</h4>
                <p>{{ item.description || '未提供补充说明' }}</p>
              </div>
            </article>
          </div>
        </section>
        <section v-else-if="currentPatent.legalStatus" class="detail-section">
          <div class="detail-section-head">
            <h3>法律状态</h3>
          </div>
          <div class="rich-text-block">{{ currentPatent.legalStatus }}</div>
        </section>

        <section v-if="structuredDetailText" class="detail-section">
          <div class="detail-section-head">
            <h3>技术详情</h3>
            <p>仅展示可读文本，自动过滤结构化 JSON 噪声。</p>
          </div>
          <div class="rich-text-block">{{ structuredDetailText }}</div>
        </section>

        <section v-if="rawInfoItems.length" class="detail-section">
          <div class="detail-section-head">
            <h3>更多原始字段</h3>
            <p>后端返回但未纳入核心信息的字段会在这里补充展示。</p>
          </div>
          <div class="raw-info-grid">
            <article v-for="item in rawInfoItems" :key="item.label" class="raw-info-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>
        </section>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, FolderOpened, MagicStick, Search, User, View } from '@element-plus/icons-vue'
import { patentApi, requirementApi } from '@/api'
import AIChat from '@/components/AIChat.vue'
import type { AiPatentSearchResponse, PatentBase, PatentEsDetailResponse, PatentEsHit } from '@/types'

const route = useRoute()

const showAIChat = ref(false)
const aiChatWidth = ref(480)
const aiChatHeight = ref(Math.min(760, window.innerHeight - 48))
const isResizing = ref(false)
const resizeMode = ref<'width' | 'both' | null>(null)
const isMobile = ref(window.innerWidth <= 768)
const VALID_PATENT_CATEGORIES = new Set(['wind', 'solar', 'biomass', 'hydrogen', 'lilon'])

const searchQuery = ref('')
const aiRequirement = ref('')
const mobileSearchMode = ref<'es' | 'ai'>('es')
const esResults = ref<PatentEsHit[]>([])
const aiResult = ref<AiPatentSearchResponse | null>(null)
const activeResultTab = ref<'es' | 'ai'>('es')
const esLoading = ref(false)
const aiLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const canSearchES = computed(() => Boolean(searchQuery.value.trim()) && !esLoading.value)
const canSearchAI = computed(() => Boolean(aiRequirement.value.trim()) && !aiLoading.value)
const searchStatusText = computed(() => {
  if (esLoading.value) return '数据库检索中'
  if (aiLoading.value) return 'AI 智能搜索中'
  if (activeResultTab.value === 'ai') return `AI 智能搜索命中 ${aiResult.value?.patents.length ?? 0} 条`
  return `数据库检索命中 ${total.value} 条`
})

const detailDialogVisible = ref(false)
const dialogTitle = ref('专利详情')
const currentPatent = ref<PatentEsDetailResponse>({
  publicNum: '',
  title: '',
  abstractText: '',
  applicant: '',
  inventor: '',
  ipc: ''
})

type DetailField = {
  label: string
  value: string
}

type ClassificationGroup = {
  label: string
  values: string[]
}

type LegalStatusNode = {
  date: string
  status: string
  description: string
}

const normalizeText = (value?: string | null) => String(value ?? '').trim()
const getFirstValue = (record: Record<string, unknown>, keys: string[]) => {
  for (const key of keys) {
    const value = record[key]
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      return value
    }
  }
  return ''
}

const PUBLIC_NUM_PREFIX = '(?:CN|US|EP|WO|JP|KR|AU|IN|JO)'
const COMPACT_PUBLIC_NUM_REGEX = new RegExp(`${PUBLIC_NUM_PREFIX}\\d{4,}[A-Z0-9.\\-/]*?(?=${PUBLIC_NUM_PREFIX}\\d{4,}|$)`, 'gi')
const normalizePatentCategory = (value: unknown) => {
  const category = String(value ?? '').trim().toLowerCase()
  return VALID_PATENT_CATEGORIES.has(category) ? category : ''
}

const normalizePatentRecord = (patent: PatentBase | Record<string, unknown> | null | undefined): PatentEsDetailResponse => {
  const record = (patent ?? {}) as Record<string, unknown>
  const rawRecord = (record.rawRecord && typeof record.rawRecord === 'object' ? record.rawRecord : {}) as Record<string, unknown>
  const merged = { ...rawRecord, ...record }
  const datasetIdValue = getFirstValue(merged, ['datasetId', 'dataset_id', '数据集ID'])
  return {
    ...(merged as unknown as Partial<PatentBase>),
    datasetId: typeof datasetIdValue === 'number'
      ? datasetIdValue
      : datasetIdValue
        ? Number(datasetIdValue)
        : undefined,
    datasetName: String(getFirstValue(merged, ['datasetName', 'dataset_name', '数据集', '数据源', '库名'])),
    recordId: String(getFirstValue(merged, ['recordId', 'record_id', 'id', 'ID', '序号', '主键', 'publicNum', 'public_num', '公开号'])),
    category: normalizePatentCategory(getFirstValue(merged, ['category', 'patentCategory', 'patent_category', '分类', '能源类型'])),
    publicNum: String(getFirstValue(merged, ['publicNum', 'public_num', 'patentPublicNum', 'patent_public_num', 'publicationNumber', 'publication_number', '公开号', '公开（公告）号', '公开公告号'])),
    title: String(getFirstValue(merged, ['title', 'patentTitle', 'patent_title', 'inventionTitle', '发明名称', '名称', '标题', '专利名称'])),
    abstractText: String(getFirstValue(merged, ['abstractText', 'abstract_text', 'abstract', '摘要', '专利摘要'])),
    applicant: String(getFirstValue(merged, ['applicant', 'applyPerson', 'apply_person', 'applicants', '申请人', '申请（专利权）人', '专利权人'])),
    applicantAddress: String(getFirstValue(merged, ['applicantAddress', 'applicant_address', '申请人地址', '申请地址'])),
    patentee: String(getFirstValue(merged, ['patentee', 'owner', 'assignee', '专利权人', '权利人'])),
    patenteeAddress: String(getFirstValue(merged, ['patenteeAddress', 'patentee_address', '专利权人地址', '权利人地址'])),
    inventor: String(getFirstValue(merged, ['inventor', 'inventors', '发明人', '设计人'])),
    agent: String(getFirstValue(merged, ['agent', 'agency', '代理人', '代理机构'])),
    ipc: String(getFirstValue(merged, ['ipc', 'ipcClass', 'ipc_class', 'IPC', 'IPC分类号', '国际专利分类号'])),
    cpc: String(getFirstValue(merged, ['cpc', 'CPC', 'CPC分类号'])),
    nec: String(getFirstValue(merged, ['nec', 'NEC'])),
    legalStatus: String(getFirstValue(merged, ['legalStatus', 'legal_status', 'latestLegalStatus', 'latest_legal_status', '法律状态', '当前法律状态', '最新法律状态'])),
    latestLegalStatus: String(getFirstValue(merged, ['latestLegalStatus', 'latest_legal_status', '最新法律状态'])),
    status: String(getFirstValue(merged, ['status', '状态'])),
    type: String(getFirstValue(merged, ['type', 'patentType', 'patent_type', '专利类型', '类型'])),
    patentDetails: String(getFirstValue(merged, ['patentDetails', 'patent_details', 'claims', '说明书', '权利要求', '专利详情'])),
    appliNum: String(getFirstValue(merged, ['appliNum', 'appli_num', 'applicationNumber', 'application_number', '申请号'])),
    appliDate: String(getFirstValue(merged, ['appliDate', 'appli_date', 'applicationDate', 'application_date', '申请日', '申请日期'])),
    publicDate: String(getFirstValue(merged, ['publicDate', 'public_date', 'publicationDate', 'publication_date', '公开日', '公开日期'])),
    rawRecord: record.rawRecord as Record<string, unknown> | undefined
  }
}

const normalizePublicNumList = (values?: string[] | null) => {
  const result: string[] = []
  const seen = new Set<string>()
  for (const raw of values ?? []) {
    const text = normalizeText(raw).toUpperCase()
    if (!text) continue
    const matches = text.match(COMPACT_PUBLIC_NUM_REGEX) ?? [text]
    for (const match of matches) {
      const value = match.replace(/[^A-Z0-9]/g, '')
      if (value && !seen.has(value)) {
        seen.add(value)
        result.push(value)
      }
    }
  }
  return result
}

const normalizeAiPatentSearchResponse = (response: AiPatentSearchResponse): AiPatentSearchResponse => ({
  ...response,
  extractedPublicNums: normalizePublicNumList(response.extractedPublicNums),
  notFoundPublicNums: normalizePublicNumList(response.notFoundPublicNums),
  patents: (response.patents ?? []).map(item => normalizePatentRecord(item) as typeof response.patents[number])
})

const splitTagValues = (value?: string | null) =>
  normalizeText(value)
    .split(/[;；,，\s]+/)
    .map(item => item.trim())
    .filter(Boolean)

const parseJsonLikeText = (value?: string | null) => {
  const text = normalizeText(value)
  if (!text || (!text.startsWith('{') && !text.startsWith('['))) return null
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

const parseLegalStatusTimeline = (value?: string | null): LegalStatusNode[] => {
  const text = normalizeText(value)
  if (!text) return []

  return text
    .split(/\n\s*\n+/)
    .map(block => block.replace(/^#+/gm, '').trim())
    .filter(Boolean)
    .map(block => {
      const compact = block.replace(/\n+/g, ' ')
      const date = compact.match(/法律状态公告日[:：]\s*([^;；\s]+)/)?.[1]?.trim() || ''
      const status = compact.match(/法律状态[:：]\s*([^;；]+)/)?.[1]?.trim() || ''
      const descriptionMatch = compact.match(/描述信息[:：]\s*(.+)$/)
      const description = descriptionMatch?.[1]?.trim()
        || compact
          .replace(/法律状态公告日[:：]\s*[^;；]+[;；]?/g, '')
          .replace(/法律状态[:：]\s*[^;；]+[;；]?/g, '')
          .replace(/描述信息[:：]/g, '')
          .trim()

      return { date, status, description }
    })
    .filter(item => item.date || item.status || item.description)
}

const overviewItems = computed<DetailField[]>(() => [
  { label: '公开号', value: normalizeText(currentPatent.value.publicNum) },
  { label: '申请号', value: normalizeText(currentPatent.value.appliNum) },
  { label: '申请日', value: normalizeText(currentPatent.value.appliDate) },
  { label: '公开日', value: normalizeText(currentPatent.value.publicDate) },
  { label: '申请人', value: normalizeText(currentPatent.value.applicant) },
  { label: '申请人地址', value: normalizeText(currentPatent.value.applicantAddress) },
  { label: '专利权人', value: normalizeText(currentPatent.value.patentee) },
  { label: '专利权人地址', value: normalizeText(currentPatent.value.patenteeAddress) },
  { label: '发明人', value: normalizeText(currentPatent.value.inventor) },
  { label: '代理人', value: normalizeText(currentPatent.value.agent) }
])

const patentStatusChips = computed(() =>
  [currentPatent.value.type, currentPatent.value.status, currentPatent.value.latestLegalStatus]
    .map(item => normalizeText(item))
    .filter(Boolean)
)

const classificationGroups = computed<ClassificationGroup[]>(() => [
  { label: 'IPC', values: splitTagValues(currentPatent.value.ipc) },
  { label: 'CPC', values: splitTagValues(currentPatent.value.cpc) },
  { label: 'NEC', values: splitTagValues(currentPatent.value.nec) }
].filter(group => group.values.length))

const legalStatusTimeline = computed(() => parseLegalStatusTimeline(currentPatent.value.legalStatus))

const structuredDetailText = computed(() => {
  const details = normalizeText(currentPatent.value.patentDetails)
  if (!details) return ''
  if (parseJsonLikeText(details)) return ''
  return details
})

const displayedRawKeys = new Set([
  'datasetId', 'dataset_id', 'datasetName', 'dataset_name', 'recordId', 'record_id',
  'publicNum', 'public_num', 'patentPublicNum', 'patent_public_num', '公开号', '公开（公告）号', '公开公告号',
  'title', 'patentTitle', 'patent_title', 'inventionTitle', '发明名称', '名称', '标题', '专利名称',
  'abstractText', 'abstract_text', 'abstract', '摘要', '专利摘要',
  'applicant', 'applyPerson', 'apply_person', 'applicants', '申请人', '申请（专利权）人',
  'inventor', 'inventors', '发明人', '设计人',
  'ipc', 'ipcClass', 'ipc_class', 'IPC', 'IPC分类号', '国际专利分类号',
  'cpc', 'CPC', 'CPC分类号',
  'nec', 'NEC',
  'legalStatus', 'legal_status', 'latestLegalStatus', 'latest_legal_status', '法律状态', '当前法律状态', '最新法律状态',
  'patentDetails', 'patent_details', 'claims', '说明书', '权利要求', '专利详情',
  'appliNum', 'appli_num', 'applicationNumber', 'application_number', '申请号',
  'appliDate', 'appli_date', 'applicationDate', 'application_date', '申请日', '申请日期',
  'publicDate', 'public_date', 'publicationDate', 'publication_date', '公开日', '公开日期'
])

const rawInfoItems = computed<DetailField[]>(() => {
  const rawRecord = currentPatent.value.rawRecord || {}
  return Object.entries(rawRecord)
    .filter(([key, value]) => !displayedRawKeys.has(key) && normalizeText(String(value ?? '')))
    .slice(0, 18)
    .map(([key, value]) => ({
      label: key,
      value: normalizeText(typeof value === 'object' ? JSON.stringify(value) : String(value))
    }))
})

const isAIChatVisible = computed(() => showAIChat.value)
const isDesktopAIChatVisible = computed(() => showAIChat.value && !isMobile.value)
const isMobileAIChatVisible = computed(() => showAIChat.value && isMobile.value)
const aiToggleLabel = computed(() => (showAIChat.value ? '关闭 AI 咨询' : '打开 AI 咨询'))

const toggleAIChat = () => {
  showAIChat.value = !showAIChat.value
}

const closeAIChat = () => {
  showAIChat.value = false
}

const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
  aiChatWidth.value = Math.min(aiChatWidth.value, window.innerWidth - 48)
  aiChatHeight.value = Math.min(aiChatHeight.value, window.innerHeight - 48)
  if (isMobile.value) {
    stopResize()
  }
}

const startResize = (event: MouseEvent, mode: 'width' | 'both' = 'width') => {
  if (isMobile.value) return
  isResizing.value = true
  resizeMode.value = mode
  document.body.classList.add('resizing')
  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
  event.preventDefault()
}

const handleResize = (event: MouseEvent) => {
  if (!isResizing.value) return
  aiChatWidth.value = Math.max(420, Math.min(980, window.innerWidth - event.clientX - 20))
  if (resizeMode.value === 'both') {
    aiChatHeight.value = Math.max(560, Math.min(window.innerHeight - 40, event.clientY - 20))
  }
}

const stopResize = () => {
  isResizing.value = false
  resizeMode.value = null
  document.body.classList.remove('resizing')
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
}

const resolveRouteKeyword = (value: unknown) => {
  if (Array.isArray(value)) return typeof value[0] === 'string' ? value[0].trim() : ''
  return typeof value === 'string' ? value.trim() : ''
}

const searchPatentsByES = async () => {
  const query = searchQuery.value.trim()
  if (!query) {
    ElMessage.warning('请输入数据库检索关键词')
    return
  }

  esLoading.value = true
  try {
    const response = await patentApi.searchPatentsByES({
      query,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    esResults.value = (response.hits || []).map(item => normalizePatentRecord(item) as PatentEsHit)
    total.value = response.total || 0
    activeResultTab.value = 'es'
    if (!response.total) {
      ElMessage.info('未找到相关数据库结果')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '数据库检索失败')
    esResults.value = []
    total.value = 0
  } finally {
    esLoading.value = false
  }
}

const searchPatentsByAI = async () => {
  const requirement = aiRequirement.value.trim()
  if (!requirement) {
    ElMessage.warning('请输入 AI 智能搜索的需求描述')
    return
  }

  aiLoading.value = true
  try {
    aiResult.value = normalizeAiPatentSearchResponse(await requirementApi.aiMatchPatents({ requirement }))
    activeResultTab.value = 'ai'
    if (!aiResult.value.patents.length) {
      ElMessage.info('AI 已完成分析，但当前没有命中的专利结果')
    }
  } catch (error: any) {
    ElMessage.error(error.message || 'AI 智能搜索失败')
    aiResult.value = null
  } finally {
    aiLoading.value = false
  }
}

const resetES = () => {
  searchQuery.value = ''
  esResults.value = []
  total.value = 0
  currentPage.value = 1
  activeResultTab.value = 'es'
}

const resetAI = () => {
  aiRequirement.value = ''
  aiResult.value = null
  activeResultTab.value = 'ai'
}

const applyRouteKeyword = async (value: unknown) => {
  const keyword = resolveRouteKeyword(value)
  if (!keyword) return
  searchQuery.value = keyword.slice(0, 120)
  currentPage.value = 1
  await searchPatentsByES()
}

const getPatentKey = (patent: PatentBase) => `${patent.datasetId ?? 'dataset'}-${patent.recordId ?? patent.publicNum ?? patent.title}`

const viewPatent = async (row: PatentBase) => {
  dialogTitle.value = row.title || '专利详情'
  detailDialogVisible.value = true
  currentPatent.value = {
    ...row,
    publicNum: row.publicNum || '',
    title: row.title || '',
    abstractText: row.abstractText || '',
    applicant: row.applicant || '',
    inventor: row.inventor || '',
    ipc: row.ipc || ''
  }

  if (!row.datasetId || !row.recordId) return

  try {
    const detail = await patentApi.getPatentDetail(row.datasetId, row.recordId)
    currentPatent.value = {
      ...currentPatent.value,
      ...normalizePatentRecord(detail),
      rawRecord: detail.rawRecord
    } as PatentEsDetailResponse
  } catch (error: any) {
    console.warn('加载专利详情失败，回退为列表摘要数据展示', error)
  }
}

const closeDetailDialog = () => {
  detailDialogVisible.value = false
  currentPatent.value = {
    publicNum: '',
    title: '',
    abstractText: '',
    applicant: '',
    inventor: '',
    ipc: ''
  }
}

const handleSizeChange = (value: number) => {
  pageSize.value = value
  currentPage.value = 1
  if (searchQuery.value.trim()) void searchPatentsByES()
}

const handleCurrentChange = (value: number) => {
  currentPage.value = value
  if (searchQuery.value.trim()) void searchPatentsByES()
}

watch(
  () => route.query.q,
  (value, previousValue) => {
    if (value === previousValue) return
    void applyRouteKeyword(value)
  }
)

onMounted(() => {
  window.addEventListener('resize', checkMobile)
  void applyRouteKeyword(route.query.q)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  stopResize()
})
</script>

<style scoped>
.patent-search-page { min-height: 100vh; display: flex; flex-direction: column; background: var(--bg-app); }
.patent-search-page {
  --search-card-bg: linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(243, 248, 255, 0.94));
  --search-card-border: rgba(61, 125, 255, 0.14);
  --search-title: #12355c;
  --search-subtitle: #5b708c;
  --search-note: #6c7f93;
  --search-switch-bg: rgba(61, 125, 255, 0.08);
  --search-switch-border: rgba(61, 125, 255, 0.12);
  --search-switch-text: #466583;
  --search-switch-strong: #12355c;
  --search-switch-active-bg: linear-gradient(135deg, rgba(47, 123, 246, 0.16), rgba(72, 186, 255, 0.12));
  --search-card-lite-bg: rgba(255, 255, 255, 0.72);
  --search-card-lite-border: rgba(61, 125, 255, 0.12);
  --search-card-lite-text: #4e6686;
  --search-tag-bg: rgba(61, 125, 255, 0.08);
  --search-tag-text: #12355c;
  --search-input-bg: rgba(255, 255, 255, 0.92);
  --search-input-text: #10263f;
  --search-placeholder: #6c7f93;
}
.header-subtitle { margin: 6px 0 0; color: var(--search-subtitle); font-size: 13px; }
.main-content { flex: 1; display: flex; gap: 24px; padding: 24px; min-width: 0; align-items: flex-start; }
.main-content.with-chat { padding-right: 0; }
.content-column { min-width: 0; display: flex; flex-direction: column; }
.search-focus {
  padding: 18px 20px;
  margin-bottom: 18px;
  border-radius: 16px;
  background: var(--search-card-bg) !important;
  border: 1px solid var(--search-card-border) !important;
}
.search-focus-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.search-focus-head h3 {
  margin: 0 0 6px;
  color: var(--search-title);
}
.search-focus-head p {
  margin: 0;
  color: var(--search-subtitle);
  font-size: 13px;
  line-height: 1.7;
}
.mode-tabs {
  display: inline-flex;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: var(--search-switch-bg);
  border: 1px solid var(--search-switch-border);
}
.mode-tab {
  border: none;
  background: transparent;
  color: var(--search-switch-text);
  padding: 10px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
.mode-tab:focus-visible,
.result-switch:focus-visible,
.patent-title-link:focus-visible,
.patent-card:focus-visible {
  outline: 3px solid rgba(47, 123, 246, 0.58);
  outline-offset: 3px;
}
.mode-tab.active {
  color: var(--search-title);
  background: var(--search-switch-active-bg);
  box-shadow: inset 0 0 0 1px var(--search-switch-border);
}
.search-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px; }
.search-card, .result-card {
  padding: 24px;
  border-radius: 16px;
  background: var(--search-card-bg) !important;
  border: 1px solid var(--search-card-border) !important;
}
.card-head, .section-header, .subsection-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.card-head, .subsection-header { margin-bottom: 18px; }
.card-head h3, .section-header h3, .subsection-header h4 { margin: 0 0 6px; color: var(--search-title); }
.card-head p, .section-header p, .subsection-header p {
  margin: 0;
  color: var(--search-subtitle);
  font-size: 13px;
  line-height: 1.7;
}
.search-body { display: flex; flex-direction: column; gap: 14px; }
.search-note { color: var(--search-note); font-size: 13px; line-height: 1.7; }
.search-actions { display: flex; gap: 12px; flex-wrap: wrap; }
.result-switcher {
  display: inline-flex;
  gap: 10px;
  padding: 6px;
  margin: 18px 0 20px;
  border-radius: 999px;
  background: var(--search-switch-bg);
  border: 1px solid var(--search-switch-border);
}
.result-switch {
  border: none;
  background: transparent;
  color: var(--search-switch-text);
  min-width: 164px;
  padding: 10px 16px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  cursor: pointer;
  transition: background .2s ease, color .2s ease, box-shadow .2s ease;
}
.result-switch span,
.result-switch strong {
  font-size: 14px;
}
.result-switch strong { color: var(--search-switch-strong); }
.result-switch.active {
  color: var(--search-title);
  background: var(--search-switch-active-bg);
  box-shadow: inset 0 0 0 1px var(--search-switch-border);
}
.result-section + .result-section { margin-top: 12px; }
.unified-subsection-header {
  margin-bottom: 16px;
}
.ai-analysis { margin-bottom: 20px; }
.analysis-text { margin: 0; color: var(--text-primary); line-height: 1.8; }
.analysis-block { margin-top: 16px; }
.analysis-block h4 { margin: 0 0 10px; font-size: 14px; color: var(--search-title); }
.chip-list { display: flex; flex-wrap: wrap; gap: 8px; }
.patent-title-link { border: none; background: transparent; padding: 0; color: #2f7bf6; cursor: pointer; text-align: left; }
.patent-title-link:hover { color: #1f63cf; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 20px; }
.mobile-patent-list { min-height: 180px; }
.patent-cards { display: grid; gap: 14px; }
.patent-card {
  width: 100%;
  text-align: left;
  cursor: pointer;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid var(--search-card-lite-border);
  background: var(--search-card-lite-bg);
}
.card-header { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 10px; font-size: 12px; color: var(--search-card-lite-text); }
.card-title { margin: 0 0 10px; color: var(--search-title); font-size: 16px; line-height: 1.6; }
.card-info { display: flex; flex-direction: column; gap: 8px; }
.info-item { display: flex; align-items: center; gap: 8px; color: var(--search-card-lite-text); font-size: 13px; }
.card-meta-line { display: flex; flex-wrap: wrap; gap: 8px; color: var(--search-card-lite-text); font-size: 12px; line-height: 1.6; }
.card-meta-line span { padding: 4px 8px; border-radius: 999px; background: rgba(61, 125, 255, 0.08); }
.empty-state { min-height: 180px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; color: var(--search-subtitle); text-align: center; }
.empty-state :deep(.el-icon) { font-size: 28px; color: #4776c3; }
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
.ai-chat-section { position: sticky; top: 24px; flex: 0 0 auto; min-width: 420px; max-width: 980px; min-height: 560px; max-height: calc(100vh - 40px); border-left: 1px solid rgba(61, 125, 255, 0.12); overflow: hidden; }
.mobile-ai-chat-shell {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: flex-end;
}
.mobile-ai-chat-mask {
  position: absolute;
  inset: 0;
  background: rgba(7, 18, 34, 0.52);
  backdrop-filter: blur(4px);
}
.mobile-ai-chat-panel {
  position: relative;
  width: 100%;
  height: min(78vh, 720px);
  border-radius: 24px 24px 0 0;
  overflow: hidden;
  border-top: 1px solid rgba(134, 196, 255, 0.2);
  box-shadow: 0 -20px 46px rgba(6, 16, 34, 0.28);
}
.mobile-ai-chat-header {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  padding: 14px 16px 0;
  color: var(--text-primary);
}
.mobile-ai-chat-header strong {
  display: block;
  font-size: 16px;
  line-height: 1.4;
}
.mobile-ai-chat-header p { margin: 4px 0 0; font-size: 12px; color: var(--search-subtitle); line-height: 1.6; }
.resize-handle { position: absolute; top: 0; bottom: 0; left: 0; width: 10px; cursor: col-resize; background: linear-gradient(180deg, rgba(61, 125, 255, 0.05), rgba(61, 125, 255, 0.18), rgba(61, 125, 255, 0.05)); z-index: 12; }
.resize-handle:hover { background: linear-gradient(180deg, rgba(61, 125, 255, 0.12), rgba(61, 125, 255, 0.34), rgba(61, 125, 255, 0.12)); }
.corner-resize-handle { position: absolute; left: 0; bottom: 0; width: 18px; height: 18px; background: linear-gradient(135deg, rgba(61,125,255,.18), rgba(61,125,255,.58)); clip-path: polygon(0 100%, 100% 0, 100% 100%); cursor: nwse-resize; z-index: 13; }
.abstract-content, .details-content, .rich-text-block { white-space: pre-wrap; word-break: break-word; line-height: 1.9; }
.patent-detail-shell { display: flex; flex-direction: column; gap: 18px; color: #0f223d; }
.patent-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  padding: 20px 22px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(233, 243, 255, 0.96), rgba(216, 234, 255, 0.9));
  border: 1px solid rgba(84, 127, 190, 0.18);
  box-shadow: 0 18px 36px rgba(36, 66, 112, 0.12);
}
.patent-public-num {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #2f66b3;
}
.patent-hero h2 {
  margin: 0;
  color: #10233d;
  font-size: 28px;
  line-height: 1.45;
}
.patent-meta-line {
  margin: 12px 0 0;
  color: #4d6482;
  font-size: 14px;
  font-weight: 600;
}
.patent-status-chips {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}
.status-chip,
.classification-tag {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(88, 130, 191, 0.18);
  color: #183863;
  font-size: 13px;
  font-weight: 700;
}
.detail-section {
  padding: 20px 22px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 249, 255, 0.98));
  border: 1px solid rgba(95, 134, 191, 0.14);
  box-shadow: 0 16px 32px rgba(42, 70, 112, 0.08);
}
.detail-section-head {
  margin-bottom: 14px;
}
.detail-section-head h3 {
  margin: 0 0 6px;
  color: #112743;
  font-size: 18px;
}
.detail-section-head p {
  margin: 0;
  color: #5d7392;
  line-height: 1.7;
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.detail-card {
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(240, 247, 255, 0.95), rgba(231, 241, 253, 0.92));
  border: 1px solid rgba(104, 144, 201, 0.16);
}
.detail-label {
  display: block;
  margin-bottom: 8px;
  color: #44648c;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
}
.detail-value {
  color: #122641;
  font-size: 15px;
  line-height: 1.8;
  font-weight: 600;
}
.detail-value.empty {
  color: #6f84a1;
  font-weight: 500;
}
.classification-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.classification-row {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.classification-label {
  flex: 0 0 56px;
  color: #36557d;
  font-size: 13px;
  font-weight: 700;
  padding-top: 8px;
}
.classification-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.rich-text-block {
  padding: 18px 20px;
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(239, 247, 255, 0.96), rgba(228, 239, 252, 0.94));
  border: 1px solid rgba(101, 145, 205, 0.14);
  color: #132844;
  font-size: 15px;
}
.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.timeline-item {
  display: grid;
  grid-template-columns: 132px 1fr;
  gap: 14px;
  align-items: stretch;
}
.timeline-date {
  border-radius: 16px;
  padding: 14px 16px;
  background: linear-gradient(180deg, rgba(55, 122, 214, 0.14), rgba(74, 190, 246, 0.12));
  color: #1a4f91;
  font-size: 14px;
  font-weight: 800;
}
.timeline-content {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(241, 247, 255, 0.94);
  border: 1px solid rgba(102, 145, 201, 0.14);
}
.timeline-content h4 {
  margin: 0 0 8px;
  color: #112743;
  font-size: 16px;
}
.timeline-content p {
  margin: 0;
  color: #3f5878;
  line-height: 1.8;
}
.raw-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.raw-info-item {
  min-width: 0;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(241, 247, 255, 0.94);
  border: 1px solid rgba(102, 145, 201, 0.14);
}
.raw-info-item span {
  display: block;
  margin-bottom: 6px;
  color: #44648c;
  font-size: 12px;
  font-weight: 800;
}
.raw-info-item strong {
  display: block;
  color: #132844;
  font-size: 14px;
  line-height: 1.7;
  font-weight: 600;
  word-break: break-word;
}
:deep(.patent-detail-dialog .el-dialog) {
  background: linear-gradient(180deg, #fdfefe, #eef5ff);
  box-shadow: 0 28px 64px rgba(10, 25, 52, 0.22);
}
:deep(.patent-detail-dialog .el-dialog__header) {
  padding-bottom: 6px;
}
:deep(.patent-detail-dialog .el-dialog__title) {
  color: #0f223d;
  font-weight: 800;
}
:deep(.patent-detail-dialog .el-dialog__body) {
  padding-top: 8px;
}
:deep(.search-card .el-tag),
:deep(.result-card .el-tag) {
  border-color: var(--search-switch-border);
  color: var(--search-tag-text);
  background: var(--search-tag-bg);
}
:deep(.search-card .el-input__wrapper),
:deep(.search-card .el-textarea__inner) {
  background: var(--search-input-bg) !important;
  box-shadow: 0 0 0 1px var(--search-card-border) inset !important;
  color: var(--search-input-text) !important;
}
:deep(.search-card .el-input__inner),
:deep(.search-card .el-textarea__inner) {
  color: var(--search-input-text) !important;
}
:deep(.search-card .el-input__inner::placeholder),
:deep(.search-card .el-textarea__inner::placeholder) {
  color: var(--search-placeholder) !important;
}
[data-theme="dark"] .patent-search-page {
  --search-card-bg: linear-gradient(145deg, rgba(8, 26, 50, 0.88), rgba(11, 38, 72, 0.8));
  --search-card-border: rgba(134, 196, 255, 0.16);
  --search-title: #f6fbff;
  --search-subtitle: rgba(214, 232, 255, 0.78);
  --search-note: rgba(214, 232, 255, 0.82);
  --search-switch-bg: rgba(255, 255, 255, 0.06);
  --search-switch-border: rgba(134, 196, 255, 0.14);
  --search-switch-text: rgba(214, 232, 255, 0.82);
  --search-switch-strong: #f4fbff;
  --search-switch-active-bg: linear-gradient(135deg, rgba(47, 123, 246, 0.34), rgba(72, 186, 255, 0.24));
  --search-card-lite-bg: rgba(255, 255, 255, 0.08);
  --search-card-lite-border: rgba(134, 196, 255, 0.14);
  --search-card-lite-text: rgba(214, 232, 255, 0.78);
  --search-tag-bg: rgba(255, 255, 255, 0.08);
  --search-tag-text: #f4fbff;
  --search-input-bg: rgba(255, 255, 255, 0.08);
  --search-input-text: #f4fbff;
  --search-placeholder: rgba(214, 232, 255, 0.68);
}
@media (min-width: 769px) {
  .patent-search-page {
    background: #f5f7fb;
  }

  .main-content {
    max-width: 1360px;
    width: 100%;
    margin: 0 auto;
    padding: 28px 32px 40px;
    gap: 28px;
  }

  .glass-header,
  .search-focus,
  .search-card,
  .result-card,
  .ai-chat-section {
    border-radius: 18px;
    box-shadow: 0 12px 28px rgba(16, 38, 63, 0.08);
  }

  .search-card,
  .result-card {
    padding: 22px;
  }

  .search-grid {
    gap: 20px;
  }

  .ai-chat-section {
    top: 28px;
    border-left: none;
  }
}
@media (max-width: 1100px) { .search-grid { grid-template-columns: 1fr; } }
@media (max-width: 768px) {
  .glass-header {
    gap: 14px;
    padding: 18px 16px;
    background: rgba(7, 22, 42, 0.82);
  }
  .header-actions {
    width: 100%;
  }
  .header-actions :deep(.el-button) {
    width: 100%;
    margin: 0;
  }
  .main-content {
    padding: 14px;
    gap: 16px;
  }
  .content-column {
    width: 100% !important;
  }
  .search-grid {
    gap: 16px;
  }
  .search-focus {
    padding: 16px;
    margin-bottom: 16px;
  }
  .search-focus-head {
    flex-direction: column;
  }
  .mode-tabs {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
  .mode-tab {
    width: 100%;
  }
  .search-card, .result-card {
    padding: 18px;
    border-radius: 18px;
    box-shadow: 0 18px 38px rgba(3, 11, 28, 0.22);
  }
  .search-card.collapsed {
    opacity: 0.92;
  }
  .card-head, .section-header, .subsection-header {
    flex-direction: column;
    gap: 10px;
  }
  .card-head h3,
  .section-header h3,
  .subsection-header h4 {
    font-size: 20px;
  }
  .result-switcher {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr 1fr;
    padding: 8px;
    border-radius: 20px;
  }
  .result-switch {
    min-width: 0;
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    padding: 12px 14px;
    border-radius: 16px;
  }
  .result-switch strong {
    font-size: 18px;
  }
  .search-actions {
    display: grid;
    grid-template-columns: 1fr;
  }
  .search-actions :deep(.el-button) {
    width: 100%;
    margin: 0;
  }
  .patent-card {
    padding: 14px;
    border-radius: 18px;
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.1), rgba(160, 212, 255, 0.06));
    box-shadow: 0 12px 24px rgba(3, 11, 28, 0.14);
  }
  .card-header {
    flex-direction: column;
    gap: 6px;
  }
  .pagination-wrapper {
    justify-content: center;
  }
  :deep(.pagination-wrapper .el-pagination) {
    justify-content: center;
    gap: 8px;
    flex-wrap: wrap;
  }
  .patent-hero,
  .detail-section {
    padding: 16px;
    border-radius: 18px;
  }
  .patent-hero {
    flex-direction: column;
  }
  .patent-hero h2 {
    font-size: 22px;
  }
  .detail-grid {
    grid-template-columns: 1fr;
  }
  .raw-info-grid {
    grid-template-columns: 1fr;
  }
  .classification-row,
  .timeline-item {
    display: flex;
    flex-direction: column;
  }
  .classification-label {
    flex-basis: auto;
    padding-top: 0;
  }
  .mobile-ai-chat-panel {
    height: min(82vh, 760px);
    border-radius: 22px 22px 0 0;
  }
}
@media (max-width: 480px) {
  .glass-header,
  .main-content {
    padding-left: 12px;
    padding-right: 12px;
  }
  .search-card, .result-card {
    padding: 16px;
  }
  .result-switcher {
    grid-template-columns: 1fr;
  }
}
:global(body.resizing) { cursor: col-resize !important; user-select: none !important; }
.mobile-chat-fade-enter-active,
.mobile-chat-fade-leave-active {
  transition: opacity .24s ease;
}
.mobile-chat-fade-enter-from,
.mobile-chat-fade-leave-to {
  opacity: 0;
}
</style>
