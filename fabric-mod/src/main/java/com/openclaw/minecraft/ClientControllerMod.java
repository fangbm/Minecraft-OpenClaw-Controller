package com.openclaw.minecraft;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

/**
 * MinecraftClient Client Controller - Fabric Mod for 1.21.1
 * 客户端主动连接 OpenClaw 服务器的 WebSocket
 */
public class ClientControllerMod implements ClientModInitializer {
    public static final String MOD_ID = "client_controller";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static MCWebSocketClient wsClient;
    private static KeyBinding configKey;
    private static KeyBinding statusKey;
    
    // 配置值（从配置文件读取）
    public static String SERVER_HOST = "localhost";
    public static int SERVER_PORT = 8080;
    public static boolean AUTO_CONNECT = false;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Client Controller Mod for Fabric 1.21.1...");
        
        // 从配置文件读取
        loadConfig();
        
        // 注册键位绑定
        registerKeyBindings();
        
        // 注册 tick 事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 检查快捷键 - 打开配置界面
            if (configKey.wasPressed()) {
                client.setScreen(new ConfigScreen(client.currentScreen));
            }
            
            // 检查快捷键 - 打开状态界面
            if (statusKey.wasPressed()) {
                client.setScreen(new StatusScreen(client.currentScreen));
            }
        });
        
        // 注册连接事件
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LOGGER.info("玩家加入世界");
            
            // 初始化输入处理器
            InputHandler.init(client);
            
            // 自动连接
            if (AUTO_CONNECT) {
                connectToServer();
            }
        });
        
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.info("玩家断开连接，关闭 WebSocket");
            disconnectFromServer();
        });
        
        LOGGER.info("Client Controller Mod 已加载!");
        LOGGER.info("OpenClaw 服务器: {}:{}", SERVER_HOST, SERVER_PORT);
        LOGGER.info("按 C 键打开配置界面");
        LOGGER.info("按 V 键查看连接状态");
    }
    
    /**
     * 注册键位绑定
     */
    private void registerKeyBindings() {
        // 注册配置快捷键 (C键)
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.client-controller.config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "category.client-controller.general"
        ));
        
        // 注册状态快捷键 (V键)
        statusKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.client-controller.status",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.client-controller.general"
        ));
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfig() {
        File configDir = new File(Minecraft.getInstance().runDirectory, "config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        File configFile = new File(configDir, "client-controller.properties");
        
        // 如果配置文件不存在，创建默认配置
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }
        
        // 读取配置
        Properties props = new Properties();
        try (FileReader reader = new FileReader(configFile)) {
            props.load(reader);
            
            SERVER_HOST = props.getProperty("server.host", "localhost");
            SERVER_PORT = Integer.parseInt(props.getProperty("server.port", "8080"));
            AUTO_CONNECT = Boolean.parseBoolean(props.getProperty("auto.connect", "false"));
            
            LOGGER.info("已加载配置文件: {}", configFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("加载配置文件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 保存配置文件
     */
    public static void saveConfig() {
        File configDir = new File(Minecraft.getInstance().runDirectory, "config");
        File configFile = new File(configDir, "client-controller.properties");
        
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("# MinecraftClient Client Controller 配置文件\n");
            writer.write("# OpenClaw 服务器地址\n");
            writer.write("server.host=" + SERVER_HOST + "\n");
            writer.write("\n");
            writer.write("# OpenClaw 服务器端口\n");
            writer.write("server.port=" + SERVER_PORT + "\n");
            writer.write("\n");
            writer.write("# 是否自动连接\n");
            writer.write("auto.connect=" + AUTO_CONNECT + "\n");
            LOGGER.info("配置已保存到: {}", configFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("保存配置文件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 创建默认配置文件
     */
    private void createDefaultConfig(File configFile) {
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("# MinecraftClient Client Controller 配置文件\n");
            writer.write("# OpenClaw 服务器地址\n");
            writer.write("server.host=localhost\n");
            writer.write("\n");
            writer.write("# OpenClaw 服务器端口\n");
            writer.write("server.port=8080\n");
            writer.write("\n");
            writer.write("# 是否自动连接\n");
            writer.write("auto.connect=false\n");
            LOGGER.info("已创建默认配置文件: {}", configFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("创建配置文件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 连接到 OpenClaw 服务器
     */
    public static void connectToServer() {
        if (wsClient != null && wsClient.isOpen()) {
            LOGGER.info("已经连接到 OpenClaw 服务器");
            return;
        }
        
        String wsUrl = "ws://" + SERVER_HOST + ":" + SERVER_PORT + "/minecraft";
        LOGGER.info("正在连接 OpenClaw 服务器: {}", wsUrl);
        
        wsClient = new MCWebSocketClient(wsUrl);
        wsClient.connect();
    }
    
    /**
     * 断开连接
     */
    public static void disconnectFromServer() {
        if (wsClient != null) {
            try {
                wsClient.close();
                LOGGER.info("已断开与 OpenClaw 服务器的连接");
            } catch (Exception e) {
                LOGGER.error("断开连接时出错: {}", e.getMessage());
            }
            wsClient = null;
        }
    }
    
    /**
     * 检查是否已连接
     */
    public static boolean isConnected() {
        return wsClient != null && wsClient.isOpen();
    }
    
    /**
     * 获取 WebSocket 客户端
     */
    public static MCWebSocketClient getWebSocketClient() {
        return wsClient;
    }
    
    /**
     * 发送事件到 OpenClaw
     */
    public static void sendEvent(String event, Object data) {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.sendEvent(event, data);
        }
    }
}
