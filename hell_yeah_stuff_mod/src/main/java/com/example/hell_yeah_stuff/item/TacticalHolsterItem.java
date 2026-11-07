package com.example.hell_yeah_stuff.item;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.registry.ModDataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

/**
 * Тактическая кобура — Curios-аксессуар слота «belt».
 *
 * <p>Работает только будучи надетой в слот ремня: по бинду (по умолчанию V)
 * кладёт в себя инструмент/оружие из главной руки, достаёт обратно или
 * меняет местами (см. {@link com.example.hell_yeah_stuff.event.HolsterHandler}).
 * Убранный предмет живёт в самой кобуре через {@link ModDataComponents#HOLSTERED_ITEM},
 * поэтому не теряется ни при перелогине, ни при снятии кобуры.
 */
public class TacticalHolsterItem extends BeltSlotCurioItem {

    /** Дополнительные предметы, которые кобура принимает (своё оружие мода). */
    public static final TagKey<Item> HOLSTERABLE_TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "holsterable"));

    public TacticalHolsterItem(Properties properties, String tooltipKey) {
        super(properties, tooltipKey);
    }

    /** Предмет, лежащий в кобуре (или EMPTY). */
    public static ItemStack getHolstered(ItemStack holster) {
        return holster.getOrDefault(ModDataComponents.HOLSTERED_ITEM.get(), ItemStack.EMPTY);
    }

    /** Кладёт предмет в кобуру (пустой стак очищает компонент). */
    public static void setHolstered(ItemStack holster, ItemStack item) {
        if (item == null || item.isEmpty()) {
            holster.remove(ModDataComponents.HOLSTERED_ITEM.get());
        } else {
            holster.set(ModDataComponents.HOLSTERED_ITEM.get(), item.copyWithCount(1));
        }
    }

    /** Кобура принимает инструменты и оружие: мечи, кирки/топоры/лопаты/тяпки, луки, арбалеты, трезубец, булаву + тег. */
    public static boolean isValidForHolster(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return stack.is(HOLSTERABLE_TAG)
                || item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof TridentItem
                || item instanceof MaceItem;
    }
}
