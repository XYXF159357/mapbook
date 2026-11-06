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
        if (super.mouseClicked(event, doubleClick)) return true;
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

        // 收集地图数据 + 计算对齐后的网格左边界
        List<GridMap> gridMaps = new ArrayList<>();
        int minGridX = Integer.MAX_VALUE, maxGridX = Integer.MIN_VALUE;
        int minGridZ = Integer.MAX_VALUE, maxGridZ = Integer.MIN_VALUE;

        for (ItemStack stack : maps) {
            if (!stack.is(Items.FILLED_MAP)) continue;
            MapId mapId = stack.get(DataComponents.MAP_ID);
            if (mapId == null || this.minecraft == null || this.minecraft.level == null) continue;
            MapItemSavedData data = MapItem.getSavedData(mapId, this.minecraft.level);
            if (data == null) continue;

            // 关键：把地图左边界对齐到最近的 128 格网格
            int gridLeftX = Math.floorDiv(data.centerX - 64, 128) * 128;
            int gridLeftZ = Math.floorDiv(data.centerZ - 64, 128) * 128;

            gridMaps.add(new GridMap(data, gridLeftX, gridLeftZ));
            minGridX = Math.min(minGridX, gridLeftX);
            maxGridX = Math.max(maxGridX, gridLeftX);
            minGridZ = Math.min(minGridZ, gridLeftZ);
            maxGridZ = Math.max(maxGridZ, gridLeftZ);
        }

        if (gridMaps.isEmpty()) return;

        // 网格范围的中心（左边界 + 128 = 中心点）
        double gridCenterX = (minGridX + maxGridX + 128) / 2.0;
        double gridCenterZ = (minGridZ + maxGridZ + 128) / 2.0;

        graphics.enableScissor(0, 25, this.width, this.height - 40);

        for (GridMap gm : gridMaps) {
            // 相对网格中心的偏移（格），换算成屏幕像素（缩放后）
            double relX = gm.gridLeftX + 64 - gridCenterX;
            double relZ = gm.gridLeftZ + 64 - gridCenterZ;

            double screenCenterX = this.width / 2.0 + offsetX + relX * scale;
            double screenCenterY = this.height / 2.0 + offsetY + relZ * scale;

            double halfSize = 64.0 * scale;
            int drawX = (int)(screenCenterX - halfSize);
            int drawY = (int)(screenCenterY - halfSize);
            int drawSize = (int)(128.0 * scale);

            int color = 0xFF000000 | ((gm.gridLeftX * 31 + gm.gridLeftZ * 17) & 0xFFFFFF);
            graphics.fill(drawX, drawY, drawX + drawSize, drawY + drawSize, color);
            graphics.outline(drawX, drawY, drawSize, drawSize, 0xFFFFFFFF);
        }

        graphics.disableScissor();

        Component zoomText = Component.literal("缩放: " + String.format("%.0f%%", scale * 100));
        graphics.text(this.font, zoomText, 10, this.height - 25, 0xCCCCCC);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record GridMap(MapItemSavedData data, int gridLeftX, int gridLeftZ) {}
}
