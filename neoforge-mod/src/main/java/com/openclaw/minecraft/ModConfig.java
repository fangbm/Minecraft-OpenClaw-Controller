package com.openclaw.minecraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Mod 配置管理 - NeoForge ModConfigSpec
 */
public class ModConfig {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    // WebSocket 配置
    public static final ModConfigSpec.IntValue WEBSOCKET_PORT;
    public static final ModConfigSpec.BooleanValue AUTO_START;
    
    // 输入配置
    public static final ModConfigSpec.DoubleValue MOUSE_SENSITIVITY;
    public static final ModConfigSpec.DoubleValue MOVEMENT_SPEED;
    
    // 调试配置
    public static final ModConfigSpec.BooleanValue DEBUG;
    
    static {
        BUILDER.push("websocket");
        WEBSOCKET_PORT = BUILDER
            .comment("WebSocket 服务器端口")
            .defineInRange("port", 8080, 1024, 65535);
        AUTO_START = BUILDER
            .comment("是否自动启动 WebSocket 服务器")
            .define("autoStart", true);
        BUILDER.pop();
        
        BUILDER.push("input");
        MOUSE_SENSITIVITY = BUILDER
            .comment("鼠标灵敏度")
            .defineInRange("mouseSensitivity", 1.0, 0.1, 10.0);
        MOVEMENT_SPEED = BUILDER
            .comment("移动速度")
            .defineInRange("movementSpeed", 1.0, 0.1, 10.0);
        BUILDER.pop();
        
        BUILDER.push("debug");
        DEBUG = BUILDER
            .comment("启用调试模式")
            .define("debug", false);
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}
