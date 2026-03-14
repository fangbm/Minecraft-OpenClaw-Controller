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
        
        long window = mc.getWindow().getWindow();
        
        // 直接设置按键状态
        if (keyCode == GLFW.GLFW_KEY_W) mc.options.keyUp.setDown(true);
        else if (keyCode == GLFW.GLFW_KEY_S) mc.options.keyDown.setDown(true);
        else if (keyCode == GLFW.GLFW_KEY_A) mc.options.keyLeft.setDown(true);
        else if (keyCode == GLFW.GLFW_KEY_D) mc.options.keyRight.setDown(true);
        else if (keyCode == GLFW.GLFW_KEY_SPACE) mc.options.keyJump.setDown(true);
        else if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) mc.options.keyShift.setDown(true);
        else if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL) mc.options.keySprint.setDown(true);
    }
    
    /**
     * 松开按键
     */
    public static void releaseKey(int keyCode) {
        if (mc == null) return;
        
        if (keyCode == GLFW.GLFW_KEY_W) mc.options.keyUp.setDown(false);
        else if (keyCode == GLFW.GLFW_KEY_S) mc.options.keyDown.setDown(false);
        else if (keyCode == GLFW.GLFW_KEY_A) mc.options.keyLeft.setDown(false);
        else if (keyCode == GLFW.GLFW_KEY_D) mc.options.keyRight.setDown(false);
        else if (keyCode == GLFW.GLFW_KEY_SPACE) mc.options.keyJump.setDown(false);
        else if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) mc.options.keyShift.setDown(false);
        else if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL) mc.options.keySprint.setDown(false);
    }
}
