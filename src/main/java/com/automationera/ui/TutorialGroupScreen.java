package com.automationera.ui;

import dev.emi.emi.api.widget.ButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.logging.Logger;

public class TutorialGroupScreen extends Screen {
    private static final ButtonTextures TEXTURE = new ButtonTextures(Identifier.ofVanilla("widget/button"), Identifier.ofVanilla("widget/button_disabled"), Identifier.ofVanilla("widget/button_highlighted"));
    private final String group; // "factory", "farm", "special"
    private int selectedMac = 0;
    private int currentStep = 1;
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("AutomationEraTutorial");

    private static final List<MachineInfo> factoryMachines = List.of(
            new MachineInfo("iron", Items.IRON_BLOCK, Text.translatable("tutorial.iron.title")),
            new MachineInfo("stone", Items.COBBLESTONE, Text.translatable("tutorial.stone.title")),
            new MachineInfo("wood", Items.OAK_LOG, Text.translatable("tutorial.wood.title"))
    );
    private static final List<MachineInfo> farmMachines = List.of();
    private static final List<MachineInfo> specialMachines = List.of();

    // ...你可以为farm/special也写类似List...

    public TutorialGroupScreen(String group) {
        super(Text.translatable("tutorial.group." + group));
        this.group = group;
    }

    @Override
    protected void init() {
        int y = 50;
        int x = 30;
        int btnHeight = 24;
        int i = 0;
        List<MachineInfo> choose;
        switch (group){
            case "factory" -> choose = factoryMachines;
            case "special" -> choose = specialMachines;
            default -> choose = farmMachines;
        }
        for (MachineInfo info : choose) {
            int index = i++;
            this.addDrawableChild(new ItemIconButton(
                    new ItemStack(info.icon),
                    info.name,
                    x, y + index * (btnHeight + 8), 100, btnHeight,
                    btn -> {
                        selectedMac = index;
                        // 切换后刷新界面
                        this.init(this.client, this.width, this.height);
                    }
            ));
        }
        // 按钮只在init里添加
        this.addDrawableChild(new TexturedButtonWidget(
                20, this.height - 30, 60, 20,
                TEXTURE,
                btn -> this.client.setScreen(new TutorialMainScreen()),
                Text.translatable("tutorial.ui.back")
        ));
        this.addDrawableChild(new TexturedButtonWidget(
                180, this.height - 30, 60, 20,
                TEXTURE,
                btn -> {
                    if (currentStep > 1) currentStep--;
                    LOGGER.info("CS--");
                },
                Text.translatable("tutorial.ui.prev")
        ));
        this.addDrawableChild(new TexturedButtonWidget(
                250, this.height - 30, 60, 20,
                TEXTURE,
                btn -> {
                    // 只有有下一个step才+1
                    List<MachineInfo> choose2 = switch (group) {
                        case "factory" -> factoryMachines;
                        case "special" -> specialMachines;
                        default -> farmMachines;
                    };
                    MachineInfo machine = choose2.get(selectedMac);
                    if (TutorialManager.loadNbtFromResource(machine.id, currentStep + 1) != null) {
                        currentStep++;
                    }
                    LOGGER.info("CS++");
                },
                Text.translatable("tutorial.ui.next")
        ));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        
        ctx.fill(0, 0, this.width, this.height, 0xCC000000);
        ctx.drawTextWithShadow(this.textRenderer, this.title, 30, 20, 0xFFFFFF);

        // 右侧显示选中的机器信息
        List<MachineInfo> choose = switch (group) {
            case "factory" -> factoryMachines;
            case "special" -> specialMachines;
            default -> farmMachines;
        };
        MachineInfo machine = choose.isEmpty() ? null : choose.get(Math.min(selectedMac, choose.size()-1));
        if (machine != null) {
            ctx.drawTextWithShadow(this.textRenderer, machine.name, 180, 50, 0xFFFFFF);
            // 渲染结构预览和简介
            //TutorialManager.renderNbtStructure(ctx, machine.id, currentStep, 180, 80);
            LOGGER.info(String.format("id:%s, Step:%s",machine.id, currentStep));
            ctx.drawTextWithShadow(this.textRenderer, Text.translatable("tutorial." + machine.id + ".step"+ currentStep), 180, 180, 0xFFFFFF);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private static class MachineInfo {
        public final String id;
        public final Item icon;
        public final Text name;

        public MachineInfo(String id, Item icon, Text name) {
            this.id = id;
            this.icon = icon;
            this.name = name;
        }
    }
}


