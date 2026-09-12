import { defineStore } from 'pinia'
import { authApi } from '@/api'

interface MenuNode {
  id: number
  permCode?: string
  permName: string
  path?: string
  icon?: string
  children?: MenuNode[]
}

interface UserState {
  token: string
  userId?: number
  username: string
  realName: string
  roles: string[]
  permissions: string[]
  menus: MenuNode[]
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: localStorage.getItem('lims_token') || '',
    username: '',
    realName: '',
    roles: [],
    permissions: [],
    menus: []
  }),
  getters: {
    isLogin: (s) => !!s.token,
    isAdmin: (s) => s.roles.includes('ROLE_ADMIN')
  },
  actions: {
    async login(username: string, password: string) {
      const data: any = await authApi.login({ username, password })
      this.token = data.token
      this.userId = data.userId
      this.username = data.username
      this.realName = data.realName
      this.roles = data.roles || []
      this.permissions = data.permissions || []
      localStorage.setItem('lims_token', data.token)
      await this.fetchInfo()
    },
    async fetchInfo() {
      const info: any = await authApi.info()
      this.userId = info.userId
      this.username = info.username
      this.realName = info.realName
      this.roles = info.roles || []
      this.permissions = info.permissions || []
      this.menus = info.menus || []
    },
    hasPerm(code: string) {
      return this.isAdmin || this.permissions.includes(code)
    },
    async logout() {
      try {
        await authApi.logout()
      } catch (e) {
        // ignore
      }
      this.$reset()
      localStorage.removeItem('lims_token')
    }
  }
})
