package com.example.mapbook;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.resources.Identifier;

public record MapBookOpenPayload() implements CustomPayload {

    public static final CustomPayload.Id<MapBookOpenPayload> ID =
            new CustomPayload.Id<>(Identifier.fromNamespaceAndPath(MapBookMod.MOD_ID, "open_book"));

    public static final PacketCodec<RegistryByteBuf, MapBookOpenPayload> CODEC =
            PacketCodec.unit(new MapBookOpenPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
