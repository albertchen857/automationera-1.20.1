package com.automationera.ui;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

public class AnalysisList {
    public static class Entry {
        public final ItemStack stack;
        public final Text displayName;
        public final int count;
        public Entry(ItemStack stack, Text displayName, int count) {
            this.stack = stack;
            this.displayName = displayName;
            this.count = count;
        }
        public String countBoxGroup() {
            int boxSize = 64*27;
            int groupSize = 64;
            int boxes = count / boxSize;
            int boxRemain = count % boxSize;
            int groups = boxRemain / groupSize;
            int groupRemain = boxRemain % groupSize;
            // 格式如：128/2B0S0
            return count + "/" + boxes + "B " + groups + "S " + groupRemain;
        }
    }
    private final List<Entry> result;

    public List<Entry> getResult() { return result; }

    // 对比底层原文
    public AnalysisList(NbtCompound currentNbt, NbtCompound baseNbt, boolean consist) {
        Map<Item, Integer> now = statItem(currentNbt);
        if (consist && baseNbt != null) {
            Map<Item, Integer> base = statItem(baseNbt);
            Map<Item, Integer> diff = new HashMap<>();
            for (var e : now.entrySet()) {
                Item item = e.getKey();
                int before = base.getOrDefault(item, 0);
                int after = e.getValue();
                if (after > before) diff.put(item, after - before);
            }
            this.result = toEntryList(diff);
        } else {
            this.result = toEntryList(now);
        }
    }

    // 普通单步统计
    public AnalysisList(NbtCompound nbt) {
        this.result = toEntryList(statItem(nbt));
    }

    private Map<Item, Integer> statItem(NbtCompound nbt) {
        Map<Item, Integer> map = new HashMap<>();
        if (nbt == null || !nbt.contains("palette") || !nbt.contains("blocks")) return map;
        var palette = nbt.getList("palette", 10);
        var blocks = nbt.getList("blocks", 10);
        for (int i = 0; i < blocks.size(); i++) {
            var blk = blocks.getCompound(i);
            int id = blk.getInt("state");
            String blockId = palette.getCompound(id).getString("Name");
            var block = Registries.BLOCK.get(Identifier.of(blockId));
            var item = block.asItem();
            if (item != null && !item.equals(net.minecraft.item.Items.AIR)) {
                map.put(item, map.getOrDefault(item, 0) + 1);
            }
        }
        return map;
    }

    private List<Entry> toEntryList(Map<Item, Integer> map) {
        List<Entry> list = new ArrayList<>();
        for (var e : map.entrySet()) {
            list.add(new Entry(new ItemStack(e.getKey()), e.getKey().getName(), e.getValue()));
        }
        list.sort((a, b) -> Integer.compare(b.count, a.count));
        return list;
    }

    public int size() { return result.size(); }
    public Entry get(int i) { return result.get(i); }
}

