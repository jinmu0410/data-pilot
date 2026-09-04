import request from './request'

export const DATASOURCE_TYPES = ['MySQL', 'TiDB', 'Doris', 'PostgreSQL', 'Kafka', 'Elastic'] as const

export interface DataSourceListRequest {
  name?: string
  code?: string
  type?: string
  status?: string
}

export interface DataSourceItem {
  id: number
  name: string
  code: string
  type: string
  url: string
  username: string
  driver: string
  status: string
  feNodes: string
  beNodes: string
  healthCheck: string
  maskColumn: string
  createUserId: number
  createTime: string
  updateTime: string
}

export interface DataSourceDetail {
  id: number
  name: string
  code: string
  type: string
  url: string
  username: string
  password: string
  driver: string
  maxPoolSize: number
  status: string
  feNodes: string
  beNodes: string
  partitioningAlgorithm: string
  healthCheck: string
  maskColumn: unknown
  description: string
  createUserId: number
  createTime: string
  updateTime: string
}

export interface MarkColumn {
  columnName: string
  maskType: string
}

export interface DataSourceForm {
  id?: number
  name: string
  type: string
  url?: string
  driver?: string
  username?: string
  password?: string
  maxPoolSize?: number
  status: string
  feNodes?: string
  beNodes?: string
  partitioningAlgorithm?: string
  healthCheck?: string
  maskColumn?: MarkColumn[]
  description?: string
}

export interface PageResult<T> {
  current: number
  size: number
  total: number
  records: T[]
}

export interface SchemaTableMap {
  key: string
  label: string
  tag: string
  children: { schema: string; key: string; label: string }[]
}

export interface TableColumn {
  name: string
  comment: string
  type: string
  primaryKey: boolean
  notNull: boolean
  defaultValue: string
  maxLength: number
}

export interface TableIndex {
  name: string
  unique: boolean
  columns: string[]
}

export interface TableDetail {
  createTime: string
  comment: string
  columns: TableColumn[]
  indexes: TableIndex[]
}

export function listDataSource(
  query: DataSourceListRequest,
  current = 1,
  size = 10
): Promise<PageResult<DataSourceItem>> {
  return request.post('/datasource/list', {
    query,
    page: { current, size }
  }) as unknown as Promise<PageResult<DataSourceItem>>
}

export function getDataSourceDetail(id: number): Promise<DataSourceDetail> {
  return request.post('/datasource/detail', { id }) as unknown as Promise<DataSourceDetail>
}

export function addDataSource(data: DataSourceForm): Promise<number> {
  return request.post('/datasource/add', data) as unknown as Promise<number>
}

export function updateDataSource(data: DataSourceForm): Promise<boolean> {
  return request.post('/datasource/update', data) as unknown as Promise<boolean>
}

export function deleteDataSource(id: number): Promise<boolean> {
  return request.post('/datasource/delete', { id }) as unknown as Promise<boolean>
}

export function testDataSource(data: DataSourceForm): Promise<boolean> {
  return request.post('/datasource/test', data) as unknown as Promise<boolean>
}

// 元数据：库/表树
export function listSchemaTable(id: number): Promise<SchemaTableMap[]> {
  return request.post('/datasource/listSchemaTable', { id }) as unknown as Promise<SchemaTableMap[]>
}

// 元数据：表结构详情
export function tableDetail(id: number, schema: string, table: string): Promise<TableDetail> {
  return request.post('/datasource/tableDetail', { id, schema, table }) as unknown as Promise<TableDetail>
}
