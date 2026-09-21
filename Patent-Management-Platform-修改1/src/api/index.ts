import axios, {
  AxiosInstance,
  AxiosResponse,
  CancelTokenSource,
  isCancel
} from 'axios'
import { ElMessage as Message, ElMessageBox } from 'element-plus'
import router from '../router'
import { getApiErrorMessage, isServiceUnavailableError } from '../lib/api-errors'
import { resolveApiBaseUrl } from '../lib/runtime'
import type { ApiResponse, AiChatRequest, AiChatResponse, AiChatResponseData, AiPatentSearchResponse, AvatarUploadResponse, ChatAskResponse, ChatCreateSessionRequest, ChatMessage, ChatMessageResponse, ChatSession, ChatSessionResponse, CreateRequirementParams, CreateTransformationParams, CreateUserPatentParams, DashboardScreenResponse, Expert, ExpertQueryParams, FullUserInfo, GenerateValuationParams, LoginCaptcha, MatchedExpert, MatchedPatent, Pageable, PatentBase, PatentCategory, PatentCategoryOption, PatentDataset, PatentDatasetSearchResponse, PatentEsDetailResponse, PatentEsHit, PatentEsSummaryResponse, PatentQueryParams, PersistedAiChatRequest, PersistExpertMatchParams, PersistPatentMatchParams, PresenceStatus, QueryValuationParams, RequestConfig, Requirement, TransformationResult, UpdateUserPatentParams, UpdateUserProfileParams, UpdateValuationParam, UserPatent, UserPatentQueryParams, UserProfile, ValuationReport } from '../types'

// 鍙栨秷璇锋眰token缂撳瓨
const cancelTokenMap = new Map<string, CancelTokenSource>()
// 璇锋眰缂撳瓨
const requestCache = new Map<string, any>()
// 鐜鍙橀噺涓殑鎺ュ彛鍦板潃
const BASE_URL = resolveApiBaseUrl()

const isApiEnvelope = (value: unknown): value is ApiResponse => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return false
  }
  return Object.prototype.hasOwnProperty.call(value, 'code')
}

const VALID_PATENT_CATEGORIES = new Set(['wind', 'solar', 'biomass', 'hydrogen', 'lilon'])

const normalizePatentCategory = (value?: string) => {
  const category = String(value || '').trim().toLowerCase()
  return VALID_PATENT_CATEGORIES.has(category) ? category : undefined
}

// 鍝嶅簲鏁版嵁绫诲瀷宸蹭粠 ../types 瀵煎叆

// 鍏峵oken鎺ュ彛鐧藉悕鍗?
const NO_TOKEN_WHITELIST = [
  '/auth/login',
  '/auth/register',
  '/auth/captcha',
  '/public/presence',
  '/public/presence/heartbeat'
]

// 妫€鏌oken鏄惁鍗冲皢杩囨湡锛堝湪杩囨湡鍓?鍒嗛挓鎻愰啋锛?
const isTokenExpiringSoon = (token: string): boolean => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000 // 杞崲涓烘绉?
    const now = Date.now()
    const fiveMinutes = 5 * 60 * 1000
    return (exp - now) < fiveMinutes
  } catch (error) {
    console.warn('鏃犳硶瑙ｆ瀽token杩囨湡鏃堕棿:', error)
    return false
  }
}

// 妫€鏌oken鏄惁宸茶繃鏈?
const isTokenExpired = (token: string): boolean => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000 // 杞崲涓烘绉?
    return Date.now() > exp
  } catch (error) {
    console.warn('鏃犳硶瑙ｆ瀽token杩囨湡鏃堕棿:', error)
    return true
  }
}

// token鐘舵€佺紦瀛橈紝閬垮厤棰戠箒鎻愮ず
let lastTokenState = {
  token: '',
  isExpiringSoon: false,
  isExpired: false,
  lastChecked: 0
}

// 妫€鏌oken鐘舵€侊紝鍙湪鐘舵€佸彉鍖栨椂鎻愮ず
const checkTokenState = (token: string): { isExpiringSoon: boolean; isExpired: boolean } => {
  const now = Date.now()
  const isExpiringSoon = isTokenExpiringSoon(token)
  const isExpired = isTokenExpired(token)
  
  // 濡傛灉token鐩稿悓涓?绉掑唴妫€鏌ヨ繃锛岀洿鎺ヨ繑鍥炵紦瀛樼姸鎬?
  if (lastTokenState.token === token && 
      now - lastTokenState.lastChecked < 5000) {
    return {
      isExpiringSoon: lastTokenState.isExpiringSoon,
      isExpired: lastTokenState.isExpired
    }
  }
  
  // 鏇存柊缂撳瓨
  lastTokenState = {
    token,
    isExpiringSoon,
    isExpired,
    lastChecked: now
  }
  
  return { isExpiringSoon, isExpired }
}

class ApiService {
  private axiosInstance: AxiosInstance

  constructor() {
    this.axiosInstance = axios.create({
      baseURL: BASE_URL,
      timeout: 180000,
      headers: {
        'Content-Type': 'application/json'
      }
    })

    // 璇锋眰鎷︽埅鍣?
    this.axiosInstance.interceptors.request.use(
      (config) => {
        const typedConfig = config as RequestConfig
        // 鍙栨秷閲嶅璇锋眰
        const requestKey = ApiService.getRequestKey(typedConfig)
        if (cancelTokenMap.has(requestKey)) {
          cancelTokenMap.get(requestKey)?.cancel('重复请求已取消')
          cancelTokenMap.delete(requestKey)
        }
        // 鍒涘缓鍙栨秷token
        const source = axios.CancelToken.source()
        typedConfig.cancelToken = source.token
        cancelTokenMap.set(requestKey, source)

        // 妫€鏌ユ槸鍚︿负鍏峵oken鎺ュ彛
        const isNoTokenApi = NO_TOKEN_WHITELIST.some(api => config.url?.includes(api))
        
        // 濡傛灉鏄厤token鎺ュ彛锛岀洿鎺ヨ繑鍥為厤缃紝涓嶈繘琛屼换浣晅oken鐩稿叧澶勭悊
        if (isNoTokenApi) {
          return config
        }
        
        // 涓篈I鑱婂ぉ鎺ュ彛璁剧疆鏇撮暱鐨勮秴鏃舵椂闂达紙180绉掞級
        if (config.url?.includes('/ai/chat')) {
          // 鍒涘缓鏂扮殑閰嶇疆瀵硅薄锛岄伩鍏嶈鍏ㄥ眬閰嶇疆瑕嗙洊
          const newConfig = { ...config }
          newConfig.timeout = 180000 // 180绉?
          return newConfig
        }
        
        // 涓洪渶姹傚尮閰嶄笓鍒╂帴鍙ｈ缃洿闀跨殑瓒呮椂鏃堕棿锛?80绉掞級
        if (config.url?.includes('/requirements/') && config.url?.includes('/match-patents')) {
          // 鍒涘缓鏂扮殑閰嶇疆瀵硅薄锛岄伩鍏嶈鍏ㄥ眬閰嶇疆瑕嗙洊
          const newConfig = { ...config }
          newConfig.timeout = 180000 // 180绉?
          return newConfig
        }
        
        // 娣诲姞token锛堥潪鍏峵oken鎺ュ彛鎵嶉渶瑕侊級
        if (!typedConfig.headers) {
          typedConfig.headers = {}
        }
        
        const token = localStorage.getItem('token')
        
        // 鍏抽敭锛氬彧鍦╰oken鏄瓧绗︿覆鏃舵墠璁剧疆璇锋眰澶?
        if (token && typeof token === 'string') {
            // 妫€鏌oken鐘舵€侊紙浣跨敤缂撳瓨鏈哄埗閬垮厤棰戠箒鎻愮ず锛?
            const tokenState = checkTokenState(token)
            
            if (tokenState.isExpired) {
              // token宸茶繃鏈燂紝娓呴櫎token骞舵彁绀洪噸鏂扮櫥褰?
              localStorage.removeItem('token')
              if (router.currentRoute.value.path !== '/login') {
                Message.warning('登录已过期，请重新登录')
                router.push('/login')
              }
              return Promise.reject(new Error('Token已过期'))
            } else if (tokenState.isExpiringSoon && 
                      (lastTokenState.token !== token || !lastTokenState.isExpiringSoon)) {
              // token鍗冲皢杩囨湡锛屼笖鐘舵€佸彂鐢熷彉鍖栨椂鎵嶆彁绀?
              Message.info('登录状态即将过期，建议保存工作后重新登录')
            }
            
            typedConfig.headers.Authorization = `Bearer ${token}`
          } else {
            // token鏃犳晥鏃讹紝娓呴櫎骞舵彁绀?
            localStorage.removeItem('token')
          }
        return config
      },
      (error) => {
        return Promise.reject(error)
      }
    )

    // 鍝嶅簲鎷︽埅鍣?
    this.axiosInstance.interceptors.response.use(
      (response: AxiosResponse<ApiResponse | any>) => {
        // 绉婚櫎鍙栨秷token
        const requestKey = ApiService.getRequestKey(response.config as RequestConfig)
        cancelTokenMap.delete(requestKey)

        const res = response.data
        if (!isApiEnvelope(res)) {
          if (!(response.config as RequestConfig).noCache) {
            requestCache.set(requestKey, res)
          }
          return res
        }
        // 涓氬姟閫昏緫閿欒澶勭悊
        if (res.code !== 0) {
          // 涓氬姟閫昏緫灞傞潰鐨?01閿欒锛堝悗绔繑鍥炵殑code涓?01锛?
          if (res.code === 401) {
            // 鍙湪鐢ㄦ埛褰撳墠椤甸潰闇€瑕佺櫥褰曟椂鎵嶆彁绀?
            if (router.currentRoute.value.path !== '/login') {
              ElMessageBox.confirm(
                '鐧诲綍鐘舵€佸凡澶辨晥锛岃閲嶆柊鐧诲綍',
                '鎻愮ず',
                {
                  confirmButtonText: '閲嶆柊鐧诲綍',
                  cancelButtonText: '鍙栨秷',
                  type: 'warning'
                }
              ).then(() => {
                  localStorage.removeItem('token')
                  router.push('/login')
              })
            }
            return Promise.reject(new Error('登录状态失效'))
          }
          // 涓氬姟閫昏緫灞傞潰鐨?03閿欒
          if (res.code === 403) {
            Message.error('鏃犳潈闄愭墽琛屾鎿嶄綔')
            return Promise.reject(new Error('鏃犳潈闄愭墽琛屾鎿嶄綔'))
          }
          const message = getApiErrorMessage({ message: res.message }, '请求失败')
          Message.error(message)
          return Promise.reject(new Error(message))
        }
        // 缂撳瓨璇锋眰缁撴灉锛堝鏋滈厤缃簡缂撳瓨锛?
        if (!(response.config as RequestConfig).noCache) {
          requestCache.set(requestKey, res.data)
        }
        return res.data
      },
      async (error) => {
        // 绉婚櫎鍙栨秷token
        const requestKey = ApiService.getRequestKey(error.config as RequestConfig)
        cancelTokenMap.delete(requestKey)

        // 澶勭悊鍙栨秷璇锋眰
        if (isCancel(error)) {
          console.log('璇锋眰宸插彇娑?', error.message)
          return Promise.reject(error)
        }

        // HTTP鐘舵€佺爜閿欒澶勭悊
        if (error.response?.status === 500) {
          // 500閿欒锛氭湇鍔″櫒鍐呴儴閿欒锛岀洿鎺ユ嫆缁濓紝涓嶉噸璇?
          Message.error('服务器内部错误，请稍后重试')
          return Promise.reject(error)
        } else if (error.response?.status === 403) {
          // 403閿欒锛氭棤鏉冮檺璁块棶
          if (router.currentRoute.value.path !== '/login') {
            Message.error('无权限访问，请确认登录状态')
            localStorage.removeItem('token')
            router.push('/login')
          }
          return Promise.reject(error)
        } else if (error.response?.status === 401) {
          // 401閿欒锛氱櫥褰曞凡杩囨湡
          if (router.currentRoute.value.path !== '/login') {
            Message.error('登录已过期，请重新登录')
            localStorage.removeItem('token')
            router.push('/login')
          }
          return Promise.reject(error)
        }

        // 閫氱敤缃戠粶閿欒鎻愮ず锛堝叾浠栭敊璇篃涓嶉噸璇曪級
        if (isServiceUnavailableError(error)) {
          Message.error(getApiErrorMessage(error, '服务暂时不可用，请稍后重试'))
        } else {
          Message.error(getApiErrorMessage(error, '网络异常，请检查网络连接'))
        }
        return Promise.reject(error)
      }
    )
  }

  // 鐢熸垚璇锋眰鍞竴鏍囪瘑锛堥潤鎬佹柟娉曪紝鍙湪鎷︽埅鍣ㄤ腑浣跨敤锛?
  private static getRequestKey(config: RequestConfig): string {
    // 瀹夊叏澶勭悊锛氭鏌onfig鏄惁涓簎ndefined鎴杗ull
    if (!config) {
      return 'unknown-request'
    }
    
    const { method, url, params, data } = config
    return [method, url, JSON.stringify(params), JSON.stringify(data)].join('-')
  }

  // 娓呴櫎璇锋眰缂撳瓨
  public clearCache(): void {
    requestCache.clear()
  }

  // GET璇锋眰
  get<T = any>(url: string, params?: any, config?: RequestConfig): Promise<T> {
    const requestKey = ApiService.getRequestKey({ method: 'get', url, params })
    // 浼樺厛璇诲彇缂撳瓨
    if (requestCache.has(requestKey) && !(config?.noCache)) {
      return Promise.resolve(requestCache.get(requestKey))
    }
    return this.axiosInstance.get(url, { params, ...config })
  }

  // POST璇锋眰
  post<T = any>(url: string, data?: any, config?: RequestConfig): Promise<T> {
    // POST璇锋眰榛樿涓嶇紦瀛?
    config = { ...config, noCache: true }
    return this.axiosInstance.post(url, data, config)
  }

  // PUT璇锋眰
  put<T = any>(url: string, data?: any, config?: RequestConfig): Promise<T> {
    config = { ...config, noCache: true }
    return this.axiosInstance.put(url, data, config)
  }

  // DELETE璇锋眰
  delete<T = any>(url: string, params?: any, config?: RequestConfig): Promise<T> {
    config = { ...config, noCache: true }
    return this.axiosInstance.delete(url, { params, ...config })
  }



  // 鏌ヨ涓撳
  getExperts(query?: string) {
    const params: { query?: string } = {}
    if (query) {
      params.query = query
    }
    return this.get<Expert[]>('/experts', params)
  }

  // 鍒涘缓涓撳
  createExpert(expertData: Omit<Expert, 'id'>) {
    return this.post<Expert>('/experts', expertData)
  }

  // 鍒涘缓闇€姹?
  createRequirement(requirementData: CreateRequirementParams) {
    return this.post<Requirement>('/requirements', requirementData)
  }

  // 鏌ヨ闇€姹傚垪琛紙鏀寔鍒嗛〉銆佺瓫閫夋潯浠讹級
  getRequirements(params?: { 
    category?: string; 
    query?: string; 
    mine?: boolean; 
    page?: number; 
    size?: number; 
  }) {
    return this.get<{
      content: Requirement[];
      totalElements: number;
      totalPages: number;
      size: number;
      number: number;
    }>('/requirements', params)
  }

  // 鍒犻櫎闇€姹?
  deleteRequirement(requirementId: number) {
    return this.delete(`/requirements/${requirementId}`)
  }

  // 闇€姹傚尮閰嶄笓鍒╋紙鏃ф帴鍙ｏ級
  matchPatentsForRequirement(requirementId: number) {
    return this.get<MatchedPatent[]>(`/requirements/${requirementId}/match-patents`)
  }

  // AI鏅鸿兘鍖归厤涓撳埄锛堟柊鎺ュ彛锛?
  aiMatchPatents(params: { requirement: string; sessionId?: string }) {
    return this.post<AiPatentSearchResponse>('/ai/chat/patent', params)
  }

  // 闇€姹傚尮閰嶄笓瀹?
  matchExpertsForRequirement(requirementId: number) {
    return this.get<MatchedExpert[]>(`/requirements/${requirementId}/match-experts`)
  }

  // 淇濆瓨涓撳埄鍖归厤缁撴灉
  persistPatentMatches(requirementId: number, params: PersistPatentMatchParams) {
    return this.post(`/requirements/${requirementId}/match-patents/persist`, params)
  }

  // 淇濆瓨涓撳鍖归厤缁撴灉
  persistExpertMatches(requirementId: number, params: PersistExpertMatchParams) {
    return this.post(`/requirements/${requirementId}/match-experts/persist`, params)
  }

  // 涓汉涓撳埄绠＄悊鏂规硶
  // 鑾峰彇鍏叡涓撳埄鍒楄〃锛堟敮鎸佸垎椤靛拰绫诲埆绛涢€夛級
  getPatents(params?: PatentQueryParams) {
    return this.get<Pageable<PatentBase>>('/patents', params)
  }

  // 涓汉涓撳埄绠＄悊鏂规硶
  // 鑾峰彇涓汉涓撳埄鍒楄〃
  getUserPatentsList(params?: UserPatentQueryParams) {
    return this.get<UserPatent[]>('/user-patents', params)
  }

  // 鑾峰彇涓汉涓撳埄璇︽儏
  getUserPatentDetail(id: number) {
    return this.get<UserPatent>(`/user-patents/${id}`)
  }

  // 鍒涘缓涓汉涓撳埄
  createUserPatent(patentData: CreateUserPatentParams) {
    return this.post<UserPatent>('/user-patents', patentData)
  }

  // 鏇存柊涓汉涓撳埄
  updateUserPatent(id: number, patentData: UpdateUserPatentParams) {
    return this.put<UserPatent>(`/user-patents/${id}`, patentData)
  }

  // 鍒犻櫎涓汉涓撳埄
  deleteUserPatent(id: number) {
    return this.delete(`/user-patents/${id}`)
  }

  // 鏌ヨ鍏ㄩ儴杞寲鎴愭灉
  getAllTransformations(params?: {
    patentSource?: string
    patentCategory?: string
    patentPublicNum?: string
    userPatentId?: number
    datasetId?: number
    recordId?: string
    status?: string
  }) {
    return this.get<TransformationResult[]>('/transformations', params)
  }

  // 鍒涘缓杞寲鎴愭灉璁板綍
  createTransformation(transformationData: CreateTransformationParams) {
    return this.post<TransformationResult>('/transformations', transformationData)
  }

  updateTransformation(id: number, transformationData: CreateTransformationParams) {
    return this.put<TransformationResult>(`/transformations/${id}`, transformationData)
  }

  deleteTransformation(id: number) {
    return this.delete(`/transformations/${id}`)
  }

  // 鐢熸垚璇勪及鎶ュ憡
  generateValuationReport(valuationParams: GenerateValuationParams) {
    return this.post<ValuationReport>('/valuations', valuationParams)
  }

  // 鏌ヨ璇勪及鎶ュ憡鍒楄〃
  getValuationReports(queryParams?: QueryValuationParams) {
    return this.get<ValuationReport[]>('/valuations', queryParams)
  }

  // ES鎼滅储涓撳埄锛堟櫤鑳藉尮閰嶏級
  searchPatentsByES(params: { 
    query: string; 
    category?: string; 
    page?: number; 
    size?: number; 
  }) {
    return this.get<{ total: number; hits: PatentEsHit[] }>('/patents/es/search', params)
  }

  getPatentEsDetail(datasetId: number, recordId: string | number) {
    return this.get<PatentEsDetailResponse>('/patents/es/detail', { datasetId, recordId })
  }

  getPatentEsSummary() {
    return this.get<PatentEsSummaryResponse>('/patents/es/summary')
  }

  getPatentDatasets() {
    return this.get<PatentDataset[]>('/patent-datasets')
  }

  getPatentCategories() {
    return this.get<PatentCategoryOption[]>('/patent-categories')
  }

  searchPatentDataset(datasetId: number, params?: { query?: string; page?: number; size?: number }) {
    return this.get<PatentDatasetSearchResponse>(`/patent-datasets/${datasetId}/search`, params)
  }

  getDashboardScreen() {
    return this.get<DashboardScreenResponse>('/dashboard/screen', undefined, { noCache: true })
  }

  // 鏇存柊璇勪及妯″瀷鍙傛暟
  updateValuationParam(key: string, paramData: UpdateValuationParam) {
    return this.put<ApiResponse>(`/valuation-params/${key}`, paramData)
  }

  // 鑾峰彇褰撳墠鐢ㄦ埛淇℃伅
  getCurrentUserInfo() {
    return this.get<FullUserInfo>('/users/me')
  }

  // 鏇存柊鐢ㄦ埛璧勬枡
  updateUserProfile(profileData: UpdateUserProfileParams) {
    const data: any = {}
    if ((profileData as any).nickname !== undefined) data.nickname = (profileData as any).nickname
    if ((profileData as any).avatarUrl !== undefined) data.avatarUrl = (profileData as any).avatarUrl
    if ((profileData as any).realName !== undefined) data.realName = (profileData as any).realName
    if ((profileData as any).idNumber !== undefined) data.idNumber = (profileData as any).idNumber
    if ((profileData as any).phone !== undefined) data.phone = (profileData as any).phone
    if ((profileData as any).email !== undefined) data.email = (profileData as any).email
    return this.put<UserProfile>('/users/me/profile', data)
  }

  uploadUserAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return this.axiosInstance.post<any, AvatarUploadResponse>('/users/me/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}

// 瀵煎嚭API瀹炰緥
const api = new ApiService()

export default api

// 璁よ瘉鐩稿叧API
export const authApi = {
  // 娉ㄥ唽
  register(data: { username: string; password: string; phone?: string; email?: string; nickname?: string }) {
    return api.post<{ userId: number }>('/auth/register', data)
  },
  // 鐧诲綍
  login(data: { username: string; password: string; captchaId: string; captchaAnswer: string }) {
    return api.post<string>('/auth/login', data)
  },
  // 获取登录验证码
  getCaptcha() {
    return api.get<LoginCaptcha>('/auth/captcha', {}, { noCache: true })
  },
  // 鑾峰彇褰撳墠鐢ㄦ埛淇℃伅
  getCurrentUser() {
    return api.get<any>('/users/me', {}, { noCache: true })
  }
}

export const presenceApi = {
  getStatus() {
    return api.get<PresenceStatus>('/public/presence', {}, { noCache: true })
  },
  heartbeat(payload: { clientId: string; page: string }) {
    return api.post<PresenceStatus>('/public/presence/heartbeat', payload, { noCache: true })
  }
}

// 涓撳埄鐩稿叧鏂规硶
export const patentApi = {
  // ES鎼滅储涓撳埄锛堟櫤鑳藉尮閰嶏級
  searchPatentsByES: (params: { 
    query: string; 
    category?: string; 
    page?: number; 
    size?: number; 
  }) => {
    return api.searchPatentsByES(params)
  },

  getPatentDetail: (datasetId: number, recordId: string | number) => {
    return api.getPatentEsDetail(datasetId, recordId)
  },

  getPatentSummary: () => {
    return api.getPatentEsSummary()
  },

  getPatentDatasets: () => {
    return api.getPatentDatasets()
  },

  getPatentCategories: () => {
    return api.getPatentCategories()
  },

  searchPatentDataset: (datasetId: number, params?: { query?: string; page?: number; size?: number }) => {
    return api.searchPatentDataset(datasetId, params)
  },

  getPatent: async (patentCategory: string, patentPublicNum: string) => {
    const category = normalizePatentCategory(patentCategory)
    const data = await api.get<{ total: number; hits: PatentEsHit[] }>('/patents/es/search', {
      query: patentPublicNum,
      category,
      page: 0,
      size: 1
    })
    const patent = data?.hits?.[0]
    if (!patent) {
      throw new Error('鏈壘鍒拌涓撳埄')
    }
    if (patent.datasetId && patent.recordId) {
      return api.getPatentEsDetail(patent.datasetId, patent.recordId)
    }
    return patent as PatentBase
  },

  // 鑾峰彇鍏叡涓撳埄鍒楄〃锛堟敮鎸佸垎椤靛拰绫诲埆绛涢€夛級
  getPatents: (params?: PatentQueryParams) => {
    return api.getPatents(params)
  },

  // 涓汉涓撳埄绠＄悊鏂规硶
  // 鑾峰彇涓汉涓撳埄鍒楄〃
  getUserPatentsList: (params?: UserPatentQueryParams) => {
    return api.getUserPatentsList(params)
  },

  // 鑾峰彇涓汉涓撳埄璇︽儏
  getUserPatentDetail: (id: number) => {
    return api.getUserPatentDetail(id)
  },

  // 鍒涘缓涓汉涓撳埄
  createUserPatent: (patentData: CreateUserPatentParams) => {
    return api.createUserPatent(patentData)
  },

  // 鏇存柊涓汉涓撳埄
  updateUserPatent: (id: number, patentData: UpdateUserPatentParams) => {
    return api.updateUserPatent(id, patentData)
  },

  // 鍒犻櫎涓汉涓撳埄
  deleteUserPatent: (id: number) => {
    return api.deleteUserPatent(id)
  }
}

export const dashboardApi = {
  getScreenData: () => {
    return api.getDashboardScreen()
  }
}
// 涓撳鐩稿叧鏂规硶
export const expertApi = {
  // 鏌ヨ涓撳
  getExperts: (params: ExpertQueryParams) => {
    return api.getExperts(params.query)
  },

  // 鍒涘缓涓撳
  createExpert: (expertData: Omit<Expert, 'id'>) => {
    return api.createExpert(expertData)
  }
}

// 闇€姹傜浉鍏虫柟娉?
export const requirementApi = {
  // 鍒涘缓闇€姹?
  createRequirement: (requirementData: CreateRequirementParams) => {
    return api.createRequirement(requirementData)
  },

  // 鏌ヨ闇€姹傚垪琛紙鏀寔鍒嗛〉銆佺瓫閫夋潯浠讹級
  getRequirements: (params?: { 
    category?: string; 
    query?: string; 
    mine?: boolean; 
    page?: number; 
    size?: number; 
  }) => {
    return api.getRequirements(params)
  },

  // 鍒犻櫎闇€姹?
  deleteRequirement: (requirementId: number) => {
    return api.deleteRequirement(requirementId)
  },

  // 闇€姹傚尮閰嶄笓鍒?
  matchPatentsForRequirement: (requirementId: number) => {
    return api.matchPatentsForRequirement(requirementId)
  },

  // AI鏅鸿兘鍖归厤涓撳埄
  aiMatchPatents: (params: { requirement: string; sessionId?: string }) => {
    return api.aiMatchPatents(params)
  },

  // 闇€姹傚尮閰嶄笓瀹?
  matchExpertsForRequirement: (requirementId: number) => {
    return api.matchExpertsForRequirement(requirementId)
  },

  // 淇濆瓨涓撳埄鍖归厤缁撴灉
  persistPatentMatches: (requirementId: number, params: PersistPatentMatchParams) => {
    return api.persistPatentMatches(requirementId, params)
  },

  // 淇濆瓨涓撳鍖归厤缁撴灉
  persistExpertMatches: (requirementId: number, params: PersistExpertMatchParams) => {
    return api.persistExpertMatches(requirementId, params)
  }
}

// 杞寲鎴愭灉鐩稿叧鏂规硶
export const transformationApi = {
  // 鏌ヨ鍏ㄩ儴杞寲鎴愭灉
  getAllTransformations: (params?: {
    patentSource?: string
    patentCategory?: string
    patentPublicNum?: string
    userPatentId?: number
    datasetId?: number
    recordId?: string
    status?: string
  }) => {
    return api.getAllTransformations(params)
  },

  // 鍒涘缓杞寲鎴愭灉璁板綍
  createTransformation: (transformationData: CreateTransformationParams) => {
    return api.createTransformation(transformationData)
  },

  updateTransformation: (id: number, transformationData: CreateTransformationParams) => {
    return api.updateTransformation(id, transformationData)
  },

  deleteTransformation: (id: number) => {
    return api.deleteTransformation(id)
  }
}

// 浠峰€艰瘎浼扮浉鍏虫柟娉?
export const valuationApi = {
  // 鐢熸垚璇勪及鎶ュ憡
  generateValuationReport: (valuationParams: GenerateValuationParams) => {
    return api.generateValuationReport(valuationParams)
  },

  // 鏌ヨ璇勪及鎶ュ憡鍒楄〃
  getValuationReports: (queryParams?: QueryValuationParams) => {
    return api.getValuationReports(queryParams)
  },

  // 鏇存柊璇勪及妯″瀷鍙傛暟
  updateValuationParam: (key: string, paramData: UpdateValuationParam) => {
    return api.updateValuationParam(key, paramData)
  }
}

// 鐢ㄦ埛璧勬枡鐩稿叧鏂规硶
export const profileApi = {
  // 鑾峰彇褰撳墠鐢ㄦ埛淇℃伅
  getCurrentUserInfo: () => {
    return api.getCurrentUserInfo()
  },

  // 鏇存柊鐢ㄦ埛璧勬枡
  updateUserProfile: (profileData: UpdateUserProfileParams) => {
    return api.updateUserProfile(profileData)
  },

  uploadAvatar: (file: File) => {
    return api.uploadUserAvatar(file)
  }
}

// AI鑱婂ぉ鐩稿叧鏂规硶
export const aiApi = {
  // AI鑱婂ぉ鍜ㄨ
  chat: (requestData: AiChatRequest) => {
    return api.post<AiChatResponseData>('/ai/chat', requestData)
  },

  ask: (requestData: PersistedAiChatRequest) => {
    return api.post<ChatAskResponse>('/chat/ask', requestData)
  },
  
  // 鑾峰彇鐢ㄦ埛鐨勬墍鏈夎亰澶╀細璇濓紙鏀寔鍒嗛〉锛?
  getSessions: (page: number = 1, size: number = 10) => {
    return api.get<ChatSessionResponse[]>('/chat/sessions', { page, size }, { noCache: true })
  },
  
  // 鍒涘缓鏂扮殑鑱婂ぉ浼氳瘽
  createSession: (title?: string) => {
    const requestData: ChatCreateSessionRequest = {
      title: title || '新对话'
    }
    return api.post<ChatSessionResponse>('/chat/sessions', requestData)
  },
  
  // 鍒犻櫎鑱婂ぉ浼氳瘽
  deleteSession: (sessionId: number) => {
    return api.delete(`/chat/sessions/${sessionId}`)
  },
  
  // 鏇存柊浼氳瘽鏍囬
  updateSessionTitle: (sessionId: number, title: string) => {
    return api.put<ChatSessionResponse>(`/chat/sessions/${sessionId}/title`, { title })
  },
  
  // 鑾峰彇浼氳瘽鐨勬墍鏈夋秷鎭?
  getMessages: (sessionId: number) => {
    return api.get<ChatMessageResponse[]>(`/chat/sessions/${sessionId}/messages`)
  }
}

