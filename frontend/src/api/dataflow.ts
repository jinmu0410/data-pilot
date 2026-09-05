import request from './request'
import type { PageResult } from './datasource'

export interface DataFlowListItem {
  id: number
  name: string
  code: string
  status: string
  description: string
  icon: string
  currentVersion: string
  publishVersion: string
  publishId: number | null
  createUserId: number
  users: unknown[]
  createTime: string
  updateTime: string
  flowErrors: unknown[]
  flowHeartbeats: unknown[]
}

export interface DataFlowListQuery {
  keyword?: string
  name?: string
  code?: string
  status?: string
}

export interface DesignNode {
  id: string
  type: string
  properties: string | Record<string, any>
}

export interface DesignEdge {
  id: string
  sourceNodeId: string
  targetNodeId: string
  properties: { order: number }
}

export interface DataFlowDetail {
  id: number
  name: string
  code: string
  status: string
  description: string
  design: { nodes: DesignNode[]; edges: DesignEdge[] } | null
  currentVersion: string
  publishVersion: string
  enableAlarm: string
  enableMonitor: string
  runStrategy: string
  instanceNumber: number
  specifyInstances: unknown
  cron: string
  nextExecTime: string
  icon: string
  createUserId: number
  createTime: string
  updateTime: string
}

export function listDataFlow(
  query: DataFlowListQuery,
  current = 1,
  size = 10
): Promise<PageResult<DataFlowListItem>> {
  return request.post('/dataflow/list', {
    query,
    page: { current, size }
  }) as unknown as Promise<PageResult<DataFlowListItem>>
}

export function createDataFlow(data: {
  name: string
  icon: string
  status: string
  description?: string
}): Promise<{ id: number; code: string }> {
  return request.post('/dataflow/create', data) as unknown as Promise<{ id: number; code: string }>
}

export function getDataFlowDetail(id: number): Promise<DataFlowDetail> {
  return request.post('/dataflow/detail', { id }) as unknown as Promise<DataFlowDetail>
}

export function updateDataFlow(data: {
  id: number
  name?: string
  icon?: string
  status?: string
  description?: string
  design?: string
  temporarily?: boolean
  enableAlarm?: string
  enableMonitor?: string
  runStrategy?: string
  instanceNumber?: number
  specifyInstances?: string[]
  cron?: string
}): Promise<boolean> {
  return request.post('/dataflow/update', data) as unknown as Promise<boolean>
}

export function deleteDataFlow(id: number): Promise<boolean> {
  return request.post('/dataflow/delete', { id }) as unknown as Promise<boolean>
}

export function startDataFlow(id: number): Promise<boolean> {
  return request.post('/dataflow/start', { id }) as unknown as Promise<boolean>
}

export function stopDataFlow(id: number): Promise<boolean> {
  return request.post('/dataflow/stop', { id }) as unknown as Promise<boolean>
}

export function publishDataFlow(id: number, publishDescription: string): Promise<boolean> {
  return request.post('/dataflow/publish', { id, publishDescription }) as unknown as Promise<boolean>
}

export interface DataFlowPublishItem {
  id: number
  name: string
  code: string
  workspaceId: number
  status: string
  publishDescription: string
  description: string
  icon: string
  version: string
  createUserId: number
  createTime: string
  updateTime: string
  flowId: number
}

export interface DataFlowPublishDetail {
  id: number
  name: string
  code: string
  status: string
  description: string
  design: { nodes: DesignNode[]; edges: DesignEdge[] } | null
  currentVersion: string
  publishVersion: string
  enableAlarm: string
  enableMonitor: string
  icon: string
  runStrategy: string
  instanceNumber: number
  specifyInstances: string[] | null
  createUserId: number
  createTime: string
  updateTime: string
}

// 发布历史（query 为数据流 code）
export function listPublishHistory(
  code: string,
  current = 1,
  size = 10
): Promise<PageResult<DataFlowPublishItem>> {
  return request.post('/dataflow/publish/historyList', {
    query: code,
    page: { current, size }
  }) as unknown as Promise<PageResult<DataFlowPublishItem>>
}

export function getPublishDetail(id: number): Promise<DataFlowPublishDetail> {
  return request.post('/dataflow/publish/detail', { id }) as unknown as Promise<DataFlowPublishDetail>
}

export function deletePublish(id: number): Promise<boolean> {
  return request.post('/dataflow/publish/delete', { id }) as unknown as Promise<boolean>
}

// 回滚到指定发布版本（传发布版本 id）
export function rollbackDataFlow(publishId: number): Promise<boolean> {
  return request.post('/dataflow/rollback', { id: publishId }) as unknown as Promise<boolean>
}

// 手动运行任务流
export function runDataFlow(id: number, failureStrategy?: string): Promise<number> {
  return request.post('/dataflow/run', { id, failureStrategy }) as unknown as Promise<number>
}

export interface FlowInstanceItem {
  id: number
  flowId: number
  flowCode: string
  flowName: string
  triggerType: string
  failureStrategy: string
  status: string
  errorMsg: string
  durationMs: number
  startTime: string
  endTime: string
  createTime: string
}

export interface FlowInstanceQuery {
  flowId?: number
  keyword?: string
  status?: string
  triggerType?: string
}

export interface FlowInstanceNode {
  id: number
  taskId: number
  taskCode: string
  taskName: string
  taskType: string
  nodeId: string
  nodeName: string
  triggerType: string
  status: string
  taskParams: string
  durationMs: number
  rowCount: number
  columns: string[]
  rows: string[][]
  truncated: boolean
  logContent: string
  errorMsg: string
  startTime: string
  endTime: string
  createTime: string
}

export interface FlowInstanceDetail {
  id: number
  flowId: number
  flowCode: string
  flowName: string
  triggerType: string
  failureStrategy: string
  status: string
  errorMsg: string
  durationMs: number
  startTime: string
  endTime: string
  createTime: string
  nodes: FlowInstanceNode[]
}

export function listFlowInstance(
  query: FlowInstanceQuery,
  current = 1,
  size = 10
): Promise<PageResult<FlowInstanceItem>> {
  return request.post('/dataflow/instance/list', {
    query,
    page: { current, size }
  }) as unknown as Promise<PageResult<FlowInstanceItem>>
}

export function getFlowInstanceDetail(id: number): Promise<FlowInstanceDetail> {
  return request.post('/dataflow/instance/detail', { id }) as unknown as Promise<FlowInstanceDetail>
}

export interface EngineConfigPreview {
  engine: string
  configContent: string
  command: string[]
}

// 预览同步引擎（DataX/SeaTunnel）最终生成的配置
export function previewEngineConfig(
  taskType: string,
  node: Record<string, any>
): Promise<EngineConfigPreview> {
  return request.post('/dataflow/config/preview', { taskType, node }) as unknown as Promise<EngineConfigPreview>
}
