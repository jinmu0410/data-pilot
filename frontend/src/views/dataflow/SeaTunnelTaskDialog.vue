<template>
  <el-dialog
    :model-value="modelValue"
    width="min(1180px, calc(100vw - 40px))"
    top="4vh"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
    :before-close="cancel"
    class="seatunnel-task-dialog"
    @closed="emit('closed')"
  >
    <template #header>
      <div class="dialog-heading">
        <span class="engine-logo">ST</span>
        <div>
          <div class="dialog-title">SeaTunnel 同步任务</div>
          <div class="dialog-subtitle">JDBC 批同步 · Source → Transform → Sink</div>
        </div>
        <span class="engine-tag">BATCH</span>
      </div>
    </template>

    <div class="wizard-shell">
      <aside class="wizard-nav">
        <button
          v-for="(item, index) in steps"
          :key="item.title"
          type="button"
          class="step-button"
          :class="{ active: step === index, done: step > index }"
          @click="goToStep(index)"
        >
          <span class="step-index">{{ step > index ? '✓' : index + 1 }}</span>
          <span><strong>{{ item.title }}</strong><small>{{ item.description }}</small></span>
        </button>

        <div class="task-summary">
          <span class="summary-label">PIPELINE</span>
          <strong>{{ config.name || '未命名任务' }}</strong>
          <div class="summary-flow">
            <span>{{ sourceDatasource?.name || '源端待选择' }}</span><i>→</i><span>{{ targetDatasource?.name || '目标端待选择' }}</span>
          </div>
          <div class="summary-meta">
            <span>{{ config.readMode === 'query' ? 'SQL 读取' : '整表读取' }}</span>
            <span>{{ completeMappings.length }} 个映射</span>
            <span>{{ config.parallelism || 1 }} 并行度</span>
          </div>
        </div>
      </aside>

      <main class="wizard-content">
        <section v-show="step === 0" class="step-panel">
          <div class="section-heading">
            <div><span class="eyebrow">BASIC</span><h3>定义同步任务</h3></div>
            <p>当前版本聚焦稳定的 JDBC 离线批同步，实时与 CDC 将由专用连接器承载。</p>
          </div>
          <div class="form-card">
            <label class="field-label">任务名称 <em>*</em></label>
            <el-input v-model="config.name" maxlength="80" show-word-limit size="large" placeholder="例如：用户维表全量同步" />
            <label class="field-label field-gap">任务描述</label>
            <el-input v-model="config.description" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="说明同步范围、业务用途或负责人（可选）" />
          </div>
          <div class="mode-grid">
            <button type="button" class="mode-card selected">
              <span class="mode-icon">B</span><span><strong>离线批同步</strong><small>使用 SeaTunnel Zeta BATCH 模式读取当前快照</small></span><i>✓</i>
            </button>
            <button type="button" class="mode-card disabled" disabled>
              <span class="mode-icon stream">S</span><span><strong>实时 / CDC</strong><small>需要 MySQL CDC、Kafka 等专用连接器，后续开放</small></span><i>规划中</i>
            </button>
          </div>
        </section>

        <section v-show="step === 1" class="step-panel">
          <div class="section-heading">
            <div><span class="eyebrow">ENDPOINT</span><h3>选择 JDBC 数据端点</h3></div>
            <p>这里只展示当前已适配的 MySQL、TiDB 和 PostgreSQL，避免错误选择专用连接器。</p>
          </div>
          <div v-if="unsupportedCount" class="info-banner">已隐藏 {{ unsupportedCount }} 个非 JDBC 数据源；Doris、Kafka、Elastic 需要单独的 Connector 配置。</div>
          <div class="endpoint-grid">
            <article class="endpoint-card source-card">
              <div class="endpoint-head"><span>01</span><div><strong>数据来源</strong><small>JDBC Source</small></div><em :class="{ ready: sourceDatasource }">{{ sourceDatasource ? '已选择' : '待配置' }}</em></div>
              <label class="field-label">源数据源 <b>*</b></label>
              <el-select v-model="config.sourceDataSourceCode" filterable size="large" placeholder="选择源数据源" @change="onDatasourceChange('source')">
                <el-option v-for="ds in supportedDatasources" :key="ds.code" :label="`${ds.name} · ${ds.type}`" :value="ds.code" />
              </el-select>
              <label class="field-label field-gap">读取方式</label>
              <el-radio-group v-model="config.readMode" @change="onReadModeChange">
                <el-radio-button value="table">整表读取</el-radio-button>
                <el-radio-button value="query">自定义 SQL</el-radio-button>
              </el-radio-group>
              <template v-if="config.readMode === 'table'">
                <label class="field-label field-gap">源库 / Schema <b>*</b></label>
                <el-select v-model="config.sourceSchema" filterable :loading="loading.sourceTree" placeholder="选择源库" @change="onSchemaChange('source')">
                  <el-option v-for="item in sourceTree" :key="item.key" :label="item.label" :value="item.key" />
                </el-select>
                <label class="field-label field-gap">源表 <b>*</b></label>
                <el-select v-model="config.sourceTable" filterable :disabled="!config.sourceSchema" placeholder="选择源表" @change="onTableChange('source')">
                  <el-option v-for="item in sourceTables" :key="item.key" :label="item.label" :value="item.key" />
                </el-select>
              </template>
              <div v-else class="query-hint">下一步编写 SELECT 查询；源字段可手动录入并映射。</div>
            </article>

            <div class="flow-direction"><span>数据流向</span><i>→</i></div>

            <article class="endpoint-card target-card">
              <div class="endpoint-head"><span>02</span><div><strong>写入目标</strong><small>JDBC Sink</small></div><em :class="{ ready: targetDatasource }">{{ targetDatasource ? '已选择' : '待配置' }}</em></div>
              <label class="field-label">目标数据源 <b>*</b></label>
              <el-select v-model="config.targetDataSourceCode" filterable size="large" placeholder="选择目标数据源" @change="onDatasourceChange('target')">
                <el-option v-for="ds in supportedDatasources" :key="ds.code" :label="`${ds.name} · ${ds.type}`" :value="ds.code" />
              </el-select>
              <label class="field-label field-gap">目标库 / Schema <b>*</b></label>
              <el-select v-model="config.targetSchema" filterable :loading="loading.targetTree" placeholder="选择目标库" @change="onSchemaChange('target')">
                <el-option v-for="item in targetTree" :key="item.key" :label="item.label" :value="item.key" />
              </el-select>
              <label class="field-label field-gap">目标表 <b>*</b></label>
              <el-select v-model="config.targetTable" filterable :disabled="!config.targetSchema" placeholder="选择目标表" @change="onTableChange('target')">
                <el-option v-for="item in targetTables" :key="item.key" :label="item.label" :value="item.key" />
              </el-select>
            </article>
          </div>
        </section>

        <section v-show="step === 2" class="step-panel">
          <div class="section-heading">
            <div><span class="eyebrow">TRANSFORM</span><h3>读取、过滤与字段映射</h3></div>
            <div class="header-actions"><el-button :disabled="!sourceColumns.length || !targetColumns.length" @click="autoMap">智能匹配</el-button><el-button type="primary" plain @click="addMapping">添加映射</el-button></div>
          </div>
          <div v-if="config.readMode === 'query'" class="sql-card">
            <div class="block-title"><span>读取 SQL <b>*</b></span><small>仅允许 SELECT，字段名需要与下方源字段一致</small></div>
            <SqlEditor v-model="config.sqlText" height="160px" />
          </div>
          <div class="advanced-grid">
            <div><label class="field-label">过滤条件</label><el-input v-model="config.whereCondition" placeholder="例如：where updated_at >= '2026-01-01'" /><small>必须以 where 开头；留空表示不过滤。</small></div>
            <div><label class="field-label">分片字段</label><el-select v-model="config.partitionColumn" filterable allow-create clearable placeholder="可选，建议主键或唯一索引"><el-option v-for="column in sourceColumns" :key="column.name" :label="columnLabel(column)" :value="column.name" /></el-select><small>用于并行拆分读取，需出现在查询结果中。</small></div>
            <div><label class="field-label">分片数量</label><el-input-number v-model="config.partitionNum" :min="1" :max="256" :controls="false" /><small>配置分片字段后生效，默认 10。</small></div>
          </div>
          <div class="mapping-table">
            <div class="mapping-head"><span>源字段</span><span>转换</span><span>目标字段</span><span></span></div>
            <div v-if="mappingRows.length" class="mapping-list">
              <div v-for="(row, index) in mappingRows" :key="index" class="mapping-row">
                <el-select v-model="row.source" filterable :allow-create="config.readMode === 'query'" placeholder="选择或输入源字段">
                  <el-option v-for="column in sourceColumns" :key="column.name" :label="columnLabel(column)" :value="column.name" />
                </el-select>
                <span class="map-arrow">→</span>
                <el-select v-model="row.target" filterable placeholder="选择目标字段">
                  <el-option v-for="column in targetColumns" :key="column.name" :label="columnLabel(column)" :value="column.name" />
                </el-select>
                <el-button link type="danger" @click="removeMapping(index)">移除</el-button>
              </div>
            </div>
            <div v-else class="mapping-empty"><strong>尚未配置字段映射</strong><p>选择两端表后使用智能匹配；映射会生成 SeaTunnel FieldMapper，而不是只改查询文本。</p></div>
          </div>
          <div class="mapping-footer"><span>源字段 {{ sourceColumns.length }}</span><span>目标字段 {{ targetColumns.length }}</span><strong>有效映射 {{ completeMappings.length }}</strong></div>
        </section>

        <section v-show="step === 3" class="step-panel">
          <div class="section-heading">
            <div><span class="eyebrow">SINK & RUNTIME</span><h3>写入与运行策略</h3></div>
            <p>生成 SQL 模式可处理建表、清表和主键冲突；自定义 SQL 模式由你控制参数顺序。</p>
          </div>
          <div class="settings-grid">
            <article class="settings-card">
              <div class="block-title"><span>写入策略</span><small>两种模式互斥</small></div>
              <el-radio-group v-model="config.sinkWriteStrategy">
                <el-radio-button value="generated">自动生成 SQL</el-radio-button>
                <el-radio-button value="custom">自定义 SQL</el-radio-button>
              </el-radio-group>
              <template v-if="config.sinkWriteStrategy === 'generated'">
                <label class="field-label field-gap">表结构处理</label>
                <el-select v-model="config.schemaSaveMode"><el-option v-for="item in schemaModes" :key="item.value" :label="item.label" :value="item.value" /></el-select>
                <label class="field-label field-gap">已有数据处理</label>
                <el-select v-model="config.dataSaveMode"><el-option v-for="item in dataModes" :key="item.value" :label="item.label" :value="item.value" /></el-select>
                <label class="field-label field-gap">冲突主键</label>
                <el-select v-model="config.primaryKeys" multiple filterable clearable placeholder="不选则普通 Insert">
                  <el-option v-for="column in targetColumns" :key="column.name" :label="column.primaryKey ? `${column.name} · 主键` : column.name" :value="column.name" />
                </el-select>
                <div class="strategy-tip">选择主键后 SeaTunnel 才能生成数据库原生 Upsert/Update；主键必须包含在字段映射中。</div>
              </template>
              <template v-else>
                <label class="field-label field-gap">参数化写入 SQL <b>*</b></label>
                <SqlEditor v-model="config.sinkQuery" height="150px" />
                <div class="strategy-tip">使用 ? 占位符，顺序必须与 FieldMapper 输出字段顺序一致；该模式不执行表结构和数据保存策略。</div>
              </template>
            </article>
            <article class="settings-card">
              <div class="block-title"><span>性能与保护</span><small>从保守值开始调优</small></div>
              <div class="number-grid">
                <div><label class="field-label">并行度</label><el-input-number v-model="config.parallelism" :min="1" :max="64" :controls="false" /></div>
                <div><label class="field-label">读取批次</label><el-input-number v-model="config.fetchSize" :min="0" :controls="false" placeholder="驱动默认" /></div>
                <div><label class="field-label">写入批次</label><el-input-number v-model="config.batchSize" :min="1" :max="100000" :controls="false" /></div>
                <div><label class="field-label">失败重试</label><el-input-number v-model="config.retryTimes" :min="0" :max="10" :controls="false" /></div>
                <div><label class="field-label">任务超时（秒）</label><el-input-number v-model="config.timeout" :min="1" :max="86400" :controls="false" /></div>
              </div>
              <div class="strategy-tip">并行度决定并发 Reader/Writer；只有可拆分字段才能让源端真正并行。</div>
            </article>
          </div>
          <div v-if="config.dataSaveMode === 'CUSTOM_PROCESSING' && config.sinkWriteStrategy === 'generated'" class="sql-card custom-process">
            <div class="block-title"><span>同步前自定义 SQL <b>*</b></span><small>例如清理指定业务分区</small></div>
            <SqlEditor v-model="config.customSql" height="130px" />
          </div>
        </section>

        <section v-show="step === 4" class="step-panel">
          <div class="section-heading">
            <div><span class="eyebrow">REVIEW</span><h3>检查并应用配置</h3></div>
            <p>预览隐藏了真实连接和密码；任务运行时由后端安全注入。</p>
          </div>
          <div class="review-grid">
            <article class="check-card">
              <div class="block-title"><span>就绪检查</span><small>{{ readiness.filter(item => item.ok).length }}/{{ readiness.length }}</small></div>
              <div v-for="item in readiness" :key="item.label" class="check-row" :class="{ ok: item.ok }"><i>{{ item.ok ? '✓' : '!' }}</i><span><strong>{{ item.label }}</strong><small>{{ item.hint }}</small></span></div>
            </article>
            <article class="preview-card">
              <div class="block-title"><span>HOCON 结构预览</span><small>敏感信息已替换</small></div>
              <pre>{{ configPreview }}</pre>
            </article>
          </div>
        </section>
      </main>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="danger" link @click="emit('delete')">删除节点</el-button>
        <el-button plain @click="preview">预览配置</el-button><span></span>
        <el-button @click="cancelWithoutDone">取消</el-button>
        <el-button v-if="step > 0" @click="step--">上一步</el-button>
        <el-button v-if="step < steps.length - 1" type="primary" @click="next">下一步</el-button>
        <el-button v-else type="primary" @click="apply">应用配置</el-button>
      </div>
    </template>

    <el-dialog
      v-model="previewVisible"
      title="最终生成的配置"
      width="760px"
      top="6vh"
      append-to-body
      :close-on-click-modal="false"
    >
      <div class="preview-command">
        <span>执行命令</span>
        <code>{{ previewCommand.join(' ') }}</code>
      </div>
      <pre class="preview-json">{{ previewContent }}</pre>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import SqlEditor from '../../components/SqlEditor.vue'
import { listSchemaTable, tableDetail, type SchemaTableMap, type TableColumn } from '../../api/datasource'
import { previewEngineConfig } from '../../api/dataflow'
import type { FieldMappingRow, NodeConfig } from './nodes'

interface DataSourceOption { id: number; code: string; name: string; type: string }
const props = defineProps<{ modelValue: boolean; config: NodeConfig; datasources: DataSourceOption[] }>()
const emit = defineEmits<{ (event: 'update:modelValue', value: boolean): void; (event: 'delete'): void; (event: 'closed'): void }>()

const steps = [
  { title: '基础信息', description: '名称与执行模式' },
  { title: '数据端点', description: 'JDBC 来源与目标' },
  { title: '字段转换', description: '读取、过滤与映射' },
  { title: '运行策略', description: '写入语义与性能' },
  { title: '检查预览', description: '确认 HOCON 结构' }
]
const schemaModes = [
  { label: '目标表必须已存在', value: 'ERROR_WHEN_SCHEMA_NOT_EXIST' },
  { label: '不存在时自动创建', value: 'CREATE_SCHEMA_WHEN_NOT_EXIST' },
  { label: '忽略表结构处理', value: 'IGNORE' },
  { label: '删除并重建目标表', value: 'RECREATE_SCHEMA' }
]
const dataModes = [
  { label: '追加数据', value: 'APPEND_DATA' },
  { label: '清空后写入', value: 'DROP_DATA' },
  { label: '执行自定义前置 SQL', value: 'CUSTOM_PROCESSING' },
  { label: '存在数据则报错', value: 'ERROR_WHEN_DATA_EXISTS' }
]
const jdbcTypes = new Set(['mysql', 'tidb', 'postgresql'])
const step = ref(0)
const snapshot = ref('')
const sourceTree = ref<SchemaTableMap[]>([])
const targetTree = ref<SchemaTableMap[]>([])
const sourceColumns = ref<TableColumn[]>([])
const targetColumns = ref<TableColumn[]>([])
const loading = reactive({ sourceTree: false, targetTree: false, sourceColumns: false, targetColumns: false })

const previewVisible = ref(false)
const previewContent = ref('')
const previewCommand = ref<string[]>([])

async function preview() {
  const node = { ...props.config }
  delete node.__type
  delete node.readMode
  try {
    const result = await previewEngineConfig('SEATUNNEL', node)
    previewContent.value = result.configContent
    previewCommand.value = result.command ?? []
    previewVisible.value = true
  } catch {
    // 错误提示已在拦截器处理
  }
}
const supportedDatasources = computed(() => props.datasources.filter(item => jdbcTypes.has(item.type.toLowerCase())))
const unsupportedCount = computed(() => props.datasources.length - supportedDatasources.value.length)
const sourceDatasource = computed(() => supportedDatasources.value.find(item => item.code === props.config.sourceDataSourceCode))
const targetDatasource = computed(() => supportedDatasources.value.find(item => item.code === props.config.targetDataSourceCode))
const sourceTables = computed(() => sourceTree.value.find(item => item.key === props.config.sourceSchema)?.children ?? [])
const targetTables = computed(() => targetTree.value.find(item => item.key === props.config.targetSchema)?.children ?? [])
const mappingRows = computed<FieldMappingRow[]>(() => {
  if (!Array.isArray(props.config.fieldMapping)) props.config.fieldMapping = []
  return props.config.fieldMapping
})
const completeMappings = computed(() => mappingRows.value.filter(item => String(item.source || '').trim() && String(item.target || '').trim()))
const mappedTargets = computed(() => new Set(completeMappings.value.map(item => item.target)))
const sourcePath = computed(() => [props.config.sourceSchema, props.config.sourceTable].filter(Boolean).join('.'))
const targetPath = computed(() => [props.config.targetSchema, props.config.targetTable].filter(Boolean).join('.'))
const readiness = computed(() => [
  { label: '数据端点', ok: !!sourceDatasource.value && !!targetDatasource.value, hint: '源端和目标端均使用已适配的 JDBC 数据源' },
  { label: '读取配置', ok: props.config.readMode === 'table' ? !!props.config.sourceTable : isSelectSql(props.config.sqlText), hint: props.config.readMode === 'table' ? sourcePath.value || '尚未选择源表' : '自定义 SQL 必须为 SELECT' },
  { label: '字段映射', ok: completeMappings.value.length > 0, hint: `已配置 ${completeMappings.value.length} 个有效 FieldMapper 映射` },
  { label: '主键约束', ok: !(props.config.primaryKeys || []).some((key: string) => !mappedTargets.value.has(key)), hint: '冲突主键必须包含在目标字段映射中' },
  { label: '写入策略', ok: props.config.sinkWriteStrategy !== 'custom' || /\?/.test(props.config.sinkQuery || ''), hint: props.config.sinkWriteStrategy === 'custom' ? '自定义 SQL 需要 ? 参数占位符' : `${props.config.schemaSaveMode} / ${props.config.dataSaveMode}` }
])
const configPreview = computed(() => {
  const mappings = completeMappings.value.map(item => `      ${quoted(item.source)} = ${quoted(item.target)}`).join('\n')
  const primaryKeys = (props.config.primaryKeys || []).map((item: string) => quoted(item)).join(', ')
  const sourceRead = props.config.readMode === 'query'
    ? `    query = ${quoted(props.config.sqlText || 'SELECT ...')}`
    : `    table_path = ${quoted(sourcePath.value || 'schema.table')}`
  const transform = mappings ? `\ntransform {\n  FieldMapper {\n    plugin_input = "source_rows"\n    plugin_output = "mapped_rows"\n    field_mapper = {\n${mappings}\n    }\n  }\n}\n` : ''
  const sinkMode = props.config.sinkWriteStrategy === 'custom'
    ? `    generate_sink_sql = false\n    query = ${quoted(props.config.sinkQuery || 'INSERT INTO ... VALUES (?)')}`
    : `    generate_sink_sql = true\n    database = "<target-database>"\n    table = ${quoted(targetPath.value || 'schema.table')}\n${primaryKeys ? `    primary_keys = [${primaryKeys}]\n` : ''}    schema_save_mode = ${quoted(props.config.schemaSaveMode)}\n    data_save_mode = ${quoted(props.config.dataSaveMode)}`
  return `env {\n  job.mode = "BATCH"\n  parallelism = ${props.config.parallelism || 1}\n  job.retry.times = ${props.config.retryTimes || 0}\n}\n\nsource {\n  Jdbc {\n    plugin_output = "source_rows"\n    url = "<source-jdbc-url>"\n    username = "<secret>"\n    password = "<secret>"\n${sourceRead}\n  }\n}\n${transform}\nsink {\n  Jdbc {\n    plugin_input = "${mappings ? 'mapped_rows' : 'source_rows'}"\n    url = "<target-jdbc-url>"\n    username = "<secret>"\n    password = "<secret>"\n${sinkMode}\n    batch_size = ${props.config.batchSize || 1000}\n    max_retries = ${props.config.retryTimes || 0}\n  }\n}`
})

watch(() => props.modelValue, async visible => {
  if (!visible) return
  snapshot.value = JSON.stringify(props.config)
  initializeDefaults()
  step.value = 0
  await Promise.all([loadTree('source'), loadTree('target')])
  await Promise.all([loadColumns('source'), loadColumns('target')])
}, { immediate: true })

function initializeDefaults() {
  props.config.description ||= ''
  props.config.readMode ||= 'table'
  props.config.sqlText ||= ''
  props.config.fieldMapping ||= []
  props.config.whereCondition ||= ''
  props.config.partitionColumn ||= ''
  props.config.partitionNum ||= 10
  props.config.sinkWriteStrategy ||= 'generated'
  props.config.sinkQuery ||= ''
  props.config.schemaSaveMode ||= 'ERROR_WHEN_SCHEMA_NOT_EXIST'
  props.config.dataSaveMode ||= 'APPEND_DATA'
  props.config.customSql ||= ''
  props.config.primaryKeys ||= []
  props.config.parallelism ||= 1
  props.config.fetchSize ??= 0
  props.config.batchSize ||= 1000
  props.config.retryTimes ??= 0
  props.config.timeout ||= 600
}
function datasource(side: 'source' | 'target') {
  const code = side === 'source' ? props.config.sourceDataSourceCode : props.config.targetDataSourceCode
  return supportedDatasources.value.find(item => item.code === code)
}
async function loadTree(side: 'source' | 'target') {
  const selected = datasource(side)
  if (!selected) return
  const key = side === 'source' ? 'sourceTree' : 'targetTree'
  const tree = side === 'source' ? sourceTree : targetTree
  loading[key] = true
  try { tree.value = await listSchemaTable(selected.id) } catch { tree.value = [] } finally { loading[key] = false }
}
async function loadColumns(side: 'source' | 'target') {
  const selected = datasource(side)
  const schema = side === 'source' ? props.config.sourceSchema : props.config.targetSchema
  const table = side === 'source' ? props.config.sourceTable : props.config.targetTable
  if (!selected || !schema || !table) return
  const key = side === 'source' ? 'sourceColumns' : 'targetColumns'
  const columns = side === 'source' ? sourceColumns : targetColumns
  loading[key] = true
  try { columns.value = (await tableDetail(selected.id, schema, table)).columns ?? [] } catch { columns.value = [] } finally { loading[key] = false }
}
async function onDatasourceChange(side: 'source' | 'target') {
  if (side === 'source') { props.config.sourceSchema = ''; props.config.sourceTable = ''; sourceTree.value = []; sourceColumns.value = [] }
  else { props.config.targetSchema = ''; props.config.targetTable = ''; targetTree.value = []; targetColumns.value = []; props.config.primaryKeys = [] }
  props.config.fieldMapping = []
  await loadTree(side)
}
function onSchemaChange(side: 'source' | 'target') {
  if (side === 'source') { props.config.sourceTable = ''; sourceColumns.value = [] }
  else { props.config.targetTable = ''; targetColumns.value = []; props.config.primaryKeys = [] }
  props.config.fieldMapping = []
}
async function onTableChange(side: 'source' | 'target') {
  if (side === 'source') sourceColumns.value = []
  else { targetColumns.value = []; props.config.primaryKeys = [] }
  props.config.fieldMapping = []
  await loadColumns(side)
  if (sourceColumns.value.length && targetColumns.value.length) autoMap()
}
function onReadModeChange() {
  props.config.fieldMapping = []
  props.config.partitionColumn = ''
}
function addMapping() { mappingRows.value.push({ source: '', target: '' }) }
function removeMapping(index: number) { mappingRows.value.splice(index, 1) }
function autoMap() {
  const unused = [...targetColumns.value]
  props.config.fieldMapping = sourceColumns.value.map((source, index) => {
    const exact = unused.findIndex(target => target.name.toLowerCase() === source.name.toLowerCase())
    const target = exact >= 0 ? unused.splice(exact, 1)[0] : unused.shift() ?? targetColumns.value[index]
    return { source: source.name, target: target?.name || '' }
  }).filter(item => item.target)
}
function columnLabel(column: TableColumn) { return column.type ? `${column.name} · ${column.type}${column.primaryKey ? ' · PK' : ''}` : column.name }
function quoted(value: string) { return JSON.stringify(String(value || '')) }
function isSelectSql(value: unknown) { return /^\s*(with\b[\s\S]+\bselect\b|select\b)/i.test(String(value || '')) }
function validate(index: number, quiet = false) {
  let message = ''
  if (index === 0 && !String(props.config.name || '').trim()) message = '请填写任务名称'
  if (index === 1) {
    if (!sourceDatasource.value) message = '请选择受支持的源数据源'
    else if (props.config.readMode === 'table' && (!props.config.sourceSchema || !props.config.sourceTable)) message = '请选择源库和源表'
    else if (!targetDatasource.value) message = '请选择受支持的目标数据源'
    else if (!props.config.targetSchema || !props.config.targetTable) message = '请选择目标库和目标表'
  }
  if (index === 2) {
    if (props.config.readMode === 'query' && !isSelectSql(props.config.sqlText)) message = '自定义读取 SQL 必须是 SELECT 查询'
    else if (props.config.whereCondition && !/^\s*where\s+/i.test(props.config.whereCondition)) message = '过滤条件必须以 where 开头'
    else if (!completeMappings.value.length) message = '请至少配置一个完整字段映射'
    else if (new Set(completeMappings.value.map(item => item.target)).size !== completeMappings.value.length) message = '目标字段不能重复映射'
  }
  if (index === 3) {
    const missingPrimary = (props.config.primaryKeys || []).find((key: string) => !mappedTargets.value.has(key))
    if (props.config.sinkWriteStrategy === 'custom' && !String(props.config.sinkQuery || '').trim()) message = '请填写参数化写入 SQL'
    else if (props.config.sinkWriteStrategy === 'custom' && !/\?/.test(props.config.sinkQuery)) message = '自定义写入 SQL 至少需要一个 ? 占位符'
    else if (props.config.dataSaveMode === 'CUSTOM_PROCESSING' && !String(props.config.customSql || '').trim()) message = '请填写同步前自定义 SQL'
    else if (missingPrimary) message = `主键 ${missingPrimary} 没有包含在目标字段映射中`
    else if (!props.config.parallelism || props.config.parallelism < 1) message = '并行度必须大于 0'
    else if (!props.config.batchSize || props.config.batchSize < 1) message = '写入批次必须大于 0'
    else if (!props.config.timeout || props.config.timeout < 1) message = '任务超时必须大于 0'
  }
  if (message && !quiet) ElMessage.warning(message)
  return !message
}
function goToStep(target: number) {
  if (target <= step.value) { step.value = target; return }
  for (let index = 0; index < target; index++) if (!validate(index)) { step.value = index; return }
  step.value = target
}
function next() { if (validate(step.value)) step.value++ }
function apply() {
  for (let index = 0; index < steps.length - 1; index++) if (!validate(index)) { step.value = index; return }
  props.config.name = String(props.config.name).trim()
  props.config.fieldMapping = completeMappings.value.map(item => ({ ...item }))
  snapshot.value = JSON.stringify(props.config)
  emit('update:modelValue', false)
  ElMessage.success('SeaTunnel 节点配置已应用')
}
function restoreSnapshot() {
  const original = JSON.parse(snapshot.value || '{}')
  for (const key of Object.keys(props.config)) delete props.config[key]
  Object.assign(props.config, original)
}
function cancel(done: () => void) { restoreSnapshot(); done() }
function cancelWithoutDone() { restoreSnapshot(); emit('update:modelValue', false) }
</script>

<style scoped>
.dialog-heading{display:flex;align-items:center;gap:12px;padding-right:38px}.engine-logo{width:40px;height:40px;display:grid;place-items:center;border-radius:12px;color:#fff;font-weight:800;font-size:13px;background:linear-gradient(145deg,#10b981,#14b8a6);box-shadow:0 8px 18px rgba(16,185,129,.25)}.dialog-title{color:#172033;font-size:17px;font-weight:700}.dialog-subtitle{color:#8a94a6;font-size:12px;margin-top:3px}.engine-tag{margin-left:auto;padding:5px 10px;color:#07845f;background:#e9fbf4;border:1px solid #c8f2df;border-radius:999px;font-size:12px}.wizard-shell{height:min(680px,78vh);display:grid;grid-template-columns:210px minmax(0,1fr);border:1px solid #e8ebf2;border-radius:12px;overflow:hidden;background:#f7f8fb}.wizard-nav{padding:18px 14px;display:flex;flex-direction:column;border-right:1px solid #e8ebf2;background:#fbfbfd}.step-button{width:100%;display:flex;gap:11px;align-items:flex-start;padding:11px 10px;border:0;border-radius:9px;text-align:left;color:#7b8495;background:transparent;cursor:pointer}.step-button:hover{background:#f1f5f4}.step-button.active{color:#07845f;background:#e9f9f3}.step-button.done{color:#374151}.step-index{width:25px;height:25px;display:grid;place-items:center;flex:0 0 auto;border:1px solid #d7dbe5;border-radius:50%;font-size:11px;font-weight:700;background:#fff}.step-button.active .step-index{color:#fff;border-color:#10b981;background:#10b981}.step-button.done .step-index{color:#fff;border-color:#14b8a6;background:#14b8a6}.step-button strong,.step-button small{display:block}.step-button strong{font-size:13px}.step-button small{margin-top:4px;font-size:11px;color:#9aa2b1}.task-summary{margin-top:auto;padding:13px;border:1px solid #e6e8ef;border-radius:10px;background:#fff}.summary-label{display:block;margin-bottom:7px;color:#10a779;font-size:10px;letter-spacing:1px}.task-summary>strong{display:block;overflow:hidden;color:#293247;font-size:12px;text-overflow:ellipsis;white-space:nowrap}.summary-flow{display:flex;align-items:center;gap:4px;margin:10px 0;color:#687287;font-size:10px}.summary-flow span{min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.summary-flow i{color:#10b981;font-style:normal}.summary-meta{display:flex;flex-wrap:wrap;gap:5px}.summary-meta span{padding:3px 6px;color:#6b7280;background:#f4f5f8;border-radius:4px;font-size:9px}.wizard-content{min-width:0;padding:27px 30px;overflow-y:auto;background:#fff}.step-panel{animation:panel-in .2s ease}@keyframes panel-in{from{opacity:0;transform:translateY(4px)}to{opacity:1;transform:none}}.section-heading{display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:21px}.section-heading h3{margin:3px 0 0;color:#20283a;font-size:20px}.section-heading p{max-width:390px;margin:17px 0 0;color:#8a94a6;font-size:12px;line-height:1.6}.eyebrow{color:#0e9f75;font-size:10px;font-weight:800;letter-spacing:1.6px}.form-card,.endpoint-card,.settings-card,.sql-card,.check-card,.preview-card{padding:20px;border:1px solid #e5e8ef;border-radius:11px;background:#fff}.field-label{display:block;margin-bottom:7px;color:#505a6d;font-size:12px;font-weight:600}.field-label em,.field-label b,.block-title b{color:#ef4444;font-style:normal}.field-gap{margin-top:17px}.mode-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-top:16px}.mode-card{position:relative;min-height:98px;display:flex;align-items:center;gap:14px;padding:17px;border:1px solid #d8eee6;border-radius:11px;color:#485267;text-align:left;background:#f7fffb}.mode-card.disabled{border-color:#e5e7eb;background:#f8f9fb;opacity:.65}.mode-icon{width:42px;height:42px;display:grid;place-items:center;flex:0 0 auto;border-radius:10px;color:#07845f;background:#ddf8ec;font-weight:800}.mode-icon.stream{color:#7c3aed;background:#f1eaff}.mode-card strong,.mode-card small{display:block}.mode-card small{margin-top:5px;color:#8992a3;font-size:11px}.mode-card i{margin-left:auto;color:#079669;font-size:11px;font-style:normal}.info-banner,.query-hint,.strategy-tip{padding:10px 12px;border-radius:8px;color:#64748b;background:#f5f7fb;font-size:11px;line-height:1.6}.info-banner{margin-bottom:14px;color:#925f13;background:#fff8e8}.endpoint-grid{display:grid;grid-template-columns:minmax(0,1fr) 64px minmax(0,1fr)}.endpoint-card{min-width:0}.source-card{border-top:3px solid #3b82f6}.target-card{border-top:3px solid #10b981}.endpoint-head{display:flex;align-items:center;gap:10px;margin-bottom:18px;padding-bottom:14px;border-bottom:1px solid #edf0f5}.endpoint-head>span{width:29px;height:29px;display:grid;place-items:center;color:#fff;border-radius:8px;background:#3b82f6;font-size:10px;font-weight:700}.target-card .endpoint-head>span{background:#10b981}.endpoint-head strong,.endpoint-head small{display:block}.endpoint-head small{margin-top:2px;color:#a0a7b5;font-size:10px}.endpoint-head em{margin-left:auto;padding:3px 7px;color:#9aa2b1;background:#f3f4f6;border-radius:999px;font-size:9px;font-style:normal}.endpoint-head em.ready{color:#07845f;background:#e8f8f2}.flow-direction{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:7px;color:#b0b6c2;font-size:9px}.flow-direction i{width:31px;height:31px;display:grid;place-items:center;color:#10b981;border:1px solid #bcebd8;border-radius:50%;background:#ecfbf5;font-size:16px;font-style:normal}.query-hint{margin-top:18px}.header-actions{display:flex;gap:8px;margin-top:8px}.sql-card{margin-bottom:15px}.block-title{display:flex;justify-content:space-between;margin-bottom:14px;color:#2d3748;font-size:13px;font-weight:700}.block-title small{color:#9aa2b1;font-size:10px;font-weight:400}.advanced-grid{display:grid;grid-template-columns:1.5fr 1fr .65fr;gap:12px;margin-bottom:15px;padding:15px;border:1px solid #e8ebf2;border-radius:10px;background:#fafbfc}.advanced-grid small{display:block;margin-top:6px;color:#9aa2b1;font-size:10px}.mapping-table{overflow:hidden;border:1px solid #e5e8ef;border-radius:10px}.mapping-head,.mapping-row{display:grid;grid-template-columns:minmax(0,1fr) 64px minmax(0,1fr) 58px;align-items:center;gap:10px}.mapping-head{padding:10px 14px;color:#8a94a6;background:#f7f8fb;font-size:10px}.mapping-head span:nth-child(2){text-align:center}.mapping-list{max-height:260px;overflow-y:auto}.mapping-row{padding:9px 14px;border-top:1px solid #f0f2f6}.map-arrow{text-align:center;color:#10b981}.mapping-empty{padding:42px;text-align:center;color:#64748b}.mapping-empty p{margin:7px 0 0;color:#9aa2b1;font-size:11px}.mapping-footer{display:flex;gap:18px;padding:11px 4px;color:#8a94a6;font-size:11px}.mapping-footer strong{margin-left:auto;color:#07845f}.settings-grid{display:grid;grid-template-columns:1fr 1fr;gap:15px}.number-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px 12px}.number-grid>div:last-child{grid-column:1/-1}.strategy-tip{margin-top:14px}.custom-process{margin-top:15px}.review-grid{display:grid;grid-template-columns:270px minmax(0,1fr);gap:15px}.check-row{display:flex;align-items:flex-start;gap:10px;padding:11px 0;border-top:1px solid #eef0f4}.check-row i{width:21px;height:21px;display:grid;place-items:center;flex:none;color:#d97706;background:#fff5df;border-radius:50%;font-size:11px;font-style:normal;font-weight:800}.check-row.ok i{color:#07845f;background:#e8f8f2}.check-row strong,.check-row small{display:block}.check-row strong{color:#374151;font-size:12px}.check-row small{margin-top:3px;color:#9aa2b1;font-size:10px;line-height:1.45}.preview-card{min-width:0;background:#15202b}.preview-card .block-title{color:#e5eef6}.preview-card pre{max-height:455px;margin:0;overflow:auto;color:#c6f6df;font:11px/1.65 ui-monospace,SFMono-Regular,Menlo,monospace;white-space:pre-wrap}.dialog-footer{display:flex;align-items:center;gap:10px}.dialog-footer>span{flex:1}:deep(.el-select),:deep(.el-input-number){width:100%}
@media(max-width:900px){.wizard-shell{grid-template-columns:150px minmax(0,1fr)}.wizard-content{padding:20px}.endpoint-grid,.settings-grid,.review-grid,.advanced-grid{grid-template-columns:1fr}.flow-direction{padding:10px}.flow-direction span{display:none}.number-grid{grid-template-columns:1fr 1fr}}
.preview-command{display:flex;align-items:center;gap:10px;margin-bottom:12px;padding:9px 12px;background:#f5f7fb;border-radius:8px}.preview-command span{flex:0 0 auto;color:#505a6d;font-size:12px;font-weight:600}.preview-command code{min-width:0;overflow:hidden;color:#0e9f75;font-size:11px;text-overflow:ellipsis;white-space:nowrap}.preview-json{max-height:60vh;overflow:auto;margin:0;padding:14px;color:#d4d4d4;background:#1e1e1e;border-radius:8px;font-size:12px;line-height:1.6;white-space:pre-wrap;word-break:break-all}
</style>
