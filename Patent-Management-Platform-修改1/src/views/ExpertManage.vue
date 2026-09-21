<template>
  <div class="expert-page">
    <div class="page-hero glass-panel">
      <div>
        <p class="hero-kicker">专家资源</p>
        <h2>专家库浏览与协同入口</h2>
        <p class="hero-text">
          当前页面面向已登录用户提供专家资料浏览和初步检索。专家档案由平台统一维护，
          如果暂时没有命中结果，并不代表系统异常。
        </p>
      </div>
      <div class="hero-tags">
        <el-tag effect="plain" type="primary">用户可见</el-tag>
        <el-tag effect="plain">运营维护资料</el-tag>
      </div>
    </div>

    <div class="glass-panel search-panel">
      <div class="panel-head">
        <div>
          <h3>检索专家</h3>
          <p>可按姓名、研究方向或核心专长筛选平台已整理的专家资源。</p>
        </div>
      </div>

      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.query"
            class="glass-input"
            placeholder="输入姓名、研究方向或核心专长"
            @keyup.enter="searchExperts"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="action-btn" @click="searchExperts">查询</el-button>
          <el-button class="ghost-btn" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="panel-tip">
        <span>如需新增或更新专家资料，请联系平台运营管理员维护基础档案。</span>
      </div>
    </div>

    <div class="glass-panel list-panel">
      <div class="panel-head">
        <div>
          <h3>专家列表</h3>
          <p>优先展示平台已收录的专家信息，空状态会给出下一步建议。</p>
        </div>
        <el-tag effect="plain" type="info">共 {{ total }} 位</el-tag>
      </div>

      <el-table
        :data="pagedExperts"
        v-loading="loading"
        class="glass-table"
        style="width: 100%"
        :header-cell-style="{ background: 'var(--expert-table-head-bg)', color: 'var(--expert-table-head-text)' }"
      >
        <el-table-column prop="name" label="专家" min-width="180">
          <template #default="{ row }">
            <div class="expert-name">
              <el-avatar :size="32" class="expert-avatar">{{ row.name?.charAt(0) || '专' }}</el-avatar>
              <div>
                <strong>{{ row.name || '未命名专家' }}</strong>
                <p>{{ row.field || '未标注研究方向' }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="expertise" label="核心专长" min-width="280" show-overflow-tooltip />
        <el-table-column prop="achievements" label="主要成果" min-width="280" show-overflow-tooltip />
        <el-table-column prop="contactInfo" label="联系信息" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewExpert(row)">查看详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">
            <h4>{{ searchForm.query ? '未找到匹配专家' : '专家资源正在持续整理中' }}</h4>
            <p>
              {{ searchForm.query
                ? '可以尝试更换关键词、缩短条件，或直接发布需求让平台协助匹配。'
                : '当前还没有可展示的专家档案。你可以先发布需求，等待平台按场景协同推荐。' }}
            </p>
            <div class="empty-actions">
              <el-button type="primary" class="action-btn" @click="goToDemand">发布需求</el-button>
              <el-button class="ghost-btn" @click="resetSearch">清空筛选</el-button>
            </div>
          </div>
        </template>
      </el-table>

      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          layout="total, prev, pager, next"
          :total="total"
        />
      </div>
    </div>

    <el-dialog
      v-model="detailDialogVisible"
      title="专家详情"
      width="640px"
      class="glass-dialog expert-detail-dialog"
      :before-close="closeDetailDialog"
    >
      <div class="detail-shell">
        <div class="detail-hero">
          <el-avatar :size="72" class="detail-avatar">{{ currentExpert.name?.charAt(0) || '专' }}</el-avatar>
          <div>
            <h3>{{ currentExpert.name || '未命名专家' }}</h3>
            <p>{{ currentExpert.field || '未标注研究方向' }}</p>
          </div>
        </div>

        <div class="detail-grid">
          <article class="detail-card">
            <span class="detail-label">核心专长</span>
            <p>{{ currentExpert.expertise || '未提供' }}</p>
          </article>
          <article class="detail-card">
            <span class="detail-label">联系信息</span>
            <p>{{ currentExpert.contactInfo || '平台暂未开放直接联系方式' }}</p>
          </article>
          <article class="detail-card detail-card-wide">
            <span class="detail-label">主要成果</span>
            <p>{{ currentExpert.achievements || '未提供成果简介' }}</p>
          </article>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { expertApi } from '@/api'
import type { Expert } from '@/types'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()

const searchForm = reactive({ query: '' })
const expertList = ref<Expert[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailDialogVisible = ref(false)
const currentExpert = ref<Expert>({} as Expert)

const pagedExperts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return expertList.value.slice(start, start + pageSize.value)
})

const searchExperts = async () => {
  loading.value = true
  currentPage.value = 1
  try {
    const result = await expertApi.getExperts({ query: searchForm.query.trim() || undefined })
    expertList.value = result || []
    total.value = expertList.value.length
  } catch (error: any) {
    ElMessage.error(error.message || '获取专家列表失败')
    expertList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.query = ''
  void searchExperts()
}

const viewExpert = (row: Expert) => {
  currentExpert.value = row
  detailDialogVisible.value = true
}

const closeDetailDialog = () => {
  detailDialogVisible.value = false
  currentExpert.value = {} as Expert
}

const goToDemand = () => {
  router.push('/demand')
}

onMounted(() => {
  void searchExperts()
})
</script>

<style scoped>
.expert-page {
  --expert-page-bg:
    radial-gradient(circle at top left, rgba(74, 162, 255, 0.16), transparent 32%),
    radial-gradient(circle at bottom right, rgba(51, 207, 180, 0.14), transparent 28%),
    linear-gradient(180deg, rgba(244, 249, 255, 0.98), rgba(236, 244, 255, 0.94));
  --expert-panel-bg: rgba(255, 255, 255, 0.78);
  --expert-panel-border: rgba(118, 160, 215, 0.18);
  --expert-panel-shadow: 0 20px 42px rgba(25, 56, 96, 0.08);
  --expert-kicker: #2f7bf6;
  --expert-title: #122641;
  --expert-text: #5f7898;
  --expert-input-bg: rgba(246, 250, 255, 0.96);
  --expert-input-border: rgba(112, 149, 198, 0.22);
  --expert-ghost-bg: rgba(242, 247, 255, 0.96);
  --expert-ghost-border: rgba(112, 149, 198, 0.22);
  --expert-ghost-text: #1d3557;
  --expert-tip-bg: rgba(235, 243, 255, 0.92);
  --expert-tip-border: rgba(112, 149, 198, 0.16);
  --expert-name: #18324f;
  --expert-table-head-bg: rgba(233, 242, 255, 0.82);
  --expert-table-head-text: #1d3557;
  --expert-empty-title: #18324f;
  --expert-detail-hero-bg: linear-gradient(135deg, rgba(232, 243, 255, 0.96), rgba(242, 249, 255, 0.98));
  --expert-detail-card-bg: rgba(247, 250, 255, 0.96);
  --expert-detail-card-border: rgba(112, 149, 198, 0.16);
  --expert-detail-label: #47678f;
  min-height: calc(100vh - 64px);
  padding: 24px;
  background: var(--expert-page-bg);
}

.glass-panel {
  max-width: 1240px;
  margin: 0 auto 22px;
  padding: 24px;
  border-radius: 24px;
  background: var(--expert-panel-bg);
  border: 1px solid var(--expert-panel-border);
  box-shadow: var(--expert-panel-shadow);
  backdrop-filter: blur(16px);
}

.page-hero,
.panel-head,
.hero-tags,
.search-form,
.panel-tip,
.expert-name,
.detail-hero,
.empty-actions {
  display: flex;
}

.page-hero,
.panel-head {
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.hero-kicker {
  margin: 0 0 8px;
  color: var(--expert-kicker);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.page-hero h2,
.panel-head h3 {
  margin: 0;
  color: var(--expert-title);
}

.hero-text,
.panel-head p,
.panel-tip span,
.expert-name p,
.detail-hero p,
.empty-state p {
  color: var(--expert-text);
  line-height: 1.8;
}

.hero-text,
.panel-head p,
.panel-tip span {
  margin: 10px 0 0;
}

.hero-tags,
.empty-actions {
  gap: 10px;
  flex-wrap: wrap;
}

.search-panel,
.list-panel {
  overflow: hidden;
}

.search-form {
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.search-form :deep(.el-input) {
  min-width: min(460px, 72vw);
}

.glass-input :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 14px;
  box-shadow: inset 0 0 0 1px var(--expert-input-border);
  background: var(--expert-input-bg);
}

.action-btn,
.ghost-btn {
  min-width: 108px;
  border-radius: 999px;
}

.ghost-btn {
  background: var(--expert-ghost-bg);
  border: 1px solid var(--expert-ghost-border);
  color: var(--expert-ghost-text);
}

.panel-tip {
  margin-top: 16px;
  padding: 12px 14px;
  border-radius: 16px;
  background: var(--expert-tip-bg);
  border: 1px solid var(--expert-tip-border);
}

.expert-name {
  align-items: center;
  gap: 12px;
}

.expert-name strong {
  display: block;
  color: var(--expert-name);
}

.expert-name p {
  margin: 4px 0 0;
  font-size: 12px;
}

.expert-avatar,
.detail-avatar {
  background: linear-gradient(135deg, #2f7bf6, #3ec7d7);
  color: #fff;
}

.empty-state {
  padding: 36px 0;
  text-align: center;
}

.empty-state h4 {
  margin: 0 0 8px;
  color: var(--expert-empty-title);
  font-size: 20px;
}

.empty-actions {
  justify-content: center;
  margin-top: 18px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 22px;
}

.detail-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-hero {
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 20px;
  background: var(--expert-detail-hero-bg);
}

.detail-hero h3 {
  margin: 0;
  color: var(--expert-title);
}

.detail-hero p {
  margin: 8px 0 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-card {
  padding: 16px;
  border-radius: 18px;
  background: var(--expert-detail-card-bg);
  border: 1px solid var(--expert-detail-card-border);
}

.detail-card-wide {
  grid-column: 1 / -1;
}

.detail-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--expert-detail-label);
  font-weight: 700;
  letter-spacing: 0.06em;
}

.detail-card p {
  margin: 0;
  color: var(--expert-name);
  line-height: 1.8;
}

[data-theme="dark"] .expert-page {
  --expert-page-bg:
    radial-gradient(circle at top left, rgba(74, 162, 255, 0.16), transparent 32%),
    radial-gradient(circle at bottom right, rgba(51, 207, 180, 0.14), transparent 28%),
    linear-gradient(180deg, rgba(9, 24, 46, 0.98), rgba(6, 18, 36, 0.96));
  --expert-panel-bg: rgba(9, 26, 50, 0.78);
  --expert-panel-border: rgba(118, 160, 215, 0.16);
  --expert-panel-shadow: 0 20px 42px rgba(3, 11, 28, 0.2);
  --expert-kicker: #8fd4ff;
  --expert-title: #f1f8ff;
  --expert-text: rgba(214, 232, 255, 0.78);
  --expert-input-bg: rgba(255, 255, 255, 0.08);
  --expert-input-border: rgba(112, 149, 198, 0.22);
  --expert-ghost-bg: rgba(255, 255, 255, 0.08);
  --expert-ghost-border: rgba(112, 149, 198, 0.2);
  --expert-ghost-text: #d9edff;
  --expert-tip-bg: rgba(255, 255, 255, 0.06);
  --expert-tip-border: rgba(112, 149, 198, 0.16);
  --expert-name: #edf6ff;
  --expert-table-head-bg: rgba(255, 255, 255, 0.08);
  --expert-table-head-text: #d9edff;
  --expert-empty-title: #f1f8ff;
  --expert-detail-hero-bg: linear-gradient(135deg, rgba(11, 31, 58, 0.96), rgba(8, 24, 46, 0.98));
  --expert-detail-card-bg: rgba(255, 255, 255, 0.06);
  --expert-detail-card-border: rgba(112, 149, 198, 0.16);
  --expert-detail-label: #9ccdf3;
}

@media (max-width: 768px) {
  .expert-page {
    padding: 14px;
  }

  .glass-panel {
    padding: 18px 16px;
    border-radius: 24px;
  }

  .page-hero,
  .panel-head,
  .search-form,
  .detail-hero {
    flex-direction: column;
  }

  .search-form :deep(.el-input) {
    min-width: 100%;
  }

  .search-form :deep(.el-form-item) {
    width: 100%;
  }

  .search-form :deep(.el-button) {
    width: 100%;
    margin: 0;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-card-wide {
    grid-column: auto;
  }
}

@media (max-width: 480px) {
  .expert-page {
    padding: 12px;
  }

  .glass-panel {
    padding: 16px 14px;
  }

  .page-hero h2,
  .panel-head h3 {
    font-size: 20px;
    line-height: 1.5;
  }
}
</style>
