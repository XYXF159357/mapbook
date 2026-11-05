package com.example.mapbook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MapBookMod implements ModInitializer {

    public static final String MOD_ID = "mapbook";

    public static final Item MAP_BOOK = register("map_book", Item::new, new Item.Properties().stacksTo(1));

    public static <T extends Item> T register(String name, java.util.function.Function<Item.Properties, T> itemFactory, Item.Properties settings) {
        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, name)
        );
        T item = itemFactory.apply(settings.setId(itemKey));
        return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    }

    @Override
    public void onInitialize() {
        // 加入创造模式物品栏
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(MAP_BOOK));

        // 右键使用物品的事件回调
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(MAP_BOOK)) {
                return InteractionResult.PASS;
            }

            System.out.println("!!! UseItemCallback fired, side: " + (level.isClientSide() ? "CLIENT" : "SERVER"));

            if (!level.isClientSide()) {
                player.addItem(new ItemStack(Items.MAP));
                player.sendSystemMessage(Component.translatable("item.mapbook.map_book.used"));
            }

            return InteractionResult.SUCCESS;
        });
    }
}
