package com.example.mapbook.client;

import com.example.mapbook.MapBookMod;
import com.example.mapbook.MapBookOpenPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class MapBookClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(MapBookOpenPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.player() == null) return;
                ItemStack held = context.player().getMainHandItem();
                if (!held.is(MapBookMod.MAP_BOOK)) return;

                List<ItemStack> maps = held.get(MapBookMod.MAP_LIST);
                if (maps == null || maps.isEmpty()) return;

                context.client().gui.setScreen(new MapBookScreen(maps));
            });
        });
    }
}
