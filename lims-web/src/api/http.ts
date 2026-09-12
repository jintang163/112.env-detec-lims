import axios, { type AxiosInstance } from 'axios'
import { message } from 'ant-design-vue'
import router from '@/router'

const http: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('lims_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    // 文件流直接返回
    if (resp.config.responseType === 'blob') {
      return resp
    }
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return body.data
      }
      if (body.code === 401) {
        localStorage.removeItem('lims_token')
        router.push('/login')
      }
      message.error(body.message || '操作失败')
      return Promise.reject(new Error(body.message))
    }
    return body
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('lims_token')
      router.push('/login')
    }
    message.error(err.response?.data?.message || err.message || '网络异常')
    return Promise.reject(err)
  }
)

export default http
