import request from './request'
import type { PageResult } from './datasource'

export interface WorkspaceListRequest {
  name?: string
  code?: string
  status?: string
}

export interface WorkspaceItem {
  id: number
  name: string
  code: string
  status: string
  createUserId: number
  createTime: string
  updateTime: string
}

export interface WorkspaceDetail {
  id: number
  name: string
  code: string
  secret: string
  status: string
  createUserId: number
  createTime: string
  updateTime: string
}

export interface WorkspaceMember {
  id: number
  username: string
  email: string
  avatar: string
  status: string
  gender: string
  phone: string
  description: string
}

export function listWorkspace(
  query: WorkspaceListRequest,
  current = 1,
  size = 10
): Promise<PageResult<WorkspaceItem>> {
  return request.post('/workspace/list', {
    query,
    page: { current, size }
  }) as unknown as Promise<PageResult<WorkspaceItem>>
}

export function getWorkspaceDetail(id: number): Promise<WorkspaceDetail> {
  return request.post('/workspace/detail', { id }) as unknown as Promise<WorkspaceDetail>
}

export function addWorkspace(data: {
  name?: string
  code?: string
  secret?: string
  status: string
}): Promise<boolean> {
  return request.post('/workspace/add', data) as unknown as Promise<boolean>
}

export function updateWorkspace(data: {
  id: number
  name?: string
  secret?: string
  status?: string
}): Promise<boolean> {
  return request.post('/workspace/update', data) as unknown as Promise<boolean>
}

export function deleteWorkspace(id: number): Promise<boolean> {
  return request.post('/workspace/delete', { id }) as unknown as Promise<boolean>
}

// 工作空间成员（type: 1=管理员 0=普通用户）
export function listWorkspaceMembers(
  workspaceId: number,
  type: number,
  username?: string,
  current = 1,
  size = 10
): Promise<PageResult<WorkspaceMember>> {
  return request.post('/user/workspace/members', {
    query: { workspaceId, type, username },
    page: { current, size }
  }) as unknown as Promise<PageResult<WorkspaceMember>>
}

// 不在该工作空间的用户
export function listNotInMembers(
  workspaceId: number,
  username?: string,
  current = 1,
  size = 10
): Promise<PageResult<WorkspaceMember>> {
  return request.post('/user/workspace/notInMembers', {
    query: { workspaceId, username },
    page: { current, size }
  }) as unknown as Promise<PageResult<WorkspaceMember>>
}

export function bindMember(userId: number, workspaceId: number): Promise<boolean> {
  return request.post('/user/workspace/bindMember', { userId, workspaceId }) as unknown as Promise<boolean>
}

export function deleteMember(workspaceId: number, userId: number): Promise<boolean> {
  return request.post('/user/workspace/deleteMember', { workspaceId, userId }) as unknown as Promise<boolean>
}

// type: 1=设为管理员 0=设为普通用户
export function permissionTransfer(workspaceId: number, userId: number, type: number): Promise<boolean> {
  return request.post('/user/workspace/permissionTransfer', { workspaceId, userId, type }) as unknown as Promise<boolean>
}
