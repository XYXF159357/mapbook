package com.example.mapbook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class MapBookMod implements ModInitializer {

    public static final String MOD_ID = "mapbook";

    // 数据组件：存储地图列表
    public static final DataComponentType<List<ItemStack>> MAP_LIST = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "map_list"),
            DataComponentType.<List<ItemStack>>builder()
                    .persistent(ItemStack.CODEC.listOf())
                    .build()
    );

    // 物品注册
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
        // 【新增 1】注册 S2C 网络包
        PayloadTypeRegistry.clientboundPlay().register(MapBookOpenPayload.ID, MapBookOpenPayload.CODEC);

        // 加入创造模式物品栏
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(MAP_BOOK));

        // 右键事件
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack bookStack = player.getItemInHand(hand);
            if (!bookStack.is(MAP_BOOK)) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide()) {
                // 【新增 2】安全地获取 ServerPlayer 实例
                if (!(player instanceof ServerPlayer serverPlayer)) {
                    return InteractionResult.PASS;
                }

                ItemStack offhand = player.getOffhandItem();

                if (offhand.is(Items.FILLED_MAP)) {
                    // 存图分支（保持不变）
                    List<ItemStack> maps = bookStack.get(MAP_LIST);
                    if (maps == null) {
                        maps = new ArrayList<>();
                    }
                    maps.add(offhand.copy());
                    bookStack.set(MAP_LIST, maps);
                    offhand.shrink(1);
                    player.sendSystemMessage(Component.translatable("item.mapbook.map_book.stored"));
                } else {
                    // 【新增 3】副手没有地图 → 打开 GUI 或提示空册
                    List<ItemStack> stored = bookStack.get(MAP_LIST);
                    if (stored != null && !stored.isEmpty()) {
                        ServerPlayNetworking.send(serverPlayer, new MapBookOpenPayload());
                    } else {
                        player.sendSystemMessage(Component.translatable("item.mapbook.map_book.empty"));
                    }
                }
            }
            return InteractionResult.SUCCESS;
        });
    }
}
