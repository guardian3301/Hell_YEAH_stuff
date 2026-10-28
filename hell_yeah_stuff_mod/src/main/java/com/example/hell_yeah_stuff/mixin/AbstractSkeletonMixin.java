package com.example.hell_yeah_stuff.mixin;

import com.example.hell_yeah_stuff.entity.SkeletonCrossbowSupport;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Скелет с мульти-арбалетом.
 *
 * <p>Ванильный {@link AbstractSkeleton} умеет только лук, поэтому здесь:
 * <ul>
 *   <li>после {@code finalizeSpawn} выдаём мульти-арбалет (яйцо спавна, команда,
 *       спавнер — всегда; естественный спавн — с шансом из конфига);</li>
 *   <li>в хвосте {@code reassessWeaponGoal} подменяем ближний бой на стрельбу
 *       (см. {@link SkeletonCrossbowSupport#onReassessWeaponGoal}).</li>
 * </ul>
 */
@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonMixin {

    @Inject(
            method = "finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;",
            at = @At("TAIL"))
    private void hys$armWithMultiCrossbow(ServerLevelAccessor level, DifficultyInstance difficulty,
                                          MobSpawnType spawnType, @Nullable SpawnGroupData spawnData,
                                          CallbackInfoReturnable<SpawnGroupData> cir) {
        AbstractSkeleton self = (AbstractSkeleton) (Object) this;
        if (!(self instanceof Mob mob)) return;
        if (self.level() == null || self.level().isClientSide) return;
        if (!SkeletonCrossbowSupport.isPlainSkeleton(mob)) return;

        if (SkeletonCrossbowSupport.isHoldingMultiCrossbow(self)) {
            SkeletonCrossbowSupport.onReassessWeaponGoal(self);
            return;
        }
        if (!SkeletonCrossbowSupport.shouldArmOnSpawn(spawnType, mob.getRandom())) return;

        SkeletonCrossbowSupport.equipMultiCrossbow(mob);
        SkeletonCrossbowSupport.onReassessWeaponGoal(self);
    }

    @Inject(method = "reassessWeaponGoal", at = @At("TAIL"))
    private void hys$crossbowWeaponGoal(CallbackInfo ci) {
        SkeletonCrossbowSupport.onReassessWeaponGoal((AbstractSkeleton) (Object) this);
    }
}
