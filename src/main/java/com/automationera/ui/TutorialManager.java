package com.automationera.ui;

import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.json.ModelTransformationMode;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
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

    public static void renderStructure3D(Screen screen, NbtCompound nbt, IsometricRenderState state, int width, int height, int size) {
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
        matrices.translate(width / 3f * 2f - 30, height / 8f * 5f + state.yc - 30, 200);//position

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

            matrices.push();
            matrices.translate(x, y, z);

            if (!bs.isAir() && bs.getFluidState().isEmpty()) {
                brm.renderBlockAsEntity(bs, matrices, immediate, 15728880, OverlayTexture.DEFAULT_UV); //Main render
            }else if (bs.getFluidState() != null && !bs.getFluidState().isEmpty()) {
                LOGGER.info("Try rendering fluid");
                var handler = FluidRenderHandlerRegistry.INSTANCE.get(bs.getFluidState().getFluid());
                if (handler != null)
                    handler.renderFluid(
                            new BlockPos(x, y, z),
                            mc.world,
                            immediate.getBuffer(RenderLayer.getTranslucent()),
                            bs,
                            bs.getFluidState()
                    );

                LOGGER.info("imm:{},world:{},pos:{},bs:{}",immediate.getBuffer(RenderLayer.getTranslucent()),mc.world,new BlockPos(x, y, z),bs);
            }
            matrices.pop();
        }
        immediate.draw();

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.disableBlend();

        matrices.pop();
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
