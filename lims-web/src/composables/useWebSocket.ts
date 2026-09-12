import { ref } from 'vue'
import { notification } from 'ant-design-vue'
import { approvalApi } from '@/api'

/** 全局 WebSocket: 接收实时通知与待办提醒 */
export function useWebSocket() {
  const connected = ref(false)
  let ws: WebSocket | null = null
  const todoCount = ref(0)

  function refreshTodoCount() {
    approvalApi.todoCount().then((n: any) => (todoCount.value = n)).catch(() => {})
  }

  function connect() {
    const token = localStorage.getItem('lims_token')
    if (!token) return
    let host = import.meta.env.VITE_WS_URL as string
    if (!host) {
      const proto = location.protocol === 'https:' ? 'wss' : 'ws'
      host = `${proto}://${location.host}`
    }
    ws = new WebSocket(`${host}/api/ws/notify?token=${encodeURIComponent(token)}`)
    ws.onopen = () => (connected.value = true)
    ws.onclose = () => {
      connected.value = false
      // 10s 自动重连
      setTimeout(connect, 10000)
    }
    ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data)
        notification.open({
          message: msg.title || '新消息',
          description: msg.content,
          type: msg.type === 'TODO' ? 'warning' : 'info',
          duration: 6
        })
        refreshTodoCount()
      } catch (e) {
        // ignore
      }
    }
  }

  function close() {
    ws?.close()
    ws = null
  }

  return { connected, todoCount, connect, close, refreshTodoCount }
}
