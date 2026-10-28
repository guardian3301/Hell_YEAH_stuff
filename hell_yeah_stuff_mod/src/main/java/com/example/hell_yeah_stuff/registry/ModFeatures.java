package com.example.hell_yeah_stuff.registry;
import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.worldgen.AmethystOutcropFeature;
import com.example.hell_yeah_stuff.worldgen.FloodedGeodePierFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, HellYeahStuffMod.MODID);
    public static final DeferredHolder<Feature<?>, AmethystOutcropFeature> AMETHYST_OUTCROP = FEATURES.register("amethyst_outcrop", AmethystOutcropFeature::new);
    public static final DeferredHolder<Feature<?>, FloodedGeodePierFeature> FLOODED_GEODE_PIER = FEATURES.register("flooded_geode_pier", FloodedGeodePierFeature::new);
    private ModFeatures() {}
}
