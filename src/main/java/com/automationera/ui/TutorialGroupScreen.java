package com.automationera.ui;

import com.automationera.keybinding.ModKeyBinding;
import com.automationera.OutputRecipe;
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
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.automationera.OutputRecipe.*;

public class TutorialGroupScreen extends Screen {
    private final String group; // "factory", "farm", "special"
    private final int rotateSpeed = 2;
    private int selectedMac = 0;
    private int currentStep = 1;
    private boolean autoRotate = true;
    private MachineInfo machine;
    private MachineEntryList list;
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("AutomationEraTutorial");
    public static class SelectionBox {
        final int x1;
        final int y1;
        final int z1;
        final int x2;
        final int y2;
        final int z2;
        public SelectionBox(int x1, int y1, int z1, int x2, int y2, int z2) {
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
        }
    }
    private NbtCompound structureNbt;
    private IsometricRenderState renderState;



    private List<MachineInfo> choose = List.of();

    public TutorialGroupScreen(String group, int selectedMac, int currentStep, IsometricRenderState renderState) {
        super(Text.translatable("tutorial.group." + group));
        this.group = group;
        this.selectedMac = selectedMac;
        this.currentStep = currentStep;
        this.renderState = renderState;
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
            this.client.setScreen(new TutorialGroupScreen(group, selectedMac, currentStep, renderState));
        }else{
            LOGGER.warn("CurrentStep Up NULL");
        }
    }
    public void lastStep(){
        if (currentStep > 1) {
            currentStep--;
            this.client.setScreen(new TutorialGroupScreen(group, selectedMac, currentStep, renderState));
        }
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
                this.client.setScreen(new TutorialGroupScreen(group, idx, 1, renderState));
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

        machine = choose.isEmpty() ? null : choose.get(Math.min(selectedMac, choose.size()-1));
        //group page title
        TextWidget titleWidget = new TextWidget(2, 20, 200, 20, this.title.copy().append(Text.literal("-")).append(machine.name), this.textRenderer);
        this.addDrawableChild(titleWidget);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x77000000);
        super.render(ctx, mouseX, mouseY, delta);
        //machine = choose.isEmpty() ? null : choose.get(Math.min(selectedMac, choose.size() - 1));
        if (machine != null) {
            //Structure render
            structureNbt = TutorialManager.loadNbtFromResource(machine.id, currentStep);
            TutorialManager.renderStructure3D(this, structureNbt, currentStep, renderState, width, height, width, machine.selectbox);
        }
        if (autoRotate) renderState.addRotation(rotateSpeed);
        renderUI();
    }

    public void renderUI(){
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
                                Text.literal((0f-renderState.pitch)+"P"),
                                btn -> {
                                    if (renderState.pitch>-60) renderState.addPitch(15);
                                    else renderState.addPitch(-120);
                                    this.client.setScreen(new TutorialGroupScreen(group, selectedMac, currentStep, renderState));
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

    public static class MachineInfo {
        public final String id;
        public final Item icon;
        public final Text name;
        private final List<List<SelectionBox>> selectbox;

        public MachineInfo(String id, Item icon, Text name, List<List<SelectionBox>> selectbox) {
            this.id = id;
            this.icon = icon;
            this.name = name;
            this.selectbox = selectbox;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyBinding.openTutorialKey.matchesKey(keyCode, scanCode)) {
            this.client.setScreen(new TutorialMainScreen());
            return true;
        }
        if (ModKeyBinding.leftKey.matchesKey(keyCode, scanCode)) {
            this.rotateKey(-10f);
            return true;
        }
        if (ModKeyBinding.rightKey.matchesKey(keyCode, scanCode)) {
            this.rotateKey(10f);
            return true;
        }
        if (ModKeyBinding.upKey.matchesKey(keyCode, scanCode)) {
            this.ycKey(2.5f);
            return true;
        }
        if (ModKeyBinding.downKey.matchesKey(keyCode, scanCode)) {
            this.ycKey(-2.5f);
            return true;
        }
        if (ModKeyBinding.inKey.matchesKey(keyCode, scanCode)) {
            this.zoomKey(0.2f);
            return true;
        }
        if (ModKeyBinding.outKey.matchesKey(keyCode, scanCode)) {
            this.zoomKey(-0.2f);
            return true;
        }
        if (ModKeyBinding.nextKey.matchesKey(keyCode, scanCode)) {
            this.nextStep();
            return true;
        }
        if (ModKeyBinding.prevKey.matchesKey(keyCode, scanCode)) {
            this.lastStep();
            return true;
        }
        if (ModKeyBinding.nextTKey.matchesKey(keyCode, scanCode)) {
            this.nextTutorialKey();
            return true;
        }
        if (ModKeyBinding.prevTKey.matchesKey(keyCode, scanCode)) {
            this.prevTutorialKey();
            return true;
        }
        if (ModKeyBinding.resetKey.matchesKey(keyCode, scanCode)) {
            this.resetKey();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}


