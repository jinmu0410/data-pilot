<template>
  <div>
    <el-card shadow="never">
      <div class="history-header">
        <el-button link :icon="ArrowLeft" @click="goBack">返回</el-button>
        <span class="history-title">发布记录 - {{ flowName }}</span>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column prop="publishDescription" label="发布说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).tag">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="warning" @click="handleRollback(row)">回滚</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="detailVisible" title="发布详情" width="640px" top="6vh">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="2" border>
          <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="编码">{{ detail.code }}</el-descriptions-item>
          <el-descriptions-item label="版本">{{ detail.publishVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusMeta(detail.status).tag">{{ statusMeta(detail.status).label }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="节点数">
            {{ detail.design?.nodes?.length ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="运行策略">
            {{ RUN_STRATEGY_META[detail.runStrategy] ?? detail.runStrategy ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="实例数量">
            {{ detail.instanceNumber ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="指定实例">
            {{ (detail.specifyInstances ?? []).join(', ') || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="监控">
            {{ detail.enableMonitor === 'ENABLE' ? '开启' : '关闭' }}
          </el-descriptions-item>
          <el-descriptions-item label="告警">
            {{ detail.enableAlarm === 'ENABLE' ? '开启' : '关闭' }}
          </el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detail.updateTime }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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

const flowCode = (route.query.code as string) || ''
const flowName = (route.query.name as string) || ''

const loading = ref(false)
const list = ref<DataFlowPublishItem[]>([])
const page = reactive({ current: 1, size: 10, total: 0 })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<DataFlowPublishDetail | null>(null)

const STATUS_META: Record<string, { label: string; tag: 'success' | 'warning' | 'info' | 'danger' }> = {
  TBP: { label: '待发布', tag: 'info' },
  ENABLE: { label: '启用', tag: 'success' },
  PAUSE: { label: '暂停', tag: 'warning' },
  HISTORY: { label: '历史', tag: 'info' }
}

const RUN_STRATEGY_META: Record<string, string> = {
  ALL_INSTANCES: '全部实例',
  SPECIFY_INSTANCES: '指定实例',
  FIXED_INSTANCE_NUMBER: '固定实例数'
}

function statusMeta(status: string) {
  return STATUS_META[status] ?? { label: status, tag: 'info' as const }
}

async function load() {
  if (!flowCode) {
    ElMessage.error('缺少任务流编码')
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
    `确认回滚到版本「${row.version}」？回滚后任务流将变为待发布状态，需重新发布生效。`,
    '回滚提示',
    { type: 'warning', confirmButtonText: '回滚', cancelButtonText: '取消' }
  )
  await rollbackDataFlow(row.id)
  ElMessage.success('回滚成功，任务流已置为待发布')
  goBack()
}

async function handleDelete(row: DataFlowPublishItem) {
  await ElMessageBox.confirm(`确认删除发布记录「${row.version}」？`, '提示', { type: 'warning' })
  await deletePublish(row.id)
  ElMessage.success('删除成功')
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
