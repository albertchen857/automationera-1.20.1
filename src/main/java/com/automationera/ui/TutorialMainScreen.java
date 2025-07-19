package com.automationera.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public class TutorialMainScreen extends Screen {
    public TutorialMainScreen() {
        super(Text.translatable("tutorial.ui.main"));
    }

    @Override
    protected void init() {
        int midX = this.width / 2;
        int y = 40;

        int buttonWidth = 60;
        int spacing = 32;
        int totalWidth = 3 * buttonWidth + 2 * spacing;
        int startX = midX - totalWidth / 2;

        // 工厂
        this.addDrawableChild(new ItemIconButton(
                new ItemStack(Items.IRON_INGOT),
                Text.translatable("tutorial.group.factory"),
                startX, y, buttonWidth, 32,
                btn -> this.client.setScreen(new TutorialGroupScreen("factory",0,1))
        ));
        // 农场
        this.addDrawableChild(new ItemIconButton(
                new ItemStack(Items.WHEAT),
                Text.translatable("tutorial.group.farm"),
                startX + buttonWidth + spacing, y, buttonWidth, 32,
                btn -> this.client.setScreen(new TutorialGroupScreen("farm",0,1))
        ));
        // 特殊
        this.addDrawableChild(new ItemIconButton(
                new ItemStack(Items.TNT),
                Text.translatable("tutorial.group.special"),
                startX + 2 * (buttonWidth + spacing), y, buttonWidth, 32,
                btn -> this.client.setScreen(new TutorialGroupScreen("special",0,1))
        ));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x99000000);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 16, 0xFFFFFF);
        super.render(ctx, mouseX, mouseY, delta);
    }
}

