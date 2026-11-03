package com.example.hell_yeah_stuff.entity;

import com.example.hell_yeah_stuff.registry.ModEntities;
import com.example.hell_yeah_stuff.registry.ModItems;
import com.example.hell_yeah_stuff.registry.ModParticles;
import com.example.hell_yeah_stuff.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Летящий спектральный конденсатор.
 *
 * <p>Бросок СРАЗУ запускает фитиль: {@value #FUSE_TICKS} тиков (5 секунд)
 * от момента ПКМ, а не от касания. Всё это время граната летит, падает на
 * землю и лежит — таймер идёт.
 *
 * <p>Дальше цепочка из трёх фаз:
 * <ol>
 *   <li><b>фитиль</b> — {@value #FUSE_TICKS} тиков (5 с);</li>
 *   <li><b>стягивание</b> — {@value #PULL_TICKS} тиков (0.75 с): всё живое
 *       в радиусе {@value #PULL_RADIUS} блоков притягивается к гранате;</li>
 *   <li><b>разряд</b> — урон областью {@code 3x2x3} блока вокруг гранаты,
 *       одна большая частица ({@code ModParticles.SPECTRAL_RUPTURE}) и свой
 *       звук. Никакого {@code Level#explode}: блоки не разрушаются.</li>
 * </ol>
 *
 * <p>После разряда граната «выпадает назад» — на месте остаётся предмет
 * конденсатора (с нулевым зарядом), а под ним вспыхивает горизонтальная
 * частица-плейсхолдер ({@code ModParticles.SPECTRAL_AFTERMATH}).
 */
public class SpectralCapacitorEntity extends ThrowableProjectile implements ItemSupplier {

    /** Фитиль: 5 секунд с момента броска. */
    public static final int FUSE_TICKS = 100;
    /** Фаза стягивания: 0.75 секунды. */
    public static final int PULL_TICKS = 15;
    /** Радиус стягивания (блоков). */
    public static final double PULL_RADIUS = 5.0D;
    /** Сила притяжения за тик на краю радиуса (блоков/тик). */
    private static final double PULL_STRENGTH = 0.55D;
    /** Полуразмер области урона по X и Z: 3 блока в ширину/длину. */
    private static final double DAMAGE_HALF_XZ = 1.5D;
    /** Полуразмер области урона по Y: 2 блока в высоту. */
    private static final double DAMAGE_HALF_Y = 1.0D;
    /** Урон разряда по всем, кто попал в область. */
    private static final float RUPTURE_DAMAGE = 6.0F;
    /** Лёгкий разлёт от центра после разряда (блоков/тик). */
    private static final double RUPTURE_KNOCKBACK = 0.35D;
    /** Насколько частица поднята над точкой приземления (блоков). */
    private static final double BURST_HEIGHT = 0.35D;
    /** Гравитация: чуть выше ванильных 0.03 — граната «тяжелее» и падает раньше. */
    private static final double GRAVITY = 0.045D;

    /** Оставшиеся тики фитиля; -1 — ещё не активирован. */
    private int fuse = -1;
    /** Оставшиеся тики стягивания (0 — фаза ещё не началась). */
    private int pullTicks = 0;

    public SpectralCapacitorEntity(EntityType<? extends SpectralCapacitorEntity> type, Level level) {
        super(type, level);
    }

    public SpectralCapacitorEntity(Level level, LivingEntity shooter) {
        super(ModEntities.SPECTRAL_CAPACITOR.get(), shooter, level);
        this.fuse = FUSE_TICKS;
    }

    public SpectralCapacitorEntity(Level level, double x, double y, double z) {
        super(ModEntities.SPECTRAL_CAPACITOR.get(), x, y, z, level);
        this.fuse = FUSE_TICKS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Своих синхронизируемых полей нет: фитиль идёт только на сервере,
        // а клиенту достаточно позиции и скорости.
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.SPECTRAL_CAPACITOR.get());
    }

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    @Override
    public void tick() {
        super.tick();

        // Никаких частиц за снарядом: гранату видно по самой модели.
        if (this.level().isClientSide || this.fuse < 0) {
            return;
        }

        // Фаза 1: фитиль. По его концу начинается стягивание.
        if (this.fuse > 0) {
            if (--this.fuse == 0) {
                this.pullTicks = PULL_TICKS;
            }
            return;
        }

        // Фаза 2: стягивание всего живого к гранате.
        if (this.pullTicks > 0) {
            this.pull();
            if (--this.pullTicks == 0) {
                this.rupture();
            }
        }
    }

    /**
     * Притягивает живое в радиусе {@value #PULL_RADIUS} блоков к гранате.
     * Сила падает к краю радиуса: у самой гранаты тянет сильно, на границе
     * почти не ощущается. Импульс добавляется каждый тик фазы, поэтому
     * за 0.75 секунды цель успевает ощутимо сместиться к центру.
     */
    private void pull() {
        double radiusSqr = PULL_RADIUS * PULL_RADIUS;
        AABB area = new AABB(
                this.getX() - PULL_RADIUS, this.getY() - PULL_RADIUS, this.getZ() - PULL_RADIUS,
                this.getX() + PULL_RADIUS, this.getY() + PULL_RADIUS, this.getZ() + PULL_RADIUS);

        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (!target.isAlive()) {
                continue;
            }
            Vec3 toCenter = new Vec3(
                    this.getX() - target.getX(),
                    this.getY() + this.getBbHeight() * 0.5D - (target.getY() + target.getBbHeight() * 0.5D),
                    this.getZ() - target.getZ());
            double distSqr = toCenter.lengthSqr();
            if (distSqr > radiusSqr || distSqr < 1.0E-4D) {
                continue;
            }
            // 1 у центра, 0 на границе радиуса.
            double falloff = 1.0D - Math.sqrt(distSqr) / PULL_RADIUS;
            Vec3 impulse = toCenter.normalize().scale(PULL_STRENGTH * falloff);
            target.push(impulse.x, impulse.y * 0.4D, impulse.z);
            // Импульс должен дойти до клиента, иначе игрок увидит «телепорт».
            target.hurtMarked = true;
        }
    }

    /**
     * Разряд: урон областью {@code 3x2x3} блока, большая частица и звук.
     * Никакого {@code Level#explode} — блоки не разрушаются. После разряда
     * граната «выпадает назад» предметом и оставляет частицу-плейсхолдер.
     */
    private void rupture() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        this.level().playSound(null, x, y, z, ModSounds.SPECTRAL_DETONATION.get(),
                SoundSource.NEUTRAL, 1.4F, 1.0F);

        AABB damageArea = new AABB(
                x - DAMAGE_HALF_XZ, y - DAMAGE_HALF_Y, z - DAMAGE_HALF_XZ,
                x + DAMAGE_HALF_XZ, y + DAMAGE_HALF_Y, z + DAMAGE_HALF_XZ);
        // Источник — сама граната: владелец броска получит засчитанное убийство.
        DamageSource source = this.damageSources().explosion(this, this.getOwner());
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, damageArea)) {
            if (!target.isAlive()) {
                continue;
            }
            target.hurt(source, RUPTURE_DAMAGE);
            Vec3 away = new Vec3(target.getX() - x, 0.0D, target.getZ() - z);
            if (away.lengthSqr() > 1.0E-4D) {
                Vec3 dir = away.normalize().scale(RUPTURE_KNOCKBACK);
                target.push(dir.x, 0.15D, dir.z);
                target.hurtMarked = true;
            }
        }

        if (this.level() instanceof ServerLevel server) {
            // Одна большая частица разряда.
            server.sendParticles(ModParticles.SPECTRAL_RUPTURE.get(),
                    x, y + BURST_HEIGHT, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            // Горизонтальная частица-плейсхолдер на месте упавшей гранаты.
            server.sendParticles(ModParticles.SPECTRAL_AFTERMATH.get(),
                    x, y + 0.02D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        this.dropSelf();
        this.discard();
    }

    /** «Выпадает назад»: на месте разряда остаётся незаряженный конденсатор. */
    private void dropSelf() {
        ItemStack stack = new ItemStack(ModItems.SPECTRAL_CAPACITOR.get());
        ItemEntity drop = new ItemEntity(this.level(),
                this.getX(), this.getY() + 0.25D, this.getZ(), stack);
        drop.setDefaultPickUpDelay();
        drop.setDeltaMovement(0.0D, 0.15D, 0.0D);
        this.level().addFreshEntity(drop);
    }

    /** Приземление не взрывает гранату — она ждёт, пока догорит фитиль. */
    @Override
    protected void onHit(HitResult result) {
        // Намеренно не вызываем super: ванильный ThrowableProjectile
        // уничтожил бы снаряд при первом касании.
        if (result.getType() == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult) result);
        } else if (result.getType() == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult) result);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // Касание моба ничего не делает: урон не предусмотрен, граната летит дальше.
        if (!this.level().isClientSide) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.4D));
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide) {
            // Гасим скорость и отключаем гравитацию: граната ЛОЖИТСЯ у точки
            // удара и спокойно ждёт, пока догорит фитиль (иначе она бы
            // «дрожала» на поверхности, каждый тик снова падая в блок).
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HysFuse", this.fuse);
        tag.putInt("HysPull", this.pullTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.fuse = tag.getInt("HysFuse");
        this.pullTicks = tag.getInt("HysPull");
    }
}
