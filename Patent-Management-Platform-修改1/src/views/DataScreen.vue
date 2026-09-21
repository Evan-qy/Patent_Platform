<template>
  <div class="data-screen-container">
    <header class="screen-header">
      <div class="header-side">{{ currentTime }}</div>
      <div class="header-center">
        <h1>高校知识产权运营数据监控中心</h1>
        <p>所有图表均来自当前数据库与 Elasticsearch 聚合结果</p>
      </div>
      <div class="header-side header-actions">
        <el-button text class="back-btn" @click="$router.push('/')">返回首页</el-button>
      </div>
    </header>

    <main class="screen-content">
      <section class="panel left-panel">
        <article class="panel-card">
          <div class="panel-title">
            <el-icon><PieChart /></el-icon>
            <span>专利分类分布</span>
          </div>
          <div ref="categoryChartRef" class="chart-container"></div>
        </article>
        <article class="panel-card">
          <div class="panel-title">
            <el-icon><TrendCharts /></el-icon>
            <span>公开专利与转化趋势</span>
          </div>
          <div ref="trendChartRef" class="chart-container"></div>
        </article>
      </section>

      <section class="panel center-panel">
        <div class="metrics-grid">
          <article v-for="item in metrics" :key="item.label" class="metric-card">
            <div class="metric-value">
              <span class="number">{{ item.value }}</span>
              <span class="unit">{{ item.unit }}</span>
            </div>
            <div class="metric-label">{{ item.label }}</div>
          </article>
        </div>

        <article class="panel-card ranking-card">
          <div class="panel-title">
            <el-icon><Histogram /></el-icon>
            <span>专利数据集活跃度</span>
          </div>
          <div ref="rankingChartRef" class="chart-container large-chart"></div>
        </article>
      </section>

      <section class="panel right-panel">
        <article class="panel-card">
          <div class="panel-title">
            <el-icon><UserFilled /></el-icon>
            <span>专家领域分布</span>
          </div>
          <div ref="expertChartRef" class="chart-container"></div>
        </article>

        <article class="panel-card transformation-card">
          <div class="panel-title">
            <el-icon><Money /></el-icon>
            <span>最新转化成果</span>
          </div>
          <div class="transformation-list">
            <div class="list-header">
              <span>项目名称</span>
              <span>转化金额</span>
              <span>时间</span>
            </div>
            <div class="list-body-wrapper">
              <ul class="list-body">
                <li v-for="item in transformationList" :key="item.id" class="list-row">
                  <span class="col-name" :title="item.title">{{ item.title }}</span>
                  <span class="col-amount">{{ item.amount }}</span>
                  <span class="col-time">{{ item.date }}</span>
                </li>
                <li v-if="!transformationList.length" class="empty-row">暂无转化数据</li>
              </ul>
            </div>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { init, use } from 'echarts/core'
import type { ECharts } from 'echarts/core'
import { PieChart as EchartsPieChart, LineChart, BarChart, RadarChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent, RadarComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { dashboardApi } from '@/api'
import type { DashboardScreenResponse } from '@/types'
import { Histogram, Money, PieChart, TrendCharts, UserFilled } from '@element-plus/icons-vue'

use([EchartsPieChart, LineChart, BarChart, RadarChart, TooltipComponent, LegendComponent, GridComponent, RadarComponent, CanvasRenderer])

const chartColors = {
  text: '#dfeeff',
  border: 'rgba(191, 232, 255, 0.18)',
  primary: '#59a2ff',
  cyan: '#37d1df',
  yellow: '#f6c55a',
  green: '#3ad29a',
  purple: '#9f8cff'
}

const currentTime = ref('')
const metrics = ref([
  { label: '专利总数', value: '--', unit: '件' },
  { label: '累计转化', value: '--', unit: '亿元' },
  { label: '入库专家', value: '--', unit: '位' },
  { label: '合作企业', value: '--', unit: '家' }
])
const transformationList = ref<Array<{ id: number; title: string; amount: string; date: string }>>([])

const categoryChartRef = ref<HTMLElement | null>(null)
const trendChartRef = ref<HTMLElement | null>(null)
const rankingChartRef = ref<HTMLElement | null>(null)
const expertChartRef = ref<HTMLElement | null>(null)

let timer: number | null = null
let chartInstances: ECharts[] = []
const state = ref<DashboardScreenResponse | null>(null)

const updateClock = () => {
  currentTime.value = new Date().toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const formatNumber = (value: number) => Number(value || 0).toLocaleString('zh-CN')
const formatTotalBenefit = (value?: number) => {
  const amount = Number(value || 0)
  if (!Number.isFinite(amount) || amount <= 0) return '0.0'
  return (amount / 100000000).toFixed(1)
}
const formatListAmount = (value?: number | null) => {
  const amount = Number(value || 0)
  if (!Number.isFinite(amount) || amount <= 0) return '--'
  if (amount >= 100000000) return `${(amount / 100000000).toFixed(2)} 亿元`
  if (amount >= 10000) return `${Math.round(amount / 10000)} 万元`
  return `${amount.toLocaleString('zh-CN')} 元`
}
const formatDate = (value?: string | null) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const fetchScreenData = async () => {
  const response = await dashboardApi.getScreenData()
  state.value = response
  metrics.value = [
    { label: '专利总数', value: formatNumber(response.metrics.patentTotal), unit: '件' },
    { label: '累计转化', value: formatTotalBenefit(response.metrics.transformationBenefitTotal), unit: '亿元' },
    { label: '入库专家', value: formatNumber(response.metrics.expertTotal), unit: '位' },
    { label: '合作企业', value: formatNumber(response.metrics.enterpriseTotal), unit: '家' }
  ]
  transformationList.value = response.latestTransformations.map(item => ({
    id: item.id,
    title: item.title,
    amount: formatListAmount(item.benefitAmount),
    date: formatDate(item.transformationDate)
  }))
}

const disposeCharts = () => {
  chartInstances.forEach(chart => chart.dispose())
  chartInstances = []
}

const initCategoryChart = () => {
  if (!categoryChartRef.value || !state.value) return
  const chart = init(categoryChartRef.value)
  chartInstances.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: chartColors.text } },
    series: [{
      type: 'pie',
      radius: ['44%', '72%'],
      center: ['50%', '45%'],
      itemStyle: { borderRadius: 10, borderColor: '#f4f8fc', borderWidth: 2 },
      label: { color: chartColors.text },
      data: state.value.patentCategories.map((item, index) => ({
        value: item.count,
        name: item.label,
        itemStyle: { color: [chartColors.primary, chartColors.cyan, chartColors.yellow, chartColors.green, chartColors.purple][index % 5] }
      }))
    }]
  })
}

const initTrendChart = () => {
  if (!trendChartRef.value || !state.value) return
  const chart = init(trendChartRef.value)
  chartInstances.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { color: chartColors.text } },
    grid: { top: 40, left: 20, right: 20, bottom: 20, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: state.value.patentTrend.map(item => item.month.slice(5)), axisLine: { lineStyle: { color: chartColors.border } }, axisLabel: { color: chartColors.text } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: chartColors.border } }, axisLabel: { color: chartColors.text } },
    series: [
      { name: '公开专利', type: 'line', smooth: true, data: state.value.patentTrend.map(item => item.patentCount), itemStyle: { color: chartColors.cyan }, areaStyle: { color: 'rgba(31, 184, 207, 0.18)' } },
      { name: '成果转化', type: 'line', smooth: true, data: state.value.patentTrend.map(item => item.transformationCount), itemStyle: { color: chartColors.primary }, areaStyle: { color: 'rgba(61, 125, 255, 0.15)' } }
    ]
  })
}

const initRankingChart = () => {
  if (!rankingChartRef.value || !state.value) return
  const chart = init(rankingChartRef.value)
  chartInstances.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { top: 20, left: 120, right: 16, bottom: 10, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: chartColors.border } }, axisLabel: { color: chartColors.text } },
    yAxis: { type: 'category', data: state.value.datasetRanking.map(item => item.datasetName), axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: chartColors.text } },
    series: [{ type: 'bar', data: state.value.datasetRanking.map(item => item.count), barWidth: 14, itemStyle: { borderRadius: [0, 8, 8, 0], color: chartColors.primary } }]
  })
}

const initExpertChart = () => {
  if (!expertChartRef.value || !state.value) return
  const chart = init(expertChartRef.value)
  chartInstances.push(chart)
  const values = state.value.expertFields.map(item => item.count)
  const max = Math.max(5, ...values)
  chart.setOption({
    tooltip: {},
    radar: {
      indicator: state.value.expertFields.map(item => ({ name: item.name, max })),
      splitLine: { lineStyle: { color: chartColors.border } },
      splitArea: { areaStyle: { color: ['rgba(255,255,255,0.1)', 'rgba(240,246,252,0.55)'] } },
      axisName: { color: chartColors.text }
    },
    series: [{ type: 'radar', data: [{ value: values, areaStyle: { color: 'rgba(126, 101, 255, 0.18)' }, lineStyle: { color: chartColors.purple }, itemStyle: { color: chartColors.purple } }] }]
  })
}

const initCharts = () => {
  disposeCharts()
  initCategoryChart()
  initTrendChart()
  initRankingChart()
  initExpertChart()
}

const handleResize = () => {
  chartInstances.forEach(chart => chart.resize())
}

onMounted(async () => {
  updateClock()
  timer = window.setInterval(updateClock, 1000)
  await fetchScreenData()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})
</script>

<style scoped>
.data-screen-container { min-height: 100vh; padding: 20px; background: radial-gradient(circle at top, rgba(71, 137, 255, 0.18), transparent 32%), linear-gradient(135deg, #08172d 0%, #10284c 42%, #0f5470 100%); color: #eaf6ff; }
.screen-header { display: grid; grid-template-columns: 220px 1fr 220px; align-items: center; gap: 16px; margin-bottom: 20px; }
.header-side { color: rgba(220, 238, 255, 0.72); font-size: 14px; }
.header-center { text-align: center; }
.header-center h1 { margin: 0; font-size: 34px; letter-spacing: 0.04em; }
.header-center p { margin: 6px 0 0; color: rgba(220, 238, 255, 0.72); }
.header-actions { display: flex; justify-content: flex-end; }
.back-btn { color: #d7eeff; }
.screen-content { display: grid; grid-template-columns: 1.05fr 1.2fr 1.05fr; gap: 20px; min-height: calc(100vh - 120px); }
.panel { min-width: 0; display: grid; gap: 20px; }
.left-panel, .right-panel { grid-template-rows: repeat(2, minmax(0, 1fr)); }
.center-panel { grid-template-rows: auto minmax(0, 1fr); }
.panel-card, .metric-card { background: rgba(9, 26, 50, 0.72); backdrop-filter: blur(18px); border: 1px solid rgba(191, 232, 255, 0.12); border-radius: 24px; box-shadow: 0 18px 46px rgba(3, 11, 28, 0.28); }
.panel-card { padding: 20px; min-height: 0; display: flex; flex-direction: column; }
.panel-title { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; font-size: 17px; font-weight: 700; }
.panel-title .el-icon { color: #6ecfff; }
.metrics-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.metric-card { padding: 18px 20px; }
.metric-value { display: flex; align-items: baseline; gap: 8px; margin-bottom: 8px; }
.metric-value .number { font-size: 34px; font-weight: 800; color: #f4fbff; }
.metric-value .unit, .metric-label, .col-time, .empty-row, .list-header { color: rgba(220, 238, 255, 0.72); }
.chart-container { flex: 1; min-height: 260px; }
.large-chart { min-height: 360px; }
.transformation-list { display: flex; flex-direction: column; min-height: 0; flex: 1; }
.list-header, .list-row { display: grid; grid-template-columns: minmax(0, 1.6fr) 0.9fr 0.8fr; gap: 12px; align-items: center; }
.list-header { padding: 0 0 12px; font-size: 12px; border-bottom: 1px solid rgba(191, 232, 255, 0.12); }
.list-body-wrapper { flex: 1; min-height: 0; overflow: auto; }
.list-body { list-style: none; margin: 0; padding: 0; }
.list-row { padding: 14px 0; border-bottom: 1px solid rgba(191, 232, 255, 0.1); font-size: 13px; }
.col-name { min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.col-amount { color: #54e0aa; font-weight: 700; }
.empty-row { padding: 24px 0; text-align: center; }
@media (max-width: 1200px) { .screen-content { grid-template-columns: 1fr; } .left-panel, .right-panel, .center-panel { grid-template-rows: unset; } }
@media (max-width: 768px) { .data-screen-container { padding: 14px; } .screen-header { grid-template-columns: 1fr; } .header-side, .header-actions { justify-content: center; text-align: center; } .header-center h1 { font-size: 26px; } .metrics-grid { grid-template-columns: 1fr; } .list-header, .list-row { grid-template-columns: minmax(0, 1.2fr) 0.9fr 0.9fr; } }
</style>
