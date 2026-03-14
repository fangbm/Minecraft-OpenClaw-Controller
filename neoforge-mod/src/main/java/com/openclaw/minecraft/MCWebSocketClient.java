package com.openclaw.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 客户端 - 主动连接 OpenClaw 服务器
 */
public class MCWebSocketClient extends WebSocketClient {
    
    private final Gson gson = new Gson();
    
    public MCWebSocketClient(String serverUri) {
        super(URI.create(serverUri));
    }
    
    @Override
    public void onOpen(ServerHandshake handshake) {
        ClientControllerMod.LOGGER.info("已连接到 OpenClaw 服务器!");
        
        // 发送连接成功事件
        JsonObject welcome = new JsonObject();
        welcome.addProperty("type", "connected");
        welcome.addProperty("message", "Minecraft Client 已连接");
        send(gson.toJson(welcome));
        
        // 发送玩家信息
        sendPlayerInfo();
    }
    
    @Override
    public void onMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String type = json.get("type").getAsString();
            
            if ("command".equals(type)) {
                handleCommand(json);
            } else {
                sendError(json.has("id") ? json.get("id").getAsString() : null, "未知消息类型");
            }
        } catch (Exception e) {
            ClientControllerMod.LOGGER.error("处理消息时出错: {}", e.getMessage());
        }
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        ClientControllerMod.LOGGER.info("与 OpenClaw 服务器断开连接: {} (code: {})", reason, code);
    }
    
    @Override
    public void onError(Exception ex) {
        ClientControllerMod.LOGGER.error("WebSocket 错误: {}", ex.getMessage());
    }
    
    /**
     * 处理命令
     */
    private void handleCommand(JsonObject json) {
        String action = json.get("action").getAsString();
        JsonObject data = json.has("data") ? json.getAsJsonObject("data") : new JsonObject();
        String cmdId = json.has("id") ? json.get("id").getAsString() : null;
        
        Minecraft mc = Minecraft.getInstance();
        
        mc.execute(() -> {
            try {
                Object result = CommandHandler.execute(action, data);
                sendResponse(cmdId, true, result);
            } catch (Exception e) {
                ClientControllerMod.LOGGER.error("执行命令失败: {} - {}", action, e.getMessage());
                sendError(cmdId, e.getMessage());
            }
        });
    }
    
    /**
     * 发送响应
     */
    private void sendResponse(String id, boolean success, Object data) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "response");
        if (id != null) response.addProperty("id", id);
        response.addProperty("success", success);
        response.add("data", gson.toJsonTree(data));
        send(gson.toJson(response));
    }
    
    /**
     * 发送错误
     */
    private void sendError(String id, String error) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "response");
        if (id != null) response.addProperty("id", id);
        response.addProperty("success", false);
        response.addProperty("error", error);
        send(gson.toJson(response));
    }
    
    /**
     * 发送事件
     */
    public void sendEvent(String event, Object data) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "event");
        message.addProperty("event", event);
        message.add("data", gson.toJsonTree(data));
        send(gson.toJson(message));
    }
    
    /**
     * 发送玩家信息
     */
    private void sendPlayerInfo() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null) return;
        
        Map<String, Object> info = new HashMap<>();
        info.put("name", player.getName().getString());
        info.put("uuid", player.getUUID().toString());
        
        Vec3 pos = player.position();
        info.put("position", Map.of(
            "x", Math.round(pos.x * 100) / 100.0,
            "y", Math.round(pos.y * 100) / 100.0,
            "z", Math.round(pos.z * 100) / 100.0
        ));
        
        sendEvent("playerInfo", info);
    }
}
