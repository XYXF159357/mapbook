package com.example.mapbook.client;

import com.example.mapbook.MapBookOpenPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapBookScreen extends Screen {

    private final List<ItemStack> maps;
    private final List<MapBookOpenPayload.MapCoord> coords;

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

    private final Map<MapId, MapRenderState> renderStateCache = new HashMap<>();
    private MapRenderer mapRenderer;

    private static final double UPDATE_RADIUS = 512.0;

    public MapBookScreen(List<ItemStack> maps, List<MapBookOpenPayload.MapCoord> coords) {
        super(Component.translatable("screen.mapbook.title"));
        this.maps = maps;
        this.coords = coords;
    }

    @Override
    protected void init() {
        super.init();
        this.mapRenderer = Minecraft.getInstance().getMapRenderer();
        centerOnPlayer();

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

    private void centerOnPlayer() {
        if (this.minecraft == null || this.minecraft.player == null || coords.isEmpty()) return;

        double playerX = this.minecraft.player.getX();
        double playerZ = this.minecraft.player.getZ();

        int closestIndex = 0;
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < coords.size(); i++) {
            MapBookOpenPayload.MapCoord c = coords.get(i);
            double dist = Math.pow(c.centerX() - playerX, 2) + Math.pow(c.centerZ() - playerZ, 2);
            if (dist < minDist) {
                minDist = dist;
                closestIndex = i;
            }
        }

        int minGridX = Integer.MAX_VALUE, maxGridX = Integer.MIN_VALUE;
        int minGridZ = Integer.MAX_VALUE, maxGridZ = Integer.MIN_VALUE;
        for (MapBookOpenPayload.MapCoord c : coords) {
            int gx = Math.floorDiv(c.centerX() - 64, 128) * 128;
            int gz = Math.floorDiv(c.centerZ() - 64, 128) * 128;
            minGridX = Math.min(minGridX, gx);
            maxGridX = Math.max(maxGridX, gx);
            minGridZ = Math.min(minGridZ, gz);
            maxGridZ = Math.max(maxGridZ, gz);
        }
        double gridCenterX = (minGridX + maxGridX + 128) / 2.0;
        double gridCenterZ = (minGridZ + maxGridZ + 128) / 2.0;

        MapBookOpenPayload.MapCoord closest = coords.get(closestIndex);
        double mapCenterX = Math.floorDiv(closest.centerX() - 64, 128) * 128 + 64;
        double mapCenterZ = Math.floorDiv(closest.centerZ() - 64, 128) * 128 + 64;

        offsetX = -(mapCenterX - gridCenterX) * scale;
        offsetY = -(mapCenterZ - gridCenterZ) * scale;
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

        if (maps.isEmpty() || coords.isEmpty()) {
            Component empty = Component.translatable("screen.mapbook.empty");
            graphics.text(this.font, empty,
                    this.width / 2 - this.font.width(empty) / 2, this.height / 2, 0xAAAAAA);
            return;
        }

        List<GridMap> gridMaps = new ArrayList<>();
        int minGridX = Integer.MAX_VALUE, maxGridX = Integer.MIN_VALUE;
        int minGridZ = Integer.MAX_VALUE, maxGridZ = Integer.MIN_VALUE;

        int count = Math.min(maps.size(), coords.size());
        for (int i = 0; i < count; i++) {
            MapBookOpenPayload.MapCoord c = coords.get(i);
            int gridLeftX = Math.floorDiv(c.centerX() - 64, 128) * 128;
            int gridLeftZ = Math.floorDiv(c.centerZ() - 64, 128) * 128;

            gridMaps.add(new GridMap(i, gridLeftX, gridLeftZ));
            minGridX = Math.min(minGridX, gridLeftX);
            maxGridX = Math.max(maxGridX, gridLeftX);
            minGridZ = Math.min(minGridZ, gridLeftZ);
            maxGridZ = Math.max(maxGridZ, gridLeftZ);
        }

        double gridCenterX = (minGridX + maxGridX + 128) / 2.0;
        double gridCenterZ = (minGridZ + maxGridZ + 128) / 2.0;

        double playerX = this.minecraft != null && this.minecraft.player != null
                ? this.minecraft.player.getX() : 0;
        double playerZ = this.minecraft != null && this.minecraft.player != null
                ? this.minecraft.player.getZ() : 0;

        graphics.enableScissor(0, 25, this.width, this.height - 40);

        for (GridMap gm : gridMaps) {
            double relX = gm.gridLeftX + 64 - gridCenterX;
            double relZ = gm.gridLeftZ + 64 - gridCenterZ;

            double screenCenterX = this.width / 2.0 + offsetX + relX * scale;
            double screenCenterY = this.height / 2.0 + offsetY + relZ * scale;

            double halfSize = 64.0 * scale;
            int drawX = (int)(screenCenterX - halfSize);
            int drawY = (int)(screenCenterY - halfSize);
            int drawSize = (int)(128.0 * scale);

            ItemStack mapStack = maps.get(gm.index);
            MapId mapId = mapStack.get(DataComponents.MAP_ID);
            if (mapId == null || this.minecraft == null || this.minecraft.level == null) {
                drawPlaceholder(graphics, gm, drawX, drawY, drawSize);
                continue;
            }

            MapItemSavedData mapData = MapItem.getSavedData(mapId, this.minecraft.level);
            if (mapData == null) {
                drawPlaceholder(graphics, gm, drawX, drawY, drawSize);
                continue;
            }

            boolean nearPlayer = Math.abs(mapData.centerX - playerX) < UPDATE_RADIUS
                    && Math.abs(mapData.centerZ - playerZ) < UPDATE_RADIUS;

            MapRenderState renderState = renderStateCache.get(mapId);
            if (renderState == null || nearPlayer) {
                renderState = new MapRenderState();
                mapRenderer.extractRenderState(mapId, mapData, renderState);
                renderStateCache.put(mapId, renderState);
            }

            graphics.pose().pushMatrix();
            graphics.pose().translate((float) drawX, (float) drawY);
            graphics.pose().scale((float) drawSize / 128f, (float) drawSize / 128f);
            graphics.submitMapRenderState(renderState); // 直接调用，无需强制转换
            graphics.pose().popMatrix();

            graphics.outline(drawX, drawY, drawSize, drawSize, 0x40FFFFFF);
        }

        graphics.disableScissor();

        Component zoomText = Component.literal("缩放: " + String.format("%.0f%%", scale * 100));
        graphics.text(this.font, zoomText, 10, this.height - 25, 0xCCCCCC);
    }

    private void drawPlaceholder(GuiGraphicsExtractor graphics, GridMap gm, int drawX, int drawY, int drawSize) {
        int color = 0xFF000000 | ((gm.gridLeftX * 31 + gm.gridLeftZ * 17 + gm.index * 67) & 0xFFFFFF);
        graphics.fill(drawX, drawY, drawX + drawSize, drawY + drawSize, color);
        graphics.outline(drawX, drawY, drawSize, drawSize, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record GridMap(int index, int gridLeftX, int gridLeftZ) {}
}
