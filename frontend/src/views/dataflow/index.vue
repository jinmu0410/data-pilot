<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item label="关键字">
            <el-input
              v-model="query.keyword"
              placeholder="名称/编码"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
              <el-option label="待发布" value="TBP" />
              <el-option label="启用" value="ENABLE" />
              <el-option label="暂停" value="PAUSE" />
              <el-option label="历史" value="HISTORY" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button type="success" :icon="Plus" @click="openCreate">新建任务流</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="code" label="编码" min-width="150" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).tag">
              {{ statusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentVersion" label="当前版本" width="110" />
        <el-table-column prop="publishVersion" label="发布版本" width="110" />
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="480" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleRun(row)">运行</el-button>
            <el-button link @click="goInstance(row)">实例</el-button>
            <el-button link type="info" @click="openRunConfig(row)">运行配置</el-button>
            <el-button link type="success" @click="openPublish(row)">发布</el-button>
            <el-button link @click="goHistory(row)">发布记录</el-button>
            <el-button
              v-if="row.status === 'ENABLE'"
              link
              type="warning"
              @click="handleStop(row)"
            >停止</el-button>
            <el-button
              v-if="row.status === 'PAUSE'"
              link
              type="success"
              @click="handleStart(row)"
            >启动</el-button>
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

    <el-dialog v-model="createVisible" title="新建任务流" width="480px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="createForm.name" maxlength="20" placeholder="任务流名称" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="createForm.icon" placeholder="图标标识，默认 default" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="publishVisible" title="发布任务流" width="480px">
      <el-form :model="publishForm" label-width="80px">
        <el-form-item label="发布说明" required>
          <el-input v-model="publishForm.publishDescription" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handlePublish">发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="runVisible" :title="`运行任务流 - ${runForm.name}`" width="480px">
      <el-form :model="runForm" label-width="100px">
        <el-form-item label="失败策略" required>
          <el-radio-group v-model="runForm.failureStrategy">
            <el-radio value="CONTINUE">继续</el-radio>
            <el-radio value="END">结束</el-radio>
          </el-radio-group>
          <div class="strategy-hint">
            继续：某节点失败后，等待同级节点跑完再结束；结束：某节点失败即终止其余节点并结束。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="runVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmRun">运行</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="runConfigVisible" :title="`运行配置 - ${runConfigForm.name}`" width="520px">
      <el-form v-loading="runConfigLoading" :model="runConfigForm" label-width="100px">
        <el-form-item label="运行策略" required>
          <el-select v-model="runConfigForm.runStrategy" style="width: 100%">
            <el-option label="全部实例" value="ALL_INSTANCES" />
            <el-option label="指定实例" value="SPECIFY_INSTANCES" />
            <el-option label="固定实例数" value="FIXED_INSTANCE_NUMBER" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="runConfigForm.runStrategy === 'FIXED_INSTANCE_NUMBER'" label="实例数量" required>
          <el-input-number v-model="runConfigForm.instanceNumber" :min="1" :max="100" style="width: 100%" />
        </el-form-item>

        <el-form-item v-if="runConfigForm.runStrategy === 'SPECIFY_INSTANCES'" label="指定实例" required>
          <el-select
            v-model="runConfigForm.specifyInstances"
            multiple
            filterable
            allow-create
            default-first-option
            :reserve-keyword="false"
            placeholder="输入实例标识后回车添加"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="监控">
          <el-radio-group v-model="runConfigForm.enableMonitor">
            <el-radio value="ENABLE">开启</el-radio>
            <el-radio value="DISABLE">关闭</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="告警">
          <el-radio-group v-model="runConfigForm.enableAlarm">
            <el-radio value="ENABLE">开启</el-radio>
            <el-radio value="DISABLE">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="runConfigVisible = false">取消</el-button>
        <el-button type="primary" :loading="runConfigSaving" @click="handleRunConfigSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
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

const STATUS_META: Record<string, { label: string; tag: 'success' | 'warning' | 'info' | 'primary' }> = {
  TBP: { label: '待发布', tag: 'info' },
  ENABLE: { label: '启用', tag: 'success' },
  PAUSE: { label: '暂停', tag: 'warning' },
  HISTORY: { label: '历史', tag: 'info' }
}

function statusMeta(status: string) {
  return STATUS_META[status] ?? { label: status, tag: 'info' as const }
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
    ElMessage.warning('请填写名称')
    return
  }
  submitting.value = true
  try {
    // 后端创建时固定置为「待发布」，status 仅作非空校验
    await createDataFlow({ ...createForm, status: 'ENABLE' })
    ElMessage.success('创建成功')
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
    ElMessage.success('已触发运行')
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
    ElMessage.warning('请填写发布说明')
    return
  }
  submitting.value = true
  try {
    await publishDataFlow(publishForm.id, publishForm.publishDescription)
    ElMessage.success('发布成功')
    publishVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleStart(row: DataFlowListItem) {
  await startDataFlow(row.id)
  ElMessage.success('启动成功')
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
    ElMessage.warning('请填写实例数量')
    return
  }
  if (runStrategy === 'SPECIFY_INSTANCES' && !specifyInstances.length) {
    ElMessage.warning('请填写指定实例')
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
    ElMessage.success('运行配置已保存')
    runConfigVisible.value = false
    load()
  } finally {
    runConfigSaving.value = false
  }
}

async function handleStop(row: DataFlowListItem) {
  await stopDataFlow(row.id)
  ElMessage.success('停止成功')
  load()
}

async function handleDelete(row: DataFlowListItem) {
  await ElMessageBox.confirm(`确认删除任务流「${row.name}」？`, '提示', { type: 'warning' })
  await deleteDataFlow(row.id)
  ElMessage.success('删除成功')
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
