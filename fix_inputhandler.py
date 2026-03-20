import re

with open('fabric-mod/src/main/java/com/openclaw/minecraft/InputHandler.java', 'r') as f:
    content = f.read()

# 替换import
content = re.sub(r'import net\.minecraft\.client\.Minecraft;', 
                'import net.minecraft.client.MinecraftClient;', content)

# 替换类型引用
content = re.sub(r'\bMinecraft\b(?!Client)', 'MinecraftClient', content)

with open('fabric-mod/src/main/java/com/openclaw/minecraft/InputHandler.java', 'w') as f:
    f.write(content)

print('InputHandler.java 已修复')
