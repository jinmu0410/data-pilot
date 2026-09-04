import request from './request'
import type { PageResult } from './datasource'

export interface RoleListRequest {
  name?: string
  code?: string
  status?: string
}

export interface RoleItem {
  id: number
  name: string
  code: string
  createUserId: number
  createTime: string
  updateTime: string
  status: string
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

export function listRole(
  query: RoleListRequest,
  current = 1,
  size = 10
): Promise<PageResult<RoleItem>> {
  return request.post('/role/list', {
    query,
    page: { current, size }
  }) as unknown as Promise<PageResult<RoleItem>>
}

export function getRoleDetail(id: number): Promise<RoleItem> {
  return request.post('/role/detail', { id }) as unknown as Promise<RoleItem>
}

export function addRole(data: { name: string; status: string }): Promise<boolean> {
  return request.post('/role/add', data) as unknown as Promise<boolean>
}

export function updateRole(data: {
  id: number
  name?: string
  code?: string
  status?: string
}): Promise<boolean> {
  return request.post('/role/update', data) as unknown as Promise<boolean>
}

export function deleteRole(id: number): Promise<boolean> {
  return request.post('/role/delete', { id }) as unknown as Promise<boolean>
}

// 角色的权限列表
export function listRolePermission(roleId: number): Promise<PermissionItem[]> {
  return request.post('/role/permission/list', { id: roleId }) as unknown as Promise<PermissionItem[]>
}

// 更新角色权限
export function upsertRolePermission(roleId: number, permissionIds: number[]): Promise<boolean> {
  return request.post('/role/permission/upsert', { roleId, permissionIds }) as unknown as Promise<boolean>
}

// 用户的角色列表
export function listUserRole(userId: number): Promise<RoleItem[]> {
  return request.post('/user/role/list', { id: userId }) as unknown as Promise<RoleItem[]>
}

// 更新用户角色（status: ENABLE/DISABLE）
export function upsertUserRole(userId: number, roleId: number, status: string): Promise<boolean> {
  return request.post('/user/role/upsert', { userId, roleId, status }) as unknown as Promise<boolean>
}
