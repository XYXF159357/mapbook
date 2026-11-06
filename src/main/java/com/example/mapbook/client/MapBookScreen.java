package com.example.mapbook.client;

import com.example.mapbook.MapBookMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;

import java.util.List;

public class MapBookScreen extends Screen {

    private final List<ItemStack> maps;
    private int currentPage = 0;
    private Button prevButton;
    private Button nextButton;

    public MapBookScreen(List<ItemStack> maps) {
        super(Component.translatable("screen.mapbook.title"));
        this.maps = maps;
    }

    @Override
    protected void init() {
        super.init();
        int buttonY = this.height - 40;

        prevButton = Button.builder(Component.literal("<"), b -> {
            if (currentPage > 0) {
                currentPage--;
                refreshButtons();
            }
        }).bounds(this.width / 2 - 60, buttonY, 40, 20).build();

        nextButton = Button.builder(Component.literal(">"), b -> {
            if (currentPage < maps.size() - 1) {
                currentPage++;
                refreshButtons();
            }
        }).bounds(this.width / 2 + 20, buttonY, 40, 20).build();

        addRenderableWidget(prevButton);
        addRenderableWidget(nextButton);
        refreshButtons();
    }

    private void refreshButtons() {
        if (prevButton != null) prevButton.active = currentPage > 0;
        if (nextButton != null) nextButton.active = currentPage < maps.size() - 1;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

       // 标题
     graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 20, 0xFFFFFF);

        if (maps.isEmpty()) {
            // 空册提示
graphics.text(this.font, Component.translatable("screen.mapbook.empty"),
        this.width / 2 - this.font.width(Component.translatable("screen.mapbook.empty")) / 2,
        this.height / 2, 0xAAAAAA);
            return;
        }

        ItemStack currentMap = maps.get(currentPage);
        if (!currentMap.is(Items.FILLED_MAP)) return;

        MapId mapId = currentMap.get(DataComponents.MAP_ID);
        if (mapId == null || this.minecraft == null || this.minecraft.level == null) return;

        MapItemSavedData mapData = this.minecraft.level.getMapData(mapId);
        if (mapData == null) return;

        int size = 160;
        int x = (this.width - size) / 2;
        int y = (this.height - size) / 2 - 20;

        // 页码
Component pageText = Component.literal((currentPage + 1) + " / " + maps.size());
graphics.text(this.font, pageText, this.width / 2 - this.font.width(pageText) / 2,
        this.height - 20, 0xCCCCCC);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
