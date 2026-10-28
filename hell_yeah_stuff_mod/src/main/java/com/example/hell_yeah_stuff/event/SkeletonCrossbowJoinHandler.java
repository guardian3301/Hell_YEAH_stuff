package com.example.hell_yeah_stuff.event;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.entity.SkeletonCrossbowSupport;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Вооружает скелета мульти-арбалетом при появлении в мире.
 *
 * <p>Миксин {@code AbstractSkeletonMixin} вешает оружие в {@code finalizeSpawn},
 * но команда {@code /summon} в 1.21.1 вызывает {@code SummonCommand#createEntity}
 * с {@code finalize=false} — {@code finalizeSpawn} не выполняется вообще (скелет
 * приходит с пустыми руками и без боевых целей). Поэтому здесь закрываем этот
 * случай: скелет без оружия в руках получает мульти-арбалет и цель на стрельбу.
 *
 * <p>Скелетов с луком/мечом не трогаем, сущности, прочитанные с диска, — тоже
 * (им оружие уже выдано и сохранено в NBT).
 */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID)
public final class SkeletonCrossbowJoinHandler {

    private SkeletonCrossbowJoinHandler() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk()) return;
        if (!(event.getEntity() instanceof AbstractSkeleton skeleton)) return;
        if (!(skeleton instanceof Mob mob)) return;
        if (skeleton.level() == null || skeleton.level().isClientSide) return;
        if (!SkeletonCrossbowSupport.isPlainSkeleton(mob)) return;

        if (SkeletonCrossbowSupport.isHoldingMultiCrossbow(skeleton)) {
            // Уже вооружён (естественный спавн/яйцо) — просто согласуем цели.
            SkeletonCrossbowSupport.onReassessWeaponGoal(skeleton);
            return;
        }
        // Лук, меч и прочее ванильное снаряжение не отбираем.
        if (SkeletonCrossbowSupport.hasAnyWeapon(skeleton)) return;
        if (!SkeletonCrossbowSupport.shouldArmOnSpawn(null, mob.getRandom())) return;

        SkeletonCrossbowSupport.equipMultiCrossbow(mob);
        SkeletonCrossbowSupport.onReassessWeaponGoal(skeleton);
    }
}
