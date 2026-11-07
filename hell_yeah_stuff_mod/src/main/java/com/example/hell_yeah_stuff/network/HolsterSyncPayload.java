package com.example.hell_yeah_stuff.network;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Сервер -> клиент: содержимое кобуры игрока.
 *
 * Curios не транслирует мутацию дата-компонента «на месте» в стеке пояса,
 * поэтому рендер на бедре питается этим явным синком через
 * {@link com.example.hell_yeah_stuff.client.ClientHolsterData}.
 */
public record HolsterSyncPayload(UUID playerId, ItemStack holstered) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HolsterSyncPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "holster_sync"));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, HolsterSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, HolsterSyncPayload::playerId,
                    ItemStack.OPTIONAL_STREAM_CODEC, HolsterSyncPayload::holstered,
                    HolsterSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
