package com.example.hell_yeah_stuff.item;

import com.example.hell_yeah_stuff.registry.ModEnchantments;
import com.example.hell_yeah_stuff.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Рельсовый арбалет — наследник ванильного {@link CrossbowItem}.
 *
 * <p>Весь цикл натяжения/зарядки/анимаций достаётся от родителя. Отличий три:
 * свой боеприпас (только болты мода), повышенная скорость снаряда без разброса
 * и зачарование «Аметистовый конденсатор».
 *
 * <p><b>Конденсатор.</b> Сценарий такой:
 * <ol>
 *   <li>игрок зажимает ПКМ и натягивает тетиву — точно так же, как с обычным болтом;</li>
 *   <li>при отпускании в ложе оказывается аметистовый болт, но ещё НЕ сформированный;</li>
 *   <li>идёт таймер 15/10/5 с по уровню чары;</li>
 *   <li>попытка выстрелить раньше срока ничего не делает — вообще никакой анимации,
 *       генерация просто продолжается;</li>
 *   <li>по истечении таймера болт готов и стреляет обычным кликом.</li>
 * </ol>
 * Таймер хранится метками игрового времени на стеке, поэтому тикает и в руке,
 * и в инвентаре, и ничего не сбрасывается при смене предмета.
 */
public class RailCrossbowItem extends CrossbowItem {

    /**
     * Скорость болта — x1.25 от ванильной (3.15 * 1.25 = 3.94).
     *
     * <p>Смещений нет, пока соблюдены два условия: ванильная точка
     * спавна снаряда и ванильный трекинг (4 / 20) в ModEntities.
     * При больших множителях рывок возвращается: клиент считает
     * баллистику сам и интерполируется с серверными пакетами;
     * на скорости выше ванильной ошибка предсказания за тик вырастает
     * до метров и видна как рывок/смещение вбок в начале полёта.
     */
    public static final float VELOCITY = 3.15F * 1.25F;
    /** Разброс выстрела: 0 — болт летит строго в точку прицела. */
    private static final float INACCURACY = 0.0F;

    /** Игровое время начала генерации болта. */
    public static final String TAG_REGEN_START = "hys_rc_regen_start";
    /** Игровое время, когда аметистовый болт будет готов к выстрелу. */
    public static final String TAG_REGEN_END = "hys_rc_regen_end";

    /** Арбалет принимает только болты мода. Стрелы и фейерверки отклоняются. */
    public static final Predicate<ItemStack> SUPPORTED_AMMO =
            stack -> stack.is(ModItems.IRON_BOLT.get())
                    || stack.is(ModItems.AMETHYST_BOLT.get())
                    || stack.is(ModItems.SYRINGE_BOLT.get());

    public RailCrossbowItem(Properties properties) {
        super(properties);
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return SUPPORTED_AMMO;
    }

    @Override
    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return SUPPORTED_AMMO;
    }

    /**
     * Боеприпас по умолчанию для креатива. Без этого переопределения
     * ванильный {@code ProjectileWeaponItem} подсовывает обычную стрелу,
     * и арбалет в креативе стрелял бы не болтом мода.
     */
    @Override
    public ItemStack getDefaultCreativeAmmo(@Nullable Player player, ItemStack projectileWeaponItem) {
        return new ItemStack(ModItems.IRON_BOLT.get());
    }

    /** Заряжен ли арбалет (в компоненте лежит болт). */
    public static boolean isCharged(ItemStack stack) {
        ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
        return charged != null && !charged.isEmpty();
    }

    /** Заряжен ли арбалет именно аметистовым болтом (для модели). */
    public static boolean isAmethystLoaded(ItemStack stack) {
        ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
        return charged != null && charged.contains(ModItems.AMETHYST_BOLT.get());
    }

    /** Заряжен ли арбалет шприц-болтом (для модели и слоя жидкости). */
    public static boolean isSyringeLoaded(ItemStack stack) {
        ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
        return charged != null && charged.contains(ModItems.SYRINGE_BOLT.get());
    }

    /**
     * Заряжен ли арбалет шприц-болтом С эффектом.
     *
     * <p>Разделение нужно модели: пустой болт (без содержимого) рисуется
     * отдельной текстурой {@code rail_crossbow_syringe_bolt_empty}, а с
     * эффектом — корпусом плюс тонируемым слоем жидкости.
     */
    public static boolean isSyringeLoadedWithContent(ItemStack stack) {
        ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
        if (charged == null) {
            return false;
        }
        for (ItemStack ammo : charged.getItems()) {
            if (ammo.is(ModItems.SYRINGE_BOLT.get()) && SyringeEffects.isCharged(ammo)) {
                return true;
            }
        }
        return false;
    }

    /** Цвет жидкости в заряженном шприц-болте (-1 — не тонируется). */
    public static int syringeColor(ItemStack stack) {
        ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
        if (charged == null) {
            return SyringeEffects.NO_COLOR;
        }
        for (ItemStack ammo : charged.getItems()) {
            if (ammo.is(ModItems.SYRINGE_BOLT.get())) {
                return SyringeEffects.color(ammo);
            }
        }
        return SyringeEffects.NO_COLOR;
    }

    // ------------------------------------------------------------------
    // Стрельба
    // ------------------------------------------------------------------

    /**
     * Как у ванильного арбалета, с двумя дополнениями: своя скорость снаряда
     * и блокировка выстрела, пока аметистовый болт не сформирован.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (isCharged(stack)) {
            // Болт ещё генерируется: fail — нет ни выстрела, ни замаха,
            // ни анимации use item. Таймер продолжает идти.
            if (isRegenerating(stack, level.getGameTime())) {
                return InteractionResultHolder.fail(stack);
            }
            this.performShooting(level, player, hand, stack, VELOCITY, INACCURACY, null);
            return InteractionResultHolder.consume(stack);
        }

        if (!player.getProjectile(stack).isEmpty()) {
            return super.use(level, player, hand);
        }

        // Болтов нет — конденсатор позволяет натянуть тетиву впустую.
        if (ModEnchantments.level(stack, ModEnchantments.AMETHYST_CONDENSER) > 0) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.fail(stack);
    }

    /**
     * Отпускание ПКМ. Сначала работает ванильная зарядка из инвентаря;
     * если болтов нет, а тетива натянута полностью — конденсатор ставит в ложе
     * «сырой» аметистовый болт и запускает таймер.
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseTicks) {
        super.releaseUsing(stack, level, entity, remainingUseTicks);
        if (level.isClientSide || isCharged(stack) || hasRegenPending(stack)) {
            return;
        }
        int used = stack.getUseDuration(entity) - remainingUseTicks;
        if (used < getChargeDuration(stack, entity) || !entity.getProjectile(stack).isEmpty()) {
            return;
        }
        int condenser = ModEnchantments.level(stack, ModEnchantments.AMETHYST_CONDENSER);
        if (condenser <= 0) {
            return;
        }
        stack.set(DataComponents.CHARGED_PROJECTILES,
                ChargedProjectiles.of(new ItemStack(ModItems.AMETHYST_BOLT.get())));
        startRegen(stack, level, condenser);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    // ------------------------------------------------------------------
    // «Аметистовый конденсатор»: таймер формирования болта
    // ------------------------------------------------------------------

    /** Длительность генерации в тиках: I — 15 с, II — 10 с, III — 5 с. */
    public static int regenDuration(int enchantLevel) {
        return switch (enchantLevel) {
            case 1 -> 300;
            case 2 -> 200;
            default -> 100;
        };
    }

    private static void startRegen(ItemStack stack, Level level, int enchantLevel) {
        CompoundTag tag = tag(stack);
        long now = level.getGameTime();
        tag.putLong(TAG_REGEN_START, now);
        tag.putLong(TAG_REGEN_END, now + regenDuration(enchantLevel));
        putTag(stack, tag);
    }

    /** Стоит ли метка незавершённой генерации. */
    public static boolean hasRegenPending(ItemStack stack) {
        return tag(stack).contains(TAG_REGEN_END);
    }

    /** Болт ещё формируется — выстрел запрещён. */
    public static boolean isRegenerating(ItemStack stack, long gameTime) {
        CompoundTag tag = tag(stack);
        return tag.contains(TAG_REGEN_END) && gameTime < tag.getLong(TAG_REGEN_END);
    }

    /** Прогресс генерации 0..1. */
    public static float regenProgress(ItemStack stack, long gameTime) {
        CompoundTag tag = tag(stack);
        if (!tag.contains(TAG_REGEN_END)) {
            return 0.0F;
        }
        long start = tag.getLong(TAG_REGEN_START);
        long end = tag.getLong(TAG_REGEN_END);
        if (end <= start) {
            return 0.0F;
        }
        return Mth.clamp((float) (gameTime - start) / (float) (end - start), 0.0F, 1.0F);
    }

    private static void clearRegen(ItemStack stack) {
        CompoundTag tag = tag(stack);
        tag.remove(TAG_REGEN_START);
        tag.remove(TAG_REGEN_END);
        putTag(stack, tag);
    }

    /**
     * Тик таймера. Сравнивается игровое время с меткой, поэтому работает
     * и в руке, и в инвентаре, и переживает перезаход в мир.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        CompoundTag tag = tag(stack);
        if (!tag.contains(TAG_REGEN_END)) {
            return;
        }
        // Болта в ложе больше нет (например, его сняли через команду) — чистим метку.
        if (!isCharged(stack)) {
            clearRegen(stack);
            return;
        }
        if (level.getGameTime() < tag.getLong(TAG_REGEN_END)) {
            return;
        }
        clearRegen(stack);
        level.playSound(null, living.getX(), living.getY(), living.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.3F);
    }

    private static CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag() : new CompoundTag();
    }

    private static void putTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
