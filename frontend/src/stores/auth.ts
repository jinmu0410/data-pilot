import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getUserInfo, myWorkspaces } from '../api/auth'
import type { UserData, WorkspaceData } from '../api/auth'
import { TOKEN_KEY, WORKSPACE_KEY } from '../api/request'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null as UserData | null,
    workspace: null as WorkspaceData | null,
    workspaces: [] as WorkspaceData[]
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    workspaceId: (state) => state.workspace?.id ?? null
  },
  actions: {
    async login(account: string, password: string) {
      const token = await loginApi({ account, password })
      this.token = token
      localStorage.setItem(TOKEN_KEY, token)
      await this.loadUserAndWorkspace()
    },
    async loadUserAndWorkspace() {
      const user = await getUserInfo()
      this.user = user
      const workspaces = await myWorkspaces()
      this.workspaces = workspaces ?? []
      // 优先恢复上次选择的工作空间，否则默认第一个
      const savedId = Number(localStorage.getItem(WORKSPACE_KEY))
      this.workspace = this.workspaces.find((w) => w.id === savedId) ?? this.workspaces[0] ?? null
      if (this.workspace) {
        localStorage.setItem(WORKSPACE_KEY, String(this.workspace.id))
      }
    },
    switchWorkspace(workspace: WorkspaceData) {
      this.workspace = workspace
      localStorage.setItem(WORKSPACE_KEY, String(workspace.id))
    },
    async logout() {
      try {
        await logoutApi()
      } catch {
        // 忽略登出失败
      }
      this.reset()
    },
    reset() {
      this.token = ''
      this.user = null
      this.workspace = null
      this.workspaces = []
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(WORKSPACE_KEY)
    }
  }
})
