const WebSocket = require('ws')
const EventEmitter = require('events')

/**
 * OpenClaw Minecraft 控制器服务器
 * 等待 Minecraft 客户端主动连接
 */
class MinecraftControllerServer extends EventEmitter {
  constructor(port = 8080) {
    super()
    this.port = port
    this.wss = null
    this.clients = new Map()  // 存储连接的 Minecraft 客户端
    this.controllers = new Map() // 存储控制端连接
    this.commandId = 0
    this.pendingCommands = new Map()
  }

  /**
   * 启动 WebSocket 服务器
   */
  start() {
    this.wss = new WebSocket.Server({ port: this.port })
    
    this.wss.on('connection', (ws, req) => {
      const path = req.url
      
      if (path === '/minecraft') {
        this.handleMinecraftClient(ws, req)
      } else if (path === '/control') {
        this.handleController(ws, req)
      } else {
        console.log(`⚠️ 未知路径连接: ${path}`)
        ws.close()
      }
    })
    
    this.wss.on('error', (err) => {
      console.error('WebSocket 服务器错误:', err.message)
    })
    
    console.log(`🚀 OpenClaw Minecraft 控制器服务器已启动`)
    console.log(`   Minecraft 客户端: ws://0.0.0.0:${this.port}/minecraft`)
    console.log(`   控制端: ws://0.0.0.0:${this.port}/control`)
    
    return this
  }
  
  /**
   * 处理 Minecraft 客户端连接
   */
  handleMinecraftClient(ws, req) {
    const clientId = `mc-${Date.now()}`
    console.log(`🎮 Minecraft 客户端已连接: ${clientId} (${req.socket.remoteAddress})`)
    
    this.clients.set(clientId, {
      ws,
      id: clientId,
      playerInfo: null,
      connectedAt: new Date()
    })
    
    ws.on('message', (data) => {
      this.handleMinecraftMessage(clientId, JSON.parse(data))
    })
    
    ws.on('close', () => {
      console.log(`🔌 Minecraft 客户端断开连接: ${clientId}`)
      this.clients.delete(clientId)
      this.emit('clientDisconnected', clientId)
    })
    
    ws.on('error', (err) => {
      console.error(`❌ 客户端 ${clientId} 错误:`, err.message)
    })
    
    this.emit('clientConnected', clientId)
  }
  
  /**
   * 处理控制端连接
   */
  handleController(ws, req) {
    const controllerId = `ctrl-${Date.now()}`
    console.log(`🎮 控制端已连接: ${controllerId} (${req.socket.remoteAddress})`)
    
    this.controllers.set(controllerId, {
      ws,
      id: controllerId,
      connectedAt: new Date()
    })
    
    ws.on('message', (data) => {
      this.handleControllerMessage(controllerId, JSON.parse(data))
    })
    
    ws.on('close', () => {
      console.log(`🔌 控制端断开连接: ${controllerId}`)
      this.controllers.delete(controllerId)
    })
    
    ws.on('error', (err) => {
      console.error(`❌ 控制端 ${controllerId} 错误:`, err.message)
    })
    
    // 发送欢迎消息
    ws.send(JSON.stringify({
      type: 'connected',
      message: '控制端已连接到 OpenClaw 服务器',
      clients: Array.from(this.clients.keys())
    }))
  }

  /**
   * 处理 Minecraft 客户端消息
   */
  handleMinecraftMessage(clientId, message) {
    switch (message.type) {
      case 'connected':
        console.log(`✅ 客户端 ${clientId}: ${message.message}`)
        break
        
      case 'response':
        this.handleResponse(clientId, message)
        break
        
      case 'event':
        this.handleEvent(clientId, message)
        break
        
      default:
        console.log('收到未知消息类型:', message.type)
    }
  }
  
  /**
   * 处理控制端消息
   */
  handleControllerMessage(controllerId, message) {
    if (message.type === 'command') {
      // 转发命令给第一个 Minecraft 客户端
      const firstClient = this.clients.values().next().value
      if (firstClient) {
        firstClient.ws.send(JSON.stringify(message))
        console.log(`📤 转发命令给 ${firstClient.id}: ${message.action}`)
      } else {
        const controller = this.controllers.get(controllerId)
        if (controller) {
          controller.ws.send(JSON.stringify({
            type: 'error',
            message: '没有连接的 Minecraft 客户端'
          }))
        }
      }
    }
  }

  /**
   * 处理命令响应
   */
  handleResponse(clientId, message) {
    // 转发给所有控制端
    for (const controller of this.controllers.values()) {
      if (controller.ws.readyState === WebSocket.OPEN) {
        controller.ws.send(JSON.stringify(message))
      }
    }
    
    // 处理 pending 的 promise
    const pending = this.pendingCommands.get(message.id)
    if (pending) {
      clearTimeout(pending.timeout)
      this.pendingCommands.delete(message.id)
      
      if (message.success) {
        pending.resolve(message.data)
      } else {
        pending.reject(new Error(message.error || '命令失败'))
      }
    }
  }

  /**
   * 处理游戏事件
   */
  handleEvent(clientId, message) {
    const client = this.clients.get(clientId)
    
    if (message.event === 'playerInfo') {
      client.playerInfo = message.data
      console.log(`👤 玩家信息: ${message.data.name} (${message.data.uuid})`)
    }
    
    this.emit(message.event, { clientId, data: message.data })
  }

  /**
   * 获取第一个可用的客户端（用于单人控制）
   */
  getFirstClient() {
    const first = this.clients.values().next().value
    return first ? first.ws : null
  }

  /**
   * 获取指定客户端
   */
  getClient(clientId) {
    const client = this.clients.get(clientId)
    return client ? client.ws : null
  }

  /**
   * 获取所有连接的客户端
   */
  getAllClients() {
    return Array.from(this.clients.keys())
  }

  /**
   * 发送命令到 Minecraft 客户端
   */
  async sendCommand(ws, action, data = {}) {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      throw new Error('客户端未连接')
    }
    
    const id = `cmd-${++this.commandId}`
    
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.pendingCommands.delete(id)
        reject(new Error('命令超时'))
      }, 30000)
      
      this.pendingCommands.set(id, { resolve, reject, timeout })
      
      ws.send(JSON.stringify({
        type: 'command',
        action,
        data,
        id
      }))
    })
  }

  // ==================== 快捷控制方法 ====================

  /**
   * 按住按键
   */
  async keyDown(ws, key) {
    return this.sendCommand(ws, 'keyDown', { key })
  }

  /**
   * 松开按键
   */
  async keyUp(ws, key) {
    return this.sendCommand(ws, 'keyUp', { key })
  }

  /**
   * 短按按键
   */
  async keyPress(ws, key, duration = 100) {
    await this.keyDown(ws, key)
    await this.sleep(duration)
    await this.keyUp(ws, key)
  }

  /**
   * 移动鼠标
   */
  async moveMouse(ws, deltaX, deltaY) {
    return this.sendCommand(ws, 'moveMouse', { deltaX, deltaY })
  }

  /**
   * 设置视角
   */
  async setRotation(ws, yaw, pitch) {
    return this.sendCommand(ws, 'setRotation', { yaw, pitch })
  }

  /**
   * 点击鼠标
   */
  async click(ws, button) {
    return this.sendCommand(ws, 'click', { button })
  }

  /**
   * 选择快捷栏槽位
   */
  async selectSlot(ws, slot) {
    return this.sendCommand(ws, 'selectSlot', { slot })
  }

  /**
   * 获取玩家状态
   */
  async getStatus(ws) {
    return this.sendCommand(ws, 'getStatus')
  }

  /**
   * 发送聊天消息
   */
  async chat(ws, message) {
    await this.keyPress(ws, 't')
    await this.sleep(100)
    await this.typeText(ws, message)
    await this.sleep(100)
    await this.keyPress(ws, 'return')
  }

  /**
   * 输入文本
   */
  async typeText(ws, text) {
    return this.sendCommand(ws, 'typeText', { text })
  }

  /**
   * 等待
   */
  sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  /**
   * 停止服务器
   */
  stop() {
    if (this.wss) {
      this.wss.close()
      this.wss = null
    }
  }
}

// ==================== 主程序 ====================

async function main() {
  const server = new MinecraftControllerServer(8080)
  
  server.on('clientConnected', (clientId) => {
    console.log(`\n🎮 新客户端连接: ${clientId}`)
    console.log('可用命令:')
    console.log('  server.getFirstClient() - 获取第一个客户端')
    console.log('  server.keyDown(ws, "w") - 向前走')
    console.log('  server.click(ws, "left") - 左键点击')
    console.log('  server.getStatus(ws) - 获取状态')
  })
  
  server.on('clientDisconnected', (clientId) => {
    console.log(`\n🔌 客户端断开: ${clientId}`)
  })
  
  server.start()
  
  // 导出供 OpenClaw 使用
  global.server = server
  global.mc = {
    // 快捷方法 - 操作第一个连接的客户端
    keyDown: (key) => server.keyDown(server.getFirstClient(), key),
    keyUp: (key) => server.keyUp(server.getFirstClient(), key),
    keyPress: (key, duration) => server.keyPress(server.getFirstClient(), key, duration),
    moveMouse: (dx, dy) => server.moveMouse(server.getFirstClient(), dx, dy),
    setRotation: (yaw, pitch) => server.setRotation(server.getFirstClient(), yaw, pitch),
    click: (button) => server.click(server.getFirstClient(), button),
    selectSlot: (slot) => server.selectSlot(server.getFirstClient(), slot),
    getStatus: () => server.getStatus(server.getFirstClient()),
    getVisibleEntities: () => server.getVisibleEntities(server.getFirstClient()),
    chat: (msg) => server.chat(server.getFirstClient(), msg),
    sleep: (ms) => server.sleep(ms)
  }
}

main().catch(console.error)

module.exports = MinecraftControllerServer
