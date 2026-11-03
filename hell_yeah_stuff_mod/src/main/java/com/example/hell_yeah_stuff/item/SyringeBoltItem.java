package com.example.hell_yeah_stuff.item;

import com.example.hell_yeah_stuff.entity.SyringeBoltEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Шприц-болт — боеприпас рельсового арбалета с эффектом внутри.
 *
 * <p>ПКМ — укол себе: игрок теряет 1 сердце, отыгрывается арбалетная анимация
 * ({@link com.example.hell_yeah_stuff.client.SyringeArmAnimator} — только рука
 * с предметом), после чего в ОДИН
 * болт из стака переезжает 75% длительности эффекта игрока (уровень сохраняется),
 * а 25% остаётся в крови.
 *
 * <p>Заряженный болт заряжается в рельсовый арбалет и при попадании отдаёт
 * эффект цели в том объёме, который в нём лежит, то есть 75% от исходного.
 */
public class SyringeBoltItem extends ArrowItem {

    /** Длительность «укола» в тиках. */
    public static final int INJECT_DURATION = 20;
    /** Урон себе при уколе — 1 сердце. */
    public static final float SELF_DAMAGE = 2.0F;

    public SyringeBoltItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        return new SyringeBoltEntity(level, shooter, ammo.copyWithCount(1), weapon);
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        SyringeBoltEntity bolt = new SyringeBoltEntity(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1));
        bolt.pickup = AbstractArrow.Pickup.ALLOWED;
        return bolt;
    }

    /**
     * Своей ванильной анимации у предмета нет: позу «натянутого арбалета»
     * ставит {@link SyringeArmAnimator} только держащей руке.
     *
     * <p>Именно поэтому здесь {@code NONE}, а не {@code CROSSBOW}:
     * {@code UseAnim.CROSSBOW} заставил бы {@code PlayerRenderer} выставить
     * двуручную позу {@code CROSSBOW_CHARGE}, которая поднимает ОБЕ руки.
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return INJECT_DURATION;
    }

    /**
     * ПКМ: укол себе. Игрок сразу теряет 1 сердце, дальше отыгрывается
     * арбалетная анимация, по её окончании один болт из стака заряжается
     * эффектом.
     *
     * <p>Если во второй руке лежит зелье, режим другой — заливка: сердце
     * не тратится, эффект берётся из зелья целиком, а бутылка пустеет
     * (см. {@link #finishUsingItem}).
     *
     * <p>Колоть нечего, если в крови нет эффектов и зелья во второй руке
     * тоже нет.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Уже заряженный болт не перезаряжается — иначе зелье ушло бы в никуда.
        if (SyringeEffects.isCharged(stack)) {
            return InteractionResultHolder.fail(stack);
        }
        // Заливка из зелья во второй руке: ни укола, ни потери сердца.
        boolean filling = SyringeEffects.canFillFromPotion(player, hand);
        if (!filling && !SyringeEffects.hasTransferable(player)) {
            return InteractionResultHolder.fail(stack);
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            if (!filling) {
                // Укол: 1 сердце. Магия — игла проходит сквозь броню.
                player.hurt(level.damageSources().magic(), SELF_DAMAGE);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.45F, 1.6F);
            }
            // Кулдаун не даёт «доколоть» повторно, если ПКМ держат зажатой:
            // иначе каждый болт из стака стоил бы ещё одно сердце.
            player.getCooldowns().addCooldown(stack.getItem(), INJECT_DURATION);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Конец анимации: один болт из стака становится заряженным.
     *
     * <p>Режим заливки отличается только источником эффекта: зелье из второй
     * руки отдаёт эффект целиком (ничего не остаётся в игроке, поэтому и
     * делить 75/25 нечего), а бутылка пустеет.
     *
     * <p>Стак уменьшается на единицу, а заряженный болт возвращается в руку
     * (если стак опустел) или кладётся в инвентарь.
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player) || level.isClientSide) {
            return stack;
        }
        InteractionHand hand = entity.getUsedItemHand();
        // Рука с болтом ещё известна: stopUsingItem() вызывается после нас.
        PotionContents contents = SyringeEffects.canFillFromPotion(player, hand)
                ? SyringeEffects.consumePotion(player, SyringeEffects.opposite(hand))
                : SyringeEffects.extract(player, SyringeEffects.BOLT_TRANSFER);
        if (!contents.hasEffects()) {
            return stack;
        }
        ItemStack charged = stack.copyWithCount(1);
        SyringeEffects.set(charged, contents);
        // Превращение 1:1 — один обычный болт становится заряженным.
        stack.shrink(1);
        SyringeEffects.burst(level, player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D),
                contents.getColor());
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.9F, 1.4F);
        if (stack.isEmpty()) {
            return charged;
        }
        if (!player.addItem(charged)) {
            player.drop(charged, false);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        SyringeEffects.appendTooltip(stack, context, tooltip, flag);
    }
}
