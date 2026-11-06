package com.example.mapbook;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MapBookOpenPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MapBookOpenPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MapBookMod.MOD_ID, "open_book"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MapBookOpenPayload> CODEC =
            StreamCodec.unit(new MapBookOpenPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
