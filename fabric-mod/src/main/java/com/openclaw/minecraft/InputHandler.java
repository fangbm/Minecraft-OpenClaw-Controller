package com.openclaw.minecraft;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * 输入处理器 - 模拟键盘和鼠标输入
 */
public class InputHandler {
    
    private static Minecraft mc;
    
    public static void init(Minecraft minecraft) {
        mc = minecraft;
    }
    
    /**
     * 按下按键
     */
    public static void pressKey(int keyCode) {
        if (mc == null) return;
        
        if (keyCode == GLFW.GLFW_KEY_W) mc.options.forwardKey.setPressed(true);
        else if (keyCode == GLFW.GLFW_KEY_S) mc.options.backKey.setPressed(true);
        else if (keyCode == GLFW.GLFW_KEY_A) mc.options.leftKey.setPressed(true);
        else if (keyCode == GLFW.GLFW_KEY_D) mc.options.rightKey.setPressed(true);
        else if (keyCode == GLFW.GLFW_KEY_SPACE) mc.options.jumpKey.setPressed(true);
        else if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) mc.options.sneakKey.setPressed(true);
        else if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL) mc.options.sprintKey.setPressed(true);
    }
    
    /**
     * 松开按键
     */
    public static void releaseKey(int keyCode) {
        if (mc == null) return;
        
        if (keyCode == GLFW.GLFW_KEY_W) mc.options.forwardKey.setPressed(false);
        else if (keyCode == GLFW.GLFW_KEY_S) mc.options.backKey.setPressed(false);
        else if (keyCode == GLFW.GLFW_KEY_A) mc.options.leftKey.setPressed(false);
        else if (keyCode == GLFW.GLFW_KEY_D) mc.options.rightKey.setPressed(false);
        else if (keyCode == GLFW.GLFW_KEY_SPACE) mc.options.jumpKey.setPressed(false);
        else if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) mc.options.sneakKey.setPressed(false);
        else if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL) mc.options.sprintKey.setPressed(false);
    }
}
