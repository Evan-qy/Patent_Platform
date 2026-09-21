// 鍏ㄥ眬閫氱敤绫诲瀷
import type { ApiResponse, LoginCaptcha, LoginParams, PresenceStatus, PresenceUserSummary } from './auth'
export type { ApiResponse, LoginCaptcha, LoginParams, PresenceStatus, PresenceUserSummary } from './auth'
import type { AxiosRequestConfig } from 'axios'

// 棣栭〉妯″潡绫诲瀷
export interface FeatureItem {
  icon: any
  title: string
  description: string
  detail?: string
  moreLabel?: string
  to: string
}

export interface AdvantageItem {
  icon: any
  title: string
  description: string
}

export interface CaseItem {
  title: string
  status: string
  description: string
  company: string
  time: string
  amount?: string
  image?: string
}

// 鎵╁睍Axios璇锋眰閰嶇疆锛屾敮鎸佸彇娑堣姹?
export interface RequestConfig extends AxiosRequestConfig {
  AbortController ?: AbortController
  noCache?: boolean // 鏄惁绂佺敤缂撳瓨
  retryCount?: number // 閲嶈瘯娆℃暟
  noRetry?: boolean // 鏄惁绂佺敤閲嶈瘯
}

// 鍙栨秷璇锋眰token绫诲瀷
export type AxiosCancelToken = {
  Controller: AbortController
  url: string
}

// 閫氱敤鍝嶅簲绫诲瀷澶嶇敤
export type BaseApiResponse<T = any> = ApiResponse<T>

// AI鑱婂ぉ鐩稿叧绫诲瀷
export interface AiChatRequest {
  question: string
  model?: string
  temperature?: number
  maxTokens?: number
}

export interface PersistedAiChatRequest extends AiChatRequest {
  sessionId?: number
  historyLimit?: number
}

export interface AiChatResponse {
  code: number                       // 鍝嶅簲鐮?
  message: string                    // 鍝嶅簲娑堟伅
  data: {
    answer: string                   // AI鍥炵瓟
    model: string                    // 浣跨敤鐨勬ā鍨?
    requestId: string                // 璇锋眰ID
  }
}

// API鎷︽埅鍣ㄥ鐞嗗悗鐨凙I鑱婂ぉ鍝嶅簲绫诲瀷锛堣繑鍥炵殑鏄痙ata閮ㄥ垎锛?
export interface AiChatResponseData {
  answer: string                   // AI鍥炵瓟
  model: string                    // 浣跨敤鐨勬ā鍨?
  requestId: string                // 璇锋眰ID
}

// 鑱婂ぉ浼氳瘽绫诲瀷锛堝搴斿悗绔疌hatSession瀹炰綋锛?
export interface ChatSession {
  id: number                         // 浼氳瘽ID
  title: string                      // 浼氳瘽鏍囬
  createdAt: string                  // 鍒涘缓鏃堕棿锛圛SO鏍煎紡锛?
  updatedAt: string                  // 鏇存柊鏃堕棿锛圛SO鏍煎紡锛?
}

// 鑱婂ぉ娑堟伅绫诲瀷锛堝搴斿悗绔疌hatMessage瀹炰綋锛?
export interface ChatMessage {
  id: number                         // 娑堟伅ID
  sessionId: number                  // 浼氳瘽ID
  userId: number                     // 鐢ㄦ埛ID
  role: 'user' | 'assistant' | 'system' // 娑堟伅瑙掕壊
  content: string                     // 娑堟伅鍐呭
  createdAt: string                  // 鍒涘缓鏃堕棿锛圛SO鏍煎紡锛?
}

// 鍚庣杩斿洖鐨勮亰澶╂秷鎭搷搴旂被鍨嬶紙瀵瑰簲鍚庣ChatMessageResponse锛?
export interface ChatMessageResponse {
  id: number                         // 娑堟伅ID
  role: 'user' | 'assistant' | 'system' // 瑙掕壊
  content: string                     // 娑堟伅鍐呭
  createdAt: string                  // 鍒涘缓鏃堕棿锛圛SO鏍煎紡锛?
}

// 鍓嶇浣跨敤鐨勭畝鍖栨秷鎭被鍨嬶紙鐢ㄤ簬鐣岄潰鏄剧ず锛?
export interface UIMessage {
  id: string                         // 鍓嶇鐢熸垚鐨勪复鏃禝D
  role: 'user' | 'assistant' | 'system' // 娑堟伅瑙掕壊
  content: string                    // 娑堟伅鍐呭
  timestamp: number                  // 鏃堕棿鎴?
  backendId?: number                 // 鍚庣娑堟伅ID锛堜繚瀛樺悗鏇存柊锛?
}

// 鍓嶇浣跨敤鐨勭畝鍖栦細璇濈被鍨嬶紙鐢ㄤ簬鐣岄潰鏄剧ず锛?
export interface UISession {
  id: string                         // 鍓嶇鐢熸垚鐨勪复鏃禝D
  title: string                      // 浼氳瘽鏍囬
  messages: UIMessage[]              // 娑堟伅鍒楄〃
  createdAt: number                 // 鍒涘缓鏃堕棿鎴?
  updatedAt: number                  // 鏇存柊鏃堕棿鎴?
  backendId?: number                 // 鍚庣浼氳瘽ID锛堜繚瀛樺悗鏇存柊锛?
}

// 鍒涘缓浼氳瘽璇锋眰绫诲瀷锛堝搴斿悗绔疌hatCreateSessionRequest锛?
export interface ChatCreateSessionRequest {
  title?: string                     // 浼氳瘽鏍囬锛堝彲閫夛級
}

// 鍒涘缓浼氳瘽鍝嶅簲绫诲瀷锛堝搴斿悗绔疌hatSessionResponse锛?
export interface ChatSessionResponse {
  id: number                         // 浼氳瘽ID
  title: string                      // 浼氳瘽鏍囬
  createdAt: string                  // 鍒涘缓鏃堕棿锛圛SO鏍煎紡锛?
  updatedAt: string                  // 鏇存柊鏃堕棿锛圛SO鏍煎紡锛?
}

// 涓撳埄鍩虹淇℃伅绫诲瀷
export interface ChatAskResponse {
  sessionId: number
  answer: string
  model?: string
  requestId?: string
}

export interface PatentBase {
  datasetId?: number
  datasetCode?: string
  datasetName?: string
  tableName?: string
  recordId?: string
  category?: string
  publicNum: string           // 鍏紑鍙凤紙涓婚敭锛?
  legalStatus?: string        // 娉曞緥鐘舵€?
  latestLegalStatus?: string  // 鏈€鏂版硶寰嬬姸鎬?
  status?: string             // 鐘舵€?
  title: string               // 鏍囬
  type?: string               // 绫诲瀷
  abstractText: string        // 鎽樿
  appliNum?: string           // 鐢宠鍙?
  appliDate?: string          // 鐢宠鏃ユ湡
  publicDate?: string         // 鍏紑鏃ユ湡
  applicant: string           // 鐢宠浜?
  applicantAddress?: string   // 鐢宠浜哄湴鍧€
  patentee?: string           // 涓撳埄鏉冧汉
  patenteeAddress?: string    // 涓撳埄鏉冧汉鍦板潃
  inventor: string            // 鍙戞槑浜?
  agent?: string              // 浠ｇ悊浜?
  ipc: string                 // 鍥介檯涓撳埄鍒嗙被
  cpc?: string                // 鍏卞悓涓撳埄鍒嗙被
  nec?: string                // 鍥藉/鍦板尯浠ｇ爜
  patentDetails?: string      // 涓撳埄璇︽儏
}

// 涓撳埄绫诲埆鏋氫妇
export interface PatentEsHit extends PatentBase {
  score?: number
  highlightTitle?: string
  highlightAbstractText?: string
  highlightPatentDetails?: string
}

export interface PatentEsDetailResponse extends PatentBase {
  rawRecord?: Record<string, unknown>
}

export interface PatentEsSummaryCategoryBucket {
  category: string
  label: string
  count: number
}

export interface PatentEsSummaryDatasetBucket {
  datasetId: number
  datasetCode: string
  datasetName: string
  tableName: string
  category?: string
  count: number
}

export interface PatentEsSummaryResponse {
  total: number
  categories: PatentEsSummaryCategoryBucket[]
  datasets: PatentEsSummaryDatasetBucket[]
}

export interface DashboardMetricSummary {
  patentTotal: number
  transformationBenefitTotal: number
  expertTotal: number
  enterpriseTotal: number
}

export interface DashboardPatentCategoryItem {
  category: string
  label: string
  count: number
}

export interface DashboardTrendItem {
  month: string
  patentCount: number
  transformationCount: number
}

export interface DashboardDatasetRankingItem {
  datasetId: number
  datasetCode: string
  datasetName: string
  count: number
}

export interface DashboardExpertFieldItem {
  name: string
  count: number
}

export interface DashboardTransformationItem {
  id: number
  title: string
  benefitAmount?: number | null
  transformationDate?: string | null
}

export interface DashboardScreenResponse {
  generatedAt: string
  metrics: DashboardMetricSummary
  patentCategories: DashboardPatentCategoryItem[]
  patentTrend: DashboardTrendItem[]
  datasetRanking: DashboardDatasetRankingItem[]
  expertFields: DashboardExpertFieldItem[]
  latestTransformations: DashboardTransformationItem[]
}

export type PatentCategory = string
export type RequirementStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
export type ValuationDimensionKey = 'technologicalInnovation' | 'marketPotential' | 'legalStatus' | 'economicValue'

// 涓撳埄鏌ヨ鍙傛暟
export interface PatentQueryParams {
  category?: string  // 鍚庣鎺ュ彛涓璫ategory鏄彲閫夊弬鏁?
  query?: string
  page?: number
  size?: number
}

// 涓撳埄鍒涘缓/鏇存柊鍙傛暟
export interface PatentCreateUpdateParams extends PatentBase {
  category: PatentCategory
}

export interface PatentDataset {
  id: number
  code: string
  name: string
  category?: string
  tableName?: string
  enabled?: boolean
}

export interface PatentCategoryOption {
  value: string
  label: string
  datasetIds: number[]
}

export interface PatentDatasetSearchResponse {
  content: Array<Record<string, unknown>>
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

// 涓撳淇℃伅绫诲瀷
export interface Expert {
  id: number          // 涓撳ID
  name: string        // 濮撳悕
  field: string       // 棰嗗煙
  expertise: string   // 涓撻暱
  achievements?: string // 鎴愭灉
  contactInfo?: string  // 鑱旂郴鏂瑰紡
}

// 涓撳鏌ヨ鍙傛暟
export interface ExpertQueryParams {
  query?: string
}

// 闇€姹備俊鎭被鍨?
export interface Requirement {
  id: number
  title: string
  description?: string
  keywords?: string
  techDirection?: string   // 鎶€鏈柟鍚?
  cooperationMode?: string   // 鍚堜綔妯″紡
  contactInfo?: string     // 鑱旂郴鏂瑰紡
  budget?: number | string
  deadline?: string
  status: RequirementStatus
  createdDate: string
  createdBy?: string        // 鍒涘缓鑰?
  updatedDate?: string      // 鏇存柊鏃堕棿
}

// 鍒涘缓闇€姹傚弬鏁?
export interface CreateRequirementParams {
  title: string
  description?: string
  keywords?: string
  budget?: number
  deadline?: string
  techDirection?: string
  cooperationMode?: string
  contactInfo?: string     // 鑱旂郴鏂瑰紡
}

// 涓汉涓撳埄淇℃伅绫诲瀷
export interface UserPatent {
  id: number
  category: string
  title: string
  publicNum?: string
  abstractText?: string
  ipc?: string
  cpc?: string
  applicant?: string
  inventor?: string
  visibility: 'PUBLIC' | 'PRIVATE'
  ownerUserId: number
  createdAt: string
}

// 鍒涘缓涓汉涓撳埄鍙傛暟
export interface CreateUserPatentParams {
  category: string
  title: string
  publicNum?: string
  abstractText?: string
  ipc?: string
  cpc?: string
  applicant?: string
  inventor?: string
  visibility?: 'PUBLIC' | 'PRIVATE'
}

// 鏇存柊涓汉涓撳埄鍙傛暟
export interface UpdateUserPatentParams {
  category?: string
  title?: string
  publicNum?: string
  abstractText?: string
  ipc?: string
  cpc?: string
  applicant?: string
  inventor?: string
  visibility?: 'PUBLIC' | 'PRIVATE'
}

// 涓汉涓撳埄鏌ヨ鍙傛暟
export interface UserPatentQueryParams {
  owner?: string  // 'me' 鏌ョ湅鎴戠殑涓撳埄锛屼笉浼犳煡鐪嬫墍鏈夊叕寮€涓撳埄
  category?: string
  query?: string
  visibility?: 'PUBLIC' | 'PRIVATE'
  page?: number
  size?: number
}

// 鍖归厤涓撳埄缁撴灉绫诲瀷
export interface MatchedPatent {
  datasetId?: number
  recordId?: string
  category: PatentCategory
  publicNum: string
  title: string
  applicant: string
  inventor: string
  userPatentId?: number
}

// AI鏅鸿兘鍖归厤涓撳埄鍝嶅簲绫诲瀷
export interface AIMatchedPatentResponse {
  requestId: string
  aiAnalysis: string
  extractedPublicNums: string[]
  notFoundPublicNums: string[]
  patents: Array<{
    datasetId?: number
    recordId?: string
    category: PatentCategory
    publicNum: string
    title: string
    abstractText?: string
  }>
}

export interface AiPatentSearchPatent extends PatentBase {
  category: PatentCategory
}

export interface AiPatentSearchResponse {
  requestId: string
  aiAnalysis: string
  extractedPublicNums: string[]
  notFoundPublicNums: string[]
  patents: AiPatentSearchPatent[]
}

// 鍖归厤涓撳缁撴灉绫诲瀷
export interface MatchedExpert {
  id: number
  name: string
  field: string
}

// 涓撳埄鍖归厤鎸佷箙鍖栧弬鏁?
export interface PersistPatentMatchParams {
  items: Array<{
    patentCategory: PatentCategory
    patentPublicNum: string
    datasetId?: number
    recordId?: string
    matchScore?: number
    matchReason?: string
  }>
}

// 鍒嗛〉瀵硅薄绫诲瀷
export interface Pageable<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
  pageable?: {
    pageNumber: number
    pageSize: number
    offset: number
    paged: boolean
    unpaged: boolean
  }
}

// 涓撳鍖归厤鎸佷箙鍖栧弬鏁?
export interface PersistExpertMatchParams {
  items: Array<{
    expertId: number
    matchScore?: number
    matchReason?: string
  }>
}

// 杞寲鎴愭灉淇℃伅绫诲瀷
export interface TransformationResult {
  id: number
  patentSource?: 'USER' | 'EXTERNAL'
  datasetId?: number
  recordId?: string
  patentCategory: PatentCategory
  patentPublicNum: string
  userPatentId?: number
  expertUserId?: number
  requirementId?: number
  partnerOrgId?: number
  description?: string
  transformationDate?: string
  status: string
  benefitAmount?: number
}

// 鍒涘缓杞寲鎴愭灉鍙傛暟
export interface CreateTransformationParams {
  patentSource?: 'USER' | 'EXTERNAL'
  datasetId?: number
  recordId?: string
  patentCategory?: PatentCategory
  patentPublicNum?: string
  userPatentId?: number
  expertUserId?: number
  requirementId?: number
  partnerOrgId?: number
  description?: string
  transformationDate?: string
  status?: string
  benefitAmount?: number
}

// 涓撳埄浠峰€艰瘎浼版姤鍛婄被鍨?
export interface ValuationReport {
  id: number
  patentSource?: 'USER' | 'EXTERNAL'
  datasetId?: number
  recordId?: string
  userPatentId?: number
  patentCategory: PatentCategory
  patentPublicNum: string
  reportTitle: string
  // 鏃у瓧娈靛吋瀹?
  valuationAmount?: number
  // 鏂板瓧娈?
  predictedValue: number
  currency: string
  valuationDate: string
  modelVersion: string
  // 缁煎悎璇勫垎
  comprehensiveScore: number
  // 缁村害寰楀垎璇︽儏锛堟柊缁撴瀯锛?
  dimensionScores: {
    technologicalInnovation: {
      score: number
      fullScore: number
      analysis: string
      keyPoints: string[]
    }
    marketPotential: {
      score: number
      fullScore: number
      analysis: string
      keyPoints: string[]
    }
    legalStatus: {
      score: number
      fullScore: number
      analysis: string
      keyPoints: string[]
    }
    economicValue: {
      score: number
      fullScore: number
      analysis: string
      keyPoints: string[]
    }
  }
  // 鍏煎鏃у瓧娈碉紙鍙€夛級
  scoreDetails?: {
    technologicalInnovation: number
    marketPotential: number
    legalStatus: number
    economicValue: number
  }
  // 缁撹
  conclusion: {
    summary: string
    strengths: string[]
    risks: string[]
    recommendations: string[]
    suggestion: string
  }
  // 涓撳埄淇℃伅蹇収
  patentInfo: {
    publicNum: string
    title: string
    applicant: string
    inventor: string
    abstract: string
    ipcClass: string
  }
  // 鏉冮噸閰嶇疆蹇収
  weights: {
    technologicalInnovation: number
    marketPotential: number
    legalStatus: number
    economicValue: number
  }
  evaluationMethod?: string
  evaluator?: string
  status?: string
  reportContent?: string
  createdAt?: string
  updatedAt?: string
  reportTime?: string
}

// 鐢熸垚璇勪及鎶ュ憡鍙傛暟
export interface GenerateValuationParams {
  patentSource?: 'USER' | 'EXTERNAL'
  datasetId?: number
  recordId?: string
  userPatentId?: number
  patentCategory?: PatentCategory
  patentPublicNum?: string
  modelVersion?: string
  weights?: {
    technologicalInnovation: number
    marketPotential: number
    legalStatus: number
    economicValue: number
  }
  additionalInfo?: {
    title?: string
    abstract?: string
    applicant?: string
    inventor?: string
    ipcClass?: string
  }
}

// 鏌ヨ璇勪及鎶ュ憡鍒楄〃鍙傛暟
export interface QueryValuationParams {
  patentSource?: 'USER' | 'EXTERNAL'
  patentCategory?: PatentCategory
  patentPublicNum?: string
  userPatentId?: number
  datasetId?: number
  recordId?: string
}

// 鏇存柊璇勪及妯″瀷鍙傛暟
export interface UpdateValuationParam {
  key: string
  value: string | number | boolean
  description?: string
}

export interface UserAccount {
  id: number
  username: string
  phone?: string
  email?: string
}

export interface UserProfile {
  userId: number
  nickname?: string
  avatarUrl?: string | null
  realName?: string | null
  idNumber?: string | null
}

export interface AvatarUploadResponse {
  url: string
}

// 涓撳璧勬枡绫诲瀷
export interface ExpertProfile {
  id: number
  userId: number
  name: string
  field: string
  expertise: string
  achievements?: string
  contactInfo?: string
  status: string
  createdAt: string
  updatedAt: string
}

// 鏈烘瀯淇℃伅绫诲瀷
export interface Organization {
  id: number
  name: string
  type: string
  description?: string
  address?: string
  contactInfo?: string
  status: string
  createdAt: string
  updatedAt: string
}

// 瀹屾暣鐢ㄦ埛淇℃伅绫诲瀷
export interface FullUserInfo {
  user: UserAccount
  profile: UserProfile | null
  expertProfile?: ExpertProfile | null
  primaryOrganization?: Organization | null
}

// 鏇存柊鐢ㄦ埛璧勬枡鍙傛暟
export interface UpdateUserProfileParams {
  nickname?: string
  avatarUrl?: string | null
  realName?: string | null
  idNumber?: string | null
  phone?: string | null
  email?: string | null
}
