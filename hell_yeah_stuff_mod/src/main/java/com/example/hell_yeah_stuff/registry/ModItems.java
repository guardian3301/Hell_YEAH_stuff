package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.item.AmethystBoltItem;
import com.example.hell_yeah_stuff.item.BattleSyringeItem;
import com.example.hell_yeah_stuff.item.BeltSlotCurioItem;
import com.example.hell_yeah_stuff.item.DartItem;
import com.example.hell_yeah_stuff.item.ExplosiveDartItem;
import com.example.hell_yeah_stuff.item.GrappleDartItem;
import com.example.hell_yeah_stuff.item.IronBoltItem;
import com.example.hell_yeah_stuff.item.MultiCrossbowItem;
import com.example.hell_yeah_stuff.item.RailCrossbowItem;
import com.example.hell_yeah_stuff.item.SpectralCapacitorItem;
import com.example.hell_yeah_stuff.item.SyringeBoltItem;
import com.example.hell_yeah_stuff.item.TacticalHolsterItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HellYeahStuffMod.MODID);

    public static final DeferredItem<Item> MULTI_CROSSBOW = ITEMS.register("multi_crossbow",
            () -> new MultiCrossbowItem(new Item.Properties().stacksTo(1).durability(326)));

    /**
     * Рельсовый арбалет. Прочность как у ванильного арбалета (465).
     */
    public static final DeferredItem<Item> RAIL_CROSSBOW = ITEMS.register("rail_crossbow",
            () -> new RailCrossbowItem(new Item.Properties().stacksTo(1).durability(465)));

    /** Обычный дротик — расходник без прочности, стак 64. */
    public static final DeferredItem<Item> DART = ITEMS.register("dart",
            () -> new DartItem(new Item.Properties()));

    public static final DeferredItem<Item> EXPLOSIVE_DART = ITEMS.register("explosive_dart",
            () -> new ExplosiveDartItem(new Item.Properties()));

    /** Цепкий дротик (крюк-кошка) — расходник без прочности. */
    public static final DeferredItem<Item> GRAPPLE_DART = ITEMS.register("grapple_dart",
            () -> new GrappleDartItem(new Item.Properties()));

    /** Железный болт — боеприпас рельсового арбалета, стак 64. */
    public static final DeferredItem<Item> IRON_BOLT = ITEMS.register("iron_bolt",
            () -> new IronBoltItem(new Item.Properties()));

    /**
     * Аметистовый болт — синтезируется только зачарованием «Аметистовый
     * конденсатор» и живёт только внутри компонента заряженных снарядов.
     */
    public static final DeferredItem<Item> AMETHYST_BOLT = ITEMS.register("amethyst_bolt",
            () -> new AmethystBoltItem(new Item.Properties()));

    /**
     * Шприц-болт — боеприпас рельсового арбалета с эффектом внутри.
     * Заряжается уколом в себя (ПКМ), несёт 75% эффекта.
     */
    public static final DeferredItem<Item> SYRINGE_BOLT = ITEMS.register("syringe_bolt",
            () -> new SyringeBoltItem(new Item.Properties()));

    /**
     * Боевой шприц — меч на 325 использований: урон 5, скорость атаки 1.7.
     * ПКМ забирает эффект владельца, удар передаёт его цели.
     */
    public static final DeferredItem<Item> BATTLE_SYRINGE = ITEMS.register("battle_syringe",
            () -> new BattleSyringeItem(BattleSyringeItem.createProperties()));

    // Блочный магазин — Curios-аксессуар слота «belt»: мульти-арбалет дозаряжается сам.
    public static final DeferredItem<Item> BLOCK_MAGAZINE = ITEMS.register("block_magazine",
            () -> new BeltSlotCurioItem(new Item.Properties().stacksTo(1),
                    "tooltip.hell_yeah_stuff.block_magazine"));

    // Усиленная верёвка — Curios-аксессуар слота «belt»: трос крюка-кошки вдвое длиннее.
    public static final DeferredItem<Item> REINFORCED_ROPE = ITEMS.register("reinforced_rope",
            () -> new BeltSlotCurioItem(new Item.Properties().stacksTo(1),
                    "tooltip.hell_yeah_stuff.reinforced_rope"));

    // Тактическая кобура — Curios-аксессуар слота «belt»: хранит один инструмент/оружие (бинд V).
    public static final DeferredItem<Item> TACTICAL_HOLSTER = ITEMS.register("tactical_holster",
            () -> new TacticalHolsterItem(new Item.Properties().stacksTo(1),
                    "tooltip.hell_yeah_stuff.tactical_holster"));

    // ------------------------------------------------------------------
    // Ингредиенты для крафта арбалетов и болтов
    // ------------------------------------------------------------------

    /** Тетива — ингредиент, обычный стак 64. */
    public static final DeferredItem<Item> BOWSTRING = ITEMS.register("bowstring",
            () -> new Item(new Item.Properties()));

    /** Колодка арбалета — ингредиент, обычный стак 64. */
    public static final DeferredItem<Item> CROSSBOW_BLOCK = ITEMS.register("crossbow_block",
            () -> new Item(new Item.Properties()));

    /** Медный провод — ингредиент, обычный стак 64 (из слитка выходит 9 штук). */
    public static final DeferredItem<Item> COPPER_WIRE = ITEMS.register("copper_wire",
            () -> new Item(new Item.Properties()));

    // ------------------------------------------------------------------
    // Метательное
    // ------------------------------------------------------------------

    /**
     * Спектральный конденсатор — граната, которую нельзя бросить сразу:
     * её заряжает ХОДЬБА владельца (см. {@link com.example.hell_yeah_stuff.item.SpectralCapacitorItem}).
     * Заряженная — бросается ПКМ, взрывается через 3 секунды после броска.
     */
    public static final DeferredItem<Item> SPECTRAL_CAPACITOR = ITEMS.register("spectral_capacitor",
            () -> new SpectralCapacitorItem(new Item.Properties().stacksTo(1)));
}
