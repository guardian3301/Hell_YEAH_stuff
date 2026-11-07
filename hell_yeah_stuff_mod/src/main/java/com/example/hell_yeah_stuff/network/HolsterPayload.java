package com.example.hell_yeah_stuff.network;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Клиент -> сервер: «нажал бинд кобуры (V)».
 *
 * Пакет пустой: сервер сам смотрит, что в главной руке и в кобуре
 * в слоте ремня, и выполняет положить/достать/поменять
 * (см. {@link com.example.hell_yeah_stuff.event.HolsterHandler#tryServerHolster}).
 */
public record HolsterPayload() implements CustomPacketPayload {

    public static final HolsterPayload INSTANCE = new HolsterPayload();

    public static final CustomPacketPayload.Type<HolsterPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "holster_swap"));

    public static final StreamCodec<ByteBuf, HolsterPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
