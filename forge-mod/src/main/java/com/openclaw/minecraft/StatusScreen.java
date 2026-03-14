package com.openclaw.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 连接状态界面 - 显示详细的 WebSocket 连接信息
 */
public class StatusScreen extends Screen {
    
    private final Screen parentScreen;
    
    public StatusScreen(Screen parent) {
        super(Component.literal("连接状态"));
        this.parentScreen = parent;
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        
        // 返回按钮
        this.addRenderableWidget(Button.builder(
            Component.literal("返回游戏"),
            button -> this.minecraft.setScreen(parentScreen)
        ).bounds(centerX - 100, this.height - 35, 200, 20).build());
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // 1. 渲染背景（模糊）
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        
        // 2. 渲染所有组件（按钮等）
        for (var widget : this.renderables) {
            widget.render(graphics, mouseX, mouseY, partialTicks);
        }
        
        // 3. 渲染文字（在最上层，确保在模糊背景之上）
        int centerX = this.width / 2;
        int startY = 40;
        
        // 标题
        graphics.drawCenteredString(this.font, this.title.getString(), centerX, startY, 0xFFFFFF);
        
        // 连接状态（大标题）
        boolean connected = ClientControllerMod.isConnected();
        String statusText = connected ? "已连接" : "未连接";
        int statusColor = connected ? 0x55FF55 : 0xFF5555;
        
        graphics.drawCenteredString(this.font, statusText, centerX, startY + 25, statusColor);
        
        // 服务器配置信息
        int y = startY + 55;
        graphics.drawString(this.font, "服务器配置:", centerX - 100, y, 0xFFFF55);
        y += 20;
        
        graphics.drawString(this.font, "地址: " + ClientControllerMod.SERVER_HOST, centerX - 100, y, 0xFFFFFF);
        y += 20;
        
        graphics.drawString(this.font, "端口: " + ClientControllerMod.SERVER_PORT, centerX - 100, y, 0xFFFFFF);
        y += 20;
        
        graphics.drawString(this.font, "URL: ws://" + ClientControllerMod.SERVER_HOST + ":" + ClientControllerMod.SERVER_PORT, 
            centerX - 100, y, 0xAAAAAA);
        y += 28;
        
        // 连接详情
        graphics.drawString(this.font, "连接详情:", centerX - 100, y, 0xFFFF55);
        y += 20;
        
        if (connected) {
            graphics.drawString(this.font, "状态: 正常", centerX - 100, y, 0x55FF55);
        } else {
            graphics.drawString(this.font, "状态: 未连接", centerX - 100, y, 0xFF5555);
            y += 20;
            graphics.drawString(this.font, "提示: 按 C 键打开配置界面", centerX - 100, y, 0xAAAAAA);
        }
        y += 28;
        
        // 快捷键说明
        graphics.drawString(this.font, "快捷键:", centerX - 100, y, 0xFFFF55);
        y += 20;
        
        graphics.drawString(this.font, "C - 配置界面", centerX - 100, y, 0xFFFFFF);
        y += 20;
        
        graphics.drawString(this.font, "V - 状态界面", centerX - 100, y, 0xFFFFFF);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
