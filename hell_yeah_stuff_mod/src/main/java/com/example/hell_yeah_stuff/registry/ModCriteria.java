package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.advancement.ModTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Триггеры достижений мода.
 *
 * <p>Реестр {@code minecraft:trigger_type} — ванильный, но NeoForge разрешает
 * в него писать: {@code DeferredRegister} для ванильных реестров
 * регистрируется на общем {@code RegisterEvent}.
 *
 * <p>Сами достижения лежат в данных
 * ({@code data/hell_yeah_stuff/advancement/*.json}) и ссылаются на эти id.
 */
public final class ModCriteria {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, HellYeahStuffMod.MODID);

    /**
     * Игрок вколол себе содержимое шприца с пометкой «неизвестная жидкость»
     * (шприц из лута спавнера или вагонетки с сундуком).
     */
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> UNKNOWN_INJECTION =
            TRIGGERS.register("unknown_injection", ModTrigger::new);

    /**
     * Игрок наполнил шприц кровью после поедания рыбы-ежа: в шприце оказались
     * отравление, голод и тошнота одновременно.
     */
    public static final DeferredHolder<CriterionTrigger<?>, ModTrigger> PUFFERFISH_SYRINGE =
            TRIGGERS.register("pufferfish_syringe", ModTrigger::new);

    /**
     * Засчитать событие игроку.
     *
     * <p>Триггеры достижений работают только на сервере: у клиента нет
     * {@code PlayerAdvancements}, поэтому вызов оттуда просто ничего не делает
     * (проверка через {@code instanceof ServerPlayer}).
     */
    public static void trigger(DeferredHolder<CriterionTrigger<?>, ModTrigger> trigger, Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            trigger.get().trigger(serverPlayer);
        }
    }

    private ModCriteria() {}
}
