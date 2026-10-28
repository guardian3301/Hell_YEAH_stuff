package com.example.hell_yeah_stuff.mixin.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Совместимость со сторонним модом <b>Death Keeper</b>
 * ({@code dev.lopyluna.death_keeper}).
 *
 * <p>Проблема: {@code DeathSnapshot.readSnapshot} читает предметы могилы через
 * {@code ItemStack.parse(...).orElse(ItemStack.EMPTY)}. Если в мире лежат
 * предметы, которых больше нет в реестре (например, удалённые нами
 * {@code hell_yeah_stuff:amethyst_skeleton_spawn_egg}, {@code hell_yeah_stuff:dash}
 * или {@code hell_yeah_stuff:belt}), они превращаются в {@link ItemStack#EMPTY}
 * и остаются в списке {@code DeathSnapshot.inv}.
 *
 * <p>При следующем сохранении мира {@code DeathSnapshot.writeSnapshot} вызывает
 * {@code ItemStack.save()} уже для ПУСТОГО стака, а ванильный метод бросает
 * {@code IllegalStateException: Cannot encode empty ItemStack}. Исключение
 * происходит внутри {@code ServerLevel#save}, то есть при каждом автосейве —
 * отсюда «спонтанный» краш.
 *
 * <p>Здесь вызов {@code ItemStack.save()} подменяется: для пустого стака
 * возвращается исходный пустой тег вместо исключения. Запись в могиле остаётся
 * пустой, но мир сохраняется штатно.
 *
 * <p>Миксин помечен {@link Pseudo}: если мода Death Keeper нет (или цель
 * не найдена в другой его версии), он просто не применяется.
 */
@Pseudo
@Mixin(targets = "dev.lopyluna.death_keeper.DeathSnapshot")
public class DeathSnapshotMixin {

    /**
     * @param stack      стак из списка могилы
     * @param registries провайдер реестров
     * @param prefix     пустой {@code CompoundTag}, который мод передаёт в save()
     * @return результат сохранения либо исходный пустой тег для пустого стака
     */
    @Redirect(
            method = "writeSnapshot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;save(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;"),
            require = 0)
    private static Tag hys$skipEmptyStacks(ItemStack stack, HolderLookup.Provider registries, Tag prefix) {
        return stack.isEmpty() ? prefix : stack.save(registries, prefix);
    }
}
