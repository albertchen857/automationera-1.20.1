package com.automationera.ui;

import com.automationera.OutputRecipe;
import com.automationera.basic.ExportFile;

import com.automationera.mixin.BlockDisplayAccessor;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.NarratedMultilineTextWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRenderers;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.LocalRandom;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

public class TutorialGroupScreen extends Screen {
    private final String group;
    private final int rotateSpeed = 2;
    private int selectedMac = 0;
    private int currentStep = 1;
    private boolean autoRotate = false;
    private MachineInfo machine;
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("AutomationEraTutorial");

    private IsometricRenderState renderState;
    private NbtCompound blockListNbtCache;
    private List<MachineInfo> choose = List.of();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final List<AnalysisList.Entry> blockListExternal;
    private List<AnalysisList.Entry> blockList;
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
            LOGGER.info("Scrolled {}",renderState.rotation);
            return true;
        } else if (mouseX >= (double) this.width *0.4 && mouseY <= (double) this.height *0.75){
            renderState.addScale(1.0f + (float) (vertical + horizontal) * 0.08f);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listW = 110, iconSize = 12, x = this.width - listW - 10, y0 = 10, listH = BLOCK_LINES * (iconSize + 2);
        int barX = x + listW - 6, barW = 4;
        int total = blockList == null ? 0 : blockList.size();
        int maxScroll = Math.max(0, total - BLOCK_LINES);
        if (maxScroll > 0 && mouseX >= barX && mouseX < barX + barW && mouseY >= y0 && mouseY < y0 + listH) {
            int sliderH = Math.max(12, listH * BLOCK_LINES / total);
            int relY = (int) mouseY - y0 - sliderH / 2;
            blockScroll = Math.max(0, Math.min(maxScroll, relY * maxScroll / (listH - sliderH)));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void init() {
        choose = switch (group) {
            case "factory" -> OutputRecipe.factoryMachines;
            case "special" -> OutputRecipe.specialMachines;
            default -> OutputRecipe.farmMachines;
        };
        int listWidth = this.width / 5;
        MachineEntryList list = new MachineEntryList(client, listWidth, this.height - 120, 25, 20);
        for (int m = 0; m < choose.size(); m++) {
            int idx = m;
            MachineInfo info = choose.get(m);
            list.addEntry(idx, new ItemStack(info.icon), info.name, () -> {
                if (this.client != null) {
                    this.client.setScreen(new TutorialGroupScreen(group, idx, 1, renderState, blockListExternal));
                }
            });
        }
        list.selectByIndex(selectedMac);
        this.addDrawableChild(list);
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tutorial.ui.close"),
                        btn -> {
                            if (this.client != null) {
                                this.client.setScreen(new TutorialMainScreen());
                            }
                        })
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
            NbtCompound structureNbt = TutorialManager.loadNbtFromResource(machine.id, currentStep);
            TutorialManager.renderStructure3D(ctx, structureNbt, currentStep, renderState, width, height, width, machine.selectbox);
//            matrices.push();
//            matrices.translate(this.width / 2.0, this.height / 2.0, 0);
//            float scale = 80.0f;
//            matrices.scale(scale, scale, scale);
//            renderQuad();
            if (blockListExternal != null) {
                blockList = blockListExternal; //execute
            } else if (structureNbt != null && (blockList == null || !structureNbt.equals(blockListNbtCache))) {
                blockList = new AnalysisList(structureNbt).getResult(); //null
                blockListNbtCache = structureNbt == null ? null : structureNbt.copy();
            }
            renderBlockList(ctx, delta);
        }
        if (autoRotate) renderState.addRotation(rotateSpeed);
        renderUI();
    }



    /*@Override
    public void close() {
        allocator.close(); // 关屏幕时再关闭
        super.close();
    }

    private static final BufferAllocator allocator = new BufferAllocator(RenderLayer.DEFAULT_BUFFER_SIZE);
    private static final MatrixStack matrices = new MatrixStack();
    public static void renderQuad(){
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        VertexConsumer builder = immediate.getBuffer(RenderLayer.getSolid());
        MatrixStack.Entry matrixEntry = matrices.peek();
        LOGGER.info("Immediate={}, builder={}, matrixEntry={}", immediate, builder, matrixEntry);
        int l = LightmapTextureManager.pack(15, 15);
        int[] light = new int[] { l, l, l, l };
        float[] brightness = new float[] { 0.75f, 0.75f, 0.75f, 1.0f };
        BlockStateModel model = mc.getBakedModelManager().getBlockModels().getModel(Blocks.BRICKS.getDefaultState());
        var RAND = new LocalRandom(0);
        List<BlockModelPart> parts = model.getParts(RAND);
        for (BlockModelPart part : parts)
        {
            for (Direction face : Direction.values())
            {
                RAND.setSeed(0);
                for (BakedQuad quad : part.getQuads(face))
                {
                    builder.quad(matrixEntry, quad, brightness, 1.0f, 1.0f, 1.0f, 1.0f, light, OverlayTexture.DEFAULT_UV, true);
                }
            }

            RAND.setSeed(0);
            for (BakedQuad quad : part.getQuads(null))
            {
                builder.quad(matrixEntry, quad, brightness, 1.0f, 1.0f, 1.0f, 1.0f, light, OverlayTexture.DEFAULT_UV, true);
            }
        }

        immediate.draw();
    }

     */

    private void renderBlockList(DrawContext ctx, float delta) {
        if (blockList == null || blockList.isEmpty()) {
            LOGGER.warn("Blocklist null");
            return;
        }

        int listW = 100, iconSize = 12;
        int x = this.width - listW - 5, y0 = 10, height = BLOCK_LINES * (iconSize + 2);
        ctx.fill(x - 5, y0 - 4, x + listW + 8, y0 + height + 4, 0x40FFFFFF);
        int total = blockList.size();
        int maxScroll = Math.max(0, total - BLOCK_LINES);
        var fontSize = 0.75;
        for (int i = 0; i < BLOCK_LINES && (i + blockScroll) < total; i++) {
            var entry = blockList.get(i + blockScroll);
            if (entry.stack.equals(new ItemStack(Items.BARRIER))) continue;
            int y = y0 + i * (iconSize + 2);
            ctx.drawItem(entry.stack,x, y);
            ctx.drawText(this.textRenderer, entry.displayName, x + iconSize + 10, y + 4, 0xFFEBEBEB, true);
            ctx.drawText(this.textRenderer, entry.countBoxGroup(), x + listW - 35, y + 7, 0xFFFF512C, true);
        }

        /*PlayerEntityModel playerModel = new PlayerEntityModel(
                mc.getLoadedEntityModels().getModelPart(EntityModelLayers.PLAYER),
                false
        );

        // 2. 拿皮肤 Identifier
        Identifier skin = mc.player.getSkinTextures().texture(); // 或 getSkinTexture()

        // 4. 调用 addPlayerSkin
        ctx.addPlayerSkin(
                playerModel,
                skin,
                20.0f,  // scale
                45.0f,   // xRotation
                45f,   // yRotation
                0f,   // yPivot
                100, 100, 200, 200
        );
        ctx.addPlayerSkin(
                playerModel,
                skin,
                20.0f,  // scale
                0f,   // xRotation
                45.0f,   // yRotation
                45f,   // yPivot
                200, 100, 300, 200
        );
        ctx.addPlayerSkin(
                playerModel,
                skin,
                20.0f,  // scale
                45f,   // xRotation
                0f,   // yRotation
                45f,   // yPivot
                300, 100, 400, 200
        );*/



        /*DisplayEntity.BlockDisplayEntity entity = new DisplayEntity.BlockDisplayEntity(
                EntityType.BLOCK_DISPLAY,mc.world
        );
        entity.setBlockState(Blocks.BRICKS.getDefaultState());
        ((BlockDisplayAccessor) entity).invokeRefreshData(false, 0f);
        FallingBlockEntity fe = new FallingBlockEntity(mc.world,0,0,0,Blocks.BRICKS.getDefaultState());
        @SuppressWarnings("unchecked")
        EntityRenderer<DisplayEntity.BlockDisplayEntity, BlockDisplayEntityRenderState> renderer = (EntityRenderer<DisplayEntity.BlockDisplayEntity, BlockDisplayEntityRenderState>)
                mc.getEntityRenderDispatcher().getRenderer(entity);
        BlockDisplayEntityRenderState state = renderer.getAndUpdateRenderState(entity, 10f);
        LOGGER.info("world = {}", mc.world);
        LOGGER.info("entity = {}", entity);
        LOGGER.info("renderer = {}", renderer);
        LOGGER.info("state = {}", state);
        if (state instanceof BlockDisplayEntityRenderState blockState) {
            LOGGER.info("state.data = {}", blockState.data);
        }


        ctx.addEntity(
                state,
                100f,
                new Vector3f(0f, 0f, 0f),
                new Quaternionf()
                        .rotateXYZ(
                                (float) Math.toRadians(0),
                                (float) Math.toRadians(0),
                                (float) Math.toRadians(0)
                        ),
                new Quaternionf(),
                100, 100,300,300
        );*/

        if (maxScroll > 0) {
            int barX = x + listW, barY = y0, barW = 4, barH = height;
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
        this.addDrawableChild(ButtonWidget.builder(Text.literal("▶"),
                btn -> autoRotate = !autoRotate).dimensions(320, this.height - 30, 20, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("↺"),
                btn -> resetKey()).dimensions(340, this.height - 30, 20, 20).build());
        NarratedMultilineTextWidget descWidget = new NarratedMultilineTextWidget(180, Text.translatable("tutorial." + machine.id + ".step" + currentStep), this.textRenderer);
        descWidget.setPosition(180, 170);
        this.addDrawableChild(descWidget);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("EMI"),
                btn -> Openpage()).dimensions(360, this.height - 30, 40, 20).build());

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
        if (choose.isEmpty()) return;
        MachineInfo machine = choose.get(selectedMac);
        if (TutorialManager.loadNbtFromResource(machine.id, currentStep + 1) != null) {
            currentStep++;
            if (this.client != null) {
                this.client.setScreen(new TutorialGroupScreen(group, selectedMac, currentStep, renderState, blockListExternal));
            }
        } else {
            LOGGER.warn("CurrentStep Up NULL");
        }
    }

    public void lastStep() {
        if (currentStep > 1) {
            currentStep--;
            if (this.client != null) {
                this.client.setScreen(new TutorialGroupScreen(group, selectedMac, currentStep, renderState, blockListExternal));
            }
        }
    }

    public void nextTutorialKey() {
        if (selectedMac < OutputRecipe.factoryMachines.size()) selectedMac++;
    }

    public void prevTutorialKey() {
        if (selectedMac > 0) selectedMac--;
    }

    public void resetKey() {
        renderState = new IsometricRenderState();
    }

    public void output(){
        if (choose.isEmpty()) return;
        int max = choose.get(selectedMac).selectbox.size();
        LOGGER.info("{}.{}",machine.id,max);
        ExportFile.exportTutorialFolder(machine.id,max);
    }

    public void Openpage(){
        Map<String,List<?>> cm = OutputRecipe.ConvertMap();
        for (Map.Entry<String,List<?>> entry:cm.entrySet()){
            LOGGER.info("{}//{}|{}/{}",entry.getKey(),entry.getValue(),group,selectedMac);
            if (entry.getValue().getFirst().equals(group) && entry.getValue().getLast().equals(selectedMac)){
                continue;
            }
        }
    }

    public void setBlockList(List<AnalysisList.Entry> blockList) {
        this.blockList = blockList;
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
