package com.example.hell_yeah_stuff.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * Мягкая совместимость с Curios через рефлексию — собирается и работает без Curios.
 * Используется для проверки «надет ли аксессуар в слот ремня» (блочный магазин,
 * усиленная верёвка).
 *
 * <p>Поиск идёт в два шага: сначала быстрый {@code findFirstCurio}, а если его нет
 * в установленной версии Curios (или он вернул пусто) — перебор всех слотов через
 * {@code getCurios()}. Поэтому совместимость не ломается между версиями Curios.
 */
public final class CuriosCompat {

    /** Слот ремня: см. data/hell_yeah_stuff/curios/slots/belt.json. */
    public static final String BELT_SLOT = "belt";

    private static boolean checked;
    private static Method getCuriosInventory;
    private static Method findFirstCurio;
    private static Method getCurios;
    private static Method stacksGetStacks;
    private static Method stacksGetSlots;
    private static Method stacksGetStackInSlot;

    private CuriosCompat() {}

    public static boolean hasCurio(LivingEntity entity, Item item) {
        return !findCurio(entity, item).isEmpty();
    }

    /** Возвращает ItemStack первого найденного Curios-предмета (или EMPTY). */
    public static ItemStack findCurio(LivingEntity entity, Item item) {
        return findCurioInSlot(entity, null, item);
    }

    /**
     * Ищет предмет в конкретном Curios-слоте.
     *
     * @param slot идентификатор слота (например {@link #BELT_SLOT});
     *             {@code null} — искать во всех слотах
     */
    public static ItemStack findCurioInSlot(LivingEntity entity, String slot, Item item) {
        if (entity == null || item == null || !resolve()) return ItemStack.EMPTY;
        Object inventory = inventory(entity);
        if (inventory == null) return ItemStack.EMPTY;

        if (slot == null) {
            ItemStack direct = findByItem(inventory, item);
            if (!direct.isEmpty()) return direct;
        }
        return scan(inventory, slot, item);
    }

    private static Object inventory(LivingEntity entity) {
        try {
            Optional<?> handler = (Optional<?>) getCuriosInventory.invoke(null, entity);
            return handler == null ? null : handler.orElse(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static ItemStack findByItem(Object inventory, Item item) {
        if (findFirstCurio == null) return ItemStack.EMPTY;
        try {
            Optional<?> result = (Optional<?>) findFirstCurio.invoke(inventory, item);
            if (result == null || result.isEmpty()) return ItemStack.EMPTY;
            Object slotResult = result.get();
            ItemStack found = (ItemStack) slotResult.getClass().getMethod("stack").invoke(slotResult);
            return found == null ? ItemStack.EMPTY : found;
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }

    @SuppressWarnings("unchecked")
    private static ItemStack scan(Object inventory, String slot, Item item) {
        if (getCurios == null) return ItemStack.EMPTY;
        try {
            Map<String, Object> curios = (Map<String, Object>) getCurios.invoke(inventory);
            if (curios == null) return ItemStack.EMPTY;
            for (Map.Entry<String, Object> entry : curios.entrySet()) {
                if (slot != null && !slot.equals(entry.getKey())) continue;
                ItemStack found = scanStacks(entry.getValue(), item);
                if (!found.isEmpty()) return found;
            }
        } catch (Throwable ignored) {
            // Другая версия Curios — считаем, что аксессуара нет.
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack scanStacks(Object stacksHandler, Item item) {
        if (stacksHandler == null || stacksGetStacks == null) return ItemStack.EMPTY;
        try {
            Object stacks = stacksGetStacks.invoke(stacksHandler);
            if (stacks == null) return ItemStack.EMPTY;
            int slots = (Integer) stacksGetSlots.invoke(stacks);
            for (int i = 0; i < slots; i++) {
                ItemStack stack = (ItemStack) stacksGetStackInSlot.invoke(stacks, i);
                if (stack != null && !stack.isEmpty() && stack.is(item)) return stack;
            }
        } catch (Throwable ignored) {
            // см. выше
        }
        return ItemStack.EMPTY;
    }

    private static boolean resolve() {
        if (!checked) {
            checked = true;
            try {
                Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                Class<?> handler = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
                getCuriosInventory = api.getMethod("getCuriosInventory", LivingEntity.class);
                getCurios = handler.getMethod("getCurios");
                try {
                    findFirstCurio = handler.getMethod("findFirstCurio", Item.class);
                } catch (Throwable ignored) {
                    findFirstCurio = null;
                }
                Class<?> stacksHandler = Class.forName("top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler");
                Class<?> dynamic = Class.forName("top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler");
                stacksGetStacks = stacksHandler.getMethod("getStacks");
                stacksGetSlots = dynamic.getMethod("getSlots");
                stacksGetStackInSlot = dynamic.getMethod("getStackInSlot", int.class);
            } catch (Throwable t) {
                getCuriosInventory = null;
                findFirstCurio = null;
                getCurios = null;
                stacksGetStacks = null;
                stacksGetSlots = null;
                stacksGetStackInSlot = null;
            }
        }
        return getCuriosInventory != null && (getCurios != null || findFirstCurio != null);
    }
}

