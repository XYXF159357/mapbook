package com.example.mapbook.client;

import com.example.mapbook.MapBookMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
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

    // 视角偏移与缩放
    private double offsetX = 0;
    private double offsetY = 0;
    private double scale = 1.0;

    // 拖动状态
    private boolean dragging = false;
    private double dragStartX;
    private double dragStartY;
    private double dragStartOffsetX;
    private double dragStartOffsetY;

    // 按钮
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

    // ---------- 拖动 ----------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            dragging = true;
            dragStartX = mouseX;
            dragStartY = mouseY;
            dragStartOffsetX = offsetX;
            dragStartOffsetY = offsetY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            offsetX = dragStartOffsetX + (mouseX - dragStartX);
            offsetY = dragStartOffsetY + (mouseY - dragStartY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ---------- 渲染 ----------

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // 标题
        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 10, 0xFFFFFF);

        if (maps.isEmpty()) {
            Component empty = Component.translatable("screen.mapbook.empty");
            graphics.text(this.font, empty, this.width / 2 - this.font.width(empty) / 2,
                    this.height / 2, 0xAAAAAA);
            return;
        }

        // 收集所有地图数据，并计算世界坐标范围
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

        // 世界中心
        double worldCenterX = (minX + maxX) / 2.0;
        double worldCenterZ = (minZ + maxZ) / 2.0;

        // 每格世界坐标对应的屏幕像素（基准值，再乘缩放）
        double basePixelsPerBlock = 1.0;

        // 启用裁剪，防止地图画出 GUI 边界
        graphics.enableScissor(0, 25, this.width, this.height - 40);

        graphics.pose().pushMatrix();
        // 以屏幕中心为原点，应用拖动偏移和缩放
        graphics.pose().translate((float)(this.width / 2 + offsetX), (float)(this.height / 2 + offsetY));
        graphics.pose().scale((float) scale, (float) scale);

        for (MapRenderEntry entry : entries) {
            MapItemSavedData data = entry.data;

            // 地图中心相对世界中心的偏移（世界格）
            double relX = data.centerX - worldCenterX;
            double relZ = data.centerZ - worldCenterZ;

            // 转为屏幕坐标（地图默认边长 128 格）
            double halfSizePx = 64 * basePixelsPerBlock;
            double centerPxX = relX * basePixelsPerBlock;
            double centerPxY = relZ * basePixelsPerBlock; // Z 正方向向下

            int drawX = (int)(centerPxX - halfSizePx);
            int drawY = (int)(centerPxY - halfSizePx);
            int sizePx = (int)(128 * basePixelsPerBlock);

            // 占位方块：用地图 ID 的哈希生成不同颜色
            int color = 0xFF000000 | (data.centerX * 31 + data.centerZ * 17) & 0xFFFFFF;
            graphics.fill(drawX, drawY, drawX + sizePx, drawY + sizePx, color);
            // 边框
            graphics.outline(drawX, drawY, sizePx, sizePx, 0xFFFFFFFF);
        }

        graphics.pose().popMatrix();
        graphics.disableScissor();

        // 缩放提示
        Component zoomText = Component.literal("缩放: " + String.format("%.0f%%", scale * 100));
        graphics.text(this.font, zoomText, 10, this.height - 25, 0xCCCCCC);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record MapRenderEntry(MapItemSavedData data) {}
}
