import { defineStore } from 'pinia'

interface UserInfo {
  userId?: number
  username: string
  realName: string
  roles: string[]
  token: string
}

export const useUserStore = defineStore('user', {
  state: () => ({
    info: (uni.getStorageSync('lims_user') || null) as UserInfo | null
  }),
  getters: {
    token: () => uni.getStorageSync('lims_token') || '',
    isLogin: () => !!uni.getStorageSync('lims_token')
  },
  actions: {
    setLogin(data: any) {
      uni.setStorageSync('lims_token', data.token)
      this.info = {
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        roles: data.roles || [],
        token: data.token
      }
      uni.setStorageSync('lims_user', this.info)
    },
    logout() {
      uni.removeStorageSync('lims_token')
      uni.removeStorageSync('lims_user')
      this.info = null
    }
  }
})
