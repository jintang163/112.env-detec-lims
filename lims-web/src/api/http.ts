import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse
} from 'axios'
import { message } from 'ant-design-vue'
import router from '@/router'

/**
 * 响应拦截器已统一解包:
 *  - 普通业务请求: 后端 Result<T> 中 code===200 时直接 resolve 出 data(T),
 *    code 非 200 时 reject;
 *  - responseType === 'blob': resolve 响应体(Blob 等二进制内容)。
 * 因此经过本实例发出的请求, 其 Promise 结果就是业务数据本身, 不再是 AxiosResponse。
 */
export interface Http {
  request<T = any>(config: AxiosRequestConfig): Promise<T>
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  head<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  options<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
}

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('lims_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (resp: AxiosResponse) => {
    // 二进制响应直接返回响应体, 与业务请求一样不再暴露 AxiosResponse
    if (resp.config.responseType === 'blob') {
      return resp.data
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

const http = instance as unknown as Http

export default http
