package com.example.hell_yeah_stuff.entity;

import java.util.EnumSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;

/**
 * Стрельба из арбалета для мобов, которые не реализуют CrossbowAttackMob
 * (обычный скелет).
 *
 * <p>Логика повторяет ванильный RangedCrossbowAttackGoal (разбойник):
 * подход на дистанцию -> натяжка (startUsingItem) -> пауза -> выстрел.
 * Отличие только в выстреле: вызывается CrossbowItem#performShooting напрямую,
 * а не через CrossbowAttackMob#performCrossbowAttack.
 */
public class SkeletonCrossbowAttackGoal extends Goal {

    private final Mob mob;
    private final double speedModifier;
    private final float attackRadiusSqr;

    private State state = State.UNCHARGED;
    private int seeTime;
    private int attackDelay;
    private int updatePathDelay;

    public SkeletonCrossbowAttackGoal(Mob mob, double speedModifier, float attackRadius) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.isValidTarget() && this.isHoldingCrossbow();
    }

    @Override
    public boolean canContinueToUse() {
        return this.isValidTarget()
                && (this.canUse() || !this.mob.getNavigation().isDone())
                && this.isHoldingCrossbow();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private boolean isValidTarget() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive();
    }

    private boolean isHoldingCrossbow() {
        return this.mob.isHolding(stack -> stack.getItem() instanceof CrossbowItem);
    }

    @Override
    public void start() {
        this.state = State.UNCHARGED;
        this.seeTime = 0;
        this.attackDelay = 0;
        this.updatePathDelay = 0;
    }

    @Override
    public void stop() {
        super.stop();
        this.seeTime = 0;
        this.attackDelay = 0;
        this.updatePathDelay = 0;
        if (this.mob.isUsingItem()) {
            this.mob.stopUsingItem();
        }
        // Сбрасываем заряд, чтобы скелет не «залипал» с заряженным арбалетом.
        ItemStack held = this.mob.getMainHandItem();
        if (held.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(held)) {
            held.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        }
        this.state = State.UNCHARGED;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        boolean canSee = this.mob.getSensing().hasLineOfSight(target);
        boolean wasSeen = this.seeTime > 0;
        if (canSee != wasSeen) this.seeTime = 0;
        if (canSee) this.seeTime++;
        else this.seeTime--;

        double distanceSqr = this.mob.distanceToSqr(target);
        boolean shouldApproach = (distanceSqr > (double) this.attackRadiusSqr || this.seeTime < 5)
                && this.attackDelay == 0;

        if (shouldApproach) {
            this.updatePathDelay--;
            if (this.updatePathDelay <= 0) {
                this.mob.getNavigation().moveTo(target,
                        this.canRun() ? this.speedModifier : this.speedModifier * 0.5D);
                this.updatePathDelay = 20 + this.mob.getRandom().nextInt(20);
            }
        } else {
            this.updatePathDelay = 0;
            this.mob.getNavigation().stop();
        }

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        switch (this.state) {
            case UNCHARGED -> {
                if (!shouldApproach) {
                    this.mob.startUsingItem(this.weaponHand());
                    this.state = State.CHARGING;
                }
            }
            case CHARGING -> {
                if (!this.mob.isUsingItem()) {
                    this.state = State.UNCHARGED;
                    break;
                }
                ItemStack using = this.mob.getUseItem();
                if (this.mob.getTicksUsingItem() >= CrossbowItem.getChargeDuration(using, this.mob)) {
                    this.mob.releaseUsingItem();
                    this.state = State.CHARGED;
                    this.attackDelay = 20 + this.mob.getRandom().nextInt(20);
                }
            }
            case CHARGED -> {
                this.attackDelay--;
                if (this.attackDelay <= 0) this.state = State.READY_TO_ATTACK;
            }
            case READY_TO_ATTACK -> {
                if (canSee) {
                    this.shootAt(target);
                    this.state = State.UNCHARGED;
                }
            }
        }
    }

    private boolean canRun() {
        return this.state == State.UNCHARGED;
    }

    private InteractionHand weaponHand() {
        return ProjectileUtil.getWeaponHoldingHand(this.mob, item -> item instanceof CrossbowItem);
    }

    private void shootAt(LivingEntity target) {
        InteractionHand hand = this.weaponHand();
        ItemStack weapon = this.mob.getItemInHand(hand);
        if (!(weapon.getItem() instanceof CrossbowItem crossbow)) return;
        float inaccuracy = (float) (14 - this.mob.level().getDifficulty().getId() * 4);
        crossbow.performShooting(this.mob.level(), this.mob, hand, weapon, 1.6F, inaccuracy, target);
    }

    private enum State {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK
    }
}
