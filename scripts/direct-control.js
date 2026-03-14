const WebSocket = require('ws')

// 直接连接到 Minecraft 客户端
const ws = new WebSocket('ws://localhost:8080/minecraft')

ws.on('open', () => {
  console.log('✅ 已连接到 Minecraft 客户端')
  
  // 发送连接消息
  ws.send(JSON.stringify({
    type: 'connected',
    message: 'Controller connected'
  }))
  
  console.log('🎮 可以开始发送命令了')
  console.log('按 Ctrl+C 退出')
})

ws.on('message', (data) => {
  const msg = JSON.parse(data)
  console.log('📥 收到:', msg)
})

ws.on('error', (err) => {
  console.error('❌ 连接错误:', err.message)
  console.log('请确保 Minecraft 客户端已启动并连接到服务器')
})

ws.on('close', () => {
  console.log('🔌 连接已关闭')
  process.exit(0)
})

// 读取键盘输入
const readline = require('readline')
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
})

rl.on('line', (input) => {
  const cmd = input.trim().toLowerCase()
  
  switch (cmd) {
    case 'w':
      sendCommand('keyDown', { key: 'w' })
      setTimeout(() => sendCommand('keyUp', { key: 'w' }), 1000)
      break
    case 'jump':
    case 'space':
      sendCommand('keyDown', { key: 'space' })
      setTimeout(() => sendCommand('keyUp', { key: 'space' }), 200)
      break
    case 'click':
    case 'left':
      sendCommand('click', { button: 'left' })
      break
    case 'right':
      sendCommand('click', { button: 'right' })
      break
    case 'status':
      sendCommand('getStatus', {})
      break
    case 'exit':
    case 'quit':
      ws.close()
      break
    default:
      console.log('可用命令: w, jump, click, right, status, exit')
  }
})

function sendCommand(action, data) {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({
      type: 'command',
      action: action,
      data: data,
      id: 'cmd-' + Date.now()
    }))
    console.log(`📤 发送: ${action}`)
  }
}
