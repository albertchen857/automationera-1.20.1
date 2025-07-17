package com.automationera.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public class ItemIconButton extends ButtonWidget {
    private final ItemStack stack;
    private static final ButtonTextures TEXTURES = new ButtonTextures(Identifier.ofVanilla("widget/button"), Identifier.ofVanilla("widget/button_disabled"), Identifier.ofVanilla("widget/button_highlighted"));

    public ItemIconButton(ItemStack stack, Text text, int x, int y, int width, int height, ButtonWidget.PressAction action) {
        super(x, y, width, height, text, action, Supplier::get);
        this.stack = stack;
    }

    @Override
    public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // 绘制按钮背景
        ctx.drawGuiTexture(TEXTURES.get(this.active, this.isSelected()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        // 绘制物品图标 (左边留点边距)
        int iconX = this.getX() + 2;
        int iconY = this.getY() + (this.getHeight() - 16) / 2;
        ctx.drawItem(stack, iconX, iconY);

        // 绘制文字 (图标右边留 margin)
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textX = iconX + 20; // 图标宽度16 + margin 4
        int textY = this.getY() + (this.getHeight() - 8) / 2; // 居中
        ctx.drawTextWithShadow(
                textRenderer,
                this.getMessage(),
                textX,
                textY,
                this.active ? 0xFFFFFF : 0xA0A0A0
        );
    }
}

