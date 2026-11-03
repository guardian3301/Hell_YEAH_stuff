package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.entity.AmethystBoltEntity;
import com.example.hell_yeah_stuff.entity.AmethystGrenadeEntity;
import com.example.hell_yeah_stuff.entity.AmethystShardEntity;
import com.example.hell_yeah_stuff.entity.DartEntity;
import com.example.hell_yeah_stuff.entity.ExplosiveDartEntity;
import com.example.hell_yeah_stuff.entity.GrappleDartEntity;
import com.example.hell_yeah_stuff.entity.IronBoltEntity;
import com.example.hell_yeah_stuff.entity.SpectralCapacitorEntity;
import com.example.hell_yeah_stuff.entity.SyringeBoltEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, HellYeahStuffMod.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<DartEntity>> DART =
            ENTITY_TYPES.register("dart",
                    () -> EntityType.Builder.<DartEntity>of(DartEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("dart"));

    public static final DeferredHolder<EntityType<?>, EntityType<ExplosiveDartEntity>> EXPLOSIVE_DART =
            ENTITY_TYPES.register("explosive_dart",
                    () -> EntityType.Builder.<ExplosiveDartEntity>of(ExplosiveDartEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("explosive_dart"));

    public static final DeferredHolder<EntityType<?>, EntityType<GrappleDartEntity>> GRAPPLE_DART =
            ENTITY_TYPES.register("grapple_dart",
                    () -> EntityType.Builder.<GrappleDartEntity>of(GrappleDartEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("grapple_dart"));

    public static final DeferredHolder<EntityType<?>, EntityType<AmethystShardEntity>> AMETHYST_SHARD =
            ENTITY_TYPES.register("amethyst_shard",
                    () -> EntityType.Builder.<AmethystShardEntity>of(AmethystShardEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("amethyst_shard"));

    public static final DeferredHolder<EntityType<?>, EntityType<AmethystGrenadeEntity>> AMETHYST_GRENADE =
            ENTITY_TYPES.register("amethyst_grenade",
                    () -> EntityType.Builder.<AmethystGrenadeEntity>of(AmethystGrenadeEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("amethyst_grenade"));

    public static final DeferredHolder<EntityType<?>, EntityType<IronBoltEntity>> IRON_BOLT =
            ENTITY_TYPES.register("iron_bolt",
                    () -> EntityType.Builder.<IronBoltEntity>of(IronBoltEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("iron_bolt"));

    public static final DeferredHolder<EntityType<?>, EntityType<AmethystBoltEntity>> AMETHYST_BOLT =
            ENTITY_TYPES.register("amethyst_bolt",
                    () -> EntityType.Builder.<AmethystBoltEntity>of(AmethystBoltEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("amethyst_bolt"));

    public static final DeferredHolder<EntityType<?>, EntityType<SyringeBoltEntity>> SYRINGE_BOLT =
            ENTITY_TYPES.register("syringe_bolt",
                    () -> EntityType.Builder.<SyringeBoltEntity>of(SyringeBoltEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("syringe_bolt"));

    public static final DeferredHolder<EntityType<?>, EntityType<SpectralCapacitorEntity>> SPECTRAL_CAPACITOR =
            ENTITY_TYPES.register("spectral_capacitor",
                    () -> EntityType.Builder.<SpectralCapacitorEntity>of(SpectralCapacitorEntity::new, MobCategory.MISC)
                            .sized(0.35F, 0.35F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("spectral_capacitor"));

    private ModEntities() {}
}