package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.loot.UnknownSyringeContentsFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Loot-функции мода. */
public final class ModLootFunctions {

    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, HellYeahStuffMod.MODID);

    /**
     * Содержимое «неизвестного» шприца из лута: тошнота 30 с + случайный
     * эффект 15 с + подмешанный цвет жидкости.
     *
     * <p>Ванильными функциями такое не собрать: {@code minecraft:set_potion}
     * умеет только заранее заданное зелье, а эффект должен выбираться при
     * генерации лута — как у загадочного рагу.
     */
    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<UnknownSyringeContentsFunction>>
            UNKNOWN_SYRINGE_CONTENTS = LOOT_FUNCTIONS.register("unknown_syringe_contents",
                    () -> new LootItemFunctionType<>(UnknownSyringeContentsFunction.CODEC));

    private ModLootFunctions() {}
}
