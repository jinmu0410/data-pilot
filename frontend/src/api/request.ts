import axios from 'axios'
import { ElMessage } from 'element-plus'

export const TOKEN_KEY = 'dp_token'
export const WORKSPACE_KEY = 'dp_workspace'

const request = axios.create({
  baseURL: '/dp-web',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = token
  }
  const workspace = localStorage.getItem(WORKSPACE_KEY)
  if (workspace) {
    config.headers['X-Workspace'] = workspace
  }
  return config
})

// 后端统一异常也返回 HTTP 200，用 state/code 区分，认证失败时跳登录
const AUTH_CODES = [99990402, 10010004, 10011039]

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.state === 'SUCCESS') {
      return res.data
    }
    if (res && AUTH_CODES.includes(res.code)) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(WORKSPACE_KEY)
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || '登录已失效'))
    }
    ElMessage.error(res?.message || '请求失败')
    return Promise.reject(new Error(res?.message || '请求失败'))
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
