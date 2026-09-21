<template>
  <div class="dataset-page">
    <el-card>
      <template #header>
        <div class="header-row">
          <strong>专利数据集</strong>
          <div class="header-actions">
            <el-button @click="loadBase">刷新</el-button>
            <el-button type="primary" @click="openCreateDialog">新增数据集</el-button>
            <el-button type="success" plain @click="discover">发现新表</el-button>
          </div>
        </div>
      </template>

      <div class="es-toolbar">
        <div class="es-status">
          <span class="status-label">ES 索引：</span>
          <el-tag :type="esStatus?.exists ? 'success' : 'warning'">
            {{ esStatus?.exists ? '已创建' : '未创建' }}
          </el-tag>
          <span>文档数：{{ esStatus?.docCount ?? '--' }}</span>
          <span>启用数据集：{{ esStatus?.enabledDatasetCount ?? '--' }}</span>
        </div>
        <div class="es-actions">
          <el-button :loading="reindexing" @click="loadEsStatus">刷新状态</el-button>
          <el-button type="primary" :loading="reindexing" @click="reindexSelected">重建所选索引</el-button>
          <el-button type="warning" plain :loading="reindexing" @click="reindexAllEnabled">重建全部启用索引</el-button>
        </div>
      </div>

      <el-table :data="datasets" v-loading="loading" row-key="id" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="code" label="编码" width="160" />
        <el-table-column prop="tableName" label="表名" min-width="180" />
        <el-table-column prop="category" label="分类" width="140" />
        <el-table-column label="启用" width="100">
          <template #default="{ row }">{{ row.enabled ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="内置" width="100">
          <template #default="{ row }">{{ row.builtIn ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editDataset(row)">编辑</el-button>
            <el-button link type="success" @click="reindexOne(row)">重建索引</el-button>
            <el-button link type="danger" :disabled="row.builtIn" @click="removeDataset(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑数据集' : '新增数据集'" width="720px">
      <el-form :model="form" label-width="120px" class="dataset-form">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.category" /></el-form-item>
        <el-form-item label="表名"><el-input v-model="form.tableName" /></el-form-item>
        <el-form-item label="主键字段"><el-input v-model="form.primaryKeyColumn" /></el-form-item>
        <el-form-item label="公开号字段"><el-input v-model="form.publicNumColumn" /></el-form-item>
        <el-form-item label="标题字段"><el-input v-model="form.titleColumn" /></el-form-item>
        <el-form-item label="摘要字段"><el-input v-model="form.abstractColumn" /></el-form-item>
        <el-form-item label="申请人字段"><el-input v-model="form.applicantColumn" /></el-form-item>
        <el-form-item label="发明人字段"><el-input v-model="form.inventorColumn" /></el-form-item>
        <el-form-item label="IPC 字段"><el-input v-model="form.ipcColumn" /></el-form-item>
        <el-form-item label="CPC 字段"><el-input v-model="form.cpcColumn" /></el-form-item>
        <el-form-item label="申请日字段"><el-input v-model="form.appliDateColumn" /></el-form-item>
        <el-form-item label="公开日字段"><el-input v-model="form.publicDateColumn" /></el-form-item>
        <el-form-item label="法律状态字段"><el-input v-model="form.legalStatusColumn" /></el-form-item>
        <el-form-item label="状态字段"><el-input v-model="form.statusColumn" /></el-form-item>
        <el-form-item label="排序字段"><el-input v-model="form.defaultSortColumn" /></el-form-item>
        <el-form-item label="搜索字段 JSON"><el-input v-model="form.searchFieldsJson" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remarks" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="discoverVisible" title="发现新表" width="900px">
      <el-table :data="discovered">
        <el-table-column prop="tableName" label="表名" min-width="180" />
        <el-table-column prop="code" label="建议编码" width="160" />
        <el-table-column prop="publicNumColumn" label="公开号字段" width="140" />
        <el-table-column prop="titleColumn" label="标题字段" width="140" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="useDiscovered(row)">使用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { PatentDataset, PatentEsStatusResponse } from '@/types/cms'

const loading = ref(false)
const reindexing = ref(false)
const saving = ref(false)
const datasets = ref<PatentDataset[]>([])
const discovered = ref<PatentDataset[]>([])
const selectedRows = ref<PatentDataset[]>([])
const esStatus = ref<PatentEsStatusResponse | null>(null)
const dialogVisible = ref(false)
const discoverVisible = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = (): Partial<PatentDataset> => ({
  dataSourceId: 1,
  code: '',
  name: '',
  category: '',
  tableName: '',
  primaryKeyColumn: 'id',
  publicNumColumn: '',
  titleColumn: '',
  abstractColumn: '',
  applicantColumn: '',
  inventorColumn: '',
  ipcColumn: '',
  cpcColumn: '',
  appliDateColumn: '',
  publicDateColumn: '',
  legalStatusColumn: '',
  statusColumn: '',
  defaultSortColumn: '',
  defaultSortDirection: 'DESC',
  searchFieldsJson: '[]',
  enabled: false,
  builtIn: false,
  remarks: ''
})

const form = reactive<Partial<PatentDataset>>(emptyForm())

const assignForm = (payload: Partial<PatentDataset>) => {
  Object.assign(form, emptyForm(), payload)
}

const loadBase = async () => {
  loading.value = true
  try {
    datasets.value = await adminApi.listDatasets()
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据集失败')
  } finally {
    loading.value = false
  }
}

const loadEsStatus = async () => {
  try {
    esStatus.value = await adminApi.getPatentEsStatus()
  } catch (error: any) {
    ElMessage.error(error.message || '加载 ES 状态失败')
  }
}

const discover = async () => {
  try {
    discovered.value = await adminApi.discoverDatasets()
    discoverVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '发现新表失败')
  }
}

const handleSelectionChange = (rows: PatentDataset[]) => {
  selectedRows.value = rows
}

const openCreateDialog = () => {
  editingId.value = null
  assignForm(emptyForm())
  dialogVisible.value = true
}

const editDataset = (row: PatentDataset) => {
  editingId.value = row.id
  assignForm(row)
  dialogVisible.value = true
}

const useDiscovered = (row: PatentDataset) => {
  discoverVisible.value = false
  editingId.value = null
  assignForm(row)
  dialogVisible.value = true
}

const save = async () => {
  saving.value = true
  try {
    await adminApi.saveDataset(form, editingId.value || undefined)
    ElMessage.success('数据集已保存')
    dialogVisible.value = false
    await Promise.all([loadBase(), loadEsStatus()])
  } catch (error: any) {
    ElMessage.error(error.message || '保存数据集失败')
  } finally {
    saving.value = false
  }
}

const removeDataset = async (row: PatentDataset) => {
  try {
    await ElMessageBox.confirm(`确定删除数据集“${row.name}”吗？`, '确认删除', { type: 'warning' })
    await adminApi.deleteDataset(row.id)
    ElMessage.success('数据集已删除')
    await Promise.all([loadBase(), loadEsStatus()])
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除数据集失败')
    }
  }
}

const reindexOne = async (row: PatentDataset) => {
  reindexing.value = true
  try {
    await adminApi.reindexDataset(row.id)
    ElMessage.success('索引重建完成')
    await loadEsStatus()
  } catch (error: any) {
    ElMessage.error(error.message || '重建索引失败')
  } finally {
    reindexing.value = false
  }
}

const reindexSelected = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先勾选要重建索引的数据集')
    return
  }
  reindexing.value = true
  try {
    await adminApi.reindexPatentEs({ datasetIds: selectedRows.value.map(item => item.id) })
    ElMessage.success('所选数据集索引已重建')
    await loadEsStatus()
  } catch (error: any) {
    ElMessage.error(error.message || '重建所选索引失败')
  } finally {
    reindexing.value = false
  }
}

const reindexAllEnabled = async () => {
  reindexing.value = true
  try {
    await adminApi.reindexPatentEs({})
    ElMessage.success('全部启用数据集索引已重建')
    await loadEsStatus()
  } catch (error: any) {
    ElMessage.error(error.message || '重建全部索引失败')
  } finally {
    reindexing.value = false
  }
}

onMounted(() => {
  void Promise.all([loadBase(), loadEsStatus()])
})
</script>

<style scoped>
.dataset-page { display: grid; gap: 16px; }
.header-row, .es-toolbar, .es-status, .es-actions, .header-actions { display: flex; align-items: center; }
.header-row, .es-toolbar { justify-content: space-between; }
.es-status, .es-actions, .header-actions { gap: 12px; flex-wrap: wrap; }
.status-label { color: var(--text-secondary); }
.dataset-form { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 12px; }
.dataset-form :deep(.el-form-item:last-child),
.dataset-form :deep(.el-form-item:nth-last-child(2)) { grid-column: span 2; }
@media (max-width: 900px) {
  .dataset-form { grid-template-columns: 1fr; }
}
</style>
