import { getStoredToken } from '@/utils/authToken'

const listeners = new Set()
let socket = null

function buildWsUrl(token) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const base = import.meta.env.VITE_APP_BASE_API || ''
  return `${protocol}//${window.location.host}${base}/mobi/notify/ws?token=${encodeURIComponent(token)}&client=staff`
}

function teardownSocket() {
  if (!socket) {
    return
  }
  socket.onopen = null
  socket.onmessage = null
  socket.onerror = null
  socket.onclose = null
  socket.close()
  socket = null
}

/**
 * 建立通知 WebSocket（页面加载 / 登录后 / token 续签时调用）。
 * 连接失败或断开后不自动重连，避免 notify 服务未启动时刷屏；刷新页面后再试。
 */
export function connectNotifySocket() {
  const token = getStoredToken()
  if (!token) {
    return
  }
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
    return
  }

  teardownSocket()
  socket = new WebSocket(buildWsUrl(token))

  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data)
      listeners.forEach((listener) => listener(payload))
    } catch {
      // ignore malformed payload
    }
  }

  socket.onerror = () => {
    teardownSocket()
  }

  socket.onclose = () => {
    socket = null
  }
}

export function disconnectNotifySocket(clearListeners = true) {
  teardownSocket()
  if (clearListeners) {
    listeners.clear()
  }
}

/** token 续签或权限刷新后，用新 token 重建连接 */
export function reconnectNotifySocket() {
  disconnectNotifySocket(false)
  connectNotifySocket()
}

export function onNotifyMessage(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}
