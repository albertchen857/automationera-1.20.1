package com.automationera.ui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;

public class TutorialScreen extends Screen {
    private int currentStep = 1;

    public TutorialScreen() {
        super(Text.translatable("tutorial.ui.main"));
    }

    @Override
    protected void init() {
        // 可添加返回按钮等
        this.addDrawableChild(
                net.minecraft.client.gui.widget.ButtonWidget.builder(Text.translatable("tutorial.ui.next"), btn -> {
                    currentStep++;
                }).position(this.width - 100, this.height - 40).size(80, 20).build()
        );
        this.addDrawableChild(
                net.minecraft.client.gui.widget.ButtonWidget.builder(Text.translatable("tutorial.ui.close"), btn -> {
                    this.close();
                }).position(20, this.height - 40).size(60, 20).build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 半透明背景
        context.fill(0, 0, this.width, this.height, 0x99000000);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);

        // 渲染 nbt 结构预览
        TutorialManager.renderNbtStructure(context, "iron" , currentStep, 60, 60);

        // 渲染说明文本
        context.drawTextWithShadow(this.textRenderer, Text.literal("Step" + currentStep), 60, 220, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }
}

