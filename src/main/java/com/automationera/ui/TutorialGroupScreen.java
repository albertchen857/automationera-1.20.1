package com.automationera.ui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.NarratedMultilineTextWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TutorialGroupScreen extends Screen {
    private final String group; // "factory", "farm", "special"
    private final int rotateSpeed = 2;
    private int selectedMac = 0;
    private int currentStep = 1;
    private boolean autoRotate = true;
    private MachineEntryList list;
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("AutomationEraTutorial");

    private NbtCompound structureNbt;
    private IsometricRenderState renderState = new IsometricRenderState();

    private static final List<MachineInfo> factoryMachines = List.of(
            new MachineInfo("iron", Items.IRON_BLOCK, Text.translatable("tutorial.iron.title")),
            new MachineInfo("stone", Items.COBBLESTONE, Text.translatable("tutorial.stone.title")),
            new MachineInfo("wood", Items.OAK_LOG, Text.translatable("tutorial.wood.title")),
            new MachineInfo("ice", Items.IRON_BLOCK, Text.translatable("tutorial.ice.title")),
            new MachineInfo("rail", Items.RAIL, Text.translatable("tutorial.rail.title"))
    );
    private static final List<MachineInfo> farmMachines = List.of(
            new MachineInfo("mob", Items.ROTTEN_FLESH, Text.translatable("tutorial.mob.title")),
            new MachineInfo("pig", Items.COOKED_PORKCHOP, Text.translatable("tutorial.pig.title")),
            new MachineInfo("farm", Items.CARROT, Text.translatable("tutorial.farm.title")),
            new MachineInfo("sugarcane", Items.SUGAR_CANE, Text.translatable("tutorial.sugarcane.title")),
            new MachineInfo("bamboo", Items.BAMBOO, Text.translatable("tutorial.bamboo.title"))
    );
    private static final List<MachineInfo> specialMachines = List.of(
            new MachineInfo("carpet", Items.WHITE_CARPET, Text.translatable("tutorial.carpet.title"))
    );
    private List<MachineInfo> choose = List.of();

    public TutorialGroupScreen(String group, int selectedMac, int currentStep) {
        super(Text.translatable("tutorial.group." + group));
        this.group = group;
        this.selectedMac = selectedMac;
        this.currentStep = currentStep;
    }

    public void rotateKey(float delta){renderState.addRotation(delta); }
    public void ycKey(float delta){renderState.addYc(delta); }
    public void zoomKey(float delta){renderState.addScale(delta); }
    public void nextStep(){
        List<MachineInfo> choose2 = switch (group) {
            case "factory" -> factoryMachines;
            case "special" -> specialMachines;
            default -> farmMachines;
        };
        MachineInfo machine = choose2.get(selectedMac);
        if (TutorialManager.loadNbtFromResource(machine.id, currentStep + 1) != null) {
            currentStep++;
            this.client.setScreen(new TutorialGroupScreen(group, selectedMac, currentStep));
        }else{
            LOGGER.warn("CurrentStep Up NULL");
        }
    }
    public void lastStep(){
        if (currentStep > 1) currentStep--;
        this.client.setScreen(new TutorialGroupScreen(group, selectedMac, currentStep));
    }
    public void nextTutorialKey(){if (selectedMac<factoryMachines.size()) selectedMac++; }
    public void prevTutorialKey(){if (selectedMac>0) selectedMac--; }
    public void resetKey(){renderState = new IsometricRenderState();}

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) { // 左键
            renderState.addRotation((float) deltaX);
            renderState.addYc((float) deltaY);
            autoRotate = false;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        renderState.addScale(1.0f + (float)(vertical + horizontal) * 0.08f);
        return true;
    }

    @Override
    protected void init() {
        choose = switch (group){//list
            case "factory" -> factoryMachines;
            case "special" -> specialMachines;
            default -> farmMachines;
        };
        int listWidth = this.width / 4;
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
        //close UI
        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.translatable("tutorial.ui.close"),
                                btn -> this.client.setScreen(new TutorialMainScreen())
                        ).dimensions(20, this.height - 30, 60, 20)
                        .build()
        );

        MachineInfo machine = choose.isEmpty() ? null : choose.get(Math.min(selectedMac, choose.size()-1));
        //group page title
        TextWidget titleWidget = new TextWidget(2, 20, 200, 20, this.title.copy().append(Text.literal("-")).append(machine.name), this.textRenderer);
        this.addDrawableChild(titleWidget);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x77000000);
        super.render(ctx, mouseX, mouseY, delta);
        MachineInfo machine = choose.isEmpty() ? null : choose.get(Math.min(selectedMac, choose.size()-1));
        if (machine != null) {
            //Structure render
            structureNbt = TutorialManager.loadNbtFromResource(machine.id, currentStep);
            TutorialManager.renderStructure3D(this, structureNbt, renderState, width, height, width);
        }
        if (autoRotate) renderState.addRotation(rotateSpeed);

        //Struct Setting UI
        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.translatable("tutorial.ui.prev"),
                                btn -> lastStep()
                        ).dimensions(120, this.height - 30, 40, 20)
                        .build()
        );
        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.translatable("tutorial.ui.next"),
                                btn -> nextStep()
                        ).dimensions(160, this.height - 30, 40, 20)
                        .build()
        );
        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.literal(renderState.pitch+"P"),
                                btn -> {
                                    renderState.addPitch(30);

                                    this.client.setScreen(new TutorialGroupScreen(group, selectedMac, currentStep));
                                }
                        ).dimensions(260, this.height - 30, 60, 20)
                        .build()
        );
        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("\u25B6"),
                                btn -> autoRotate = !autoRotate
                        ).dimensions(320, this.height - 30, 20, 20)
                        .build()
        );
        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("\u21BA"),
                                btn -> resetKey()
                        ).dimensions(340, this.height - 30, 20, 20)
                        .build()
        );
        //struct tutorials
        NarratedMultilineTextWidget descWidget = new NarratedMultilineTextWidget(180, Text.translatable("tutorial." + machine.id + ".step" + currentStep), this.textRenderer);
        descWidget.setPosition(180, 170);
        this.addDrawableChild(descWidget);
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


