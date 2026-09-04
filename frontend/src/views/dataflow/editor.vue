<template>
  <div class="editor">
    <div class="editor-header">
      <div class="header-left">
        <el-button link :icon="ArrowLeft" @click="goBack">返回</el-button>
        <span class="title">{{ detail?.name || '任务流画布' }}</span>
      </div>
      <div class="header-right">
        <el-tooltip content="缩小" placement="bottom">
          <el-button :icon="ZoomOut" circle @click="zoomOut" />
        </el-tooltip>
        <el-tooltip content="放大" placement="bottom">
          <el-button :icon="ZoomIn" circle @click="zoomIn" />
        </el-tooltip>
        <el-tooltip content="适应画布" placement="bottom">
          <el-button :icon="FullScreen" circle @click="fitView" />
        </el-tooltip>
        <el-button plain @click="autoLayout">自动布局</el-button>
        <el-button type="success" plain :loading="running" @click="handleRun">运行</el-button>
        <el-button :loading="saving" @click="handleSave(true)">保存</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave(false)">发布</el-button>
      </div>
    </div>

    <div class="editor-body">
      <aside class="palette">
        <div class="palette-title">组件</div>
        <div
          v-for="t in NODE_TYPES"
          :key="t.type"
          class="palette-item"
          draggable="true"
          @dragstart="onDragStart($event, t.type)"
          @click="addNode(t.type)"
        >
          <span class="palette-icon" :style="{ background: t.color }">{{ t.icon }}</span>
          <span>{{ t.label }}</span>
        </div>
        <div class="palette-hint">点击或拖拽组件到画布</div>
      </aside>

      <div class="canvas-wrap" @dragover.prevent @drop.prevent="onDrop">
        <div ref="canvasRef" class="canvas"></div>
      </div>

      <aside class="config">
        <template v-if="selectedEdgeId">
          <div class="config-title">连线配置</div>
          <el-form label-position="top">
            <el-form-item label="执行顺序（越小越先执行）">
              <el-input-number v-model="edgeOrder" :min="0" :step="1" style="width: 100%" />
            </el-form-item>
          </el-form>
          <el-button type="danger" plain style="width: 100%" @click="deleteSelectedEdge">
            删除连线
          </el-button>
        </template>

        <div v-else class="config-schedule">
          <div class="config-title">调度配置</div>
          <el-form label-position="top">
            <el-form-item label="Cron 表达式（留空 = 仅手动运行）">
              <div class="cron-row">
                <el-input
                  v-model="flowCron"
                  placeholder="0 0 1 * * ?"
                  clearable
                  @blur="checkFlowCron"
                />
                <el-button :loading="flowCronResult.loading" @click="checkFlowCron">校验</el-button>
              </div>
              <div v-if="flowCronResult.valid === true && flowCronResult.nexts.length" class="cron-nexts">
                <div class="cron-nexts-title">最近 5 次执行时间</div>
                <div v-for="(t, i) in flowCronResult.nexts" :key="i" class="cron-next">{{ t }}</div>
              </div>
              <div v-else-if="flowCronResult.valid === false" class="cron-invalid">{{ flowCronResult.message }}</div>
            </el-form-item>
          </el-form>
          <div class="config-schedule-hint">点击节点在弹窗中编辑参数，点击空白处回到此处配置调度</div>
        </div>
      </aside>
    </div>

    <el-dialog
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
import { ArrowLeft, ArrowRight, ZoomIn, ZoomOut, FullScreen } from '@element-plus/icons-vue'
import LogicFlow, { HtmlNode, HtmlNodeModel } from '@logicflow/core'
import '@logicflow/core/dist/index.css'
import { NODE_TYPES, getNodeType, buildDefaultConfig, type NodeConfig, type FieldMappingRow, type FieldDef } from './nodes'
import SqlEditor from '../../components/SqlEditor.vue'
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

const canvasRef = ref<HTMLElement>()
const detail = ref<DataFlowDetail | null>(null)
const datasources = ref<{ id: number; code: string; name: string; type: string }[]>([])
const saving = ref(false)
const running = ref(false)

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

const flowId = Number(route.params.id)

const selectedConfig = computed(() =>
  selectedNodeId.value ? nodeConfigs[selectedNodeId.value] : null
)
const selectedNodeType = computed(() => getNodeType(selectedConfig.value?.__type ?? ''))

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
          this.width = 180
          this.height = 64
        }
      },
      view: class FlowNodeView extends HtmlNode {
        setHtml(rootEl: SVGForeignObjectElement) {
          const el = rootEl as unknown as HTMLElement
          const model = this.props.model as unknown as { type: string; properties: NodeConfig }
          const def = getNodeType(model.type)
          const name = model.properties?.name ?? ''
          el.innerHTML = `
            <div class="flow-node" style="border-color:${def?.color ?? '#ccc'}">
              <span class="flow-node-icon">${def?.icon ?? '📦'}</span>
              <div class="flow-node-meta">
                <div class="flow-node-name">${escapeHtml(name)}</div>
                <div class="flow-node-type" style="color:${def?.color ?? '#999'}">${def?.label ?? model.type}</div>
              </div>
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
  const X_GAP = 240
  const Y_GAP = 120
  byLevel.forEach((ids, lv) => {
    ids.forEach((id, i) => {
      pos.set(id, { x: 60 + lv * X_GAP, y: 60 + i * Y_GAP })
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
}

function buildDesign() {
  const graphData = lf!.getGraphData() as {
    nodes: { id: string; type: string }[]
    edges: { id: string; sourceNodeId: string; targetNodeId: string; properties?: { order?: number } }[]
  }
  const nodes = graphData.nodes.map((n) => {
    const cfg = { ...nodeConfigs[n.id] }
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

function zoomIn() {
  lf?.zoom(true)
}

function zoomOut() {
  lf?.zoom(false)
}

function fitView() {
  lf?.fitView()
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
}

function deleteSelectedNode() {
  if (!selectedNodeId.value) return
  lf!.deleteNode(selectedNodeId.value)
  delete nodeConfigs[selectedNodeId.value]
  selectedNodeId.value = ''
  nodeDialogVisible.value = false
}

function deleteSelectedEdge() {
  if (!selectedEdgeId.value) return
  lf!.deleteEdge(selectedEdgeId.value)
  selectedEdgeId.value = ''
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
    grid: true,
    edgeType: 'polyline',
    isSilentMode: false,
    stopZoomGraph: false,
    stopMoveGraph: false
  })
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
})

watch(
  () => (selectedNodeId.value ? nodeConfigs[selectedNodeId.value] : null),
  (cfg) => {
    if (cfg && lf) {
      lf.setProperties(selectedNodeId.value, { ...cfg })
    }
  },
  { deep: true }
)

// 节点配置变化时，按需拉取库/表/字段元数据
watch(
  () => (selectedNodeId.value ? nodeConfigs[selectedNodeId.value] : null),
  (cfg) => {
    if (!cfg || !cfg.__type) return
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
  lf?.destroy?.()
  lf = null
})
</script>

<style scoped>
.editor {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.editor-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #eef0f4;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.palette {
  width: 160px;
  border-right: 1px solid #eef0f4;
  padding: 12px;
  overflow-y: auto;
}

.palette-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 6px;
  border: 1px solid #eef0f4;
  border-radius: 6px;
  cursor: grab;
  font-size: 13px;
  color: #303133;
  transition: all 0.15s;
}

.palette-item:active {
  cursor: grabbing;
}

.palette-item:hover {
  border-color: var(--el-color-primary);
  background: #f7f6ff;
}

.palette-icon {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #fff;
}

.palette-hint {
  margin-top: 12px;
  font-size: 12px;
  color: #c0c4cc;
}

.canvas-wrap {
  flex: 1;
  position: relative;
}

.canvas {
  width: 100%;
  height: 100%;
}

.config {
  width: 280px;
  border-left: 1px solid #eef0f4;
  padding: 12px;
  overflow-y: auto;
}

.config-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.config-empty {
  color: #c0c4cc;
  font-size: 13px;
  text-align: center;
  margin-top: 40px;
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

.config-schedule-hint {
  margin-top: 16px;
  font-size: 12px;
  color: #c0c4cc;
  line-height: 1.6;
}

.cron-row {
  display: flex;
  gap: 8px;
}

.cron-nexts {
  margin-top: 8px;
  padding: 8px 10px;
  background: #f7f8fa;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
}

.cron-nexts-title {
  font-weight: 600;
  color: #303133;
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
</style>

<style>
/* 非 scoped：LogicFlow 的 HTML 节点内容由 innerHTML 注入，需全局样式 */
.flow-node {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  background: #fff;
  border: 1px solid #ccc;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  box-sizing: border-box;
}

.flow-node-icon {
  font-size: 20px;
}

.flow-node-meta {
  overflow: hidden;
}

.flow-node-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.flow-node-type {
  font-size: 11px;
  white-space: nowrap;
}

/* 节点编辑弹窗：表单区限高滚动，保证底部按钮始终可见 */
.node-dialog .el-dialog__body {
  max-height: 68vh;
  overflow-y: auto;
  padding-top: 8px;
}
</style>
