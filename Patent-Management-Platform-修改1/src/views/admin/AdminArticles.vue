<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <strong>文章管理</strong>
        <el-button type="primary" @click="router.push('/admin/articles/new')">新建文章</el-button>
      </div>
    </template>

    <el-table :data="articles" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="slug" label="Slug" min-width="180" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="sortOrder" label="排序" width="100" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/admin/articles/${row.id}`)">编辑</el-button>
          <el-button link @click="openArticle(row.slug)">预览</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { isNativeApp } from '@/lib/runtime'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { CmsArticle } from '@/types/cms'

const router = useRouter()
const loading = ref(false)
const articles = ref<CmsArticle[]>([])

const load = async () => {
  loading.value = true
  try {
    articles.value = await adminApi.listArticles()
  } catch (error: any) {
    ElMessage.error(error.message || '加载文章失败')
  } finally {
    loading.value = false
  }
}

const remove = async (id: number) => {
  await ElMessageBox.confirm('确认删除这篇文章吗？', '提示', { type: 'warning' })
  await adminApi.deleteArticle(id)
  ElMessage.success('文章已删除')
  await load()
}

const openArticle = (slug: string) => {
  const targetPath = `/articles/${slug}`
  if (isNativeApp()) {
    void router.push(targetPath)
    return
  }
  window.open(targetPath, '_blank')
}

onMounted(() => {
  void load()
})
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
