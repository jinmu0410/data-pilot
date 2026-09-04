import request from './request'

export interface LoginRequest {
  account: string
  password: string
}

export interface WorkspaceData {
  id: number
  name: string
  code: string
  secret: string
  isWorkspaceAdmin: boolean | null
}

export interface UserData {
  id: number
  username: string
  email: string
  [key: string]: unknown
}

export function login(data: LoginRequest): Promise<string> {
  return request.post('/login', data) as unknown as Promise<string>
}

export function logout(): Promise<boolean> {
  return request.post('/logout') as unknown as Promise<boolean>
}

export function getUserInfo(): Promise<UserData> {
  return request.post('/user/getUserInfo') as unknown as Promise<UserData>
}

export function myWorkspaces(): Promise<WorkspaceData[]> {
  return request.post('/user/workspace/my') as unknown as Promise<WorkspaceData[]>
}
