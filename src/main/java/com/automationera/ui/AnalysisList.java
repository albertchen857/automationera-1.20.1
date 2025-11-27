package com.automationera.ui;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AnalysisList {
    private static List<@NotNull Item> NonInclude = List.of(
            Items.AIR,
            Items.BARRIER,
            Items.YELLOW_CONCRETE,
            Items.BLACK_CONCRETE,
            Items.BLUE_STAINED_GLASS,
            Items.GREEN_STAINED_GLASS
    );
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
            return count + "/" + count / 64 + "S " + count % 64;
        }
    }
    private final List<Entry> result;

    public List<Entry> getResult() { return result; }

    // 对比底层原文
    public AnalysisList(NbtCompound currentNbt, NbtCompound baseNbt, boolean consist) {
        Map<Item, Integer> now = statItem(currentNbt, true);
        if (consist && baseNbt != null) {
            Map<Item, Integer> prev = statItem(baseNbt, true);
            var it = now.entrySet().iterator();
            while (it.hasNext()){
                var e=it.next();
                Item item = e.getKey();
                int num = e.getValue()- prev.getOrDefault(item,0);
                if (num>0){
                    e.setValue(num);
                }else{
                    it.remove();
                }
            }
        }
        this.result = toEntryList(now);
    }

    // 普通单步统计
    public AnalysisList(NbtCompound nbt) {
        this.result = toEntryList(statItem(nbt, true));
    }

    private Map<Item, Integer> statItem(NbtCompound nbt, boolean include) {
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
            if (!(item == null || item.equals(Items.AIR) || (NonInclude.contains(item)) && include)) {
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

