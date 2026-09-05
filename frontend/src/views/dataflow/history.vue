<template>
  <div>
    <el-card shadow="never">
      <div class="history-header">
        <el-button link :icon="ArrowLeft" @click="goBack">{{ t('history.back') }}</el-button>
        <span class="history-title">{{ t('history.title') }} - {{ flowName }}</span>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="version" :label="t('history.version')" width="100" />
        <el-table-column prop="publishDescription" :label="t('history.publishDescription')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('dataflow.status')" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).tag">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="t('dataflow.description')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createTime" :label="t('history.createTime')" width="170" />
        <el-table-column prop="updateTime" :label="t('history.updateTime')" width="170" />
        <el-table-column :label="t('dataflow.colActions')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ t('history.detail') }}</el-button>
            <el-button link type="warning" @click="handleRollback(row)">{{ t('history.rollback') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        class="pager"
        layout="total, prev, pager, next, sizes"
        :total="page.total"
        :page-sizes="[10, 20, 50]"
        @change="load"
      />
    </el-card>

    <el-dialog v-model="detailVisible" :title="t('history.detailTitle')" width="640px" top="6vh">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="2" border>
          <el-descriptions-item :label="t('dataflow.name')">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item :label="t('history.code')">{{ detail.code }}</el-descriptions-item>
          <el-descriptions-item :label="t('history.version')">{{ detail.publishVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('dataflow.status')">
            <el-tag :type="statusMeta(detail.status).tag">{{ statusMeta(detail.status).label }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('history.nodeCount')">
            {{ detail.design?.nodes?.length ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('dataflow.runStrategy')">
            {{ runStrategyLabel(detail.runStrategy) }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('dataflow.instanceCount')">
            {{ detail.instanceNumber ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('dataflow.specifyInstances')">
            {{ (detail.specifyInstances ?? []).join(', ') || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('dataflow.monitor')">
            {{ detail.enableMonitor === 'ENABLE' ? t('dataflow.on') : t('dataflow.off') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('dataflow.alarm')">
            {{ detail.enableAlarm === 'ENABLE' ? t('dataflow.on') : t('dataflow.off') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('history.createTime')">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item :label="t('history.updateTime')">{{ detail.updateTime }}</el-descriptions-item>
          <el-descriptions-item :label="t('dataflow.description')" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  listPublishHistory,
  getPublishDetail,
  deletePublish,
  rollbackDataFlow,
  type DataFlowPublishItem,
  type DataFlowPublishDetail
} from '../../api/dataflow'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const flowCode = (route.query.code as string) || ''
const flowName = (route.query.name as string) || ''

const loading = ref(false)
const list = ref<DataFlowPublishItem[]>([])
const page = reactive({ current: 1, size: 10, total: 0 })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<DataFlowPublishDetail | null>(null)

const STATUS_META: Record<string, { labelKey: string; tag: 'success' | 'warning' | 'info' | 'danger' }> = {
  TBP: { labelKey: 'dataflow.statusTBP', tag: 'info' },
  ENABLE: { labelKey: 'dataflow.statusEnable', tag: 'success' },
  PAUSE: { labelKey: 'dataflow.statusPause', tag: 'warning' },
  HISTORY: { labelKey: 'dataflow.statusHistory', tag: 'info' }
}

const RUN_STRATEGY_META: Record<string, string> = {
  ALL_INSTANCES: 'dataflow.allInstances',
  SPECIFY_INSTANCES: 'dataflow.specifyInstances',
  FIXED_INSTANCE_NUMBER: 'dataflow.fixedInstanceNumber'
}

function statusMeta(status: string) {
  const meta = STATUS_META[status]
  return meta ? { label: t(meta.labelKey), tag: meta.tag } : { label: status, tag: 'info' as const }
}

function runStrategyLabel(value?: string) {
  if (!value) return '-'
  const key = RUN_STRATEGY_META[value]
  return key ? t(key) : value
}

async function load() {
  if (!flowCode) {
    ElMessage.error(t('history.missingCode'))
    goBack()
    return
  }
  loading.value = true
  try {
    const res = await listPublishHistory(flowCode, page.current, page.size)
    list.value = res.records
    page.total = res.total
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/dataflow')
}

async function openDetail(row: DataFlowPublishItem) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getPublishDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function handleRollback(row: DataFlowPublishItem) {
  await ElMessageBox.confirm(
    t('history.rollbackConfirm', { version: row.version }),
    t('history.rollbackTitle'),
    { type: 'warning', confirmButtonText: t('history.rollback'), cancelButtonText: t('common.cancel') }
  )
  await rollbackDataFlow(row.id)
  ElMessage.success(t('history.rollbackSuccess'))
  goBack()
}

async function handleDelete(row: DataFlowPublishItem) {
  await ElMessageBox.confirm(t('history.deleteConfirm', { version: row.version }), t('dataflow.prompt'), { type: 'warning' })
  await deletePublish(row.id)
  ElMessage.success(t('history.deleteSuccess'))
  load()
}

onMounted(load)
</script>

<style scoped>
.history-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.history-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
