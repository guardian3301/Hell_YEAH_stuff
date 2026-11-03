package com.example.hell_yeah_stuff.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Общая «кровь в шприце» для боевого шприца и шприц-болта.
 *
 * <p>Эффект живёт в ванильном компоненте {@link DataComponents#POTION_CONTENTS}
 * ({@link PotionContents}), поэтому цвет слоя жидкости, подсказка и длительность
 * считаются готовым ванильным кодом — ровно как у обычного зелья.
 *
 * <p>Цикл один и тот же у обоих предметов:
 * <ol>
 *   <li>игрок колет себя (ПКМ) — теряет 1 сердце;</li>
 *   <li>из его активных эффектов забирается доля {@code transfer} (уровень и
 *       длительность сохраняются);</li>
 *   <li>остаток {@code 1 - transfer} остаётся в крови игрока
 *       (шприц забирает 100%, болт — 75%, значит 25% остаётся);</li>
 *   <li>эффект лежит в предмете и передаётся цели ударом (шприц) или
 *       попаданием болта.</li>
 * </ol>
 *
 * <p>Второй, более быстрый путь — заливка из зелья: если во второй руке лежит
 * зелье, ПКМ по шприцу переносит эффект зелья целиком (уровни и длительности
 * как есть), а бутылка опустошается ({@link #potionContents} /
 * {@link #consumePotion}).
 *
 * <p>Мгновенные эффекты (зелья исцеления/вреда) переносить нельзя: у них нет
 * длительности — они пропускаются.
 */
public final class SyringeEffects {

    /** Шприц забирает эффект целиком. */
    public static final float FULL_TRANSFER = 1.0F;
    /** Шприц-болт забирает 75%, 25% остаётся игроку. */
    public static final float BOLT_TRANSFER = 1.0F;

    /** Цвет «нет жидкости» — слой не тонируется. */
    public static final int NO_COLOR = -1;

    /** Эффект, наложенный шприцем. */
    public static PotionContents get(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    }

    /** В предмете есть переносимый эффект. */
    public static boolean isCharged(ItemStack stack) {
        return get(stack).hasEffects();
    }

    /** Положить эффект в предмет (пустой {@link PotionContents#EMPTY} — разрядить). */
    public static void set(ItemStack stack, PotionContents contents) {
        stack.set(DataComponents.POTION_CONTENTS, contents);
    }

    /** Разрядить предмет после передачи эффекта. */
    public static void clear(ItemStack stack) {
        stack.remove(DataComponents.POTION_CONTENTS);
    }

    /** Цвет жидкости для тонировки слоя: {@link #NO_COLOR}, если эффекта нет. */
    public static int color(ItemStack stack) {
        return colorOf(get(stack));
    }

    /** Цвет конкретного набора эффектов (усреднение ванильным кодом). */
    public static int colorOf(PotionContents contents) {
        return contents.hasEffects() ? contents.getColor() : NO_COLOR;
    }

    /** Есть ли у сущности что забирать (мгновенные эффекты не в счёт). */
    public static boolean hasTransferable(LivingEntity entity) {
        for (MobEffectInstance active : entity.getActiveEffects()) {
            if (!active.getEffect().value().isInstantenous()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Забирает эффекты из {@code source}: в предмет уходит доля {@code transfer}
     * длительности, в крови остаётся {@code 1 - transfer}.
     *
     * <p>Длительность {@link MobEffectInstance#INFINITE_DURATION} (-1) не
     * масштабируется — бесконечный эффект остаётся бесконечным.
     */
    public static PotionContents extract(LivingEntity source, float transfer) {
        // Снимок обязателен: ниже мы меняем activeEffects во время обхода.
        List<MobEffectInstance> active = List.copyOf(source.getActiveEffects());
        List<MobEffectInstance> stored = new ArrayList<>();
        float remainderShare = 1.0F - transfer;
        for (MobEffectInstance instance : active) {
            if (instance.getEffect().value().isInstantenous()) {
                continue;
            }
            int duration = instance.getDuration();
            int storedDuration = duration < 0 ? MobEffectInstance.INFINITE_DURATION
                    : Math.max(1, Mth.floor(duration * transfer));
            stored.add(new MobEffectInstance(instance.getEffect(), storedDuration,
                    instance.getAmplifier(), instance.isAmbient(), instance.isVisible(), instance.showIcon()));

            // Эффект пересоздаётся, а не правится на месте: у MobEffectInstance
            // нет публичного сеттера длительности, а remove + add корректно
            // обновляет и сервер, и клиент.
            source.removeEffect(instance.getEffect());
            int remainder;
            if (duration < 0) {
                // Бесконечный эффект не делится: остаток тоже бесконечен,
                // поэтому он возвращается владельцу как был.
                remainder = MobEffectInstance.INFINITE_DURATION;
            } else {
                remainder = Mth.floor(duration * remainderShare);
            }
            if (remainder > 0 || remainder == MobEffectInstance.INFINITE_DURATION) {
                source.addEffect(new MobEffectInstance(instance.getEffect(), remainder,
                        instance.getAmplifier(), instance.isAmbient(), instance.isVisible(), instance.showIcon()));
            }
        }
        return stored.isEmpty()
                ? PotionContents.EMPTY
                : new PotionContents(Optional.empty(), Optional.empty(), List.copyOf(stored));
    }

    /**
     * Накладывает сохранённый эффект на цель — с тем же уровнем и той же
     * длительностью, что лежат в предмете.
     */
    public static void apply(ItemStack stack, LivingEntity target, @Nullable Entity source) {
        PotionContents contents = get(stack);
        if (!contents.hasEffects()) {
            return;
        }
        Entity effectSource = source != null ? source : target;
        for (MobEffectInstance instance : contents.getAllEffects()) {
            target.addEffect(new MobEffectInstance(instance), effectSource);
        }
        burst(target.level(), target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D),
                contents.getColor());
    }

    /** Облако частиц цвета жидкости — цвет тот же, что у слоя и у подсказки. */
    public static void burst(Level level, Vec3 pos, int color) {
        if (color == NO_COLOR || !(level instanceof ServerLevel server)) {
            return;
        }
        server.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color),
                pos.x, pos.y, pos.z, 12, 0.25D, 0.35D, 0.25D, 0.02D);
    }

    /** Подсказка по эффекту внутри предмета (как у зелья). */
    public static void appendTooltip(ItemStack stack, Item.TooltipContext context,
                                     List<Component> tooltip, TooltipFlag flag) {
        PotionContents contents = get(stack);
        if (contents.hasEffects()) {
            contents.addPotionTooltip(tooltip::add, 1.0F, context.tickRate());
        }
    }

    // ------------------------------------------------------------------
    // Заливка из зелья (вторая рука)
    // ------------------------------------------------------------------

    /** Рука, противоположная указанной. */
    public static InteractionHand opposite(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    /** Предмет — зелье, из которого можно перелить эффект в шприц. */
    public static boolean isPotion(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    /**
     * Эффект зелья в том виде, в каком он переедет в шприц: уровни и
     * длительности как есть, мгновенные эффекты отброшены (их нельзя «носить»
     * в предмете). Цвет зелья сохраняется, чтобы жидкость в шприце была того
     * же оттенка, что и у исходного зелья.
     */
    public static PotionContents potionContents(ItemStack stack) {
        if (!isPotion(stack)) {
            return PotionContents.EMPTY;
        }
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        List<MobEffectInstance> kept = new ArrayList<>();
        for (MobEffectInstance instance : contents.getAllEffects()) {
            if (instance.getEffect().value().isInstantenous()) {
                continue;
            }
            kept.add(new MobEffectInstance(instance));
        }
        if (kept.isEmpty()) {
            return PotionContents.EMPTY;
        }
        // Список эффектов разворачивается в custom_effects: если оставить
        // holder зелья, getAllEffects() вернул бы эффекты дважды.
        return new PotionContents(Optional.empty(), contents.customColor(), List.copyOf(kept));
    }

    /** В другой руке лежит зелье, из которого есть что залить. */
    public static boolean canFillFromPotion(LivingEntity entity, InteractionHand hand) {
        ItemStack other = entity.getItemInHand(opposite(hand));
        return isPotion(other) && potionContents(other).hasEffects();
    }

    /**
     * Переливает зелье из указанной руки в шприц: эффект возвращается,
     * бутылка становится пустой — ровно как после выпитого зелья.
     *
     * <p>Если в руке не зелье или в нём нет переносимых эффектов, вернётся
     * {@link PotionContents#EMPTY}, а предмет останется нетронутым: вызывающий
     * код может спокойно откатиться на «укол себе».
     */
    public static PotionContents consumePotion(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        PotionContents contents = potionContents(stack);
        if (!contents.hasEffects()) {
            return PotionContents.EMPTY;
        }
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
        return contents;
    }

    /** Предмет — шприц или шприц-болт. */
    public static boolean isSyringe(ItemStack stack) {
        return stack.getItem() instanceof BattleSyringeItem || stack.getItem() instanceof SyringeBoltItem;
    }

    private SyringeEffects() {}
}
