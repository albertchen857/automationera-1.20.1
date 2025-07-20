package com.automationera.ui;

import com.google.gson.Gson;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
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
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class TutorialManager {
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("AutomationEraTutorial");
    private static final Gson GSON = new Gson();

    /**
     * 从资源加载 nbt 文件
     */
    public static NbtCompound loadNbtFromResource(String group, int step) {
        ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
        Identifier id = Identifier.of(
                "automationera",
                "tutorial/" + group + "/" + group + "_step" + step + ".nbt"
        );
        Resource res = rm.getResource(id).orElse(null);
        if (res == null) return null;
        try (InputStream is = res.getInputStream()) {
            return NbtIo.readCompressed(is, NbtSizeTracker.of(Long.MAX_VALUE));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void render(Screen screen, NbtCompound nbt, IsometricRenderState state, int centerX, int centerY) {
        if (nbt == null || !nbt.contains("palette") || !nbt.contains("blocks")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        var itemRenderer = mc.getItemRenderer();

        List<BlockState> palette = new ArrayList<>();
        NbtList pal = nbt.getList("palette", NbtElement.COMPOUND_TYPE);
        for (NbtElement elem : pal) palette.add(TutorialManager.readBlockStateFromNbt((NbtCompound) elem));
        NbtList blocks = nbt.getList("blocks", NbtElement.COMPOUND_TYPE);

        for (NbtElement blk : blocks) {
            NbtCompound tag = (NbtCompound) blk;
            NbtList posList = tag.getList("pos", NbtElement.INT_TYPE);
            int x = posList.getInt(0);
            int y = posList.getInt(1);
            int z = posList.getInt(2);
            int stateIdx = tag.getInt("state");

            // 等轴投影
            double angle = state.angle;
            double scale = state.scale * 18.0;
            double sx = (x - z) * Math.cos(angle) - (x + z) * Math.sin(angle) * 0.5;
            double sy = (x + z) * Math.cos(angle) * 0.5 + y;
            int drawX = centerX + (int) (sx * scale);
            int drawY = centerY - (int) (sy * scale);

            BlockState stateObj = palette.get(stateIdx);
            ItemStack stack = new ItemStack(stateObj.getBlock().asItem());

            MatrixStack matrices = new MatrixStack();
            matrices.push();
            matrices.translate(drawX, drawY, 0); // drawX/drawY为你希望绘制的屏幕坐标
            MinecraftClient.getInstance().getItemRenderer().renderItem(
                    stack, ModelTransformationMode.GUI, 15728880, OverlayTexture.DEFAULT_UV, matrices,
                    MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(), null, 0
            );
            matrices.pop();
        }
    }

    public static NbtCompound render90 (NbtCompound nbt) {
        if (!nbt.contains("blocks", NbtElement.LIST_TYPE)) return nbt.copy();
        NbtCompound newNbt = nbt.copy();
        NbtList oldBlocks = nbt.getList("blocks", NbtElement.COMPOUND_TYPE);
        NbtList newBlocks = new NbtList();

        for (NbtElement blkElem : oldBlocks) {
            NbtCompound blk = (NbtCompound) blkElem;
            NbtList pos = blk.getList("pos", NbtElement.INT_TYPE);
            int x = pos.getInt(0);
            int y = pos.getInt(1);
            int z = pos.getInt(2);

            // 顺时针旋转90°: (x, y, z) -> (z, y, -x)
            NbtList newPos = new NbtList();
            newPos.add(NbtInt.of(z));
            newPos.add(NbtInt.of(y));
            newPos.add(NbtInt.of(-x));

            NbtCompound newBlk = blk.copy();
            newBlk.put("pos", newPos);
            newBlocks.add(newBlk);
        }
        newNbt.put("blocks", newBlocks);
        return newNbt;
    }

    private static List<BlockState> parsePalette(NbtCompound nbt) {
        List<BlockState> palette = new ArrayList<>();
        if (!nbt.contains("palette", NbtElement.LIST_TYPE)) return palette;
        NbtList palList = nbt.getList("palette", NbtElement.COMPOUND_TYPE);
        for (NbtElement elem : palList) {
            palette.add(readBlockStateFromNbt((NbtCompound) elem));
        }
        return palette;
    }

    public static BlockState readBlockStateFromNbt(NbtCompound compound) {
        Identifier blockId = Identifier.of(compound.getString("Name"));
        Block block = Registries.BLOCK.get(blockId);
        BlockState state = block.getDefaultState();
        if (compound.contains("Properties", NbtElement.COMPOUND_TYPE)) {
            NbtCompound props = compound.getCompound("Properties");
            for (String key : props.getKeys()) {
                String value = props.getString(key);
                Property<?> property = block.getStateManager().getProperty(key);
                if (property != null) {
                    Optional<?> parsed = property.parse(value);
                    if (parsed.isPresent()) {
                        state = with(state, property, parsed.get());
                    }
                }
            }
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, Object value) {
        return state.with(property, (T) value);
    }
}
