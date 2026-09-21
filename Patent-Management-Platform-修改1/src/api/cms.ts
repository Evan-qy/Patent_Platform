import api from './index'
import type { CmsArticle, HomeContentPayload, PatentDataset, PagedMapResponse } from '@/types/cms'

export const cmsApi = {
  getHomeContent() {
    return api.get<HomeContentPayload>('/home-content', {}, { noCache: true })
  },
  getArticles() {
    return api.get<CmsArticle[]>('/articles', {}, { noCache: true })
  },
  getArticle(slug: string) {
    return api.get<CmsArticle>(`/articles/${slug}`, {}, { noCache: true })
  },
  getDatasets() {
    return api.get<PatentDataset[]>('/patent-datasets', {}, { noCache: true })
  },
  searchDataset(id: number, params: { query?: string; page?: number; size?: number }) {
    return api.get<PagedMapResponse>(`/patent-datasets/${id}/search`, params, { noCache: true })
  }
}
