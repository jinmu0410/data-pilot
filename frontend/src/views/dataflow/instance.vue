<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item :label="t('instance.flow')">
            <el-select
              v-model="query.flowId"
              :placeholder="t('instance.all')"
              clearable
              filterable
              style="width: 200px"
            >
              <el-option v-for="f in flows" :key="f.id" :label="f.name" :value="f.id" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('instance.triggerType')">
            <el-select v-model="query.triggerType" :placeholder="t('instance.all')" clearable style="width: 120px">
              <el-option :label="t('instance.manual')" value="MANUAL" />
              <el-option :label="t('instance.cron')" value="CRON" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('instance.status')">
            <el-select v-model="query.status" :placeholder="t('instance.all')" clearable style="width: 120px">
              <el-option :label="t('instance.running')" value="RUNNING" />
              <el-option :label="t('instance.success')" value="SUCCESS" />
              <el-option :label="t('instance.fail')" value="FAIL" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('instance.keyword')">
            <el-input
              v-model="query.keyword"
              :placeholder="t('instance.keywordPlaceholder')"
              clearable
              style="width: 160px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">{{ t('common.search') }}</el-button>
            <el-button :icon="Refresh" @click="load">{{ t('instance.refresh') }}</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="flowName" :label="t('instance.flow')" min-width="160" show-overflow-tooltip />
        <el-table-column :label="t('instance.triggerType')" width="90">
          <template #default="{ row }">
            {{ TRIGGER_META[row.triggerType] ?? row.triggerType }}
          </template>
        </el-table-column>
        <el-table-column :label="t('instance.colFailureStrategy')" width="90">
          <template #default="{ row }">
            {{ FAILURE_STRATEGY_META[row.failureStrategy] ?? row.failureStrategy ?? t('instance.continue') }}
          </template>
        </el-table-column>
        <el-table-column :label="t('instance.status')" width="90">
          <template #default="{ row }">
            <el-tag :type="flowStatusMeta(row.status).tag">
              {{ flowStatusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('instance.colDuration')" width="100">
          <template #default="{ row }">{{ formatDuration(row.durationMs) }}</template>
        </el-table-column>
        <el-table-column prop="startTime" :label="t('instance.colStartTime')" width="170" />
        <el-table-column prop="endTime" :label="t('instance.colEndTime')" width="170" />
        <el-table-column prop="errorMsg" :label="t('instance.colErrorMsg')" min-width="160" show-overflow-tooltip />
        <el-table-column :label="t('instance.colActions')" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ t('instance.detail') }}</el-button>
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

    <el-dialog
      v-model="detailVisible"
      width="min(1180px, calc(100vw - 40px))"
      top="4vh"
      append-to-body
      destroy-on-close
      :close-on-click-modal="false"
      class="instance-detail-dialog"
      @close="stopPolling"
    >
      <template #header>
        <div class="dialog-heading">
          <span class="dialog-logo">RUN</span>
          <div class="dialog-heading-copy">
            <div class="dialog-title">{{ t('instance.detailTitle') }}</div>
            <div class="dialog-subtitle">{{ t('instance.detailSubtitle') }}</div>
          </div>
          <span v-if="detail" class="flow-status" :class="statusClass(detail.status)">
            <i></i>{{ flowStatusMeta(detail.status).label }}
          </span>
        </div>
      </template>

      <div v-loading="detailLoading" class="detail-shell">
        <template v-if="detail">
          <section class="instance-overview">
            <div class="flow-identity">
              <div class="identity-topline">
                <span class="eyebrow">WORKFLOW INSTANCE</span>
                <span class="instance-id">#{{ detail.id }}</span>
              </div>
              <h3>{{ detail.flowName }}</h3>
              <div class="identity-meta">
                <span>{{ detail.flowCode }}</span>
                <i></i>
                <span>{{ TRIGGER_META[detail.triggerType] ?? detail.triggerType }}</span>
                <i></i>
                <span>失败后{{ FAILURE_STRATEGY_META[detail.failureStrategy] ?? detail.failureStrategy ?? t('instance.continue') }}</span>
              </div>
            </div>

            <div class="overview-metrics">
              <div class="metric-card">
                <span>{{ t('instance.progress') }}</span>
                <strong>{{ completedNodeCount }} / {{ detail.nodes?.length ?? 0 }}</strong>
                <el-progress :percentage="progressPercent" :show-text="false" :stroke-width="5" />
              </div>
              <div class="metric-card">
                <span>{{ t('instance.duration') }}</span>
                <strong>{{ formatDuration(detail.durationMs) }}</strong>
                <small>{{ detail.status === 'RUNNING' ? t('instance.runningHint') : t('instance.totalDuration') }}</small>
              </div>
              <div class="metric-card metric-time">
                <span>{{ t('instance.startTime') }}</span>
                <strong>{{ detail.startTime || '-' }}</strong>
                <small>{{ t('instance.endAt', { time: detail.endTime || t('instance.notEnded') }) }}</small>
              </div>
            </div>
          </section>

          <div v-if="detail.errorMsg" class="flow-error-banner">
            <span class="error-symbol">!</span>
            <div><strong>{{ t('instance.flowError') }}</strong><p>{{ detail.errorMsg }}</p></div>
          </div>

          <section v-if="detail.nodes?.length" class="execution-workspace">
            <aside class="node-rail">
              <div class="panel-heading">
                <div><span class="eyebrow">EXECUTION PATH</span><h4>{{ t('instance.executionPath') }}</h4></div>
                <span>{{ t('instance.nodeCount', { count: detail.nodes.length }) }}</span>
              </div>

              <div class="node-list">
                <button
                  v-for="(node, index) in detail.nodes"
                  :key="node.id"
                  type="button"
                  class="node-track-item"
                  :class="[{ active: selectedNodeId === node.id }, statusClass(node.status)]"
                  @click="selectedNodeId = node.id"
                >
                  <span class="track-line" :class="{ hidden: index === detail.nodes.length - 1 }"></span>
                  <span class="track-dot"><i></i></span>
                  <span class="track-index">{{ String(index + 1).padStart(2, '0') }}</span>
                  <span class="track-copy">
                    <strong>{{ node.nodeName || node.taskName || node.nodeId }}</strong>
                    <small>{{ node.taskType }} · {{ formatDuration(node.durationMs) }}</small>
                  </span>
                  <span class="track-status">{{ nodeStatusMeta(node.status).label }}</span>
                </button>
              </div>
            </aside>

            <main v-if="selectedNode" class="node-detail-panel">
              <header class="node-detail-head">
                <div class="node-type-icon">{{ taskTypeAbbr(selectedNode.taskType) }}</div>
                <div class="node-title-copy">
                  <div class="node-title-line">
                    <h4>{{ selectedNode.nodeName || selectedNode.taskName || selectedNode.nodeId }}</h4>
                    <span class="node-type-pill">{{ selectedNode.taskType }}</span>
                  </div>
                  <span>{{ selectedNode.taskCode || selectedNode.nodeId }}</span>
                </div>
                <span class="flow-status node-current-status" :class="statusClass(selectedNode.status)">
                  <i></i>{{ nodeStatusMeta(selectedNode.status).label }}
                </span>
              </header>

              <div class="node-facts">
                <div><span>{{ t('instance.duration') }}</span><strong>{{ formatDuration(selectedNode.durationMs) }}</strong></div>
                <div><span>{{ t('instance.rowCount') }}</span><strong>{{ selectedNode.rowCount == null ? '-' : t('instance.rowsUnit', { count: selectedNode.rowCount }) }}</strong></div>
                <div><span>{{ t('instance.startTime') }}</span><strong>{{ selectedNode.startTime || '-' }}</strong></div>
                <div><span>{{ t('instance.endTime') }}</span><strong>{{ selectedNode.endTime || '-' }}</strong></div>
              </div>

              <div v-if="selectedNode.errorMsg" class="node-error">
                <div class="content-heading"><span>ERROR</span><strong>{{ t('instance.errorInfo') }}</strong></div>
                <p>{{ selectedNode.errorMsg }}</p>
              </div>

              <section v-if="selectedNode.taskType === 'SQL' && selectedNode.columns?.length" class="node-content-section">
                <div class="content-heading">
                  <span>RESULT</span>
                  <strong>{{ t('instance.queryResult') }}</strong>
                  <small>{{ selectedNode.rowCount ?? selectedNode.rows?.length ?? 0 }} 行{{ selectedNode.truncated ? t('instance.rowsTruncated') : '' }}</small>
                </div>
                <el-table :data="selectedNode.rows ?? []" border size="small" max-height="260" class="result-table">
                  <el-table-column
                    v-for="(col, i) in selectedNode.columns"
                    :key="i"
                    :prop="String(i)"
                    :label="col"
                    min-width="120"
                    show-overflow-tooltip
                  />
                </el-table>
              </section>

              <section class="node-content-section log-section">
                <div class="content-heading">
                  <span>LOG</span>
                  <strong>{{ t('instance.runLog') }}</strong>
                  <small v-if="selectedNode.status === 'RUNNING'" class="live-label"><i></i>{{ t('instance.liveUpdate') }}</small>
                </div>
                <pre v-if="selectedNode.logContent" class="node-log">{{ selectedNode.logContent }}</pre>
                <div v-else class="empty-log">{{ t('instance.emptyLog') }}</div>
              </section>
            </main>
          </section>

          <el-empty v-else :description="t('instance.noNodeRecords')" :image-size="72" class="detail-empty" />
        </template>
        <el-empty v-else :description="t('instance.loadingDetail')" :image-size="72" class="detail-empty" />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <span v-if="detail?.status === 'RUNNING'" class="polling-tip"><i></i>{{ t('instance.pollingTip') }}</span>
          <el-button @click="detailVisible = false">{{ t('instance.close') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()
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
const selectedNodeId = ref<number | null>(null)
const currentDetailId = ref<number | null>(null)
const detailTimer = ref<number | null>(null)

const selectedNode = computed(() => detail.value?.nodes?.find((node) => node.id === selectedNodeId.value) ?? null)
const completedNodeCount = computed(() => detail.value?.nodes?.filter((node) => node.status !== 'RUNNING').length ?? 0)
const progressPercent = computed(() => {
  const total = detail.value?.nodes?.length ?? 0
  return total ? Math.round((completedNodeCount.value / total) * 100) : 0
})

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
    if (!selectedNodeId.value || !d.nodes?.some((node) => node.id === selectedNodeId.value)) {
      selectedNodeId.value = preferredNodeId(d)
    }
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

const TRIGGER_META = computed<Record<string, string>>(() => ({ MANUAL: t('instance.manual'), CRON: t('instance.cron') }))
const FAILURE_STRATEGY_META = computed<Record<string, string>>(() => ({ CONTINUE: t('instance.continue'), END: t('instance.end') }))

function flowStatusMeta(status: string) {
  const map: Record<string, { label: string; tag: 'primary' | 'success' | 'danger' | 'info' }> = {
    RUNNING: { label: t('instance.running'), tag: 'primary' },
    SUCCESS: { label: t('instance.success'), tag: 'success' },
    FAIL: { label: t('instance.fail'), tag: 'danger' }
  }
  return map[status] ?? { label: status, tag: 'info' as const }
}

function nodeStatusMeta(status: string) {
  const map: Record<string, { label: string; tag: 'primary' | 'success' | 'danger' | 'info' }> = {
    RUNNING: { label: t('instance.running'), tag: 'primary' },
    SUCCESS: { label: t('instance.success'), tag: 'success' },
    FAIL: { label: t('instance.fail'), tag: 'danger' },
    SKIP: { label: t('instance.skip'), tag: 'info' }
  }
  return map[status] ?? { label: status, tag: 'info' as const }
}

function statusClass(status?: string) {
  return `status-${(status || 'unknown').toLowerCase()}`
}

function taskTypeAbbr(type?: string) {
  const abbr: Record<string, string> = { DATAX: 'DX', SQL: 'SQL', PYTHON: 'PY', SHELL: 'SH', SEATUNNEL: 'ST' }
  return abbr[type || ''] ?? (type || 'TASK').slice(0, 3)
}

function preferredNodeId(d: FlowInstanceDetail) {
  return d.nodes?.find((node) => node.status === 'RUNNING')?.id
    ?? d.nodes?.find((node) => node.status === 'FAIL')?.id
    ?? d.nodes?.[0]?.id
    ?? null
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
  selectedNodeId.value = null
  stopPolling()
  currentDetailId.value = row.id
  try {
    detail.value = await getFlowInstanceDetail(row.id)
    selectedNodeId.value = preferredNodeId(detail.value)
    if (hasRunningNode(detail.value)) {
      startPolling()
    }
  } catch (e) {
    ElMessage.error(t('instance.loadDetailFailed'))
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

.dialog-heading { display: flex; align-items: center; gap: 12px; padding-right: 38px; }
.dialog-logo { width: 40px; height: 40px; display: grid; place-items: center; flex: 0 0 auto; border-radius: 11px; color: #fff; background: linear-gradient(145deg, #635bff, #8b5cf6); box-shadow: 0 8px 18px rgba(99, 91, 255, .24); font-size: 10px; font-weight: 800; letter-spacing: .5px; }
.dialog-heading-copy { min-width: 0; }
.dialog-title { color: #172033; font-size: 17px; font-weight: 700; }
.dialog-subtitle { margin-top: 3px; color: #8a94a6; font-size: 12px; }
.detail-shell { min-height: 500px; max-height: calc(92vh - 150px); overflow-y: auto; color: #344054; }
.instance-overview { display: grid; grid-template-columns: minmax(220px, .85fr) minmax(0, 1.6fr); gap: 18px; padding: 20px; border: 1px solid #e7e9f1; border-radius: 12px; background: linear-gradient(135deg, #fbfbff, #f7f8fc); }
.flow-identity { display: flex; min-width: 0; flex-direction: column; justify-content: center; padding: 3px 7px; }
.identity-topline { display: flex; align-items: center; gap: 9px; }
.eyebrow { color: #7669ef; font-size: 9px; font-weight: 800; letter-spacing: 1.6px; }
.instance-id { padding: 2px 6px; border-radius: 4px; color: #7d8698; background: #eceef4; font-size: 9px; font-weight: 700; }
.flow-identity h3 { overflow: hidden; margin: 8px 0 7px; color: #20283a; font-size: 20px; text-overflow: ellipsis; white-space: nowrap; }
.identity-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 7px; color: #818a9a; font-size: 10px; }
.identity-meta i { width: 3px; height: 3px; border-radius: 50%; background: #c4c8d1; }
.overview-metrics { display: grid; grid-template-columns: .85fr .85fr 1.3fr; gap: 10px; }
.metric-card { min-width: 0; padding: 13px 14px; border: 1px solid #eaecf2; border-radius: 9px; background: rgba(255, 255, 255, .88); }
.metric-card > span, .metric-card > small, .metric-card > strong { display: block; }
.metric-card > span { color: #969ead; font-size: 9px; }
.metric-card > strong { margin: 7px 0 6px; color: #30394c; font-size: 17px; line-height: 1.2; }
.metric-card > small { overflow: hidden; color: #a0a7b5; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.metric-time > strong { margin-top: 9px; font-size: 11px; }
.flow-status { display: inline-flex; align-items: center; gap: 6px; margin-left: auto; padding: 5px 10px; border: 1px solid #dfe3ea; border-radius: 999px; color: #667085; background: #f6f7f9; font-size: 11px; font-weight: 650; }
.flow-status i { width: 6px; height: 6px; border-radius: 50%; background: #98a2b3; }
.flow-status.status-running { color: #4f46d6; border-color: #d9d5ff; background: #f1efff; }
.flow-status.status-running i { background: #635bff; box-shadow: 0 0 0 3px rgba(99, 91, 255, .13); animation: status-pulse 1.4s infinite; }
.flow-status.status-success { color: #08775a; border-color: #bce9dc; background: #edf9f5; }
.flow-status.status-success i { background: #10b981; }
.flow-status.status-fail { color: #c23f4c; border-color: #f2c7cc; background: #fff1f2; }
.flow-status.status-fail i { background: #ef5361; }
@keyframes status-pulse { 50% { box-shadow: 0 0 0 5px rgba(99, 91, 255, 0); } }
.flow-error-banner { display: flex; align-items: flex-start; gap: 11px; margin-top: 13px; padding: 12px 14px; border: 1px solid #f1c9cd; border-radius: 9px; color: #a6323f; background: #fff5f5; }
.error-symbol { width: 22px; height: 22px; display: grid; place-items: center; flex: 0 0 auto; border-radius: 50%; color: #fff; background: #e34d59; font-size: 12px; font-weight: 800; }
.flow-error-banner strong { font-size: 11px; }
.flow-error-banner p { margin: 4px 0 0; color: #bd5a64; font-size: 10px; line-height: 1.55; white-space: pre-wrap; word-break: break-all; }
.execution-workspace { min-height: 405px; display: grid; grid-template-columns: 280px minmax(0, 1fr); margin-top: 14px; overflow: hidden; border: 1px solid #e6e9ef; border-radius: 12px; background: #fff; }
.node-rail { min-width: 0; padding: 16px 12px; border-right: 1px solid #e8eaf0; background: #fafbfc; }
.panel-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 10px; padding: 0 8px 13px; }
.panel-heading h4 { margin: 3px 0 0; color: #293247; font-size: 13px; }
.panel-heading > span { color: #9aa2b1; font-size: 9px; }
.node-list { max-height: 455px; overflow-y: auto; padding: 1px 4px 1px 0; }
.node-track-item { position: relative; width: 100%; min-height: 66px; display: flex; align-items: center; gap: 9px; padding: 9px 8px 9px 7px; border: 1px solid transparent; border-radius: 9px; color: #687286; text-align: left; background: transparent; cursor: pointer; transition: .18s ease; }
.node-track-item:hover { background: #f3f4f8; }
.node-track-item.active { border-color: #d9d5ff; background: #f2f0ff; box-shadow: 0 3px 10px rgba(82, 72, 190, .06); }
.track-line { position: absolute; z-index: 0; top: 42px; left: 19px; width: 1px; height: 42px; background: #dfe3ea; }
.track-line.hidden { display: none; }
.track-dot { position: relative; z-index: 1; width: 25px; height: 25px; display: grid; place-items: center; flex: 0 0 auto; border: 1px solid #d7dce5; border-radius: 50%; background: #fff; }
.track-dot i { width: 7px; height: 7px; border-radius: 50%; background: #98a2b3; }
.status-success .track-dot { border-color: #9fddcd; }.status-success .track-dot i { background: #10b981; }
.status-running .track-dot { border-color: #c7c1ff; }.status-running .track-dot i { background: #635bff; box-shadow: 0 0 0 3px rgba(99, 91, 255, .12); }
.status-fail .track-dot { border-color: #f1b9bf; }.status-fail .track-dot i { background: #ef5361; }
.status-skip .track-dot i { background: #a8afbb; }
.track-index { color: #b0b6c2; font-size: 9px; font-weight: 700; }
.track-copy { min-width: 0; flex: 1; }
.track-copy strong, .track-copy small { display: block; }
.track-copy strong { overflow: hidden; color: #354052; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.track-copy small { margin-top: 4px; color: #969ead; font-size: 9px; }
.track-status { flex: 0 0 auto; font-size: 9px; }
.status-success .track-status { color: #079669; }.status-running .track-status { color: #635bff; }.status-fail .track-status { color: #dc4452; }
.node-detail-panel { min-width: 0; padding: 19px 20px 21px; overflow-y: auto; max-height: 493px; }
.node-detail-head { display: flex; align-items: center; gap: 12px; padding-bottom: 16px; border-bottom: 1px solid #eceef3; }
.node-type-icon { width: 39px; height: 39px; display: grid; place-items: center; flex: 0 0 auto; border-radius: 10px; color: #fff; background: linear-gradient(145deg, #3b82f6, #6366f1); font-size: 9px; font-weight: 800; }
.node-title-copy { min-width: 0; flex: 1; }
.node-title-line { display: flex; align-items: center; gap: 8px; }
.node-title-line h4 { overflow: hidden; margin: 0; color: #253047; font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }
.node-title-copy > span { display: block; margin-top: 5px; color: #9aa2b1; font-size: 9px; }
.node-type-pill { padding: 3px 6px; border-radius: 4px; color: #516079; background: #f0f2f6; font-size: 8px; font-weight: 700; }
.node-current-status { margin-left: 8px; }
.node-facts { display: grid; grid-template-columns: .8fr .8fr 1.2fr 1.2fr; gap: 8px; margin: 14px 0; }
.node-facts > div { min-width: 0; padding: 10px 11px; border-radius: 7px; background: #f7f8fa; }
.node-facts span, .node-facts strong { display: block; }
.node-facts span { color: #9ca3af; font-size: 8px; }
.node-facts strong { overflow: hidden; margin-top: 5px; color: #4b5568; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.node-error { margin: 13px 0; padding: 12px 13px; border: 1px solid #f3c5ca; border-radius: 8px; background: #fff5f5; }
.node-error p { margin: 8px 0 0; color: #bd4854; font-size: 10px; line-height: 1.6; white-space: pre-wrap; word-break: break-all; }
.node-content-section { margin-top: 15px; }
.content-heading { display: flex; align-items: center; gap: 8px; margin-bottom: 9px; }
.content-heading > span { padding: 3px 5px; border-radius: 4px; color: #6758e8; background: #efedff; font-size: 8px; font-weight: 800; letter-spacing: .6px; }
.content-heading > strong { color: #3b4558; font-size: 11px; }
.content-heading > small { margin-left: auto; color: #9aa2b1; font-size: 9px; }
.result-table { border-radius: 7px; }
.log-section { margin-top: 16px; }
.live-label { display: inline-flex; align-items: center; gap: 5px; color: #7669ef !important; }
.live-label i, .polling-tip i { width: 6px; height: 6px; border-radius: 50%; background: #635bff; animation: status-pulse 1.4s infinite; }
.node-log { min-height: 130px; max-height: 270px; box-sizing: border-box; margin: 0; overflow: auto; padding: 14px 15px; border: 1px solid #283246; border-radius: 8px; color: #d6dbe5; background: #1d2533; font: 10px/1.7 ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; white-space: pre-wrap; word-break: break-all; }
.empty-log { height: 115px; display: grid; place-items: center; border: 1px dashed #dfe2e9; border-radius: 8px; color: #a0a7b5; background: #fafbfc; font-size: 10px; }
.detail-empty { min-height: 500px; display: flex; align-items: center; justify-content: center; flex-direction: column; }
.dialog-footer { display: flex; align-items: center; justify-content: flex-end; width: 100%; }
.polling-tip { display: inline-flex; align-items: center; gap: 7px; margin-right: auto; color: #7e8797; font-size: 10px; }

@media (max-width: 900px) {
  .dialog-heading { padding-right: 28px; }
  .instance-overview { grid-template-columns: 1fr; }
  .overview-metrics { grid-template-columns: repeat(3, 1fr); }
  .execution-workspace { grid-template-columns: 220px minmax(0, 1fr); }
  .node-facts { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 680px) {
  .overview-metrics { grid-template-columns: 1fr; }
  .execution-workspace { grid-template-columns: 1fr; }
  .node-rail { border-right: 0; border-bottom: 1px solid #e8eaf0; }
  .node-list { max-height: 190px; }
  .node-detail-panel { max-height: none; }
}
</style>

<style>
.instance-detail-dialog.el-dialog { overflow: hidden; border-radius: 14px; }
.instance-detail-dialog .el-dialog__header { margin-right: 0; padding: 18px 22px 14px; border-bottom: 1px solid #edf0f4; }
.instance-detail-dialog .el-dialog__body { padding: 16px 20px; }
.instance-detail-dialog .el-dialog__footer { padding: 13px 20px 17px; border-top: 1px solid #edf0f4; }
</style>
