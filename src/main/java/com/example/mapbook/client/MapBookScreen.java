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

    // ---------- 拖动（26.3 新签名）----------

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

    // ---------- 渲染（保持不变）----------

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 10, 0xFFFFFF);

        if (maps.isEmpty()) {
            Component empty = Component.translatable("screen.mapbook.empty");
            graphics.text(this.font, empty, this.width / 2 - this.font.width(empty) / 2,
                    this.height / 2, 0xAAAAAA);
            return;
        }

        List<MapRenderEntry> entries = new ArrayList<>();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (ItemStack stack : maps) {
            if (!stack.is(Items.FILLED_MAP)) continue;
            MapId mapId = stack.get(DataComponents.MAP_ID);
            if (mapId == null || this.minecraft == null || this.minecraft.level == null) continue;
            MapItemSavedData data = MapItem.getSavedData(mapId, this.minecraft.level);
            if (data == null) continue;

            entries.add(new MapRenderEntry(data));
            minX = Math.min(minX, data.centerX);
            maxX = Math.max(maxX, data.centerX);
            minZ = Math.min(minZ, data.centerZ);
            maxZ = Math.max(maxZ, data.centerZ);
        }

        if (entries.isEmpty()) return;

        double worldCenterX = (minX + maxX) / 2.0;
        double worldCenterZ = (minZ + maxZ) / 2.0;
        double basePixelsPerBlock = 1.0;

        graphics.enableScissor(0, 25, this.width, this.height - 40);

        graphics.pose().pushMatrix();
        graphics.pose().translate((float)(this.width / 2 + offsetX), (float)(this.height / 2 + offsetY));
        graphics.pose().scale((float) scale, (float) scale);

        for (MapRenderEntry entry : entries) {
            System.out.println("!!! Render map: centerX=" + entry.data().centerX + " centerZ=" + entry.data().centerZ);
            MapItemSavedData data = entry.data;
            double relX = data.centerX - worldCenterX;
            double relZ = data.centerZ - worldCenterZ;
            double halfSizePx = 64 * basePixelsPerBlock;
            double centerPxX = relX * basePixelsPerBlock;
            double centerPxY = relZ * basePixelsPerBlock;

            int drawX = (int)(centerPxX - halfSizePx);
            int drawY = (int)(centerPxY - halfSizePx);
            int sizePx = (int)(128 * basePixelsPerBlock);

            int color = 0xFF000000 | (data.centerX * 31 + data.centerZ * 17) & 0xFFFFFF;
            graphics.fill(drawX, drawY, drawX + sizePx, drawY + sizePx, color);
            graphics.outline(drawX, drawY, sizePx, sizePx, 0xFFFFFFFF);
        }

        graphics.pose().popMatrix();
        graphics.disableScissor();

        Component zoomText = Component.literal("缩放: " + String.format("%.0f%%", scale * 100));
        graphics.text(this.font, zoomText, 10, this.height - 25, 0xCCCCCC);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record MapRenderEntry(MapItemSavedData data) {}
}
