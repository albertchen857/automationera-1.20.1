package com.automationera.ui;

import com.google.gson.Gson;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.gui.DrawContext;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class TutorialManager {
    /**
     * 从资源加载 nbt 文件
     */
    public static RegistryEntryLookup<Block> EntryLookup;
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("AutomationEraTutorial");
    private static final Gson GSON = new Gson();

    public static NbtCompound loadNbtFromResource(String group, int step) {
        TutorialData data = TutorialLoader.loadTutorial(group);
        ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
        Identifier id = Identifier.of(
                "automationera",
                "tutorial/"+group+"/"+group+"_step"+step+".nbt"
        );
        Resource res = rm.getResource(id).orElse(null);
        if (res == null) {
            LOGGER.info("Res Null: {},{}",res,id);
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

        // 解析 Structure nbt 的 palette/block
        List<BlockState> palette = new ArrayList<>();
        if (nbt.contains("palette", 9)) { // 9: List
            for (var pal : nbt.getList("palette", 10)) {
                palette.add(NbtHelper.toBlockState(EntryLookup, (NbtCompound) pal));
            }
        }

        if (nbt.contains("blocks", 9)) {
            for (var blk : nbt.getList("blocks", 10)) {
                NbtCompound blkTag = (NbtCompound) blk;
                int px = blkTag.getList("pos", 3).getInt(0);
                int py = blkTag.getList("pos", 3).getInt(1);
                int pz = blkTag.getList("pos", 3).getInt(2);
                int stateIdx = blkTag.getInt("state");
                BlockState state = palette.get(stateIdx);
                // 只渲染俯视图
                int drawX = x + px * 12;
                int drawY = y + pz * 12;
                int color = state.getBlock() == Blocks.AIR ? 0 : 0xCCCCCC;
                ctx.fill(drawX, drawY, drawX + 10, drawY + 10, color);
            }
        }
    }
    public class TutorialLoader {
        private static final Gson GSON = new Gson();

        public static TutorialData loadTutorial(String group) {
            Identifier id = Identifier.of("automationera", "tutorial/" + group + ".json");

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

    public class TutorialData {
        public String id;
        public String title;
        public List<TutorialStep> steps;

        public static class TutorialStep {
            public String nbt;
            public String text;
        }
    }

}
