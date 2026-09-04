import request from './request'
import type { PageResult } from './datasource'

export interface LoginLogListRequest {
  requestId?: string
  userId?: number
  username?: string
  ip?: string
  startCreateTime?: string
  endCreateTime?: string
}

export interface LoginLogItem {
  id: number
  requestId: string
  user: { id: number; username: string; email: string; avatar: string }
  ip: string
  browser: string
  os: string
  platform: string
  createTime: string
}

export interface LoginLogDetail {
  id: number
  requestId: string
  userId: number
  username: string
  ip: string
  browser: string
  os: string
  userAgent: string
  platform: string
  createTime: string
}

export function listLoginLog(
  query: LoginLogListRequest,
  current = 1,
  size = 10
): Promise<PageResult<LoginLogItem>> {
  return request.post('/user/login/log/list', {
    query,
    page: { current, size }
  }) as unknown as Promise<PageResult<LoginLogItem>>
}

export function getLoginLogDetail(id: number): Promise<LoginLogDetail> {
  return request.post('/user/login/log/detail', { id }) as unknown as Promise<LoginLogDetail>
}

export function deleteLoginLog(id: number): Promise<boolean> {
  return request.post('/user/login/log/delete', { id }) as unknown as Promise<boolean>
}
