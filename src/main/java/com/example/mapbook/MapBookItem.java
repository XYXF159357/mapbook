package com.example.mapbook;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

public class MapBookItem extends Item {

    public MapBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            // 给予一张空地图；这是基础版本，后续你可以改成打开容器或给予已填充地图
            player.addItem(new ItemStack(Items.MAP));
            player.sendSystemMessage(Component.translatable("item.mapbook.map_book.used"));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
