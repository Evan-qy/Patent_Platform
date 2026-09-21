<template>
  <el-card>
    <template #header>
      <strong>SQL 控制台</strong>
    </template>

    <el-alert
      title="为避免误操作影响生产数据，当前仅允许执行 SELECT、SHOW、DESC、DESCRIBE、EXPLAIN 语句。"
      type="warning"
      :closable="false"
      class="mb-4"
    />

    <el-form label-position="top">
      <el-form-item label="数据源">
        <el-select v-model="dataSourceId" style="width: 100%">
          <el-option v-for="item in sources" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="SQL">
        <el-input v-model="sql" type="textarea" :rows="10" placeholder="例如：SELECT id, title FROM cms_article LIMIT 20" />
      </el-form-item>

      <div class="action-row">
        <el-button type="primary" :loading="executing" @click="executeSql">执行 SQL</el-button>
        <el-button @click="loadHistory">刷新历史</el-button>
      </div>
    </el-form>

    <div class="result-block">
      <div class="result-header">
        <h4>执行结果</h4>
        <span v-if="result">耗时 {{ result.durationMs }} ms</span>
      </div>
      <el-empty v-if="!result" description="暂未执行 SQL" />
      <template v-else>
        <el-descriptions :column="3" border size="small" class="result-meta">
          <el-descriptions-item label="语句类型">{{ result.statementType }}</el-descriptions-item>
          <el-descriptions-item label="返回行数">{{ result.rows.length }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ result.durationMs }} ms</el-descriptions-item>
        </el-descriptions>

        <el-table v-if="result.columns.length" :data="result.rows" max-height="360">
          <el-table-column v-for="column in result.columns" :key="column" :prop="column" :label="column" min-width="160" />
        </el-table>
      </template>
    </div>

    <div class="result-block">
      <h4>最近执行历史</h4>
      <el-empty v-if="!history.length" description="暂无执行记录" />
      <el-table v-else :data="history" max-height="320">
        <el-table-column prop="statementType" label="类型" width="120" />
        <el-table-column prop="sqlText" label="SQL" min-width="260" show-overflow-tooltip />
        <el-table-column label="结果" width="120">
          <template #default="{ row }">{{ row.successFlag ? '成功' : '失败' }}</template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时(ms)" width="120" />
        <el-table-column prop="executedAt" label="执行时间" min-width="180" />
      </el-table>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { AdminSqlExecution, PatentDataSource, SqlExecuteResponse } from '@/types/cms'

const sources = ref<PatentDataSource[]>([])
const dataSourceId = ref<number>()
const sql = ref('SELECT id, title, status FROM cms_article LIMIT 20')
const executing = ref(false)
const result = ref<SqlExecuteResponse | null>(null)
const history = ref<AdminSqlExecution[]>([])

const loadSources = async () => {
  sources.value = await adminApi.listSources()
  dataSourceId.value = sources.value[0]?.id
}

const loadHistory = async () => {
  history.value = await adminApi.getSqlHistory()
}

const executeSql = async () => {
  if (!dataSourceId.value) {
    ElMessage.warning('请先选择数据源')
    return
  }
  if (!sql.value.trim()) {
    ElMessage.warning('请输入要执行的 SQL')
    return
  }

  executing.value = true
  try {
    result.value = await adminApi.executeSql({
      dataSourceId: dataSourceId.value,
      sql: sql.value
    })
    ElMessage.success('SQL 执行成功')
    await loadHistory()
  } catch (error: any) {
    ElMessage.error(error.message || 'SQL 执行失败')
  } finally {
    executing.value = false
  }
}

onMounted(async () => {
  try {
    await loadSources()
    await loadHistory()
  } catch (error: any) {
    ElMessage.error(error.message || '加载 SQL 控制台失败')
  }
})
</script>

<style scoped>
.action-row { display: flex; gap: 12px; }
.result-block { margin-top: 24px; }
.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.result-meta { margin-bottom: 16px; }
.mb-4 { margin-bottom: 16px; }
</style>
