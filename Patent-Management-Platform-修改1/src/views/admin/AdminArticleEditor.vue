<template>
  <el-card v-loading="loading">
    <template #header>
      <strong>{{ isEdit ? '编辑文章' : '新建文章' }}</strong>
    </template>

    <el-form :model="form" label-position="top">
      <el-form-item label="标题">
        <el-input v-model="form.title" />
      </el-form-item>

      <el-form-item label="Slug">
        <el-input v-model="form.slug" />
      </el-form-item>

      <el-form-item label="摘要">
        <el-input v-model="form.summary" type="textarea" :rows="3" />
      </el-form-item>

      <el-form-item label="封面 URL">
        <el-input v-model="form.coverUrl" />
      </el-form-item>

      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 180px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
        </el-select>
      </el-form-item>

      <el-form-item label="排序">
        <el-input-number v-model="form.sortOrder" :min="0" />
      </el-form-item>

      <el-form-item label="Markdown 正文">
        <el-input v-model="form.contentMd" type="textarea" :rows="18" />
      </el-form-item>

      <div class="action-row">
        <el-button @click="router.push('/admin/articles')">返回</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存文章</el-button>
      </div>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { CmsArticle } from '@/types/cms'

type ArticleStatus = CmsArticle['status']

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const isEdit = computed(() => Boolean(route.params.id) && route.params.id !== 'new')

const form = reactive({
  slug: '',
  title: '',
  summary: '',
  coverUrl: '',
  contentMd: '',
  status: 'DRAFT' as ArticleStatus,
  sortOrder: 0
})

const toPayload = (): Partial<CmsArticle> => ({
  slug: form.slug.trim(),
  title: form.title.trim(),
  summary: form.summary.trim(),
  coverUrl: form.coverUrl.trim(),
  contentMd: form.contentMd,
  status: form.status,
  sortOrder: form.sortOrder
})

const load = async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    const detail = await adminApi.getArticle(Number(route.params.id))
    Object.assign(form, {
      slug: detail.slug || '',
      title: detail.title || '',
      summary: detail.summary || '',
      coverUrl: detail.coverUrl || '',
      contentMd: detail.contentMd || '',
      status: detail.status || 'DRAFT',
      sortOrder: detail.sortOrder || 0
    })
  } catch (error: any) {
    ElMessage.error(error.message || '加载文章失败')
  } finally {
    loading.value = false
  }
}

const save = async () => {
  saving.value = true
  try {
    const payload = toPayload()
    if (isEdit.value) {
      await adminApi.updateArticle(Number(route.params.id), payload)
    } else {
      await adminApi.createArticle(payload)
    }
    ElMessage.success('文章已保存')
    router.push('/admin/articles')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<style scoped>
.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
