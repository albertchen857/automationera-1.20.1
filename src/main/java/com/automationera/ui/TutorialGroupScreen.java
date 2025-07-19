package com.automationera.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.NarratedMultilineTextWidget;
import net.minecraft.client.gui.widget.TextWidget;
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
    private MachineEntryList list;
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("AutomationEraTutorial");

    private static final List<MachineInfo> factoryMachines = List.of(
            new MachineInfo("iron", Items.IRON_BLOCK, Text.translatable("tutorial.iron.title")),
            new MachineInfo("stone", Items.COBBLESTONE, Text.translatable("tutorial.stone.title")),
            new MachineInfo("wood", Items.OAK_LOG, Text.translatable("tutorial.wood.title"))
    );
    private static final List<MachineInfo> farmMachines = List.of();
    private static final List<MachineInfo> specialMachines = List.of();

    // ...你可以为farm/special也写类似List...

    public TutorialGroupScreen(String group, int selectedMac, int currentStep) {
        super(Text.translatable("tutorial.group." + group));
        this.group = group;
        this.selectedMac = selectedMac;
        this.currentStep = currentStep;
    }

    public void Refresh(String group, int selectedMac, int currentStep){
        client.setScreen(new TutorialGroupScreen(group, selectedMac, currentStep));
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
        int listWidth = this.width / 3;
        list = new MachineEntryList(client, listWidth, this.height - 40, 40, 20);

        for (int m = 0; m < choose.size(); m++) {
            int idx = m;
            MachineInfo info = choose.get(m);
            list.addEntry(idx, new ItemStack(info.icon), info.name, () -> {
                this.client.setScreen(new TutorialGroupScreen(group, idx, 1));
            });
        }
        list.selectByIndex(selectedMac);
        this.addDrawableChild(list);

        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.translatable("tutorial.ui.close"),
                                btn -> this.client.setScreen(new TutorialMainScreen())
                        ).dimensions(20, this.height - 30, 60, 20)
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.translatable("tutorial.ui.prev"),
                                btn -> {
                                    if (currentStep > 1) currentStep--;
                                    this.client.setScreen(new TutorialGroupScreen(group, selectedMac, currentStep));
                                }
                        ).dimensions(180, this.height - 30, 60, 20)
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.translatable("tutorial.ui.next"),
                                btn -> {
                                    List<MachineInfo> choose2 = switch (group) {
                                        case "factory" -> factoryMachines;
                                        case "special" -> specialMachines;
                                        default -> farmMachines;
                                    };
                                    MachineInfo machine = choose2.get(selectedMac);
                                    if (TutorialManager.loadNbtFromResource(machine.id, currentStep + 1) != null) {
                                        currentStep++;
                                        this.client.setScreen(new TutorialGroupScreen(group, selectedMac, currentStep));
                                        LOGGER.info("CS++");
                                    }else{
                                        LOGGER.info("CS+ NULL");
                                    }
                                }
                        ).dimensions(250, this.height - 30, 60, 20)
                        .build()
        );

    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        
        ctx.fill(0, 0, this.width, this.height, 0xCC000000);
        TextWidget titleWidget = new TextWidget(30, 20, 200, 20, this.title, this.textRenderer);
        this.addDrawableChild(titleWidget);
        List<MachineInfo> choose = switch (group) {
            case "factory" -> factoryMachines;
            case "special" -> specialMachines;
            default -> farmMachines;
        };
        MachineInfo machine = choose.isEmpty() ? null : choose.get(Math.min(selectedMac, choose.size()-1));
        if (machine != null) {
            TextWidget machineWidget = new TextWidget(180, 50, 200, 20, machine.name, this.textRenderer);
            this.addDrawableChild(machineWidget);
            TutorialManager.renderNbtStructure(ctx, machine.id, currentStep, 180, 80);
            Text descText = Text.translatable("tutorial." + machine.id + ".step" + currentStep);
            NarratedMultilineTextWidget descWidget = new NarratedMultilineTextWidget(180, descText, this.textRenderer);
            descWidget.setPosition(180, 180);
            this.addDrawableChild(descWidget);
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

    public class IsometricRenderState {
        public double angle = Math.PI / 4; // 45度视角
        public double scale = 1.0;
        public int offsetX = 0, offsetY = 0; // 拖动平移
        public boolean dragging = false;
        public int lastMouseX, lastMouseY;
    }

    IsometricRenderState renderState = new IsometricRenderState();

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (renderState.dragging && button == 0) {
            renderState.offsetX += deltaX;
            renderState.offsetY += deltaY;
            renderState.angle += deltaX*0.01;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            renderState.dragging = true;
            renderState.lastMouseX = (int) mouseX;
            renderState.lastMouseY = (int) mouseY;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            renderState.dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

}


