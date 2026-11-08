package com.example.hell_yeah_stuff.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Универсальный триггер «игрок сделал вот это» — для достижений, у которых
 * условие целиком проверяется кодом, а не данными.
 *
 * <p>Ванильный {@code PlayerTrigger} для модовых событий не подходит: его
 * экземпляр зарегистрирован в {@code CriteriaTriggers} под конкретными id
 * ({@code minecraft:tick}, {@code minecraft:slept_in_bed} и т.п.), и вызвать
 * его «от себя» нельзя — {@code trigger()} запускает слушателей ВСЕХ
 * достижений этого триггера.
 *
 * <p>Поэтому каждый модовый триггер — отдельный экземпляр этого класса со
 * своим id в реестре {@code minecraft:trigger_type} (см.
 * {@link com.example.hell_yeah_stuff.registry.ModCriteria}). В JSON достижения
 * он выглядит как обычный критерий:
 *
 * <pre>
 * "criteria": {
 *   "injected": { "trigger": "hell_yeah_stuff:unknown_injection" }
 * }
 * </pre>
 *
 * <p>Поле {@code player} в условиях необязательное — как у ванильных триггеров
 * этого вида: без него достижение засчитывается любому игроку.
 */
public class ModTrigger extends SimpleCriterionTrigger<ModTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /** Засчитать событие игроку. Вызывать только на сервере. */
    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    /** Условия критерия: только необязательный предикат игрока. */
    public record TriggerInstance(Optional<ContextAwarePredicate> player)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                                        .forGetter(TriggerInstance::player))
                        .apply(instance, TriggerInstance::new));
    }
}
