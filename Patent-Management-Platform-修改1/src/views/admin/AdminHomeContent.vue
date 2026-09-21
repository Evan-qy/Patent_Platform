<template>
  <el-card class="home-content-card">
    <template #header>
      <div class="card-header">
        <strong>首页配置</strong>
        <span>维护首页文案、数字卡片和文章区域配置</span>
      </div>
    </template>

    <el-form label-position="top" v-loading="loading" class="content-form">
      <el-form-item label="主标题">
        <el-input v-model="form.heroTitle" />
      </el-form-item>

      <el-form-item label="品牌标语">
        <el-input v-model="form.brandSlogan" />
      </el-form-item>

      <el-form-item label="首页描述">
        <el-input v-model="form.heroDescription" type="textarea" :rows="3" />
      </el-form-item>

      <el-form-item label="核心功能标题">
        <el-input v-model="form.coreFeaturesTitle" />
      </el-form-item>

      <el-form-item label="核心功能简介">
        <el-input v-model="form.coreFeaturesIntro" type="textarea" :rows="3" />
      </el-form-item>

      <el-form-item label="首页数字卡片">
        <div class="metric-editor">
          <div v-for="(item, index) in form.heroMetrics" :key="`metric-${index}`" class="metric-editor-row">
            <el-input v-model="item.value" placeholder="数值，例如 120+" />
            <el-input v-model="item.label" placeholder="标签，例如 服务高校" />
            <el-input v-model="item.detail" placeholder="补充说明" />
            <el-button text type="danger" @click="removeHeroMetric(index)">删除</el-button>
          </div>

          <div class="metric-editor-actions">
            <el-button @click="addHeroMetric">新增数字卡片</el-button>
            <el-button type="primary" plain @click="applyDefaultHeroMetrics">恢复默认数字</el-button>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="功能入口 JSON">
        <el-input v-model="featuresJson" type="textarea" :rows="8" />
      </el-form-item>

      <el-form-item label="平台优势 JSON">
        <el-input v-model="advantagesJson" type="textarea" :rows="8" />
      </el-form-item>

      <el-form-item label="移动端提示 JSON">
        <el-input v-model="mobileTipsJson" type="textarea" :rows="5" />
      </el-form-item>

      <el-form-item label="服务承诺 JSON">
        <el-input v-model="servicePromiseJson" type="textarea" :rows="8" />
      </el-form-item>

      <el-form-item label="首页亮点 JSON">
        <el-input v-model="heroHighlightsJson" type="textarea" :rows="5" />
      </el-form-item>

      <el-form-item label="首页 KPI JSON">
        <el-input v-model="caseKpisJson" type="textarea" :rows="6" />
      </el-form-item>

      <el-form-item label="文章区标题">
        <el-input v-model="form.articlesSectionTitle" />
      </el-form-item>

      <el-form-item label="文章区副标题">
        <el-input v-model="form.articlesSectionSubtitle" />
      </el-form-item>

      <el-alert
        title="列表型字段仍使用 JSON 数组保存，请保持数据结构与前端类型一致。首页数字卡片已改为结构化编辑。"
        type="info"
        :closable="false"
        class="helper-alert"
      />

      <el-button type="primary" :loading="saving" @click="save">保存首页配置</el-button>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { HomeContentPayload, HomeMetricItem } from '@/types/cms'

const DEFAULT_HERO_METRICS: HomeMetricItem[] = [
  { value: '120+', label: '服务高校', detail: '覆盖重点院校、科研团队与成果运营机构' },
  { value: '3200+', label: '入库专利成果', detail: '持续沉淀专利、需求与评估报告数据' },
  { value: '40%', label: '匹配效率提升', detail: '加快检索、评估与成果转化推进节奏' }
]

const loading = ref(false)
const saving = ref(false)
const form = reactive<HomeContentPayload>({
  heroTitle: '',
  brandSlogan: '',
  heroDescription: '',
  coreFeaturesTitle: '',
  coreFeaturesIntro: '',
  heroMetrics: [],
  features: [],
  advantages: [],
  mobileOperationTips: [],
  mobileServicePromises: [],
  heroHighlights: [],
  caseKpis: [],
  articlesSectionTitle: '',
  articlesSectionSubtitle: ''
})

const featuresJson = ref('[]')
const advantagesJson = ref('[]')
const mobileTipsJson = ref('[]')
const servicePromiseJson = ref('[]')
const heroHighlightsJson = ref('[]')
const caseKpisJson = ref('[]')

const cloneMetrics = (items: HomeMetricItem[]) => items.map(item => ({ ...item }))

const syncJson = () => {
  featuresJson.value = JSON.stringify(form.features, null, 2)
  advantagesJson.value = JSON.stringify(form.advantages, null, 2)
  mobileTipsJson.value = JSON.stringify(form.mobileOperationTips, null, 2)
  servicePromiseJson.value = JSON.stringify(form.mobileServicePromises, null, 2)
  heroHighlightsJson.value = JSON.stringify(form.heroHighlights, null, 2)
  caseKpisJson.value = JSON.stringify(form.caseKpis, null, 2)
}

const applyDefaultHeroMetrics = () => {
  form.heroMetrics = cloneMetrics(DEFAULT_HERO_METRICS)
}

const addHeroMetric = () => {
  form.heroMetrics.push({ value: '', label: '', detail: '' })
}

const removeHeroMetric = (index: number) => {
  form.heroMetrics.splice(index, 1)
}

const load = async () => {
  loading.value = true
  try {
    Object.assign(form, await adminApi.getHomeContent())
    if (!Array.isArray(form.heroMetrics) || !form.heroMetrics.length) {
      applyDefaultHeroMetrics()
    }
    syncJson()
  } catch (error: any) {
    ElMessage.error(error.message || '加载首页配置失败')
  } finally {
    loading.value = false
  }
}

const save = async () => {
  saving.value = true
  try {
    form.features = JSON.parse(featuresJson.value)
    form.advantages = JSON.parse(advantagesJson.value)
    form.mobileOperationTips = JSON.parse(mobileTipsJson.value)
    form.mobileServicePromises = JSON.parse(servicePromiseJson.value)
    form.heroHighlights = JSON.parse(heroHighlightsJson.value)
    form.caseKpis = JSON.parse(caseKpisJson.value)
    await adminApi.updateHomeContent({ ...form, heroMetrics: cloneMetrics(form.heroMetrics) })
    ElMessage.success('首页配置已保存')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败，请检查 JSON 格式')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<style scoped>
.card-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-header span {
  color: #6b7280;
  font-size: 13px;
}

.content-form {
  display: flex;
  flex-direction: column;
}

.metric-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metric-editor-row {
  display: grid;
  grid-template-columns: 140px 180px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.metric-editor-actions {
  display: flex;
  gap: 12px;
}

.helper-alert {
  margin-bottom: 16px;
}
</style>
