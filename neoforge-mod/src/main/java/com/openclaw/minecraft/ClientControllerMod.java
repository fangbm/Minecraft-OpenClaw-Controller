package com.openclaw.minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ClientTickEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

/**
 * Minecraft Client Controller - NeoForge Mod for 1.21.1
 * 客户端主动连接 OpenClaw 服务器的 WebSocket
 */
@Mod(ClientControllerMod.MOD_ID)
public class ClientControllerMod {
    public static final String MOD_ID = "client_controller";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static MCWebSocketClient wsClient;
    private static KeyMapping configKey;
    private static KeyMapping statusKey;
    
    // 配置值（从配置文件读取）
    public static String SERVER_HOST = "localhost";
    public static int SERVER_PORT = 8080;
    public static boolean AUTO_CONNECT = false;
    
    public ClientControllerMod() {
        LOGGER.info("Initializing Client Controller Mod for NeoForge 1.21.1...");
        
        // 只在客户端加载
        if (FMLEnvironment.dist == Dist.CLIENT) {
            // 从配置文件读取
            loadConfig();
            
            // 注册事件监听
            NeoForge.EVENT_BUS.register(this);
            
            LOGGER.info("Client Controller Mod 已加载!");
            LOGGER.info("OpenClaw 服务器: {}:{}", SERVER_HOST, SERVER_PORT);
            LOGGER.info("按 C 键打开配置界面");
            LOGGER.info("按 V 键查看连接状态");
        }
    }
    
    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Client setup...");
        
        // 初始化输入处理器
        event.enqueueWork(() -> {
            InputHandler.init(Minecraft.getInstance());
            
            // 自动连接
            if (AUTO_CONNECT) {
                connectToServer();
            }
        });
    }
    
    @SubscribeEvent
    public void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // 注册配置快捷键 (C键)
        configKey = new KeyMapping(
            "key.client-controller.config",
            GLFW.GLFW_KEY_C,
            "category.client-controller.general"
        );
        event.register(configKey);
        
        // 注册状态快捷键 (V键)
        statusKey = new KeyMapping(
            "key.client-controller.status",
            GLFW.GLFW_KEY_V,
            "category.client-controller.general"
        );
        event.register(statusKey);
    }
    
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        // 检查快捷键 - 打开配置界面
        if (configKey != null && configKey.consumeClick()) {
            Minecraft.getInstance().setScreen(new ConfigScreen(Minecraft.getInstance().screen));
        }
        
        // 检查快捷键 - 打开状态界面
        if (statusKey != null && statusKey.consumeClick()) {
            Minecraft.getInstance().setScreen(new StatusScreen(Minecraft.getInstance().screen));
        }
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfig() {
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
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
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
        File configFile = new File(configDir, "client-controller.properties");
        
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("# Minecraft Client Controller 配置文件\n");
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
        Properties props = new Properties();
        props.setProperty("server.host", "localhost");
        props.setProperty("server.port", "8080");
        props.setProperty("auto.connect", "false");
        
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("# Minecraft Client Controller 配置文件\n");
            writer.write("# OpenClaw 服务器地址\n");
            writer.write("server.host=localhost\n");
            writer.write("\n");
            writer.write("# OpenClaw 服务器端口\n");
            writer.write("server.port=8080\n");
            writer.write("\n");
            writer.write("# 是否自动连接\n");
            writer.write("auto.connect=false\n");
            props.store(writer, null);
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
