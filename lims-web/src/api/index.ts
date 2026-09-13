import http from './http'

export const authApi = {
  login: (data: { username: string; password: string }) => http.post('/auth/login', data),
  logout: () => http.post('/auth/logout'),
  info: () => http.get('/auth/info')
}

export const dictApi = {
  items: (code: string) => http.get(`/dict/${code}`)
}

export const dashboardApi = {
  summary: () => http.get('/dashboard/summary'),
  entrustStatus: () => http.get('/dashboard/entrust-status'),
  expiring: () => http.get('/dashboard/expiring-entrusts'),
  qualWarnings: () => http.get('/dashboard/qualification-warnings')
}

export const customerApi = {
  page: (params: any) => http.get('/customers', { params }),
  detail: (id: number | string) => http.get(`/customers/${id}`),
  save: (data: any) => http.post('/customers', data),
  claim: (id: number) => http.post(`/customers/${id}/claim`),
  release: (id: number, remark?: string) => http.post(`/customers/${id}/release`, { remark }),
  transfer: (id: number, userId: number, remark?: string) =>
    http.post(`/customers/${id}/transfer`, { userId, remark }),
  setStatus: (id: number, status: number) => http.post(`/customers/${id}/status`, { status }),
  poolLogs: (id: number) => http.get(`/customers/${id}/pool-logs`),
  adjustCredit: (data: any) => http.post('/customers/credit/adjust', data),
  creditLogs: (id: number) => http.get(`/customers/${id}/credit-logs`),
  qualifications: (id: number) => http.get(`/customers/${id}/qualifications`),
  saveQualification: (data: any) => http.post('/customers/qualifications', data),
  deleteQualification: (id: number) => http.delete(`/customers/qualifications/${id}`),
  addFollow: (data: any) => http.post('/customers/follows', data),
  follows: (id: number) => http.get(`/customers/${id}/follows`)
}

export const contractApi = {
  page: (params: any) => http.get('/contracts', { params }),
  detail: (id: number) => http.get(`/contracts/${id}`),
  save: (data: any) => http.post('/contracts', data),
  submit: (id: number) => http.post(`/contracts/${id}/submit`),
  changeStatus: (id: number, status: string, remark?: string) =>
    http.post(`/contracts/${id}/status`, { status, remark }),
  performance: (id: number) => http.get(`/contracts/${id}/performance`),
  payments: (id: number, payType?: number) =>
    http.get(`/contracts/${id}/payments`, { params: { payType } }),
  savePayment: (data: any) => http.post('/contracts/payments', data),
  deletePayment: (pid: number) => http.delete(`/contracts/payments/${pid}`),
  applyChange: (data: any) => http.post('/contracts/changes', data),
  changes: (id: number) => http.get(`/contracts/${id}/changes`),
  editorConfig: (id: number, edit = true) =>
    http.get(`/onlyoffice/editor-config/CONTRACT/${id}`, { params: { edit } })
}

export const quoteApi = {
  page: (params: any) => http.get('/quotes', { params }),
  detail: (id: number) => http.get(`/quotes/${id}`),
  save: (data: any) => http.post('/quotes', data),
  calculate: (items: any[], urgentFactor = 1) =>
    http.post('/quotes/calculate', { items, urgentFactor }),
  submit: (id: number) => http.post(`/quotes/${id}/submit`),
  void: (id: number) => http.post(`/quotes/${id}/void`),
  exportWordUrl: (id: number) => `/api/quotes/${id}/export-word`,
  rules: () => http.get('/quotes/rules'),
  saveRule: (data: any) => http.post('/quotes/rules', data)
}

export const entrustApi = {
  page: (params: any) => http.get('/entrusts', { params }),
  detail: (id: number) => http.get(`/entrusts/${id}`),
  save: (data: any) => http.post('/entrusts', data),
  fromQuote: (quoteId: number, data: any) => http.post(`/entrusts/from-quote/${quoteId}`, data),
  submitReview: (id: number, remark?: string) =>
    http.post(`/entrusts/${id}/submit-review`, { remark }),
  progress: (id: number, action: string, remark?: string) =>
    http.post(`/entrusts/${id}/progress`, { action, remark }),
  cancel: (id: number, reason: string) => http.post(`/entrusts/${id}/cancel`, { reason }),
  urgent: (id: number, urgent: boolean, reason?: string) =>
    http.post(`/entrusts/${id}/urgent`, { urgent, reason }),
  timeline: (id: number) => http.get(`/entrusts/${id}/timeline`),
  items: (id: number) => http.get(`/entrusts/${id}/items`),
  points: (id: number) => http.get(`/entrusts/${id}/points`),
  applyAdjustment: (data: any) => http.post('/entrusts/adjustments', data),
  adjustments: (id: number) => http.get(`/entrusts/${id}/adjustments`),
  applySubcontract: (data: any) => http.post('/entrusts/subcontracts', data),
  subcontracts: (id: number) => http.get(`/entrusts/${id}/subcontracts`)
}

export const approvalApi = {
  todo: () => http.get('/approval/todo'),
  todoCount: () => http.get('/approval/todo-count'),
  timeline: (instanceId: number) => http.get(`/approval/timeline/${instanceId}`),
  act: (taskId: number, approve: boolean, comment?: string) =>
    http.post('/approval/act', { taskId, approve, comment })
}

export const notificationApi = {
  page: (params: any) => http.get('/notifications', { params }),
  unreadCount: () => http.get('/notifications/unread-count'),
  read: (id: number) => http.post(`/notifications/${id}/read`),
  readAll: () => http.post('/notifications/read-all')
}

export const fileApi = {
  uploadUrl: '/api/files/upload'
}

export const systemApi = {
  userOptions: (roleCode?: string) =>
    http.get('/system/users/options', { params: { roleCode } }),
  operationLogs: (params: any) =>
    http.get('/system/operation-logs', { params })
}
