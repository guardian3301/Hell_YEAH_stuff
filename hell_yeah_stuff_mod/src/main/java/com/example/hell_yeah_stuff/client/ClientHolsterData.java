package com.example.hell_yeah_stuff.client;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Клиентский кеш «кто что носит в тактической кобуре».
 * Наполняется пакетом {@link com.example.hell_yeah_stuff.network.HolsterSyncPayload};
 * читается рендер-слоем кобуры.
 */
public final class ClientHolsterData {

    private static final Map<UUID, ItemStack> HOLSTERED = new HashMap<>();

    public static void update(UUID playerId, ItemStack holstered) {
        if (holstered == null || holstered.isEmpty()) {
            HOLSTERED.remove(playerId);
        } else {
            HOLSTERED.put(playerId, holstered.copy());
        }
    }

    public static ItemStack get(UUID playerId) {
        return HOLSTERED.getOrDefault(playerId, ItemStack.EMPTY);
    }

    public static void clear() {
        HOLSTERED.clear();
    }

    private ClientHolsterData() {}
}
