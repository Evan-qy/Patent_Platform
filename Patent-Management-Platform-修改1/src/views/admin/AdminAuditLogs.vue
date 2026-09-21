<template>
  <div class="audit-page">
    <el-card>
      <template #header>
        <div class="header-row">
          <strong>审计日志</strong>
          <el-button @click="loadLogs">刷新</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="filters" class="filters">
        <el-form-item label="用户名">
          <el-input v-model="filters.username" clearable placeholder="输入用户名" />
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="filters.eventType" clearable placeholder="全部">
            <el-option label="登录" value="LOGIN" />
            <el-option label="注册" value="REGISTER" />
            <el-option label="新增" value="CREATE" />
            <el-option label="查看" value="READ" />
            <el-option label="修改" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="查询" value="QUERY" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作">
          <el-input v-model="filters.action" clearable placeholder="如 修改个人资料" />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-input v-model="filters.resourceType" clearable placeholder="如 requirement" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="filters.operationResult" clearable placeholder="全部">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAIL" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogs">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logs" v-loading="loading" row-key="id">
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="displayName" label="姓名" width="140" />
        <el-table-column prop="eventType" label="事件类型" width="110" />
        <el-table-column prop="action" label="动作" min-width="180" />
        <el-table-column prop="resourceType" label="资源类型" width="140" />
        <el-table-column prop="requestMethod" label="方法" width="90" />
        <el-table-column prop="ipAddress" label="IP" width="150" />
        <el-table-column prop="hostName" label="主机名" width="150" />
        <el-table-column prop="operationResult" label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.operationResult === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.operationResult || '--' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="详情" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="detailVisible" title="日志详情" size="520px">
      <template v-if="currentLog">
        <div class="detail-grid">
          <div><strong>时间：</strong>{{ currentLog.createdAt }}</div>
          <div><strong>用户名：</strong>{{ currentLog.username || '--' }}</div>
          <div><strong>姓名：</strong>{{ currentLog.displayName || '--' }}</div>
          <div><strong>角色：</strong>{{ currentLog.roleName || '--' }}</div>
          <div><strong>事件：</strong>{{ currentLog.eventType || '--' }}</div>
          <div><strong>动作：</strong>{{ currentLog.action }}</div>
          <div><strong>资源类型：</strong>{{ currentLog.resourceType || '--' }}</div>
          <div><strong>资源 ID：</strong>{{ currentLog.resourceId || '--' }}</div>
          <div><strong>请求方法：</strong>{{ currentLog.requestMethod || '--' }}</div>
          <div><strong>请求路径：</strong>{{ currentLog.requestPath || '--' }}</div>
          <div><strong>IP：</strong>{{ currentLog.ipAddress || '--' }}</div>
          <div><strong>MAC：</strong>{{ currentLog.macAddress || '--' }}</div>
          <div><strong>主机名：</strong>{{ currentLog.hostName || '--' }}</div>
          <div><strong>地点：</strong>{{ currentLog.location || '--' }}</div>
          <div><strong>平台：</strong>{{ currentLog.platformType || '--' }}</div>
          <div><strong>结果：</strong>{{ currentLog.operationResult || '--' }}</div>
          <div class="full-line"><strong>User-Agent：</strong>{{ currentLog.userAgent || '--' }}</div>
          <div class="full-line"><strong>详细说明：</strong>{{ currentLog.detail || '--' }}</div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { AuditLogRecord } from '@/types/cms'

const loading = ref(false)
const detailVisible = ref(false)
const logs = ref<AuditLogRecord[]>([])
const currentLog = ref<AuditLogRecord | null>(null)

const filters = reactive({
  username: '',
  eventType: '',
  action: '',
  resourceType: '',
  operationResult: ''
})

const loadLogs = async () => {
  loading.value = true
  try {
    logs.value = await adminApi.listAuditLogs({
      username: filters.username || undefined,
      eventType: filters.eventType || undefined,
      action: filters.action || undefined,
      resourceType: filters.resourceType || undefined,
      operationResult: filters.operationResult || undefined
    })
  } catch (error: any) {
    ElMessage.error(error.message || '加载审计日志失败')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.username = ''
  filters.eventType = ''
  filters.action = ''
  filters.resourceType = ''
  filters.operationResult = ''
  void loadLogs()
}

const openDetail = (row: AuditLogRecord) => {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(() => {
  void loadLogs()
})
</script>

<style scoped>
.audit-page {
  display: grid;
  gap: 16px;
}

.header-row,
.filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.filters {
  margin-bottom: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  line-height: 1.7;
}

.full-line {
  grid-column: 1 / -1;
  word-break: break-word;
}
</style>
