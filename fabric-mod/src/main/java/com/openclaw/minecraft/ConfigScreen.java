package com.openclaw.minecraft;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * 配置界面 - 在游戏内设置 WebSocket 服务器地址和查看连接状态
 */
public class ConfigScreen extends Screen {
    
    private final Screen parentScreen;
    private TextFieldWidget hostField;
    private TextFieldWidget portField;
    
    private String statusMessage = "";
    private int statusColor = 0xFFFFFF;
    
    public ConfigScreen(Screen parent) {
        super(Text.literal("OpenClaw 控制器配置"));
        this.parentScreen = parent;
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 40;
        
        // 服务器地址输入框
        this.hostField = new TextFieldWidget(this.textRenderer, centerX - 100, startY + 30, 200, 20, 
            Text.literal(""));
        this.hostField.setText(ClientControllerMod.SERVER_HOST);
        this.hostField.setMaxLength(100);
        this.hostField.setPlaceholder(Text.literal("输入服务器地址"));
        this.addDrawableChild(this.hostField);
        
        // 端口输入框
        this.portField = new TextFieldWidget(this.textRenderer, centerX - 100, startY + 75, 200, 20,
            Text.literal(""));
        this.portField.setText(String.valueOf(ClientControllerMod.SERVER_PORT));
        this.portField.setMaxLength(5);
        this.portField.setPlaceholder(Text.literal("输入端口号"));
        this.addDrawableChild(this.portField);
        
        // 连接按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("连接服务器"),
            button -> connectToServer()
        ).dimensions(centerX - 100, startY + 110, 95, 20).build());
        
        // 断开按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("断开连接"),
            button -> disconnectFromServer()
        ).dimensions(centerX + 5, startY + 110, 95, 20).build());
        
        // 保存配置按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("保存配置"),
            button -> saveConfig()
        ).dimensions(centerX - 100, startY + 140, 200, 20).build());
        
        // 返回按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("返回游戏"),
            button -> this.client.setScreen(parentScreen)
        ).dimensions(centerX - 100, this.height - 30, 200, 20).build());
        
        // 设置初始焦点
        this.setFocused(this.hostField);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 渲染背景
        this.renderBackground(context, mouseX, mouseY, delta);
        
        // 标题
        context.drawCenteredTextWithShadow(this.textRenderer, this.title.getString(), this.width / 2, 15, 0xFFFFFF);
        
        // 标签
        context.drawTextWithShadow(this.textRenderer, "服务器地址", this.width / 2 - 100, 42, 0xFFFF55);
        context.drawTextWithShadow(this.textRenderer, "端口", this.width / 2 - 100, 87, 0xFFFF55);
        
        // 连接状态
        String status = ClientControllerMod.isConnected() ? "已连接" : "未连接";
        int color = ClientControllerMod.isConnected() ? 0x55FF55 : 0xFF5555;
        context.drawTextWithShadow(this.textRenderer, "连接状态: " + status, this.width / 2 - 100, 175, color);
        
        // 当前配置信息
        if (!ClientControllerMod.SERVER_HOST.equals("localhost")) {
            context.drawTextWithShadow(this.textRenderer, 
                "当前: " + ClientControllerMod.SERVER_HOST + ":" + ClientControllerMod.SERVER_PORT, 
                this.width / 2 - 100, 190, 0xAAAAAA);
        }
        
        // 状态消息
        if (!statusMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, statusMessage, this.width / 2, 210, statusColor);
        }
        
        // 提示
        context.drawCenteredTextWithShadow(this.textRenderer, "提示: 修改后先保存再连接", 
            this.width / 2, this.height - 45, 0x888888);
        
        // 渲染所有组件
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.hostField.charTyped(chr, modifiers)) {
            return true;
        }
        if (this.portField.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Tab 键切换焦点
        if (keyCode == 258) {
            if (this.hostField.isFocused()) {
                this.hostField.setFocused(false);
                this.portField.setFocused(true);
                this.setFocused(this.portField);
            } else {
                this.portField.setFocused(false);
                this.hostField.setFocused(true);
                this.setFocused(this.hostField);
            }
            return true;
        }
        
        // 输入框处理按键
        if (this.hostField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.portField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 先让父类处理（按钮点击）
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        
        // 检查是否点击了输入框
        if (this.hostField.isMouseOver(mouseX, mouseY)) {
            this.hostField.setFocused(true);
            this.portField.setFocused(false);
            this.setFocused(this.hostField);
            return true;
        }
        if (this.portField.isMouseOver(mouseX, mouseY)) {
            this.hostField.setFocused(false);
            this.portField.setFocused(true);
            this.setFocused(this.portField);
            return true;
        }
        
        // 点击其他地方取消焦点
        if (!handled) {
            this.hostField.setFocused(false);
            this.portField.setFocused(false);
        }
        
        return handled;
    }
    
    private void connectToServer() {
        try {
            String host = this.hostField.getText().trim();
            int port = Integer.parseInt(this.portField.getText().trim());
            
            if (host.isEmpty()) {
                setStatus("错误: 服务器地址不能为空", 0xFF5555);
                return;
            }
            
            ClientControllerMod.SERVER_HOST = host;
            ClientControllerMod.SERVER_PORT = port;
            
            setStatus("正在连接...", 0xFFFF55);
            
            // 在新线程中连接
            new Thread(() -> {
                ClientControllerMod.connectToServer();
                try {
                    Thread.sleep(1000);
                    Minecraft.getInstance().execute(() -> {
                        if (ClientControllerMod.isConnected()) {
                            setStatus("连接成功!", 0x55FF55);
                        } else {
                            setStatus("连接失败，请检查地址和端口", 0xFF5555);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            
        } catch (NumberFormatException e) {
            setStatus("错误: 端口必须是数字", 0xFF5555);
        }
    }
    
    private void disconnectFromServer() {
        ClientControllerMod.disconnectFromServer();
        setStatus("已断开连接", 0xFFAA55);
    }
    
    private void saveConfig() {
        try {
            String host = this.hostField.getText().trim();
            int port = Integer.parseInt(this.portField.getText().trim());
            
            if (host.isEmpty()) {
                setStatus("错误: 服务器地址不能为空", 0xFF5555);
                return;
            }
            
            ClientControllerMod.SERVER_HOST = host;
            ClientControllerMod.SERVER_PORT = port;
            ClientControllerMod.saveConfig();
            setStatus("配置已保存!", 0x55FF55);
        } catch (NumberFormatException e) {
            setStatus("错误: 端口必须是数字", 0xFF5555);
        }
    }
    
    private void setStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
