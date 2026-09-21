import { DocumentChecked, User, EditPen, TrendCharts } from '@element-plus/icons-vue'
import { markRaw } from 'vue'
import type { FeatureItem } from '@/types'
import { PLATFORM_NAME } from '@/constants/brand'

export const homeCopy = {
  heroTitle: PLATFORM_NAME,
  brandSlogan: '让专利检索、需求对接与成果转化高效协同',
  heroDescription: '面向高校、科研团队与合作企业，提供专利查询、需求发布、价值评估、智能匹配与成果转化的一体化运营服务。',
  coreFeaturesTitle: '核心功能',
  coreFeaturesIntro: '聚焦知识产权运营全流程，把检索、评估、匹配和落地入口集中到一个工作台。'
}

export const homeFeatures: FeatureItem[] = [
  {
    icon: markRaw(DocumentChecked),
    title: '专利检索',
    description: '多维条件筛选与重点专利追踪，快速定位高价值技术成果。',
    detail: '支持按关键词、分类、申请人和状态等字段组合检索，帮助团队更快完成专利筛查与研判。',
    moreLabel: '查看详情',
    to: '/patent'
  },
  {
    icon: markRaw(User),
    title: '成果转化',
    description: '围绕技术需求、专家资源和专利成果建立转化链路。',
    detail: '将专利、需求和合作方资源联动展示，提升对接效率，缩短成果落地周期。',
    moreLabel: '查看详情',
    to: '/transformation'
  },
  {
    icon: markRaw(EditPen),
    title: '需求发布',
    description: '标准化录入技术需求，沉淀合作线索与转化机会。',
    detail: '通过统一模板发布技术需求，便于平台进行专利、专家和机构资源的后续匹配。',
    moreLabel: '查看详情',
    to: '/demand'
  },
  {
    icon: markRaw(TrendCharts),
    title: '价值评估',
    description: '从技术、市场与法律维度生成可参考的评估结论。',
    detail: '结合多维指标形成评估结果，为专利运营、合作谈判和成果交易提供决策支持。',
    moreLabel: '查看详情',
    to: '/evaluation'
  }
]
