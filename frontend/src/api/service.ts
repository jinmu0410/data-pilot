import request from './request'

export interface QueryTemplateItem {
  id: number
  name: string
  code: string
  dataSourceCode: string
  dataSourceName: string
  status: string
  currentVersion: string
  publishVersion: string
  timeout: number
  description: string
  createUserId: number
  createTime: string
  updateTime: string
}

export interface QueryTemplateDetail extends QueryTemplateItem {
  template: string
  secret: string
}

export interface QueryTemplateForm {
  id?: number
  name: string
  dataSourceCode: string
  template: string
  timeout?: number
  status: string
  description?: string
}

export interface QueryTemplateListRequest {
  keyword?: string
  dataSourceCode?: string
  status?: string
}

export interface QueryTemplatePublishRequest {
  id: number
  secret?: string
  enableCache?: string
  enableLimiting?: string
  limitRate?: number
  limitRefreshInterval?: number
  limitTimeUnit?: string
  recordLog?: string
}

export interface QueryTemplatePublishResult {
  id: number
  name: string
  code: string
  version: string
  template: string
  dataSourceCode: string
  status: string
  secret: string
  timeout: number
  enableCache: string
  enableLimiting: string
  limitRate: number
  limitRefreshInterval: number
  limitTimeUnit: string
  recordLog: string
  createUserId: number
  createTime: string
}

export interface QueryTemplateTestRequest {
  id?: number
  dataSourceCode?: string
  template?: string
  params?: Record<string, unknown>
}

export interface QueryExecuteResult {
  columns: string[]
  rows: Record<string, unknown>[]
  rowCount: number
  truncated: boolean
  durationMs: number
}

export interface QueryLogItem {
  id: number
  templateCode: string
  templateName: string
  method: string
  status: string
  cost: number
  number: number
  hitCache: string
  ip: string
  requestId: string
  createTime: string
}

export interface QueryLogDetail extends QueryLogItem {
  workspaceCode: string
  requestArg: string
  responseArg: string
  exception: string
}

export interface QueryLogListRequest {
  templateCode?: string
  status?: string
}

export interface PageResult<T> {
  current: number
  size: number
  total: number
  records: T[]
}

export function listTemplate(
  query: QueryTemplateListRequest,
  current = 1,
  size = 10
): Promise<PageResult<QueryTemplateItem>> {
  return request.post('/service/api/list', {
    query,
    page: { current, size }
  }) as unknown as Promise<PageResult<QueryTemplateItem>>
}

export function getTemplateDetail(id: number): Promise<QueryTemplateDetail> {
  return request.post('/service/api/detail', { id }) as unknown as Promise<QueryTemplateDetail>
}

export function addTemplate(data: QueryTemplateForm): Promise<number> {
  return request.post('/service/api/add', data) as unknown as Promise<number>
}

export function updateTemplate(data: QueryTemplateForm): Promise<boolean> {
  return request.post('/service/api/update', data) as unknown as Promise<boolean>
}

export function deleteTemplate(id: number): Promise<boolean> {
  return request.post('/service/api/delete', { id }) as unknown as Promise<boolean>
}

export function publishTemplate(data: QueryTemplatePublishRequest): Promise<QueryTemplatePublishResult> {
  return request.post('/service/api/publish', data) as unknown as Promise<QueryTemplatePublishResult>
}

export function testTemplate(data: QueryTemplateTestRequest): Promise<QueryExecuteResult> {
  return request.post('/service/api/test', data) as unknown as Promise<QueryExecuteResult>
}

export function listLog(
  query: QueryLogListRequest,
  current = 1,
  size = 10
): Promise<PageResult<QueryLogItem>> {
  return request.post('/service/api/log/list', {
    query,
    page: { current, size }
  }) as unknown as Promise<PageResult<QueryLogItem>>
}

export function getLogDetail(id: number): Promise<QueryLogDetail> {
  return request.post('/service/api/log/detail', { id }) as unknown as Promise<QueryLogDetail>
}
