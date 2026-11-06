package com.example.mapbook;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record MapBookOpenPayload(List<ItemStack> maps) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MapBookOpenPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MapBookMod.MOD_ID, "open_book"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MapBookOpenPayload> CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), MapBookOpenPayload::maps,
                    MapBookOpenPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
