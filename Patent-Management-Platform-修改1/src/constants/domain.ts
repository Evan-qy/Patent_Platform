import type { PatentBase, PatentCategory, RequirementStatus, ValuationDimensionKey } from '@/types'

export const PATENT_SEARCH_MAX_LENGTH = 100

export const PATENT_CATEGORY_OPTIONS: Array<{ label: string; value: PatentCategory }> = [
  { label: '风能', value: 'wind' },
  { label: '太阳能', value: 'solar' },
  { label: '生物质能', value: 'biomass' },
  { label: '氢能', value: 'hydrogen' },
  { label: '锂离子电池', value: 'lilon' }
]

export const TECH_DIRECTION_OPTIONS = [
  '新一代信息技术产业',
  '电子核心产业',
  '软件和信息技术服务业',
  '互联网与云计算、大数据服务',
  '人工智能产业',
  '新材料产业',
  '生物产业',
  '高端装备制造产业',
  '新能源与智能网联汽车产业'
] as const

export const COOPERATION_MODE_OPTIONS = ['技术开发', '成果转让', '联合攻关'] as const

export const REQUIREMENT_STATUS_TEXT: Record<RequirementStatus, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  COMPLETED: '已完成',
  FAILED: '失败'
}

export const REQUIREMENT_STATUS_TAG: Record<RequirementStatus, string> = {
  PENDING: 'info',
  PROCESSING: 'warning',
  COMPLETED: 'success',
  FAILED: 'danger'
}

export const PATENT_CATEGORY_TAG: Record<PatentCategory, string> = {
  wind: 'primary',
  solar: 'success',
  biomass: 'warning',
  hydrogen: 'danger',
  lilon: 'info'
}

const PATENT_CATEGORY_VALUES = new Set(PATENT_CATEGORY_OPTIONS.map(item => item.value))

export function normalizePatentCategory(category?: string) {
  const value = String(category || '').trim().toLowerCase()
  return PATENT_CATEGORY_VALUES.has(value) ? value : ''
}

export const VALUATION_DIMENSION_LABELS: Record<ValuationDimensionKey, string> = {
  technologicalInnovation: '技术创新',
  marketPotential: '市场潜力',
  legalStatus: '法律状态',
  economicValue: '经济价值'
}

export function getPatentCategoryText(category?: string) {
  return PATENT_CATEGORY_OPTIONS.find(item => item.value === category)?.label || category || '未分类'
}

export function getPatentCategoryTagType(category?: string) {
  return PATENT_CATEGORY_TAG[category as PatentCategory] || 'info'
}

export function getRequirementStatusText(status?: string) {
  return REQUIREMENT_STATUS_TEXT[status as RequirementStatus] || status || '未知状态'
}

export function getRequirementStatusTagType(status?: string) {
  return REQUIREMENT_STATUS_TAG[status as RequirementStatus] || 'info'
}

export function getValuationScoreColor(score: number) {
  if (score >= 90) return '#67C23A'
  if (score >= 75) return '#409EFF'
  if (score >= 60) return '#E6A23C'
  return '#F56C6C'
}

export function formatCurrency(amount: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0
  }).format(amount || 0)
}

export function formatWan(amount: number) {
  if (!amount) return '0 万'
  return `${(amount / 10000).toFixed(amount >= 100000 ? 0 : 1)} 万`
}

export function normalizeAiPatent(patent: Record<string, any>) {
  return {
    datasetId: patent.datasetId || patent.dataset_id,
    datasetCode: patent.datasetCode || patent.dataset_code || '',
    datasetName: patent.datasetName || patent.dataset_name || '',
    tableName: patent.tableName || patent.table_name || '',
    recordId: patent.recordId || patent.record_id || '',
    category: normalizePatentCategory(patent.category) as PatentCategory,
    publicNum: patent.publicNum || patent.public_num || '',
    title: patent.title || '',
    abstractText: patent.abstractText || patent.abstract_text || '',
    applicant: patent.applicant || '',
    inventor: patent.inventor || '',
    ipc: patent.ipc || '',
    cpc: patent.cpc || '',
    legalStatus: patent.legalStatus || patent.latestLegalStatus || patent.status || '',
    appliDate: patent.appliDate || '',
    publicDate: patent.publicDate || '',
    appliNum: patent.appliNum || '',
    patentDetails: patent.patentDetails || ''
  } satisfies PatentBase & { category: PatentCategory }
}

export function normalizeDatasetPatent(record: Record<string, any>) {
  return {
    datasetId: record.datasetId || record.dataset_id,
    datasetCode: record.datasetCode || record.dataset_code || '',
    datasetName: record.datasetName || record.dataset_name || '',
    tableName: record.tableName || record.table_name || '',
    recordId: String(record.recordId || record.record_id || record.id || record.ID || ''),
    category: normalizePatentCategory(String(record.category || record.patentCategory || record.patent_category || '')),
    publicNum: String(record.publicNum || record.public_num || record.patentPublicNum || record.patent_public_num || ''),
    title: String(record.title || record.patentTitle || record.patent_title || ''),
    abstractText: String(record.abstractText || record.abstract_text || record.abstract || ''),
    applicant: String(record.applicant || record.applyPerson || record.apply_person || ''),
    inventor: String(record.inventor || record.inventors || ''),
    ipc: String(record.ipc || record.ipcClass || record.ipc_class || ''),
    cpc: String(record.cpc || ''),
    legalStatus: String(record.legalStatus || record.latestLegalStatus || record.status || ''),
    appliDate: String(record.appliDate || record.appli_date || ''),
    publicDate: String(record.publicDate || record.public_date || ''),
    appliNum: String(record.appliNum || record.appli_num || ''),
    patentDetails: String(record.patentDetails || record.patent_details || '')
  } satisfies PatentBase
}
