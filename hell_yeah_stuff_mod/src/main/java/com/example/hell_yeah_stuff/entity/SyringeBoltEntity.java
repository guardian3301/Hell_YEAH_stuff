package com.example.hell_yeah_stuff.entity;

import com.example.hell_yeah_stuff.item.SyringeEffects;
import com.example.hell_yeah_stuff.registry.ModEntities;
import com.example.hell_yeah_stuff.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Шприц-болт — снаряд рельсового арбалета с эффектом внутри.
 *
 * <p>Наследует железный болт (свист после 20 блоков), но урон 4 вместо 3 и
 * при попадании отдаёт цели эффект из своего стака — с уровнем и
 * длительностью, которые туда положил {@link com.example.hell_yeah_stuff.item.SyringeBoltItem}.
 *
 * <p>Цвет жидкости синхронизируется отдельным полем: стак боеприпаса клиенту
 * не передаётся, а частицы и подкраска рендера нужны на обеих сторонах.
 */
public class SyringeBoltEntity extends IronBoltEntity {

    /** Урон шприц-болта. */
    public static final double DAMAGE = 2.7D;

    private static final EntityDataAccessor<Integer> ID_EFFECT_COLOR =
            SynchedEntityData.defineId(SyringeBoltEntity.class, EntityDataSerializers.INT);

    public SyringeBoltEntity(EntityType<? extends SyringeBoltEntity> type, Level level) {
        super(type, level);
    }

    public SyringeBoltEntity(Level level, LivingEntity shooter, ItemStack pickupStack,
                             @Nullable ItemStack firedFromWeapon) {
        super(ModEntities.SYRINGE_BOLT.get(), level, shooter, pickupStack, firedFromWeapon);
        this.setBaseDamage(DAMAGE);
        this.updateColor();
    }

    public SyringeBoltEntity(Level level, double x, double y, double z, ItemStack pickupStack) {
        super(ModEntities.SYRINGE_BOLT.get(), level, x, y, z, pickupStack);
        this.setBaseDamage(DAMAGE);
        this.updateColor();
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.SYRINGE_BOLT.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ID_EFFECT_COLOR, SyringeEffects.NO_COLOR);
    }

    /** Цвет жидкости в болте (-1 — пусто). */
    public int getColor() {
        return this.entityData.get(ID_EFFECT_COLOR);
    }

    private PotionContents potionContents() {
        return this.getPickupItemStackOrigin().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    }

    private void updateColor() {
        PotionContents contents = this.potionContents();
        this.entityData.set(ID_EFFECT_COLOR, SyringeEffects.colorOf(contents));
    }

    @Override
    public void setPickupItemStack(ItemStack stack) {
        super.setPickupItemStack(stack);
        this.updateColor();
    }

    /** Эффект передаётся ровно в том объёме, который лежит в болте. */
    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        if (this.level().isClientSide) {
            return;
        }
        ItemStack ammo = this.getPickupItemStackOrigin().copy();
        Entity owner = this.getOwner();
        SyringeEffects.apply(ammo, target, owner);
        // Эффект одноразовый: болт больше ничего не несёт.
        this.setPickupItemStack(new ItemStack(ModItems.SYRINGE_BOLT.get()));
    }

    /** Цветной след в полёте — тот же цвет, что у жидкости в предмете. */
    @Override
    public void tick() {
        super.tick();
        int color = this.getColor();
        if (!this.level().isClientSide || color == SyringeEffects.NO_COLOR) {
            return;
        }
        if (this.inGround) {
            if (this.inGroundTime % 10 == 0) {
                this.spawnColorParticle(color);
            }
        } else {
            this.spawnColorParticle(color);
        }
    }

    private void spawnColorParticle(int color) {
        this.level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color),
                this.getRandomX(0.4D), this.getRandomY(), this.getRandomZ(0.4D),
                0.0D, 0.0D, 0.0D);
    }
}
