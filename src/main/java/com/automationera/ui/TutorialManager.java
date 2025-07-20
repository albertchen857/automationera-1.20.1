package com.automationera.ui;

import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.json.ModelTransformationMode;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class TutorialManager {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("AutomationEraTutorial");
    private static final Gson GSON = new Gson();

    public static NbtCompound loadNbtFromResource(String group, int step) {
        ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
        Identifier id = Identifier.of("automationera","tutorial/"+group+"/"+group+"_step"+step+".nbt");
        Resource res = rm.getResource(id).orElse(null);
        LOGGER.info("loadNbtFromResource: res={} id={}", res, id);
        if (res == null) return null;
        try (InputStream is = res.getInputStream()) {
            return NbtIo.readCompressed(is, NbtSizeTracker.of(Long.MAX_VALUE));
        } catch (Exception e) {
            LOGGER.error("NBT加载失败", e);
            return null;
        }
    }

    public static void renderStructure3D(Screen screen, NbtCompound nbt, IsometricRenderState state, int centerX, int centerY, int size) {
        if (nbt == null || !nbt.contains("palette") || !nbt.contains("blocks")) {
            LOGGER.warn("renderStructure3D: NBT无效或缺失根标签");
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockRenderManager brm = mc.getBlockRenderManager();

        // 解析 palette
        List<BlockState> palette = new ArrayList<>();
        for (NbtElement e : nbt.getList("palette", NbtElement.COMPOUND_TYPE))
            palette.add(readBlockStateFromNbt((NbtCompound)e));
        LOGGER.info("palette size={}", palette.size());

        var blocks = nbt.getList("blocks", NbtElement.COMPOUND_TYPE);
        // 计算中心和缩放
        double minX=99e6, maxX=-99e6, minY=99e6, maxY=-99e6, minZ=99e6, maxZ=-99e6;
        for (NbtElement blk : blocks) {
            NbtList pos = ((NbtCompound)blk).getList("pos", NbtElement.INT_TYPE);
            minX = Math.min(minX, pos.getInt(0)); maxX = Math.max(maxX, pos.getInt(0));
            minY = Math.min(minY, pos.getInt(1)); maxY = Math.max(maxY, pos.getInt(1));
            minZ = Math.min(minZ, pos.getInt(2)); maxZ = Math.max(maxZ, pos.getInt(2));
        }
        double dx=(minX+maxX)/2f, dy=(minY+maxY)/2f, dz=(minZ+maxZ)/2f;
        double globalScale = state.scale * size / Math.max(1, Math.max(maxX-minX+1, Math.max(maxY-minY+1, maxZ-minZ+1)));
        LOGGER.info("centerPerAxis dx={} dy={} dz={} scale={}", dx,dy,dz,globalScale);

        var matrices = new MatrixStack();
        matrices.push();
        matrices.translate(centerX, centerY, 200);
        matrices.scale((float) globalScale, (float) -globalScale, (float) globalScale);
        matrices.multiply(new Quaternionf().rotateXYZ(
                (float)Math.toRadians(state.pitch),
                (float)Math.toRadians(state.rotation),
                0
        ));
        matrices.translate(-dx, -dy, -dz);

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        VertexConsumerProvider.Immediate vc = mc.getBufferBuilders().getEntityVertexConsumers();

        for (NbtElement blk : blocks) {
            NbtCompound tag = (NbtCompound)blk;
            NbtList pos = tag.getList("pos", NbtElement.INT_TYPE);
            int x=pos.getInt(0), y=pos.getInt(1), z=pos.getInt(2);
            BlockState bs = palette.get(Math.min(tag.getInt("state"), palette.size()-1));
            if (bs.isAir()) continue;

            matrices.push();
            matrices.translate(x, y, z);
            brm.renderBlockAsEntity(bs, matrices, vc, 0xF000F0, 0);
            matrices.pop();
        }
        vc.draw();
        RenderSystem.disableDepthTest();
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
