<template>
  <div class="api-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="page-eyebrow">DATA SERVICE</span>
        <h2>API 服务</h2>
        <p>把查询 SQL 封装成可鉴权、可限流、可观测的数据接口。</p>
      </div>
      <div class="hero-stats">
        <div><strong>{{ page.total }}</strong><span>API 总数</span></div>
        <div><strong>{{ publishedCount }}</strong><span>当前页已发布</span></div>
        <div><strong>{{ enabledCount }}</strong><span>当前页启用</span></div>
      </div>
      <el-button type="primary" :icon="Plus" size="large" @click="openAdd">创建 API</el-button>
    </section>

    <el-card shadow="never" class="filter-card">
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
    </el-card>

    <el-card shadow="never" class="list-card">
      <div class="list-heading">
        <div><h3>服务列表</h3><span>统一管理草稿、发布版本与调用入口</span></div>
        <span class="support-tip">PREPARED SQL · SECRET · RATE LIMIT · LOG</span>
      </div>
      <el-table v-loading="loading" :data="list" class="api-table">
        <template #empty>
          <el-empty description="暂无查询模板，点击右上角「新建 API」开始" :image-size="80" />
        </template>
        <el-table-column label="API 服务" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="api-name-cell">
              <span class="api-logo">API</span>
              <div class="name-cell">
                <div class="name-text">{{ row.name }}</div>
                <div class="name-desc">{{ row.description || '暂无服务描述' }}</div>
              </div>
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
        <el-table-column label="数据源" min-width="145" show-overflow-tooltip>
          <template #default="{ row }"><span class="datasource-pill">{{ row.dataSourceName }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="{ enabled: row.status === 'ENABLE' }"><i></i>{{ row.status === 'ENABLE' ? '启用' : '禁用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.publishVersion" class="version-badge">{{ row.publishVersion }}</span>
            <span v-else class="draft-badge">草稿</span>
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
    <el-dialog v-model="formVisible" width="min(1160px, calc(100vw - 40px))" top="3vh" append-to-body class="api-config-dialog" destroy-on-close :close-on-click-modal="false">
      <template #header>
        <div class="dialog-heading">
          <span class="dialog-logo">API</span>
          <div><div class="dialog-title">{{ form.id ? '编辑 API 服务' : '创建 API 服务' }}</div><div class="dialog-subtitle">定义查询逻辑、请求参数与运行策略，保存后可发布为 REST API</div></div>
          <span class="dialog-mode">{{ form.id ? 'EDIT SERVICE' : 'NEW SERVICE' }}</span>
        </div>
      </template>
      <div class="config-shell">
        <aside class="config-sidebar">
          <span class="sidebar-label">SERVICE BUILDER</span>
          <div class="step-card done"><span>01</span><div><strong>基本信息</strong><small>名称、数据源与状态</small></div></div>
          <div class="step-card" :class="{ done: !!form.template.trim() }"><span>02</span><div><strong>查询逻辑</strong><small>只读 SQL 与动态占位符</small></div></div>
          <div class="step-card" :class="{ done: templateParams.length > 0 }"><span>03</span><div><strong>请求参数</strong><small>自动识别并配置测试值</small></div></div>
          <div class="service-summary"><span>配置概览</span><strong>{{ form.name || '未命名 API' }}</strong><p>{{ selectedDatasource?.name || '尚未选择数据源' }}</p><div><i :class="{ ready: canDraftTest }"></i>{{ canDraftTest ? '可执行草稿测试' : '等待完成必填配置' }}</div></div>
        </aside>
        <main class="config-content">
          <section class="form-section">
            <div class="section-heading"><div><span class="section-index">01</span><div><h3>基本信息</h3><p>用于识别服务并选择查询数据源</p></div></div></div>
            <div class="form-grid basic-grid">
              <div class="field"><label>API 名称 <em>*</em></label><el-input v-model="form.name" size="large" maxlength="200" show-word-limit placeholder="例如：订单明细查询" /></div>
              <div class="field"><label>查询数据源 <em>*</em></label><el-select v-model="form.dataSourceCode" size="large" placeholder="选择 JDBC 数据源" filterable style="width:100%"><el-option v-for="ds in jdbcDatasources" :key="ds.code" :label="`${ds.name} (${ds.type})`" :value="ds.code" /></el-select></div>
              <div class="field wide-field"><label>服务描述</label><el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" placeholder="说明接口用途、使用方或数据口径（可选）" /></div>
            </div>
          </section>
          <section class="form-section">
            <div class="section-heading"><div><span class="section-index">02</span><div><h3>SQL 查询模板</h3><p>仅支持 SELECT / WITH 查询，变量使用 &#36;{param} 占位</p></div></div><button type="button" class="format-action" @click="sqlEditorRef?.format()"><el-icon><MagicStick /></el-icon>格式化 SQL</button></div>
            <div class="editor-panel">
          <div class="editor-toolbar">
            <span class="editor-lang">{{ editorDialect === 'postgresql' ? 'POSTGRESQL' : 'SQL' }}</span>
            <div class="editor-toolbar-actions">
              <span class="editor-toolbar-hint">PreparedStatement 安全绑定参数</span>
            </div>
          </div>
          <div class="editor-wrap">
            <SqlEditor ref="sqlEditorRef" v-model="form.template" :dialect="editorDialect" />
          </div>
        </div>
            <div class="sql-hint"><span>i</span><p>占位符只能代表参数值，不能替代库名、表名或 SQL 关键字；服务端会预编译绑定以避免参数注入。</p></div>
          </section>
          <section class="form-section">
            <div class="section-heading"><div><span class="section-index">03</span><div><h3>请求参数</h3><p>根据 SQL 自动识别。填写测试值可在保存前验证查询结果</p></div></div><span class="param-counter">{{ templateParams.length }} PARAMETERS</span></div>
            <div v-if="parameterRows.length" class="parameter-table">
              <div class="parameter-head"><span>参数名</span><span>值类型</span><span>测试值</span><span>说明</span></div>
              <div v-for="item in parameterRows" :key="item.name" class="parameter-row">
                <div class="parameter-name"><code v-text="'${' + item.name + '}'"></code><small>必传</small></div>
                <el-select v-model="item.type"><el-option v-for="option in PARAM_TYPES" :key="option.value" :label="option.label" :value="option.value" /></el-select>
                <el-input v-model="item.value" :placeholder="parameterPlaceholder(item.type)" clearable />
                <el-input v-model="item.description" placeholder="业务含义（当前仅本次测试）" clearable />
              </div>
            </div>
            <div v-else class="parameter-empty"><span>{ }</span><div><strong>当前 SQL 没有动态参数</strong><p>输入类似 WHERE id = &#36;{id} 后会自动生成参数配置行</p></div></div>
          </section>
          <section class="form-section runtime-section">
            <div class="section-heading"><div><span class="section-index">04</span><div><h3>运行设置</h3><p>控制查询超时与草稿启用状态</p></div></div></div>
            <div class="runtime-grid"><div class="setting-card"><div><strong>查询超时</strong><small>避免慢查询长期占用连接</small></div><el-input-number v-model="form.timeout" :min="1" :max="3600" controls-position="right" /><span>秒</span></div><div class="setting-card"><div><strong>服务状态</strong><small>禁用后不能发布或调用</small></div><el-switch v-model="form.status" active-value="ENABLE" inactive-value="DISABLE" active-text="启用" inactive-text="禁用" /></div></div>
          </section>
        </main>
      </div>

      <template #footer>
        <div class="dialog-footer"><span class="footer-tip">参数定义由 SQL 实时生成，无需重复维护。</span><el-button @click="formVisible = false">取消</el-button><el-button :icon="VideoPlay" :loading="testingDraft" :disabled="!canDraftTest" @click="handleDraftTest">测试草稿</el-button><el-button type="primary" :loading="saving" @click="handleSave">{{ form.id ? '保存修改' : '创建 API' }}</el-button></div>
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
      width="min(980px, calc(100vw - 40px))"
      top="3vh"
      append-to-body
      class="api-test-dialog"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <template #header><div class="dialog-heading"><span class="dialog-logo test-logo">RUN</span><div><div class="dialog-title">接口测试 · {{ testName }}</div><div class="dialog-subtitle">填写动态参数，预览 SQL 结果并生成外部调用示例</div></div><span class="dialog-mode">{{ testCode }}</span></div></template>
      <div class="test-shell">
        <section class="test-params-panel">
          <div class="test-panel-head"><div><strong>请求参数</strong><span>{{ testParameterRows.length }} 个 SQL 占位符</span></div><el-switch v-model="advancedParams" active-text="JSON" /></div>
          <template v-if="!advancedParams">
            <div v-if="testParameterRows.length" class="test-param-list">
              <div v-for="item in testParameterRows" :key="item.name" class="test-param-item"><div class="test-param-label"><code>{{ item.name }}</code><span>必传</span></div><div class="test-param-control"><el-select v-model="item.type" style="width:105px"><el-option v-for="option in PARAM_TYPES" :key="option.value" :label="option.label" :value="option.value" /></el-select><el-input v-model="item.value" :placeholder="parameterPlaceholder(item.type)" clearable /></div></div>
            </div>
            <div v-else class="parameter-empty compact"><span>{ }</span><div><strong>此接口无需参数</strong><p>可以直接运行查询</p></div></div>
          </template>
          <div v-else><el-input v-model="testParamsJson" type="textarea" :rows="8" class="test-params-input" placeholder='{"id": 1}' /><div class="json-hint">适合粘贴复杂参数；必须是合法 JSON 对象。</div></div>
          <div class="test-actions"><el-button type="primary" :icon="CaretRight" :loading="testing" @click="handleTest">运行查询</el-button><el-button @click="resetTestParams">重置参数</el-button></div>
        </section>
        <section class="test-result-panel">
          <div class="test-panel-head"><div><strong>运行结果</strong><span v-if="testResult">{{ testResult.rowCount }} 行 · {{ testResult.durationMs }} ms</span><span v-else>尚未执行</span></div><el-tag v-if="testResult?.truncated" type="warning" effect="light" size="small">仅展示前 200 行</el-tag></div>
          <el-table v-if="testResult?.columns?.length" :data="testResult.rows" border size="small" max-height="300" class="result-table"><el-table-column v-for="col in testResult.columns" :key="col" :prop="col" :label="col" min-width="120" show-overflow-tooltip /></el-table>
          <el-empty v-else :description="testResult ? '查询成功，无返回结果' : '运行后将在这里展示查询结果'" :image-size="62" />
        </section>
      </div>
      <div class="curl-block">
        <div class="curl-head"><div><strong>外部调用示例</strong><span>选择响应模式后自动生成 cURL</span></div><div class="curl-head-right"><el-select v-model="testMethod" size="small" style="width:105px"><el-option label="单条 one" value="one" /><el-option label="数量 count" value="count" /><el-option label="列表 list" value="list" /><el-option label="分页 page" value="page" /></el-select><template v-if="testMethod === 'page'"><el-input-number v-model="testPageNum" size="small" :min="1" :max="100000" controls-position="right" style="width:95px" /><el-input-number v-model="testPageSize" size="small" :min="1" :max="1000" controls-position="right" style="width:95px" /></template><el-button link type="primary" size="small" @click="copyCurl">复制 cURL</el-button></div></div>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Plus,
  CaretRight,
  ArrowDown,
  Refresh,
  Edit,
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

const JDBC_TYPES = ['Doris', 'MySQL', 'TiDB', 'PostgreSQL']
type ParameterType = 'string' | 'number' | 'boolean' | 'null'
interface ParameterRow { name: string; type: ParameterType; value: string; description: string }
const PARAM_TYPES: Array<{ label: string; value: ParameterType }> = [
  { label: '文本', value: 'string' },
  { label: '数字', value: 'number' },
  { label: '布尔', value: 'boolean' },
  { label: '空值', value: 'null' }
]

const loading = ref(false)
const list = ref<QueryTemplateItem[]>([])
const query = reactive({ keyword: '', status: '', dataSourceCode: '' })
const page = reactive({ current: 1, size: 10, total: 0 })
const publishedCount = computed(() => list.value.filter((item) => !!item.publishVersion).length)
const enabledCount = computed(() => list.value.filter((item) => item.status === 'ENABLE').length)

const allDatasources = ref<DataSourceItem[]>([])
const jdbcDatasources = computed(() =>
  allDatasources.value
    .filter((d) => JDBC_TYPES.includes(d.type))
    .sort((a, b) => (a.type === 'Doris' ? -1 : b.type === 'Doris' ? 1 : 0))
)

const formVisible = ref(false)
const saving = ref(false)
const testingDraft = ref(false)
const form = reactive<QueryTemplateForm>({ name: '', dataSourceCode: '', template: '', timeout: 30, status: 'ENABLE', description: '' })

const templateParams = computed(() => {
  const matches = form.template.match(/\$\{([a-zA-Z0-9_]+)\}/g) ?? []
  return [...new Set(matches.map((m) => m.slice(2, -1)))]
})
const parameterRows = ref<ParameterRow[]>([])
const selectedDatasource = computed(() => jdbcDatasources.value.find((item) => item.code === form.dataSourceCode))
const canDraftTest = computed(() => !!form.dataSourceCode && !!form.template.trim() && isReadOnlySql(form.template))

watch(templateParams, (names) => {
  const previous = new Map(parameterRows.value.map((item) => [item.name, item]))
  parameterRows.value = names.map((name) => previous.get(name) ?? { name, type: 'string', value: '', description: '' })
}, { immediate: true })

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
const testParamsJson = ref('{}')
const testParameterRows = ref<ParameterRow[]>([])
const advancedParams = ref(false)
const testMethod = ref('list')
const testPageNum = ref(1)
const testPageSize = ref(20)
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
  const body: Record<string, unknown> = { method: testMethod.value, params: currentTestParams(false) ?? {} }
  if (testMethod.value === 'page') {
    body.pageNum = testPageNum.value
    body.pageSize = testPageSize.value
  }
  return `curl -X POST '${origin}/dp-web/open/api/${testCode.value}' \\\n` +
    `  -H 'Content-Type: application/json' \\\n` +
    secretHeader +
    `  -d '${JSON.stringify(body)}'`
})

function isReadOnlySql(sql: string) {
  return /^(select|with)\b/i.test(sql.trim()) && !/;\s*\S/.test(sql.trim())
}

function parameterPlaceholder(type: ParameterType) {
  return type === 'number' ? '例如：1001' : type === 'boolean' ? 'true / false' : type === 'null' ? '固定传入 null' : '输入文本值'
}

function convertParameter(item: ParameterRow): unknown {
  if (item.type === 'null') return null
  if (item.type === 'number') {
    if (!item.value.trim() || Number.isNaN(Number(item.value))) throw new Error(`参数 ${item.name} 需要填写有效数字`)
    return Number(item.value)
  }
  if (item.type === 'boolean') {
    if (!['true', 'false'].includes(item.value.trim().toLowerCase())) throw new Error(`参数 ${item.name} 只能填写 true 或 false`)
    return item.value.trim().toLowerCase() === 'true'
  }
  if (!item.value.trim()) throw new Error(`请填写参数 ${item.name}`)
  return item.value
}

function rowsToParams(rows: ParameterRow[], notify = true): Record<string, unknown> | null {
  try {
    return Object.fromEntries(rows.map((item) => [item.name, convertParameter(item)]))
  } catch (error) {
    if (notify) ElMessage.warning(error instanceof Error ? error.message : '参数格式错误')
    return null
  }
}

function currentTestParams(notify = true): Record<string, unknown> | null {
  if (!advancedParams.value) return rowsToParams(testParameterRows.value, notify)
  try {
    const value = JSON.parse(testParamsJson.value || '{}')
    if (!value || Array.isArray(value) || typeof value !== 'object') throw new Error('请求参数必须是 JSON 对象')
    return value
  } catch (error) {
    if (notify) ElMessage.warning(error instanceof Error ? error.message : 'JSON 格式错误')
    return null
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
  parameterRows.value = []
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
  parameterRows.value = []
  formVisible.value = true
}

async function handleDraftTest() {
  if (!canDraftTest.value) {
    ElMessage.warning('请先选择数据源并填写只读 SQL')
    return
  }
  const params = rowsToParams(parameterRows.value)
  if (params === null) return
  testingDraft.value = true
  try {
    const result = await testTemplate({ dataSourceCode: form.dataSourceCode, template: form.template, params })
    ElMessage.success(`草稿执行成功：${result.rowCount} 行，耗时 ${result.durationMs} ms`)
  } finally {
    testingDraft.value = false
  }
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
  if (!isReadOnlySql(form.template)) {
    ElMessage.warning('API 服务只允许单条 SELECT 或 WITH 查询语句')
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

async function openPublish(row: QueryTemplateItem) {
  publishTarget.value = row
  try {
    const detail = await getTemplateDetail(row.id)
    publishForm.secret = detail?.secret ?? ''
  } catch {
    publishForm.secret = ''
  }
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
  let template = ''
  try {
    const d = await getTemplateDetail(row.id)
    secret = d?.secret ?? ''
    template = d?.template ?? ''
  } catch {
    /* ignore */
  }
  testId.value = row.id
  testName.value = row.name
  testCode.value = row.code
  testSecret.value = secret
  const names = [...new Set((template.match(/\$\{([a-zA-Z0-9_]+)\}/g) ?? []).map((item) => item.slice(2, -1)))]
  testParameterRows.value = names.map((name) => ({ name, type: 'string', value: '', description: '' }))
  testParamsJson.value = JSON.stringify(Object.fromEntries(names.map((name) => [name, ''])), null, 2)
  advancedParams.value = false
  testMethod.value = 'list'
  testPageNum.value = 1
  testPageSize.value = 20
  testResult.value = null
  testVisible.value = true
}

async function handleTest() {
  const params = currentTestParams()
  if (params === null) return
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await testTemplate({ id: testId.value, params })
  } finally {
    testing.value = false
  }
}

function resetTestParams() {
  testParameterRows.value.forEach((item) => { item.type = 'string'; item.value = '' })
  testParamsJson.value = JSON.stringify(Object.fromEntries(testParameterRows.value.map((item) => [item.name, ''])), null, 2)
  testResult.value = null
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

/* API service refresh */
.api-page { display: grid; gap: 14px; }
.page-hero { min-height: 116px; display: flex; align-items: center; gap: 28px; padding: 22px 25px; border: 1px solid #e7e9f1; border-radius: 12px; background: linear-gradient(135deg, #fbfbff, #f5f7fc); }
.hero-copy { min-width: 280px; flex: 1; }.page-eyebrow { color: #7669ef; font-size: 9px; font-weight: 800; letter-spacing: 1.7px; }.page-hero h2 { margin: 5px 0 6px; color: #20283a; font-size: 22px; }.page-hero p { margin: 0; color: #8992a3; font-size: 11px; }
.hero-stats { display: flex; align-items: center; }.hero-stats > div { min-width: 92px; padding: 3px 20px; border-left: 1px solid #e0e3ea; text-align: center; }.hero-stats strong, .hero-stats span { display: block; }.hero-stats strong { color: #30394c; font-size: 21px; }.hero-stats span { margin-top: 3px; color: #9aa2b1; font-size: 9px; }
.filter-card :deep(.el-card__body) { padding: 16px 18px 0; }.filter-bar { margin: 0; }.filter-bar :deep(.el-form-item) { margin: 0 12px 16px 0; }
.list-card :deep(.el-card__body) { padding: 0; }.list-heading { height: 67px; display: flex; align-items: center; justify-content: space-between; padding: 0 19px; border-bottom: 1px solid #edf0f4; }.list-heading h3 { display: inline; margin: 0; color: #2c3548; font-size: 14px; }.list-heading > div > span { margin-left: 10px; color: #9aa2b1; font-size: 10px; }.support-tip { padding: 5px 9px; border-radius: 5px; color: #6e7790; background: #f4f5f8; font-size: 8px; letter-spacing: .3px; }
.api-name-cell { display: flex; align-items: center; gap: 11px; min-width: 0; }.api-logo, .dialog-logo { width: 36px; height: 36px; display: grid; place-items: center; flex-shrink: 0; border-radius: 10px; color: #fff; background: linear-gradient(145deg, #6c5ce7, #8b76f6); font-size: 8px; font-weight: 800; box-shadow: 0 7px 16px rgba(108, 92, 231, .2); }.name-cell { min-width: 0; }.name-text { color: #344054; font-size: 11px; font-weight: 650; }.name-desc { margin-top: 4px; color: #a0a7b5; font-size: 8px; }.code-text { color: #626d80; font-size: 10px; }.datasource-pill { padding: 4px 7px; border-radius: 5px; color: #326a94; background: #edf6fb; font-size: 9px; }.status-badge { display: inline-flex; align-items: center; gap: 6px; color: #929aaa; font-size: 9px; }.status-badge i { width: 6px; height: 6px; border-radius: 50%; background: #b9bec8; }.status-badge.enabled { color: #079669; }.status-badge.enabled i { background: #10b981; box-shadow: 0 0 0 3px rgba(16,185,129,.1); }.version-badge, .draft-badge { display: inline-flex; padding: 4px 8px; border-radius: 999px; font-size: 8px; font-weight: 700; }.version-badge { color: #6758e8; background: #f0eeff; }.draft-badge { color: #8a93a4; background: #f1f2f5; }.pager { margin: 0; justify-content: flex-end; padding: 16px 18px; border-top: 1px solid #edf0f4; }
.dialog-heading { display: flex; align-items: center; gap: 12px; padding-right: 38px; }.dialog-logo { width: 40px; height: 40px; border-radius: 11px; }.dialog-title { color: #172033; font-size: 17px; font-weight: 700; }.dialog-subtitle { margin-top: 3px; color: #8a94a6; font-size: 12px; }.dialog-mode { max-width: 260px; overflow: hidden; margin-left: auto; padding: 5px 9px; border: 1px solid #dedaff; border-radius: 999px; color: #6758e8; background: #f0efff; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.config-shell { height: min(680px, calc(94vh - 155px)); min-height: 520px; display: grid; grid-template-columns: 220px minmax(0, 1fr); overflow: hidden; border: 1px solid #e7e9f0; border-radius: 12px; background: #fafbfc; }.config-sidebar { display: flex; flex-direction: column; padding: 19px 14px; border-right: 1px solid #e7e9f0; background: #fbfbfd; }.sidebar-label { margin: 0 8px 10px; color: #9aa2b1; font-size: 9px; font-weight: 700; letter-spacing: .9px; }.step-card { display: flex; align-items: center; gap: 10px; padding: 11px 9px; border-radius: 8px; color: #7a8497; }.step-card + .step-card { margin-top: 3px; }.step-card > span { width: 29px; height: 29px; display: grid; place-items: center; flex-shrink: 0; border-radius: 8px; color: #9aa2b1; background: #eef0f4; font-size: 8px; font-weight: 800; }.step-card strong, .step-card small { display: block; }.step-card strong { color: #536075; font-size: 10px; }.step-card small { margin-top: 3px; color: #a0a7b5; font-size: 8px; }.step-card.done { background: #f2f0ff; }.step-card.done > span { color: #fff; background: linear-gradient(145deg,#6c5ce7,#8b76f6); }.step-card.done strong { color: #4c42a8; }
.service-summary { margin-top: auto; padding: 13px; border: 1px solid #e6e8ef; border-radius: 9px; background: #fff; }.service-summary > span { color: #9aa2b1; font-size: 8px; letter-spacing: .8px; }.service-summary > strong { display: block; overflow: hidden; margin-top: 6px; color: #364154; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }.service-summary p { margin: 5px 0 10px; color: #8e97a7; font-size: 8px; }.service-summary div { display: flex; align-items: center; gap: 6px; color: #9aa2b1; font-size: 8px; }.service-summary div i { width: 6px; height: 6px; border-radius: 50%; background: #c0c5cf; }.service-summary div i.ready { background: #10b981; }
.config-content { min-width: 0; overflow-y: auto; padding: 24px 27px; background: #fff; }.form-section + .form-section { margin-top: 25px; padding-top: 23px; border-top: 1px solid #eceef3; }.section-heading { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; }.section-heading > div { display: flex; align-items: flex-start; gap: 10px; }.section-index { width: 25px; height: 25px; display: grid; place-items: center; flex-shrink: 0; border-radius: 7px; color: #635bff; background: #eeecff; font-size: 8px; font-weight: 800; }.section-heading h3 { margin: 0; color: #293247; font-size: 13px; }.section-heading p { margin: 4px 0 0; color: #929aaa; font-size: 9px; }.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 16px; }.field { min-width: 0; }.field label { display: block; margin-bottom: 7px; color: #4f596c; font-size: 10px; font-weight: 650; }.field label em { color: #ef5361; font-style: normal; }.wide-field { grid-column: 1 / -1; }.format-action { display: inline-flex; align-items: center; gap: 5px; padding: 6px 9px; border: 1px solid #dfe2e9; border-radius: 6px; color: #667085; background: #fff; font-size: 9px; cursor: pointer; }.format-action:hover { color: #635bff; border-color: #bdb7f8; }
.editor-panel { border: 1px solid #343542; border-radius: 9px; box-shadow: none; }.editor-toolbar { padding: 8px 12px; background: #252630; }.editor-lang { font-size: 9px; }.editor-toolbar-hint { font-size: 9px; }.editor-wrap { height: 245px; }.sql-hint { display: flex; align-items: flex-start; gap: 9px; margin-top: 10px; padding: 10px 12px; border-radius: 7px; color: #667085; background: #f5f7fb; }.sql-hint span { width: 17px; height: 17px; display: grid; place-items: center; flex-shrink: 0; border-radius: 50%; color: #fff; background: #7b72e8; font-size: 9px; font-weight: 700; }.sql-hint p { margin: 1px 0 0; font-size: 9px; line-height: 1.55; }.param-counter { padding: 4px 7px; border-radius: 5px; color: #6758e8; background: #f0efff; font-size: 8px; font-weight: 700; }
.parameter-table { overflow: hidden; border: 1px solid #e5e8ef; border-radius: 9px; }.parameter-head, .parameter-row { display: grid; grid-template-columns: 1.1fr .75fr 1.2fr 1.3fr; align-items: center; gap: 10px; padding: 9px 12px; }.parameter-head { color: #8992a3; background: #f7f8fa; font-size: 8px; font-weight: 700; }.parameter-row + .parameter-row { border-top: 1px solid #edf0f4; }.parameter-name { min-width: 0; }.parameter-name code { display: block; overflow: hidden; color: #635bff; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }.parameter-name small { display: block; margin-top: 3px; color: #eb5967; font-size: 8px; }.parameter-empty { display: flex; align-items: center; gap: 13px; padding: 18px; border: 1px dashed #dfe3ea; border-radius: 9px; background: #fafbfc; }.parameter-empty > span { width: 37px; height: 37px; display: grid; place-items: center; border-radius: 9px; color: #7469df; background: #eeecff; font-family: monospace; font-size: 12px; }.parameter-empty strong, .parameter-empty p { display: block; }.parameter-empty strong { color: #566176; font-size: 10px; }.parameter-empty p { margin: 4px 0 0; color: #9aa2b1; font-size: 8px; }.runtime-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }.setting-card { display: flex; align-items: center; gap: 9px; padding: 13px 14px; border: 1px solid #e8eaf0; border-radius: 8px; background: #fafbfc; }.setting-card > div { min-width: 0; flex: 1; }.setting-card strong, .setting-card small { display: block; }.setting-card strong { color: #4b5568; font-size: 10px; }.setting-card small { margin-top: 4px; color: #969ead; font-size: 8px; }.setting-card > span { color: #8f98a8; font-size: 9px; }.dialog-footer { width: 100%; display: flex; align-items: center; }.footer-tip { margin-right: auto; color: #8f98a8; font-size: 9px; }
.test-shell { display: grid; grid-template-columns: 320px minmax(0,1fr); overflow: hidden; min-height: 350px; border: 1px solid #e5e8ef; border-radius: 10px; }.test-params-panel, .test-result-panel { min-width: 0; padding: 16px; }.test-params-panel { border-right: 1px solid #e5e8ef; background: #fafbfc; }.test-panel-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 14px; }.test-panel-head > div strong, .test-panel-head > div span { display: block; }.test-panel-head strong { color: #3c4659; font-size: 11px; }.test-panel-head > div span { margin-top: 3px; color: #9aa2b1; font-size: 8px; }.test-param-list { display: grid; gap: 12px; max-height: 245px; overflow-y: auto; padding-right: 3px; }.test-param-item { padding: 10px; border: 1px solid #e5e8ef; border-radius: 8px; background: #fff; }.test-param-label { display: flex; align-items: center; justify-content: space-between; margin-bottom: 7px; }.test-param-label code { color: #635bff; font-size: 10px; font-weight: 700; }.test-param-label span { color: #e45564; font-size: 8px; }.test-param-control { display: flex; gap: 7px; }.test-actions { margin: 14px 0 0; }.parameter-empty.compact { padding: 14px; }.json-hint { margin-top: 5px; color: #9aa2b1; font-size: 8px; }.test-logo { font-size: 7px; background: linear-gradient(145deg,#0ea5a4,#3b82f6); }.result-table { width: 100%; }.curl-block { margin-top: 14px; border-radius: 9px; }.curl-head { padding: 9px 12px; }.curl-head > div:first-child strong, .curl-head > div:first-child span { display: block; }.curl-head > div:first-child strong { font-size: 10px; }.curl-head > div:first-child span { margin-top: 3px; color: #929aaa; font-size: 8px; font-weight: 400; }.curl-code { max-height: 145px; font-size: 10px; }
.publish-target { border-radius: 9px; }.log-block { border-radius: 8px; }.muted { color: #c0c4cc; }
@media (max-width: 900px) { .page-hero { align-items: flex-start; flex-wrap: wrap; }.hero-stats { order: 3; width: 100%; }.hero-stats > div:first-child { border-left: 0; }.config-shell { grid-template-columns: 185px minmax(0,1fr); }.config-content { padding: 21px 18px; }.parameter-head, .parameter-row { grid-template-columns: 1fr 90px 1fr; }.parameter-head span:last-child, .parameter-row > :last-child { display: none; }.test-shell { grid-template-columns: 280px minmax(0,1fr); } }
@media (max-width: 680px) { .page-hero { gap: 16px; padding: 18px; }.hero-copy { min-width: 0; width: 100%; }.page-hero p, .support-tip { display: none; }.hero-stats > div { min-width: 0; flex: 1; padding: 3px 9px; }.list-heading { padding: 0 14px; }.dialog-heading { gap: 9px; padding-right: 28px; }.dialog-logo { width: 34px; height: 34px; }.dialog-subtitle, .dialog-mode { display: none; }.dialog-title { font-size: 15px; white-space: nowrap; }.config-shell { height: calc(94vh - 138px); min-height: 0; display: block; overflow-y: auto; }.config-sidebar { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 5px; padding: 10px; border-right: 0; border-bottom: 1px solid #e7e9f0; }.sidebar-label { grid-column: 1/-1; }.step-card { padding: 7px; }.step-card small, .service-summary { display: none; }.config-content { overflow: visible; padding: 18px 13px; }.form-grid, .runtime-grid { grid-template-columns: 1fr; }.wide-field { grid-column: auto; }.parameter-head { display: none; }.parameter-row { grid-template-columns: 1fr; gap: 7px; padding: 12px; }.parameter-row > :last-child { display: flex; }.parameter-row + .parameter-row { border-top: 1px solid #e7e9f0; }.footer-tip { display: none; }.dialog-footer { gap: 5px; }.dialog-footer .el-button { min-width: 0; margin-left: 0; padding: 8px 9px; font-size: 11px; }.test-shell { display: block; max-height: calc(92vh - 340px); overflow-y: auto; }.test-params-panel { border-right: 0; border-bottom: 1px solid #e5e8ef; }.curl-head { align-items: flex-start; gap: 9px; }.curl-head-right { flex-wrap: wrap; justify-content: flex-end; } }
</style>

<style>
.api-config-dialog.el-dialog, .api-test-dialog.el-dialog { overflow: hidden; border-radius: 14px; }
.api-config-dialog .el-dialog__header, .api-test-dialog .el-dialog__header { margin-right: 0; padding: 18px 22px 14px; border-bottom: 1px solid #edf0f4; }
.api-config-dialog .el-dialog__body, .api-test-dialog .el-dialog__body { padding: 16px 20px; }
.api-config-dialog .el-dialog__footer { padding: 13px 20px 17px; border-top: 1px solid #edf0f4; }
@media (max-width:680px) { .api-config-dialog .el-dialog__header, .api-test-dialog .el-dialog__header { padding: 14px 13px 11px; }.api-config-dialog .el-dialog__body, .api-test-dialog .el-dialog__body { padding: 10px; }.api-config-dialog .el-dialog__footer { padding: 10px 12px 12px; } }
</style>
