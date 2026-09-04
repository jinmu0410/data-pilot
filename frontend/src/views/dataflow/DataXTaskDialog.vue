<template>
  <el-dialog
    :model-value="modelValue"
    width="min(1180px, calc(100vw - 40px))"
    top="4vh"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
    :before-close="cancel"
    class="datax-task-dialog"
    @closed="emit('closed')"
  >
    <template #header>
      <div class="dialog-heading">
        <span class="engine-logo">DX</span>
        <div>
          <div class="dialog-title">DataX 同步任务</div>
          <div class="dialog-subtitle">配置数据读取、字段映射和写入策略</div>
        </div>
        <span class="engine-tag">离线同步</span>
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
          <span class="summary-label">任务概览</span>
          <strong>{{ config.name || '未命名任务' }}</strong>
          <div class="summary-flow">
            <span>{{ sourceDatasource?.name || '源端待选择' }}</span>
            <i>→</i>
            <span>{{ targetDatasource?.name || '目标端待选择' }}</span>
          </div>
          <div class="summary-meta">
            <span>{{ config.readMode === 'query' ? '自定义 SQL' : '整表读取' }}</span>
            <span>{{ completeMappings.length }} 个字段</span>
            <span>{{ config.channel || 3 }} 并发</span>
          </div>
        </div>
      </aside>

      <main class="wizard-content">
        <section v-show="step === 0" class="step-panel">
          <div class="section-heading">
            <div><span class="eyebrow">BASIC</span><h3>定义任务</h3></div>
            <p>先描述这条同步链路，便于后续检索与运维。</p>
          </div>

          <div class="form-card">
            <label class="field-label">任务名称 <em>*</em></label>
            <el-input v-model="config.name" maxlength="80" show-word-limit placeholder="例如：订单明细每日同步" size="large" />
            <label class="field-label field-gap">任务描述</label>
            <el-input
              v-model="config.description"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              placeholder="说明数据用途、同步范围或负责人（可选）"
            />
          </div>

          <div class="mode-grid">
            <button type="button" class="mode-card" :class="{ selected: config.readMode === 'table' }" @click="setReadMode('table')">
              <span class="mode-icon table-icon">▦</span>
              <span><strong>整表同步</strong><small>选择源表，自动获取字段并生成读取 SQL</small></span>
              <i class="mode-check">✓</i>
            </button>
            <button type="button" class="mode-card" :class="{ selected: config.readMode === 'query' }" @click="setReadMode('query')">
              <span class="mode-icon sql-icon">SQL</span>
              <span><strong>自定义 SQL</strong><small>通过查询、过滤和别名组织需要同步的数据</small></span>
              <i class="mode-check">✓</i>
            </button>
          </div>
        </section>

        <section v-show="step === 1" class="step-panel">
          <div class="section-heading">
            <div><span class="eyebrow">CONNECTION</span><h3>选择数据端点</h3></div>
            <p>按数据源、库和表逐级选择，元数据会自动加载。</p>
          </div>

          <div class="endpoint-grid">
            <article class="endpoint-card source-card">
              <div class="endpoint-head">
                <span class="endpoint-badge">01</span>
                <div><strong>数据来源</strong><small>Reader</small></div>
                <span class="endpoint-status" :class="{ ready: sourceDatasource }">{{ sourceDatasource ? '已选择' : '待配置' }}</span>
              </div>
              <label class="field-label">源数据源 <em>*</em></label>
              <el-select
                v-model="config.sourceDataSourceCode"
                placeholder="请选择源数据源"
                filterable
                size="large"
                @change="onDatasourceChange('source')"
              >
                <el-option v-for="ds in datasources" :key="ds.code" :label="`${ds.name} · ${ds.type}`" :value="ds.code" />
              </el-select>
              <template v-if="config.readMode === 'table'">
                <label class="field-label field-gap">源库 / Schema</label>
                <el-select
                  v-model="config.sourceSchema"
                  placeholder="请选择源库"
                  filterable
                  :loading="loading.sourceTree"
                  @change="onSchemaChange('source')"
                >
                  <el-option v-for="item in sourceTree" :key="item.key" :label="item.label" :value="item.key" />
                </el-select>
                <label class="field-label field-gap">源表 <em>*</em></label>
                <el-select
                  v-model="config.sourceTable"
                  placeholder="请选择源表"
                  filterable
                  :disabled="!config.sourceSchema"
                  :loading="loading.sourceColumns"
                  @change="onTableChange('source')"
                >
                  <el-option v-for="item in sourceTables" :key="item.key" :label="item.label" :value="item.key" />
                </el-select>
              </template>
              <div v-else class="query-mode-hint">自定义 SQL 模式无需绑定源表，可在下一步编写查询。</div>
            </article>

            <div class="flow-direction"><span>数据流向</span><i>→</i></div>

            <article class="endpoint-card target-card">
              <div class="endpoint-head">
                <span class="endpoint-badge">02</span>
                <div><strong>写入目标</strong><small>Writer</small></div>
                <span class="endpoint-status" :class="{ ready: targetDatasource }">{{ targetDatasource ? '已选择' : '待配置' }}</span>
              </div>
              <label class="field-label">目标数据源 <em>*</em></label>
              <el-select
                v-model="config.targetDataSourceCode"
                placeholder="请选择目标数据源"
                filterable
                size="large"
                @change="onDatasourceChange('target')"
              >
                <el-option v-for="ds in datasources" :key="ds.code" :label="`${ds.name} · ${ds.type}`" :value="ds.code" />
              </el-select>
              <label class="field-label field-gap">目标库 / Schema</label>
              <el-select
                v-model="config.targetSchema"
                placeholder="请选择目标库"
                filterable
                :loading="loading.targetTree"
                @change="onSchemaChange('target')"
              >
                <el-option v-for="item in targetTree" :key="item.key" :label="item.label" :value="item.key" />
              </el-select>
              <label class="field-label field-gap">目标表 <em>*</em></label>
              <el-select
                v-model="config.targetTable"
                placeholder="请选择目标表"
                filterable
                :disabled="!config.targetSchema"
                :loading="loading.targetColumns"
                @change="onTableChange('target')"
              >
                <el-option v-for="item in targetTables" :key="item.key" :label="item.label" :value="item.key" />
              </el-select>
            </article>
          </div>
        </section>

        <section v-show="step === 2" class="step-panel mapping-panel">
          <div class="section-heading mapping-heading">
            <div><span class="eyebrow">MAPPING</span><h3>配置读取与字段映射</h3></div>
            <div class="mapping-actions">
              <el-button :disabled="!sourceColumns.length || !targetColumns.length" @click="autoMap">智能匹配</el-button>
              <el-button :disabled="!mappingRows.length" @click="clearMappings">清空</el-button>
              <el-button type="primary" plain @click="addMapping">添加映射</el-button>
            </div>
          </div>

          <div v-if="config.readMode === 'query'" class="sql-block">
            <div class="block-title"><span>读取 SQL <em>*</em></span><small>建议使用 AS 别名与目标字段保持一致</small></div>
            <SqlEditor v-model="config.sqlText" height="190px" />
          </div>
          <div v-else class="generated-query">
            <div><strong>读取语句已自动生成</strong><span>{{ sourceQualifiedName }}</span></div>
            <code>{{ config.sqlText || '选择源表后自动生成' }}</code>
          </div>

          <div class="mapping-table-wrap">
            <div class="mapping-table-head">
              <span>源字段</span><span class="map-center">映射</span><span>目标字段</span><span></span>
            </div>
            <div v-if="mappingRows.length" class="mapping-list">
              <div v-for="(row, index) in mappingRows" :key="index" class="mapping-item">
                <el-select
                  v-model="row.source"
                  placeholder="选择或输入源字段"
                  filterable
                  :allow-create="config.readMode === 'query'"
                  :default-first-option="config.readMode === 'query'"
                  @change="regenerateSql"
                >
                  <el-option v-for="column in sourceColumns" :key="column.name" :label="columnLabel(column)" :value="column.name" />
                </el-select>
                <span class="map-link"><i></i>→</span>
                <el-select v-model="row.target" placeholder="选择目标字段" filterable @change="regenerateSql">
                  <el-option v-for="column in targetColumns" :key="column.name" :label="columnLabel(column)" :value="column.name" />
                </el-select>
                <el-button link type="danger" @click="removeMapping(index)">移除</el-button>
              </div>
            </div>
            <div v-else class="mapping-empty">
              <span>⇄</span>
              <strong>还没有字段映射</strong>
              <p>选择源表和目标表后点击“智能匹配”，或手动添加映射。</p>
            </div>
          </div>
          <div class="mapping-footer">
            <span><i class="dot ok"></i>已映射 {{ completeMappings.length }} 个</span>
            <span><i class="dot"></i>源字段 {{ sourceColumns.length }} 个</span>
            <span><i class="dot"></i>目标字段 {{ targetColumns.length }} 个</span>
          </div>
        </section>

        <section v-show="step === 3" class="step-panel">
          <div class="section-heading">
            <div><span class="eyebrow">RUNTIME</span><h3>写入与运行策略</h3></div>
            <p>使用稳妥默认值即可运行，也可以按数据规模细调。</p>
          </div>

          <div class="settings-grid">
            <article class="settings-card">
              <div class="block-title"><span>写入策略</span><small>控制目标端写入行为</small></div>
              <label class="field-label">写入模式</label>
              <el-select v-model="config.writeMode" size="large">
                <el-option v-for="mode in writeModeOptions" :key="mode.value" :label="mode.label" :value="mode.value" />
              </el-select>
              <div v-if="!targetSupportsConflictMode" class="performance-tip">
                {{ targetDatasource?.type || '当前目标端' }} 的 DataX Writer 仅开放 Insert 模式。
              </div>
              <div v-if="requiresConflictKey" class="conflict-card" :class="{ invalid: !conflictReady }">
                <div class="conflict-title">
                  <span>冲突判定约束</span>
                  <em>{{ conflictReady ? '映射完整' : '需要处理' }}</em>
                </div>
                <template v-if="conflictConstraints.length">
                  <div v-for="constraint in conflictConstraints" :key="constraint.name" class="constraint-row">
                    <span>{{ constraint.label }}</span>
                    <div>
                      <el-tag
                        v-for="column in constraint.columns"
                        :key="column"
                        size="small"
                        :type="mappedTargetColumns.has(column) ? 'success' : 'danger'"
                        effect="plain"
                      >{{ column }}</el-tag>
                    </div>
                  </div>
                  <p>{{ targetDatasource?.type }} 根据主键或唯一索引自动判断冲突；至少保证一组约束字段全部参与映射。</p>
                </template>
                <p v-else>目标表没有主键或唯一索引，{{ config.writeMode }} 无法判断需要更新或替换的记录。</p>
              </div>
              <div class="number-row">
                <div><label class="field-label">读取批次</label><el-input-number v-model="config.fetchSize" :min="1" :controls="false" placeholder="自动" /></div>
                <div><label class="field-label">写入批次</label><el-input-number v-model="config.batchSize" :min="1" :controls="false" placeholder="自动" /></div>
              </div>
            </article>

            <article class="settings-card">
              <div class="block-title"><span>性能与保护</span><small>控制并行度、限速与超时</small></div>
              <div class="number-row three">
                <div><label class="field-label">并发通道</label><el-input-number v-model="config.channel" :min="1" :max="64" :controls="false" /></div>
                <div><label class="field-label">超时（秒）</label><el-input-number v-model="config.timeout" :min="1" :max="86400" :controls="false" /></div>
                <div><label class="field-label">记录/秒</label><el-input-number v-model="config.jobSpeedRecord" :min="1" :controls="false" placeholder="不限" /></div>
              </div>
              <label class="field-label field-gap">字节/秒</label>
              <el-input-number v-model="config.jobSpeedByte" :min="1" :controls="false" placeholder="不限制传输字节速率" class="full-number" />
              <div class="performance-tip">建议从 3 个并发通道开始，根据源库与目标库负载逐步提升。</div>
            </article>
          </div>

          <el-collapse class="sql-collapse">
            <el-collapse-item name="pre">
              <template #title><strong>前置 SQL</strong><span>同步开始前在目标库执行</span></template>
              <SqlEditor v-model="config.preSql" height="140px" />
            </el-collapse-item>
            <el-collapse-item name="post">
              <template #title><strong>后置 SQL</strong><span>同步成功后在目标库执行</span></template>
              <SqlEditor v-model="config.postSql" height="140px" />
            </el-collapse-item>
          </el-collapse>
        </section>
      </main>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="danger" link @click="emit('delete')">删除节点</el-button>
        <span class="footer-spacer"></span>
        <el-button @click="cancelWithoutDone">取消</el-button>
        <el-button v-if="step > 0" @click="step--">上一步</el-button>
        <el-button v-if="step < steps.length - 1" type="primary" @click="next">下一步</el-button>
        <el-button v-else type="primary" @click="apply">应用配置</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import SqlEditor from '../../components/SqlEditor.vue'
import { listSchemaTable, tableDetail, type SchemaTableMap, type TableColumn, type TableIndex } from '../../api/datasource'
import type { NodeConfig, FieldMappingRow } from './nodes'

interface DataSourceOption {
  id: number
  code: string
  name: string
  type: string
}

const props = defineProps<{
  modelValue: boolean
  config: NodeConfig
  datasources: DataSourceOption[]
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'delete'): void
  (event: 'closed'): void
}>()

const steps = [
  { title: '基础信息', description: '名称与读取方式' },
  { title: '数据端点', description: '来源与写入目标' },
  { title: '字段映射', description: '读取 SQL 与列映射' },
  { title: '运行策略', description: '性能、限流与 SQL' }
]

const step = ref(0)
const snapshot = ref('')
const sourceTree = ref<SchemaTableMap[]>([])
const targetTree = ref<SchemaTableMap[]>([])
const sourceColumns = ref<TableColumn[]>([])
const targetColumns = ref<TableColumn[]>([])
const targetIndexes = ref<TableIndex[]>([])
const loading = reactive({ sourceTree: false, targetTree: false, sourceColumns: false, targetColumns: false })

const sourceDatasource = computed(() => props.datasources.find((item) => item.code === props.config.sourceDataSourceCode))
const targetDatasource = computed(() => props.datasources.find((item) => item.code === props.config.targetDataSourceCode))
const targetSupportsConflictMode = computed(() => ['mysql', 'tidb'].includes(targetDatasource.value?.type?.toLowerCase() ?? ''))
const writeModeOptions = computed(() => targetSupportsConflictMode.value
  ? [
      { label: 'Insert · 直接插入，冲突时报错', value: 'insert' },
      { label: 'Replace · 按主键/唯一键替换整行', value: 'replace' },
      { label: 'Update · 冲突时更新映射字段', value: 'update' }
    ]
  : [{ label: 'Insert · 直接插入', value: 'insert' }]
)
const sourceTables = computed(() => sourceTree.value.find((item) => item.key === props.config.sourceSchema)?.children ?? [])
const targetTables = computed(() => targetTree.value.find((item) => item.key === props.config.targetSchema)?.children ?? [])
const mappingRows = computed<FieldMappingRow[]>(() => {
  if (!Array.isArray(props.config.fieldMapping)) props.config.fieldMapping = []
  return props.config.fieldMapping
})
const completeMappings = computed(() => mappingRows.value.filter((item) => item.source && item.target))
const mappedTargetColumns = computed(() => new Set(completeMappings.value.map((item) => item.target)))
const requiresConflictKey = computed(() => targetSupportsConflictMode.value && ['replace', 'update'].includes(props.config.writeMode))
const conflictConstraints = computed(() => {
  const result: { name: string; label: string; columns: string[] }[] = []
  const primaryColumns = targetColumns.value.filter((column) => column.primaryKey).map((column) => column.name)
  if (primaryColumns.length) result.push({ name: 'PRIMARY', label: '主键', columns: primaryColumns })
  for (const index of targetIndexes.value.filter((item) => item.unique && item.columns?.length)) {
    if (index.columns.join('\u0000') === primaryColumns.join('\u0000')) continue
    result.push({ name: index.name, label: `唯一索引 ${index.name}`, columns: index.columns })
  }
  return result
})
const conflictReady = computed(() => conflictConstraints.value.some((constraint) =>
  constraint.columns.every((column) => mappedTargetColumns.value.has(column))
))
const sourceQualifiedName = computed(() => [props.config.sourceSchema, props.config.sourceTable].filter(Boolean).join('.') || '尚未选择源表')

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return
    snapshot.value = JSON.stringify(props.config)
    initializeDefaults()
    step.value = 0
    await Promise.all([loadTree('source'), loadTree('target')])
    await Promise.all([loadTableColumns('source'), loadTableColumns('target')])
  },
  { immediate: true }
)

function initializeDefaults() {
  props.config.readMode ||= props.config.sourceTable ? 'table' : 'table'
  props.config.description ||= ''
  props.config.fieldMapping ||= []
  props.config.sqlText ||= ''
  props.config.preSql ||= ''
  props.config.postSql ||= ''
  props.config.writeMode ||= 'insert'
  props.config.channel ||= 3
  props.config.timeout ||= 30
  normalizeWriteMode()
}

function normalizeWriteMode() {
  if (!writeModeOptions.value.some((mode) => mode.value === props.config.writeMode)) {
    props.config.writeMode = 'insert'
  }
}

function datasource(side: 'source' | 'target') {
  const code = side === 'source' ? props.config.sourceDataSourceCode : props.config.targetDataSourceCode
  return props.datasources.find((item) => item.code === code)
}

async function loadTree(side: 'source' | 'target') {
  const selected = datasource(side)
  if (!selected) return
  const tree = side === 'source' ? sourceTree : targetTree
  const key = side === 'source' ? 'sourceTree' : 'targetTree'
  loading[key] = true
  try {
    tree.value = await listSchemaTable(selected.id)
  } catch {
    tree.value = []
  } finally {
    loading[key] = false
  }
}

async function loadTableColumns(side: 'source' | 'target') {
  const selected = datasource(side)
  const schema = side === 'source' ? props.config.sourceSchema : props.config.targetSchema
  const table = side === 'source' ? props.config.sourceTable : props.config.targetTable
  if (!selected || !schema || !table) return
  const columns = side === 'source' ? sourceColumns : targetColumns
  const key = side === 'source' ? 'sourceColumns' : 'targetColumns'
  loading[key] = true
  try {
    const detail = await tableDetail(selected.id, schema, table)
    columns.value = detail.columns ?? []
    if (side === 'target') targetIndexes.value = detail.indexes ?? []
  } catch {
    columns.value = []
    if (side === 'target') targetIndexes.value = []
  } finally {
    loading[key] = false
  }
}

async function onDatasourceChange(side: 'source' | 'target') {
  if (side === 'source') {
    props.config.sourceSchema = ''
    props.config.sourceTable = ''
    sourceTree.value = []
    sourceColumns.value = []
  } else {
    props.config.targetSchema = ''
    props.config.targetTable = ''
    targetTree.value = []
    targetColumns.value = []
    targetIndexes.value = []
    normalizeWriteMode()
  }
  clearMappings()
  await loadTree(side)
  regenerateSql()
}

function onSchemaChange(side: 'source' | 'target') {
  if (side === 'source') {
    props.config.sourceTable = ''
    sourceColumns.value = []
  } else {
    props.config.targetTable = ''
    targetColumns.value = []
    targetIndexes.value = []
  }
  clearMappings()
  regenerateSql()
}

async function onTableChange(side: 'source' | 'target') {
  if (side === 'source') sourceColumns.value = []
  else {
    targetColumns.value = []
    targetIndexes.value = []
  }
  clearMappings()
  await loadTableColumns(side)
  if (sourceColumns.value.length && targetColumns.value.length) autoMap()
  regenerateSql()
}

function setReadMode(mode: 'table' | 'query') {
  if (props.config.readMode === mode) return
  props.config.readMode = mode
  if (mode === 'table') regenerateSql()
}

function addMapping() {
  mappingRows.value.push({ source: '', target: '' })
}

function removeMapping(index: number) {
  mappingRows.value.splice(index, 1)
  regenerateSql()
}

function clearMappings() {
  props.config.fieldMapping = []
  regenerateSql()
}

function autoMap() {
  const targetUnused = [...targetColumns.value]
  props.config.fieldMapping = sourceColumns.value.map((source, index) => {
    const exactIndex = targetUnused.findIndex((target) => target.name.toLowerCase() === source.name.toLowerCase())
    const target = exactIndex >= 0 ? targetUnused.splice(exactIndex, 1)[0] : targetUnused.shift() ?? targetColumns.value[index]
    return { source: source.name, target: target?.name ?? '' }
  }).filter((item) => item.target)
  regenerateSql()
}

function regenerateSql() {
  if (props.config.readMode !== 'table') return
  const table = sourceQualifiedName.value
  if (!props.config.sourceTable) {
    props.config.sqlText = ''
    return
  }
  const columns = completeMappings.value.length
    ? completeMappings.value.map((item) => item.source === item.target ? item.source : `${item.source} AS ${item.target}`).join(', ')
    : '*'
  props.config.sqlText = `SELECT ${columns} FROM ${table}`
}

function columnLabel(column: TableColumn) {
  const meta = [column.type, column.comment].filter(Boolean).join(' · ')
  return meta ? `${column.name} (${meta})` : column.name
}

function validate(targetStep: number, quiet = false) {
  let message = ''
  if (targetStep === 0 && !String(props.config.name || '').trim()) message = '请填写任务名称'
  if (targetStep === 1) {
    if (!props.config.sourceDataSourceCode) message = '请选择源数据源'
    else if (props.config.readMode === 'table' && !props.config.sourceTable) message = '请选择源表'
    else if (!props.config.targetDataSourceCode) message = '请选择目标数据源'
    else if (!props.config.targetTable) message = '请选择目标表'
  }
  if (targetStep === 2) {
    if (!String(props.config.sqlText || '').trim()) message = '请配置读取 SQL'
    else if (!completeMappings.value.length) message = '请至少配置一个完整的字段映射'
  }
  if (targetStep === 3) {
    if (!props.config.channel || props.config.channel < 1) message = '并发通道必须大于 0'
    else if (!props.config.timeout || props.config.timeout < 1) message = '超时时间必须大于 0'
    else if (requiresConflictKey.value && !conflictConstraints.value.length) message = '目标表需要主键或唯一索引才能使用更新/替换模式'
    else if (requiresConflictKey.value && !conflictReady.value) message = '请将至少一组主键或唯一索引字段完整加入字段映射'
  }
  if (message && !quiet) ElMessage.warning(message)
  return !message
}

function goToStep(target: number) {
  if (target <= step.value) {
    step.value = target
    return
  }
  for (let index = 0; index < target; index++) {
    if (!validate(index)) {
      step.value = index
      return
    }
  }
  step.value = target
}

function next() {
  if (!validate(step.value)) return
  step.value++
}

function apply() {
  for (let index = 0; index < steps.length; index++) {
    if (!validate(index)) {
      step.value = index
      return
    }
  }
  props.config.name = String(props.config.name).trim()
  props.config.fieldMapping = completeMappings.value.map((item) => ({ ...item }))
  regenerateSql()
  snapshot.value = JSON.stringify(props.config)
  emit('update:modelValue', false)
  ElMessage.success('DataX 节点配置已应用')
}

function restoreSnapshot() {
  const original = JSON.parse(snapshot.value || '{}')
  for (const key of Object.keys(props.config)) delete props.config[key]
  Object.assign(props.config, original)
}

function cancel(done: () => void) {
  restoreSnapshot()
  done()
}

function cancelWithoutDone() {
  restoreSnapshot()
  emit('update:modelValue', false)
}
</script>

<style scoped>
.dialog-heading { display: flex; align-items: center; gap: 12px; padding-right: 38px; }
.engine-logo { width: 40px; height: 40px; display: grid; place-items: center; border-radius: 11px; color: #fff; font-weight: 800; font-size: 13px; letter-spacing: -.5px; background: linear-gradient(145deg, #635bff, #8b5cf6); box-shadow: 0 8px 18px rgba(99, 91, 255, .25); }
.dialog-title { color: #172033; font-size: 17px; font-weight: 700; }
.dialog-subtitle { color: #8a94a6; font-size: 12px; margin-top: 3px; }
.engine-tag { margin-left: auto; padding: 5px 10px; color: #6758e8; background: #f0efff; border: 1px solid #dedaff; border-radius: 999px; font-size: 12px; }
.wizard-shell { min-height: 610px; display: grid; grid-template-columns: 205px minmax(0, 1fr); border: 1px solid #e8ebf2; border-radius: 12px; overflow: hidden; background: #f7f8fb; }
.wizard-nav { padding: 18px 14px; display: flex; flex-direction: column; border-right: 1px solid #e8ebf2; background: #fbfbfd; }
.step-button { width: 100%; display: flex; gap: 11px; align-items: flex-start; padding: 12px 10px; border: 0; border-radius: 9px; text-align: left; color: #7b8495; background: transparent; cursor: pointer; transition: .18s ease; }
.step-button:hover { background: #f2f3f8; }
.step-button.active { color: #5648dc; background: #eeecff; }
.step-button.done { color: #374151; }
.step-index { width: 25px; height: 25px; display: grid; place-items: center; flex: 0 0 auto; border: 1px solid #d7dbe5; border-radius: 50%; font-size: 11px; font-weight: 700; background: #fff; }
.step-button.active .step-index { color: #fff; border-color: #635bff; background: #635bff; }
.step-button.done .step-index { color: #fff; border-color: #10b981; background: #10b981; }
.step-button strong, .step-button small { display: block; }
.step-button strong { margin-top: 1px; font-size: 13px; }
.step-button small { margin-top: 4px; font-size: 11px; color: #9aa2b1; }
.task-summary { margin-top: auto; padding: 13px; border: 1px solid #e6e8ef; border-radius: 10px; background: #fff; }
.summary-label { display: block; margin-bottom: 7px; color: #9aa2b1; font-size: 10px; letter-spacing: 1px; }
.task-summary > strong { display: block; overflow: hidden; color: #293247; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.summary-flow { display: flex; align-items: center; gap: 4px; margin: 10px 0; color: #687287; font-size: 10px; }
.summary-flow span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.summary-flow i { color: #635bff; font-style: normal; }
.summary-meta { display: flex; flex-wrap: wrap; gap: 5px; }
.summary-meta span { padding: 3px 6px; color: #6b7280; background: #f4f5f8; border-radius: 4px; font-size: 9px; }
.wizard-content { min-width: 0; padding: 27px 30px; overflow-y: auto; background: #fff; }
.step-panel { animation: panel-in .2s ease; }
@keyframes panel-in { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
.section-heading { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 22px; }
.section-heading h3 { margin: 3px 0 0; color: #20283a; font-size: 20px; line-height: 1.25; }
.section-heading p { max-width: 350px; margin: 17px 0 0; color: #8a94a6; font-size: 12px; }
.eyebrow { color: #7669ef; font-size: 10px; font-weight: 800; letter-spacing: 1.6px; }
.form-card, .settings-card, .endpoint-card { padding: 20px; border: 1px solid #e5e8ef; border-radius: 11px; background: #fff; }
.field-label { display: block; margin-bottom: 7px; color: #505a6d; font-size: 12px; font-weight: 600; }
.field-label em { color: #f05252; font-style: normal; }
.field-gap { margin-top: 17px; }
.mode-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-top: 16px; }
.mode-card { position: relative; min-height: 100px; display: flex; align-items: center; gap: 14px; padding: 17px; border: 1px solid #e4e7ee; border-radius: 11px; color: #485267; text-align: left; background: #fff; cursor: pointer; transition: .18s ease; }
.mode-card:hover { border-color: #a8a1f5; transform: translateY(-1px); }
.mode-card.selected { border-color: #7468ee; background: #f8f7ff; box-shadow: 0 0 0 2px rgba(99, 91, 255, .08); }
.mode-icon { width: 42px; height: 42px; display: grid; place-items: center; flex: 0 0 auto; border-radius: 10px; font-weight: 800; }
.table-icon { color: #2563eb; background: #eaf2ff; font-size: 21px; }
.sql-icon { color: #7c3aed; background: #f1eaff; font-size: 11px; }
.mode-card strong, .mode-card small { display: block; }
.mode-card strong { color: #283247; font-size: 14px; }
.mode-card small { margin-top: 5px; color: #8992a3; font-size: 11px; line-height: 1.5; }
.mode-check { position: absolute; top: 9px; right: 10px; display: none; color: #635bff; font-style: normal; font-weight: 800; }
.mode-card.selected .mode-check { display: block; }
.endpoint-grid { display: grid; grid-template-columns: minmax(0, 1fr) 64px minmax(0, 1fr); align-items: stretch; }
.endpoint-card { min-width: 0; }
.source-card { border-top: 3px solid #5b8ff9; }
.target-card { border-top: 3px solid #8b5cf6; }
.endpoint-head { display: flex; align-items: center; gap: 10px; margin-bottom: 21px; padding-bottom: 15px; border-bottom: 1px solid #edf0f5; }
.endpoint-badge { width: 29px; height: 29px; display: grid; place-items: center; color: #fff; border-radius: 8px; background: #5b8ff9; font-size: 10px; font-weight: 700; }
.target-card .endpoint-badge { background: #8b5cf6; }
.endpoint-head strong, .endpoint-head small { display: block; }
.endpoint-head strong { color: #293247; font-size: 13px; }
.endpoint-head small { margin-top: 2px; color: #a0a7b5; font-size: 10px; }
.endpoint-status { margin-left: auto; padding: 3px 7px; color: #9aa2b1; background: #f3f4f6; border-radius: 999px; font-size: 9px; }
.endpoint-status.ready { color: #079669; background: #e8f8f2; }
.flow-direction { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 7px; color: #b0b6c2; font-size: 9px; }
.flow-direction i { width: 31px; height: 31px; display: grid; place-items: center; color: #635bff; border: 1px solid #dedaff; border-radius: 50%; background: #f3f1ff; font-size: 16px; font-style: normal; }
.query-mode-hint, .danger-tip, .performance-tip { margin-top: 18px; padding: 10px 12px; border-radius: 7px; font-size: 11px; line-height: 1.55; }
.query-mode-hint, .performance-tip { color: #64748b; background: #f5f7fb; }
.danger-tip { color: #b45309; background: #fff7e6; }
.conflict-card { margin-top: 13px; padding: 12px; border: 1px solid #b9ead9; border-radius: 8px; background: #f2fbf8; }
.conflict-card.invalid { border-color: #f4c7c7; background: #fff7f7; }
.conflict-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; color: #354052; font-size: 11px; font-weight: 700; }
.conflict-title em { color: #0f9f74; font-size: 10px; font-style: normal; }
.conflict-card.invalid .conflict-title em { color: #d9534f; }
.constraint-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; margin-top: 7px; color: #697386; font-size: 10px; }
.constraint-row > div { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 4px; }
.conflict-card p { margin: 8px 0 0; color: #7e8797; font-size: 10px; line-height: 1.5; }
.mapping-heading { align-items: center; }
.mapping-actions { display: flex; gap: 7px; }
.sql-block { margin-bottom: 16px; padding: 15px; border: 1px solid #e6e8ef; border-radius: 10px; }
.block-title { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 13px; }
.block-title span { color: #30384a; font-size: 13px; font-weight: 700; }
.block-title em { color: #f05252; font-style: normal; }
.block-title small { color: #929aab; font-size: 10px; }
.generated-query { display: flex; align-items: center; gap: 16px; margin-bottom: 14px; padding: 11px 13px; border: 1px solid #dfe8fc; border-radius: 9px; background: #f7faff; }
.generated-query > div { flex: 0 0 auto; }
.generated-query strong, .generated-query span { display: block; }
.generated-query strong { color: #31415d; font-size: 11px; }
.generated-query span { margin-top: 3px; color: #8190a7; font-size: 9px; }
.generated-query code { min-width: 0; overflow: hidden; color: #4f5ab7; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.mapping-table-wrap { border: 1px solid #e5e8ef; border-radius: 10px; overflow: hidden; }
.mapping-table-head, .mapping-item { display: grid; grid-template-columns: minmax(0, 1fr) 70px minmax(0, 1fr) 48px; align-items: center; gap: 9px; }
.mapping-table-head { padding: 9px 14px; color: #8790a1; background: #f7f8fa; font-size: 10px; font-weight: 700; text-transform: uppercase; }
.map-center { text-align: center; }
.mapping-list { max-height: 250px; overflow-y: auto; }
.mapping-item { padding: 8px 13px; border-top: 1px solid #f0f1f4; }
.map-link { display: flex; align-items: center; color: #7367e8; font-size: 13px; }
.map-link i { height: 1px; flex: 1; background: #d9d5fb; }
.mapping-empty { min-height: 180px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #a0a7b5; }
.mapping-empty > span { width: 40px; height: 40px; display: grid; place-items: center; margin-bottom: 8px; border-radius: 10px; background: #f1f2f6; font-size: 20px; }
.mapping-empty strong { color: #606a7e; font-size: 12px; }
.mapping-empty p { margin: 5px 0 0; font-size: 10px; }
.mapping-footer { display: flex; gap: 18px; margin-top: 10px; color: #8790a1; font-size: 10px; }
.dot { width: 6px; height: 6px; display: inline-block; margin-right: 5px; border-radius: 50%; background: #b8bec9; }
.dot.ok { background: #10b981; }
.settings-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.number-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 17px; }
.number-row.three { grid-template-columns: repeat(3, 1fr); margin-top: 0; }
.number-row > div { min-width: 0; }
.number-row :deep(.el-input-number), .full-number { width: 100%; }
.sql-collapse { margin-top: 16px; border: 1px solid #e5e8ef; border-radius: 10px; padding: 0 15px; }
.sql-collapse :deep(.el-collapse-item__header) { gap: 9px; }
.sql-collapse :deep(.el-collapse-item__header span) { color: #969ead; font-size: 10px; font-weight: 400; }
.dialog-footer { display: flex; align-items: center; width: 100%; }
.footer-spacer { flex: 1; }

@media (max-width: 900px) {
  .wizard-shell { grid-template-columns: 1fr; }
  .wizard-nav { flex-direction: row; gap: 4px; overflow-x: auto; border-right: 0; border-bottom: 1px solid #e8ebf2; }
  .step-button { min-width: 145px; }
  .task-summary { display: none; }
  .wizard-content { padding: 22px 18px; }
  .dialog-heading { padding-right: 28px; }
  .engine-tag { display: none; }
  .section-heading { display: block; }
  .section-heading p { margin: 7px 0 0; }
  .mapping-heading { display: flex; align-items: flex-start; gap: 12px; }
  .mapping-actions { flex-wrap: wrap; justify-content: flex-end; }
  .endpoint-grid { grid-template-columns: 1fr; gap: 12px; }
  .flow-direction { flex-direction: row; }
  .settings-grid, .mode-grid { grid-template-columns: 1fr; }
}
</style>

<style>
.datax-task-dialog .el-dialog__header { margin-right: 0; padding: 18px 22px 14px; border-bottom: 1px solid #edf0f4; }
.datax-task-dialog .el-dialog__body { padding: 16px 20px; }
.datax-task-dialog .el-dialog__footer { padding: 13px 20px 17px; border-top: 1px solid #edf0f4; }
.datax-task-dialog.el-dialog { border-radius: 14px; }
</style>
