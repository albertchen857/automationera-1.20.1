package com.automationera.ui;

import com.automationera.basic.ExportFile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.NarratedMultilineTextWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TutorialGroupScreen extends Screen {
    private final String group;
    private final int rotateSpeed = 2;
    private int selectedMac = 0;
    private int currentStep = 1;
    private boolean autoRotate = false;
    private MachineInfo machine;
    private MachineEntryList list;
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("AutomationEraTutorial");

    private NbtCompound structureNbt;
    private IsometricRenderState renderState;
    private List<MachineInfo> choose = List.of();

    private List<AnalysisList.Entry> blockListExternal;
    private List<AnalysisList.Entry> blockList;
    private NbtCompound blockListNbtCache = null;
    private int blockScroll = 0;
    private static final int BLOCK_LINES = 8;

    public TutorialGroupScreen(String group, int selectedMac, int currentStep, IsometricRenderState renderState, List<AnalysisList.Entry> blockListExternal) {
        super(Text.translatable("tutorial.group." + group));
        this.group = group;
        this.selectedMac = selectedMac;
        this.currentStep = currentStep;
        this.renderState = renderState;
        this.blockListExternal = blockListExternal;
    }
    public TutorialGroupScreen(String group, int selectedMac, int currentStep, IsometricRenderState renderState) {
        this(group, selectedMac, currentStep, renderState, null);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            renderState.addRotation((float) deltaX);
            renderState.addYc((float) deltaY);
            autoRotate = false;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int listW = 110, iconSize = 12, x = this.width - listW - 10, y0 = 10, height = BLOCK_LINES * (iconSize + 2);
        if (mouseX >= x - 5 && mouseX < x + listW + 8 && mouseY >= y0 - 4 && mouseY < y0 + height + 4) {
            int total = blockList == null ? 0 : blockList.size();
            int maxScroll = Math.max(0, total - BLOCK_LINES);
            blockScroll -= vertical > 0 ? 1 : -1;
            if (blockScroll < 0) blockScroll = 0;
            if (blockScroll > maxScroll) blockScroll = maxScroll;
            return true;
        } else if (mouseX >= (double) this.width *0.4 && mouseY <= (double) this.height *0.75){
            renderState.addScale(1.0f + (float) (vertical + horizontal) * 0.08f);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listW = 110, iconSize = 12, x = this.width - listW - 10, y0 = 10, listH = BLOCK_LINES * (iconSize + 2);
        int barX = x + listW - 6, barY = y0, barW = 4, barH = listH;
        int total = blockList == null ? 0 : blockList.size();
        int maxScroll = Math.max(0, total - BLOCK_LINES);
        if (maxScroll > 0 && mouseX >= barX && mouseX < barX + barW && mouseY >= barY && mouseY < barY + barH) {
            int sliderH = Math.max(12, barH * BLOCK_LINES / total);
            int relY = (int) mouseY - barY - sliderH / 2;
            blockScroll = Math.max(0, Math.min(maxScroll, relY * maxScroll / (barH - sliderH)));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void init() {
        choose = switch (group) {
            case "factory" -> com.automationera.OutputRecipe.factoryMachines;
            case "special" -> com.automationera.OutputRecipe.specialMachines;
            default -> com.automationera.OutputRecipe.farmMachines;
        };
        int listWidth = this.width / 5;
        list = new MachineEntryList(client, listWidth, this.height - 120, 25, 20);
        for (int m = 0; m < choose.size(); m++) {
            int idx = m;
            MachineInfo info = choose.get(m);
            list.addEntry(idx, new ItemStack(info.icon), info.name, () -> {
                this.client.setScreen(new TutorialGroupScreen(group, idx, 1, renderState, blockListExternal));
            });
        }
        list.selectByIndex(selectedMac);
        this.addDrawableChild(list);
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tutorial.ui.close"),
                        btn -> this.client.setScreen(new TutorialMainScreen()))
                .dimensions(20, this.height - 30, 60, 20).build());
        machine = choose.isEmpty() ? new MachineInfo("NULL",Items.AIR,Text.literal("null"),List.of(),true) : choose.get(Math.min(selectedMac, choose.size() - 1));
        TextWidget titleWidget = new TextWidget(0, 5, 200, 20, this.title.copy().append(Text.literal("-")).append(machine.name), this.textRenderer);
        this.addDrawableChild(titleWidget);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x77000000);
        super.render(ctx, mouseX, mouseY, delta);
        if (machine != null) {
            structureNbt = TutorialManager.loadNbtFromResource(machine.id, currentStep);
            TutorialManager.renderStructure3D(this, structureNbt, currentStep, renderState, width, height, width, machine.selectbox);
            if (blockListExternal != null) {
                blockList = blockListExternal;
            } else if (blockList == null || blockListNbtCache == null || !structureNbt.equals(blockListNbtCache)) {
                blockList = new AnalysisList(structureNbt).getResult();
                blockListNbtCache = structureNbt == null ? null : structureNbt.copy();
            }
            renderBlockList(ctx);
        }
        if (autoRotate) renderState.addRotation(rotateSpeed);
        renderUI();
    }

    private void renderBlockList(DrawContext ctx) {
        if (blockList == null || blockList.isEmpty()) return;

        int listW = 110, iconSize = 12;
        int x = this.width - listW - 10, y0 = 10, height = BLOCK_LINES * (iconSize + 2);
        ctx.fill(x - 5, y0 - 4, x + listW + 8, y0 + height + 4, 0x40FFFFFF);

        int total = blockList.size();
        int maxScroll = Math.max(0, total - BLOCK_LINES);
        float fontSize = 0.75f;

        MinecraftClient mc = MinecraftClient.getInstance();
        ItemRenderer ir = mc.getItemRenderer();
        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();

        net.minecraft.client.render.DiffuseLighting.enableGuiDepthLighting();
        RenderSystem.enableDepthTest();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-2f, -2f);

        final int FULL_BRIGHT = LightmapTextureManager.pack(15, 15);
        MatrixStack matrices = ctx.getMatrices();

        for (int i = 0; i < BLOCK_LINES && (i + blockScroll) < total; i++) {
            var entry = blockList.get(i + blockScroll);
            int y = y0 + i * (iconSize + 2);

            matrices.push();
            matrices.translate(x + 8.0f, y + 8.0f, 0f);
            matrices.scale(iconSize, iconSize, 16);
            matrices.scale(1f, -1f, 1f);

            ir.renderItem(
                    entry.stack,
                    net.minecraft.client.render.model.json.ModelTransformationMode.GUI,
                    FULL_BRIGHT,
                    OverlayTexture.DEFAULT_UV,
                    matrices, vcp, mc.world, 0
            );
            matrices.pop();

            matrices.push();
            matrices.translate(x + iconSize + 6, y + 2, 0);
            matrices.scale(fontSize, fontSize, 1.0f);
            ctx.drawText(this.textRenderer, entry.displayName, 0, 0, 0xEBEBEB, false);
            matrices.pop();

            matrices.push();
            matrices.translate(x + listW - 52, y + 2, 0);
            matrices.scale(fontSize, fontSize, 1.0f);
            ctx.drawText(this.textRenderer, entry.countBoxGroup(), 0, 0, 0x6BC1FF, false);
            matrices.pop();
        }

        vcp.draw();

        RenderSystem.disablePolygonOffset();
        net.minecraft.client.render.DiffuseLighting.disableGuiDepthLighting();

        if (maxScroll > 0) {
            int barX = x + listW - 6, barY = y0, barW = 4, barH = height;
            ctx.fill(barX, barY, barX + barW, barY + barH, 0x22000000);
            int sliderH = Math.max(12, barH * BLOCK_LINES / total);
            int sliderY = barY + (barH - sliderH) * blockScroll / maxScroll;
            ctx.fill(barX + 1, sliderY, barX + barW - 1, sliderY + sliderH, 0xFFAAAAAA);
        }
    }


    public void renderUI() {
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tutorial.ui.prev"),
                btn -> lastStep()).dimensions(120, this.height - 30, 40, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tutorial.ui.next"),
                btn -> nextStep()).dimensions(160, this.height - 30, 40, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tutorial.ui.output"),
                btn -> output()).dimensions(200, this.height - 30, 60, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal((0f - renderState.pitch) + "P"),
                btn -> {
                    if (renderState.pitch > -60) renderState.addPitch(15);
                    else renderState.addPitch(-120);
                    this.client.setScreen(new TutorialGroupScreen(group, selectedMac, currentStep, renderState, blockListExternal));
                }).dimensions(260, this.height - 30, 60, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("\u25B6"),
                btn -> autoRotate = !autoRotate).dimensions(320, this.height - 30, 20, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("\u21BA"),
                btn -> resetKey()).dimensions(340, this.height - 30, 20, 20).build());
        NarratedMultilineTextWidget descWidget = new NarratedMultilineTextWidget(180, Text.translatable("tutorial." + machine.id + ".step" + currentStep), this.textRenderer);
        descWidget.setPosition(180, 170);
        this.addDrawableChild(descWidget);

    }

    public void rotateKey(float delta) {
        renderState.addRotation(delta);
    }

    public void ycKey(float delta) {
        renderState.addYc(delta);
    }

    public void zoomKey(float delta) {
        renderState.addScale(delta);
    }

    public void nextStep() {
        List<MachineInfo> choose2 = switch (group) {
            case "factory" -> com.automationera.OutputRecipe.factoryMachines;
            case "special" -> com.automationera.OutputRecipe.specialMachines;
            default -> com.automationera.OutputRecipe.farmMachines;
        };
        MachineInfo machine = choose2.get(selectedMac);
        if (TutorialManager.loadNbtFromResource(machine.id, currentStep + 1) != null) {
            currentStep++;
            this.client.setScreen(new TutorialGroupScreen(group, selectedMac, currentStep, renderState, blockListExternal));
        } else {
            LOGGER.warn("CurrentStep Up NULL");
        }
    }

    public void lastStep() {
        if (currentStep > 1) {
            currentStep--;
            this.client.setScreen(new TutorialGroupScreen(group, selectedMac, currentStep, renderState, blockListExternal));
        }
    }

    public void nextTutorialKey() {
        if (selectedMac < com.automationera.OutputRecipe.factoryMachines.size()) selectedMac++;
    }

    public void prevTutorialKey() {
        if (selectedMac > 0) selectedMac--;
    }

    public void resetKey() {
        renderState = new IsometricRenderState();
    }

    public void output(){
        List<MachineInfo> choose2 = switch (group) {
            case "factory" -> com.automationera.OutputRecipe.factoryMachines;
            case "special" -> com.automationera.OutputRecipe.specialMachines;
            default -> com.automationera.OutputRecipe.farmMachines;
        };
        int max = choose2.get(selectedMac).selectbox.size();
        LOGGER.info("{}.{}",machine.id,max);
        ExportFile.exportTutorialFolder(machine.id,max);
    }

    public static class MachineInfo {
        public final String id;
        public final Item icon;
        public final Text name;
        private final List<List<SelectionBox>> selectbox;
        private final boolean consist;

        public MachineInfo(String id, Item icon, Text name, List<List<SelectionBox>> selectbox, boolean consist) {
            this.id = id;
            this.icon = icon;
            this.name = name;
            this.selectbox = selectbox;
            this.consist = consist;
        }
    }

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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (com.automationera.keybinding.ModKeyBinding.openTutorialKey.matchesKey(keyCode, scanCode)) {
            this.client.setScreen(new TutorialMainScreen());
            return true;
        }
        if (com.automationera.keybinding.ModKeyBinding.leftKey.matchesKey(keyCode, scanCode)) {
            this.rotateKey(-10f);
            return true;
        }
        if (com.automationera.keybinding.ModKeyBinding.rightKey.matchesKey(keyCode, scanCode)) {
            this.rotateKey(10f);
            return true;
        }
        if (com.automationera.keybinding.ModKeyBinding.downKey.matchesKey(keyCode, scanCode)) {
            this.ycKey(2.5f);
            return true;
        }
        if (com.automationera.keybinding.ModKeyBinding.upKey.matchesKey(keyCode, scanCode)) {
            this.ycKey(-2.5f);
            return true;
        }
        if (com.automationera.keybinding.ModKeyBinding.inKey.matchesKey(keyCode, scanCode)) {
            this.zoomKey(0.2f);
            return true;
        }
        if (com.automationera.keybinding.ModKeyBinding.outKey.matchesKey(keyCode, scanCode)) {
            this.zoomKey(-0.2f);
            return true;
        }
        if (com.automationera.keybinding.ModKeyBinding.nextKey.matchesKey(keyCode, scanCode)) {
            this.nextStep();
            return true;
        }
        if (com.automationera.keybinding.ModKeyBinding.prevKey.matchesKey(keyCode, scanCode)) {
            this.lastStep();
            return true;
        }
        if (com.automationera.keybinding.ModKeyBinding.nextTKey.matchesKey(keyCode, scanCode)) {
            this.nextTutorialKey();
            return true;
        }
        if (com.automationera.keybinding.ModKeyBinding.prevTKey.matchesKey(keyCode, scanCode)) {
            this.prevTutorialKey();
            return true;
        }
        if (com.automationera.keybinding.ModKeyBinding.resetKey.matchesKey(keyCode, scanCode)) {
            this.resetKey();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
