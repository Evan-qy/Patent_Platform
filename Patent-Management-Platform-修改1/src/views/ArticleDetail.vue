<template>
  <div class="article-detail-page">
    <div class="article-shell panel-glow" v-loading="loading">
      <div class="article-actions">
        <el-button link @click="goBack">返回</el-button>
      </div>

      <template v-if="article">
        <img v-if="article.coverUrl" :src="article.coverUrl" :alt="article.title" class="article-cover" />
        <div class="article-header">
          <h1>{{ article.title }}</h1>
          <p>{{ article.summary }}</p>
          <span>{{ formatDate(article.publishedAt || article.updatedAt) }}</span>
        </div>
        <article class="article-body markdown-body" v-html="renderMarkdown(article.contentMd)"></article>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cmsApi } from '@/api/cms'
import { getApiErrorMessage } from '@/lib/api-errors'
import { renderMarkdown } from '@/lib/markdown'
import type { CmsArticle } from '@/types/cms'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const article = ref<CmsArticle | null>(null)

const loadArticle = async () => {
  loading.value = true
  try {
    article.value = await cmsApi.getArticle(String(route.params.slug))
  } catch (error: any) {
    ElMessage.error(getApiErrorMessage(error, '文章加载失败'))
    router.push('/')
  } finally {
    loading.value = false
  }
}

const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString('zh-CN') : '')
const goBack = () => router.back()

onMounted(() => {
  void loadArticle()
})
</script>

<style scoped>
.article-detail-page {
  min-height: 100vh;
  padding: 32px 16px;
  background: transparent;
}

.article-shell {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
}

.article-cover {
  width: 100%;
  max-height: 360px;
  object-fit: cover;
  border-radius: 20px;
  margin-bottom: 20px;
}

.article-header h1 {
  margin: 0 0 12px;
  font-size: 34px;
  color: #f4fbff;
}

.article-header p {
  margin: 0 0 8px;
  color: rgba(220, 238, 255, 0.74);
}

.article-body {
  line-height: 1.8;
  color: #edf6ff;
}
</style>
