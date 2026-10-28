package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid = HellYeahStuffMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCreativeTabs {
    @SubscribeEvent
    static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.RAIL_CROSSBOW.get());
            event.accept(ModItems.MULTI_CROSSBOW.get());
            event.accept(ModItems.DART.get());
            event.accept(ModItems.GRAPPLE_DART.get());
            event.accept(ModItems.EXPLOSIVE_DART.get());
            event.accept(ModItems.BLOCK_MAGAZINE.get());
            event.accept(ModItems.REINFORCED_ROPE.get());
        }
    }
    private ModCreativeTabs() {}
}
