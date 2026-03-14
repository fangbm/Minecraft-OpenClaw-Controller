#!/bin/bash

# Minecraft Client Controller 启动脚本

SKILL_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SKILL_DIR"

# 检查 node_modules
if [ ! -d "node_modules" ]; then
    echo "📦 首次运行，正在安装依赖..."
    npm install
fi

# 启动 Agent
echo "🎮 启动 Minecraft 客户端控制器..."
echo "🔗 连接到: $(grep -o '"host": "[^"]*"' config.json | head -1 | cut -d'"' -f4):$(grep -o '"port": [0-9]*' config.json | head -1 | cut -d' ' -f2)"
echo ""
echo "请确保:"
echo "  1. Minecraft 客户端已启动"
echo "  2. Client Controller Mod 已安装"
echo "  3. 游戏内 WebSocket 服务器已启动"
echo ""

node agent.js
