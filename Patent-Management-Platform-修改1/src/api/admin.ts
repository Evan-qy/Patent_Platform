import axios from 'axios'
import type {
  AdminLoginResponse,
  AdminProfile,
  AdminSqlExecution,
  AuditLogRecord,
  CmsArticle,
  HomeContentPayload,
  PatentDataSource,
  PatentDataset,
  PatentEsReindexRequest,
  PatentEsStatusResponse,
  SqlExecuteResponse,
  TableColumnInfo,
  TableInfo
} from '@/types/cms'

const BASE_URL = String(import.meta.env.VITE_API_BASE_URL || '/api').replace(/['"]/g, '').trim().replace(/\/+$/, '')

type Wrapped<T> = {
  code: number
  message: string
  data: T
}

const client = axios.create({
  baseURL: BASE_URL,
  timeout: 180000,
  headers: {
    'Content-Type': 'application/json'
  }
})

const isApiEnvelope = <T>(value: unknown): value is Wrapped<T> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return false
  }
  return Object.prototype.hasOwnProperty.call(value, 'code')
}

const normalizeError = (error: any) => {
  const message =
    error?.response?.data?.message
    || error?.response?.data?.error
    || error?.message
    || '请求失败'
  return new Error(message)
}

client.interceptors.request.use((config) => {
  const isLogin = config.url?.includes('/admin/auth/login')
  if (!isLogin) {
    const token = localStorage.getItem('adminToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const data = response.data
    if (!isApiEnvelope(data)) {
      return data
    }
    if (data.code !== 0) {
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data.data
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminProfile')
      if (!window.location.pathname.startsWith('/admin/login')) {
        window.location.href = '/admin/login'
      }
    }
    return Promise.reject(normalizeError(error))
  }
)

export const adminApi = {
  login(payload: { username: string; password: string }) {
    return client.post<any, AdminLoginResponse>('/admin/auth/login', payload)
  },

  getMe() {
    return client.get<any, AdminProfile>('/admin/auth/me')
  },

  getHomeContent() {
    return client.get<any, HomeContentPayload>('/admin/home-content')
  },

  updateHomeContent(payload: HomeContentPayload) {
    return client.post<any, HomeContentPayload>('/admin/home-content', payload)
  },

  listArticles() {
    return client.get<any, CmsArticle[]>('/admin/articles')
  },

  getArticle(id: number) {
    return client.get<any, CmsArticle>(`/admin/articles/${id}`)
  },

  createArticle(payload: Partial<CmsArticle>) {
    return client.post<any, CmsArticle>('/admin/articles', payload)
  },

  updateArticle(id: number, payload: Partial<CmsArticle>) {
    return client.put<any, CmsArticle>(`/admin/articles/${id}`, payload)
  },

  deleteArticle(id: number) {
    return client.delete<any, void>(`/admin/articles/${id}`)
  },

  listSources() {
    return client.get<any, PatentDataSource[]>('/admin/patent-sources')
  },

  listTables(id: number) {
    return client.get<any, TableInfo[]>(`/admin/patent-sources/${id}/tables`)
  },

  listColumns(sourceId: number, tableName: string) {
    return client.get<any, TableColumnInfo[]>(
      `/admin/patent-sources/${sourceId}/tables/${encodeURIComponent(tableName)}/columns`
    )
  },

  listDatasets() {
    return client.get<any, PatentDataset[]>('/admin/patent-datasets')
  },

  discoverDatasets() {
    return client.post<any, PatentDataset[]>('/admin/patent-datasets/discover')
  },

  saveDataset(payload: Partial<PatentDataset>, id?: number) {
    if (id) {
      return client.put<any, PatentDataset>(`/admin/patent-datasets/${id}`, payload)
    }
    return client.post<any, PatentDataset>('/admin/patent-datasets', payload)
  },

  deleteDataset(id: number) {
    return client.delete<any, void>(`/admin/patent-datasets/${id}`)
  },

  reindexDataset(id: number) {
    return client.post<any, void>(`/admin/patent-datasets/${id}/reindex`)
  },

  getPatentEsStatus() {
    return client.get<any, PatentEsStatusResponse>('/patents/es/status')
  },

  async reindexPatentEs(payload: PatentEsReindexRequest = {}) {
    const datasetIds = Array.from(new Set(payload.datasetIds || []))
    if (!datasetIds.length) {
      await client.post('/patents/es/reindex')
      return
    }

    for (const id of datasetIds) {
      await client.post(`/admin/patent-datasets/${id}/reindex`)
    }
  },

  executeSql(payload: { dataSourceId: number; sql: string }) {
    return client.post<any, SqlExecuteResponse>('/admin/sql/execute', payload)
  },

  getSqlHistory() {
    return client.get<any, AdminSqlExecution[]>('/admin/sql/history')
  },

  listAuditLogs(params: {
    userId?: number
    username?: string
    eventType?: string
    action?: string
    requestMethod?: string
    resourceType?: string
    operationResult?: string
    from?: string
    to?: string
  } = {}) {
    return client.get<any, AuditLogRecord[]>('/admin/audit-logs', { params })
  }
}
