package com.example.mapbook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
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
import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.List;

public class MapBookMod implements ModInitializer {

    public static final String MOD_ID = "mapbook";

    // 1. 注册自定义数据组件：用于存储地图列表
    public static final DataComponentType<List<ItemStack>> MAP_LIST = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "map_list"),
            DataComponentType.<List<ItemStack>>builder()
                    .persistent(ItemStack.CODEC.listOf()) // 持久化到存档
                    .build()
    );

    // 2. 创建物品时，挂载组件并给一个空的 ArrayList 作为默认值
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
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(MAP_BOOK));

        // 3. 右键事件：把副手的已填充地图存入地图册
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack bookStack = player.getItemInHand(hand);
            if (!bookStack.is(MAP_BOOK)) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide()) {
                ItemStack offhand = player.getOffhandItem();
                if (offhand.is(Items.FILLED_MAP)) {
                    List<ItemStack> maps = bookStack.get(MAP_LIST);
                    if (maps == null) {
                        maps = new ArrayList<>();
                    }
                    maps.add(offhand.copy());
                    bookStack.set(MAP_LIST, maps);
                    offhand.shrink(1);
                    player.sendSystemMessage(Component.translatable("item.mapbook.map_book.stored"));
                } else {
                    player.sendSystemMessage(Component.translatable("item.mapbook.map_book.need_map"));
                }
            }
            return InteractionResult.SUCCESS;
        });
    }
}
