<template>
  <div ref="editorRef" class="editor">
    <div class="editor-header">
      <div class="header-left">
        <el-button class="back-button" link :icon="ArrowLeft" @click="goBack">返回列表</el-button>
        <span class="header-divider"></span>
        <div class="flow-heading">
          <div class="flow-title-row">
            <span class="title">{{ detail?.name || '任务流画布' }}</span>
            <span class="flow-status" :class="`is-${flowStatusTone}`">{{ flowStatusText }}</span>
          </div>
          <div class="flow-subtitle">
            <span>{{ detail?.code || 'DAG WORKFLOW' }}</span>
            <span class="subtitle-dot"></span>
            <span>{{ graphStats.nodes }} 个任务 · {{ graphStats.edges }} 条依赖</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-button :icon="VideoPlay" plain :loading="running" @click="handleRun">运行</el-button>
        <el-button :icon="DocumentChecked" :loading="saving" @click="handleSave(true)">保存草稿</el-button>
        <el-button type="primary" :icon="Upload" :loading="saving" @click="handleSave(false)">发布</el-button>
      </div>
    </div>

    <div class="editor-body">
      <aside class="palette">
        <div class="panel-heading">
          <div>
            <div class="panel-title">任务组件</div>
            <div class="panel-subtitle">拖拽到画布创建任务</div>
          </div>
          <span class="panel-count">{{ NODE_TYPES.length }}</span>
        </div>
        <el-input v-model="paletteKeyword" class="palette-search" :prefix-icon="Search" placeholder="搜索任务类型" clearable />
        <div v-for="group in paletteGroups" :key="group.key" class="palette-group">
          <div class="palette-group-title">{{ group.label }}</div>
          <div
            v-for="t in group.items"
            :key="t.type"
            class="palette-item"
            draggable="true"
            @dragstart="onDragStart($event, t.type)"
            @click="addNode(t.type)"
          >
            <span class="palette-icon" :style="{ color: t.color, background: `${t.color}14` }">{{ t.icon }}</span>
            <span class="palette-item-meta">
              <span class="palette-item-name">{{ t.label }}</span>
              <span class="palette-item-type">{{ t.type }}</span>
            </span>
            <el-icon class="palette-drag"><Rank /></el-icon>
          </div>
        </div>
        <div v-if="!paletteGroups.length" class="palette-empty">没有匹配的任务组件</div>
        <div class="palette-hint">
          <span class="hint-icon">i</span>
          点击组件可快速添加，拖拽可指定位置
        </div>
      </aside>

      <div class="canvas-wrap" @dragover.prevent @drop.prevent="onDrop">
        <div class="canvas-toolbar">
          <div class="tool-group">
            <el-tooltip content="撤销 Ctrl/⌘ Z" placement="bottom"><button class="tool-button" aria-label="撤销" @click="undo"><el-icon><RefreshLeft /></el-icon></button></el-tooltip>
            <el-tooltip content="重做 Ctrl/⌘ Shift Z" placement="bottom"><button class="tool-button" aria-label="重做" @click="redo"><el-icon><RefreshRight /></el-icon></button></el-tooltip>
          </div>
          <span class="tool-divider"></span>
          <div class="tool-group">
            <el-tooltip content="缩小" placement="bottom"><button class="tool-button" aria-label="缩小画布" @click="zoomOut"><el-icon><ZoomOut /></el-icon></button></el-tooltip>
            <button class="zoom-value" title="重置为 100%" @click="resetZoom">{{ zoomPercent }}%</button>
            <el-tooltip content="放大" placement="bottom"><button class="tool-button" aria-label="放大画布" @click="zoomIn"><el-icon><ZoomIn /></el-icon></button></el-tooltip>
          </div>
          <span class="tool-divider"></span>
          <div class="tool-group">
            <el-tooltip content="适应画布" placement="bottom"><button class="tool-button" aria-label="适应画布" @click="fitView"><el-icon><Aim /></el-icon></button></el-tooltip>
            <el-tooltip content="自动布局" placement="bottom"><button class="tool-button tool-button-wide" @click="autoLayout"><el-icon><MagicStick /></el-icon><span>自动布局</span></button></el-tooltip>
            <el-tooltip content="全屏画布" placement="bottom"><button class="tool-button" aria-label="全屏画布" @click="toggleFullscreen"><el-icon><FullScreen /></el-icon></button></el-tooltip>
          </div>
        </div>
        <div v-if="graphStats.nodes === 0" class="canvas-empty">
          <div class="empty-visual"><el-icon><Connection /></el-icon></div>
          <div class="empty-title">开始编排你的任务流</div>
          <div class="empty-text">从左侧拖入任务组件，然后拖动节点锚点建立依赖关系</div>
          <el-button type="primary" plain @click="addNode('DATAX')">添加 DataX 同步任务</el-button>
        </div>
        <div ref="canvasRef" class="canvas"></div>
        <div class="canvas-statusbar">
          <span class="status-ready"><i></i>画布就绪</span>
          <span>任务 {{ graphStats.nodes }}</span>
          <span>依赖 {{ graphStats.edges }}</span>
          <span class="status-shortcut">Ctrl/⌘ S 保存 · Delete 删除 · 0 适应画布</span>
        </div>
      </div>

      <aside class="config">
        <div class="panel-heading config-heading">
          <div>
            <div class="panel-title">流程配置</div>
            <div class="panel-subtitle">调度与依赖关系</div>
          </div>
          <el-icon class="config-heading-icon"><Operation /></el-icon>
        </div>
        <template v-if="selectedEdgeId">
          <div class="section-label">依赖配置</div>
          <div class="edge-card">
            <div class="edge-node">
              <span class="edge-dot source"></span>
              <div><small>上游任务</small><strong>{{ selectedEdgeInfo.source }}</strong></div>
            </div>
            <div class="edge-line"><span></span><el-icon><ArrowRight /></el-icon></div>
            <div class="edge-node">
              <span class="edge-dot target"></span>
              <div><small>下游任务</small><strong>{{ selectedEdgeInfo.target }}</strong></div>
            </div>
          </div>
          <el-form label-position="top" class="edge-form">
            <el-form-item label="执行顺序（越小越先执行）">
              <el-input-number v-model="edgeOrder" :min="0" :step="1" style="width: 100%" />
            </el-form-item>
          </el-form>
          <el-button type="danger" plain :icon="Delete" style="width: 100%" @click="deleteSelectedEdge">
            删除连线
          </el-button>
          <div class="config-help">连线表示任务依赖。仅当所有上游任务完成后，下游任务才会进入执行队列。</div>
        </template>

        <div v-else class="config-schedule">
          <div class="overview-card">
            <div class="overview-icon"><el-icon><Connection /></el-icon></div>
            <div>
              <strong>{{ detail?.name || '未命名任务流' }}</strong>
              <span>{{ graphStats.nodes }} 个任务，{{ graphStats.edges }} 条依赖</span>
            </div>
          </div>
          <div class="metric-grid">
            <div class="metric-item"><strong>{{ graphStats.nodes }}</strong><span>任务节点</span></div>
            <div class="metric-item"><strong>{{ graphStats.edges }}</strong><span>依赖关系</span></div>
            <div class="metric-item"><strong>{{ configuredCount }}</strong><span>已配置</span></div>
          </div>
          <div class="section-label schedule-label">调度设置</div>
          <el-form label-position="top">
            <el-form-item label="Cron 表达式">
              <el-popover
                v-model:visible="cronPickerVisible"
                placement="left-start"
                width="380"
                trigger="manual"
                popper-class="cron-picker-popover"
              >
                <template #reference>
                  <div class="cron-row">
                    <el-input
                      v-model="flowCron"
                      placeholder="留空表示仅手动运行"
                      readonly
                      clearable
                      @click="openCronPicker"
                      @clear="clearFlowCron"
                    >
                      <template #prefix><el-icon><Calendar /></el-icon></template>
                    </el-input>
                    <el-button :icon="Calendar" @click="openCronPicker">选择</el-button>
                    <el-button :loading="flowCronResult.loading" @click="checkFlowCron">校验</el-button>
                  </div>
                </template>

                <div class="cron-picker">
                  <div class="cron-picker-head">
                    <div>
                      <strong>调度周期</strong>
                      <span>选择后自动生成 Spring Cron 表达式</span>
                    </div>
                    <el-button link @click="cronPickerVisible = false">关闭</el-button>
                  </div>

                  <div class="cron-preset-grid">
                    <button
                      v-for="preset in cronPresets"
                      :key="preset.expression"
                      class="cron-preset"
                      type="button"
                      @click="applyCronPreset(preset.expression)"
                    >
                      <strong>{{ preset.label }}</strong>
                      <span>{{ preset.expression }}</span>
                    </button>
                  </div>

                  <div class="cron-picker-section">
                    <div class="cron-field-label">自定义频率</div>
                    <el-radio-group v-model="cronDraft.mode" class="cron-mode-group">
                      <el-radio-button label="interval">按分钟</el-radio-button>
                      <el-radio-button label="daily">每天</el-radio-button>
                      <el-radio-button label="weekly">每周</el-radio-button>
                      <el-radio-button label="monthly">每月</el-radio-button>
                    </el-radio-group>
                  </div>

                  <div v-if="cronDraft.mode === 'interval'" class="cron-picker-section">
                    <div class="cron-field-label">间隔分钟</div>
                    <el-select v-model="cronDraft.intervalMinutes">
                      <el-option
                        v-for="item in cronIntervalOptions"
                        :key="item"
                        :label="`${item} 分钟`"
                        :value="item"
                      />
                    </el-select>
                  </div>

                  <div v-else class="cron-picker-section">
                    <div class="cron-field-label">执行时间</div>
                    <el-time-picker
                      v-model="cronDraft.time"
                      format="HH:mm"
                      value-format="HH:mm"
                      :clearable="false"
                      style="width: 100%"
                    />
                  </div>

                  <div v-if="cronDraft.mode === 'weekly'" class="cron-picker-section">
                    <div class="cron-field-label">执行星期</div>
                    <el-checkbox-group v-model="cronDraft.weekdays" class="cron-check-grid">
                      <el-checkbox-button
                        v-for="item in cronWeekdayOptions"
                        :key="item.value"
                        :label="item.value"
                      >
                        {{ item.label }}
                      </el-checkbox-button>
                    </el-checkbox-group>
                  </div>

                  <div v-if="cronDraft.mode === 'monthly'" class="cron-picker-section">
                    <div class="cron-field-label">每月日期</div>
                    <el-select v-model="cronDraft.monthDays" multiple collapse-tags collapse-tags-tooltip>
                      <el-option
                        v-for="day in cronMonthDayOptions"
                        :key="day"
                        :label="`${day} 号`"
                        :value="day"
                      />
                    </el-select>
                  </div>

                  <div class="cron-preview-card">
                    <span>生成表达式</span>
                    <strong>{{ cronPreview }}</strong>
                  </div>

                  <div class="cron-picker-actions">
                    <el-button @click="clearFlowCron">仅手动运行</el-button>
                    <el-button type="primary" @click="applyCronDraft">应用表达式</el-button>
                  </div>
                </div>
              </el-popover>
              <div v-if="flowCronResult.valid === true && flowCronResult.nexts.length" class="cron-nexts">
                <div class="cron-nexts-title">最近 5 次执行时间</div>
                <div v-for="(t, i) in flowCronResult.nexts" :key="i" class="cron-next">{{ t }}</div>
              </div>
              <div v-else-if="flowCronResult.valid === false" class="cron-invalid">{{ flowCronResult.message }}</div>
            </el-form-item>
          </el-form>
          <div class="config-tip">
            <div class="config-tip-title">画布操作</div>
            <div>单击节点配置参数，拖动锚点创建依赖，单击连线可设置执行顺序。</div>
          </div>
        </div>
      </aside>
    </div>

    <DataXTaskDialog
      v-if="selectedConfig && selectedNodeType?.type === 'DATAX'"
      v-model="nodeDialogVisible"
      :config="selectedConfig"
      :datasources="datasources"
      @delete="deleteSelectedNode"
      @closed="onNodeDialogClosed"
    />

    <SeaTunnelTaskDialog
      v-else-if="selectedConfig && selectedNodeType?.type === 'SEATUNNEL'"
      v-model="nodeDialogVisible"
      :config="selectedConfig"
      :datasources="datasources"
      @delete="deleteSelectedNode"
      @closed="onNodeDialogClosed"
    />

    <CodeTaskDialog
      v-else-if="selectedConfig && ['SQL', 'PYTHON', 'SHELL'].includes(selectedNodeType?.type ?? '')"
      v-model="nodeDialogVisible"
      :type="selectedNodeType?.type ?? 'SQL'"
      :config="selectedConfig"
      :datasources="datasources"
      @delete="deleteSelectedNode"
      @closed="onNodeDialogClosed"
    />

    <el-dialog
      v-else
      v-model="nodeDialogVisible"
      :title="`${selectedNodeType?.label ?? ''} 配置`"
      width="720px"
      top="5vh"
      :close-on-click-modal="false"
      destroy-on-close
      class="node-dialog"
      @closed="onNodeDialogClosed"
    >
      <el-form v-if="selectedConfig" label-position="top" class="config-form">
        <el-form-item label="名称" required>
          <el-input v-model="selectedConfig.name" />
        </el-form-item>
        <el-form-item
          v-for="field in selectedNodeType?.fields ?? []"
          :key="field.key"
          :label="field.label"
          :required="field.required"
        >
          <el-select
            v-if="field.type === 'datasource'"
            v-model="selectedConfig[field.key]"
            placeholder="选择数据源"
            clearable
            filterable
          >
            <el-option
              v-for="ds in datasources"
              :key="ds.code"
              :label="`${ds.name} (${ds.type})`"
              :value="ds.code"
            />
          </el-select>
          <el-select
            v-else-if="field.type === 'select'"
            v-model="selectedConfig[field.key]"
            clearable
          >
            <el-option
              v-for="opt in field.options"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-else-if="field.type === 'schema'"
            v-model="selectedConfig[field.key]"
            placeholder="选择库"
            clearable
            filterable
            @change="onMetaFieldChange(field)"
          >
            <el-option
              v-for="opt in schemaOptions(field)"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-else-if="field.type === 'table'"
            v-model="selectedConfig[field.key]"
            placeholder="选择表"
            clearable
            filterable
            @change="onMetaFieldChange(field)"
          >
            <el-option
              v-for="opt in tableOptions(field)"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-else-if="field.type === 'columns'"
            v-model="selectedConfig[field.key]"
            placeholder="选择字段（留空=全部）"
            multiple
            clearable
            filterable
            collapse-tags
            @change="onMetaFieldChange(field)"
          >
            <el-option
              v-for="opt in columnOptions(field)"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <div v-else-if="field.type === 'fieldMap'" class="field-map">
            <div v-for="(row, idx) in selectedConfig[field.key]" :key="idx" class="field-map-row">
              <el-select v-model="row.source" placeholder="源字段" clearable filterable @change="onFieldMapChange">
                <el-option
                  v-for="opt in fieldMapSourceOptions(field)"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <span class="mapping-arrow">→</span>
              <el-select v-model="row.target" placeholder="目标字段" clearable filterable @change="onFieldMapChange">
                <el-option
                  v-for="opt in fieldMapTargetOptions(field)"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <el-button link type="danger" @click="removeFieldMapRow(field.key, idx)">删除</el-button>
            </div>
            <el-button link type="primary" @click="addFieldMapRow(field.key)">+ 添加映射</el-button>
          </div>
          <el-select
            v-else-if="field.type === 'tags'"
            v-model="selectedConfig[field.key]"
            multiple
            filterable
            allow-create
            default-first-option
            :reserve-keyword="false"
            placeholder="输入后回车添加，可多个"
            style="width: 100%"
          />
          <el-input-number
            v-else-if="field.type === 'number'"
            v-model="selectedConfig[field.key]"
            :controls="false"
            style="width: 160px"
          />
          <SqlEditor
            v-else-if="field.type === 'sql'"
            v-model="selectedConfig[field.key]"
            :height="field.height || '220px'"
          />
          <div v-else-if="field.type === 'collapsibleSql'" class="collapsible-field">
            <div class="collapsible-header" @click="toggleFieldCollapse(field.key)">
              <el-icon class="collapsible-arrow" :class="{ expanded: !collapsedFields[field.key] }">
                <ArrowRight />
              </el-icon>
              <span>{{ field.label }}</span>
            </div>
            <div v-show="!collapsedFields[field.key]" class="collapsible-body">
              <SqlEditor v-model="selectedConfig[field.key]" :height="field.height || '150px'" />
            </div>
          </div>
          <div v-else-if="field.type === 'fieldMapping'" class="field-mapping">
            <div v-for="(row, idx) in selectedConfig[field.key]" :key="idx" class="mapping-row">
              <el-input v-model="row.source" placeholder="源字段" />
              <span class="mapping-arrow">→</span>
              <el-input v-model="row.target" placeholder="目标字段" />
              <el-button link type="danger" @click="removeMapping(field.key, idx)">删除</el-button>
            </div>
            <el-button link type="primary" @click="addMapping(field.key)">+ 添加映射</el-button>
          </div>
          <el-input
            v-else
            v-model="selectedConfig[field.key]"
            :type="field.type === 'textarea' ? 'textarea' : 'text'"
            :rows="field.type === 'textarea' ? 4 : 1"
            :placeholder="field.placeholder"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="danger" plain @click="deleteSelectedNode">删除节点</el-button>
        <el-button @click="nodeDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Aim,
  ArrowLeft,
  ArrowRight,
  Calendar,
  Connection,
  Delete,
  DocumentChecked,
  FullScreen,
  MagicStick,
  Operation,
  Rank,
  RefreshLeft,
  RefreshRight,
  Search,
  Upload,
  VideoPlay,
  ZoomIn,
  ZoomOut
} from '@element-plus/icons-vue'
import LogicFlow, { HtmlNode, HtmlNodeModel } from '@logicflow/core'
import '@logicflow/core/dist/index.css'
import { NODE_TYPES, getNodeType, buildDefaultConfig, type NodeConfig, type FieldMappingRow, type FieldDef } from './nodes'
import SqlEditor from '../../components/SqlEditor.vue'
import DataXTaskDialog from './DataXTaskDialog.vue'
import SeaTunnelTaskDialog from './SeaTunnelTaskDialog.vue'
import CodeTaskDialog from './CodeTaskDialog.vue'
import {
  getDataFlowDetail,
  updateDataFlow,
  publishDataFlow,
  runDataFlow,
  type DataFlowDetail
} from '../../api/dataflow'
import { listDataSource, listSchemaTable, tableDetail, type SchemaTableMap, type TableColumn } from '../../api/datasource'
import { cronValid, cronNexts } from '../../api/cron'

const route = useRoute()
const router = useRouter()

const editorRef = ref<HTMLElement>()
const canvasRef = ref<HTMLElement>()
const detail = ref<DataFlowDetail | null>(null)
const datasources = ref<{ id: number; code: string; name: string; type: string }[]>([])
const saving = ref(false)
const running = ref(false)
const paletteKeyword = ref('')
const graphRevision = ref(0)
const zoomPercent = ref(100)

let lf: LogicFlow | null = null
const nodeConfigs = reactive<Record<string, NodeConfig>>({})
const selectedNodeId = ref('')
const selectedEdgeId = ref('')
const nodeDialogVisible = ref(false)
const edgeOrder = ref(0)

// 前置/后置 SQL 默认折叠
const collapsedFields = reactive<Record<string, boolean>>({ preSql: true, postSql: true })

// 元数据缓存：库表树（按数据源 code）与字段（按 code::schema::table）
const schemaTrees = reactive<Record<string, SchemaTableMap[]>>({})
const columnCache = reactive<Record<string, TableColumn[]>>({})

function datasourceByCode(code: string) {
  return datasources.value.find((d) => d.code === code)
}

async function loadSchemas(code: string) {
  if (!code || schemaTrees[code]) return
  const ds = datasourceByCode(code)
  if (!ds) return
  try {
    schemaTrees[code] = await listSchemaTable(ds.id)
  } catch {
    schemaTrees[code] = []
  }
}

async function loadColumns(code: string, schema: string, table: string) {
  if (!code || !schema || !table) return
  const key = `${code}::${schema}::${table}`
  if (columnCache[key]) return
  const ds = datasourceByCode(code)
  if (!ds) return
  try {
    columnCache[key] = (await tableDetail(ds.id, schema, table)).columns ?? []
  } catch {
    columnCache[key] = []
  }
}

function schemaOptions(field: FieldDef) {
  const cfg = selectedConfig.value
  if (!cfg) return []
  const dsCode = field.dependsOn?.datasource ? cfg[field.dependsOn.datasource] : ''
  return (schemaTrees[dsCode] ?? []).map((s) => ({ value: s.key, label: s.label }))
}

function tableOptions(field: FieldDef) {
  const cfg = selectedConfig.value
  if (!cfg) return []
  const dsCode = field.dependsOn?.datasource ? cfg[field.dependsOn.datasource] : ''
  const schema = field.dependsOn?.schema ? cfg[field.dependsOn.schema] : ''
  const node = (schemaTrees[dsCode] ?? []).find((s) => s.key === schema)
  return (node?.children ?? []).map((c) => ({ value: c.key, label: c.label }))
}

function columnOptions(field: FieldDef) {
  const cfg = selectedConfig.value
  if (!cfg) return []
  const dsCode = field.dependsOn?.datasource ? cfg[field.dependsOn.datasource] : ''
  const schema = field.dependsOn?.schema ? cfg[field.dependsOn.schema] : ''
  const table = field.dependsOn?.table ? cfg[field.dependsOn.table] : ''
  const key = `${dsCode}::${schema}::${table}`
  return (columnCache[key] ?? []).map((c) => ({ value: c.name, label: c.comment ? `${c.name} (${c.comment})` : c.name }))
}

function onMetaFieldChange(field: FieldDef) {
  const cfg = selectedConfig.value
  if (!cfg) return
  if (field.key === 'sourceSchema') {
    cfg.sourceTable = ''
    cfg.fieldMapping = []
  } else if (field.key === 'sourceTable' || field.key === 'targetTable') {
    cfg.fieldMapping = []
  } else if (field.key === 'targetSchema') {
    cfg.targetTable = ''
    cfg.fieldMapping = []
  }
  if (field.key === 'sourceSchema' || field.key === 'sourceTable' || field.key === 'targetSchema' || field.key === 'targetTable') {
    regenerateSql(cfg)
  }
}

function regenerateSql(cfg: Record<string, any>) {
  const schema = cfg.sourceSchema
  const table = cfg.sourceTable
  const mappings = (Array.isArray(cfg.fieldMapping) ? cfg.fieldMapping : []).filter((m: any) => m && m.source)
  if (!table) {
    cfg.sqlText = ''
    return
  }
  const qualified = schema ? `${schema}.${table}` : table
  const colStr = mappings.length
    ? mappings.map((m: any) => (m.target && m.source !== m.target ? `${m.source} AS ${m.target}` : m.source)).join(', ')
    : '*'
  cfg.sqlText = `SELECT ${colStr} FROM ${qualified}`
}

// 字段映射：源/目标字段选项
function fieldMapSourceOptions(field: FieldDef) {
  const cfg = selectedConfig.value
  if (!cfg) return []
  const dsCode = field.dependsOn?.datasource ? cfg[field.dependsOn.datasource] : ''
  const schema = field.dependsOn?.schema ? cfg[field.dependsOn.schema] : ''
  const table = field.dependsOn?.table ? cfg[field.dependsOn.table] : ''
  const key = `${dsCode}::${schema}::${table}`
  return (columnCache[key] ?? []).map((c) => ({ value: c.name, label: c.name }))
}

function fieldMapTargetOptions(field: FieldDef) {
  const cfg = selectedConfig.value
  if (!cfg) return []
  const dsCode = field.dependsOn?.targetDatasource ? cfg[field.dependsOn.targetDatasource] : ''
  const schema = field.dependsOn?.targetSchema ? cfg[field.dependsOn.targetSchema] : ''
  const table = field.dependsOn?.targetTable ? cfg[field.dependsOn.targetTable] : ''
  const key = `${dsCode}::${schema}::${table}`
  return (columnCache[key] ?? []).map((c) => ({ value: c.name, label: c.name }))
}

function addFieldMapRow(key: string) {
  addMapping(key)
}

function removeFieldMapRow(key: string, idx: number) {
  removeMapping(key, idx)
  const cfg = selectedConfig.value
  if (cfg) regenerateSql(cfg)
}

function onFieldMapChange() {
  const cfg = selectedConfig.value
  if (cfg) regenerateSql(cfg)
}

// 源/目标字段都加载后，默认一一对应（按列名匹配，无匹配按下标）
function autoPopulateMapping(cfg: Record<string, any>) {
  const sourceKey = `${cfg.sourceDataSourceCode}::${cfg.sourceSchema}::${cfg.sourceTable}`
  const targetKey = `${cfg.targetDataSourceCode}::${cfg.targetSchema}::${cfg.targetTable}`
  const sourceCols = columnCache[sourceKey] ?? []
  const targetCols = columnCache[targetKey] ?? []
  if (!sourceCols.length || !targetCols.length) return
  const mapping = sourceCols.map((sc, i) => {
    const matched = targetCols.find((tc) => tc.name === sc.name)
    return { source: sc.name, target: matched ? matched.name : (targetCols[i]?.name ?? '') }
  })
  cfg.fieldMapping = mapping
  regenerateSql(cfg)
}

const flowCron = ref('')
const flowCronResult = reactive<{ loading: boolean; valid: boolean | null; nexts: string[]; message: string }>({
  loading: false,
  valid: null,
  nexts: [],
  message: ''
})
type CronMode = 'interval' | 'daily' | 'weekly' | 'monthly'

const cronPickerVisible = ref(false)
const cronDraft = reactive({
  mode: 'daily' as CronMode,
  intervalMinutes: 5,
  time: '00:00',
  weekdays: ['MON'] as string[],
  monthDays: [1] as number[]
})
const cronIntervalOptions = [5, 10, 15, 30]
const cronMonthDayOptions = Array.from({ length: 31 }, (_, i) => i + 1)
const cronWeekdayOptions = [
  { label: '周一', value: 'MON' },
  { label: '周二', value: 'TUE' },
  { label: '周三', value: 'WED' },
  { label: '周四', value: 'THU' },
  { label: '周五', value: 'FRI' },
  { label: '周六', value: 'SAT' },
  { label: '周日', value: 'SUN' }
]
const cronPresets = [
  { label: '每 5 分钟', expression: '0 */5 * * * ?' },
  { label: '每 15 分钟', expression: '0 */15 * * * ?' },
  { label: '每小时', expression: '0 0 * * * ?' },
  { label: '每天 00:00', expression: '0 0 0 * * ?' },
  { label: '每天 09:00', expression: '0 0 9 * * ?' },
  { label: '工作日 09:00', expression: '0 0 9 ? * MON-FRI' },
  { label: '每月 1 号', expression: '0 0 0 1 * ?' },
  { label: '每月 1/15 号', expression: '0 0 0 1,15 * ?' }
]
const cronPreview = computed(() => buildCronFromDraft())

const flowId = Number(route.params.id)

const selectedConfig = computed(() =>
  selectedNodeId.value ? nodeConfigs[selectedNodeId.value] : null
)
const selectedNodeType = computed(() => getNodeType(selectedConfig.value?.__type ?? ''))
const flowStatusText = computed(() => {
  const status = detail.value?.status?.toUpperCase()
  if (status === 'ONLINE' || status === 'PUBLISHED') return '已发布'
  if (status === 'RUNNING') return '运行中'
  if (status === 'PAUSE' || status === 'PAUSED' || status === 'STOP' || status === 'STOPPED') return '已暂停'
  if (status === 'WAIT_PUBLISH' || status === 'PENDING') return '待发布'
  return '草稿'
})
const flowStatusTone = computed(() => {
  if (flowStatusText.value === '已发布') return 'success'
  if (flowStatusText.value === '运行中') return 'running'
  if (flowStatusText.value === '已暂停' || flowStatusText.value === '待发布') return 'warning'
  return 'draft'
})

const PALETTE_GROUPS = [
  { key: 'sync', label: '数据同步', types: ['DATAX', 'SEATUNNEL'] },
  { key: 'process', label: '数据处理', types: ['SQL'] },
  { key: 'script', label: '脚本任务', types: ['PYTHON', 'SHELL'] }
]

const paletteGroups = computed(() => {
  const keyword = paletteKeyword.value.trim().toLowerCase()
  return PALETTE_GROUPS.map((group) => ({
    ...group,
    items: NODE_TYPES.filter((item) =>
      group.types.includes(item.type) &&
      (!keyword || item.label.toLowerCase().includes(keyword) || item.type.toLowerCase().includes(keyword))
    )
  })).filter((group) => group.items.length > 0)
})

const graphStats = computed(() => {
  graphRevision.value
  if (!lf) return { nodes: 0, edges: 0 }
  const graph = lf.getGraphData() as { nodes?: unknown[]; edges?: unknown[] }
  return { nodes: graph.nodes?.length ?? 0, edges: graph.edges?.length ?? 0 }
})

function hasConfigValue(value: unknown) {
  if (Array.isArray(value)) return value.length > 0
  return value !== undefined && value !== null && String(value).trim() !== ''
}

function isNodeConfigured(config: NodeConfig | undefined) {
  if (!config) return false
  const def = getNodeType(config.__type ?? '')
  return Boolean(config.name?.trim()) && (def?.fields ?? []).filter((field) => field.required).every((field) => hasConfigValue(config[field.key]))
}

const configuredCount = computed(() => {
  graphRevision.value
  if (!lf) return 0
  const graph = lf.getGraphData() as { nodes?: { id: string; properties?: NodeConfig }[] }
  return (graph.nodes ?? []).filter((node) => isNodeConfigured(nodeConfigs[node.id] ?? node.properties)).length
})

const selectedEdgeInfo = computed(() => {
  graphRevision.value
  if (!selectedEdgeId.value || !lf) return { source: '-', target: '-' }
  const edge = lf.getEdgeModelById(selectedEdgeId.value)
  const sourceId = edge?.sourceNodeId ?? ''
  const targetId = edge?.targetNodeId ?? ''
  return {
    source: nodeConfigs[sourceId]?.name || sourceId || '-',
    target: nodeConfigs[targetId]?.name || targetId || '-'
  }
})

function escapeHtml(s: string) {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]!))
}

function genId(prefix: string) {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function registerNodes() {
  for (const t of NODE_TYPES) {
    lf!.register({
      type: t.type,
      model: class FlowNodeModel extends HtmlNodeModel {
        setAttributes() {
          this.width = 220
          this.height = 76
        }
      },
      view: class FlowNodeView extends HtmlNode {
        setHtml(rootEl: SVGForeignObjectElement) {
          const el = rootEl as unknown as HTMLElement
          const model = this.props.model as unknown as { type: string; properties: NodeConfig }
          const def = getNodeType(model.type)
          const name = model.properties?.name ?? ''
          const configured = isNodeConfigured(model.properties)
          el.innerHTML = `
            <div class="flow-node" style="--node-color:${def?.color ?? '#64748b'}">
              <span class="flow-node-accent"></span>
              <span class="flow-node-icon">${def?.icon ?? '📦'}</span>
              <div class="flow-node-meta">
                <div class="flow-node-name">${escapeHtml(name)}</div>
                <div class="flow-node-type">${def?.label ?? model.type} · ${model.type}</div>
              </div>
              <span class="flow-node-state ${configured ? 'is-ready' : ''}"><i></i>${configured ? '已配置' : '待配置'}</span>
            </div>
          `
        }
      }
    } as never)
  }
}

function layoutNodes(
  nodes: { id: string }[],
  edges: { sourceNodeId: string; targetNodeId: string }[]
) {
  const indegree = new Map<string, number>()
  const adj = new Map<string, string[]>()
  nodes.forEach((n) => {
    indegree.set(n.id, 0)
    adj.set(n.id, [])
  })
  edges.forEach((e) => {
    if (!adj.has(e.sourceNodeId)) adj.set(e.sourceNodeId, [])
    if (!adj.has(e.targetNodeId)) adj.set(e.targetNodeId, [])
    indegree.set(e.targetNodeId, (indegree.get(e.targetNodeId) ?? 0) + 1)
    adj.get(e.sourceNodeId)!.push(e.targetNodeId)
  })

  const level = new Map<string, number>()
  const queue: string[] = []
  nodes.forEach((n) => {
    if ((indegree.get(n.id) ?? 0) === 0) {
      level.set(n.id, 0)
      queue.push(n.id)
    }
  })
  while (queue.length) {
    const cur = queue.shift()!
    const curLevel = level.get(cur) ?? 0
    for (const next of adj.get(cur) ?? []) {
      level.set(next, Math.max(level.get(next) ?? 0, curLevel + 1))
      indegree.set(next, (indegree.get(next) ?? 1) - 1)
      if (indegree.get(next) === 0) queue.push(next)
    }
  }
  nodes.forEach((n) => {
    if (!level.has(n.id)) level.set(n.id, 0)
  })

  const byLevel = new Map<number, string[]>()
  level.forEach((lv, id) => {
    if (!byLevel.has(lv)) byLevel.set(lv, [])
    byLevel.get(lv)!.push(id)
  })

  const pos = new Map<string, { x: number; y: number }>()
  const X_GAP = 300
  const Y_GAP = 132
  byLevel.forEach((ids, lv) => {
    ids.forEach((id, i) => {
      const columnOffset = Math.max(0, (ids.length - 1) * Y_GAP) / 2
      pos.set(id, { x: 160 + lv * X_GAP, y: 240 - columnOffset + i * Y_GAP })
    })
  })
  return pos
}

function renderDesign(design: DataFlowDetail['design']) {
  if (!design || !design.nodes?.length) return
  const pos = layoutNodes(design.nodes, design.edges ?? [])
  const nodes = design.nodes.map((n) => {
    const raw = typeof n.properties === 'string' ? JSON.parse(n.properties || '{}') : n.properties
    const config: NodeConfig = { __type: n.type, ...(raw || {}) }
    // 兼容历史数据：primaryKey 曾按逗号分隔字符串保存，现统一为数组
    if (typeof config.primaryKey === 'string') {
      config.primaryKey = config.primaryKey ? config.primaryKey.split(',').map((s: string) => s.trim()).filter(Boolean) : []
    }
    nodeConfigs[n.id] = config
    const p = pos.get(n.id) ?? { x: 60, y: 60 }
    return { id: n.id, type: n.type, x: p.x, y: p.y, properties: config }
  })
  const edges = (design.edges ?? []).map((e) => ({
    id: e.id,
    type: 'polyline',
    sourceNodeId: e.sourceNodeId,
    targetNodeId: e.targetNodeId,
    properties: { order: e.properties?.order ?? 0 }
  }))
  lf!.render({ nodes, edges })
  refreshGraphState()
}

function buildDesign() {
  const graphData = lf!.getGraphData() as {
    nodes: { id: string; type: string; properties?: NodeConfig }[]
    edges: { id: string; sourceNodeId: string; targetNodeId: string; properties?: { order?: number } }[]
  }
  const nodes = graphData.nodes.map((n) => {
    const cfg = { ...(nodeConfigs[n.id] ?? n.properties ?? {}) }
    delete cfg.__type
    return { id: n.id, type: n.type, properties: cfg }
  })
  const edges = graphData.edges.map((e) => ({
    id: e.id,
    sourceNodeId: e.sourceNodeId,
    targetNodeId: e.targetNodeId,
    properties: { order: e.properties?.order ?? 0 }
  }))
  return { nodes, edges }
}

const dragType = ref('')

function onDragStart(e: DragEvent, type: string) {
  dragType.value = type
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'copy'
    e.dataTransfer.setData('text/plain', type)
  }
}

function onDrop(e: DragEvent) {
  const type = dragType.value || e.dataTransfer?.getData('text/plain') || ''
  dragType.value = ''
  if (!type || !lf) return
  const pos = lf.getPointByClient(e.clientX, e.clientY).canvasOverlayPosition
  addNode(type, pos.x, pos.y)
}

function addNode(type: string, x?: number, y?: number) {
  const def = getNodeType(type)
  if (!def) return
  const id = genId(type)
  const config = buildDefaultConfig(type, `${def.label}_${Object.keys(nodeConfigs).length + 1}`)
  config.__type = type
  nodeConfigs[id] = config
  if (x === undefined || y === undefined) {
    const cx = (canvasRef.value?.clientWidth ?? 800) / 2
    x = cx - 90 + (Object.keys(nodeConfigs).length % 5) * 40
    y = 80 + (Object.keys(nodeConfigs).length % 6) * 80
  }
  lf!.addNode({
    id,
    type,
    x,
    y,
    properties: config
  })
  refreshGraphState()
  selectNode(id)
}

function selectNode(id: string) {
  selectedNodeId.value = id
  selectedEdgeId.value = ''
  nodeDialogVisible.value = true
}

function onNodeDialogClosed() {
  selectedNodeId.value = ''
  selectedEdgeId.value = ''
}

async function checkFlowCron() {
  const cron = flowCron.value.trim()
  if (!cron) {
    flowCronResult.valid = null
    flowCronResult.nexts = []
    flowCronResult.message = ''
    return
  }
  flowCronResult.loading = true
  try {
    const [valid, nexts] = await Promise.all([cronValid(cron), cronNexts(cron)])
    flowCronResult.valid = valid
    flowCronResult.nexts = nexts ?? []
    flowCronResult.message = valid ? '' : 'Cron 表达式无效'
  } catch {
    flowCronResult.valid = false
    flowCronResult.nexts = []
    flowCronResult.message = '校验失败，请检查表达式'
  } finally {
    flowCronResult.loading = false
  }
}

function parseCronTime(hour: string, minute: string) {
  const h = Number(hour)
  const m = Number(minute)
  if (Number.isNaN(h) || Number.isNaN(m)) return '00:00'
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

function parseDraftTime() {
  const [hour = '0', minute = '0'] = cronDraft.time.split(':')
  return {
    hour: String(Number(hour) || 0),
    minute: String(Number(minute) || 0)
  }
}

function buildCronFromDraft() {
  if (cronDraft.mode === 'interval') return `0 */${cronDraft.intervalMinutes} * * * ?`
  const time = parseDraftTime()
  if (cronDraft.mode === 'weekly') {
    const weekdays = cronDraft.weekdays.length ? cronDraft.weekdays.join(',') : 'MON'
    return `0 ${time.minute} ${time.hour} ? * ${weekdays}`
  }
  if (cronDraft.mode === 'monthly') {
    const days = cronDraft.monthDays.length ? [...cronDraft.monthDays].sort((a, b) => a - b).join(',') : '1'
    return `0 ${time.minute} ${time.hour} ${days} * ?`
  }
  return `0 ${time.minute} ${time.hour} * * ?`
}

function resetCronResult() {
  flowCronResult.valid = null
  flowCronResult.nexts = []
  flowCronResult.message = ''
}

function syncCronDraft(expression: string) {
  const parts = expression.trim().split(/\s+/)
  if (parts.length < 6 || parts[0] !== '0') return
  const [, minute, hour, day, month, weekday] = parts
  const intervalMatch = minute.match(/^\*\/(\d+)$/)
  if (intervalMatch && hour === '*' && day === '*' && month === '*' && weekday === '?') {
    const interval = Number(intervalMatch[1])
    if (cronIntervalOptions.includes(interval)) cronDraft.intervalMinutes = interval
    cronDraft.mode = 'interval'
    return
  }
  if (/^\d+$/.test(hour) && /^\d+$/.test(minute)) {
    cronDraft.time = parseCronTime(hour, minute)
    if (day === '*' && month === '*' && weekday === '?') {
      cronDraft.mode = 'daily'
      return
    }
    if (day === '?' && month === '*' && weekday) {
      cronDraft.mode = 'weekly'
      if (weekday === 'MON-FRI') {
        cronDraft.weekdays = ['MON', 'TUE', 'WED', 'THU', 'FRI']
      } else {
        const selected = weekday.split(',').filter((item) => cronWeekdayOptions.some((option) => option.value === item))
        if (selected.length) cronDraft.weekdays = selected
      }
      return
    }
    if (month === '*' && weekday === '?') {
      const selectedDays = day.split(',').map(Number).filter((item) => item >= 1 && item <= 31)
      cronDraft.mode = 'monthly'
      if (selectedDays.length) cronDraft.monthDays = selectedDays
    }
  }
}

function openCronPicker() {
  if (flowCron.value.trim()) syncCronDraft(flowCron.value)
  cronPickerVisible.value = true
}

function applyCronPreset(expression: string) {
  flowCron.value = expression
  syncCronDraft(expression)
  cronPickerVisible.value = false
  void checkFlowCron()
}

function applyCronDraft() {
  flowCron.value = cronPreview.value
  cronPickerVisible.value = false
  void checkFlowCron()
}

function clearFlowCron() {
  flowCron.value = ''
  cronPickerVisible.value = false
  resetCronResult()
}

function addMapping(key: string) {
  const cfg = selectedConfig.value
  if (!cfg) return
  if (!Array.isArray(cfg[key])) cfg[key] = []
  cfg[key].push({ source: '', target: '' } as FieldMappingRow)
}

function removeMapping(key: string, idx: number) {
  const cfg = selectedConfig.value
  if (!cfg) return
  ;(cfg[key] as FieldMappingRow[]).splice(idx, 1)
}

function toggleFieldCollapse(key: string) {
  collapsedFields[key] = !collapsedFields[key]
}

function refreshGraphState() {
  graphRevision.value += 1
  updateZoomPercent()
}

function updateZoomPercent() {
  if (!lf) return
  zoomPercent.value = Math.round(lf.getTransform().SCALE_X * 100)
}

function syncNodeConfigsFromGraph() {
  if (!lf) return
  const graph = lf.getGraphData() as { nodes?: { id: string; type: string; properties?: NodeConfig }[] }
  for (const node of graph.nodes ?? []) {
    if (!nodeConfigs[node.id]) {
      const properties = node.properties ?? ({} as NodeConfig)
      nodeConfigs[node.id] = { ...properties, name: properties.name || getNodeType(node.type)?.label || node.type, __type: node.type }
    }
  }
}

function undo() {
  lf?.undo()
  window.setTimeout(() => {
    syncNodeConfigsFromGraph()
    refreshGraphState()
  })
}

function redo() {
  lf?.redo()
  window.setTimeout(() => {
    syncNodeConfigsFromGraph()
    refreshGraphState()
  })
}

function zoomIn() {
  const nextScale = Math.min(2, (lf?.getTransform().SCALE_X ?? 1) + 0.1)
  const scale = lf?.zoom(nextScale)
  if (scale) zoomPercent.value = Math.round(Number.parseFloat(scale))
}

function zoomOut() {
  const nextScale = Math.max(0.4, (lf?.getTransform().SCALE_X ?? 1) - 0.1)
  const scale = lf?.zoom(nextScale)
  if (scale) zoomPercent.value = Math.round(Number.parseFloat(scale))
}

function resetZoom() {
  lf?.resetZoom()
  zoomPercent.value = 100
}

function fitView() {
  lf?.fitView()
  window.requestAnimationFrame(updateZoomPercent)
}

function autoLayout() {
  if (!lf) return
  const graphData = lf.getGraphData() as {
    nodes: { id: string }[]
    edges: { sourceNodeId: string; targetNodeId: string }[]
  }
  if (!graphData.nodes?.length) return
  const pos = layoutNodes(graphData.nodes, graphData.edges ?? [])
  pos.forEach((p, id) => {
    lf!.getNodeModelById(id)?.moveTo(p.x, p.y, true)
  })
  lf.translateCenter()
  updateZoomPercent()
  ElMessage.success('已完成自动布局')
}

async function toggleFullscreen() {
  if (!editorRef.value) return
  try {
    if (document.fullscreenElement) await document.exitFullscreen()
    else await editorRef.value.requestFullscreen()
    window.setTimeout(() => lf?.resize())
  } catch {
    ElMessage.warning('当前浏览器不支持全屏画布')
  }
}

function deleteSelectedNode() {
  if (!selectedNodeId.value) return
  lf!.deleteNode(selectedNodeId.value)
  delete nodeConfigs[selectedNodeId.value]
  selectedNodeId.value = ''
  nodeDialogVisible.value = false
  refreshGraphState()
}

function deleteSelectedEdge() {
  if (!selectedEdgeId.value) return
  lf!.deleteEdge(selectedEdgeId.value)
  selectedEdgeId.value = ''
  refreshGraphState()
}

function onEditorKeydown(event: KeyboardEvent) {
  const target = event.target as HTMLElement | null
  const isEditing = target?.tagName === 'INPUT' || target?.tagName === 'TEXTAREA' || target?.isContentEditable
  const modifier = event.metaKey || event.ctrlKey

  if (modifier && event.key.toLowerCase() === 's') {
    event.preventDefault()
    handleSave(true)
    return
  }
  if (isEditing || nodeDialogVisible.value) return
  if (modifier && event.key.toLowerCase() === 'z') {
    event.preventDefault()
    event.shiftKey ? redo() : undo()
  } else if (event.key === 'Delete' || event.key === 'Backspace') {
    if (selectedEdgeId.value) {
      event.preventDefault()
      deleteSelectedEdge()
    }
  } else if (event.key === '0') {
    event.preventDefault()
    fitView()
  } else if (event.key === '+' || event.key === '=') {
    event.preventDefault()
    zoomIn()
  } else if (event.key === '-') {
    event.preventDefault()
    zoomOut()
  }
}

async function handleSave(isDraft: boolean) {
  if (!lf) return
  const graphData = lf.getGraphData() as { nodes: unknown[] }
  if (!graphData.nodes?.length) {
    ElMessage.warning('请先添加组件')
    return
  }
  saving.value = true
  try {
    const design = JSON.stringify(buildDesign())
    await updateDataFlow({ id: flowId, design, temporarily: isDraft, cron: flowCron.value })
    if (isDraft) {
      ElMessage.success('已保存')
    } else {
      await doPublish()
    }
  } catch {
    // 错误提示已在拦截器处理
  } finally {
    saving.value = false
  }
}

async function doPublish() {
  const { value } = await ElMessageBox.prompt('请输入发布说明', '发布数据流', {
    confirmButtonText: '发布',
    cancelButtonText: '取消'
  })
  await publishDataFlow(flowId, value || '发布')
  ElMessage.success('发布成功')
  goBack()
}

async function handleRun() {
  if (!lf) return
  const graphData = lf.getGraphData() as { nodes: unknown[] }
  if (!graphData.nodes?.length) {
    ElMessage.warning('请先添加组件')
    return
  }
  running.value = true
  try {
    const design = JSON.stringify(buildDesign())
    await updateDataFlow({ id: flowId, design, temporarily: true, cron: flowCron.value })
    await runDataFlow(flowId)
    ElMessage.success('已提交运行')
    router.push({ path: '/dataflow/instance', query: { flowId, flowName: detail.value?.name ?? '' } })
  } catch {
    // 错误提示已在拦截器处理
  } finally {
    running.value = false
  }
}

function goBack() {
  router.push('/dataflow')
}

onMounted(async () => {
  window.addEventListener('keydown', onEditorKeydown)
  document.addEventListener('fullscreenchange', refreshGraphState)
  try {
    const [d, ds] = await Promise.all([
      getDataFlowDetail(flowId),
      listDataSource({}, 1, 100)
    ])
    detail.value = d
    flowCron.value = d.cron ?? ''
    datasources.value = (ds.records ?? []).map((r) => ({ id: r.id, code: r.code, name: r.name, type: r.type }))
  } catch {
    ElMessage.error('加载数据流失败')
    goBack()
    return
  }

  lf = new LogicFlow({
    container: canvasRef.value!,
    grid: {
      size: 20,
      visible: true,
      type: 'dot',
      config: { color: '#d9dee8', thickness: 1 }
    },
    edgeType: 'polyline',
    isSilentMode: false,
    stopZoomGraph: false,
    stopMoveGraph: false
  })
  lf.setTheme({
    polyline: { stroke: '#94a3b8', strokeWidth: 2 },
    arrow: { offset: 9, verticalLength: 4, fill: '#94a3b8', stroke: '#94a3b8' },
    anchor: { r: 4, fill: '#ffffff', stroke: '#6366f1', strokeWidth: 2 },
    anchorLine: { stroke: '#6366f1', strokeWidth: 2, strokeDasharray: '4 4' },
    outline: { stroke: '#6366f1', strokeWidth: 2 },
    edgeOutline: { stroke: '#6366f1', strokeWidth: 8, strokeOpacity: 0.12 }
  } as never)
  lf.setZoomMiniSize(0.4)
  lf.setZoomMaxSize(2)
  registerNodes()
  renderDesign(detail.value?.design ?? null)

  lf.on('node:click', ({ data }) => {
    selectNode(data.id as string)
  })
  lf.on('edge:click', ({ data }) => {
    selectedNodeId.value = ''
    selectedEdgeId.value = data.id as string
    nodeDialogVisible.value = false
    const props = lf!.getProperties(data.id as string) as { order?: number } | undefined
    edgeOrder.value = props?.order ?? 0
  })
  lf.on('blank:click', () => {
    selectedNodeId.value = ''
    selectedEdgeId.value = ''
    nodeDialogVisible.value = false
  })
  lf.on('node:add', refreshGraphState)
  lf.on('node:delete', refreshGraphState)
  lf.on('edge:add', refreshGraphState)
  lf.on('edge:delete', refreshGraphState)
  lf.on('graph:transform', updateZoomPercent)
  lf.on('history:change', () => {
    syncNodeConfigsFromGraph()
    refreshGraphState()
  })
  refreshGraphState()
})

watch(
  () => (selectedNodeId.value ? nodeConfigs[selectedNodeId.value] : null),
  (cfg) => {
    if (cfg && lf) {
      lf.setProperties(selectedNodeId.value, { ...cfg })
      refreshGraphState()
    }
  },
  { deep: true }
)

// 节点配置变化时，按需拉取库/表/字段元数据
watch(
  () => (selectedNodeId.value ? nodeConfigs[selectedNodeId.value] : null),
  (cfg) => {
    if (!cfg || !cfg.__type) return
    if (cfg.__type === 'DATAX') return
    const def = getNodeType(cfg.__type)
    for (const field of def?.fields ?? []) {
      if (!field.dependsOn) continue
      const dsCode = field.dependsOn.datasource ? cfg[field.dependsOn.datasource] : ''
      if (dsCode) loadSchemas(dsCode)
      const schema = field.dependsOn.schema ? cfg[field.dependsOn.schema] : ''
      const table = field.dependsOn.table ? cfg[field.dependsOn.table] : ''
      if (field.type === 'columns') {
        if (dsCode && schema && table) loadColumns(dsCode, schema, table)
      } else if (field.type === 'fieldMap') {
        if (dsCode && schema && table) loadColumns(dsCode, schema, table)
        const tCode = field.dependsOn.targetDatasource ? cfg[field.dependsOn.targetDatasource] : ''
        const tSchema = field.dependsOn.targetSchema ? cfg[field.dependsOn.targetSchema] : ''
        const tTable = field.dependsOn.targetTable ? cfg[field.dependsOn.targetTable] : ''
        if (tCode) loadSchemas(tCode)
        if (tCode && tSchema && tTable) loadColumns(tCode, tSchema, tTable)
      }
    }
  },
  { deep: true }
)

// 源/目标字段加载完成后，默认填充一一对应映射
watch(
  () => {
    const cfg = selectedConfig.value
    if (!cfg || cfg.__type !== 'DATAX') return ''
    const sk = `${cfg.sourceDataSourceCode}::${cfg.sourceSchema}::${cfg.sourceTable}`
    const tk = `${cfg.targetDataSourceCode}::${cfg.targetSchema}::${cfg.targetTable}`
    return `${sk}|${tk}|${(columnCache[sk] ?? []).length}|${(columnCache[tk] ?? []).length}`
  },
  () => {
    const cfg = selectedConfig.value
    if (!cfg || cfg.__type !== 'DATAX') return
    if (Array.isArray(cfg.fieldMapping) && cfg.fieldMapping.length > 0) return
    autoPopulateMapping(cfg)
  },
  { immediate: true }
)

watch(edgeOrder, (order) => {
  if (selectedEdgeId.value && lf) {
    lf.setProperties(selectedEdgeId.value, { order })
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onEditorKeydown)
  document.removeEventListener('fullscreenchange', refreshGraphState)
  lf?.destroy?.()
  lf = null
})
</script>

<style scoped>
.editor {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f5f7fb;
  color: #1f2937;
}

.editor-header {
  height: 68px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 0 20px;
  border-bottom: 1px solid #e7eaf0;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
  z-index: 20;
}

.header-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.back-button {
  color: #64748b;
}

.header-divider {
  width: 1px;
  height: 28px;
  margin: 0 16px 0 10px;
  background: #e5e7eb;
}

.flow-heading {
  min-width: 0;
}

.flow-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title {
  font-size: 16px;
  font-weight: 650;
  color: #172033;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.flow-status {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}

.flow-status.is-draft {
  color: #64748b;
  background: #f1f5f9;
}

.flow-status.is-success {
  color: #15803d;
  background: #ecfdf3;
}

.flow-status.is-running {
  color: #2563eb;
  background: #eff6ff;
}

.flow-status.is-warning {
  color: #b45309;
  background: #fffbeb;
}

.flow-subtitle {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 3px;
  color: #94a3b8;
  font-size: 11px;
  white-space: nowrap;
}

.subtitle-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #cbd5e1;
}

.header-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.editor-body {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
}

.palette {
  width: 220px;
  flex-shrink: 0;
  box-sizing: border-box;
  border-right: 1px solid #e7eaf0;
  padding: 18px 14px;
  overflow-y: auto;
  background: #fff;
  z-index: 10;
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.panel-title {
  color: #172033;
  font-size: 14px;
  font-weight: 650;
}

.panel-subtitle {
  margin-top: 3px;
  color: #94a3b8;
  font-size: 11px;
}

.panel-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  color: #6366f1;
  background: #eef2ff;
  font-size: 11px;
  font-weight: 700;
}

.palette-search {
  margin-bottom: 18px;
}

.palette-search :deep(.el-input__wrapper) {
  border-radius: 7px;
  box-shadow: 0 0 0 1px #e5e7eb inset;
  background: #f8fafc;
}

.palette-group {
  margin-bottom: 18px;
}

.palette-group-title {
  margin: 0 4px 8px;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 650;
  letter-spacing: 0.08em;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 52px;
  box-sizing: border-box;
  padding: 8px 9px;
  margin-bottom: 7px;
  border: 1px solid #e8ebf0;
  border-radius: 8px;
  cursor: grab;
  background: #fff;
  transition: border-color 0.16s, box-shadow 0.16s, transform 0.16s;
}

.palette-item:active {
  cursor: grabbing;
}

.palette-item:hover {
  border-color: #a5b4fc;
  box-shadow: 0 5px 16px rgba(79, 70, 229, 0.09);
  transform: translateY(-1px);
}

.palette-icon {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.palette-item-meta {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.palette-item-name {
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.palette-item-type {
  color: #a0aec0;
  font-size: 10px;
}

.palette-drag {
  flex-shrink: 0;
  color: #cbd5e1;
  font-size: 14px;
}

.palette-empty {
  padding: 28px 8px;
  color: #94a3b8;
  font-size: 12px;
  text-align: center;
}

.palette-hint {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin-top: 8px;
  padding: 10px;
  border-radius: 7px;
  color: #8491a6;
  background: #f8fafc;
  font-size: 11px;
  line-height: 1.55;
}

.hint-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 15px;
  height: 15px;
  flex-shrink: 0;
  margin-top: 1px;
  border: 1px solid #cbd5e1;
  border-radius: 50%;
  font-size: 9px;
}

.canvas-wrap {
  flex: 1;
  min-width: 0;
  position: relative;
  overflow: hidden;
  background: #f7f8fc;
}

.canvas {
  width: 100%;
  height: 100%;
}

.canvas-toolbar {
  position: absolute;
  top: 14px;
  left: 50%;
  z-index: 8;
  display: flex;
  align-items: center;
  gap: 7px;
  height: 42px;
  padding: 0 8px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 8px 24px rgba(30, 41, 59, 0.1);
  transform: translateX(-50%);
  backdrop-filter: blur(8px);
}

.tool-group {
  display: flex;
  align-items: center;
  gap: 2px;
}

.tool-divider {
  width: 1px;
  height: 20px;
  background: #e5e7eb;
}

.tool-button,
.zoom-value {
  height: 30px;
  border: 0;
  border-radius: 6px;
  color: #64748b;
  background: transparent;
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}

.tool-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  font-size: 16px;
}

.tool-button-wide {
  width: auto;
  gap: 5px;
  padding: 0 8px;
  font-size: 12px;
}

.tool-button:hover,
.zoom-value:hover {
  color: #4f46e5;
  background: #eef2ff;
}

.zoom-value {
  width: 48px;
  padding: 0;
  color: #475569;
  font-size: 11px;
  font-weight: 600;
}

.canvas-empty {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 6;
  display: flex;
  width: 360px;
  flex-direction: column;
  align-items: center;
  transform: translate(-50%, -52%);
  text-align: center;
  pointer-events: none;
}

.canvas-empty :deep(.el-button) {
  pointer-events: auto;
}

.empty-visual {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 68px;
  height: 68px;
  margin-bottom: 16px;
  border: 1px solid #dfe3ff;
  border-radius: 20px;
  color: #6366f1;
  background: linear-gradient(145deg, #fff, #eef2ff);
  box-shadow: 0 14px 32px rgba(79, 70, 229, 0.12);
  font-size: 30px;
}

.empty-title {
  color: #334155;
  font-size: 16px;
  font-weight: 650;
}

.empty-text {
  margin: 8px 0 18px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.7;
}

.canvas-statusbar {
  position: absolute;
  right: 14px;
  bottom: 12px;
  left: 14px;
  z-index: 8;
  display: flex;
  align-items: center;
  gap: 18px;
  height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 7px;
  color: #8491a6;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
  font-size: 10px;
  pointer-events: none;
  backdrop-filter: blur(6px);
}

.status-ready {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
}

.status-ready i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 3px #dcfce7;
}

.status-shortcut {
  margin-left: auto;
  color: #a7b0bf;
}

.config {
  width: 300px;
  flex-shrink: 0;
  box-sizing: border-box;
  border-left: 1px solid #e7eaf0;
  padding: 18px 16px;
  overflow-y: auto;
  background: #fff;
  z-index: 10;
}

.config-heading {
  padding-bottom: 14px;
  border-bottom: 1px solid #eef0f4;
}

.config-heading-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  color: #6366f1;
  background: #eef2ff;
}

.section-label {
  margin: 20px 0 12px;
  color: #475569;
  font-size: 12px;
  font-weight: 650;
}

.overview-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px;
  border: 1px solid #e7eaf0;
  border-radius: 9px;
  background: linear-gradient(135deg, #fff, #f8faff);
}

.overview-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #818cf8);
  font-size: 18px;
}

.overview-card > div:last-child {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.overview-card strong {
  overflow: hidden;
  color: #334155;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overview-card span {
  color: #94a3b8;
  font-size: 10px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 7px;
  margin-top: 10px;
}

.metric-item {
  display: flex;
  align-items: center;
  padding: 9px 4px;
  border-radius: 7px;
  flex-direction: column;
  background: #f8fafc;
}

.metric-item strong {
  color: #334155;
  font-size: 15px;
}

.metric-item span {
  margin-top: 2px;
  color: #94a3b8;
  font-size: 9px;
}

.schedule-label {
  margin-bottom: 8px;
}

.edge-card {
  padding: 13px;
  border: 1px solid #e7eaf0;
  border-radius: 9px;
  background: #f8fafc;
}

.edge-node {
  display: flex;
  align-items: center;
  gap: 9px;
}

.edge-node > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.edge-node small {
  color: #94a3b8;
  font-size: 9px;
}

.edge-node strong {
  overflow: hidden;
  color: #334155;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edge-dot {
  width: 9px;
  height: 9px;
  flex-shrink: 0;
  border: 3px solid #e0e7ff;
  border-radius: 50%;
  background: #6366f1;
}

.edge-dot.target {
  border-color: #dcfce7;
  background: #22c55e;
}

.edge-line {
  display: flex;
  width: 15px;
  height: 24px;
  margin-left: 4px;
  align-items: center;
  flex-direction: column;
  color: #94a3b8;
  font-size: 9px;
}

.edge-line span {
  width: 1px;
  height: 15px;
  background: #cbd5e1;
}

.edge-line .el-icon {
  transform: rotate(90deg);
}

.edge-form {
  margin-top: 18px;
}

.config-help,
.config-tip {
  margin-top: 14px;
  padding: 11px 12px;
  border-radius: 7px;
  color: #8491a6;
  background: #f8fafc;
  font-size: 10px;
  line-height: 1.65;
}

.config-tip-title {
  margin-bottom: 3px;
  color: #64748b;
  font-weight: 650;
}

.field-mapping {
  width: 100%;
}

.mapping-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.field-map {
  width: 100%;
}

.field-map-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.mapping-arrow {
  color: #909399;
  flex-shrink: 0;
}

.cron-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.cron-row .el-input {
  flex: 1;
}

:global(.cron-picker-popover) {
  padding: 0 !important;
  border: 1px solid #e7eaf0 !important;
  border-radius: 8px !important;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.14) !important;
}

.cron-picker {
  padding: 14px;
}

.cron-picker-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eef0f4;
}

.cron-picker-head > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.cron-picker-head strong {
  color: #334155;
  font-size: 13px;
  font-weight: 650;
}

.cron-picker-head span {
  color: #94a3b8;
  font-size: 11px;
}

.cron-preset-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.cron-preset {
  display: flex;
  min-width: 0;
  height: 58px;
  padding: 9px 10px;
  border: 1px solid #e7eaf0;
  border-radius: 7px;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  background: #fff;
  text-align: left;
  transition: border-color 0.16s, background 0.16s, box-shadow 0.16s;
}

.cron-preset:hover {
  border-color: #c7d2fe;
  background: #f8faff;
  box-shadow: 0 8px 18px rgba(99, 102, 241, 0.08);
}

.cron-preset strong {
  color: #334155;
  font-size: 12px;
  font-weight: 650;
}

.cron-preset span {
  overflow: hidden;
  max-width: 100%;
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cron-picker-section {
  margin-top: 12px;
}

.cron-field-label {
  margin-bottom: 7px;
  color: #64748b;
  font-size: 11px;
  font-weight: 650;
}

.cron-mode-group {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  width: 100%;
}

.cron-mode-group :deep(.el-radio-button__inner) {
  width: 100%;
  padding-right: 0;
  padding-left: 0;
  font-size: 11px;
}

.cron-check-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.cron-check-grid :deep(.el-checkbox-button__inner) {
  width: 100%;
  border: 1px solid #e7eaf0;
  border-radius: 6px !important;
  padding: 7px 0;
  font-size: 11px;
}

.cron-preview-card {
  display: flex;
  margin-top: 12px;
  padding: 10px 11px;
  border: 1px solid #e0e7ff;
  border-radius: 7px;
  flex-direction: column;
  gap: 5px;
  background: #f8faff;
}

.cron-preview-card span {
  color: #818cf8;
  font-size: 10px;
  font-weight: 650;
}

.cron-preview-card strong {
  color: #334155;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 650;
  word-break: break-all;
}

.cron-picker-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.cron-nexts {
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  color: #64748b;
  background: #f8fafc;
  font-size: 10px;
}

.cron-nexts-title {
  font-weight: 600;
  color: #475569;
  margin-bottom: 4px;
}

.cron-next {
  line-height: 1.7;
}

.cron-invalid {
  margin-top: 8px;
  font-size: 12px;
  color: #f56c6c;
}

.collapsible-field {
  width: 100%;
  border: 1px solid #eef0f4;
  border-radius: 6px;
  overflow: hidden;
}

.collapsible-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  height: 38px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  background: #fafafa;
  user-select: none;
}

.collapsible-header:hover {
  background: #f2f3f5;
}

.collapsible-arrow {
  transition: transform 0.2s;
  color: #909399;
}

.collapsible-arrow.expanded {
  transform: rotate(90deg);
}

.collapsible-body {
  padding: 8px;
  border-top: 1px solid #eef0f4;
  background: #fff;
}

@media (max-width: 1200px) {
  .palette {
    width: 190px;
  }

  .config {
    width: 270px;
  }

  .status-shortcut {
    display: none;
  }
}
</style>

<style>
/* 非 scoped：LogicFlow 的 HTML 节点内容由 innerHTML 注入，需全局样式 */
.flow-node {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 10px 0 14px;
  background: #fff;
  border: 1px solid #dfe3ea;
  border-radius: 9px;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.08);
  box-sizing: border-box;
  overflow: hidden;
  transition: border-color 0.16s, box-shadow 0.16s, transform 0.16s;
}

.flow-node:hover {
  border-color: color-mix(in srgb, var(--node-color) 55%, #fff);
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.12);
  transform: translateY(-1px);
}

.flow-node-accent {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 4px;
  background: var(--node-color);
}

.flow-node-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 9px;
  color: var(--node-color);
  background: color-mix(in srgb, var(--node-color) 9%, #fff);
  font-size: 17px;
}

.flow-node-meta {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.flow-node-name {
  color: #273449;
  font-size: 12px;
  font-weight: 650;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.flow-node-type {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 9px;
  white-space: nowrap;
}

.flow-node-state {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 10px;
  padding: 3px 6px;
  border-radius: 8px;
  color: #d97706;
  background: #fffbeb;
  font-size: 8px;
}

.flow-node-state i {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
}

.flow-node-state.is-ready {
  color: #16a34a;
  background: #f0fdf4;
}

.lf-node-selected .flow-node {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12), 0 8px 22px rgba(15, 23, 42, 0.12);
}

.lf-anchor {
  opacity: 0;
  transition: opacity 0.15s;
}

.lf-node:hover .lf-anchor,
.lf-node-selected .lf-anchor {
  opacity: 1;
}

.lf-edge-selected path,
.lf-edge:hover path {
  stroke: #6366f1;
}

.lf-graph {
  background: #f7f8fc;
}

/* 节点编辑弹窗：表单区限高滚动，保证底部按钮始终可见 */
.node-dialog .el-dialog__body {
  max-height: 68vh;
  overflow-y: auto;
  padding-top: 8px;
}
</style>
