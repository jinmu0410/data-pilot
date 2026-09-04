import request from './request'

export function cronValid(cron: string): Promise<boolean> {
  return request.post('/cron/valid', { param: cron }) as unknown as Promise<boolean>
}

export function cronNexts(cron: string): Promise<string[]> {
  return request.post('/cron/nexts', { param: cron }) as unknown as Promise<string[]>
}
