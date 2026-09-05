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
          <div class="dialog-title">{{ t('seatunnel.title') }}</div>
          <div class="dialog-subtitle">{{ t('seatunnel.subtitle') }}</div>
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
          <strong>{{ config.name || t('task.unnamed') }}</strong>
          <div class="summary-flow">
            <span>{{ sourceDatasource?.name || t('datax.sourcePending') }}</span><i>→</i><span>{{ targetDatasource?.name || t('datax.targetPending') }}</span>
          </div>
          <div class="summary-meta">
            <span>{{ config.readMode === 'query' ? t('seatunnel.sqlRead') : t('datax.tableRead') }}</span>
            <span>{{ t('seatunnel.mappingCount', { n: completeMappings.length }) }}</span>
            <span>{{ t('seatunnel.parallelismCount', { n: config.parallelism || 1 }) }}</span>
          </div>
        </div>
      </aside>

      <main class="wizard-content">
        <section v-show="step === 0" class="step-panel">
          <div class="section-heading">
            <div><span class="eyebrow">BASIC</span><h3>{{ t('seatunnel.defineTask') }}</h3></div>
            <p>{{ t('seatunnel.defineTaskDesc') }}</p>
          </div>
          <div class="form-card">
            <label class="field-label">{{ t('task.name') }} <em>*</em></label>
            <el-input v-model="config.name" maxlength="80" show-word-limit size="large" :placeholder="t('seatunnel.namePlaceholder')" />
            <label class="field-label field-gap">{{ t('task.description') }}</label>
            <el-input v-model="config.description" type="textarea" :rows="3" maxlength="300" show-word-limit :placeholder="t('seatunnel.descriptionPlaceholder')" />
          </div>
          <div class="mode-grid">
            <button type="button" class="mode-card selected">
              <span class="mode-icon">B</span><span><strong>{{ t('seatunnel.batchSync') }}</strong><small>{{ t('seatunnel.batchSyncDesc') }}</small></span><i>✓</i>
            </button>
            <button type="button" class="mode-card disabled" disabled>
              <span class="mode-icon stream">S</span><span><strong>{{ t('seatunnel.realtimeCdc') }}</strong><small>{{ t('seatunnel.realtimeCdcDesc') }}</small></span><i>{{ t('seatunnel.planned') }}</i>
            </button>
          </div>
        </section>

        <section v-show="step === 1" class="step-panel">
          <div class="section-heading">
            <div><span class="eyebrow">ENDPOINT</span><h3>{{ t('seatunnel.selectEndpoint') }}</h3></div>
            <p>{{ t('seatunnel.selectEndpointDesc') }}</p>
          </div>
          <div v-if="unsupportedCount" class="info-banner">{{ t('seatunnel.unsupportedBanner', { n: unsupportedCount }) }}</div>
          <div class="endpoint-grid">
            <article class="endpoint-card source-card">
              <div class="endpoint-head"><span>01</span><div><strong>{{ t('datax.source') }}</strong><small>JDBC Source</small></div><em :class="{ ready: sourceDatasource }">{{ sourceDatasource ? t('datax.selected') : t('datax.pending') }}</em></div>
              <label class="field-label">{{ t('datax.sourceDatasource') }} <b>*</b></label>
              <el-select v-model="config.sourceDataSourceCode" filterable size="large" :placeholder="t('datax.sourceDatasourcePlaceholder')" @change="onDatasourceChange('source')">
                <el-option v-for="ds in supportedDatasources" :key="ds.code" :label="`${ds.name} · ${ds.type}`" :value="ds.code" />
              </el-select>
              <label class="field-label field-gap">{{ t('seatunnel.readMode') }}</label>
              <el-radio-group v-model="config.readMode" @change="onReadModeChange">
                <el-radio-button value="table">{{ t('datax.tableRead') }}</el-radio-button>
                <el-radio-button value="query">{{ t('datax.customSql') }}</el-radio-button>
              </el-radio-group>
              <template v-if="config.readMode === 'table'">
                <label class="field-label field-gap">{{ t('datax.sourceSchema') }} <b>*</b></label>
                <el-select v-model="config.sourceSchema" filterable :loading="loading.sourceTree" :placeholder="t('datax.sourceSchemaPlaceholder')" @change="onSchemaChange('source')">
                  <el-option v-for="item in sourceTree" :key="item.key" :label="item.label" :value="item.key" />
                </el-select>
                <label class="field-label field-gap">{{ t('datax.sourceTable') }} <b>*</b></label>
                <el-select v-model="config.sourceTable" filterable :disabled="!config.sourceSchema" :placeholder="t('datax.sourceTablePlaceholder')" @change="onTableChange('source')">
                  <el-option v-for="item in sourceTables" :key="item.key" :label="item.label" :value="item.key" />
                </el-select>
              </template>
              <div v-else class="query-hint">{{ t('seatunnel.queryHint') }}</div>
            </article>

            <div class="flow-direction"><span>{{ t('datax.dataFlow') }}</span><i>→</i></div>

            <article class="endpoint-card target-card">
              <div class="endpoint-head"><span>02</span><div><strong>{{ t('datax.target') }}</strong><small>JDBC Sink</small></div><em :class="{ ready: targetDatasource }">{{ targetDatasource ? t('datax.selected') : t('datax.pending') }}</em></div>
              <label class="field-label">{{ t('datax.targetDatasource') }} <b>*</b></label>
              <el-select v-model="config.targetDataSourceCode" filterable size="large" :placeholder="t('datax.targetDatasourcePlaceholder')" @change="onDatasourceChange('target')">
                <el-option v-for="ds in supportedDatasources" :key="ds.code" :label="`${ds.name} · ${ds.type}`" :value="ds.code" />
              </el-select>
              <label class="field-label field-gap">{{ t('datax.targetSchema') }} <b>*</b></label>
              <el-select v-model="config.targetSchema" filterable :loading="loading.targetTree" :placeholder="t('datax.targetSchemaPlaceholder')" @change="onSchemaChange('target')">
                <el-option v-for="item in targetTree" :key="item.key" :label="item.label" :value="item.key" />
              </el-select>
              <label class="field-label field-gap">{{ t('datax.targetTable') }} <b>*</b></label>
              <el-select v-model="config.targetTable" filterable :disabled="!config.targetSchema" :placeholder="t('datax.targetTablePlaceholder')" @change="onTableChange('target')">
                <el-option v-for="item in targetTables" :key="item.key" :label="item.label" :value="item.key" />
              </el-select>
            </article>
          </div>
        </section>

        <section v-show="step === 2" class="step-panel">
          <div class="section-heading">
            <div><span class="eyebrow">TRANSFORM</span><h3>{{ t('seatunnel.mappingTitle') }}</h3></div>
            <div class="header-actions"><el-button :disabled="!sourceColumns.length || !targetColumns.length" @click="autoMap">{{ t('datax.autoMap') }}</el-button><el-button type="primary" plain @click="addMapping">{{ t('datax.addMapping') }}</el-button></div>
          </div>
          <div v-if="config.readMode === 'query'" class="sql-card">
            <div class="block-title"><span>{{ t('datax.readSql') }} <b>*</b></span><small>{{ t('seatunnel.readSqlHint') }}</small></div>
            <SqlEditor v-model="config.sqlText" height="160px" />
          </div>
          <div class="advanced-grid">
            <div><label class="field-label">{{ t('seatunnel.whereCondition') }}</label><el-input v-model="config.whereCondition" :placeholder="t('seatunnel.whereConditionPlaceholder')" /><small>{{ t('seatunnel.whereConditionHint') }}</small></div>
            <div><label class="field-label">{{ t('seatunnel.partitionColumn') }}</label><el-select v-model="config.partitionColumn" filterable allow-create clearable :placeholder="t('seatunnel.partitionColumnPlaceholder')"><el-option v-for="column in sourceColumns" :key="column.name" :label="columnLabel(column)" :value="column.name" /></el-select><small>{{ t('seatunnel.partitionColumnHint') }}</small></div>
            <div><label class="field-label">{{ t('seatunnel.partitionNum') }}</label><el-input-number v-model="config.partitionNum" :min="1" :max="256" :controls="false" /><small>{{ t('seatunnel.partitionNumHint') }}</small></div>
          </div>
          <div class="mapping-table">
            <div class="mapping-head"><span>{{ t('datax.sourceField') }}</span><span>{{ t('seatunnel.transform') }}</span><span>{{ t('datax.targetField') }}</span><span></span></div>
            <div v-if="mappingRows.length" class="mapping-list">
              <div v-for="(row, index) in mappingRows" :key="index" class="mapping-row">
                <el-select v-model="row.source" filterable :allow-create="config.readMode === 'query'" :placeholder="t('datax.sourceFieldPlaceholder')">
                  <el-option v-for="column in sourceColumns" :key="column.name" :label="columnLabel(column)" :value="column.name" />
                </el-select>
                <span class="map-arrow">→</span>
                <el-select v-model="row.target" filterable :placeholder="t('datax.targetFieldPlaceholder')">
                  <el-option v-for="column in targetColumns" :key="column.name" :label="columnLabel(column)" :value="column.name" />
                </el-select>
                <el-button link type="danger" @click="removeMapping(index)">{{ t('datax.remove') }}</el-button>
              </div>
            </div>
            <div v-else class="mapping-empty"><strong>{{ t('seatunnel.noMapping') }}</strong><p>{{ t('seatunnel.noMappingHint') }}</p></div>
          </div>
          <div class="mapping-footer"><span>{{ t('seatunnel.sourceFieldCount', { n: sourceColumns.length }) }}</span><span>{{ t('seatunnel.targetFieldCount', { n: targetColumns.length }) }}</span><strong>{{ t('seatunnel.validMapping', { n: completeMappings.length }) }}</strong></div>
        </section>

        <section v-show="step === 3" class="step-panel">
          <div class="section-heading">
            <div><span class="eyebrow">SINK & RUNTIME</span><h3>{{ t('datax.runtimeTitle') }}</h3></div>
            <p>{{ t('seatunnel.runtimeDesc') }}</p>
          </div>
          <div class="settings-grid">
            <article class="settings-card">
              <div class="block-title"><span>{{ t('datax.writeStrategy') }}</span><small>{{ t('seatunnel.writeStrategyDesc') }}</small></div>
              <el-radio-group v-model="config.sinkWriteStrategy">
                <el-radio-button value="generated">{{ t('seatunnel.generatedSql') }}</el-radio-button>
                <el-radio-button value="custom">{{ t('datax.customSql') }}</el-radio-button>
              </el-radio-group>
              <template v-if="config.sinkWriteStrategy === 'generated'">
                <label class="field-label field-gap">{{ t('seatunnel.schemaSaveMode') }}</label>
                <el-select v-model="config.schemaSaveMode"><el-option v-for="item in schemaModes" :key="item.value" :label="item.label" :value="item.value" /></el-select>
                <label class="field-label field-gap">{{ t('seatunnel.dataSaveMode') }}</label>
                <el-select v-model="config.dataSaveMode"><el-option v-for="item in dataModes" :key="item.value" :label="item.label" :value="item.value" /></el-select>
                <label class="field-label field-gap">{{ t('seatunnel.primaryKeys') }}</label>
                <el-select v-model="config.primaryKeys" multiple filterable clearable :placeholder="t('seatunnel.primaryKeysPlaceholder')">
                  <el-option v-for="column in targetColumns" :key="column.name" :label="column.primaryKey ? t('seatunnel.primaryKeyLabel', { name: column.name }) : column.name" :value="column.name" />
                </el-select>
                <div class="strategy-tip">{{ t('seatunnel.primaryKeyHint') }}</div>
              </template>
              <template v-else>
                <label class="field-label field-gap">{{ t('seatunnel.sinkQuery') }} <b>*</b></label>
                <SqlEditor v-model="config.sinkQuery" height="150px" />
                <div class="strategy-tip">{{ t('seatunnel.sinkQueryHint') }}</div>
              </template>
            </article>
            <article class="settings-card">
              <div class="block-title"><span>{{ t('datax.performance') }}</span><small>{{ t('seatunnel.performanceDesc') }}</small></div>
              <div class="number-grid">
                <div><label class="field-label">{{ t('seatunnel.parallelism') }}</label><el-input-number v-model="config.parallelism" :min="1" :max="64" :controls="false" /></div>
                <div><label class="field-label">{{ t('datax.fetchSize') }}</label><el-input-number v-model="config.fetchSize" :min="0" :controls="false" :placeholder="t('seatunnel.driverDefault')" /></div>
                <div><label class="field-label">{{ t('datax.batchSize') }}</label><el-input-number v-model="config.batchSize" :min="1" :max="100000" :controls="false" /></div>
                <div><label class="field-label">{{ t('seatunnel.retryTimes') }}</label><el-input-number v-model="config.retryTimes" :min="0" :max="10" :controls="false" /></div>
                <div><label class="field-label">{{ t('seatunnel.timeout') }}</label><el-input-number v-model="config.timeout" :min="1" :max="86400" :controls="false" /></div>
              </div>
              <div class="strategy-tip">{{ t('seatunnel.parallelismHint') }}</div>
            </article>
          </div>
          <div v-if="config.dataSaveMode === 'CUSTOM_PROCESSING' && config.sinkWriteStrategy === 'generated'" class="sql-card custom-process">
            <div class="block-title"><span>{{ t('seatunnel.customSql') }} <b>*</b></span><small>{{ t('seatunnel.customSqlHint') }}</small></div>
            <SqlEditor v-model="config.customSql" height="130px" />
          </div>
        </section>

        <section v-show="step === 4" class="step-panel">
          <div class="section-heading">
            <div><span class="eyebrow">REVIEW</span><h3>{{ t('seatunnel.reviewTitle') }}</h3></div>
            <p>{{ t('seatunnel.reviewDesc') }}</p>
          </div>
          <div class="review-grid">
            <article class="check-card">
              <div class="block-title"><span>{{ t('seatunnel.readiness') }}</span><small>{{ readiness.filter(item => item.ok).length }}/{{ readiness.length }}</small></div>
              <div v-for="item in readiness" :key="item.label" class="check-row" :class="{ ok: item.ok }"><i>{{ item.ok ? '✓' : '!' }}</i><span><strong>{{ item.label }}</strong><small>{{ item.hint }}</small></span></div>
            </article>
            <article class="preview-card">
              <div class="block-title"><span>{{ t('seatunnel.hoconPreview') }}</span><small>{{ t('seatunnel.sensitiveReplaced') }}</small></div>
              <pre>{{ configPreview }}</pre>
            </article>
          </div>
        </section>
      </main>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="danger" link @click="emit('delete')">{{ t('task.deleteNode') }}</el-button>
        <el-button plain @click="preview">{{ t('datax.preview') }}</el-button><span></span>
        <el-button @click="cancelWithoutDone">{{ t('common.cancel') }}</el-button>
        <el-button v-if="step > 0" @click="step--">{{ t('task.prev') }}</el-button>
        <el-button v-if="step < steps.length - 1" type="primary" @click="next">{{ t('task.next') }}</el-button>
        <el-button v-else type="primary" @click="apply">{{ t('task.apply') }}</el-button>
      </div>
    </template>

    <el-dialog
      v-model="previewVisible"
      :title="t('datax.previewTitle')"
      width="760px"
      top="6vh"
      append-to-body
      :close-on-click-modal="false"
    >
      <div class="preview-command">
        <span>{{ t('datax.previewCommand') }}</span>
        <code>{{ previewCommand.join(' ') }}</code>
      </div>
      <pre class="preview-json">{{ previewContent }}</pre>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import SqlEditor from '../../components/SqlEditor.vue'
import { listSchemaTable, tableDetail, type SchemaTableMap, type TableColumn } from '../../api/datasource'
import { previewEngineConfig } from '../../api/dataflow'
import type { FieldMappingRow, NodeConfig } from './nodes'

interface DataSourceOption { id: number; code: string; name: string; type: string }
const props = defineProps<{ modelValue: boolean; config: NodeConfig; datasources: DataSourceOption[] }>()
const emit = defineEmits<{ (event: 'update:modelValue', value: boolean): void; (event: 'delete'): void; (event: 'closed'): void }>()

const { t } = useI18n()

const steps = computed(() => [
  { title: t('seatunnel.stepBasic'), description: t('seatunnel.stepBasicDesc') },
  { title: t('seatunnel.stepEndpoint'), description: t('seatunnel.stepEndpointDesc') },
  { title: t('seatunnel.stepTransform'), description: t('seatunnel.stepTransformDesc') },
  { title: t('seatunnel.stepRuntime'), description: t('seatunnel.stepRuntimeDesc') },
  { title: t('seatunnel.stepReview'), description: t('seatunnel.stepReviewDesc') }
])
const schemaModes = computed(() => [
  { label: t('seatunnel.schemaModeError'), value: 'ERROR_WHEN_SCHEMA_NOT_EXIST' },
  { label: t('seatunnel.schemaModeCreate'), value: 'CREATE_SCHEMA_WHEN_NOT_EXIST' },
  { label: t('seatunnel.schemaModeIgnore'), value: 'IGNORE' },
  { label: t('seatunnel.schemaModeRecreate'), value: 'RECREATE_SCHEMA' }
])
const dataModes = computed(() => [
  { label: t('seatunnel.dataModeAppend'), value: 'APPEND_DATA' },
  { label: t('seatunnel.dataModeDrop'), value: 'DROP_DATA' },
  { label: t('seatunnel.dataModeCustom'), value: 'CUSTOM_PROCESSING' },
  { label: t('seatunnel.dataModeError'), value: 'ERROR_WHEN_DATA_EXISTS' }
])
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
  { label: t('seatunnel.readinessEndpoint'), ok: !!sourceDatasource.value && !!targetDatasource.value, hint: t('seatunnel.readinessEndpointHint') },
  { label: t('seatunnel.readinessRead'), ok: props.config.readMode === 'table' ? !!props.config.sourceTable : isSelectSql(props.config.sqlText), hint: props.config.readMode === 'table' ? sourcePath.value || t('datax.noSourceTable') : t('seatunnel.readinessReadQueryHint') },
  { label: t('seatunnel.readinessMapping'), ok: completeMappings.value.length > 0, hint: t('seatunnel.readinessMappingHint', { n: completeMappings.value.length }) },
  { label: t('seatunnel.readinessPrimaryKey'), ok: !(props.config.primaryKeys || []).some((key: string) => !mappedTargets.value.has(key)), hint: t('seatunnel.readinessPrimaryKeyHint') },
  { label: t('seatunnel.readinessWrite'), ok: props.config.sinkWriteStrategy !== 'custom' || /\?/.test(props.config.sinkQuery || ''), hint: props.config.sinkWriteStrategy === 'custom' ? t('seatunnel.readinessWriteQueryHint') : `${props.config.schemaSaveMode} / ${props.config.dataSaveMode}` }
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
  if (index === 0 && !String(props.config.name || '').trim()) message = t('seatunnel.nameRequired')
  if (index === 1) {
    if (!sourceDatasource.value) message = t('seatunnel.sourceDatasourceRequired')
    else if (props.config.readMode === 'table' && (!props.config.sourceSchema || !props.config.sourceTable)) message = t('seatunnel.sourceTableRequired')
    else if (!targetDatasource.value) message = t('seatunnel.targetDatasourceRequired')
    else if (!props.config.targetSchema || !props.config.targetTable) message = t('seatunnel.targetTableRequired')
  }
  if (index === 2) {
    if (props.config.readMode === 'query' && !isSelectSql(props.config.sqlText)) message = t('seatunnel.readSqlSelectRequired')
    else if (props.config.whereCondition && !/^\s*where\s+/i.test(props.config.whereCondition)) message = t('seatunnel.wherePrefixRequired')
    else if (!completeMappings.value.length) message = t('seatunnel.mappingRequired')
    else if (new Set(completeMappings.value.map(item => item.target)).size !== completeMappings.value.length) message = t('seatunnel.targetDuplicateRequired')
  }
  if (index === 3) {
    const missingPrimary = (props.config.primaryKeys || []).find((key: string) => !mappedTargets.value.has(key))
    if (props.config.sinkWriteStrategy === 'custom' && !String(props.config.sinkQuery || '').trim()) message = t('seatunnel.sinkQueryRequired')
    else if (props.config.sinkWriteStrategy === 'custom' && !/\?/.test(props.config.sinkQuery)) message = t('seatunnel.sinkQueryPlaceholderRequired')
    else if (props.config.dataSaveMode === 'CUSTOM_PROCESSING' && !String(props.config.customSql || '').trim()) message = t('seatunnel.customSqlRequired')
    else if (missingPrimary) message = t('seatunnel.primaryKeyMissing', { key: missingPrimary })
    else if (!props.config.parallelism || props.config.parallelism < 1) message = t('seatunnel.parallelismPositive')
    else if (!props.config.batchSize || props.config.batchSize < 1) message = t('seatunnel.batchSizePositive')
    else if (!props.config.timeout || props.config.timeout < 1) message = t('seatunnel.timeoutPositive')
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
  for (let index = 0; index < steps.value.length - 1; index++) if (!validate(index)) { step.value = index; return }
  props.config.name = String(props.config.name).trim()
  props.config.fieldMapping = completeMappings.value.map(item => ({ ...item }))
  snapshot.value = JSON.stringify(props.config)
  emit('update:modelValue', false)
  ElMessage.success(t('seatunnel.applied'))
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
