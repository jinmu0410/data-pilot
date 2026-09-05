import request from './request'

export interface SystemConfigItem {
  id: number
  configKey: string
  configValue: string
  description: string
  createTime?: string
  updateTime?: string
}

export function listConfig(): Promise<SystemConfigItem[]> {
  return request.post('/system/config/list', {}) as unknown as Promise<SystemConfigItem[]>
}

export function addConfig(data: {
  configKey: string
  configValue: string
  description?: string
}): Promise<boolean> {
  return request.post('/system/config/add', data) as unknown as Promise<boolean>
}

export function updateConfig(data: {
  id: number
  configKey: string
  configValue: string
  description?: string
}): Promise<boolean> {
  return request.post('/system/config/update', data) as unknown as Promise<boolean>
}

export function deleteConfig(id: number): Promise<boolean> {
  return request.post('/system/config/delete', { id }) as unknown as Promise<boolean>
}
