package com.example.mapbook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.List;

public class MapBookMod implements ModInitializer {

    public static final String MOD_ID = "mapbook";

    public static final DataComponentType<List<ItemStack>> MAP_LIST = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "map_list"),
            DataComponentType.<List<ItemStack>>builder()
                    .persistent(ItemStack.CODEC.listOf())
                    .build()
    );

    public static final Item MAP_BOOK = register("map_book", Item::new,
            new Item.Properties()
                    .stacksTo(1)
                    .component(MAP_LIST, new ArrayList<>())
    );

    public static <T extends Item> T register(String name, java.util.function.Function<Item.Properties, T> itemFactory, Item.Properties settings) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name));
        T item = itemFactory.apply(settings.setId(itemKey));
        return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(MapBookOpenPayload.ID, MapBookOpenPayload.CODEC);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(MAP_BOOK));

        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack bookStack = player.getItemInHand(hand);
            if (!bookStack.is(MAP_BOOK)) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide()) {
                if (!(player instanceof ServerPlayer serverPlayer)) {
                    return InteractionResult.PASS;
                }

                ItemStack offhand = player.getOffhandItem();
                MinecraftServer server = serverPlayer.level().getServer();
                if (server == null) return InteractionResult.PASS;

                if (offhand.is(Items.FILLED_MAP)) {
                    // 获取副手地图的 MapId
                    MapId newMapId = offhand.get(DataComponents.MAP_ID);
                    if (newMapId == null) {
                        player.sendSystemMessage(Component.translatable("item.mapbook.map_book.invalid"));
                        return InteractionResult.SUCCESS;
                    }

                    // 在所有维度中查找地图数据
                    MapItemSavedData newData = findMapDataAnywhere(newMapId, server);
                    if (newData == null) {
                        player.sendSystemMessage(Component.translatable("item.mapbook.map_book.invalid"));
                        return InteractionResult.SUCCESS;
                    }

                    // 条件 1：必须是默认大小（scale == 0）
                    if (newData.scale != 0) {
                        player.sendSystemMessage(Component.translatable("item.mapbook.map_book.wrong_scale"));
                        return InteractionResult.SUCCESS;
                    }

                    List<ItemStack> maps = bookStack.get(MAP_LIST);
                    if (maps == null) maps = new ArrayList<>();

                    // 条件 2：不能重复
                    for (ItemStack stored : maps) {
                        MapId storedId = stored.get(DataComponents.MAP_ID);
                        if (newMapId.equals(storedId)) {
                            player.sendSystemMessage(Component.translatable("item.mapbook.map_book.duplicate"));
                            return InteractionResult.SUCCESS;
                        }
                    }

                    // 条件 3：必须与已存地图同维度
                    if (!maps.isEmpty()) {
                        MapId firstId = maps.get(0).get(DataComponents.MAP_ID);
                        MapItemSavedData firstData = firstId == null ? null : findMapDataAnywhere(firstId, server);
                        if (firstData != null && !newData.dimension.equals(firstData.dimension)) {
                            player.sendSystemMessage(Component.translatable("item.mapbook.map_book.different_dimension"));
                            return InteractionResult.SUCCESS;
                        }
                    }

                    maps.add(offhand.copy());
                    bookStack.set(MAP_LIST, maps);
                    offhand.shrink(1);
                    player.sendSystemMessage(Component.translatable("item.mapbook.map_book.stored"));
                } else {
                    // 副手没有地图 → 打开 GUI
                    List<ItemStack> stored = bookStack.get(MAP_LIST);
                    if (stored != null && !stored.isEmpty()) {
                        ServerPlayNetworking.send(serverPlayer, new MapBookOpenPayload(stored));
                    } else {
                        player.sendSystemMessage(Component.translatable("item.mapbook.map_book.empty"));
                    }
                }
            }
            return InteractionResult.SUCCESS;
        });
    }

    /** 在所有已加载维度中查找某张地图的数据 */
    private static MapItemSavedData findMapDataAnywhere(MapId mapId, MinecraftServer server) {
        for (ServerLevel lvl : server.getAllLevels()) {
            MapItemSavedData data = MapItem.getSavedData(mapId, lvl);
            if (data != null) return data;
        }
        return null;
    }
}
