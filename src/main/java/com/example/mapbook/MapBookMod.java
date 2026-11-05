package com.example.mapbook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class MapBookMod implements ModInitializer {

    public static final String MOD_ID = "mapbook";

    // 在 26.3 中，物品注册必须带上 useItemPrefixedTranslationKey() 和 registryKey
    public static final Item MAP_BOOK = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "map_book"),
            new MapBookItem(new Item.Properties()
                    .stacksTo(1)
                    .useItemPrefixedTranslationKey()
                    .registryKey(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.ITEM,
                            ResourceLocation.fromNamespaceAndPath(MOD_ID, "map_book")
                    ))
            )
    );

    @Override
    public void onInitialize() {
        // 把物品加入创造模式物品栏的“工具与实用物品”页
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(MAP_BOOK));
    }
}
