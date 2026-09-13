/**
 * 统一请求封装(uni.request): 自动带 JWT、401 跳登录、弱网时离线降级。
 */
const BASE_URL = '/api'

export interface ApiOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  /** 离线时是否允许返回本地缓存 */
  offlineCache?: boolean
}

export function getToken(): string {
  return uni.getStorageSync('lims_token') || ''
}

export function request<T = any>(opts: ApiOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + opts.url,
      method: opts.method || 'GET',
      data: opts.data || {},
      header: {
        'Content-Type': 'application/json',
        Authorization: getToken() ? `Bearer ${getToken()}` : ''
      },
      success: (res: any) => {
        const body = res.data
        if (res.statusCode === 200 && body?.code === 200) {
          // 成功数据写离线缓存
          if (opts.offlineCache) {
            try {
              uni.setStorageSync('cache:' + opts.url, JSON.stringify({ t: Date.now(), d: body.data }))
            } catch (e) {
              // ignore quota
            }
          }
          resolve(body.data)
        } else if (body?.code === 401) {
          uni.removeStorageSync('lims_token')
          uni.reLaunch({ url: '/pages/login/login' })
          reject(new Error(body.message || '未登录'))
        } else {
          uni.showToast({ title: body?.message || '请求失败', icon: 'none' })
          reject(new Error(body?.message || 'error'))
        }
      },
      fail: async () => {
        // 网络失败: 尝试离线缓存
        if (opts.offlineCache) {
          const raw = uni.getStorageSync('cache:' + opts.url)
          if (raw) {
            try {
              const cached = JSON.parse(raw)
              uni.showToast({ title: '当前为离线数据', icon: 'none' })
              resolve(cached.d)
              return
            } catch (e) {
              // fallthrough
            }
          }
        }
        uni.showToast({ title: '网络异常,请检查连接', icon: 'none' })
        reject(new Error('network error'))
      }
    })
  })
}

/**
 * 文件上传(uni.uploadFile 不走 request() 的 JSON 通道)。
 * 返回后端 SysFile; 网络失败时 reject, 由调用方决定是否进入离线队列。
 */
export function uploadFile(
  filePath: string,
  formData?: Record<string, any>
): Promise<any> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/files/upload',
      filePath,
      name: 'file',
      formData: formData || {},
      header: { Authorization: getToken() ? `Bearer ${getToken()}` : '' },
      success: (res: any) => {
        try {
          const body = JSON.parse(res.data)
          if (res.statusCode === 200 && body?.code === 200) {
            resolve(body.data)
          } else {
            reject(new Error(body?.message || '上传失败'))
          }
        } catch (e) {
          reject(new Error('上传响应解析失败'))
        }
      },
      fail: () => reject(new Error('network error'))
    })
  })
}

export const api = {
  login: (username: string, password: string) =>
    request({ url: '/auth/login', method: 'POST', data: { username, password } }),
  info: () => request({ url: '/auth/info' }),
  dict: (code: string) => request({ url: `/dict/${code}`, offlineCache: true }),
  // —— 现场采样新接口 ——
  myTasks: (params: any) =>
    request({ url: '/mobile/sampling/my-tasks', data: params, offlineCache: true }),
  taskDetail: (id: number) =>
    request({ url: `/mobile/sampling/tasks/${id}`, offlineCache: true }),
  markDownloaded: (id: number) =>
    request({ url: `/mobile/sampling/tasks/${id}/download`, method: 'POST' }),
  submitSample: (data: any) =>
    request({ url: '/mobile/sampling/samples', method: 'POST', data }),
  submitTask: (id: number, remark?: string) =>
    request({ url: `/mobile/sampling/tasks/${id}/submit`, method: 'POST', data: { remark } }),
  createHandover: (data: any) =>
    request({ url: '/mobile/sampling/handovers', method: 'POST', data }),
  myHandovers: () => request({ url: '/mobile/sampling/handovers/my' }),
  sampleByCode: (code: string) =>
    request({ url: `/mobile/sampling/samples/${encodeURIComponent(code)}` }),
  // —— 旧版委托采样(点位采集仍复用) ——
  samplingTasks: (params: any) =>
    request({ url: '/mobile/sampling-tasks', data: params, offlineCache: true }),
  entrustDetail: (id: number) =>
    request({ url: `/mobile/entrusts/${id}`, offlineCache: true }),
  addPoint: (id: number, data: any) =>
    request({ url: `/mobile/entrusts/${id}/points`, method: 'POST', data }),
  finishSampling: (id: number) =>
    request({ url: `/mobile/entrusts/${id}/finish-sampling`, method: 'POST' }),
  customers: (params: any) =>
    request({ url: '/mobile/customers', data: params, offlineCache: true }),
  approvals: () => request({ url: '/approval/todo' }),
  approvalAct: (taskId: number, approve: boolean, comment?: string) =>
    request({ url: '/approval/act', method: 'POST', data: { taskId, approve, comment } }),
  dashboard: () => request({ url: '/dashboard/summary', offlineCache: true }),
  entrustStatus: () => request({ url: '/dashboard/entrust-status', offlineCache: true }),
  progress: (id: number, action: string) =>
    request({ url: `/entrusts/${id}/progress`, method: 'POST', data: { action } })
}
