const listeners = new Set()

let socket = null
let reconnectTimer = null
let activeToken = ''
let manualClose = false

function socketUrl(token) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/messages?token=${encodeURIComponent(token)}`
}

function emit(event) {
  listeners.forEach((listener) => {
    try {
      listener(event)
    } catch (error) {
      console.error('message socket listener failed', error)
    }
  })
}

function clearReconnectTimer() {
  if (reconnectTimer) {
    window.clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

function scheduleReconnect() {
  clearReconnectTimer()
  if (manualClose || !activeToken) return
  reconnectTimer = window.setTimeout(() => {
    connectMessageSocket(activeToken)
  }, 3000)
}

export function connectMessageSocket(token) {
  if (!token) return
  activeToken = token
  manualClose = false

  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
    return
  }

  socket = new WebSocket(socketUrl(token))
  socket.addEventListener('open', () => {
    clearReconnectTimer()
    emit({ type: 'socket-open' })
  })
  socket.addEventListener('message', (rawEvent) => {
    try {
      emit(JSON.parse(rawEvent.data))
    } catch (error) {
      console.error('invalid message socket payload', error)
    }
  })
  socket.addEventListener('close', () => {
    socket = null
    emit({ type: 'socket-close' })
    scheduleReconnect()
  })
  socket.addEventListener('error', () => {
    if (socket && socket.readyState !== WebSocket.CLOSED) {
      socket.close()
    }
  })
}

export function disconnectMessageSocket() {
  manualClose = true
  activeToken = ''
  clearReconnectTimer()
  if (socket) {
    socket.close()
    socket = null
  }
}

export function subscribeMessageSocket(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}
