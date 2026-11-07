package com.example.hell_yeah_stuff.network;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.client.ClientHolsterData;
import com.example.hell_yeah_stuff.event.DashTrailHandler;
import com.example.hell_yeah_stuff.event.HolsterHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Регистрация сетевых пакетов оставшегося контента мода. */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID)
public final class ModNetwork {

    @SubscribeEvent
    static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(DashPayload.TYPE, DashPayload.STREAM_CODEC, ModNetwork::handleDash);
        registrar.playToServer(HolsterPayload.TYPE, HolsterPayload.STREAM_CODEC, ModNetwork::handleHolster);
        registrar.playToClient(HolsterSyncPayload.TYPE, HolsterSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientHolsterData.update(payload.playerId(), payload.holstered())));
    }

    private static void handleDash(DashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DashTrailHandler.tryServerDash(player);
            }
        });
    }

    private static void handleHolster(HolsterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                HolsterHandler.tryServerHolster(player);
            }
        });
    }

    private ModNetwork() {}
}
