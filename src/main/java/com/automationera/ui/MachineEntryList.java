package com.automationera.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class MachineEntryList extends EntryListWidget<MachineEntryList.Entry> {

    public MachineEntryList(MinecraftClient client, int width, int height, int y, int itemHeight) {
        super(client, width, height, y, itemHeight);
    }

    /**
     * 添加条目，onClick里建议传index给Screen
     */
    public void addEntry(int index, ItemStack icon, Text text, Runnable onClick) {
        this.addEntry(new Entry(this, index, icon, text, onClick));
    }

    // 高亮用
    public void selectByIndex(int index) {
        if (index >= 0 && index < this.children().size()) {
            this.setSelected(this.children().get(index));
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }

    public static class Entry extends EntryListWidget.Entry<Entry> {
        private final MachineEntryList parentList;
        private final int index;
        private final ItemStack icon;
        private final Text text;
        private final Runnable onClick;

        public Entry(MachineEntryList parentList, int index, ItemStack icon, Text text, Runnable onClick) {
            this.parentList = parentList;
            this.index = index;
            this.icon = icon;
            this.text = text;
            this.onClick = onClick;
        }

        @Override
        public void render(DrawContext ctx, int index, int y, int x, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            // 高亮选中项
            if (parentList.getSelectedOrNull() == this) {
                ctx.fill(x, y, x + width, y + height, 0x44007FFF);
            } else if (hovered) {
                ctx.fill(x, y, x + width, y + height, 0x22000000);
            }
            ctx.drawItem(icon, x + 4, y + 2);
            ctx.drawText(MinecraftClient.getInstance().textRenderer, text, x + 24, y + 6, 0xFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            parentList.setSelected(this); // 列表高亮当前项
            onClick.run();                // 通知外部Screen
            return true;
        }
    }
    @Override
    public int getRowWidth() {
        return 120;
    }
    @Override
    public int getRowLeft() {
        return this.getX() + 8;
    }
}
