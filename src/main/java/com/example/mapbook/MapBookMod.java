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
    public void onInitialize(import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// 在 onInitialize() 方法内部添加：
UseItemCallback.EVENT.register((player, level, hand) -> {
    ItemStack stack = player.getItemInHand(hand);
    if (!stack.is(MapBookMod.MAP_BOOK)) {
        return InteractionResult.PASS; // 不是地图册，放行
    }

    // 只在服务端执行给予逻辑，避免重复
    if (!level.isClientSide()) {
        player.addItem(new ItemStack(Items.MAP));
        player.sendSystemMessage(
            Component.translatable("item.mapbook.map_book.used")
        );
    }

    // 返回 SUCCESS 让客户端播放挥手动画，同时服务端完成给予
    return InteractionResult.SUCCESS;
});) {
        // 类名、包路径、方法名均已更新
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(MAP_BOOK));
    }
}
