export interface FieldOption {
  label: string
  value: string
}

export type FieldType =
  | 'input'
  | 'textarea'
  | 'number'
  | 'select'
  | 'datasource'
  | 'tags'
  | 'sql'
  | 'fieldMapping'
  | 'collapsibleSql'
  | 'schema'
  | 'table'
  | 'columns'
  | 'fieldMap'

export interface FieldDepends {
  /** 依赖的数据源字段 key（值为数据源 code） */
  datasource?: string
  /** 依赖的库字段 key */
  schema?: string
  /** 依赖的表字段 key */
  table?: string
  /** 字段映射目标侧：目标数据源字段 key */
  targetDatasource?: string
  /** 字段映射目标侧：目标库字段 key */
  targetSchema?: string
  /** 字段映射目标侧：目标表字段 key */
  targetTable?: string
}

export interface FieldDef {
  key: string
  label: string
  type: FieldType
  required?: boolean
  placeholder?: string
  options?: FieldOption[]
  default?: unknown
  height?: string
  dependsOn?: FieldDepends
}

export interface NodeTypeDef {
  type: string
  label: string
  icon: string
  color: string
  fields: FieldDef[]
}

export interface NodeConfig {
  name: string
  description?: string
  [key: string]: any
}

export interface FieldMappingRow {
  source: string
  target: string
}

export type TranslateFn = (key: string) => string

const SYNC_FIELDS: FieldDef[] = [
  { key: 'sourceDataSourceCode', label: 'node.field.sourceDataSource', type: 'datasource', required: true },
  { key: 'sourceSchema', label: 'node.field.sourceSchema', type: 'schema', default: '' },
  { key: 'sourceTable', label: 'node.field.sourceTable', type: 'table', required: true },
  { key: 'readMode', label: 'node.field.readMode', type: 'input', default: 'table' },
  { key: 'sqlText', label: 'node.field.readSql', type: 'sql', default: '' },
  { key: 'whereCondition', label: 'node.field.whereCondition', type: 'input', default: '' },
  { key: 'partitionColumn', label: 'node.field.partitionColumn', type: 'input', default: '' },
  { key: 'partitionNum', label: 'node.field.partitionNum', type: 'number', default: 10 },
  { key: 'targetDataSourceCode', label: 'node.field.targetDataSource', type: 'datasource', required: true },
  { key: 'targetSchema', label: 'node.field.targetSchema', type: 'schema', default: '' },
  { key: 'targetTable', label: 'node.field.targetTable', type: 'table', required: true },
  { key: 'fieldMapping', label: 'node.field.fieldMapping', type: 'fieldMapping', default: [] },
  { key: 'sinkWriteStrategy', label: 'node.field.sinkWriteStrategy', type: 'input', default: 'generated' },
  { key: 'sinkQuery', label: 'node.field.sinkQuery', type: 'sql', default: '' },
  { key: 'schemaSaveMode', label: 'node.field.schemaSaveMode', type: 'input', default: 'ERROR_WHEN_SCHEMA_NOT_EXIST' },
  { key: 'dataSaveMode', label: 'node.field.dataSaveMode', type: 'input', default: 'APPEND_DATA' },
  { key: 'customSql', label: 'node.field.customSql', type: 'sql', default: '' },
  { key: 'primaryKeys', label: 'node.field.primaryKeys', type: 'tags', default: [] },
  { key: 'fetchSize', label: 'node.field.fetchSize', type: 'number' },
  { key: 'batchSize', label: 'node.field.batchSize', type: 'number', default: 1000 },
  { key: 'parallelism', label: 'node.field.parallelism', type: 'number', default: 1 },
  { key: 'retryTimes', label: 'node.field.retryTimes', type: 'number', default: 0 },
  { key: 'timeout', label: 'node.field.timeout', type: 'number', default: 600 }
]

// DataX 同步（DolphinScheduler 风格：SQL 语句 + 目标表 + 字段映射 + 前置/后置 SQL + 限流）
const DATAX_FIELDS: FieldDef[] = [
  { key: 'sourceDataSourceCode', label: 'node.field.dataSource', type: 'datasource', required: true },
  { key: 'sourceSchema', label: 'node.field.sourceSchema', type: 'schema', dependsOn: { datasource: 'sourceDataSourceCode' } },
  { key: 'sourceTable', label: 'node.field.sourceTable', type: 'table', dependsOn: { datasource: 'sourceDataSourceCode', schema: 'sourceSchema' } },
  { key: 'targetDataSourceCode', label: 'node.field.targetDataSource', type: 'datasource', required: true },
  { key: 'targetSchema', label: 'node.field.targetSchema', type: 'schema', dependsOn: { datasource: 'targetDataSourceCode' } },
  { key: 'targetTable', label: 'node.field.targetTable', type: 'table', required: true, dependsOn: { datasource: 'targetDataSourceCode', schema: 'targetSchema' } },
  {
    key: 'fieldMapping',
    label: 'node.field.fieldMapping',
    type: 'fieldMap',
    default: [],
    dependsOn: {
      datasource: 'sourceDataSourceCode',
      schema: 'sourceSchema',
      table: 'sourceTable',
      targetDatasource: 'targetDataSourceCode',
      targetSchema: 'targetSchema',
      targetTable: 'targetTable'
    }
  },
  { key: 'sqlText', label: 'node.field.sqlText', type: 'sql', required: true, default: '', height: '240px', placeholder: 'node.placeholder.sqlText' },
  { key: 'preSql', label: 'node.field.targetPreSql', type: 'collapsibleSql', default: '', height: '150px', placeholder: 'node.placeholder.preSql' },
  { key: 'postSql', label: 'node.field.targetPostSql', type: 'collapsibleSql', default: '', height: '150px', placeholder: 'node.placeholder.postSql' },
  { key: 'jobSpeedByte', label: 'node.field.jobSpeedByte', type: 'number' },
  { key: 'jobSpeedRecord', label: 'node.field.jobSpeedRecord', type: 'number' },
  { key: 'fetchSize', label: 'node.field.fetchSize', type: 'number' },
  {
    key: 'writeMode',
    label: 'node.field.writeMode',
    type: 'select',
    default: 'insert',
    options: [
      { label: 'node.option.writeInsert', value: 'insert' },
      { label: 'node.option.writeReplace', value: 'replace' },
      { label: 'node.option.writeUpdate', value: 'update' }
    ]
  },
  { key: 'batchSize', label: 'node.field.batchSize', type: 'number' },
  { key: 'channel', label: 'node.field.channel', type: 'number', default: 3 },
  { key: 'timeout', label: 'node.field.timeout', type: 'number', default: 30 }
]

export const NODE_TYPES: NodeTypeDef[] = [
  {
    type: 'SQL',
    label: 'node.sql',
    icon: '🧮',
    color: '#409eff',
    fields: [
      { key: 'datasourceCode', label: 'node.field.dataSource', type: 'datasource', required: true },
      {
        key: 'sqlType',
        label: 'node.field.sqlType',
        type: 'select',
        default: 'QUERY',
        options: [
          { label: 'node.option.query', value: 'QUERY' },
          { label: 'node.option.nonQuery', value: 'NON_QUERY' }
        ]
      },
      { key: 'sqlText', label: 'node.field.sqlText', type: 'sql', required: true, default: '', height: '300px' },
      { key: 'sqlParams', label: 'node.field.sqlParams', type: 'input', default: '', placeholder: 'node.placeholder.sqlParams' },
      { key: 'preSql', label: 'node.field.preSql', type: 'collapsibleSql', default: '', placeholder: 'node.placeholder.preSqlMain', height: '150px' },
      { key: 'postSql', label: 'node.field.postSql', type: 'collapsibleSql', default: '', placeholder: 'node.placeholder.postSqlMain', height: '150px' },
      { key: 'timeout', label: 'node.field.timeout', type: 'number', default: 30 }
    ]
  },
  {
    type: 'DATAX',
    label: 'node.datax',
    icon: '🔄',
    color: '#6d5ef2',
    fields: DATAX_FIELDS
  },
  {
    type: 'SEATUNNEL',
    label: 'node.seatunnel',
    icon: '🌊',
    color: '#67c23a',
    fields: SYNC_FIELDS
  },
  {
    type: 'PYTHON',
    label: 'node.python',
    icon: '🐍',
    color: '#e6a23c',
    fields: [
      { key: 'script', label: 'node.field.script', type: 'textarea', required: true, default: '' },
      { key: 'timeout', label: 'node.field.timeout', type: 'number', default: 30 }
    ]
  },
  {
    type: 'SHELL',
    label: 'node.shell',
    icon: '⌨️',
    color: '#909399',
    fields: [
      { key: 'script', label: 'node.field.script', type: 'textarea', required: true, default: '' },
      { key: 'timeout', label: 'node.field.timeout', type: 'number', default: 30 }
    ]
  }
]

export function getNodeType(type: string): NodeTypeDef | undefined {
  return NODE_TYPES.find((t) => t.type === type)
}

function translateField(field: FieldDef, t: TranslateFn): FieldDef {
  return {
    ...field,
    label: t(field.label),
    placeholder: field.placeholder ? t(field.placeholder) : undefined,
    options: field.options?.map((opt) => ({ ...opt, label: t(opt.label) }))
  }
}

function translateNodeTypeDef(def: NodeTypeDef, t: TranslateFn): NodeTypeDef {
  return { ...def, label: t(def.label), fields: def.fields.map((field) => translateField(field, t)) }
}

export function translateNodeType(type: string, t: TranslateFn): NodeTypeDef | undefined {
  const def = getNodeType(type)
  return def ? translateNodeTypeDef(def, t) : undefined
}

export function translateNodeTypes(t: TranslateFn): NodeTypeDef[] {
  return NODE_TYPES.map((def) => translateNodeTypeDef(def, t))
}

export function buildDefaultConfig(type: string, name: string): NodeConfig {
  const def = getNodeType(type)
  const config: NodeConfig = { name }
  if (def) {
    for (const field of def.fields) {
      if (field.default !== undefined) {
        config[field.key] = Array.isArray(field.default) ? [...field.default] : field.default
      }
    }
  }
  return config
}
