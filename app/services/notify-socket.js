import { getToken } from '@/api/modules/auth-token.js'
import { GATEWAY_BASE_URL } from '@/config/env.js'

const listeners = new Set()
let socketTask = null
let reconnectTimer = null
let reconnectAttempts = 0
let socketOpen = false
let connecting = false
let heartbeatTimer = null
const MAX_RECONNECT_DELAY = 30000
const HEARTBEAT_INTERVAL = 30000

function buildWsUrl(token) {
	const base = GATEWAY_BASE_URL.replace(/^http/i, 'ws')
	return `${base}/mobi/notify/ws?token=${encodeURIComponent(token)}&client=customer`
}

function clearHeartbeat() {
	if (heartbeatTimer) {
		clearInterval(heartbeatTimer)
		heartbeatTimer = null
	}
}

function startHeartbeat() {
	clearHeartbeat()
	heartbeatTimer = setInterval(() => {
		sendNotifyPing()
	}, HEARTBEAT_INTERVAL)
}

function scheduleReconnect() {
	if (reconnectTimer || !getToken()) {
		return
	}
	const delay = Math.min(1000 * 2 ** reconnectAttempts, MAX_RECONNECT_DELAY)
	reconnectAttempts += 1
	reconnectTimer = setTimeout(() => {
		reconnectTimer = null
		connectNotifySocket()
	}, delay)
}

function resetSocketState() {
	connecting = false
	socketOpen = false
	clearHeartbeat()
	socketTask = null
}

function attachSocketHandlers(task) {
	task.onOpen(() => {
		connecting = false
		socketOpen = true
		reconnectAttempts = 0
		startHeartbeat()
	})

	task.onMessage((res) => {
		try {
			const payload = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
			listeners.forEach((listener) => listener(payload))
		} catch {
			// ignore malformed payload
		}
	})

	task.onClose(() => {
		resetSocketState()
		scheduleReconnect()
	})

	task.onError(() => {
		resetSocketState()
		scheduleReconnect()
	})
}

export function connectNotifySocket() {
	const token = getToken()
	if (!token) {
		return
	}
	if (socketOpen) {
		return
	}
	if (connecting) {
		return
	}

	if (socketTask) {
		try {
			socketTask.close({})
		} catch {
			// ignore
		}
		socketTask = null
	}

	connecting = true
	socketTask = uni.connectSocket({
		url: buildWsUrl(token),
		fail: () => {
			resetSocketState()
			scheduleReconnect()
		}
	})

	if (socketTask) {
		attachSocketHandlers(socketTask)
	} else {
		resetSocketState()
		scheduleReconnect()
	}
}

export function disconnectNotifySocket(clearListeners = true) {
	if (reconnectTimer) {
		clearTimeout(reconnectTimer)
		reconnectTimer = null
	}
	reconnectAttempts = 0
	if (socketTask) {
		try {
			socketTask.close({})
		} catch {
			// ignore
		}
	}
	resetSocketState()
	if (clearListeners) {
		listeners.clear()
	}
}

export function onNotifyMessage(listener) {
	listeners.add(listener)
	return () => listeners.delete(listener)
}

export function sendNotifyPing() {
	if (socketTask && socketOpen) {
		socketTask.send({ data: 'ping' })
	}
}

export function isNotifySocketOpen() {
	return socketOpen
}
