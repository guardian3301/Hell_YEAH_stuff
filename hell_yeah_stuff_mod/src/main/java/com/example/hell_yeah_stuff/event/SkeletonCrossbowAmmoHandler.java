package com.example.hell_yeah_stuff.event;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.entity.SkeletonCrossbowSupport;
import com.example.hell_yeah_stuff.item.MultiCrossbowItem;
import com.example.hell_yeah_stuff.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;

/**
 * Боезапас для мобов с мульти-арбалетом (скелет-стрелок).
 *
 * <p>Ванильный {@code LivingEntity#getProjectile} у мобов возвращает пустой стак,
 * поэтому арбалет у скелета не мог зарядиться. Здесь мобу выдаётся боеприпас:
 * каждый вызов возвращает свежую копию, так что запас у моба не кончается.
 *
 * <p>Тип боеприпаса задаётся флагом на самом арбалете
 * ({@link MultiCrossbowItem#TAG_AMETHYST_AMMO}): скелет с аметистовым запасом
 * заряжается осколком аметиста — мульти-арбалет превращает его в дробовой веер
 * (см. {@code MultiCrossbowItem#shoot}). Игроков не трогаем — они берут
 * боеприпас из инвентаря.
 *
 * <p>Важно: подменяем боеприпас <b>всегда</b>, даже если в событие пришёл
 * непустой стак. У мобов скелет приходит со «стрелой» по умолчанию, и именно
 * из-за неё арбалет заряжался стрелами вместо дротиков/осколков.
 */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID)
public final class SkeletonCrossbowAmmoHandler {

    private SkeletonCrossbowAmmoHandler() {}

    @SubscribeEvent
    public static void onGetProjectile(LivingGetProjectileEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return;
        ItemStack weapon = event.getProjectileWeaponItemStack();
        if (!weapon.is(ModItems.MULTI_CROSSBOW.get())) return;

        boolean amethyst = MultiCrossbowItem.hasAmethystAmmo(weapon)
                || SkeletonCrossbowSupport.hasAmethystAmmo(entity);
        event.setProjectileItemStack(
                new ItemStack(amethyst ? Items.AMETHYST_SHARD : ModItems.DART.get()));
    }
}
