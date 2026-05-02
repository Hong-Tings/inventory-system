import axios from 'axios'
import type { Result } from '../types/api'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api/v1',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  // 清理 undefined/null/空字符串的参数
  if (config.params) {
    const cleaned: Record<string, unknown> = {}
    for (const key in config.params) {
      const val = config.params[key]
      if (val !== undefined && val !== null && val !== '') {
        cleaned[key] = val
      }
    }
    config.params = cleaned
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    // Blob/文件流直接返回，不解析 JSON
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response
    }
    const res = response.data as Result
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      window.location.href = '/login'
      return Promise.reject(error)
    }
    if (error.response?.status === 403) {
      ElMessage.error('权限不足，无法执行此操作')
      return Promise.reject(error)
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  },
)

export async function downloadFile(url: string, filename: string) {
  try {
    const res = await request.get(url, { responseType: 'blob' })
    const blob = new Blob([res.data])
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = filename
    a.click()
    URL.revokeObjectURL(a.href)
  } catch { /* handled by interceptor */ }
}

export default request
