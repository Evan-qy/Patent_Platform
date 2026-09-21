export interface HomeMetricItem {
  value: string
  label: string
  detail?: string
}

export interface HomeFeatureItem {
  icon?: string
  title: string
  description: string
  detail?: string
  moreLabel?: string
  to?: string
}

export interface HomeAdvantageItem {
  icon?: string
  title: string
  description: string
}

export interface HomeServicePromiseItem {
  icon?: string
  title: string
  text: string
}

export interface HomeKpiItem {
  label: string
  value: string
}

export interface HomeContentPayload {
  heroTitle: string
  brandSlogan: string
  heroDescription: string
  coreFeaturesTitle: string
  coreFeaturesIntro: string
  heroMetrics: HomeMetricItem[]
  features: HomeFeatureItem[]
  advantages: HomeAdvantageItem[]
  mobileOperationTips: string[]
  mobileServicePromises: HomeServicePromiseItem[]
  heroHighlights: string[]
  caseKpis: HomeKpiItem[]
  articlesSectionTitle: string
  articlesSectionSubtitle: string
}

export interface CmsArticle {
  id: number
  slug: string
  title: string
  summary?: string
  coverUrl?: string
  contentMd?: string
  status: 'DRAFT' | 'PUBLISHED'
  sortOrder: number
  publishedAt?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface AdminProfile {
  id: number
  username: string
  displayName?: string
  status: string
}

export interface AdminLoginResponse {
  token: string
  admin: AdminProfile
}

export interface PatentDataSource {
  id: number
  code: string
  name: string
  sourceType: 'PRIMARY' | 'EXTERNAL_MYSQL'
  host?: string
  port?: number
  databaseName?: string
  username?: string
  password?: string
  driverClassName?: string
  jdbcUrl?: string
  enabled: boolean
  builtIn: boolean
  remarks?: string
}

export interface PatentDataset {
  id: number
  dataSourceId: number
  code: string
  name: string
  category?: string
  tableName: string
  primaryKeyColumn: string
  publicNumColumn?: string
  titleColumn?: string
  abstractColumn?: string
  applicantColumn?: string
  inventorColumn?: string
  ipcColumn?: string
  cpcColumn?: string
  appliDateColumn?: string
  publicDateColumn?: string
  legalStatusColumn?: string
  statusColumn?: string
  defaultSortColumn?: string
  defaultSortDirection?: 'ASC' | 'DESC'
  searchFieldsJson?: string
  enabled: boolean
  builtIn: boolean
  remarks?: string
}

export interface TableInfo {
  name: string
}

export interface TableColumnInfo {
  name: string
  type: string
  size?: number
}

export interface SqlExecuteResponse {
  statementType: string
  affectedRows?: number
  columns: string[]
  rows: Array<Record<string, unknown>>
  durationMs: number
}

export interface AdminSqlExecution {
  id: number
  adminId: number
  dataSourceId: number
  statementType: string
  sqlText: string
  successFlag: boolean
  affectedRows?: number
  resultPreview?: string
  errorMessage?: string
  durationMs: number
  executedAt: string
}

export interface PagedMapResponse {
  content: Array<Record<string, unknown>>
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface PatentEsStatusResponse {
  indexName: string
  exists: boolean
  docCount: number
  enabledDatasetCount: number
}

export interface PatentEsReindexRequest {
  datasetIds?: number[]
}

export interface AuditLogRecord {
  id: number
  userId?: number | null
  username?: string | null
  displayName?: string | null
  roleName?: string | null
  eventType?: string | null
  action: string
  resourceType?: string | null
  resourceId?: string | null
  detail?: string | null
  requestMethod?: string | null
  requestPath?: string | null
  ipAddress?: string | null
  macAddress?: string | null
  hostName?: string | null
  location?: string | null
  userAgent?: string | null
  platformType?: string | null
  operationResult?: string | null
  createdAt: string
}
