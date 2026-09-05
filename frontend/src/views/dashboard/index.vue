<template>
  <div class="workplace" v-loading="loading">
    <section class="welcome-panel">
      <div class="welcome-copy">
        <span class="eyebrow"><i /> DATA WORKSPACE</span>
        <h1>{{ greeting }}，{{ username }}</h1>
        <p>{{ t('dashboard.welcome') }}</p>
        <div class="welcome-meta">
          <span><el-icon><Calendar /></el-icon>{{ today }}</span>
          <span><el-icon><Location /></el-icon>{{ workspaceName }}</span>
        </div>
      </div>
      <div class="welcome-visual" aria-hidden="true">
        <span class="visual-orbit orbit-one" />
        <span class="visual-orbit orbit-two" />
        <div class="visual-card card-source"><el-icon><Coin /></el-icon><i /></div>
        <div class="visual-card card-flow"><el-icon><Share /></el-icon><i /></div>
        <div class="visual-card card-api"><el-icon><Link /></el-icon><i /></div>
        <svg viewBox="0 0 250 130" role="img">
          <path d="M45 72 C85 10, 148 125, 206 54" />
          <path d="M48 76 C94 118, 151 19, 205 57" />
        </svg>
      </div>
    </section>

    <section class="stat-grid">
      <article v-for="item in stats" :key="item.label" class="stat-card">
        <div class="stat-icon" :class="item.tone"><el-icon><component :is="item.icon" /></el-icon></div>
        <div class="stat-content">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </div>
        <span class="stat-accent" :class="item.tone" />
      </article>
    </section>

    <section class="content-grid">
      <el-card shadow="never" class="project-panel">
        <template #header>
          <div class="panel-heading">
            <div><strong>{{ t('dashboard.quickWork') }}</strong><span>{{ t('dashboard.quickWorkDesc') }}</span></div>
            <el-button link type="primary" @click="router.push('/dataflow')">{{ t('dashboard.enterDev') }} <el-icon><ArrowRight /></el-icon></el-button>
          </div>
        </template>
        <div class="project-grid">
          <button v-for="item in quickActions" :key="item.path" class="project-card" type="button" @click="router.push(item.path)">
            <span class="project-icon" :class="item.tone"><el-icon><component :is="item.icon" /></el-icon></span>
            <span class="project-copy"><strong>{{ item.title }}</strong><small>{{ item.description }}</small></span>
            <span class="project-arrow"><el-icon><TopRight /></el-icon></span>
          </button>
        </div>
      </el-card>

      <el-card shadow="never" class="health-panel">
        <template #header>
          <div class="panel-heading">
            <div><strong>{{ t('dashboard.runOverview') }}</strong><span>{{ t('dashboard.runOverviewDesc') }}</span></div>
            <span class="live-label"><i /> {{ t('dashboard.live') }}</span>
          </div>
        </template>
        <div class="health-overview">
          <div class="health-ring" :style="ringStyle">
            <div><strong>{{ successRate }}%</strong><span>{{ t('dashboard.successRate') }}</span></div>
          </div>
          <div class="health-legend">
            <div><i class="success" /><span>{{ t('dashboard.success') }}</span><strong>{{ instanceStatus.success }}</strong></div>
            <div><i class="running" /><span>{{ t('dashboard.running') }}</span><strong>{{ instanceStatus.running }}</strong></div>
            <div><i class="failed" /><span>{{ t('dashboard.failedSkip') }}</span><strong>{{ instanceStatus.failed }}</strong></div>
          </div>
        </div>
        <div class="health-foot">
          <span><el-icon><CircleCheck /></el-icon>{{ t('dashboard.connectionOk') }}</span>
          <button type="button" @click="router.push('/dataflow/instance')">{{ t('dashboard.viewDetail') }}</button>
        </div>
      </el-card>
    </section>

    <section class="bottom-grid">
      <el-card shadow="never" class="recent-panel">
        <template #header>
          <div class="panel-heading">
            <div><strong>{{ t('dashboard.recentRun') }}</strong><span>{{ t('dashboard.recentRunDesc') }}</span></div>
            <el-button link type="primary" @click="router.push('/dataflow/instance')">{{ t('dashboard.allInstances') }}</el-button>
          </div>
        </template>
        <div v-if="recentInstances.length" class="recent-list">
          <button v-for="item in recentInstances" :key="item.id" type="button" class="recent-item" @click="router.push(`/dataflow/instance?id=${item.id}`)">
            <span class="recent-status" :class="statusTone(item.status)"><el-icon><VideoPlay /></el-icon></span>
            <span class="recent-main"><strong>{{ item.flowName || item.flowCode }}</strong><small>{{ item.triggerType === 'CRON' ? t('dashboard.cronTrigger') : t('dashboard.manualTrigger') }} · {{ formatTime(item.createTime) }}</small></span>
            <el-tag size="small" :type="statusTag(item.status)">{{ statusLabel(item.status) }}</el-tag>
            <span class="duration">{{ formatDuration(item.durationMs) }}</span>
          </button>
        </div>
        <el-empty v-else :description="t('dashboard.noInstance')" :image-size="88" />
      </el-card>

      <el-card shadow="never" class="flow-panel">
        <template #header>
          <div class="panel-heading">
            <div><strong>{{ t('dashboard.recentUpdate') }}</strong><span>{{ t('dashboard.recentUpdateDesc') }}</span></div>
            <el-button link type="primary" @click="router.push('/dataflow')">{{ t('dashboard.flowList') }}</el-button>
          </div>
        </template>
        <div v-if="recentFlows.length" class="flow-list">
          <button v-for="flow in recentFlows" :key="flow.id" type="button" @click="router.push(`/dataflow/edit/${flow.id}`)">
            <span class="flow-symbol"><el-icon><Operation /></el-icon></span>
            <span><strong>{{ flow.name }}</strong><small>{{ flow.code }}</small></span>
            <el-tag size="small" :type="flow.status === 'ENABLE' ? 'success' : 'info'">{{ flowStatus(flow.status) }}</el-tag>
          </button>
        </div>
        <el-empty v-else :description="t('dashboard.noFlow')" :image-size="88" />
      </el-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  Coin,
  Connection,
  DataAnalysis,
  Files,
  Link,
  Operation,
  Share,
  Timer
} from '@element-plus/icons-vue'
import { listDataSource } from '../../api/datasource'
import { listDataFlow, listFlowInstance, type DataFlowListItem, type FlowInstanceItem } from '../../api/dataflow'
import { listTemplate } from '../../api/service'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const { t, locale } = useI18n()
const authStore = useAuthStore()
const loading = ref(false)
const totals = ref({ datasource: 0, flow: 0, service: 0, instance: 0 })
const recentInstances = ref<FlowInstanceItem[]>([])
const recentFlows = ref<DataFlowListItem[]>([])

const username = computed(() => authStore.user?.username || t('layout.admin'))
const workspaceName = computed(() => authStore.workspace?.name || t('layout.defaultWorkspace'))
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return t('dashboard.greetingNight')
  if (hour < 12) return t('dashboard.greetingMorning')
  if (hour < 18) return t('dashboard.greetingAfternoon')
  return t('dashboard.greetingEvening')
})
const today = computed(() => new Intl.DateTimeFormat(locale.value, {
  month: 'long', day: 'numeric', weekday: 'long'
}).format(new Date()))

const stats = computed(() => [
  { label: t('dashboard.statDatasource'), value: totals.value.datasource, hint: t('dashboard.statDatasourceHint'), icon: Coin, tone: 'blue' },
  { label: t('dashboard.statFlow'), value: totals.value.flow, hint: t('dashboard.statFlowHint'), icon: Operation, tone: 'purple' },
  { label: t('dashboard.statApi'), value: totals.value.service, hint: t('dashboard.statApiHint'), icon: Link, tone: 'cyan' },
  { label: t('dashboard.statInstance'), value: totals.value.instance, hint: t('dashboard.statInstanceHint'), icon: Timer, tone: 'orange' }
])

const quickActions = computed(() => [
  { title: t('dashboard.actionDatasource'), description: t('dashboard.actionDatasourceDesc'), path: '/datasource', icon: Connection, tone: 'blue' },
  { title: t('dashboard.actionFlow'), description: t('dashboard.actionFlowDesc'), path: '/dataflow', icon: Operation, tone: 'purple' },
  { title: t('dashboard.actionApi'), description: t('dashboard.actionApiDesc'), path: '/service/api', icon: DataAnalysis, tone: 'cyan' },
  { title: t('dashboard.actionInstance'), description: t('dashboard.actionInstanceDesc'), path: '/dataflow/instance', icon: Files, tone: 'orange' }
])

const instanceStatus = computed(() => {
  const status = { success: 0, running: 0, failed: 0 }
  recentInstances.value.forEach((item) => {
    if (item.status === 'SUCCESS') status.success++
    else if (item.status === 'RUNNING') status.running++
    else status.failed++
  })
  return status
})
const successRate = computed(() => {
  const total = recentInstances.value.length
  return total ? Math.round((instanceStatus.value.success / total) * 100) : 100
})
const ringStyle = computed(() => ({
  background: `conic-gradient(var(--dp-success) 0 ${successRate.value}%, var(--dp-border) ${successRate.value}% 100%)`
}))

async function loadDashboard() {
  loading.value = true
  try {
    const [source, flow, service, instance] = await Promise.allSettled([
      listDataSource({}, 1, 1),
      listDataFlow({}, 1, 5),
      listTemplate({}, 1, 1),
      listFlowInstance({}, 1, 6)
    ])
    if (source.status === 'fulfilled') totals.value.datasource = source.value.total
    if (flow.status === 'fulfilled') {
      totals.value.flow = flow.value.total
      recentFlows.value = flow.value.records.slice(0, 4)
    }
    if (service.status === 'fulfilled') totals.value.service = service.value.total
    if (instance.status === 'fulfilled') {
      totals.value.instance = instance.value.total
      recentInstances.value = instance.value.records
    }
  } finally {
    loading.value = false
  }
}

function statusTone(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'RUNNING') return 'running'
  return 'failed'
}

function statusTag(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'RUNNING') return 'primary'
  if (status === 'SKIP') return 'info'
  return 'danger'
}

function statusLabel(status: string) {
  return ({ SUCCESS: t('dashboard.statusSuccess'), RUNNING: t('dashboard.statusRunning'), FAIL: t('dashboard.statusFail'), SKIP: t('dashboard.statusSkip') } as Record<string, string>)[status] || status
}

function flowStatus(status: string) {
  return ({ ENABLE: t('dashboard.flowEnable'), PAUSE: t('dashboard.flowPause'), TBP: t('dashboard.flowTBP'), HISTORY: t('dashboard.flowHistory') } as Record<string, string>)[status] || status
}

function formatTime(value: string) {
  if (!value) return '-'
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat(locale.value, { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date)
}

function formatDuration(value: number) {
  if (!value) return '<1s'
  return value < 1000 ? `${value}ms` : `${(value / 1000).toFixed(1)}s`
}

onMounted(loadDashboard)
</script>

<style scoped>
.workplace {
  display: grid;
  gap: 18px;
}

.welcome-panel {
  position: relative;
  display: flex;
  min-height: 202px;
  overflow: hidden;
  color: #fff;
  background:
    radial-gradient(circle at 72% -20%, rgba(154, 173, 255, 0.38), transparent 40%),
    linear-gradient(116deg, #344fc6 0%, #4e6dec 48%, #7358dc 100%);
  border-radius: 16px;
  box-shadow: 0 18px 38px rgba(56, 76, 176, 0.2);
}

.welcome-panel::before {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(255, 255, 255, 0.14) 0.8px, transparent 0.8px);
  background-size: 18px 18px;
  mask-image: linear-gradient(90deg, transparent, #000 55%, #000);
  content: '';
}

.welcome-copy {
  position: relative;
  z-index: 2;
  flex: 1;
  padding: 32px 38px;
}

.eyebrow {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 10px;
  font-weight: 650;
  letter-spacing: 1.8px;
}

.eyebrow i {
  width: 17px;
  height: 2px;
  background: #95ffd5;
}

.welcome-copy h1 {
  margin: 15px 0 8px;
  font-size: 26px;
  font-weight: 720;
  letter-spacing: 0.2px;
}

.welcome-copy p {
  margin: 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
}

.welcome-meta {
  display: flex;
  gap: 22px;
  margin-top: 25px;
}

.welcome-meta span {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 11px;
}

.welcome-visual {
  position: relative;
  width: 390px;
  min-width: 390px;
}

.welcome-visual svg {
  position: absolute;
  top: 35px;
  right: 45px;
  width: 260px;
  fill: none;
  stroke: rgba(255, 255, 255, 0.28);
  stroke-dasharray: 4 5;
  stroke-width: 1.2;
}

.visual-card {
  position: absolute;
  z-index: 2;
  display: grid;
  width: 54px;
  height: 54px;
  color: #5269d5;
  background: rgba(255, 255, 255, 0.93);
  border: 5px solid rgba(255, 255, 255, 0.18);
  border-radius: 16px;
  box-shadow: 0 16px 30px rgba(31, 35, 100, 0.2);
  place-items: center;
  backdrop-filter: blur(8px);
}

.visual-card i {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 10px;
  height: 10px;
  background: #4ce1a6;
  border: 2px solid white;
  border-radius: 50%;
}

.card-source { top: 61px; left: 44px; }
.card-flow { top: 26px; left: 164px; width: 64px; height: 64px; color: #8058dd; }
.card-api { top: 106px; left: 258px; color: #12a9b7; }

.visual-orbit {
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 50%;
}

.orbit-one { top: -60px; right: -30px; width: 250px; height: 250px; }
.orbit-two { right: 10px; bottom: -94px; width: 190px; height: 190px; }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 118px;
  overflow: hidden;
  padding: 20px;
  background: var(--dp-surface);
  border: 1px solid var(--dp-border);
  border-radius: 13px;
  box-shadow: var(--dp-shadow);
}

.stat-icon,
.project-icon {
  display: grid;
  color: var(--tone);
  background: color-mix(in srgb, var(--tone) 11%, transparent);
  border-radius: 11px;
  place-items: center;
}

.stat-icon {
  width: 47px;
  height: 47px;
  margin-right: 15px;
  font-size: 21px;
}

.blue { --tone: #4f6df5; }
.purple { --tone: #8b5cf6; }
.cyan { --tone: #13a9b8; }
.orange { --tone: #ee9b25; }

.stat-content {
  display: grid;
  min-width: 0;
}

.stat-content > span {
  color: var(--dp-text-secondary);
  font-size: 11px;
}

.stat-content strong {
  margin: 2px 0 3px;
  color: var(--dp-text);
  font-size: 25px;
  font-weight: 720;
}

.stat-content small {
  overflow: hidden;
  color: var(--dp-text-muted);
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stat-accent {
  position: absolute;
  top: 0;
  right: 0;
  width: 48px;
  height: 3px;
  background: var(--tone);
  border-radius: 0 12px 0 4px;
}

.content-grid,
.bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(300px, 0.8fr);
  gap: 18px;
}

.bottom-grid {
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.9fr);
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.panel-heading > div {
  display: flex;
  flex-direction: column;
}

.panel-heading strong {
  color: var(--dp-text);
  font-size: 14px;
  font-weight: 680;
}

.panel-heading span {
  margin-top: 3px;
  color: var(--dp-text-muted);
  font-size: 9px;
  font-weight: 450;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.project-card {
  display: flex;
  align-items: center;
  min-height: 78px;
  padding: 12px;
  color: var(--dp-text-secondary);
  text-align: left;
  background: var(--dp-surface-soft);
  border: 1px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: 0.18s ease;
}

.project-card:hover {
  background: var(--dp-surface);
  border-color: color-mix(in srgb, var(--dp-primary) 28%, var(--dp-border));
  box-shadow: 0 8px 20px rgba(34, 45, 77, 0.08);
  transform: translateY(-2px);
}

.project-icon {
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  margin-right: 11px;
  font-size: 19px;
}

.project-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.project-copy strong {
  color: var(--dp-text);
  font-size: 12px;
  font-weight: 620;
}

.project-copy small {
  margin-top: 5px;
  overflow: hidden;
  color: var(--dp-text-muted);
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-arrow {
  display: grid;
  width: 25px;
  height: 25px;
  color: var(--dp-text-muted);
  border: 1px solid var(--dp-border);
  border-radius: 7px;
  place-items: center;
}

.health-overview {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  min-height: 142px;
}

.health-ring {
  display: grid;
  width: 108px;
  height: 108px;
  border-radius: 50%;
  place-items: center;
}

.health-ring > div {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 82px;
  height: 82px;
  background: var(--dp-surface);
  border-radius: 50%;
}

.health-ring strong {
  color: var(--dp-text);
  font-size: 21px;
}

.health-ring span {
  color: var(--dp-text-muted);
  font-size: 9px;
}

.health-legend {
  display: grid;
  gap: 12px;
  min-width: 125px;
}

.health-legend div {
  display: grid;
  grid-template-columns: 7px 1fr auto;
  align-items: center;
  gap: 8px;
  color: var(--dp-text-secondary);
  font-size: 10px;
}

.health-legend i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.health-legend i.success { background: var(--dp-success); }
.health-legend i.running { background: var(--dp-primary); }
.health-legend i.failed { background: var(--dp-danger); }
.health-legend strong { color: var(--dp-text); }

.live-label {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--dp-success) !important;
}

.live-label i {
  width: 6px;
  height: 6px;
  background: var(--dp-success);
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(25, 179, 122, 0.12);
}

.health-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid var(--dp-border);
}

.health-foot span {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--dp-success);
  font-size: 10px;
}

.health-foot button {
  color: var(--dp-primary);
  background: transparent;
  border: 0;
  cursor: pointer;
  font-size: 10px;
}

.recent-list,
.flow-list {
  display: grid;
}

.recent-item,
.flow-list button {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 58px;
  padding: 8px 3px;
  color: var(--dp-text-secondary);
  text-align: left;
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--dp-border);
  cursor: pointer;
}

.recent-item:last-child,
.flow-list button:last-child {
  border-bottom: 0;
}

.recent-item:hover,
.flow-list button:hover {
  background: linear-gradient(90deg, var(--dp-primary-soft), transparent);
}

.recent-status,
.flow-symbol {
  display: grid;
  flex: 0 0 35px;
  width: 35px;
  height: 35px;
  margin-right: 10px;
  color: var(--dp-primary);
  background: var(--dp-primary-soft);
  border-radius: 9px;
  place-items: center;
}

.recent-status.success { color: var(--dp-success); background: rgba(25, 179, 122, 0.1); }
.recent-status.failed { color: var(--dp-danger); background: rgba(239, 91, 99, 0.1); }

.recent-main,
.flow-list button > span:nth-child(2) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.recent-main strong,
.flow-list strong {
  overflow: hidden;
  color: var(--dp-text);
  font-size: 11px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-main small,
.flow-list small {
  margin-top: 4px;
  color: var(--dp-text-muted);
  font-size: 9px;
}

.recent-item .el-tag,
.flow-list .el-tag {
  margin: 0 12px;
}

.duration {
  width: 56px;
  color: var(--dp-text-muted);
  font-size: 9px;
  text-align: right;
}

@media (max-width: 1100px) {
  .stat-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .welcome-visual { width: 330px; min-width: 330px; transform: translateX(20px); }
  .content-grid,
  .bottom-grid { grid-template-columns: 1fr; }
}

@media (max-width: 700px) {
  .welcome-panel { min-height: 220px; }
  .welcome-copy { padding: 28px 24px; }
  .welcome-copy h1 { font-size: 22px; }
  .welcome-visual { position: absolute; right: -170px; opacity: 0.42; }
  .stat-grid { grid-template-columns: 1fr; gap: 10px; }
  .stat-card { min-height: 96px; }
  .project-grid { grid-template-columns: 1fr; }
}

@media (max-width: 480px) {
  .welcome-meta { flex-direction: column; gap: 7px; }
  .recent-item .el-tag { display: none; }
}
</style>
