package com.openclaw.minecraft;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import com.google.gson.JsonObject;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 命令处理器 - 执行 OpenClaw 发送的各种命令
 */
public class CommandHandler {
    
    private static final Map<String, Boolean> pressedKeys = new HashMap<>();
    private static final Map<String, Boolean> pressedMouse = new HashMap<>();
    
    /**
     * 执行命令
     */
    public static Object execute(String action, JsonObject data) {
        MinecraftClient mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        
        if (player == null) {
            throw new IllegalStateException("玩家不存在");
        }
        
        switch (action) {
            // 键盘控制
            case "keyDown":
                return handleKeyDown(mc, data.get("key").getAsString());
            case "keyUp":
                return handleKeyUp(mc, data.get("key").getAsString());
            case "typeText":
                return handleTypeText(mc, data.get("text").getAsString());
                
            // 鼠标控制
            case "moveMouse":
                return handleMoveMouse(mc, data.get("deltaX").getAsInt(), data.get("deltaY").getAsInt());
            case "setRotation":
                return handleSetRotation(player, data.get("yaw").getAsFloat(), data.get("pitch").getAsFloat());
            case "lookAt":
                return handleLookAt(player, data.get("x").getAsDouble(), data.get("y").getAsDouble(), data.get("z").getAsDouble());
            case "click":
                return handleClick(mc, data.get("button").getAsString());
            case "mouseDown":
                return handleMouseDown(mc, data.get("button").getAsString());
            case "mouseUp":
                return handleMouseUp(mc, data.get("button").getAsString());
            case "scroll":
                return handleScroll(mc, data.get("delta").getAsInt());
                
            // 快捷操作
            case "selectSlot":
                return handleSelectSlot(player, data.get("slot").getAsInt());
                
            // 信息获取
            case "getStatus":
                return handleGetStatus(player);
            case "getVisibleEntities":
                return handleGetVisibleEntities(player);
            case "getTargetedBlock":
                return handleGetTargetedBlock(mc);
            case "screenshot":
                return handleScreenshot(mc);
                
            default:
                throw new IllegalArgumentException("未知命令: " + action);
        }
    }
    
    // ==================== 键盘控制 ====================
    
    private static boolean handleKeyDown(MinecraftClient mc, String key) {
        int keyCode = getKeyCode(key);
        
        if (keyCode != -1) {
            mc.execute(() -> {
                InputHandler.pressKey(keyCode);
            });
            pressedKeys.put(key, true);
            return true;
        }
        return false;
    }
    
    private static boolean handleKeyUp(MinecraftClient mc, String key) {
        int keyCode = getKeyCode(key);
        
        if (keyCode != -1) {
            mc.execute(() -> {
                InputHandler.releaseKey(keyCode);
            });
            pressedKeys.put(key, false);
            return true;
        }
        return false;
    }
    
    private static boolean handleTypeText(MinecraftClient mc, String text) {
        mc.execute(() -> {
            if (mc.currentScreen != null) {
                // 向当前 GUI 输入文本
                for (char c : text.toCharArray()) {
                    mc.currentScreen.charTyped(c, 0);
                }
            }
        });
        return true;
    }
    
    // ==================== 鼠标控制 ====================
    
    private static boolean handleMoveMouse(MinecraftClient mc, int deltaX, int deltaY) {
        mc.execute(() -> {
            // 获取当前鼠标灵敏度
            double sensitivity = mc.options.getMouseSensitivity().getValue();
            
            // 应用移动
            double f = sensitivity * 0.6 + 0.2;
            double g = f * f * f * 8.0;
            
            double h = (double) deltaX * g;
            double i = (double) deltaY * g;
            
            // 水平移动（yaw）
            mc.player.changeLookDirection(h, 0);
            // 垂直移动（pitch）
            mc.player.changeLookDirection(0, i);
        });
        return true;
    }
    
    private static boolean handleSetRotation(ClientPlayerEntity player, float yaw, float pitch) {
        player.setYaw(yaw);
        player.setPitch(MathHelper.clamp(pitch, -90, 90));
        return true;
    }
    
    private static boolean handleLookAt(ClientPlayerEntity player, double x, double y, double z) {
        Vec3d eyePos = player.getEyePos();
        double dx = x - eyePos.x;
        double dy = y - eyePos.y;
        double dz = z - eyePos.z;
        
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, distance));
        
        player.setYaw(yaw);
        player.setPitch(MathHelper.clamp(pitch, -90, 90));
        
        return true;
    }
    
    private static boolean handleClick(MinecraftClient mc, String button) {
        mc.execute(() -> {
            if ("left".equals(button)) {
                // 左键点击
                mc.options.attackKey.setPressed(true);
                mc.options.attackKey.setPressed(false);
            } else if ("right".equals(button)) {
                // 右键点击
                mc.options.useKey.setPressed(true);
                mc.options.useKey.setPressed(false);
            }
        });
        return true;
    }
    
    private static boolean handleMouseDown(MinecraftClient mc, String button) {
        mc.execute(() -> {
            if ("left".equals(button)) {
                mc.options.attackKey.setPressed(true);
            } else if ("right".equals(button)) {
                mc.options.useKey.setPressed(true);
            }
        });
        pressedMouse.put(button, true);
        return true;
    }
    
    private static boolean handleMouseUp(MinecraftClient mc, String button) {
        mc.execute(() -> {
            if ("left".equals(button)) {
                mc.options.attackKey.setPressed(false);
            } else if ("right".equals(button)) {
                mc.options.useKey.setPressed(false);
            }
        });
        pressedMouse.put(button, false);
        return true;
    }
    
    private static boolean handleScroll(MinecraftClient mc, int delta) {
        mc.execute(() -> {
            // 模拟滚轮
            mc.player.getInventory().scrollInHotbar(delta > 0 ? -1 : 1);
        });
        return true;
    }
    
    // ==================== 快捷操作 ====================
    
    private static boolean handleSelectSlot(ClientPlayerEntity player, int slot) {
        if (slot >= 0 && slot < 9) {
            player.getInventory().selectedSlot = slot;
            return true;
        }
        return false;
    }
    
    // ==================== 信息获取 ====================
    
    private static Map<String, Object> handleGetStatus(ClientPlayerEntity player) {
        Map<String, Object> status = new HashMap<>();
        
        // 位置
        Vec3d pos = player.getPos();
        status.put("position", Map.of(
            "x", Math.round(pos.x * 100) / 100.0,
            "y", Math.round(pos.y * 100) / 100.0,
            "z", Math.round(pos.z * 100) / 100.0
        ));
        
        // 视角
        status.put("rotation", Map.of(
            "yaw", Math.round(player.getYaw() * 100) / 100.0,
            "pitch", Math.round(player.getPitch() * 100) / 100.0
        ));
        
        // 生命值和饥饿度
        status.put("health", Math.round(player.getHealth() * 10) / 10.0);
        status.put("food", player.getHungerManager().getFoodLevel());
        status.put("saturation", Math.round(player.getHungerManager().getSaturationLevel() * 10) / 10.0);
        
        // 经验
        status.put("experience", player.experienceProgress);
        status.put("experienceLevel", player.experienceLevel);
        
        // 背包
        List<Map<String, Object>> inventory = new ArrayList<>();
        for (int i = 0; i < player.getInventory().main.size(); i++) {
            ItemStack stack = player.getInventory().main.get(i);
            if (!stack.isEmpty()) {
                inventory.add(Map.of(
                    "slot", i,
                    "name", stack.getName().getString(),
                    "count", stack.getCount()
                ));
            }
        }
        status.put("inventory", inventory);
        status.put("selectedSlot", player.getInventory().selectedSlot);
        
        return status;
    }
    
    private static List<Map<String, Object>> handleGetVisibleEntities(ClientPlayerEntity player) {
        List<Map<String, Object>> entities = new ArrayList<>();
        
        double range = 64.0; // 视野范围
        Vec3d playerPos = player.getPos();
        
        for (Entity entity : player.getWorld().getEntitiesByClass(Entity.class, player.getBoundingBox().expand(range), e -> true)) {
            if (entity == player) continue;
            
            double distance = entity.getPos().distanceTo(playerPos);
            if (distance <= range) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", entity.getId());
                info.put("type", entity.getType().getUntranslatedName());
                info.put("name", entity.getName().getString());
                info.put("distance", Math.round(distance * 10) / 10.0);
                info.put("position", Map.of(
                    "x", Math.round(entity.getX() * 100) / 100.0,
                    "y", Math.round(entity.getY() * 100) / 100.0,
                    "z", Math.round(entity.getZ() * 100) / 100.0
                ));
                
                if (entity instanceof PlayerEntity) {
                    info.put("health", ((PlayerEntity) entity).getHealth());
                }
                
                entities.add(info);
            }
        }
        
        return entities;
    }
    
    private static Map<String, Object> handleGetTargetedBlock(MinecraftClient mc) {
        HitResult hit = mc.crosshairTarget;
        
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos pos = blockHit.getBlockPos();
            
            return Map.of(
                "type", mc.world.getBlockState(pos).getBlock().getName().getString(),
                "position", Map.of("x", pos.getX(), "y", pos.getY(), "z", pos.getZ()),
                "face", blockHit.getSide().getName()
            );
        }
        
        return null;
    }
    
    private static String handleScreenshot(MinecraftClient mc) {
        return "screenshot_not_implemented";
    }
    
    // ==================== 辅助方法 ====================
    
    private static int getKeyCode(String key) {
        return switch (key.toLowerCase()) {
            case "w" -> GLFW.GLFW_KEY_W;
            case "a" -> GLFW.GLFW_KEY_A;
            case "s" -> GLFW.GLFW_KEY_S;
            case "d" -> GLFW.GLFW_KEY_D;
            case "space" -> GLFW.GLFW_KEY_SPACE;
            case "shift" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "ctrl" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "alt" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "e" -> GLFW.GLFW_KEY_E;
            case "q" -> GLFW.GLFW_KEY_Q;
            case "t" -> GLFW.GLFW_KEY_T;
            case "escape" -> GLFW.GLFW_KEY_ESCAPE;
            case "return", "enter" -> GLFW.GLFW_KEY_ENTER;
            case "tab" -> GLFW.GLFW_KEY_TAB;
            case "1" -> GLFW.GLFW_KEY_1;
            case "2" -> GLFW.GLFW_KEY_2;
            case "3" -> GLFW.GLFW_KEY_3;
            case "4" -> GLFW.GLFW_KEY_4;
            case "5" -> GLFW.GLFW_KEY_5;
            case "6" -> GLFW.GLFW_KEY_6;
            case "7" -> GLFW.GLFW_KEY_7;
            case "8" -> GLFW.GLFW_KEY_8;
            case "9" -> GLFW.GLFW_KEY_9;
            default -> -1;
        };
    }
}
