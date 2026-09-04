import request from './request'
import type { PageResult } from './datasource'

export interface PermissionListRequest {
  name?: string
  code?: string
  status?: string
}

export interface PermissionItem {
  id: number
  name: string
  code: string
  createUserId: number
  createTime: string
  updateTime: string
  status: string
}

export function listPermission(
  query: PermissionListRequest,
  current = 1,
  size = 10
): Promise<PageResult<PermissionItem>> {
  return request.post('/permission/list', {
    query,
    page: { current, size }
  }) as unknown as Promise<PageResult<PermissionItem>>
}

export function getPermissionDetail(id: number): Promise<PermissionItem> {
  return request.post('/permission/detail', { id }) as unknown as Promise<PermissionItem>
}

export function addPermission(data: {
  name: string
  code: string
  status: string
}): Promise<boolean> {
  return request.post('/permission/add', data) as unknown as Promise<boolean>
}

export function updatePermission(data: {
  id: number
  name: string
  code: string
  status: string
}): Promise<boolean> {
  return request.post('/permission/update', data) as unknown as Promise<boolean>
}

export function deletePermission(id: number): Promise<boolean> {
  return request.post('/permission/delete', { id }) as unknown as Promise<boolean>
}
