<template>
  <div class="api-page">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <span class="card-title">查询模板</span>
            <span class="card-desc">将一条 SQL 保存为模板并发布成对外可调用的 REST API</span>
          </div>
          <el-button type="primary" :icon="Plus" @click="openAdd">新建 API</el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item>
            <el-input
              v-model="query.keyword"
              placeholder="搜索名称 / 编码"
              clearable
              :prefix-icon="Search"
              style="width: 220px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px">
              <el-option label="启用" value="ENABLE" />
              <el-option label="禁用" value="DISABLE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select
              v-model="query.dataSourceCode"
              placeholder="数据源"
              clearable
              filterable
              style="width: 180px"
            >
              <el-option v-for="ds in jdbcDatasources" :key="ds.code" :label="ds.name" :value="ds.code" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <template #empty>
          <el-empty description="暂无查询模板，点击右上角「新建 API」开始" :image-size="80" />
        </template>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="name-cell">
              <div class="name-text">{{ row.name }}</div>
              <div v-if="row.description" class="name-desc">{{ row.description }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="编码" min-width="180">
          <template #default="{ row }">
            <div class="code-cell">
              <span class="code-text" :title="row.code">{{ row.code }}</span>
              <el-icon class="code-copy" @click="copyText(row.code, '编码已复制')"><CopyDocument /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="dataSourceName" label="数据源" min-width="130" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'" effect="light" round size="small">
              {{ row.status === 'ENABLE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.publishVersion" type="primary" effect="light" round size="small">
              {{ row.publishVersion }}
            </el-tag>
            <el-tag v-else type="info" effect="light" round size="small">未发布</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" :icon="Promotion" @click="openPublish(row)">发布</el-button>
            <el-button link type="warning" :icon="VideoPlay" @click="openTest(row)">测试</el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, row)">
              <el-button link type="info">
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="log"><el-icon><Document /></el-icon>调用日志</el-dropdown-item>
                  <el-dropdown-item command="delete" divided><el-icon><Delete /></el-icon>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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

    <!-- 新建/编辑 -->
    <el-dialog v-model="formVisible" width="800px" top="3vh" class="form-dialog" destroy-on-close>
      <template #header>
        <div class="dialog-header">
          <el-icon class="dialog-header-icon"><EditPen /></el-icon>
          <div>
            <div class="dialog-header-title">{{ form.id ? '编辑 API' : '新建 API' }}</div>
            <div class="dialog-header-sub">{{ form.id ? '修改查询模板配置' : '保存一条 SQL 查询模板，发布后即可对外调用' }}</div>
          </div>
        </div>
      </template>

      <el-form :model="form" label-width="90px">
        <div class="section-title">基础信息</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" required>
              <el-input v-model="form.name" placeholder="API 名称" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据源" required>
              <el-select v-model="form.dataSourceCode" placeholder="选择数据源" filterable style="width: 100%">
                <el-option v-for="ds in jdbcDatasources" :key="ds.code" :label="`${ds.name} (${ds.type})`" :value="ds.code" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" placeholder="描述这个接口的用途（可选）" />
        </el-form-item>

        <div class="section-title">SQL 模板<span class="req">*</span></div>
        <div class="editor-panel">
          <div class="editor-toolbar">
            <span class="editor-lang">SQL</span>
            <div class="editor-toolbar-actions">
              <span class="editor-toolbar-hint">支持 ${'${param}'} 占位符</span>
              <button type="button" class="editor-format-btn" @click="sqlEditorRef?.format()">
                <el-icon><MagicStick /></el-icon>
                <span>格式化</span>
              </button>
            </div>
          </div>
          <div class="editor-wrap">
            <SqlEditor ref="sqlEditorRef" v-model="form.template" :dialect="editorDialect" />
          </div>
        </div>
        <div v-if="templateParams.length" class="param-chips">
          <span class="param-chips-label">检测到参数</span>
          <el-tag v-for="p in templateParams" :key="p" size="small" effect="light" round>{{ p }}</el-tag>
        </div>

        <div class="section-title">运行配置</div>
        <el-form-item label="超时(秒)">
          <el-input-number v-model="form.timeout" :min="1" :max="3600" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            active-value="ENABLE"
            inactive-value="DISABLE"
            active-text="启用"
            inactive-text="禁用"
            inline-prompt
            :width="56"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 发布 -->
    <el-dialog v-model="publishVisible" title="发布 API" width="560px" top="5vh" class="publish-dialog" destroy-on-close>
      <div class="publish-target">
        <el-icon class="publish-target-icon"><Promotion /></el-icon>
        <div class="publish-target-info">
          <div class="publish-target-name">{{ publishTarget?.name }}</div>
          <div class="publish-target-code">{{ publishTarget?.code }}</div>
        </div>
      </div>

      <div class="section-title">访问安全</div>
      <div class="field-label">密钥 secret</div>
      <el-input v-model="publishForm.secret" placeholder="留空 = 公开调用，无需鉴权" clearable show-password />

      <div class="section-title">运行配置</div>
      <div class="switch-row">
        <div class="switch-row-text">
          <div class="switch-row-title">结果缓存</div>
          <div class="switch-row-desc">开启后 60s 内相同参数直接返回缓存</div>
        </div>
        <el-switch v-model="publishForm.cacheOn" />
      </div>
      <div class="switch-row">
        <div class="switch-row-text">
          <div class="switch-row-title">限流</div>
          <div class="switch-row-desc">控制该接口的调用频率，防止被打爆</div>
        </div>
        <el-switch v-model="publishForm.limitOn" />
      </div>
      <div v-if="publishForm.limitOn" class="limit-config">
        <span>每</span>
        <el-input-number v-model="publishForm.limitRefreshInterval" :min="1" :max="86400" controls-position="right" />
        <el-select v-model="publishForm.limitTimeUnit">
          <el-option label="秒" value="SECONDS" />
          <el-option label="分钟" value="MINUTES" />
          <el-option label="小时" value="HOURS" />
        </el-select>
        <span>允许</span>
        <el-input-number v-model="publishForm.limitRate" :min="1" :max="100000" controls-position="right" />
        <span>次</span>
      </div>
      <div class="switch-row">
        <div class="switch-row-text">
          <div class="switch-row-title">记录日志</div>
          <div class="switch-row-desc">每次调用写入请求 / 响应 / 耗时等明细</div>
        </div>
        <el-switch v-model="publishForm.logOn" />
      </div>

      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="handlePublish">发布</el-button>
      </template>
    </el-dialog>

    <!-- 测试/预览 -->
    <el-dialog
      v-model="testVisible"
      :title="`测试 - ${testName}`"
      width="860px"
      top="3vh"
      class="test-dialog"
      destroy-on-close
    >
      <div class="test-field-label">动态参数 (JSON)</div>
      <el-input v-model="testParams" type="textarea" :rows="2" class="test-params-input" placeholder='{"id": 1}' />

      <div class="test-actions">
        <el-button type="primary" :icon="CaretRight" :loading="testing" @click="handleTest">运行</el-button>
        <div v-if="testResult" class="test-meta">
          <el-tag v-if="testResult.truncated" type="warning" effect="light" size="small">仅显示前 200 行</el-tag>
          <span v-if="testResult.rowCount != null">{{ testResult.rowCount }} 行</span>
          <span v-if="testResult.durationMs != null">{{ testResult.durationMs }} ms</span>
        </div>
      </div>

      <el-table
        v-if="testResult?.columns?.length"
        :data="testResult.rows"
        border
        size="small"
        max-height="320"
        class="result-table"
      >
        <el-table-column
          v-for="col in testResult.columns"
          :key="col"
          :prop="col"
          :label="col"
          min-width="120"
          show-overflow-tooltip
        />
      </el-table>
      <el-empty v-else-if="testResult && testResult.rowCount === 0" description="无返回结果" :image-size="60" />

      <div class="curl-block">
        <div class="curl-head">
          <span>调用示例（外部系统）</span>
          <div class="curl-head-right">
            <el-select v-model="testMethod" size="small" style="width: 90px">
              <el-option label="one" value="one" />
              <el-option label="count" value="count" />
              <el-option label="list" value="list" />
              <el-option label="page" value="page" />
            </el-select>
            <el-button link type="primary" size="small" @click="copyCurl">复制</el-button>
          </div>
        </div>
        <pre class="curl-code">{{ curlText }}</pre>
      </div>
    </el-dialog>

    <!-- 日志列表 -->
    <el-dialog v-model="logVisible" :title="`调用日志 - ${logName}`" width="960px" top="5vh" destroy-on-close>
      <el-table v-loading="logLoading" :data="logList" border size="small">
        <template #empty>
          <el-empty description="暂无调用记录" :image-size="70" />
        </template>
        <el-table-column prop="id" label="ID" width="64" align="center" />
        <el-table-column label="方法" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="methodType(row.method)" effect="light" size="small">{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="84" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" effect="light" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="number" label="数量" width="72" align="center" />
        <el-table-column prop="cost" label="耗时" width="90" align="center">
          <template #default="{ row }">{{ row.cost }} ms</template>
        </el-table-column>
        <el-table-column label="缓存" width="72" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.hitCache === 'YES'" type="warning" effect="light" size="small">命中</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="70" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openLogDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="logPage.current"
        v-model:page-size="logPage.size"
        class="pager"
        layout="total, prev, pager, next"
        :total="logPage.total"
        @change="loadLogs"
      />
    </el-dialog>

    <!-- 日志详情 -->
    <el-dialog v-model="logDetailVisible" :title="`日志详情 #${logDetail?.id ?? ''}`" width="760px" top="5vh" destroy-on-close>
      <template v-if="logDetail">
        <el-descriptions :column="2" border size="small" class="log-desc">
          <el-descriptions-item label="状态">
            <el-tag :type="logDetail.status === 'SUCCESS' ? 'success' : 'danger'" effect="light" size="small">
              {{ logDetail.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="方法">{{ logDetail.method }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ logDetail.cost }} ms</el-descriptions-item>
          <el-descriptions-item label="数量">{{ logDetail.number }}</el-descriptions-item>
          <el-descriptions-item label="缓存">
            {{ logDetail.hitCache === 'YES' ? '命中' : '未命中' }}
          </el-descriptions-item>
          <el-descriptions-item label="IP">{{ logDetail.ip }}</el-descriptions-item>
        </el-descriptions>
        <div class="log-block-title">请求参数</div>
        <pre class="log-block">{{ prettyJson(logDetail.requestArg) }}</pre>
        <div class="log-block-title">响应参数</div>
        <pre class="log-block">{{ prettyJson(logDetail.responseArg) }}</pre>
        <el-alert v-if="logDetail.exception" :title="logDetail.exception" type="error" :closable="false" show-icon />
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Plus,
  CaretRight,
  ArrowDown,
  Refresh,
  Edit,
  EditPen,
  Promotion,
  VideoPlay,
  CopyDocument,
  Document,
  Delete,
  MagicStick
} from '@element-plus/icons-vue'
import SqlEditor from '../../components/SqlEditor.vue'
import { listDataSource, type DataSourceItem } from '../../api/datasource'
import {
  listTemplate,
  getTemplateDetail,
  addTemplate,
  updateTemplate,
  deleteTemplate,
  publishTemplate,
  testTemplate,
  listLog,
  getLogDetail,
  type QueryTemplateItem,
  type QueryTemplateForm,
  type QueryLogItem,
  type QueryLogDetail,
  type QueryExecuteResult
} from '../../api/service'

const JDBC_TYPES = ['Doris', 'MySQL', 'PostgreSQL']

const loading = ref(false)
const list = ref<QueryTemplateItem[]>([])
const query = reactive({ keyword: '', status: '', dataSourceCode: '' })
const page = reactive({ current: 1, size: 10, total: 0 })

const allDatasources = ref<DataSourceItem[]>([])
const jdbcDatasources = computed(() =>
  allDatasources.value
    .filter((d) => JDBC_TYPES.includes(d.type))
    .sort((a, b) => (a.type === 'Doris' ? -1 : b.type === 'Doris' ? 1 : 0))
)

const formVisible = ref(false)
const saving = ref(false)
const form = reactive<QueryTemplateForm>({ name: '', dataSourceCode: '', template: '', timeout: 30, status: 'ENABLE', description: '' })

const templateParams = computed(() => {
  const matches = form.template.match(/\$\{([a-zA-Z0-9_]+)\}/g) ?? []
  return [...new Set(matches.map((m) => m.slice(2, -1)))]
})

const sqlEditorRef = ref<{ format: () => void } | null>(null)

const editorDialect = computed<'mysql' | 'postgresql'>(() => {
  const ds = allDatasources.value.find((d) => d.code === form.dataSourceCode)
  return (ds?.type ?? '').toLowerCase() === 'postgresql' ? 'postgresql' : 'mysql'
})

const publishVisible = ref(false)
const publishing = ref(false)
const publishTarget = ref<QueryTemplateItem | null>(null)
const publishForm = reactive({
  secret: '',
  cacheOn: false,
  limitOn: false,
  limitRate: 10,
  limitRefreshInterval: 1,
  limitTimeUnit: 'SECONDS',
  logOn: true
})

const testVisible = ref(false)
const testing = ref(false)
const testId = ref(0)
const testName = ref('')
const testCode = ref('')
const testSecret = ref('')
const testParams = ref('{}')
const testMethod = ref('list')
const testResult = ref<QueryExecuteResult | null>(null)

const logVisible = ref(false)
const logLoading = ref(false)
const logName = ref('')
const logTemplateCode = ref('')
const logList = ref<QueryLogItem[]>([])
const logPage = reactive({ current: 1, size: 10, total: 0 })

const logDetailVisible = ref(false)
const logDetail = ref<QueryLogDetail | null>(null)

const curlText = computed(() => {
  const origin = window.location.origin
  const secretHeader = testSecret.value ? `  -H 'X-Secret: ${testSecret.value}' \\\n` : ''
  return `curl -X POST '${origin}/dp-web/open/api/${testCode.value}' \\\n` +
    `  -H 'Content-Type: application/json' \\\n` +
    secretHeader +
    `  -d '${JSON.stringify({ method: testMethod.value, params: parseParams() })}'`
})

function parseParams(): Record<string, unknown> {
  try {
    const v = JSON.parse(testParams.value || '{}')
    return v && typeof v === 'object' ? v : {}
  } catch {
    return {}
  }
}

function methodType(method: string): 'primary' | 'success' | 'info' | 'warning' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'info' | 'warning' | 'danger'> = {
    one: 'primary',
    count: 'success',
    list: 'info',
    page: 'warning'
  }
  return map[method] ?? 'info'
}

function prettyJson(raw?: string): string {
  if (!raw) return '（无）'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

async function copyText(text: string, tip = '已复制') {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(tip)
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

async function load() {
  loading.value = true
  try {
    const res = await listTemplate(
      {
        keyword: query.keyword || undefined,
        status: query.status || undefined,
        dataSourceCode: query.dataSourceCode || undefined
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

function handleReset() {
  query.keyword = ''
  query.status = ''
  query.dataSourceCode = ''
  page.current = 1
  load()
}

async function loadDatasources() {
  const res = await listDataSource({}, 1, 500)
  allDatasources.value = res.records
}

function openAdd() {
  form.id = undefined
  form.name = ''
  form.dataSourceCode = jdbcDatasources.value[0]?.code ?? ''
  form.template = ''
  form.timeout = 30
  form.status = 'ENABLE'
  form.description = ''
  formVisible.value = true
}

async function openEdit(row: QueryTemplateItem) {
  const d = await getTemplateDetail(row.id)
  form.id = row.id
  form.name = row.name
  form.dataSourceCode = row.dataSourceCode
  form.template = d?.template ?? ''
  form.timeout = row.timeout
  form.status = row.status
  form.description = row.description
  formVisible.value = true
}

async function handleSave() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写名称')
    return
  }
  if (!form.dataSourceCode) {
    ElMessage.warning('请选择数据源')
    return
  }
  if (!form.template.trim()) {
    ElMessage.warning('SQL 模板不能为空')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name,
      dataSourceCode: form.dataSourceCode,
      template: form.template,
      timeout: form.timeout,
      status: form.status,
      description: form.description || undefined
    }
    if (form.id) {
      await updateTemplate({ ...payload, id: form.id })
      ElMessage.success('更新成功')
    } else {
      await addTemplate(payload)
      ElMessage.success('保存成功')
    }
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openPublish(row: QueryTemplateItem) {
  publishTarget.value = row
  publishForm.secret = ''
  publishForm.cacheOn = false
  publishForm.limitOn = false
  publishForm.limitRate = 10
  publishForm.limitRefreshInterval = 1
  publishForm.limitTimeUnit = 'SECONDS'
  publishForm.logOn = true
  publishVisible.value = true
}

async function handlePublish() {
  if (!publishTarget.value) return
  publishing.value = true
  try {
    const res = await publishTemplate({
      id: publishTarget.value.id,
      secret: publishForm.secret || undefined,
      enableCache: publishForm.cacheOn ? 'ENABLE' : 'DISABLE',
      enableLimiting: publishForm.limitOn ? 'ENABLE' : 'DISABLE',
      limitRate: publishForm.limitOn ? publishForm.limitRate : undefined,
      limitRefreshInterval: publishForm.limitOn ? publishForm.limitRefreshInterval : undefined,
      limitTimeUnit: publishForm.limitOn ? publishForm.limitTimeUnit : undefined,
      recordLog: publishForm.logOn ? 'ENABLE' : 'DISABLE'
    })
    ElMessage.success(`发布成功（${res.version}）`)
    publishVisible.value = false
    load()
  } finally {
    publishing.value = false
  }
}

async function openTest(row: QueryTemplateItem) {
  let secret = ''
  try {
    const d = await getTemplateDetail(row.id)
    secret = d?.secret ?? ''
  } catch {
    /* ignore */
  }
  testId.value = row.id
  testName.value = row.name
  testCode.value = row.code
  testSecret.value = secret
  testParams.value = '{}'
  testMethod.value = 'list'
  testResult.value = null
  testVisible.value = true
}

async function handleTest() {
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await testTemplate({ id: testId.value, params: parseParams() })
  } finally {
    testing.value = false
  }
}

async function copyCurl() {
  await copyText(curlText.value, 'curl 已复制')
}

async function handleDelete(row: QueryTemplateItem) {
  await ElMessageBox.confirm(`确认删除 API「${row.name}」？删除后其发布接口将不可用。`, '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteTemplate(row.id)
  ElMessage.success('删除成功')
  load()
}

function handleCommand(cmd: string, row: QueryTemplateItem) {
  if (cmd === 'log') openLog(row)
  else if (cmd === 'delete') handleDelete(row)
}

function openLog(row: QueryTemplateItem) {
  logName.value = row.name
  logTemplateCode.value = row.code
  logPage.current = 1
  logVisible.value = true
  loadLogs()
}

async function loadLogs() {
  logLoading.value = true
  try {
    const res = await listLog({ templateCode: logTemplateCode.value || undefined }, logPage.current, logPage.size)
    logList.value = res.records
    logPage.total = res.total
  } finally {
    logLoading.value = false
  }
}

async function openLogDetail(row: QueryLogItem) {
  logDetailVisible.value = true
  logDetail.value = null
  logDetail.value = await getLogDetail(row.id)
}

onMounted(async () => {
  await loadDatasources()
  load()
})
</script>

<style scoped>
.api-page {
  --radius: 8px;
}

.page-card {
  border: none;
  border-radius: var(--radius);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-desc {
  font-size: 12px;
  color: #909399;
}

.filter-bar {
  margin-bottom: 16px;
}

.filter-bar :deep(.el-form-item) {
  margin-right: 12px;
  margin-bottom: 0;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

/* 表格单元格 */
.name-cell {
  line-height: 1.4;
}

.name-text {
  color: #303133;
  font-weight: 500;
}

.name-desc {
  color: #909399;
  font-size: 12px;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.code-text {
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-copy {
  flex-shrink: 0;
  cursor: pointer;
  color: #c0c4cc;
  font-size: 14px;
  transition: color 0.2s;
}

.code-copy:hover {
  color: var(--el-color-primary);
}

.muted {
  color: #c0c4cc;
}

/* 对话框通用 */
.form-dialog :deep(.el-dialog__body),
.test-dialog :deep(.el-dialog__body) {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.dialog-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dialog-header-icon {
  font-size: 22px;
  color: var(--el-color-primary);
}

.dialog-header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.dialog-header-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.form-dialog .section-title:first-child {
  margin-top: 0;
}

.req {
  color: #f56c6c;
  margin-left: 4px;
}

/* 编辑器 */
.editor-panel {
  border: 1px solid #3c3c3c;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #252526;
  border-bottom: 1px solid #3c3c3c;
}

.editor-lang {
  font-size: 12px;
  font-weight: 600;
  color: #c9c3fa;
  letter-spacing: 0.6px;
}

.editor-toolbar-hint {
  font-size: 12px;
  color: #9a9a9a;
}

.editor-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.editor-format-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 6px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #c9c3fa;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}

.editor-format-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.editor-wrap {
  width: 100%;
  height: 260px;
}

.param-chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.param-chips-label {
  font-size: 12px;
  color: #909399;
}

/* 发布对话框 */
.publish-target {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 6px;
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-8);
  border-radius: 6px;
}

.publish-target-icon {
  font-size: 22px;
  color: var(--el-color-primary);
}

.publish-target-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.publish-target-code {
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.section-title {
  position: relative;
  margin: 18px 0 12px;
  padding-left: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: var(--el-color-primary);
}

.field-label {
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
}

.switch-row + .switch-row {
  border-top: 1px solid #f0f1f5;
}

.switch-row-title {
  font-size: 14px;
  color: #303133;
}

.switch-row-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.limit-config {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 12px;
  margin-bottom: 4px;
  background: #f7f8fa;
  border-radius: 6px;
}

.limit-config span {
  color: #606266;
  font-size: 13px;
}

.limit-config :deep(.el-input-number) {
  width: 96px;
}

.limit-config :deep(.el-select) {
  width: 96px;
}

/* 测试对话框 */
.test-field-label {
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}

.test-params-input :deep(textarea) {
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
}

.test-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
}

.test-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #909399;
}

.result-table {
  width: 100%;
}

.curl-block {
  margin-top: 16px;
  border: 1px solid #eef0f4;
  border-radius: 6px;
  overflow: hidden;
}

.curl-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f5f6fa;
  border-bottom: 1px solid #eef0f4;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.curl-head-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.curl-code {
  margin: 0;
  padding: 12px 14px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  background: #1e1e1e;
  color: #d4d4d4;
  overflow: auto;
}

/* 日志详情 */
.log-desc {
  margin-bottom: 6px;
}

.log-block-title {
  margin: 14px 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.log-block {
  background: #f5f6fa;
  border: 1px solid #eef0f4;
  border-radius: 6px;
  padding: 12px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0 0 6px;
  max-height: 220px;
  overflow: auto;
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
}
</style>
