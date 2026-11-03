package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Кастомные звуки мода (см. assets/hell_yeah_stuff/sounds.json).
 * Варианты зарегистрированы ОТДЕЛЬНЫМИ событиями, а ротация
 * делается в коде через random*() — гарантированно случайный
 * выбор варианта на каждое воспроизведение.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, HellYeahStuffMod.MODID);

    /** Свист болта в полёте — единоразово после 20 пройденных блоков. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_WHISTLE =
            register("bolt_whistle");

    /**
     * Детонация спектрального конденсатора.
     * Пока ПЛЕЙСХОЛДЕР: файла в assets/.../sounds нет, звук подменяется
     * ванильным в точке вызова (см. SpectralCapacitorEntity#detonate).
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> SPECTRAL_DETONATION =
            register("spectral_detonation");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, name)));
    }

    private ModSounds() {}
}
