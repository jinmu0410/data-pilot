<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item :label="t('dataflow.keyword')">
            <el-input
              v-model="query.keyword"
              :placeholder="t('dataflow.keywordPlaceholder')"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item :label="t('dataflow.status')">
            <el-select v-model="query.status" :placeholder="t('dataflow.all')" clearable style="width: 130px">
              <el-option :label="t('dataflow.statusTBP')" value="TBP" />
              <el-option :label="t('dataflow.statusEnable')" value="ENABLE" />
              <el-option :label="t('dataflow.statusPause')" value="PAUSE" />
              <el-option :label="t('dataflow.statusHistory')" value="HISTORY" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">{{ t('common.search') }}</el-button>
            <el-button type="success" :icon="Plus" @click="openCreate">{{ t('dataflow.create') }}</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" :label="t('dataflow.colName')" min-width="150" />
        <el-table-column prop="code" :label="t('dataflow.colCode')" min-width="150" />
        <el-table-column :label="t('dataflow.colStatus')" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).tag">
              {{ statusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentVersion" :label="t('dataflow.colCurrentVersion')" width="110" />
        <el-table-column prop="publishVersion" :label="t('dataflow.colPublishVersion')" width="110" />
        <el-table-column prop="description" :label="t('dataflow.colDescription')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="updateTime" :label="t('dataflow.colUpdateTime')" width="170" />
        <el-table-column :label="t('dataflow.colActions')" width="480" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goEdit(row)">{{ t('dataflow.edit') }}</el-button>
            <el-button link type="success" @click="handleRun(row)">{{ t('dataflow.run') }}</el-button>
            <el-button link @click="goInstance(row)">{{ t('dataflow.instance') }}</el-button>
            <el-button link type="info" @click="openRunConfig(row)">{{ t('dataflow.runConfig') }}</el-button>
            <el-button link type="success" @click="openPublish(row)">{{ t('dataflow.publish') }}</el-button>
            <el-button link @click="goHistory(row)">{{ t('dataflow.publishHistory') }}</el-button>
            <el-button
              v-if="row.status === 'ENABLE'"
              link
              type="warning"
              @click="handleStop(row)"
            >{{ t('dataflow.stop') }}</el-button>
            <el-button
              v-if="row.status === 'PAUSE'"
              link
              type="success"
              @click="handleStart(row)"
            >{{ t('dataflow.start') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row)">{{ t('dataflow.delete') }}</el-button>
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

    <el-dialog v-model="createVisible" :title="t('dataflow.createTitle')" width="480px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item :label="t('dataflow.name')" required>
          <el-input v-model="createForm.name" maxlength="20" :placeholder="t('dataflow.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('dataflow.icon')">
          <el-input v-model="createForm.icon" :placeholder="t('dataflow.iconPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('dataflow.description')">
          <el-input v-model="createForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="publishVisible" :title="t('dataflow.publishTitle')" width="480px">
      <el-form :model="publishForm" label-width="80px">
        <el-form-item :label="t('dataflow.publishDesc')" required>
          <el-input v-model="publishForm.publishDescription" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handlePublish">{{ t('dataflow.publish') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="runVisible" :title="t('dataflow.runTitle', { name: runForm.name })" width="480px">
      <el-form :model="runForm" label-width="100px">
        <el-form-item :label="t('dataflow.failureStrategy')" required>
          <el-radio-group v-model="runForm.failureStrategy">
            <el-radio value="CONTINUE">{{ t('dataflow.continue') }}</el-radio>
            <el-radio value="END">{{ t('dataflow.end') }}</el-radio>
          </el-radio-group>
          <div class="strategy-hint">
            {{ t('dataflow.failureStrategyHint') }}
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="runVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmRun">{{ t('dataflow.run') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="runConfigVisible" :title="t('dataflow.runConfigTitle', { name: runConfigForm.name })" width="520px">
      <el-form v-loading="runConfigLoading" :model="runConfigForm" label-width="100px">
        <el-form-item :label="t('dataflow.runStrategy')" required>
          <el-select v-model="runConfigForm.runStrategy" style="width: 100%">
            <el-option :label="t('dataflow.allInstances')" value="ALL_INSTANCES" />
            <el-option :label="t('dataflow.specifyInstances')" value="SPECIFY_INSTANCES" />
            <el-option :label="t('dataflow.fixedInstanceNumber')" value="FIXED_INSTANCE_NUMBER" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="runConfigForm.runStrategy === 'FIXED_INSTANCE_NUMBER'" :label="t('dataflow.instanceCount')" required>
          <el-input-number v-model="runConfigForm.instanceNumber" :min="1" :max="100" style="width: 100%" />
        </el-form-item>

        <el-form-item v-if="runConfigForm.runStrategy === 'SPECIFY_INSTANCES'" :label="t('dataflow.specifyInstancesLabel')" required>
          <el-select
            v-model="runConfigForm.specifyInstances"
            multiple
            filterable
            allow-create
            default-first-option
            :reserve-keyword="false"
            :placeholder="t('dataflow.instancePlaceholder')"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item :label="t('dataflow.monitor')">
          <el-radio-group v-model="runConfigForm.enableMonitor">
            <el-radio value="ENABLE">{{ t('dataflow.on') }}</el-radio>
            <el-radio value="DISABLE">{{ t('dataflow.off') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item :label="t('dataflow.alarm')">
          <el-radio-group v-model="runConfigForm.enableAlarm">
            <el-radio value="ENABLE">{{ t('dataflow.on') }}</el-radio>
            <el-radio value="DISABLE">{{ t('dataflow.off') }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="runConfigVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="runConfigSaving" @click="handleRunConfigSave">{{ t('dataflow.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import {
  listDataFlow,
  createDataFlow,
  deleteDataFlow,
  startDataFlow,
  stopDataFlow,
  publishDataFlow,
  getDataFlowDetail,
  updateDataFlow,
  runDataFlow,
  type DataFlowListItem
} from '../../api/dataflow'

const router = useRouter()
const { t } = useI18n()
const loading = ref(false)
const list = ref<DataFlowListItem[]>([])
const query = reactive({ keyword: '', status: '' })
const page = reactive({ current: 1, size: 10, total: 0 })

const createVisible = ref(false)
const publishVisible = ref(false)
const submitting = ref(false)
const createForm = reactive({ name: '', icon: 'default', description: '' })
const publishForm = reactive({ id: 0, publishDescription: '' })

const runVisible = ref(false)
const runForm = reactive({ id: 0, name: '', failureStrategy: 'CONTINUE' })

const runConfigVisible = ref(false)
const runConfigLoading = ref(false)
const runConfigSaving = ref(false)
const runConfigForm = reactive({
  id: 0,
  name: '',
  enableAlarm: 'DISABLE',
  enableMonitor: 'DISABLE',
  runStrategy: 'ALL_INSTANCES',
  instanceNumber: 1,
  specifyInstances: [] as string[]
})

function statusMeta(status: string) {
  const map: Record<string, { label: string; tag: 'success' | 'warning' | 'info' | 'primary' }> = {
    TBP: { label: t('dataflow.statusTBP'), tag: 'info' },
    ENABLE: { label: t('dataflow.statusEnable'), tag: 'success' },
    PAUSE: { label: t('dataflow.statusPause'), tag: 'warning' },
    HISTORY: { label: t('dataflow.statusHistory'), tag: 'info' }
  }
  return map[status] ?? { label: status, tag: 'info' as const }
}

async function load() {
  loading.value = true
  try {
    const res = await listDataFlow(
      { keyword: query.keyword || undefined, status: query.status || undefined },
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

function openCreate() {
  Object.assign(createForm, { name: '', icon: 'default', description: '' })
  createVisible.value = true
}

async function handleCreate() {
  if (!createForm.name) {
    ElMessage.warning(t('dataflow.msgNameRequired'))
    return
  }
  submitting.value = true
  try {
    // 后端创建时固定置为「待发布」，status 仅作非空校验
    await createDataFlow({ ...createForm, status: 'ENABLE' })
    ElMessage.success(t('dataflow.msgCreateSuccess'))
    createVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

function goEdit(row: DataFlowListItem) {
  router.push(`/dataflow/edit/${row.id}`)
}

function goHistory(row: DataFlowListItem) {
  router.push({ path: '/dataflow/history', query: { code: row.code, name: row.name } })
}

function handleRun(row: DataFlowListItem) {
  Object.assign(runForm, { id: row.id, name: row.name, failureStrategy: 'CONTINUE' })
  runVisible.value = true
}

async function confirmRun() {
  submitting.value = true
  try {
    await runDataFlow(runForm.id, runForm.failureStrategy)
    ElMessage.success(t('dataflow.msgRunTriggered'))
    runVisible.value = false
    router.push({ path: '/dataflow/instance', query: { flowId: runForm.id, flowName: runForm.name } })
  } finally {
    submitting.value = false
  }
}

function goInstance(row: DataFlowListItem) {
  router.push({ path: '/dataflow/instance', query: { flowId: row.id, flowName: row.name } })
}

function openPublish(row: DataFlowListItem) {
  Object.assign(publishForm, { id: row.id, publishDescription: '' })
  publishVisible.value = true
}

async function handlePublish() {
  if (!publishForm.publishDescription) {
    ElMessage.warning(t('dataflow.msgPublishDescRequired'))
    return
  }
  submitting.value = true
  try {
    await publishDataFlow(publishForm.id, publishForm.publishDescription)
    ElMessage.success(t('dataflow.msgPublishSuccess'))
    publishVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleStart(row: DataFlowListItem) {
  await startDataFlow(row.id)
  ElMessage.success(t('dataflow.msgStartSuccess'))
  load()
}

async function openRunConfig(row: DataFlowListItem) {
  runConfigVisible.value = true
  runConfigLoading.value = true
  Object.assign(runConfigForm, {
    id: row.id,
    name: row.name,
    enableAlarm: 'DISABLE',
    enableMonitor: 'DISABLE',
    runStrategy: 'ALL_INSTANCES',
    instanceNumber: 1,
    specifyInstances: []
  })
  try {
    const d = await getDataFlowDetail(row.id)
    const specifyInstances = Array.isArray(d.specifyInstances) ? (d.specifyInstances as string[]) : []
    Object.assign(runConfigForm, {
      name: d.name,
      enableAlarm: d.enableAlarm || 'DISABLE',
      enableMonitor: d.enableMonitor || 'DISABLE',
      runStrategy: d.runStrategy || 'ALL_INSTANCES',
      instanceNumber: d.instanceNumber ?? 1,
      specifyInstances
    })
  } finally {
    runConfigLoading.value = false
  }
}

async function handleRunConfigSave() {
  const { runStrategy, specifyInstances } = runConfigForm
  if (runStrategy === 'FIXED_INSTANCE_NUMBER' && !runConfigForm.instanceNumber) {
    ElMessage.warning(t('dataflow.msgInstanceCountRequired'))
    return
  }
  if (runStrategy === 'SPECIFY_INSTANCES' && !specifyInstances.length) {
    ElMessage.warning(t('dataflow.msgSpecifyInstancesRequired'))
    return
  }
  runConfigSaving.value = true
  try {
    await updateDataFlow({
      id: runConfigForm.id,
      enableAlarm: runConfigForm.enableAlarm,
      enableMonitor: runConfigForm.enableMonitor,
      runStrategy,
      instanceNumber: runStrategy === 'FIXED_INSTANCE_NUMBER' ? runConfigForm.instanceNumber : undefined,
      specifyInstances: runStrategy === 'SPECIFY_INSTANCES' ? specifyInstances : undefined
    })
    ElMessage.success(t('dataflow.msgRunConfigSaved'))
    runConfigVisible.value = false
    load()
  } finally {
    runConfigSaving.value = false
  }
}

async function handleStop(row: DataFlowListItem) {
  await stopDataFlow(row.id)
  ElMessage.success(t('dataflow.msgStopSuccess'))
  load()
}

async function handleDelete(row: DataFlowListItem) {
  await ElMessageBox.confirm(t('dataflow.msgDeleteConfirm', { name: row.name }), t('dataflow.prompt'), { type: 'warning' })
  await deleteDataFlow(row.id)
  ElMessage.success(t('dataflow.msgDeleteSuccess'))
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.strategy-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
