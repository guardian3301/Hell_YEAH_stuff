package com.example.hell_yeah_stuff.entity;

import com.example.hell_yeah_stuff.item.MultiCrossbowItem;
import com.example.hell_yeah_stuff.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

/**
 * Поддержка скелета с мульти-арбалетом: выдача оружия и подмена целей AI.
 *
 * <p>Ванильный {@link AbstractSkeleton} умеет только лук: {@code reassessWeaponGoal}
 * ставит {@code RangedBowAttackGoal} для лука и {@code MeleeAttackGoal} во всех
 * остальных случаях. Здесь ближний бой снимается и ставится
 * {@link SkeletonCrossbowAttackGoal} — тот же паттерн, что у разбойника
 * ({@code RangedCrossbowAttackGoal}), но без реализации {@code CrossbowAttackMob}.
 */
public final class SkeletonCrossbowSupport {

    private SkeletonCrossbowSupport() {}

    /** Это обычный скелет (не зимогор и не другой наследник). */
    public static boolean isPlainSkeleton(Mob mob) {
        return mob.getType() == EntityType.SKELETON;
    }

    /** В руке (главной или второй) мульти-арбалет. */
    public static boolean isHoldingMultiCrossbow(LivingEntity entity) {
        return entity.getMainHandItem().is(ModItems.MULTI_CROSSBOW.get())
                || entity.getOffhandItem().is(ModItems.MULTI_CROSSBOW.get());
    }

    /** В руках вообще есть оружие (лук, арбалет, меч...) — не перевооружаем. */
    public static boolean hasAnyWeapon(LivingEntity entity) {
        return !entity.getMainHandItem().isEmpty() || !entity.getOffhandItem().isEmpty();
    }

    /** Надеть мульти-арбалет (малый шанс дропа, как у ванильного оружия мобов). */
    public static void equipMultiCrossbow(Mob mob) {
        equipMultiCrossbow(mob, rollAmethystAmmo(mob.getRandom()));
    }

    /**
     * Надеть мульти-арбалет, явно указав тип боезапаса.
     *
     * @param amethyst {@code true} — стрелок заряжается осколком аметиста
     *                 (дробовой веер), {@code false} — обычным дротиком
     */
    public static void equipMultiCrossbow(Mob mob, boolean amethyst) {
        ItemStack crossbow = new ItemStack(ModItems.MULTI_CROSSBOW.get());
        MultiCrossbowItem.setAmethystAmmo(crossbow, amethyst);
        mob.setItemSlot(EquipmentSlot.MAINHAND, crossbow);
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
    }

    /** Бросок шанса аметистового боезапаса (см. skeleton_amethyst_ammo_chance). */
    public static boolean rollAmethystAmmo(RandomSource random) {
        double chance;
        try {
            chance = com.example.hell_yeah_stuff.config.CommonConfig.INSTANCE.skeletonAmethystAmmoChance.get();
        } catch (Throwable ignored) {
            chance = 0.25D;
        }
        return chance > 0.0D && random.nextDouble() < chance;
    }

    /** У скелета аметистовый боезапас (флаг на самом арбалете в руке). */
    public static boolean hasAmethystAmmo(LivingEntity entity) {
        ItemStack main = entity.getMainHandItem();
        if (main.is(ModItems.MULTI_CROSSBOW.get())) {
            return MultiCrossbowItem.hasAmethystAmmo(main);
        }
        ItemStack off = entity.getOffhandItem();
        return off.is(ModItems.MULTI_CROSSBOW.get()) && MultiCrossbowItem.hasAmethystAmmo(off);
    }

    /**
     * Стоит ли вооружать этого скелета при спавне. Яйцо спавна, команда и
     * спавнеры вооружают всегда, естественный спавн — с шансом из конфига.
     *
     * <p>{@code null} (тип неизвестен — например {@code /summon} в 1.21.1 не
     * вызывает {@code finalizeSpawn}) считается командой: вооружаем всегда.
     */
    public static boolean shouldArmOnSpawn(MobSpawnType spawnType, RandomSource random) {
        if (spawnType == null) return true;
        if (spawnType == MobSpawnType.SPAWN_EGG
                || spawnType == MobSpawnType.COMMAND
                || spawnType == MobSpawnType.SPAWNER
                || spawnType == MobSpawnType.TRIAL_SPAWNER
                || spawnType == MobSpawnType.DISPENSER) {
            return true;
        }
        double chance;
        try {
            chance = com.example.hell_yeah_stuff.config.CommonConfig.INSTANCE.skeletonMultiCrossbowChance.get();
        } catch (Throwable ignored) {
            chance = 0.12D;
        }
        return random.nextDouble() < chance;
    }

    /**
     * Согласовать цели с оружием в руке. Вызывается после спавна/загрузки и после
     * смены предмета в главной руке.
     */
    public static void onReassessWeaponGoal(AbstractSkeleton skeleton) {
        if (skeleton.level() == null || skeleton.level().isClientSide) return;

        if (!isHoldingMultiCrossbow(skeleton)) {
            // Оружие убрали — возвращаем ванильные цели (лук/ближний бой).
            if (removeCrossbowGoals(skeleton)) skeleton.reassessWeaponGoal();
            return;
        }

        removeMeleeGoals(skeleton);
        removeCrossbowGoals(skeleton);
        skeleton.goalSelector.addGoal(4, new SkeletonCrossbowAttackGoal(skeleton, 1.0D, 8.0F));
    }

    private static boolean removeCrossbowGoals(AbstractSkeleton skeleton) {
        boolean removed = false;
        for (WrappedGoal wrapped : new ArrayList<>(skeleton.goalSelector.getAvailableGoals())) {
            if (wrapped.getGoal() instanceof SkeletonCrossbowAttackGoal) {
                skeleton.goalSelector.removeGoal(wrapped.getGoal());
                removed = true;
            }
        }
        return removed;
    }

    private static void removeMeleeGoals(AbstractSkeleton skeleton) {
        List<WrappedGoal> goals = new ArrayList<>(skeleton.goalSelector.getAvailableGoals());
        for (WrappedGoal wrapped : goals) {
            if (wrapped.getGoal() instanceof MeleeAttackGoal) {
                skeleton.goalSelector.removeGoal(wrapped.getGoal());
            }
        }
    }

    /** Идёт натяжка арбалета (для клиентской позы рук). */
    public static boolean isChargingCrossbow(LivingEntity entity) {
        if (!entity.isUsingItem()) return false;
        ItemStack use = entity.getUseItem();
        return use.getItem() instanceof CrossbowItem && !CrossbowItem.isCharged(use);
    }
}
