package com.automationera.ui;

import com.automationera.AutomationeraClient;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.atlas.Atlases;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


public class TutorialManager {
    private static long nowtime;
    private static long lasttime=0;
    private static boolean rt = true;
    public static class SLogger {
        private final Logger logger;

        public SLogger(Logger logger) {
            this.logger = logger;
        }

        public void info(String msg) {
            if (logger != null && rt) {
                logger.info(msg);
            }
        }

        public void warn(String msg) {
            if (logger != null && rt) {
                logger.warn(msg);
            }
        }

        public void error(String msg, Exception e) {
            if (logger != null && rt) {
                logger.error(msg, e);
            }
        }
    }

    private static final SLogger LOGGER = new SLogger(LoggerFactory.getLogger("AutomationEraTutorial"));


    public static NbtCompound loadNbtFromResource(String group, int step) {
        ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
        Identifier id = Identifier.of("automationera","tutorial/"+group+"/"+group+"_step"+step+".nbt");
        Resource res = rm.getResource(id).orElse(null);
        if (res == null) return null;
        try (InputStream is = res.getInputStream()) {
            return NbtIo.readCompressed(is, NbtSizeTracker.of(Long.MAX_VALUE));
        } catch (Exception e) {
            LOGGER.error("NBT加载失败", e);
            return null;
        }
    }
    public static void renderStructure3D(DrawContext ctx, NbtCompound nbt, int step, IsometricRenderState state, int width, int height, int size, List<List<TutorialGroupScreen.SelectionBox>> SelectBox) {
        nowtime = Instant.now().toEpochMilli();
        if (nbt == null || !nbt.contains("palette") || !nbt.contains("blocks")) {
            LOGGER.warn("renderStructure3D: NBT invalid");
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockRenderManager brm = mc.getBlockRenderManager();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        var matrices = new MatrixStack();
        List<BlockState> palette = new ArrayList<>();
        Optional<NbtList> optn = nbt.getList("palette");
        optn.ifPresent(nbtElements -> {
            for (int i = 0; i < optn.get().size(); i++)
                palette.add(readBlockStateFromNbt(optn.get().getCompoundOrEmpty(i)));
        });

        NbtList blocks = nbt.getList("blocks").orElse(new NbtList());

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int i = 0; i < blocks.size(); i++) {
            NbtCompound blk = blocks.getCompoundOrEmpty(i);
            NbtList pos = blk.getList("pos").orElse(new NbtList());
            minX = Math.min(minX, pos.getInt(0).orElse(0));
            maxX = Math.max(maxX, pos.getInt(0).orElse(0));
            minY = Math.min(minY, pos.getInt(1).orElse(0));
            maxY = Math.max(maxY, pos.getInt(1).orElse(0));
            minZ = Math.min(minZ, pos.getInt(2).orElse(0));
            maxZ = Math.max(maxZ, pos.getInt(2).orElse(0));
        }
        double dx = (minX + maxX) / 2.0;
        double dy = (minY + maxY) / 2.0;
        double dz = (minZ + maxZ) / 2.0;
        double globalScale = state.scale * size / Math.max(1, Math.max(maxX-minX+1, Math.max(maxY-minY+1, maxZ-minZ+1))) / 1.2f;

        matrices.push();
        matrices.translate(width / 3f * 2f - 30, height / 8f * 5f + state.yc - 40, 200);//position

        double nx = (Math.sin(Math.toRadians(state.rotation)) * Math.cos(Math.toRadians(state.pitch))),
                ny = (-Math.sin(Math.toRadians(state.pitch))),
                nz = (Math.cos(Math.toRadians(state.rotation)) * Math.cos(Math.toRadians(state.pitch)));
        matrices.multiply(new Quaternionf().rotateX((float) -Math.asin(ny)));
        matrices.multiply(new Quaternionf().rotateY((float) Math.atan2(nx, nz)));//Normal Rotation

        matrices.scale((float)globalScale, (float)-globalScale, (float)globalScale);
        matrices.translate(-dx, -dy, -dz);//center point

        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);
        GlStateManager._enableCull();
        GlStateManager._enableBlend();
        LOGGER.info(brm + "\n" + immediate + "\n" + mc + "\n" + nbt);
        for (int i = 0; i < blocks.size(); i++) {
            NbtCompound blk = blocks.getCompoundOrEmpty(i);
            NbtList pos = blk.getList("pos").orElse(new NbtList());
            int x = pos.getInt(0).orElse(0), y = pos.getInt(1).orElse(0), z = pos.getInt(2).orElse(0);
            Optional<Integer> idx = blk.getInt("state");
            if (idx.isPresent()) {
                if (idx.get() < 0 || idx.get() >= palette.size()) continue;
                BlockState bs = palette.get(idx.get());

                matrices.push();
                matrices.translate(x, y, z);
                Block block = bs.getBlock();

                if (!bs.isAir() || bs.getRenderType() == BlockRenderType.MODEL) {
                    //LOGGER.info("{},{},{},{}|{}",block,x,y,z,bs);
                    brm.renderBlockAsEntity(bs, matrices, immediate, 15728880, OverlayTexture.DEFAULT_UV);
                }
                if (!bs.getFluidState().isEmpty()) {
                    GlStateManager._depthMask(false);
                    boolean isWaterlogged = false;
                    int level = 0;
                    Identifier tex = block == Blocks.LAVA
                            ? Identifier.of("minecraft", "block/lava_still")
                            : Identifier.of("minecraft", "block/water_still");
                    for (Property<?> prop : bs.getProperties()) {
                        if (prop.getName().equals("waterlogged") && bs.get(prop).toString().equals("true")) {
                            isWaterlogged = true;
                            break;
                        }
                    }
                    for (Property<?> prop : bs.getProperties()) {
                        if (prop.getName().toLowerCase().contains("level")) {
                            level = Integer.parseInt(bs.get(prop).toString());
                            break;
                        }
                    }
                    boolean isWater = (block == Blocks.WATER || isWaterlogged);
                    float fluidHeight = (level == 0) ? 1.0f : (8 - level) / 8.0f;
                    int r = isWater ? 63 : 255;
                    int g = isWater ? 118 : 255;
                    int b = isWater ? 228 : 255;
                    if (isWater || block == Blocks.LAVA) {
                        renderFluidCube(matrices, immediate, tex, 0, fluidHeight-0.001f, 0.9f, r, g, b);
                    }
                }
                matrices.pop();
            }
        }
        immediate.draw();

        matrices.push();
        if (SelectBox != null && step-1 < SelectBox.size()) {
            long time = System.currentTimeMillis();
            float a = 0.5f + 0.3f * (float)Math.abs(Math.sin(time / 300.0));
            for (TutorialGroupScreen.SelectionBox box : SelectBox.get(step-1)) {
                renderSelectionBoxOutline(matrices, immediate, box, 2.0f, 1f, 1f, 0.0f, a); // 粗亮黄
            }
        }
        immediate.draw();
        GlStateManager._depthMask(true);
        GlStateManager._disableDepthTest();
        GlStateManager._disableCull();
        GlStateManager._disableBlend();
        matrices.pop();
    }


    public static void renderFluidCube(MatrixStack matrices, VertexConsumerProvider consumers,
                                       Identifier texture, float y1, float y2, float alpha, int r,int g, int b) {
        Sprite sprite = MinecraftClient.getInstance()
                .getBakedModelManager()
                .getAtlas(Identifier.of("minecraft","textures/atlas/blocks.png"))
                .getSprite(texture);

        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();

        GlStateManager._enableBlend();
        GlStateManager._disableCull();

        Matrix4f mat = matrices.peek().getPositionMatrix();
        VertexConsumer builder = consumers.getBuffer(RenderLayer.getGlintTranslucent());

        int a = Math.round(255 * alpha);
        int light = 0xF000F0;
        int overlay = OverlayTexture.DEFAULT_UV;

        // 上面
        builder.vertex(mat, 0, y2, 1).texture(u0, v1).color(r, g, b, a).light(light).normal(0, 1, 0).overlay(overlay);
        builder.vertex(mat, 1, y2, 1).texture(u1, v1).color(r, g, b, a).light(light).normal(0, 1, 0).overlay(overlay);
        builder.vertex(mat, 1, y2, 0).texture(u1, v0).color(r, g, b, a).light(light).normal(0, 1, 0).overlay(overlay);
        builder.vertex(mat, 0, y2, 0).texture(u0, v0).color(r, g, b, a).light(light).normal(0, 1, 0).overlay(overlay);


        // 下面
        builder.vertex(mat, 0, y1, 0).texture(u0, v0).color(r, g, b, a).light(light).normal(0, -1, 0).overlay(overlay);
        builder.vertex(mat, 1, y1, 0).texture(u1, v0).color(r, g, b, a).light(light).normal(0, -1, 0).overlay(overlay);
        builder.vertex(mat, 1, y1, 1).texture(u1, v1).color(r, g, b, a).light(light).normal(0, -1, 0).overlay(overlay);
        builder.vertex(mat, 0, y1, 1).texture(u0, v1).color(r, g, b, a).light(light).normal(0, -1, 0).overlay(overlay);

        // 前面
        builder.vertex(mat, 0, y2, 0).texture(u0, v0).color(r, g, b, a).light(light).normal(0, 0, -1).overlay(overlay);
        builder.vertex(mat, 1, y2, 0).texture(u1, v0).color(r, g, b, a).light(light).normal(0, 0, -1).overlay(overlay);
        builder.vertex(mat, 1, y1, 0).texture(u1, v1).color(r, g, b, a).light(light).normal(0, 0, -1).overlay(overlay);
        builder.vertex(mat, 0, y1, 0).texture(u0, v1).color(r, g, b, a).light(light).normal(0, 0, -1).overlay(overlay);

        // 后面
        builder.vertex(mat, 0, y1, 1).texture(u0, v0).color(r, g, b, a).light(light).normal(0, 0, 1).overlay(overlay);
        builder.vertex(mat, 1, y1, 1).texture(u1, v0).color(r, g, b, a).light(light).normal(0, 0, 1).overlay(overlay);
        builder.vertex(mat, 1, y2, 1).texture(u1, v1).color(r, g, b, a).light(light).normal(0, 0, 1).overlay(overlay);
        builder.vertex(mat, 0, y2, 1).texture(u0, v1).color(r, g, b, a).light(light).normal(0, 0, 1).overlay(overlay);

        // 左面
        builder.vertex(mat, 0, y1, 0).texture(u0, v0).color(r, g, b, a).light(light).normal(-1, 0, 0).overlay(overlay);
        builder.vertex(mat, 0, y1, 1).texture(u1, v0).color(r, g, b, a).light(light).normal(-1, 0, 0).overlay(overlay);
        builder.vertex(mat, 0, y2, 1).texture(u1, v1).color(r, g, b, a).light(light).normal(-1, 0, 0).overlay(overlay);
        builder.vertex(mat, 0, y2, 0).texture(u0, v1).color(r, g, b, a).light(light).normal(-1, 0, 0).overlay(overlay);

        // 右面
        builder.vertex(mat, 1, y2, 0).texture(u0, v0).color(r, g, b, a).light(light).normal(1, 0, 0).overlay(overlay);
        builder.vertex(mat, 1, y2, 1).texture(u1, v0).color(r, g, b, a).light(light).normal(1, 0, 0).overlay(overlay);
        builder.vertex(mat, 1, y1, 1).texture(u1, v1).color(r, g, b, a).light(light).normal(1, 0, 0).overlay(overlay);
        builder.vertex(mat, 1, y1, 0).texture(u0, v1).color(r, g, b, a).light(light).normal(1, 0, 0).overlay(overlay);
    }

    public static void renderSelectionBoxOutline(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            TutorialGroupScreen.SelectionBox box,
            float lineWidth,
            float r, float g, float b, float a
    ) {
        float minX = Math.min(box.x1, box.x2), minY = Math.min(box.y1, box.y2), minZ = Math.min(box.z1, box.z2);
        float maxX = Math.max(box.x1, box.x2) + 1, maxY = Math.max(box.y1, box.y2) + 1, maxZ = Math.max(box.z1, box.z2) + 1;

        RenderSystem.lineWidth(lineWidth);

        Matrix4f mat = matrices.peek().getPositionMatrix();
        VertexConsumer builder = consumers.getBuffer(RenderLayer.getLines());
        //底
        builder.vertex(mat, minX, minY, minZ).color(r, g, b, a).normal(1, 0, 0);
        builder.vertex(mat, maxX, minY, minZ).color(r, g, b, a).normal(1, 0, 0);

        builder.vertex(mat, maxX, minY, minZ).color(r, g, b, a).normal(1, 0, 0);
        builder.vertex(mat, maxX, minY, maxZ).color(r, g, b, a).normal(1, 0, 0);

        builder.vertex(mat, maxX, minY, maxZ).color(r, g, b, a).normal(1, 0, 0);
        builder.vertex(mat, minX, minY, maxZ).color(r, g, b, a).normal(1, 0, 0);

        builder.vertex(mat, minX, minY, maxZ).color(r, g, b, a).normal(1, 0, 0);
        builder.vertex(mat, minX, minY, minZ).color(r, g, b, a).normal(1, 0, 0);

        // 顶面
        builder.vertex(mat, minX, maxY, minZ).color(r, g, b, a).normal(1, 0, 0);
        builder.vertex(mat, maxX, maxY, minZ).color(r, g, b, a).normal(1, 0, 0);

        builder.vertex(mat, maxX, maxY, minZ).color(r, g, b, a).normal(1, 0, 0);
        builder.vertex(mat, maxX, maxY, maxZ).color(r, g, b, a).normal(1, 0, 0);

        builder.vertex(mat, maxX, maxY, maxZ).color(r, g, b, a).normal(1, 0, 0);
        builder.vertex(mat, minX, maxY, maxZ).color(r, g, b, a).normal(1, 0, 0);

        builder.vertex(mat, minX, maxY, maxZ).color(r, g, b, a).normal(1, 0, 0);
        builder.vertex(mat, minX, maxY, minZ).color(r, g, b, a).normal(1, 0, 0);

        // 竖边
        builder.vertex(mat, minX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
        builder.vertex(mat, minX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);

        builder.vertex(mat, maxX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
        builder.vertex(mat, maxX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);

        builder.vertex(mat, maxX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        builder.vertex(mat, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);

        builder.vertex(mat, minX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        builder.vertex(mat, minX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);
    }


    //private static BlockState state; fuck you java
    public static BlockState readBlockStateFromNbt(NbtCompound compound) {
        String name = compound.getString("Name").orElse(null);
        if (name == null || name.isEmpty()) {
            return Blocks.AIR.getDefaultState();
        }
        Identifier id = Identifier.of(name);
        Block block = Registries.BLOCK.get(id);
        AtomicReference<BlockState> state = new AtomicReference<>(block.getDefaultState());

        if (compound.contains("Properties")) {
            Optional<NbtCompound> oprops = compound.getCompound("Properties");
            oprops.ifPresent(props-> {
                for (String key : props.getKeys()) {
                    Property<?> prop = block.getStateManager().getProperty(key);
                    if (prop == null) continue;

                    String valueStr = props.getString(key).orElse(null);
                    if (valueStr == null) continue;

                    state.set(setProperty(state.get(), prop, valueStr));
                }
            });
        }
        return state.get();
    }

    private static <T extends Comparable<T>> BlockState setProperty(BlockState state, Property<T> prop, String value) {
        return prop.parse(value).map(v -> state.with(prop, v)).orElse(state);
    }
}
