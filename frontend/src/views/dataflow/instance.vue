<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item label="任务流">
            <el-select
              v-model="query.flowId"
              placeholder="全部"
              clearable
              filterable
              style="width: 200px"
            >
              <el-option v-for="f in flows" :key="f.id" :label="f.name" :value="f.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="触发方式">
            <el-select v-model="query.triggerType" placeholder="全部" clearable style="width: 120px">
              <el-option label="手动" value="MANUAL" />
              <el-option label="定时" value="CRON" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
              <el-option label="运行中" value="RUNNING" />
              <el-option label="成功" value="SUCCESS" />
              <el-option label="失败" value="FAIL" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键字">
            <el-input
              v-model="query.keyword"
              placeholder="名称/编码"
              clearable
              style="width: 160px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="load">刷新</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="flowName" label="任务流" min-width="160" show-overflow-tooltip />
        <el-table-column label="触发方式" width="90">
          <template #default="{ row }">
            {{ TRIGGER_META[row.triggerType] ?? row.triggerType }}
          </template>
        </el-table-column>
        <el-table-column label="失败策略" width="90">
          <template #default="{ row }">
            {{ FAILURE_STRATEGY_META[row.failureStrategy] ?? row.failureStrategy ?? '继续' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="flowStatusMeta(row.status).tag">
              {{ flowStatusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">{{ formatDuration(row.durationMs) }}</template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column prop="errorMsg" label="错误信息" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="任务流实例详情" width="860px" top="5vh" @close="stopPolling">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="3" border class="detail-meta">
          <el-descriptions-item label="任务流">{{ detail.flowName }}</el-descriptions-item>
          <el-descriptions-item label="编码">{{ detail.flowCode }}</el-descriptions-item>
          <el-descriptions-item label="触发方式">
            {{ TRIGGER_META[detail.triggerType] ?? detail.triggerType }}
          </el-descriptions-item>
          <el-descriptions-item label="失败策略">
            {{ FAILURE_STRATEGY_META[detail.failureStrategy] ?? detail.failureStrategy ?? '继续' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="flowStatusMeta(detail.status).tag">
              {{ flowStatusMeta(detail.status).label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="耗时">{{ formatDuration(detail.durationMs) }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ detail.startTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="错误信息" :span="3">
            {{ detail.errorMsg || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="node-title">节点运行记录</div>
        <el-collapse v-if="detail?.nodes?.length" v-model="activeNodes">
          <el-collapse-item
            v-for="node in detail.nodes"
            :key="node.id"
            :name="String(node.id)"
          >
            <template #title>
              <div class="node-header">
                <el-tag size="small" :type="nodeStatusMeta(node.status).tag" class="node-status">
                  {{ nodeStatusMeta(node.status).label }}
                </el-tag>
                <span class="node-name">{{ node.nodeName || node.taskName || node.nodeId }}</span>
                <span class="node-type">{{ node.taskType }}</span>
                <span class="node-meta">{{ formatDuration(node.durationMs) }}</span>
                <span v-if="node.rowCount != null" class="node-meta">{{ node.rowCount }} 行</span>
              </div>
            </template>

            <div class="node-body">
              <el-table
                v-if="node.taskType === 'SQL' && node.columns?.length"
                :data="node.rows ?? []"
                border
                size="small"
                max-height="260"
              >
                <el-table-column
                  v-for="(col, i) in node.columns"
                  :key="i"
                  :prop="String(i)"
                  :label="col"
                  min-width="120"
                  show-overflow-tooltip
                />
              </el-table>

              <div v-if="node.errorMsg" class="node-error">{{ node.errorMsg }}</div>

              <pre v-if="node.logContent" class="node-log">{{ node.logContent }}</pre>
            </div>
          </el-collapse-item>
        </el-collapse>
        <el-empty v-else :description="detail ? '无节点记录' : '加载中'" :image-size="60" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import {
  listDataFlow,
  listFlowInstance,
  getFlowInstanceDetail,
  type DataFlowListItem,
  type FlowInstanceItem,
  type FlowInstanceDetail
} from '../../api/dataflow'

const loading = ref(false)
const list = ref<FlowInstanceItem[]>([])
const flows = ref<DataFlowListItem[]>([])
const query = reactive<{ flowId?: number; keyword: string; status: string; triggerType: string }>({
  flowId: undefined,
  keyword: '',
  status: '',
  triggerType: ''
})
const page = reactive({ current: 1, size: 10, total: 0 })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<FlowInstanceDetail | null>(null)
const activeNodes = ref<string[]>([])
const currentDetailId = ref<number | null>(null)
const detailTimer = ref<number | null>(null)

function hasRunningNode(d: FlowInstanceDetail | null) {
  if (!d) return false
  return d.status === 'RUNNING' || !!d.nodes?.some((n) => n.status === 'RUNNING')
}

function scrollLogsToBottom() {
  document.querySelectorAll('.node-log').forEach((el) => {
    el.scrollTop = el.scrollHeight
  })
}

async function refreshDetail() {
  if (!currentDetailId.value) return
  try {
    const d = await getFlowInstanceDetail(currentDetailId.value)
    detail.value = d
    await nextTick()
    scrollLogsToBottom()
    if (!hasRunningNode(d)) {
      stopPolling()
    }
  } catch {
    /* 轮询失败忽略，下一轮重试 */
  }
}

function startPolling() {
  stopPolling()
  detailTimer.value = window.setInterval(refreshDetail, 1500)
}

function stopPolling() {
  if (detailTimer.value != null) {
    clearInterval(detailTimer.value)
    detailTimer.value = null
  }
}

const TRIGGER_META: Record<string, string> = { MANUAL: '手动', CRON: '定时' }
const FAILURE_STRATEGY_META: Record<string, string> = { CONTINUE: '继续', END: '结束' }

const FLOW_STATUS_META: Record<string, { label: string; tag: 'primary' | 'success' | 'danger' | 'info' }> = {
  RUNNING: { label: '运行中', tag: 'primary' },
  SUCCESS: { label: '成功', tag: 'success' },
  FAIL: { label: '失败', tag: 'danger' }
}

const NODE_STATUS_META: Record<string, { label: string; tag: 'primary' | 'success' | 'danger' | 'info' }> = {
  RUNNING: { label: '运行中', tag: 'primary' },
  SUCCESS: { label: '成功', tag: 'success' },
  FAIL: { label: '失败', tag: 'danger' },
  SKIP: { label: '跳过', tag: 'info' }
}

function flowStatusMeta(status: string) {
  return FLOW_STATUS_META[status] ?? { label: status, tag: 'info' as const }
}

function nodeStatusMeta(status: string) {
  return NODE_STATUS_META[status] ?? { label: status, tag: 'info' as const }
}

function formatDuration(ms?: number | null) {
  if (ms == null) return '-'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

async function load() {
  loading.value = true
  try {
    const res = await listFlowInstance(
      {
        flowId: query.flowId,
        keyword: query.keyword || undefined,
        status: query.status || undefined,
        triggerType: query.triggerType || undefined
      },
      page.current,
      page.size
    )
    list.value = res.records
    page.total = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.current = 1
  load()
}

async function loadFlows() {
  try {
    const res = await listDataFlow({}, 1, 100)
    flows.value = res.records ?? []
  } catch {
    /* 忽略 */
  }
}

async function openDetail(row: FlowInstanceItem) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  activeNodes.value = []
  stopPolling()
  currentDetailId.value = row.id
  try {
    detail.value = await getFlowInstanceDetail(row.id)
    if (detail.value?.nodes?.length) {
      activeNodes.value = detail.value.nodes.map((n) => String(n.id))
    }
    if (hasRunningNode(detail.value)) {
      startPolling()
    }
  } catch (e) {
    ElMessage.error('加载实例详情失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  const flowId = Number((new URLSearchParams(location.search).get('flowId')) ?? 0)
  if (flowId) query.flowId = flowId
  loadFlows()
  load()
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.detail-meta {
  margin-bottom: 16px;
}

.node-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.node-header {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.node-status {
  flex-shrink: 0;
}

.node-name {
  font-weight: 600;
  color: #303133;
}

.node-type {
  font-size: 12px;
  color: #909399;
}

.node-meta {
  font-size: 12px;
  color: #909399;
}

.node-body {
  padding: 4px 0;
}

.node-error {
  margin: 8px 0;
  padding: 8px 10px;
  background: #fef0f0;
  color: #f56c6c;
  border-radius: 4px;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
}

.node-log {
  margin: 8px 0 0;
  padding: 10px;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  max-height: 260px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
