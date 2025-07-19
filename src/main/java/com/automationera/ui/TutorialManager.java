package com.automationera.ui;

import com.google.gson.Gson;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
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
        //LOGGER.info("Res Null: {},{}|{}/{}", res, id, group, step);

        if (res == null) {
            return null;
        }
        try (InputStream is = res.getInputStream()) {
            return NbtIo.readCompressed(is, NbtSizeTracker.of(Long.MAX_VALUE));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 简单渲染 nbt 结构（俯视图），每个方块画成小方块
     */
    public static void renderNbtStructure(DrawContext ctx, String group, int step, int x, int y) {
        NbtCompound nbt = loadNbtFromResource(group, step);
        if (nbt == null) {
            ctx.drawText(MinecraftClient.getInstance().textRenderer, "找不到结构 " + group, x, y, 0xFF0000, false);
            return;
        }

        if (!nbt.contains("palette", NbtElement.LIST_TYPE) || !nbt.contains("blocks", NbtElement.LIST_TYPE)) {
            ctx.drawText(MinecraftClient.getInstance().textRenderer, "结构体格式不符 " + group, x, y + 12, 0xFF0000, false);
            LOGGER.error("Invalid structure nbt: " + nbt);
            return;
        }

        // 解析 palette
        List<BlockState> palette = new ArrayList<>();
        NbtList paletteList = nbt.getList("palette", NbtElement.COMPOUND_TYPE);
        for (NbtElement pal : paletteList) {
            palette.add(readBlockStateFromNbt((NbtCompound) pal));
        }

        // 渲染 blocks
        NbtList blocksList = nbt.getList("blocks", NbtElement.COMPOUND_TYPE);
        for (NbtElement blk : blocksList) {
            NbtCompound blkTag = (NbtCompound) blk;
            NbtList posList = blkTag.getList("pos", NbtElement.INT_TYPE);
            int px = posList.getInt(0);
            int py = posList.getInt(1);
            int pz = posList.getInt(2);
            int stateIdx = blkTag.getInt("state");
            BlockState state = (stateIdx < palette.size()) ? palette.get(stateIdx) : Blocks.AIR.getDefaultState();
            // 只渲染俯视图：x = x+px*12, y = y+pz*12
            int drawX = x + px * 12;
            int drawY = y + pz * 12;
            int color = state.getBlock() == Blocks.AIR ? 0 : 0xCCCCCC;
            ctx.fill(drawX, drawY, drawX + 10, drawY + 10, color);
        }
        LOGGER.info("size:{},{}\nblock:{},{}",palette.size(),palette,blocksList.size(),blocksList);
    }


    // BlockState解析修正，Name字段才是真正的Block ID
    public static BlockState readBlockStateFromNbt(NbtCompound compound) {
        Identifier blockId = Identifier.of(compound.getString("Name")); // 不是 "automationera"
        Block block = Registries.BLOCK.get(blockId);
        BlockState state = block.getDefaultState();
        // 处理 properties...
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

    public static class TutorialLoader {
        private static final Gson GSON = new Gson();

        public static TutorialData loadTutorial(String group) {
            Identifier id = Identifier.of("automationera", "nbt/" + group + ".json");

            try {
                ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
                Resource resource = rm.getResource(id).orElse(null);
                if (resource == null) return null;

                try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
                    return GSON.fromJson(reader, TutorialData.class);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static class TutorialData {
        public String id;
        public String title;
        public List<TutorialStep> steps;

        public static class TutorialStep {
            public String nbt;
            public String text;
        }
    }


}
