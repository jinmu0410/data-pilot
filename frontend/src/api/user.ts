import request from './request'
import type { PageResult } from './datasource'

export interface UserListRequest {
  username?: string
  email?: string
  status?: string
}

export interface UserItem {
  id: number
  username: string
  email: string
  phone: string
  avatar: string
  createUserId: number
  createTime: string
  updateTime: string
  status: string
  online: boolean
}

export interface UserDetail {
  id: number
  username: string
  email: string
  phone: string
  avatar: string
  gender: string
  createUserId: number
  status: string
  description: string
  createTime: string
  updateTime: string
}

export interface UserAddRequest {
  username: string
  password: string
  phone?: string
  email: string
  avatar?: string
  status: string
}

export interface UserUpdateRequest {
  id: number
  password?: string
  email?: string
  phone?: string
  avatar?: string
  status?: string
  gender?: string
}

export function listUser(
  query: UserListRequest,
  current = 1,
  size = 10
): Promise<PageResult<UserItem>> {
  return request.post('/user/list', {
    query,
    page: { current, size }
  }) as unknown as Promise<PageResult<UserItem>>
}

export function getUserDetail(id: number): Promise<UserDetail> {
  return request.post('/user/detail', { id }) as unknown as Promise<UserDetail>
}

export function addUser(data: UserAddRequest): Promise<boolean> {
  return request.post('/user/add', data) as unknown as Promise<boolean>
}

export function updateUser(data: UserUpdateRequest): Promise<boolean> {
  return request.post('/user/update', data) as unknown as Promise<boolean>
}

export function deleteUser(id: number): Promise<boolean> {
  return request.post('/user/delete', { id }) as unknown as Promise<boolean>
}

export function resetPassword(id: number, password: string): Promise<boolean> {
  return request.post('/user/resetPassword', { id, password }) as unknown as Promise<boolean>
}

export function changePassword(oldPassword: string, newPassword: string): Promise<boolean> {
  return request.post('/user/changePassword', { oldPassword, newPassword }) as unknown as Promise<boolean>
}
