package com.automationera.ui;

import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.json.ModelTransformationMode;

import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.*;

public class TutorialManager {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("AutomationEraTutorial");
    private static final Gson GSON = new Gson();

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

    public static void renderStructure3D(Screen screen, NbtCompound nbt,int step, IsometricRenderState state, int width, int height, int size, List<List<TutorialGroupScreen.SelectionBox>> SelectBox) {
        if (nbt == null || !nbt.contains("palette") || !nbt.contains("blocks")) {
            LOGGER.warn("renderStructure3D: NBT invalid");
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockRenderManager brm = mc.getBlockRenderManager();

        List<BlockState> palette = new ArrayList<>();
        for (NbtElement e : nbt.getList("palette", NbtElement.COMPOUND_TYPE))
            palette.add(readBlockStateFromNbt((NbtCompound)e));

        var blocks = nbt.getList("blocks", NbtElement.COMPOUND_TYPE);

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (NbtElement blk : blocks) {
            NbtList pos = ((NbtCompound)blk).getList("pos", NbtElement.INT_TYPE);
            minX = Math.min(minX, pos.getInt(0));
            maxX = Math.max(maxX, pos.getInt(0));
            minY = Math.min(minY, pos.getInt(1));
            maxY = Math.max(maxY, pos.getInt(1));
            minZ = Math.min(minZ, pos.getInt(2));
            maxZ = Math.max(maxZ, pos.getInt(2));
        }
        double dx = (minX + maxX) / 2.0;
        double dy = (minY + maxY) / 2.0;
        double dz = (minZ + maxZ) / 2.0;
        double globalScale = state.scale * size / Math.max(1, Math.max(maxX-minX+1, Math.max(maxY-minY+1, maxZ-minZ+1))) / 1.2f;

        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.translate(width / 3f * 2f - 30, height / 8f * 5f + state.yc - 40, 200);//position

        double nx = (Math.sin(Math.toRadians(state.rotation)) * Math.cos(Math.toRadians(state.pitch))),
                ny = (-Math.sin(Math.toRadians(state.pitch))),
                nz = (Math.cos(Math.toRadians(state.rotation)) * Math.cos(Math.toRadians(state.pitch)));
        matrices.multiply(new Quaternionf().rotateX((float) -Math.asin(ny)));
        matrices.multiply(new Quaternionf().rotateY((float) Math.atan2(nx, nz)));//Normal Rotation

        matrices.scale((float)globalScale, (float)-globalScale, (float)globalScale);
        matrices.translate(-dx, -dy, -dz);//center point

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderLights(
                new org.joml.Vector3f(0.2f, 1.0f, -0.7f).normalize(),
                new org.joml.Vector3f(-0.2f, 1.0f, 0.7f).normalize()
        );

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        for (NbtElement blk : blocks) {
            NbtCompound tag = (NbtCompound) blk;
            NbtList pos = tag.getList("pos", NbtElement.INT_TYPE);
            int x = pos.getInt(0), y = pos.getInt(1), z = pos.getInt(2);
            int idx = tag.getInt("state");
            if (idx < 0 || idx >= palette.size()) continue;
            BlockState bs = palette.get(idx);
//            LOGGER.info(bs.toString());
            matrices.push();
            matrices.translate(x, y, z);

            Block block = bs.getBlock();

            if (!bs.isAir() || bs.getRenderType() == BlockRenderType.MODEL) {
                //LOGGER.info("{},{},{},{}|{}",block,x,y,z,bs);
                brm.renderBlockAsEntity(bs, matrices, immediate, 15728880, OverlayTexture.DEFAULT_UV);
            }
            if (!bs.getFluidState().isEmpty()) {
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
                    renderFluidCube(matrices, immediate, tex, 0, fluidHeight, 0.9f, r, g, b);
                }
            }
            matrices.pop();
        }
        if (SelectBox != null && step-1 < SelectBox.size()) {
            long time = System.currentTimeMillis();
            float a = 0.5f + 0.3f * (float)Math.abs(Math.sin(time / 300.0));
            for (TutorialGroupScreen.SelectionBox box : SelectBox.get(step-1)) {
                renderSelectionBoxOutline(matrices, immediate, box, 2.0f, 1f, 1f, 0.0f, a); // 粗亮黄
            }
        }
        immediate.draw();

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
    }

    public static void renderFluidCube(MatrixStack matrices, VertexConsumerProvider consumers,
                                       Identifier texture, float y1, float y2, float alpha, int r,int g, int b) {
        MinecraftClient mc = MinecraftClient.getInstance();
        SpriteAtlasTexture atlas = mc.getBakedModelManager().getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        Sprite sprite = atlas.getSprite(texture);

        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        RenderSystem.setShaderColor(1, 1, 1, alpha);

        Matrix4f mat = matrices.peek().getPositionMatrix();
        VertexConsumer builder = consumers.getBuffer(RenderLayer.getTranslucent());

        int a = Math.round(255 * alpha);
        int light = 0xF000F0;

        // 上面
        builder.vertex(mat, 0, y2, 0).texture(u0, v0).color(r, g, b, a).light(light).normal(0, 1, 0);
        builder.vertex(mat, 1, y2, 0).texture(u1, v0).color(r, g, b, a).light(light).normal(0, 1, 0);
        builder.vertex(mat, 1, y2, 1).texture(u1, v1).color(r, g, b, a).light(light).normal(0, 1, 0);
        builder.vertex(mat, 0, y2, 1).texture(u0, v1).color(r, g, b, a).light(light).normal(0, 1, 0);

        // 下面
        builder.vertex(mat, 0, y1, 0).texture(u0, v0).color(r, g, b, a).light(light).normal(0, -1, 0);
        builder.vertex(mat, 1, y1, 0).texture(u1, v0).color(r, g, b, a).light(light).normal(0, -1, 0);
        builder.vertex(mat, 1, y1, 1).texture(u1, v1).color(r, g, b, a).light(light).normal(0, -1, 0);
        builder.vertex(mat, 0, y1, 1).texture(u0, v1).color(r, g, b, a).light(light).normal(0, -1, 0);

        // 前面
        builder.vertex(mat, 0, y1, 0).texture(u0, v0).color(r, g, b, a).light(light).normal(0, 0, -1);
        builder.vertex(mat, 1, y1, 0).texture(u1, v0).color(r, g, b, a).light(light).normal(0, 0, -1);
        builder.vertex(mat, 1, y2, 0).texture(u1, v1).color(r, g, b, a).light(light).normal(0, 0, -1);
        builder.vertex(mat, 0, y2, 0).texture(u0, v1).color(r, g, b, a).light(light).normal(0, 0, -1);

        // 后面
        builder.vertex(mat, 0, y1, 1).texture(u0, v0).color(r, g, b, a).light(light).normal(0, 0, 1);
        builder.vertex(mat, 1, y1, 1).texture(u1, v0).color(r, g, b, a).light(light).normal(0, 0, 1);
        builder.vertex(mat, 1, y2, 1).texture(u1, v1).color(r, g, b, a).light(light).normal(0, 0, 1);
        builder.vertex(mat, 0, y2, 1).texture(u0, v1).color(r, g, b, a).light(light).normal(0, 0, 1);

        // 左面
        builder.vertex(mat, 0, y1, 0).texture(u0, v0).color(r, g, b, a).light(light).normal(-1, 0, 0);
        builder.vertex(mat, 0, y1, 1).texture(u1, v0).color(r, g, b, a).light(light).normal(-1, 0, 0);
        builder.vertex(mat, 0, y2, 1).texture(u1, v1).color(r, g, b, a).light(light).normal(-1, 0, 0);
        builder.vertex(mat, 0, y2, 0).texture(u0, v1).color(r, g, b, a).light(light).normal(-1, 0, 0);

        // 右面
        builder.vertex(mat, 1, y1, 0).texture(u0, v0).color(r, g, b, a).light(light).normal(1, 0, 0);
        builder.vertex(mat, 1, y1, 1).texture(u1, v0).color(r, g, b, a).light(light).normal(1, 0, 0);
        builder.vertex(mat, 1, y2, 1).texture(u1, v1).color(r, g, b, a).light(light).normal(1, 0, 0);
        builder.vertex(mat, 1, y2, 0).texture(u0, v1).color(r, g, b, a).light(light).normal(1, 0, 0);
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

        com.mojang.blaze3d.systems.RenderSystem.lineWidth(lineWidth);

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
        Identifier id = Identifier.tryParse(compound.getString("Name"));
        Block block = Registries.BLOCK.get(id);
        BlockState state = block.getDefaultState();

        if (compound.contains("Properties", NbtElement.COMPOUND_TYPE)) {
            NbtCompound props = compound.getCompound("Properties");
            for (String key : props.getKeys()) {
                String value = props.getString(key);
                Property<?> prop = block.getStateManager().getProperty(key);
                if (prop != null) {
                    state = setProperty(state, prop, value);
                }
            }
        }
        return state;
    }

    // 用一个泛型辅助方法明确类型
    private static <T extends Comparable<T>> BlockState setProperty(BlockState state, Property<T> prop, String value) {
        Optional<T> opt = prop.parse(value);
        if (opt.isPresent()) {
            return state.with(prop, opt.get());
        }
        return state;
    }
}
