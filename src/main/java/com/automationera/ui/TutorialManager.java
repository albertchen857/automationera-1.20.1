package com.automationera.ui;

import com.automationera.AutomationeraClient;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.fabric.api.renderer.v1.render.BlockVertexConsumerProvider;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;

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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
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

        public void info(String msg, SpriteAtlasTexture spriteAtlasTexture) {
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

    public static BlockVertexConsumerProvider fromImmediate(VertexConsumerProvider.Immediate immediate) {
        return (layer) -> immediate.getBuffer(mapLayer(layer));
    }

    private static RenderLayer mapLayer(BlockRenderLayer layer) {
        return switch (layer) {
            case SOLID          -> RenderLayer.getSolid();
            case CUTOUT         -> RenderLayer.getCutout();
            case CUTOUT_MIPPED  -> RenderLayer.getCutoutMipped();
            case TRANSLUCENT -> RenderLayer.getTranslucentMovingBlock();
            case TRIPWIRE -> RenderLayer.getTripwire();
        };
    }
    static final class DummyWorld implements BlockRenderView {
        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override public BlockState getBlockState(BlockPos pos) { return Blocks.AIR.getDefaultState(); }
        @Override public FluidState getFluidState(BlockPos pos) { return Fluids.EMPTY.getDefaultState(); }
        @Override public int getHeight() { return 256; }
        @Override public int getBottomY() { return 0; }
        @Override public float getBrightness(Direction dir, boolean shaded) { return 15; }  // 关键：满亮
        @Override public LightingProvider getLightingProvider() {
            return LightingProvider.DEFAULT;
        }
        @Override public int getColor(BlockPos pos, ColorResolver colorResolver) {
            return 0;
        }

        // 其余默认或抛不影响调用的 UnsupportedOperationException 均可
    }
    public void renderStructure3D(DrawContext ctx, NbtCompound nbt, int step, IsometricRenderState state, int width, int height, int size, List<List<TutorialGroupScreen.SelectionBox>> SelectBox) {
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockRenderManager brm = mc.getBlockRenderManager();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        var matrices = new MatrixStack();
        var vp = fromImmediate(immediate);

        matrices.push();
        matrices.translate(width / 3f * 2f - 30, height / 8f * 5f + state.yc - 40, 200);//position
        matrices.scale(60, 60, 60);         // GUI → 3D 放大
        matrices.scale(1f, -1f, 1f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35f));

        //GlStateManager._enableDepthTest();
        //GlStateManager._depthMask(true);
        //GlStateManager._enableCull();
        //GlStateManager._enableBlend();

        matrices.push();
        matrices.translate(0, 0, 0);
        brm.renderBlockAsEntity(Blocks.BRICKS.getDefaultState(), matrices, immediate, 15728880, OverlayTexture.DEFAULT_UV);//bs
        var model = brm.getModel(Blocks.BRICKS.getDefaultState());
        long rnd  = 0;

        brm.getModelRenderer().render(new DummyWorld(), model, Blocks.BRICKS.getDefaultState(), BlockPos.ORIGIN, matrices, vp, false,
                                rnd, OverlayTexture.DEFAULT_UV);
        renderFluidCube(matrices, immediate, Identifier.of("minecraft", "block/lava_still"), 0, 1f, 0.9f, 63, 118, 228);
        matrices.pop();

        immediate.draw();
        //GlStateManager._disableDepthTest();
        //GlStateManager._depthMask(true);
        //GlStateManager._disableCull();
        //GlStateManager._disableBlend();
        matrices.pop();
    }

    public static void renderFluidCube(MatrixStack matrices, VertexConsumerProvider consumers,
                                       Identifier texture, float y1, float y2, float alpha, int r,int g, int b) {
        Sprite sprite = (AtlasCache.FLUID_SPRITE != null)
                ? AtlasCache.FLUID_SPRITE
                : new SpriteIdentifier(Identifier.of("automationera", "atlases/ae_block.json"), texture).getSprite();

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
        builder.vertex(mat, 0, y2, 0).texture(u0, v0).color(r, g, b, a).light(light).normal(0, 1, 0).overlay(overlay);
        builder.vertex(mat, 1, y2, 0).texture(u1, v0).color(r, g, b, a).light(light).normal(0, 1, 0).overlay(overlay);
        builder.vertex(mat, 1, y2, 1).texture(u1, v1).color(r, g, b, a).light(light).normal(0, 1, 0).overlay(overlay);
        builder.vertex(mat, 0, y2, 1).texture(u0, v1).color(r, g, b, a).light(light).normal(0, 1, 0).overlay(overlay);

        // 下面
        builder.vertex(mat, 0, y1, 0).texture(u0, v0).color(r, g, b, a).light(light).normal(0, -1, 0).overlay(overlay);
        builder.vertex(mat, 1, y1, 0).texture(u1, v0).color(r, g, b, a).light(light).normal(0, -1, 0).overlay(overlay);
        builder.vertex(mat, 1, y1, 1).texture(u1, v1).color(r, g, b, a).light(light).normal(0, -1, 0).overlay(overlay);
        builder.vertex(mat, 0, y1, 1).texture(u0, v1).color(r, g, b, a).light(light).normal(0, -1, 0).overlay(overlay);

        // 前面
        builder.vertex(mat, 0, y1, 0).texture(u0, v0).color(r, g, b, a).light(light).normal(0, 0, -1).overlay(overlay);
        builder.vertex(mat, 1, y1, 0).texture(u1, v0).color(r, g, b, a).light(light).normal(0, 0, -1).overlay(overlay);
        builder.vertex(mat, 1, y2, 0).texture(u1, v1).color(r, g, b, a).light(light).normal(0, 0, -1).overlay(overlay);
        builder.vertex(mat, 0, y2, 0).texture(u0, v1).color(r, g, b, a).light(light).normal(0, 0, -1).overlay(overlay);

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
        builder.vertex(mat, 1, y1, 0).texture(u0, v0).color(r, g, b, a).light(light).normal(1, 0, 0).overlay(overlay);
        builder.vertex(mat, 1, y1, 1).texture(u1, v0).color(r, g, b, a).light(light).normal(1, 0, 0).overlay(overlay);
        builder.vertex(mat, 1, y2, 1).texture(u1, v1).color(r, g, b, a).light(light).normal(1, 0, 0).overlay(overlay);
        builder.vertex(mat, 1, y2, 0).texture(u0, v1).color(r, g, b, a).light(light).normal(1, 0, 0).overlay(overlay);
    }

    public static void renderSelectionBoxOutline(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            TutorialGroupScreen.SelectionBox box,
            float lineWidth,
            float r, float g, float b, float a
    ) {
        try {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
