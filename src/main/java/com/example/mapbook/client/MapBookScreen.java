package com.example.mapbook.client;

import com.example.mapbook.MapBookOpenPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.Arrays;
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
    private double dragStartX, dragStartY, dragStartOffsetX, dragStartOffsetY;

    private Button zoomInButton, zoomOutButton;

    private final Map<MapId, Identifier> bakedTextures = new HashMap<>();
    private final Map<MapId, DynamicTexture> dynamicTextures = new HashMap<>();
    private final Map<MapId, byte[]> lastBakedColors = new HashMap<>();

    private static final double UPDATE_RADIUS = 512.0;

    public MapBookScreen(List<ItemStack> maps, List<MapBookOpenPayload.MapCoord> coords) {
        super(Component.translatable("screen.mapbook.title"));
        this.maps = maps;
        this.coords = coords;
    }

    @Override
    protected void init() {
        super.init();
        bakeAllMapTextures();
        centerOnPlayer();

        int buttonY = this.height - 30;
        zoomInButton = Button.builder(Component.literal("+"), b -> scale = Math.min(scale * 1.25, 4.0))
                .bounds(this.width - 80, buttonY, 20, 20).build();
        zoomOutButton = Button.builder(Component.literal("-"), b -> scale = Math.max(scale / 1.25, 0.25))
                .bounds(this.width - 50, buttonY, 20, 20).build();
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
            if (dist < minDist) { minDist = dist; closestIndex = i; }
        }

        int minGridX = Integer.MAX_VALUE, maxGridX = Integer.MIN_VALUE;
        int minGridZ = Integer.MAX_VALUE, maxGridZ = Integer.MIN_VALUE;
        for (MapBookOpenPayload.MapCoord c : coords) {
            int gx = Math.floorDiv(c.centerX() - 64, 128) * 128;
            int gz = Math.floorDiv(c.centerZ() - 64, 128) * 128;
            minGridX = Math.min(minGridX, gx); maxGridX = Math.max(maxGridX, gx);
            minGridZ = Math.min(minGridZ, gz); maxGridZ = Math.max(maxGridZ, gz);
        }
        double gridCenterX = (minGridX + maxGridX + 128) / 2.0;
        double gridCenterZ = (minGridZ + maxGridZ + 128) / 2.0;

        MapBookOpenPayload.MapCoord closest = coords.get(closestIndex);
        double mapCenterX = Math.floorDiv(closest.centerX() - 64, 128) * 128 + 64;
        double mapCenterZ = Math.floorDiv(closest.centerZ() - 64, 128) * 128 + 64;

        offsetX = -(mapCenterX - gridCenterX) * scale;
        offsetY = -(mapCenterZ - gridCenterZ) * scale;
    }

    private void bakeAllMapTextures() {
        if (this.minecraft == null || this.minecraft.level == null) return;
        for (int i = 0; i < Math.min(maps.size(), coords.size()); i++) {
            MapId mapId = maps.get(i).get(DataComponents.MAP_ID);
            if (mapId != null && !bakedTextures.containsKey(mapId)) {
                MapItemSavedData data = MapItem.getSavedData(mapId, this.minecraft.level);
                if (data != null) {
                    bakeTexture(mapId, data);
                    lastBakedColors.put(mapId, data.colors.clone());
                }
            }
        }
    }

    private void bakeTexture(MapId mapId, MapItemSavedData data) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 128, 128, true);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                byte colorId = data.colors[x + y * 128];
                int color = (colorId == 0) ? 0 : argbToAbgr(MapColor.getColorFromPackedId(colorId));
                image.setPixelABGR(x, y, color);
            }
        }
        DynamicTexture texture = new DynamicTexture(() -> "mapbook_map_" + mapId.id(), image);
        Identifier textureId = Identifier.fromNamespaceAndPath("mapbook", "dynamic/map_" + mapId.id());
        Minecraft.getInstance().getTextureManager().register(textureId, texture);

        AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(textureId);
        abstractTexture.setFilter(TriState.FALSE, false);

        bakedTextures.put(mapId, textureId);
        dynamicTextures.put(mapId, texture);
    }

    private void rebakeTexture(MapId mapId, MapItemSavedData data) {
        DynamicTexture old = dynamicTextures.remove(mapId);
        if (old != null) old.close();
        bakeTexture(mapId, data);
    }

    private static int argbToAbgr(int argb) {
        return ((argb & 0xFF) << 24) | ((argb & 0xFF00) << 8) | ((argb & 0xFF0000) >> 8) | ((argb >> 24) & 0xFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.buttonInfo().button() == InputConstants.MOUSE_BUTTON_LEFT) {
            dragging = true;
            dragStartX = event.x(); dragStartY = event.y();
            dragStartOffsetX = offsetX; dragStartOffsetY = offsetY;
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
        if (event.buttonInfo().button() == InputConstants.MOUSE_BUTTON_LEFT) dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 10, 0xFFFFFF);

        if (maps.isEmpty() || coords.isEmpty()) {
            Component empty = Component.translatable("screen.mapbook.empty");
            graphics.text(this.font, empty, this.width / 2 - this.font.width(empty) / 2, this.height / 2, 0xAAAAAA);
            return;
        }

        List<GridMap> gridMaps = new ArrayList<>();
        int minGridX = Integer.MAX_VALUE, maxGridX = Integer.MIN_VALUE;
        int minGridZ = Integer.MAX_VALUE, maxGridZ = Integer.MIN_VALUE;

        for (int i = 0; i < Math.min(maps.size(), coords.size()); i++) {
            MapBookOpenPayload.MapCoord c = coords.get(i);
            int gx = Math.floorDiv(c.centerX() - 64, 128) * 128;
            int gz = Math.floorDiv(c.centerZ() - 64, 128) * 128;
            gridMaps.add(new GridMap(i, gx, gz));
            minGridX = Math.min(minGridX, gx); maxGridX = Math.max(maxGridX, gx);
            minGridZ = Math.min(minGridZ, gz); maxGridZ = Math.max(maxGridZ, gz);
        }

        double gridCenterX = (minGridX + maxGridX + 128) / 2.0;
        double gridCenterZ = (minGridZ + maxGridZ + 128) / 2.0;
        double playerX = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getX() : 0;
        double playerZ = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getZ() : 0;

        graphics.enableScissor(0, 25, this.width, this.height - 40);

        for (GridMap gm : gridMaps) {
            MapId mapId = maps.get(gm.index).get(DataComponents.MAP_ID);
            if (mapId == null || this.minecraft == null || this.minecraft.level == null) {
                drawPlaceholder(graphics, gm, gridCenterX, gridCenterZ); continue;
            }

            MapItemSavedData data = MapItem.getSavedData(mapId, this.minecraft.level);
            if (data == null) { drawPlaceholder(graphics, gm, gridCenterX, gridCenterZ); continue; }

            boolean nearPlayer = Math.abs(data.centerX - playerX) < UPDATE_RADIUS
                    && Math.abs(data.centerZ - playerZ) < UPDATE_RADIUS;
            if (nearPlayer) {
                byte[] current = data.colors.clone();
                byte[] last = lastBakedColors.get(mapId);
                if (last == null || !Arrays.equals(last, current)) {
                    rebakeTexture(mapId, data);
                    lastBakedColors.put(mapId, current);
                }
            }

            Identifier textureId = bakedTextures.get(mapId);
            if (textureId == null) { drawPlaceholder(graphics, gm, gridCenterX, gridCenterZ); continue; }

            double relX = gm.gridLeftX + 64 - gridCenterX;
            double relZ = gm.gridLeftZ + 64 - gridCenterZ;
            int drawSize = (int)(128.0 * scale);
            int drawX = (int)(this.width / 2.0 + offsetX + relX * scale - drawSize / 2.0);
            int drawY = (int)(this.height / 2.0 + offsetY + relZ * scale - drawSize / 2.0);

            graphics.blit(RenderPipelines.GUI_TEXTURED, textureId,
                    drawX, drawY, 0, 0, drawSize, drawSize, 128, 128);
        }

        graphics.disableScissor();

        Component zoomText = Component.literal("缩放: " + String.format("%.0f%%", scale * 100));
        graphics.text(this.font, zoomText, 10, this.height - 25, 0xCCCCCC);
    }

    private void drawPlaceholder(GuiGraphicsExtractor graphics, GridMap gm, double gridCenterX, double gridCenterZ) {
        int color = 0xFF000000 | ((gm.gridLeftX * 31 + gm.gridLeftZ * 17 + gm.index * 67) & 0xFFFFFF);
        double relX = gm.gridLeftX + 64 - gridCenterX;
        double relZ = gm.gridLeftZ + 64 - gridCenterZ;
        int drawSize = (int)(128.0 * scale);
        int drawX = (int)(this.width / 2.0 + offsetX + relX * scale - drawSize / 2.0);
        int drawY = (int)(this.height / 2.0 + offsetY + relZ * scale - drawSize / 2.0);
        graphics.fill(drawX, drawY, drawX + drawSize, drawY + drawSize, color);
        graphics.outline(drawX, drawY, drawSize, drawSize, 0xFFFFFFFF);
    }

    @Override
    public void removed() {
        for (DynamicTexture tex : dynamicTextures.values()) tex.close();
        dynamicTextures.clear();
        bakedTextures.clear();
        lastBakedColors.clear();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record GridMap(int index, int gridLeftX, int gridLeftZ) {}
}
