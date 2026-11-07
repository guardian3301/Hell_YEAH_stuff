package com.example.hell_yeah_stuff.event;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.compat.CuriosCompat;
import com.example.hell_yeah_stuff.item.TacticalHolsterItem;
import com.example.hell_yeah_stuff.network.HolsterSyncPayload;
import com.example.hell_yeah_stuff.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Серверная логика тактической кобуры.
 *
 * <p>Свап по бинду (пакет {@link com.example.hell_yeah_stuff.network.HolsterPayload}):
 * кобура пуста + рука валидна — положить; кобура занята + рука пуста — достать;
 * обе заняты — поменять местами. Вне слота ремня кобура не работает.
 *
 * <p>Точки синка (см. {@link com.example.hell_yeah_stuff.network.HolsterSyncPayload}):
 * вход/респавн/смена измерения — владельцу и наблюдающим;
 * {@code StartTracking} — игроку, начавшему видеть владельца.
 */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID)
public final class HolsterHandler {

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncHolster(player);
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncHolster(player);
    }

    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncHolster(player);
    }

    @SubscribeEvent
    static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer tracker
                && event.getTarget() instanceof Player tracked) {
            tracker.connection.send(new HolsterSyncPayload(tracked.getUUID(), currentHolstered(tracked)));
        }
    }

    /** Что сейчас лежит в кобуре на поясе игрока (EMPTY, если кобуры нет). */
    public static ItemStack currentHolstered(Player player) {
        ItemStack holster = CuriosCompat.findCurioInSlot(player, CuriosCompat.BELT_SLOT,
                ModItems.TACTICAL_HOLSTER.get());
        return holster.isEmpty() ? ItemStack.EMPTY : TacticalHolsterItem.getHolstered(holster);
    }

    /** Рассылает содержимое кобуры игрока: владельцу и всем, кто его видит. */
    public static void syncHolster(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new HolsterSyncPayload(player.getUUID(), currentHolstered(player)));
    }

    /** Обработка бинда кобуры: положить / достать / поменять. */
    public static void tryServerHolster(ServerPlayer player) {
        ItemStack holster = CuriosCompat.findCurioInSlot(player, CuriosCompat.BELT_SLOT,
                ModItems.TACTICAL_HOLSTER.get());
        if (holster.isEmpty()) return;

        ItemStack holstered = TacticalHolsterItem.getHolstered(holster);
        ItemStack mainHand = player.getMainHandItem();

        if (holstered.isEmpty()) {
            // Положить: рука должна держать валидный инструмент/оружие.
            if (mainHand.isEmpty() || !TacticalHolsterItem.isValidForHolster(mainHand)) return;
            TacticalHolsterItem.setHolstered(holster, mainHand);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            playSwapSound(player, 0.7F);
        } else if (mainHand.isEmpty()) {
            // Достать.
            player.setItemInHand(InteractionHand.MAIN_HAND, holstered.copy());
            TacticalHolsterItem.setHolstered(holster, ItemStack.EMPTY);
            playSwapSound(player, 0.9F);
        } else if (TacticalHolsterItem.isValidForHolster(mainHand)) {
            // Поменять: текущее из кобуры в руку, из руки — в кобуру.
            TacticalHolsterItem.setHolstered(holster, mainHand);
            player.setItemInHand(InteractionHand.MAIN_HAND, holstered.copy());
            playSwapSound(player, 0.8F);
        } else {
            // В руке невалидный предмет — ничего не делаем.
            return;
        }
        syncHolster(player);
    }

    private static void playSwapSound(ServerPlayer player, float pitch) {
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7F, pitch);
    }

    private HolsterHandler() {}
}
