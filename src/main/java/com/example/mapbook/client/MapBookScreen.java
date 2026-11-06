package com.example.mapbook.client;

import com.example.mapbook.MapBookMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.List;

public class MapBookScreen extends Screen {

    private final List<ItemStack> maps;

    private double offsetX = 0;
    private double offsetY = 0;
    private double scale = 1.0;

    private boolean dragging = false;
    private double dragStartX;
    private double dragStartY;
    private double dragStartOffsetX;
    private double dragStartOffsetY;

    private Button zoomInButton;
    private Button zoomOutButton;

    public MapBookScreen(List<ItemStack> maps) {
        super(Component.translatable("screen.mapbook.title"));
        this.maps = maps;
    }

    @Override
    protected void init() {
        super.init();
        int buttonY = this.height - 30;

        zoomInButton = Button.builder(Component.literal("+"), b -> {
            scale = Math.min(scale * 1.25, 4.0);
        }).bounds(this.width - 80, buttonY, 20, 20).build();

        zoomOutButton = Button.builder(Component.literal("-"), b -> {
            scale = Math.max(scale / 1.25, 0.25);
        }).bounds(this.width - 50, buttonY, 20, 20).build();

        addRenderableWidget(zoomInButton);
        addRenderableWidget(zoomOutButton);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.buttonInfo().button() == InputConstants.MOUSE_BUTTON_LEFT) {
            dragging = true;
            dragStartX = event.x();
            dragStartY = event.y();
            dragStartOffsetX = offsetX;
            dragStartOffsetY = offsetY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            offsetX = dragStartOffsetX + (event.x() - dragStartX);
            offsetY = dragStartOffsetY + (event.y() - dragStartY);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.buttonInfo().button() == InputConstants.MOUSE_BUTTON_LEFT) {
            dragging = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(this.font, this.title,
                this.width / 2 - this.font.width(this.title) / 2, 10, 0xFFFFFF);

        if (maps.isEmpty()) {
            Component empty = Component.translatable("screen.mapbook.empty");
            graphics.text(this.font, empty,
                    this.width / 2 - this.font.width(empty) / 2, this.height / 2, 0xAAAAAA);
            return;
        }

        // 收集地图数据
        List<MapItemSavedData> dataList = new ArrayList<>();
        for (ItemStack stack : maps) {
            if (!stack.is(Items.FILLED_MAP)) continue;
            MapId mapId = stack.get(DataComponents.MAP_ID);
            if (mapId == null || this.minecraft == null || this.minecraft.level == null) continue;
            MapItemSavedData data = MapItem.getSavedData(mapId, this.minecraft.level);
            if (data != null) dataList.add(data);
        }

        if (dataList.isEmpty()) return;

        // 检查是否所有地图坐标完全相同
        boolean allCoordsSame = true;
        if (dataList.size() > 1) {
            MapItemSavedData first = dataList.get(0);
            for (int i = 1; i < dataList.size(); i++) {
                MapItemSavedData d = dataList.get(i);
                if (d.centerX != first.centerX || d.centerZ != first.centerZ) {
                    allCoordsSame = false;
                    break;
                }
            }
        }

        graphics.enableScissor(0, 25, this.width, this.height - 40);

        if (allCoordsSame) {
            // === 兜底：坐标全部相同 → 网格排列 ===
            int cols = (int) Math.ceil(Math.sqrt(dataList.size()));
            int rows = (int) Math.ceil((double) dataList.size() / cols);

            for (int i = 0; i < dataList.size(); i++) {
                int row = i / cols;
                int col = i % cols;

                // 每格间距 140 像素（地图本身 128 + 12 间隙）
                double gridX = (col - (cols - 1) / 2.0) * 140;
                double gridY = (row - (rows - 1) / 2.0) * 140;

                double screenCenterX = this.width / 2.0 + offsetX + gridX * scale;
                double screenCenterY = this.height / 2.0 + offsetY + gridY * scale;

                double halfSize = 64.0 * scale;
                int drawX = (int)(screenCenterX - halfSize);
                int drawY = (int)(screenCenterY - halfSize);
                int drawSize = (int)(128.0 * scale);

                // 用索引生成颜色，保证每张地图颜色不同
                int color = 0xFF000000 | ((i * 67 + 100) & 0xFFFFFF);
                graphics.fill(drawX, drawY, drawX + drawSize, drawY + drawSize, color);
                graphics.outline(drawX, drawY, drawSize, drawSize, 0xFFFFFFFF);
            }
        } else {
            // === 正常：按世界坐标拼接 ===
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (MapItemSavedData d : dataList) {
                minX = Math.min(minX, d.centerX);
                maxX = Math.max(maxX, d.centerX);
                minZ = Math.min(minZ, d.centerZ);
                maxZ = Math.max(maxZ, d.centerZ);
            }
            double worldCenterX = (minX + maxX) / 2.0;
            double worldCenterZ = (minZ + maxZ) / 2.0;

            for (MapItemSavedData d : dataList) {
                double relX = d.centerX - worldCenterX;
                double relZ = d.centerZ - worldCenterZ;

                double screenCenterX = this.width / 2.0 + offsetX + relX * scale;
                double screenCenterY = this.height / 2.0 + offsetY + relZ * scale;

                double halfSize = 64.0 * scale;
                int drawX = (int)(screenCenterX - halfSize);
                int drawY = (int)(screenCenterY - halfSize);
                int drawSize = (int)(128.0 * scale);

                int color = 0xFF000000 | ((d.centerX * 31 + d.centerZ * 17) & 0xFFFFFF);
                graphics.fill(drawX, drawY, drawX + drawSize, drawY + drawSize, color);
                graphics.outline(drawX, drawY, drawSize, drawSize, 0xFFFFFFFF);
            }
        }

        graphics.disableScissor();

        Component zoomText = Component.literal("缩放: " + String.format("%.0f%%", scale * 100));
        graphics.text(this.font, zoomText, 10, this.height - 25, 0xCCCCCC);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
