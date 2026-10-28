package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.item.RailCrossbowItem;
import com.example.hell_yeah_stuff.registry.ModEnchantments;
import com.example.hell_yeah_stuff.registry.ModEntities;
import com.example.hell_yeah_stuff.registry.ModItems;
import com.example.hell_yeah_stuff.registry.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = HellYeahStuffMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ModItems.MULTI_CROSSBOW.get(),
                    ResourceLocation.withDefaultNamespace("pull"),
                    (stack, level, entity, seed) -> {
                        if (entity == null) return 0.0F;
                        return CrossbowItem.isCharged(stack) ? 0.0F : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / (float) CrossbowItem.getChargeDuration(stack, entity);
                    });
            ItemProperties.register(ModItems.MULTI_CROSSBOW.get(),
                    ResourceLocation.withDefaultNamespace("pulling"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
            ItemProperties.register(ModItems.MULTI_CROSSBOW.get(),
                    ResourceLocation.withDefaultNamespace("charged"),
                    (stack, level, entity, seed) -> CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
            ItemProperties.register(ModItems.MULTI_CROSSBOW.get(),
                    ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "explosive"),
                    (stack, level, entity, seed) -> {
                        ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
                        return charged != null && charged.contains(ModItems.EXPLOSIVE_DART.get()) ? 1.0F : 0.0F;
                    });
            ItemProperties.register(ModItems.MULTI_CROSSBOW.get(),
                    ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "grapple"),
                    (stack, level, entity, seed) -> {
                        ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
                        return charged != null && charged.contains(ModItems.GRAPPLE_DART.get()) ? 1.0F : 0.0F;
                    });
            ItemProperties.register(ModItems.MULTI_CROSSBOW.get(),
                    ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "amethyst"),
                    (stack, level, entity, seed) -> {
                        ChargedProjectiles c = stack.get(DataComponents.CHARGED_PROJECTILES);
                        return c != null && c.contains(net.minecraft.world.item.Items.AMETHYST_SHARD) ? 1.0F : 0.0F;
                    });
            ItemProperties.register(ModItems.MULTI_CROSSBOW.get(),
                    ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "grenades"),
                    (stack, level, entity, seed) -> 0.0F);
            Item crossbow = ModItems.RAIL_CROSSBOW.get();
            ItemProperties.register(crossbow, ResourceLocation.withDefaultNamespace("pull"),
                (stack, level, entity, seed) -> entity == null ? 0.0F : CrossbowItem.isCharged(stack) ? 0.0F : (float)(stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / (float)CrossbowItem.getChargeDuration(stack, entity));
            ItemProperties.register(crossbow, ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
            ItemProperties.register(crossbow, ResourceLocation.withDefaultNamespace("charged"),
                (stack, level, entity, seed) -> RailCrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
            ItemProperties.register(crossbow, ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "amethyst"),
                (stack, level, entity, seed) -> RailCrossbowItem.isAmethystLoaded(stack) ? 1.0F : 0.0F);
            ItemProperties.register(crossbow, ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "regenerating"),
                (stack, level, entity, seed) -> RailCrossbowItem.isRegenerating(stack, gameTime(level, entity)) ? 1.0F : 0.0F);
            ItemProperties.register(crossbow, ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "regen"),
                (stack, level, entity, seed) -> { long now = gameTime(level, entity); if (!RailCrossbowItem.isRegenerating(stack, now)) return 0.0F; return 0.01F + RailCrossbowItem.regenProgress(stack, now) * 0.99F; });
            ItemProperties.register(crossbow, ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "condenser"),
                (stack, level, entity, seed) -> ModEnchantments.level(stack, ModEnchantments.AMETHYST_CONDENSER) / 3.0F);
        });
    }

    private static long gameTime(@Nullable ClientLevel level, @Nullable LivingEntity entity) {
        if (level != null) return level.getGameTime();
        return entity != null ? entity.level().getGameTime() : 0L;
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DART.get(), DartRenderer::new);
        event.registerEntityRenderer(ModEntities.EXPLOSIVE_DART.get(), ExplosiveDartRenderer::new);
        event.registerEntityRenderer(ModEntities.GRAPPLE_DART.get(), GrappleDartRenderer::new);
        event.registerEntityRenderer(ModEntities.AMETHYST_SHARD.get(), AmethystShardRenderer::new);
        event.registerEntityRenderer(ModEntities.AMETHYST_GRENADE.get(), AmethystGrenadeRenderer::new);
        event.registerEntityRenderer(ModEntities.IRON_BOLT.get(), IronBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.AMETHYST_BOLT.get(), AmethystBoltRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.DASH_TRAIL.get(), sprites -> new DashTrailParticle.Provider(sprites));
    }

    @SubscribeEvent
    static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(ModItems.MULTI_CROSSBOW.get(), new MagazineBarDecorator());
    }

    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new MultiCrossbowClientExtensions(), ModItems.MULTI_CROSSBOW.get());
        event.registerItem(new RailCrossbowClientExtensions(), ModItems.RAIL_CROSSBOW.get());
    }

    private ClientSetup() {}
}
