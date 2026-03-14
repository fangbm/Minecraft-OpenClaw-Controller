package com.openclaw.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
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
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
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
    
    private static boolean handleKeyDown(Minecraft mc, String key) {
        long window = mc.getWindow().getWindow();
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
    
    private static boolean handleKeyUp(Minecraft mc, String key) {
        long window = mc.getWindow().getWindow();
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
    
    private static boolean handleTypeText(Minecraft mc, String text) {
        mc.execute(() -> {
            if (mc.screen != null) {
                // 向当前 GUI 输入文本
                for (char c : text.toCharArray()) {
                    mc.screen.charTyped(c, 0);
                }
            }
        });
        return true;
    }
    
    // ==================== 鼠标控制 ====================
    
    private static boolean handleMoveMouse(Minecraft mc, int deltaX, int deltaY) {
        mc.execute(() -> {
            // 获取当前鼠标灵敏度
            double sensitivity = mc.options.sensitivity().get();
            
            // 应用移动
            double f = sensitivity * 0.6 + 0.2;
            double g = f * f * f * 8.0;
            
            double h = (double) deltaX * g;
            double i = (double) deltaY * g;
            
            // 水平移动（yaw）
            mc.player.turn(h, 0);
            // 垂直移动（pitch）
            mc.player.turn(0, i);
        });
        return true;
    }
    
    private static boolean handleSetRotation(LocalPlayer player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.setXRot(Mth.clamp(pitch, -90, 90));
        return true;
    }
    
    private static boolean handleLookAt(LocalPlayer player, double x, double y, double z) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        double dx = x - eyePos.x;
        double dy = y - eyePos.y;
        double dz = z - eyePos.z;
        
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, distance));
        
        player.setYRot(yaw);
        player.setXRot(Mth.clamp(pitch, -90, 90));
        
        return true;
    }
    
    private static boolean handleClick(Minecraft mc, String button) {
        mc.execute(() -> {
            if ("left".equals(button)) {
                // 左键点击
                mc.options.keyAttack.setDown(true);
                mc.options.keyAttack.setDown(false);
            } else if ("right".equals(button)) {
                // 右键点击
                mc.options.keyUse.setDown(true);
                mc.options.keyUse.setDown(false);
            }
        });
        return true;
    }
    
    private static boolean handleMouseDown(Minecraft mc, String button) {
        mc.execute(() -> {
            if ("left".equals(button)) {
                mc.options.keyAttack.setDown(true);
            } else if ("right".equals(button)) {
                mc.options.keyUse.setDown(true);
            }
        });
        pressedMouse.put(button, true);
        return true;
    }
    
    private static boolean handleMouseUp(Minecraft mc, String button) {
        mc.execute(() -> {
            if ("left".equals(button)) {
                mc.options.keyAttack.setDown(false);
            } else if ("right".equals(button)) {
                mc.options.keyUse.setDown(false);
            }
        });
        pressedMouse.put(button, false);
        return true;
    }
    
    private static boolean handleScroll(Minecraft mc, int delta) {
        mc.execute(() -> {
            // 模拟滚轮
            mc.player.getInventory().swapPaint(delta > 0 ? -1 : 1);
        });
        return true;
    }
    
    // ==================== 快捷操作 ====================
    
    private static boolean handleSelectSlot(LocalPlayer player, int slot) {
        if (slot >= 0 && slot < 9) {
            player.getInventory().selected = slot;
            return true;
        }
        return false;
    }
    
    // ==================== 信息获取 ====================
    
    private static Map<String, Object> handleGetStatus(LocalPlayer player) {
        Map<String, Object> status = new HashMap<>();
        
        // 位置
        Vec3 pos = player.position();
        status.put("position", Map.of(
            "x", Math.round(pos.x * 100) / 100.0,
            "y", Math.round(pos.y * 100) / 100.0,
            "z", Math.round(pos.z * 100) / 100.0
        ));
        
        // 视角
        status.put("rotation", Map.of(
            "yaw", Math.round(player.getYRot() * 100) / 100.0,
            "pitch", Math.round(player.getXRot() * 100) / 100.0
        ));
        
        // 生命值和饥饿度
        status.put("health", Math.round(player.getHealth() * 10) / 10.0);
        status.put("food", player.getFoodData().getFoodLevel());
        status.put("saturation", Math.round(player.getFoodData().getSaturationLevel() * 10) / 10.0);
        
        // 经验
        status.put("experience", player.experienceProgress);
        status.put("experienceLevel", player.experienceLevel);
        
        // 游戏模式
        // 游戏模式 - 1.21.1 API 变化，暂时注释掉
        // status.put("gamemode", player.gameMode.getGameModeForPlayer().getSerializedName());
        
        // 背包
        List<Map<String, Object>> inventory = new ArrayList<>();
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                inventory.add(Map.of(
                    "slot", i,
                    "name", stack.getItem().getName(stack).getString(),
                    "count", stack.getCount()
                ));
            }
        }
        status.put("inventory", inventory);
        status.put("selectedSlot", player.getInventory().selected);
        
        return status;
    }
    
    private static List<Map<String, Object>> handleGetVisibleEntities(LocalPlayer player) {
        List<Map<String, Object>> entities = new ArrayList<>();
        
        double range = 64.0; // 视野范围
        Vec3 playerPos = player.position();
        
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(range))) {
            if (entity == player) continue;
            
            double distance = entity.position().distanceTo(playerPos);
            if (distance <= range) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", entity.getId());
                info.put("type", entity.getType().getDescriptionId());
                info.put("name", entity.getName().getString());
                info.put("distance", Math.round(distance * 10) / 10.0);
                info.put("position", Map.of(
                    "x", Math.round(entity.getX() * 100) / 100.0,
                    "y", Math.round(entity.getY() * 100) / 100.0,
                    "z", Math.round(entity.getZ() * 100) / 100.0
                ));
                
                if (entity instanceof Player) {
                    info.put("health", ((Player) entity).getHealth());
                }
                
                entities.add(info);
            }
        }
        
        return entities;
    }
    
    private static Map<String, Object> handleGetTargetedBlock(Minecraft mc) {
        HitResult hit = mc.hitResult;
        
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos pos = blockHit.getBlockPos();
            
            return Map.of(
                "type", mc.level.getBlockState(pos).getBlock().getName().getString(),
                "position", Map.of("x", pos.getX(), "y", pos.getY(), "z", pos.getZ()),
                "face", blockHit.getDirection().getName()
            );
        }
        
        return null;
    }
    
    private static String handleScreenshot(Minecraft mc) {
        // 截图功能需要额外实现
        // 返回 base64 编码的图片或保存路径
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
