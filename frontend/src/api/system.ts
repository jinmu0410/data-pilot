import request from './request'

export interface SysUser {
  id?: number
  username: string
  password?: string
  nickname?: string
  status?: number
}

export function listUsers() {
  return request.get('/system/user/list')
}

export function createUser(data: SysUser) {
  return request.post('/system/user', data)
}
