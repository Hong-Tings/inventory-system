import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../api/request'
import type { UserInfo } from '../types/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.isAdmin === true)

  async function login(username: string, password: string) {
    const res = await request.post('/auth/login', { username, password })
    const data = res.data.data as UserInfo
    token.value = data.token
    userInfo.value = data
    localStorage.setItem('token', data.token)
    return data
  }

  async function getUserInfo() {
    const res = await request.get('/auth/userinfo')
    userInfo.value = res.data.data
    return userInfo.value
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return { token, userInfo, isLoggedIn, isAdmin, login, getUserInfo, logout }
})
