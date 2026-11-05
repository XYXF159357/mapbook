package com.example.mapbook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class MapBookMod implements ModInitializer {

    public static final String MOD_ID = "mapbook";

    public static final Item MAP_BOOK = register("map_book", Item::new, new Item.Properties().stacksTo(1));

    public static <T extends Item> T register(String name, java.util.function.Function<Item.Properties, T> itemFactory, Item.Properties settings) {
        // 26.3 标准写法：先创建 ResourceKey，再用 setId 绑定
        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, name)
        );
        T item = itemFactory.apply(settings.setId(itemKey));
        return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    }

    @Override
    public void onInitialize() {
        // 类名、包路径、方法名均已更新
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(MAP_BOOK));
    }
}
