"use strict";
const common_vendor = require("../common/vendor.js");
const api_modules_authToken = require("../api/modules/auth-token.js");
const config_env = require("../config/env.js");
const listeners = /* @__PURE__ */ new Set();
let socketTask = null;
let reconnectTimer = null;
let reconnectAttempts = 0;
let socketOpen = false;
let connecting = false;
let heartbeatTimer = null;
const MAX_RECONNECT_DELAY = 3e4;
const HEARTBEAT_INTERVAL = 3e4;
function buildWsUrl(token) {
  const base = config_env.GATEWAY_BASE_URL.replace(/^http/i, "ws");
  return `${base}/mobi/notify/ws?token=${encodeURIComponent(token)}&client=customer`;
}
function clearHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
}
function startHeartbeat() {
  clearHeartbeat();
  heartbeatTimer = setInterval(() => {
    sendNotifyPing();
  }, HEARTBEAT_INTERVAL);
}
function scheduleReconnect() {
  if (reconnectTimer || !api_modules_authToken.getToken()) {
    return;
  }
  const delay = Math.min(1e3 * 2 ** reconnectAttempts, MAX_RECONNECT_DELAY);
  reconnectAttempts += 1;
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    connectNotifySocket();
  }, delay);
}
function resetSocketState() {
  connecting = false;
  socketOpen = false;
  clearHeartbeat();
  socketTask = null;
}
function attachSocketHandlers(task) {
  task.onOpen(() => {
    connecting = false;
    socketOpen = true;
    reconnectAttempts = 0;
    startHeartbeat();
  });
  task.onMessage((res) => {
    try {
      const payload = typeof res.data === "string" ? JSON.parse(res.data) : res.data;
      listeners.forEach((listener) => listener(payload));
    } catch {
    }
  });
  task.onClose(() => {
    resetSocketState();
    scheduleReconnect();
  });
  task.onError(() => {
    resetSocketState();
    scheduleReconnect();
  });
}
function connectNotifySocket() {
  const token = api_modules_authToken.getToken();
  if (!token) {
    return;
  }
  if (socketOpen) {
    return;
  }
  if (connecting) {
    return;
  }
  if (socketTask) {
    try {
      socketTask.close({});
    } catch {
    }
    socketTask = null;
  }
  connecting = true;
  socketTask = common_vendor.index.connectSocket({
    url: buildWsUrl(token),
    fail: () => {
      resetSocketState();
      scheduleReconnect();
    }
  });
  if (socketTask) {
    attachSocketHandlers(socketTask);
  } else {
    resetSocketState();
    scheduleReconnect();
  }
}
function disconnectNotifySocket(clearListeners = true) {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  reconnectAttempts = 0;
  if (socketTask) {
    try {
      socketTask.close({});
    } catch {
    }
  }
  resetSocketState();
  if (clearListeners) {
    listeners.clear();
  }
}
function onNotifyMessage(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
function sendNotifyPing() {
  if (socketTask && socketOpen) {
    socketTask.send({ data: "ping" });
  }
}
exports.connectNotifySocket = connectNotifySocket;
exports.disconnectNotifySocket = disconnectNotifySocket;
exports.onNotifyMessage = onNotifyMessage;
