package com.example.hell_yeah_stuff.loot;

import com.example.hell_yeah_stuff.item.SyringeEffects;
import com.example.hell_yeah_stuff.registry.ModLootFunctions;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.FastColor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Optional;

/**
 * Содержимое «неизвестного» шприца, выпадающего из спавнера и вагонетки
 * с сундуком.
 *
 * <p>В шприц кладутся ровно два эффекта:
 * <ol>
 *   <li><b>тошнота</b> — {@value #NAUSEA_DURATION} тиков (30 с), она же служит
 *       «якорем» состава и видна всегда;</li>
 *   <li><b>случайный эффект</b> — {@value #RANDOM_DURATION} тиков (15 с),
 *       выбирается при генерации лута из всех зарегистрированных эффектов,
 *       как у загадочного рагу ({@code minecraft:suspicious_stew}).</li>
 * </ol>
 *
 * <p>Цвет жидкости — среднее из трёх цветов: тошноты, «загадочного» лилового
 * {@link #MYSTERY_COLOR} и выпавшего эффекта. Так слой жидкости в модели
 * ({@code battle_syringe_content}) не повторяет цвет эффекта один в один,
 * но остаётся «живым» оттенком.
 *
 * <p>Мгновенные эффекты (исцеление/вред) в пул не попадают: у них нет
 * длительности, носить их в предмете нельзя. Тошнота из пула исключена —
 * иначе состав схлопнулся бы в один эффект вместо двух.
 */
public class UnknownSyringeContentsFunction extends LootItemConditionalFunction {

    /** Длительность случайного эффекта — 15 с. */
    public static final int RANDOM_DURATION = 15 * 20;
    /** Длительность тошноты — 30 с. */
    public static final int NAUSEA_DURATION = 30 * 20;
    /** Третий цвет в смеси, помимо тошноты и выпавшего эффекта. */
    public static final int MYSTERY_COLOR = 0x8E7AB3;

    public static final MapCodec<UnknownSyringeContentsFunction> CODEC =
            RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
                    .apply(instance, UnknownSyringeContentsFunction::new));

    private UnknownSyringeContentsFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return ModLootFunctions.UNKNOWN_SYRINGE_CONTENTS.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        Registry<MobEffect> effects =
                context.getLevel().registryAccess().registryOrThrow(Registries.MOB_EFFECT);
        // Снимок пула: holders() — поток по реестру, а нам нужен индекс.
        List<Holder.Reference<MobEffect>> pool = effects.holders()
                .filter(holder -> !holder.value().isInstantenous())
                .filter(holder -> !holder.is(MobEffects.CONFUSION))
                .toList();
        if (pool.isEmpty()) {
            return stack;
        }
        Holder<MobEffect> random = pool.get(context.getRandom().nextInt(pool.size()));
        MobEffectInstance randomEffect = new MobEffectInstance(random, RANDOM_DURATION);
        MobEffectInstance nausea = new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_DURATION);
        int color = blend(MobEffects.CONFUSION.value().getColor(), MYSTERY_COLOR,
                random.value().getColor());

        stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Optional.empty(), Optional.of(color),
                        List.of(randomEffect, nausea)));
        // Сиреневая жидкость остаётся «неизвестной»: подсказка показывает
        // маскировку вместо названий эффектов (SyringeEffects#appendTooltip).
        SyringeEffects.setUnknown(stack, true);
        return stack;
    }

    /**
     * Среднее по каналам — так же усредняет цвета ванильный {@link PotionContents#getColor()}.
     *
     * <p>Результат оборачивается в {@link FastColor.ARGB32#color(int, int, int)},
     * чтобы в NBT лежал непрозрачный ARGB. Иначе alpha осталась бы нулевой, и
     * {@code ItemRenderer} нарисовал бы слой жидкости полностью прозрачным —
     * он берёт альфа-канал именно из цвета тинта.
     */
    private static int blend(int first, int second, int third) {
        int red = ((first >> 16 & 0xFF) + (second >> 16 & 0xFF) + (third >> 16 & 0xFF)) / 3;
        int green = ((first >> 8 & 0xFF) + (second >> 8 & 0xFF) + (third >> 8 & 0xFF)) / 3;
        int blue = ((first & 0xFF) + (second & 0xFF) + (third & 0xFF)) / 3;
        return FastColor.ARGB32.color(red, green, blue);
    }
}
