import { defineStore } from 'pinia'
import request from '@/api/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: null,
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.userInfo?.isAdmin === true,
  },
  actions: {
    async login(username, password) {
      const res = await request.post('/auth/login', { username, password, rememberMe: true })
      this.token = res.data.token
      this.userInfo = res.data
      uni.setStorageSync('token', res.data.token)
      return res.data
    },
    logout() {
      this.token = ''
      this.userInfo = null
      uni.removeStorageSync('token')
      uni.reLaunch({ url: '/pages/login/login' })
    },
  },
})
