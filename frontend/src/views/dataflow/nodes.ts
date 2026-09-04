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

const SYNC_FIELDS: FieldDef[] = [
  { key: 'sourceDataSourceCode', label: '源数据源', type: 'datasource', required: true },
  { key: 'sourceSchema', label: '源库', type: 'input', placeholder: '可选，如 test', default: '' },
  { key: 'sourceTable', label: '源表', type: 'input', required: true, placeholder: '如 t' },
  { key: 'targetDataSourceCode', label: '目标数据源', type: 'datasource', required: true },
  { key: 'targetSchema', label: '目标库', type: 'input', placeholder: '可选，如 test', default: '' },
  { key: 'targetTable', label: '目标表', type: 'input', required: true, placeholder: '如 t' },
  { key: 'fieldMapping', label: '字段映射', type: 'fieldMapping', default: [] },
  { key: 'timeout', label: '超时(秒)', type: 'number', default: 30 }
]

// DataX 同步（DolphinScheduler 风格：SQL 语句 + 目标表 + 字段映射 + 前置/后置 SQL + 限流）
const DATAX_FIELDS: FieldDef[] = [
  { key: 'sourceDataSourceCode', label: '数据源', type: 'datasource', required: true },
  { key: 'sourceSchema', label: '源库', type: 'schema', dependsOn: { datasource: 'sourceDataSourceCode' } },
  { key: 'sourceTable', label: '源表', type: 'table', dependsOn: { datasource: 'sourceDataSourceCode', schema: 'sourceSchema' } },
  { key: 'targetDataSourceCode', label: '目标数据源', type: 'datasource', required: true },
  { key: 'targetSchema', label: '目标库', type: 'schema', dependsOn: { datasource: 'targetDataSourceCode' } },
  { key: 'targetTable', label: '目标表', type: 'table', required: true, dependsOn: { datasource: 'targetDataSourceCode', schema: 'targetSchema' } },
  {
    key: 'fieldMapping',
    label: '字段映射',
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
  { key: 'sqlText', label: 'SQL 语句', type: 'sql', required: true, default: '', height: '240px', placeholder: '抽取数据的 SQL，可用 as 别名映射目标列' },
  { key: 'preSql', label: '目标库前置 SQL', type: 'collapsibleSql', default: '', height: '150px', placeholder: '同步前在目标库执行' },
  { key: 'postSql', label: '目标库后置 SQL', type: 'collapsibleSql', default: '', height: '150px', placeholder: '同步后在目标库执行' },
  { key: 'jobSpeedByte', label: '限流(字节数)', type: 'number' },
  { key: 'jobSpeedRecord', label: '限流(记录数)', type: 'number' },
  { key: 'fetchSize', label: '读取批次', type: 'number' },
  {
    key: 'writeMode',
    label: '写入模式',
    type: 'select',
    default: 'insert',
    options: [
      { label: 'insert（插入）', value: 'insert' },
      { label: 'replace（替换）', value: 'replace' },
      { label: 'update（更新）', value: 'update' },
      { label: 'append（追加）', value: 'append' },
      { label: 'truncate（清空后写入）', value: 'truncate' }
    ]
  },
  { key: 'batchSize', label: '写入批次', type: 'number' },
  { key: 'channel', label: '并发度', type: 'number', default: 3 },
  { key: 'timeout', label: '超时(秒)', type: 'number', default: 30 }
]

export const NODE_TYPES: NodeTypeDef[] = [
  {
    type: 'SQL',
    label: 'SQL 任务',
    icon: '🧮',
    color: '#409eff',
    fields: [
      { key: 'datasourceCode', label: '数据源', type: 'datasource', required: true },
      {
        key: 'sqlType',
        label: 'SQL 类型',
        type: 'select',
        default: 'QUERY',
        options: [
          { label: '查询', value: 'QUERY' },
          { label: '非查询', value: 'NON_QUERY' }
        ]
      },
      { key: 'sqlText', label: 'SQL 语句', type: 'sql', required: true, default: '', height: '300px' },
      { key: 'sqlParams', label: 'SQL 参数', type: 'input', default: '', placeholder: 'key1=value1;key2=value2，SQL 内 ${key} 替换' },
      { key: 'preSql', label: '前置 SQL', type: 'collapsibleSql', default: '', placeholder: '主 SQL 前执行，如 SET 会话变量、use db', height: '150px' },
      { key: 'postSql', label: '后置 SQL', type: 'collapsibleSql', default: '', placeholder: '主 SQL 后执行，如清理', height: '150px' },
      { key: 'timeout', label: '超时(秒)', type: 'number', default: 30 }
    ]
  },
  {
    type: 'DATAX',
    label: 'DataX 同步',
    icon: '🔄',
    color: '#6d5ef2',
    fields: DATAX_FIELDS
  },
  {
    type: 'SEATUNNEL',
    label: 'SeaTunnel 同步',
    icon: '🌊',
    color: '#67c23a',
    fields: SYNC_FIELDS
  },
  {
    type: 'PYTHON',
    label: 'Python 脚本',
    icon: '🐍',
    color: '#e6a23c',
    fields: [
      { key: 'script', label: '脚本', type: 'textarea', required: true, default: '' },
      { key: 'timeout', label: '超时(秒)', type: 'number', default: 30 }
    ]
  },
  {
    type: 'SHELL',
    label: 'Shell 脚本',
    icon: '⌨️',
    color: '#909399',
    fields: [
      { key: 'script', label: '脚本', type: 'textarea', required: true, default: '' },
      { key: 'timeout', label: '超时(秒)', type: 'number', default: 30 }
    ]
  }
]

export function getNodeType(type: string): NodeTypeDef | undefined {
  return NODE_TYPES.find((t) => t.type === type)
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
