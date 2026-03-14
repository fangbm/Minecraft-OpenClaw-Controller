package com.openclaw.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 连接状态界面 - 显示详细的 WebSocket 连接信息
 */
public class StatusScreen extends Screen {
    
    private final Screen parentScreen;
    
    public StatusScreen(Screen parent) {
        super(Text.literal("连接状态"));
        this.parentScreen = parent;
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        
        // 返回按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("返回游戏"),
            button -> this.client.setScreen(parentScreen)
        ).dimensions(centerX - 100, this.height - 35, 200, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. 渲染背景（模糊）
        this.renderBackground(context, mouseX, mouseY, delta);
        
        // 2. 渲染所有组件（按钮等）
        for (var widget : this.drawables) {
            widget.render(context, mouseX, mouseY, delta);
        }
        
        // 3. 渲染文字（在最上层）
        int centerX = this.width / 2;
        int startY = 40;
        
        // 标题
        context.drawCenteredTextWithShadow(this.textRenderer, this.title.getString(), centerX, startY, 0xFFFFFF);
        
        // 连接状态（大标题）
        boolean connected = ClientControllerMod.isConnected();
        String statusText = connected ? "已连接" : "未连接";
        int statusColor = connected ? 0x55FF55 : 0xFF5555;
        
        context.drawCenteredTextWithShadow(this.textRenderer, statusText, centerX, startY + 25, statusColor);
        
        // 服务器配置信息
        int y = startY + 55;
        context.drawTextWithShadow(this.textRenderer, "服务器配置:", centerX - 100, y, 0xFFFF55);
        y += 20;
        
        context.drawTextWithShadow(this.textRenderer, "地址: " + ClientControllerMod.SERVER_HOST, centerX - 100, y, 0xFFFFFF);
        y += 20;
        
        context.drawTextWithShadow(this.textRenderer, "端口: " + ClientControllerMod.SERVER_PORT, centerX - 100, y, 0xFFFFFF);
        y += 20;
        
        context.drawTextWithShadow(this.textRenderer, "URL: ws://" + ClientControllerMod.SERVER_HOST + ":" + ClientControllerMod.SERVER_PORT, 
            centerX - 100, y, 0xAAAAAA);
        y += 28;
        
        // 连接详情
        context.drawTextWithShadow(this.textRenderer, "连接详情:", centerX - 100, y, 0xFFFF55);
        y += 20;
        
        if (connected) {
            context.drawTextWithShadow(this.textRenderer, "状态: 正常", centerX - 100, y, 0x55FF55);
        } else {
            context.drawTextWithShadow(this.textRenderer, "状态: 未连接", centerX - 100, y, 0xFF5555);
            y += 20;
            context.drawTextWithShadow(this.textRenderer, "提示: 按 C 键打开配置界面", centerX - 100, y, 0xAAAAAA);
        }
        y += 28;
        
        // 快捷键说明
        context.drawTextWithShadow(this.textRenderer, "快捷键:", centerX - 100, y, 0xFFFF55);
        y += 20;
        
        context.drawTextWithShadow(this.textRenderer, "C - 配置界面", centerX - 100, y, 0xFFFFFF);
        y += 20;
        
        context.drawTextWithShadow(this.textRenderer, "V - 状态界面", centerX - 100, y, 0xFFFFFF);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
