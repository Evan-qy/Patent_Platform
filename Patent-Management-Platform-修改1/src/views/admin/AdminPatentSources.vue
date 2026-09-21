<template>
  <div class="source-page">
    <el-card>
      <template #header>
        <div class="header-row">
          <strong>专利数据源</strong>
          <el-tag type="info">当前仅接入应用主库，用于发现新表并配置 ES 数据集</el-tag>
        </div>
      </template>

      <el-table :data="sources" v-loading="loading" @row-click="selectSource">
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="code" label="编码" width="160" />
        <el-table-column prop="sourceType" label="类型" width="160" />
        <el-table-column prop="jdbcUrl" label="JDBC 地址" min-width="280" show-overflow-tooltip />
        <el-table-column label="启用" width="100">
          <template #default="{ row }">{{ row.enabled ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="内置" width="100">
          <template #default="{ row }">{{ row.builtIn ? '是' : '否' }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="selectedSource" class="preview-card">
      <template #header>
        <div class="header-row">
          <strong>数据源预览：{{ selectedSource.name }}</strong>
          <el-button type="primary" @click="$router.push('/admin/patent-datasets')">前往数据集配置</el-button>
        </div>
      </template>

      <div class="preview-layout">
        <div>
          <div class="toolbar-row">
            <span>数据表 / 视图</span>
            <el-button text @click="loadTables">刷新</el-button>
          </div>
          <el-table :data="tables" height="360" @row-click="selectTable">
            <el-table-column prop="name" label="表名" min-width="180" />
            <el-table-column prop="type" label="类型" width="160" />
          </el-table>
        </div>

        <div>
          <div class="toolbar-row">
            <span>字段预览</span>
            <span v-if="selectedTableName" class="muted">{{ selectedTableName }}</span>
          </div>
          <el-table :data="columns" height="360">
            <el-table-column prop="name" label="字段名" min-width="180" />
            <el-table-column prop="type" label="类型" min-width="140" />
            <el-table-column prop="size" label="长度" width="100" />
          </el-table>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { PatentDataSource, TableColumnInfo, TableInfo } from '@/types/cms'

const loading = ref(false)
const sources = ref<PatentDataSource[]>([])
const selectedSource = ref<PatentDataSource | null>(null)
const tables = ref<TableInfo[]>([])
const columns = ref<TableColumnInfo[]>([])
const selectedTableName = ref('')

const resetPreview = () => {
  tables.value = []
  columns.value = []
  selectedTableName.value = ''
}

const load = async () => {
  loading.value = true
  try {
    sources.value = await adminApi.listSources()
    if (!selectedSource.value && sources.value.length) {
      await selectSource(sources.value[0])
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据源失败')
  } finally {
    loading.value = false
  }
}

const selectSource = async (row: PatentDataSource) => {
  selectedSource.value = row
  resetPreview()
  await loadTables()
}

const loadTables = async () => {
  if (!selectedSource.value) return
  try {
    tables.value = await adminApi.listTables(selectedSource.value.id)
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据表失败')
  }
}

const selectTable = async (row: TableInfo) => {
  if (!selectedSource.value) return
  selectedTableName.value = row.name
  try {
    columns.value = await adminApi.listColumns(selectedSource.value.id, row.name)
  } catch (error: any) {
    ElMessage.error(error.message || '加载字段失败')
  }
}

onMounted(() => {
  void load()
})
</script>

<style scoped>
.source-page { display: grid; gap: 16px; }
.header-row, .toolbar-row { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.preview-card :deep(.el-card__body) { padding-top: 12px; }
.preview-layout { display: grid; grid-template-columns: minmax(240px, 0.9fr) minmax(0, 1.1fr); gap: 16px; }
.muted { color: var(--text-secondary); font-size: 12px; }
@media (max-width: 900px) { .preview-layout { grid-template-columns: 1fr; } }
</style>
