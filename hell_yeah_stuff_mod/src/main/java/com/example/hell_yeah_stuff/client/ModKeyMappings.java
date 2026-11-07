package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.event.DashTrailHandler;
import com.example.hell_yeah_stuff.network.DashPayload;
import com.example.hell_yeah_stuff.network.HolsterPayload;
import com.example.hell_yeah_stuff.registry.ModEnchantments;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Бинды мода: рывок и тактическая кобура. */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID, value = Dist.CLIENT)
public final class ModKeyMappings {

    public static final KeyMapping DASH_KEY = new KeyMapping(
            "key.hell_yeah_stuff.dash",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.hell_yeah_stuff");

    public static final KeyMapping HOLSTER_KEY = new KeyMapping(
            "key.hell_yeah_stuff.holster",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.hell_yeah_stuff");

    private static final double DASH_STRENGTH = 1.1D;
    private static final double DASH_UP = 0.18D;
    private static long lastDashGameTime = -1000L;

    @EventBusSubscriber(modid = HellYeahStuffMod.MODID, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(DASH_KEY);
            event.register(HOLSTER_KEY);
        }
        private Registration() {}
    }

    @SubscribeEvent
    static void onClientTickDash(ClientTickEvent.Post event) {
        boolean pressed = false;
        while (DASH_KEY.consumeClick()) pressed = true;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!pressed || player == null || mc.screen != null || player.isSpectator()) return;
        if (ModEnchantments.level(player.getItemBySlot(EquipmentSlot.LEGS), ModEnchantments.DASH) <= 0) return;
        long now = player.level().getGameTime();
        if (now - lastDashGameTime < DashTrailHandler.DASH_COOLDOWN_TICKS) return;
        lastDashGameTime = now;
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0.0D, look.z);
        if (dir.lengthSqr() < 1.0E-4D) dir = Vec3.directionFromRotation(0.0F, player.getYRot());
        dir = dir.normalize();
        player.setDeltaMovement(player.getDeltaMovement().add(dir.x * DASH_STRENGTH, DASH_UP, dir.z * DASH_STRENGTH));
        player.fallDistance = 0.0F;
        PacketDistributor.sendToServer(DashPayload.INSTANCE);
    }

    @SubscribeEvent
    static void onClientTickHolster(ClientTickEvent.Post event) {
        boolean pressed = false;
        while (HOLSTER_KEY.consumeClick()) pressed = true;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!pressed || player == null || mc.screen != null || player.isSpectator()) return;
        PacketDistributor.sendToServer(HolsterPayload.INSTANCE);
    }

    private ModKeyMappings() {}
}
