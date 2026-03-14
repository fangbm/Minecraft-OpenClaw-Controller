const WebSocket = require('ws')
const EventEmitter = require('events')
const fs = require('fs')
const path = require('path')

// 加载配置
const configPath = path.join(__dirname, 'config.json')
const config = JSON.parse(fs.readFileSync(configPath, 'utf8'))

/**
 * Minecraft 客户端控制器
 * 通过 WebSocket 连接本地 Minecraft 客户端 Mod
 */
class MinecraftClientController extends EventEmitter {
  constructor(config) {
    super()
    this.config = config
    this.ws = null
    this.connected = false
    this.commandId = 0
    this.pendingCommands = new Map()
    this.playerState = null
    this.pressedKeys = new Set()
    this.pressedMouse = new Set()
  }

  /**
   * 连接到 Minecraft 客户端
   */
  async connect() {
    const wsUrl = `ws://${this.config.client.host}:${this.config.client.port}/agent`
    
    console.log(`🔗 连接到 Minecraft 客户端: ${wsUrl}...`)
    
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(wsUrl)
      
      this.ws.on('open', () => {
        console.log('✅ 已连接到 Minecraft 客户端!')
        this.connected = true
        this.emit('connected')
        resolve()
      })
      
      this.ws.on('message', (data) => {
        this.handleMessage(JSON.parse(data))
      })
      
      this.ws.on('close', () => {
        console.log('🔌 连接已关闭')
        this.connected = false
        this.emit('disconnected')
        
        // 自动重连
        if (this.config.agent.autoReconnect) {
          setTimeout(() => this.connect(), this.config.agent.reconnectDelay)
        }
      })
      
      this.ws.on('error', (err) => {
        console.error('❌ WebSocket 错误:', err.message)
        reject(err)
      })
    })
  }

  /**
   * 发送消息到客户端
   */
  send(message) {
    if (!this.connected) {
      throw new Error('未连接到 Minecraft 客户端')
    }
    this.ws.send(JSON.stringify(message))
  }

  /**
   * 发送命令并等待响应
   */
  async sendCommand(action, data = {}) {
    const id = `cmd-${++this.commandId}`
    
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.pendingCommands.delete(id)
        reject(new Error('命令超时'))
      }, this.config.agent.actionTimeout || 30000)
      
      this.pendingCommands.set(id, { resolve, reject, timeout })
      
      this.send({
        type: 'command',
        action,
        data,
        id
      })
    })
  }

  /**
   * 处理收到的消息
   */
  handleMessage(message) {
    switch (message.type) {
      case 'response':
        this.handleResponse(message)
        break
        
      case 'event':
        this.handleEvent(message)
        break
        
      case 'state':
        this.playerState = message.data
        this.emit('stateUpdate', message.data)
        break
        
      default:
        console.log('收到未知消息类型:', message.type)
    }
  }

  /**
   * 处理命令响应
   */
  handleResponse(message) {
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
  handleEvent(message) {
    this.emit(message.event, message.data)
  }

  // ==================== 键盘控制 ====================

  /**
   * 按住按键
   * @param {string} key - w/a/s/d/space/shift/ctrl等
   */
  async keyDown(key) {
    this.pressedKeys.add(key)
    return this.sendCommand('keyDown', { key })
  }

  /**
   * 松开按键
   */
  async keyUp(key) {
    this.pressedKeys.delete(key)
    return this.sendCommand('keyUp', { key })
  }

  /**
   * 短按按键（按下后自动松开）
   * @param {string} key - 按键
   * @param {number} duration - 按住时间(ms)
   */
  async keyPress(key, duration = 100) {
    await this.keyDown(key)
    await this.sleep(duration)
    await this.keyUp(key)
  }

  /**
   * 组合键
   */
  async keyCombo(keys) {
    for (const key of keys) {
      await this.keyDown(key)
    }
    await this.sleep(100)
    for (const key of keys) {
      await this.keyUp(key)
    }
  }

  /**
   * 松开所有按键
   */
  async releaseAllKeys() {
    for (const key of this.pressedKeys) {
      await this.keyUp(key)
    }
    this.pressedKeys.clear()
  }

  // ==================== 鼠标控制 ====================

  /**
   * 移动鼠标（相对移动）
   * @param {number} deltaX - 水平移动（正数向右）
   * @param {number} deltaY - 垂直移动（正数向下）
   */
  async moveMouse(deltaX, deltaY) {
    return this.sendCommand('moveMouse', { deltaX, deltaY })
  }

  /**
   * 设置绝对视角
   * @param {number} yaw - 水平角度 (0-360)
   * @param {number} pitch - 垂直角度 (-90 到 90)
   */
  async setRotation(yaw, pitch) {
    return this.sendCommand('setRotation', { yaw, pitch })
  }

  /**
   * 点击鼠标
   * @param {string} button - left/right
   */
  async click(button) {
    return this.sendCommand('click', { button })
  }

  /**
   * 按住鼠标
   */
  async mouseDown(button) {
    this.pressedMouse.add(button)
    return this.sendCommand('mouseDown', { button })
  }

  /**
   * 松开鼠标
   */
  async mouseUp(button) {
    this.pressedMouse.delete(button)
    return this.sendCommand('mouseUp', { button })
  }

  /**
   * 按住左键一段时间（用于挖掘）
   * @param {number} duration - 按住时间(ms)
   */
  async holdLeftClick(duration) {
    await this.mouseDown('left')
    await this.sleep(duration)
    await this.mouseUp('left')
  }

  /**
   * 滚轮滚动
   * @param {number} delta - 正数向下，负数向上
   */
  async scroll(delta) {
    return this.sendCommand('scroll', { delta })
  }

  // ==================== 快捷操作 ====================

  /**
   * 选择快捷栏槽位 (0-8)
   */
  async selectSlot(slot) {
    return this.sendCommand('selectSlot', { slot })
  }

  /**
   * 打开背包 (E键)
   */
  async openInventory() {
    return this.keyPress('e')
  }

  /**
   * 关闭界面 (Esc键)
   */
  async closeGui() {
    return this.keyPress('escape')
  }

  /**
   * 丢弃手中物品 (Q键)
   */
  async dropItem() {
    return this.keyPress('q')
  }

  /**
   * 丢弃整组物品 (Ctrl+Q)
   */
  async dropStack() {
    return this.keyCombo(['ctrl', 'q'])
  }

  /**
   * 发送聊天消息 (T键 + 输入 + 回车)
   */
  async chat(message) {
    await this.keyPress('t')
    await this.sleep(100)
    await this.typeText(message)
    await this.sleep(100)
    await this.keyPress('return')
  }

  /**
   * 输入文本
   */
  async typeText(text) {
    return this.sendCommand('typeText', { text })
  }

  // ==================== 组合动作 ====================

  /**
   * 向前走
   */
  async moveForward() {
    return this.keyDown('w')
  }

  /**
   * 向后走
   */
  async moveBackward() {
    return this.keyDown('s')
  }

  /**
   * 向左走
   */
  async moveLeft() {
    return this.keyDown('a')
  }

  /**
   * 向右走
   */
  async moveRight() {
    return this.keyDown('d')
  }

  /**
   * 跳跃
   */
  async jump() {
    return this.keyPress('space')
  }

  /**
   * 蹲下
   */
  async sneak(state) {
    if (state) {
      return this.keyDown('shift')
    } else {
      return this.keyUp('shift')
    }
  }

  /**
   * 疾跑
   */
  async sprint(state) {
    if (state) {
      return this.keyDown('ctrl')
    } else {
      return this.keyUp('ctrl')
    }
  }

  /**
   * 停止所有移动
   */
  async stopMoving() {
    await this.keyUp('w')
    await this.keyUp('s')
    await this.keyUp('a')
    await this.keyUp('d')
    await this.keyUp('ctrl')
  }

  /**
   * 转向指定位置
   */
  async lookAt(x, y, z) {
    return this.sendCommand('lookAt', { x, y, z })
  }

  /**
   * 走到指定位置（简单实现，需要配合路径查找）
   */
  async walkTo(targetX, targetZ, stopDistance = 2) {
    while (true) {
      const status = await this.getStatus()
      const dx = targetX - status.position.x
      const dz = targetZ - status.position.z
      const distance = Math.sqrt(dx * dx + dz * dz)
      
      if (distance <= stopDistance) {
        await this.stopMoving()
        break
      }
      
      // 计算角度
      const yaw = Math.atan2(dx, dz) * (180 / Math.PI)
      await this.setRotation(yaw, 0)
      
      // 向前走
      await this.moveForward()
      
      await this.sleep(100)
    }
  }

  // ==================== 信息获取 ====================

  /**
   * 获取玩家状态
   */
  async getStatus() {
    return this.sendCommand('getStatus')
  }

  /**
   * 获取视野内的实体
   */
  async getVisibleEntities() {
    return this.sendCommand('getVisibleEntities')
  }

  /**
   * 获取视野内的方块
   */
  async getVisibleBlocks() {
    return this.sendCommand('getVisibleBlocks')
  }

  /**
   * 获取准星指向的方块
   */
  async getTargetedBlock() {
    return this.sendCommand('getTargetedBlock')
  }

  /**
   * 查找指定类型的方块
   */
  async findBlocks(type, radius = 20) {
    return this.sendCommand('findBlocks', { type, radius })
  }

  /**
   * 获取指定玩家信息
   */
  async findPlayer(username) {
    return this.sendCommand('findPlayer', { username })
  }

  /**
   * 查找最近的实体
   */
  async findNearestEntity(type, radius = 10) {
    return this.sendCommand('findNearestEntity', { type, radius })
  }

  /**
   * 获取屏幕截图
   */
  async screenshot() {
    return this.sendCommand('screenshot')
  }

  // ==================== 背包管理 ====================

  /**
   * 在背包中查找物品
   */
  findItemInInventory(itemName) {
    if (!this.playerState || !this.playerState.inventory) return -1
    
    const slot = this.playerState.inventory.findIndex(item => 
      item && item.name.toLowerCase().includes(itemName.toLowerCase())
    )
    return slot
  }

  /**
   * 获取到目标的距离
   */
  getDistanceTo(target) {
    if (!this.playerState) return Infinity
    
    const dx = target.x - this.playerState.position.x
    const dy = (target.y || 0) - this.playerState.position.y
    const dz = target.z - this.playerState.position.z
    
    return Math.sqrt(dx * dx + dy * dy + dz * dz)
  }

  // ==================== 高级功能 ====================

  /**
   * 停止所有动作
   */
  async stopAllActions() {
    await this.releaseAllKeys()
    for (const button of this.pressedMouse) {
      await this.mouseUp(button)
    }
    return this.sendCommand('stopAll')
  }

  /**
   * 等待指定时间
   */
  sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.ws) {
      this.ws.close()
    }
  }
}

// ==================== 主程序 ====================

async function main() {
  const mc = new MinecraftClientController(config)
  
  // 事件监听
  mc.on('connected', () => {
    console.log('🎮 Minecraft 客户端控制器已就绪!')
    console.log('可用命令:')
    console.log('  mc.moveForward() - 向前走')
    console.log('  mc.jump() - 跳跃')
    console.log('  mc.click("left") - 左键点击')
    console.log('  mc.chat("Hello!") - 发送消息')
    console.log('  mc.getStatus() - 获取状态')
  })
  
  mc.on('disconnected', () => {
    console.log('🔌 与 Minecraft 客户端断开连接')
  })
  
  mc.on('chat', (data) => {
    console.log(`💬 ${data.sender}: ${data.message}`)
  })
  
  mc.on('damage', (data) => {
    console.log(`💔 受到 ${data.damage} 点伤害 (来源: ${data.source})`)
  })
  
  mc.on('death', () => {
    console.log('💀 玩家死亡了!')
  })
  
  mc.on('stateUpdate', (state) => {
    // 状态更新
  })
  
  // 连接客户端
  try {
    await mc.connect()
  } catch (err) {
    console.error('连接失败:', err.message)
    console.log('请确保:')
    console.log('1. Minecraft 客户端已启动')
    console.log('2. Client Controller Mod 已安装并加载')
    console.log('3. WebSocket 服务器已启动')
    process.exit(1)
  }
  
  // 导出实例供 OpenClaw 使用
  global.mc = mc
}

// 启动
main().catch(console.error)

// 导出类
module.exports = MinecraftClientController
