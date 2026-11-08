package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Дата-компоненты мода. */
public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, HellYeahStuffMod.MODID);

    /**
     * Предмет, убранный в тактическую кобуру. Живёт в стеке самой кобуры,
     * поэтому персистится вместе с ней (Curios сохраняет стеки пояса в NBT).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemStack>> HOLSTERED_ITEM =
            DATA_COMPONENTS.register("holstered_item", () -> DataComponentType.<ItemStack>builder()
                    .persistent(ItemStack.CODEC)
                    .networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC)
                    .build());

    /**
     * «Неизвестная жидкость» в шприце: состав скрыт — подсказка предмета
     * показывает «?» вместо названия зелья (см.
     * {@link com.example.hell_yeah_stuff.item.SyringeEffects#appendTooltip}).
     *
     * <p>Ставится только лутом (лут-таблицы спавнера и вагонетки с сундуком),
     * поэтому компонент персистентный и синхронизируется на клиент: иначе
     * подсказку нечем было бы отрисовать.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> UNKNOWN_CONTENTS =
            DATA_COMPONENTS.register("unknown_contents", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    private ModDataComponents() {}
}
