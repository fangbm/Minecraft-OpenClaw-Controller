# NeoForge Mod - Minecraft OpenClaw Controller

这是 **Minecraft OpenClaw Controller** 的 **NeoForge** 版本，适用于 Minecraft 1.21.1。

## 与 Forge 版本的区别

| 特性 | Forge 版本 | NeoForge 版本 |
|------|-----------|--------------|
| 加载器 | Forge 1.21.1 | NeoForge 1.21.1 |
| 构建系统 | ForgeGradle | NeoGradle |
| 配置系统 | ForgeConfigSpec | ModConfigSpec |
| 事件总线 | FMLJavaModLoadingContext | NeoForge 事件总线 |

## 构建

```bash
./gradlew build
```

构建输出位于 `build/libs/` 目录。

## 安装

1. 安装 NeoForge 1.21.1
2. 将构建的 jar 文件放入 `.minecraft/mods/` 文件夹
3. 启动游戏

## 配置

配置文件位置：`.minecraft/config/client-controller.properties`

```properties
server.host=localhost
server.port=8080
auto.connect=false
```

## 快捷键

- **C** - 打开配置界面
- **V** - 查看连接状态

## 与服务器端配合使用

需要配合 OpenClaw 的 WebSocket 服务器使用：

```bash
cd ../scripts
npm install
node server.js
```

## 许可证

MIT License
