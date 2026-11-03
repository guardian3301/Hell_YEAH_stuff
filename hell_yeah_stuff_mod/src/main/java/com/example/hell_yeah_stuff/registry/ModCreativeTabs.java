package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Наполнение ванильных вкладок креатива предметами мода.
 *
 * <p>Предметы не сваливаются в конец вкладки «Бой», а встают в свои разделы:
 * <ul>
 *   <li>арбалеты — сразу после ванильного арбалета;</li>
 *   <li>болты и дротики — к стрелам (после обычной стрелы);</li>
 *   <li>магазин и верёвка — к броне (после незеритовых ботинок);</li>
 *   <li>боевой шприц — к мечам (после незеритового меча).</li>
 * </ul>
 *
 * <p>Порядок вставок важен: каждый предмет вставляется после ВАНИЛЬНОГО
 * элемента, поэтому модовые предметы выстраиваются в том порядке, в котором
 * вызываются вставки.
 */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCreativeTabs {

    private static final CreativeModeTab.TabVisibility BOTH =
            CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;

    @SubscribeEvent
    static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            addIngredients(event);
            return;
        }
        if (event.getTabKey() != CreativeModeTabs.COMBAT) {
            return;
        }

        // Мечи: боевой шприц — сразу после незеритового меча.
        event.insertAfter(new ItemStack(Items.NETHERITE_SWORD),
                new ItemStack(ModItems.BATTLE_SYRINGE.get()), BOTH);

        // Арбалеты: рельсовый и мульти — после ванильного арбалета.
        event.insertAfter(new ItemStack(Items.CROSSBOW),
                new ItemStack(ModItems.RAIL_CROSSBOW.get()), BOTH);
        event.insertAfter(new ItemStack(ModItems.RAIL_CROSSBOW.get()),
                new ItemStack(ModItems.MULTI_CROSSBOW.get()), BOTH);

        // Боеприпасы: болты и дротики — к стрелам, после обычной стрелы.
        // Аметистового болта здесь нет: он не снаряд для игрока, а
        // «placeholder»-боеприпас, который рельсовый арбалет генерирует сам
        // (аметистовый конденсатор), поэтому в креативе ему делать нечего.
        event.insertAfter(new ItemStack(Items.ARROW),
                new ItemStack(ModItems.IRON_BOLT.get()), BOTH);
        event.insertAfter(new ItemStack(ModItems.IRON_BOLT.get()),
                new ItemStack(ModItems.SYRINGE_BOLT.get()), BOTH);
        event.insertAfter(new ItemStack(ModItems.SYRINGE_BOLT.get()),
                new ItemStack(ModItems.DART.get()), BOTH);
        event.insertAfter(new ItemStack(ModItems.DART.get()),
                new ItemStack(ModItems.EXPLOSIVE_DART.get()), BOTH);
        event.insertAfter(new ItemStack(ModItems.EXPLOSIVE_DART.get()),
                new ItemStack(ModItems.GRAPPLE_DART.get()), BOTH);
        // Граната — метательное: после крюка-кошки, в том же разделе боеприпасов.
        event.insertAfter(new ItemStack(ModItems.GRAPPLE_DART.get()),
                new ItemStack(ModItems.SPECTRAL_CAPACITOR.get()), BOTH);

        // Аксессуары: магазин и верёвка — к броне, после незеритовых ботинок.
        event.insertAfter(new ItemStack(Items.NETHERITE_BOOTS),
                new ItemStack(ModItems.BLOCK_MAGAZINE.get()), BOTH);
        event.insertAfter(new ItemStack(ModItems.BLOCK_MAGAZINE.get()),
                new ItemStack(ModItems.REINFORCED_ROPE.get()), BOTH);
    }

    /**
     * Ингредиенты для крафта: тетива и колодка арбалета — во вкладке
     * «Ингредиенты», сразу после нити (рядом с прочими волокнистыми
     * материалами), друг за другом.
     */
    private static void addIngredients(BuildCreativeModeTabContentsEvent event) {
        event.insertAfter(new ItemStack(Items.STRING),
                new ItemStack(ModItems.BOWSTRING.get()), BOTH);
        event.insertAfter(new ItemStack(ModItems.BOWSTRING.get()),
                new ItemStack(ModItems.CROSSBOW_BLOCK.get()), BOTH);
        // Провод — сразу после медного слитка (рядом с остальными слитками).
        event.insertAfter(new ItemStack(Items.COPPER_INGOT),
                new ItemStack(ModItems.COPPER_WIRE.get()), BOTH);
    }

    private ModCreativeTabs() {}
}

