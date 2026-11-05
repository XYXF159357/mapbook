package com.example.mapbook;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class MapBookItem extends Item {

    public MapBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            player.addItem(new ItemStack(Items.MAP));
            player.sendSystemMessage(Component.translatable("item.mapbook.map_book.used"));
            return InteractionResult.CONSUME; // 服务端：消耗交互
        }
        return InteractionResult.SUCCESS; // 客户端：播放动画
    }
}
