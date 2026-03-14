# 🎮 Minecraft OpenClaw Controller

通过 WebSocket 控制本地 Minecraft 客户端，让 AI Agent 像正常玩家一样操控游戏角色。

## 架构设计

```
                    WebSocket (反向连接)
┌─────────────┐  ◄──────────────────────  ┌──────────────┐     Mod API    ┌─────────────┐
│  OpenClaw   │   Minecraft主动连接服务器  │ MC Client    │ ◜────────────► │ Minecraft   │
│  (Server)   │  ──────────────────────►  │  (Mod)       │   (Forge)      │  (Client)   │
└─────────────┘                           └──────────────┘                └─────────────┘
    公网IP/云服务器                              家庭宽带/NAT
```

## 工作流程

1. **OpenClaw 启动 WebSocket 服务器**（公网服务器，端口 8080）
2. **玩家启动 Minecraft 客户端**（带 mod）
3. **Mod 主动连接 OpenClaw 服务器**（穿透 NAT）
4. **OpenClaw 发送控制指令**
5. **Mod 模拟玩家输入**（键盘、鼠标）
6. **游戏响应，状态回传给 OpenClaw**

## 功能特性

### 玩家输入模拟
- ✅ 键盘输入（WASD、空格、Shift 等）
- ✅ 鼠标移动（视角控制）
- ✅ 鼠标点击（左键/右键）
- ✅ 滚轮切换
- ✅ 快捷键

### 游戏感知
- 👁️ 玩家状态（位置、生命值、饥饿度等）
- 👁️ 背包内容
- 👁️ 视野范围内的方块和实体
- 👁️ 聊天消息
- 👁️ GUI 状态（打开背包、箱子等）

### AI 行为
- 🧠 基于感知信息自主决策
- 🧠 目标导向的行为规划
- 🧠 路径查找和避障
- 🧠 与玩家和其他实体交互

## 安装步骤

### 1. 安装客户端 Mod

#### Forge 版本 (Minecraft 1.21.1)
1. 安装 [Forge 1.21.1](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.21.1.html)
2. 构建 Mod：
   ```bash
   cd forge-mod
   ./gradlew build
   ```
3. 将生成的 jar 放入 `.minecraft/mods/` 文件夹
4. 启动游戏

#### NeoForge 版本 (Minecraft 1.21.1)
1. 安装 [NeoForge 1.21.1](https://neoforged.net/)
2. 构建 Mod：
   ```bash
   cd neoforge-mod
   ./gradlew build
   ```
3. 将生成的 jar 放入 `.minecraft/mods/` 文件夹
4. 启动游戏

> **提示**: NeoForge 是 Forge 的现代分支，提供更简洁的 API 和更好的性能。

### 2. 配置 Mod

首次启动游戏后，Mod 会自动创建配置文件：

**文件位置：** `.minecraft/config/client-controller.properties`

```properties
# OpenClaw 服务器地址
server.host=your-openclaw-server.com

# OpenClaw 服务器端口
server.port=8080

# 是否自动连接
auto.connect=false
```

修改 `server.host` 为你的 OpenClaw 服务器地址，然后重启游戏或在游戏中按 **C 键**连接。

### 3. 启动 OpenClaw 服务器

```bash
cd scripts
npm install
node server.js
```

服务器会监听 `0.0.0.0:8080`，等待 Minecraft 客户端连接。

## OpenClaw 控制接口

### 键盘控制

```javascript
// 按住按键
await mc.keyDown('w')      // 向前
await mc.keyDown('space')  // 跳跃

// 松开按键
await mc.keyUp('w')

// 短按（按下后自动松开）
await mc.keyPress('space', 100)  // 跳跃 100ms

// 组合键
await mc.keyCombo(['ctrl', 'w'])  // 疾跑前进
```

### 鼠标控制

```javascript
// 移动视角（相对移动）
await mc.moveMouse(100, 0)   // 向右转
await mc.moveMouse(0, -50)   // 向上看

// 设置绝对视角
await mc.setRotation(yaw, pitch)

// 点击
await mc.click('left')   // 左键（攻击/挖掘）
await mc.click('right')  // 右键（使用/放置）

// 滚轮
await mc.scroll(1)   // 向下滚
await mc.scroll(-1)  // 向上滚
```

### 快捷操作

```javascript
// 选择快捷栏槽位 (0-8)
await mc.selectSlot(0)

// 打开背包
await mc.openInventory()

// 关闭界面
await mc.closeGui()

// 发送聊天消息
await mc.chat('Hello!')
```

### 获取状态

```javascript
// 获取玩家状态
const status = await mc.getStatus()
console.log(status.position)    // {x, y, z}
console.log(status.rotation)    // {yaw, pitch}
console.log(status.health)      // 20
console.log(status.food)        // 20
console.log(status.inventory)   // [...]

// 获取视野内的实体
const entities = await mc.getVisibleEntities()

// 获取准星指向的方块
const target = await mc.getTargetedBlock()

// 获取屏幕截图（用于视觉识别）
const screenshot = await mc.screenshot()
```

## AI 行为示例

### 自动采集木材

```javascript
async function collectWood() {
  // 1. 寻找树木
  const trees = await mc.findBlocks('log', 20)
  
  if (trees.length === 0) {
    mc.chat('附近没有找到树木')
    return
  }
  
  // 2. 转向树木
  const tree = trees[0]
  await mc.lookAt(tree.x, tree.y, tree.z)
  
  // 3. 切换到斧头
  const axeSlot = mc.findItemInInventory('axe')
  if (axeSlot !== -1) {
    await mc.selectSlot(axeSlot)
  }
  
  // 4. 走到树木前
  await mc.walkTo(tree.x, tree.z, 2)  // 距离 2 格
  
  // 5. 挖掘
  await mc.holdLeftClick(2000)  // 按住左键 2 秒
  
  mc.chat('采集完成!')
}
```

### 响应玩家指令

```javascript
mc.on('chat', async (data) => {
  if (data.message.startsWith('!')) {
    const args = data.message.slice(1).split(' ')
    const cmd = args[0]
    
    switch (cmd) {
      case 'come':
        const player = await mc.findPlayer(data.sender)
        if (player) {
          mc.chat(`好的 ${data.sender}，我来了!`)
          await mc.walkTo(player.x, player.z, 2)
        }
        break
        
      case 'stop':
        mc.stopAllActions()
        mc.chat('已停止')
        break
    }
  }
})
```

## WebSocket 协议

### 连接

```
ws://localhost:8080/agent
```

### 消息格式

**命令（OpenClaw → Mod）**
```json
{
  "type": "command",
  "action": "keyDown",
  "data": {"key": "w"},
  "id": "cmd-001"
}
```

**事件（Mod → OpenClaw）**
```json
{
  "type": "event",
  "event": "chat",
  "data": {
    "sender": "Player",
    "message": "Hello!"
  }
}
```

## 目录结构

```
.
├── forge-mod/           # Minecraft Forge Mod (Java)
│   ├── src/
│   └── build.gradle
├── neoforge-mod/        # Minecraft NeoForge Mod (Java)
│   ├── src/
│   └── build.gradle
├── scripts/             # OpenClaw 控制器 (Node.js)
│   ├── server.js        # WebSocket 服务器
│   ├── agent.js         # 控制器逻辑
│   └── package.json
└── README.md
```

## 故障排除

### 无法连接 WebSocket
- 检查 Minecraft 是否已启动
- 确认 mod 已正确加载
- 检查端口是否被占用
- 查看 Minecraft 日志

### 输入无响应
- 确认游戏窗口有焦点
- 检查是否有其他程序拦截输入
- 尝试调整 `keyPressDuration`

### 视角移动异常
- 调整 `mouseSensitivity`
- 检查游戏内的鼠标灵敏度设置

## 安全提示

⚠️ **不要在多人服务器上使用，除非获得管理员许可**  
⚠️ **遵守服务器规则**  
⚠️ **注意自动化行为可能被反作弊系统检测**

## 相关链接

- [Forge for 1.21.1](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.21.1.html)
- [Forge Documentation](https://docs.minecraftforge.net/)
- [OpenClaw](https://github.com/openclaw/openclaw)

## License

MIT License - 详见 [LICENSE](LICENSE) 文件

---

**作者:** OpenClaw Community  
**版本:** 1.0.0
