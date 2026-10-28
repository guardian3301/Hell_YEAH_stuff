package com.example.hell_yeah_stuff.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * COMMON-конфиг мода: мульти-арбалет и скелет-стрелок. Регистрируется в HellYeahStuffMod.
 */
public final class CommonConfig {
    public static final ModConfigSpec SPEC;
    public static final CommonConfig INSTANCE;

    /** Множитель дуги мульти-арбалета. */
    public final ModConfigSpec.DoubleValue multicrossbowArcMultiplier;
    /** Применять ли дугу к выстрелам игрока. */
    public final ModConfigSpec.BooleanValue applyArcToPlayers;
    /** Шанс, что обычный скелет заспавнится с мульти-арбалетом. */
    public final ModConfigSpec.DoubleValue skeletonMultiCrossbowChance;
    /** Шанс, что скелет с мульти-арбалетом получит аметистовый боезапас. */
    public final ModConfigSpec.DoubleValue skeletonAmethystAmmoChance;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        INSTANCE = new CommonConfig(b);
        SPEC = b.build();
    }

    private CommonConfig(ModConfigSpec.Builder b) {
        b.push("multicrossbow");
        multicrossbowArcMultiplier = b.comment("Vertical arc multiplier for multi-crossbow aiming (1.0-3.0). Effective lift = horizDist * factor, clamped to 3 blocks.")
                .defineInRange("multicrossbow_arc_multiplier", 1.5, 1.0, 3.0);
        applyArcToPlayers = b.comment("If false, arc aiming applies only to mobs (e.g. skeletons). Players shoot straight.")
                .define("apply_arc_to_players", false);
        skeletonMultiCrossbowChance = b.comment("Chance (0.0-1.0) that a naturally spawned skeleton carries the multi crossbow. Spawn eggs, commands and spawners always arm the skeleton.")
                .defineInRange("skeleton_multi_crossbow_chance", 0.12, 0.0, 1.0);
        skeletonAmethystAmmoChance = b.comment("Chance (0.0-1.0) that a skeleton armed with the multi crossbow gets amethyst ammo instead of darts (a shard fires a shotgun-like spread of amethyst shards). Rolled once per skeleton when it is armed.")
                .defineInRange("skeleton_amethyst_ammo_chance", 0.25, 0.0, 1.0);
        b.pop();
    }
}
