package com.example.hell_yeah_stuff.entity;

import com.example.hell_yeah_stuff.registry.ModEntities;
import com.example.hell_yeah_stuff.registry.ModItems;
import com.example.hell_yeah_stuff.registry.ModMobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ExplosiveDartEntity extends AbstractArrow {

    /** Мощность взрыва (у ТНТ = 4.0) */
    private static final float EXPLOSION_POWER = 2.5F;
    /** Задержка детонации: 3 секунды = 60 тиков */
    private static final int FUSE_TIME = 60;
    /** Урон при прямом попадании (до взрыва) */
    private static final double IMPACT_DAMAGE = 3.0D;

    /** -1 = не активирован; иначе — оставшиеся тики до взрыва */
    private int fuseTicks = -1;

    public ExplosiveDartEntity(EntityType<? extends ExplosiveDartEntity> type, Level level) {
        super(type, level);
    }

    public ExplosiveDartEntity(Level level, LivingEntity shooter, ItemStack pickupStack, @Nullable ItemStack firedFromWeapon) {
        super(ModEntities.EXPLOSIVE_DART.get(), shooter, level, pickupStack, firedFromWeapon);
        this.setBaseDamage(IMPACT_DAMAGE);
    }

    public ExplosiveDartEntity(Level level, double x, double y, double z, ItemStack pickupStack) {
        super(ModEntities.EXPLOSIVE_DART.get(), x, y, z, level, pickupStack, null);
        this.setBaseDamage(IMPACT_DAMAGE);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        // Фитиль дротика (попадание в блок / неживую цель): нарастающий писк,
        // взрыв по достижении нуля. Попадание по мобу идёт другим путём —
        // через эффект «Взрывоопасность», см. onHitEntity.
        if (this.fuseTicks >= 0) {
            if (this.fuseTicks == 0) {
                this.explode();
                return;
            }

            // Нарастающий писк: интервал между сигналами сокращается,
            // тон становится выше по мере приближения взрыва
            int elapsed = FUSE_TIME - this.fuseTicks;
            int interval = this.fuseTicks > 40 ? 12 : (this.fuseTicks > 20 ? 6 : 3);
            if (this.fuseTicks % interval == 0) {
                float progress = elapsed / (float) FUSE_TIME; // 0.0 -> 1.0
                float pitch = 0.8F + 1.2F * progress;          // 0.8 -> 2.0
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.NEUTRAL,
                        1.5F, pitch);
            }

            this.fuseTicks--;
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        this.arm();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // Не вызываем super: ванильный путь уничтожил бы дротик сразу.
        // По живой цели дротик наносит прямой урон и «минирует» её —
        // вешает эффект «Взрывоопасность» на фиксированные 3 секунды.
        // По истечении эффекта ExplosivenessHandler делает ТОТ ЖЕ взрыв,
        // что и фитиль при попадании в блок.
        if (this.level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().arrow(this, owner != null ? owner : this);
        target.hurt(source, (float) this.getBaseDamage());

        if (target instanceof LivingEntity living && living.isAlive()) {
            living.addEffect(new MobEffectInstance(
                    ModMobEffects.EXPLOSIVENESS, FUSE_TIME, 0, false, true, true));
            this.discard();
        } else {
            // Неживая цель (лодка, стойка и т.п.) — ведём себя как при
            // ударе о блок: гасим скорость и запускаем фитиль на месте.
            this.setDeltaMovement(Vec3.ZERO);
            this.arm();
        }
    }

    /** Активирует 3-секундный таймер детонации */
    private void arm() {
        if (!this.level().isClientSide && this.fuseTicks < 0) {
            this.fuseTicks = FUSE_TIME;
            this.pickup = Pickup.DISALLOWED; // активированный дротик нельзя подобрать
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.NEUTRAL,
                    1.5F, 0.8F);
        }
    }

    /**
     * Взрыв взрывного дротика — общий и для попадания в блок (по фитилю),
     * и для истечения эффекта «Взрывоопасность» на цели: одинаковая
     * мощность и тип взаимодействия (TNT).
     */
    public static void detonate(Level level, @Nullable Entity source, double x, double y, double z) {
        level.explode(source, x, y, z, EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
    }

    /**
     * Взрыв на самой цели эффекта «Взрывоопасность».
     *
     * <p>Источник взрыва — {@code null}, и это принципиально:
     * {@code Level#getEntities(source, ...)} исключает source из списка целей,
     * поэтому при передаче самой жертвы она бы урона не получила. Без direct
     * source в зону попадают все, включая носителя эффекта.
     */
    public static void detonateOnVictim(Level level, LivingEntity victim) {
        level.explode(null, victim.getX(), victim.getY(0.5D), victim.getZ(),
                EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
    }

    private void explode() {
        detonate(this.level(), this, this.getX(), this.getY(), this.getZ());
        this.discard();
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.EXPLOSIVE_DART.get());
    }
}
