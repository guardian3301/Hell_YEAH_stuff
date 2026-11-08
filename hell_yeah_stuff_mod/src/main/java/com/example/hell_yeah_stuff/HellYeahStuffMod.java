package com.example.hell_yeah_stuff;

import com.example.hell_yeah_stuff.config.CommonConfig;
import com.example.hell_yeah_stuff.registry.ModBlockEntities;
import com.example.hell_yeah_stuff.registry.ModCriteria;
import com.example.hell_yeah_stuff.registry.ModDataComponents;
import com.example.hell_yeah_stuff.registry.ModBlocks;
import com.example.hell_yeah_stuff.registry.ModEnchantmentEffects;
import com.example.hell_yeah_stuff.registry.ModEntities;
import com.example.hell_yeah_stuff.registry.ModFeatures;
import com.example.hell_yeah_stuff.registry.ModItems;
import com.example.hell_yeah_stuff.registry.ModLootFunctions;
import com.example.hell_yeah_stuff.registry.ModMobEffects;
import com.example.hell_yeah_stuff.registry.ModParticles;
import com.example.hell_yeah_stuff.registry.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(HellYeahStuffMod.MODID)
public class HellYeahStuffMod {
    public static final String MODID = "hell_yeah_stuff";

    public HellYeahStuffMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModEnchantmentEffects.ENCHANTMENT_ENTITY_EFFECTS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModLootFunctions.LOOT_FUNCTIONS.register(modEventBus);
        ModCriteria.TRIGGERS.register(modEventBus);
    }
}
