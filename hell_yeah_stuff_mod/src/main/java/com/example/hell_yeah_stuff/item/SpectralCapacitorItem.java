package com.example.hell_yeah_stuff.item;

import com.example.hell_yeah_stuff.entity.SpectralCapacitorEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;


import java.util.List;

/**
 * Спектральный конденсатор — граната, которую нельзя бросить сразу.
 *
 * <p>Заряжается <b>только от передвижения</b> владельца: пока предмет лежит
 * в инвентаре (в любой руке), каждый тик считается фактически пройденное
 * расстояние, и заряд растёт на {@code расстояние / модификатор} единиц.
 * Стоит встать — расстояние нулевое, заряд не растёт (и не тратится).
 * Шаг, спринт, прыжки и полёт на элитрах дают разный прирост, потому что он
 * пропорционален пройденному пути: полная шкала — примерно
 * {@value #CHARGE_SECONDS_AT_WALK} секунд непрерывной ходьбы.
 * Порог задаётся конфигом {@code spectral_capacitor.charge_units}, делитель
 * расстояния — {@code spectral_capacitor.charge_speed_modifier}.
 *
 * <p>Заряженная граната бросается ПКМ. Она ощутимо тяжелее снежка/яйца:
 * скорость броска втрое ниже ({@value #THROW_SPEED} против 1.5), поэтому
 * летит ближе и падает раньше. Сразу после броска запускается пятисекундный
 * фитиль, затем стягивание и разряд: одна большая частица, отдельный звук,
 * никакого разрушения блоков (см. {@link SpectralCapacitorEntity}).
 *
 * <p>Заряд хранится в компоненте {@code custom_data} самого стака, поэтому
 * переживает перезапуск мира и копируется вместе с предметом.
 */
public class SpectralCapacitorItem extends Item {

    /**
     * Полная шкала заряда по умолчанию: столько «единиц» нужно набрать.
     * При ходьбе начисляется ~1 единица за тик, поэтому 700 единиц — это
     * примерно {@value #CHARGE_SECONDS_AT_WALK} секунд непрерывной ходьбы.
     * Значение переопределяется конфигом {@code spectral_capacitor.charge_units}.
     */
    public static final int DEFAULT_CHARGE_UNITS = 700;
    /** Ориентир для документации: сколько секунд ходьбы нужно на полный заряд. */
    public static final int CHARGE_SECONDS_AT_WALK = 35;
    /**
     * Делитель смещения по умолчанию: за тик начисляется
     * {@code смещение / делитель} единиц заряда. Скорость ходьбы — около
     * 0.216 блока за тик, поэтому делитель 0.216 даёт ровно ~1 единицу за тик
     * ходьбы; спринт и полёт заряжают заметно быстрее.
     */
    public static final double DEFAULT_SPEED_MODIFIER = 0.216D;
    /** Скорость броска: у снежка 1.5 — здесь втрое меньше, «тяжёлая» граната. */
    public static final float THROW_SPEED = 0.5F;
    /** Разброс при броске: тяжёлый предмет летит предсказуемее снежка (1.0). */
    public static final float THROW_INACCURACY = 0.4F;

    /**
     * Ключ заряда в custom_data.
     */
    private static final String TAG_CHARGE = "HysSpectralCharge";
    /** Последняя известная позиция владельца — база для подсчёта смещения. */
    private static final String TAG_LAST_X = "HysSpectralLastX";
    private static final String TAG_LAST_Y = "HysSpectralLastY";
    private static final String TAG_LAST_Z = "HysSpectralLastZ";
    /**
     * Минимальное смещение за тик (блоков), ниже которого движение не считается:
     * отсекает микро-дрейф и покачивание стоящего игрока.
     */
    private static final double MIN_STEP = 1.0E-3D;
    /**
     * Максимальное правдоподобное смещение за тик (блоков). Всё, что выше —
     * телепорт, вход в мир или смена измерения; такая дельта заряд не даёт.
     */
    private static final double MAX_STEP = 8.0D;

    public SpectralCapacitorItem(Properties properties) {
        super(properties);
    }

    /** Текущий заряд в единицах шкалы (0..{@link #chargeUnits()}). */
    public static double charge(ItemStack stack) {
        return tag(stack).getDouble(TAG_CHARGE);
    }

    /**
     * Сколько единиц заряда нужно для полного: значение из COMMON-конфига,
     * с откатом на {@link #DEFAULT_CHARGE_UNITS}, если конфиг ещё не загружен
     * (например, в датагене или на очень раннем этапе).
     */
    public static int chargeUnits() {
        try {
            return com.example.hell_yeah_stuff.config.CommonConfig.INSTANCE
                    .spectralCapacitorChargeUnits.get();
        } catch (Throwable ignored) {
            return DEFAULT_CHARGE_UNITS;
        }
    }

    /**
     * Делитель смещения из COMMON-конфига: за тик граната получает
     * {@code пройденное_расстояние / модификатор} единиц заряда.
     * Никогда не возвращает 0 — иначе деление дало бы бесконечность.
     */
    public static double speedModifier() {
        try {
            double value = com.example.hell_yeah_stuff.config.CommonConfig.INSTANCE
                    .spectralCapacitorChargeSpeedModifier.get();
            return value > 0.0D ? value : DEFAULT_SPEED_MODIFIER;
        } catch (Throwable ignored) {
            return DEFAULT_SPEED_MODIFIER;
        }
    }

    /** Заряжена ли граната настолько, что её можно бросить. */
    public static boolean isCharged(ItemStack stack) {
        return charge(stack) >= chargeUnits();
    }

    /** Прогресс заряда 0..1 — для полоски на слоте. */
    public static float chargeProgress(ItemStack stack) {
        return Mth.clamp((float) (charge(stack) / (double) chargeUnits()), 0.0F, 1.0F);
    }

    private static CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    /** Запоминает текущую позицию владельца как точку отсчёта следующего тика. */
    private static void setAnchor(ItemStack stack, Player player) {
        writeState(stack, charge(stack), player);
    }

    /** Одна запись в стак: и заряд, и позиция-якорь. */
    private static void writeState(ItemStack stack, double charge, Player player) {
        CompoundTag tag = tag(stack).copy();
        tag.putDouble(TAG_CHARGE, Mth.clamp(charge, 0.0D, (double) chargeUnits()));
        tag.putDouble(TAG_LAST_X, player.getX());
        tag.putDouble(TAG_LAST_Y, player.getY());
        tag.putDouble(TAG_LAST_Z, player.getZ());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Заряд начисляется за ФАКТИЧЕСКИ пройденное расстояние, а не за время
     * нахождения в руке.
     *
     * <p>Каждый тик берётся смещение позиции владельца относительно прошлого
     * тика и делится на {@link #speedModifier()}. Стоящий игрок не двигается —
     * расстояние нулевое, заряд не растёт. Шаг, спринт, прыжки и полёт на
     * элитрах дают разный прирост, потому что он пропорционален пути.
     *
     * <p>Позиция-якорь хранится в самом стаке ({@code custom_data}) и
     * обновляется только при реальном смещении — стоя на месте граната не
     * переписывает NBT.
     *
     * <p>Смещение выше {@link #MAX_STEP} — телепорт, вход в мир или смена
     * измерения: заряд не начисляется, но якорь переставляется, чтобы
     * следующая дельта снова была нормальной.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        CompoundTag tag = tag(stack);
        // Первый тик после получения предмета: только запоминаем позицию.
        if (!tag.contains(TAG_LAST_X)) {
            setAnchor(stack, player);
            return;
        }
        double dx = player.getX() - tag.getDouble(TAG_LAST_X);
        double dy = player.getY() - tag.getDouble(TAG_LAST_Y);
        double dz = player.getZ() - tag.getDouble(TAG_LAST_Z);
        double step = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Стоим (или почти стоим) — ни заряда, ни записи в стак.
        if (step < MIN_STEP) {
            return;
        }
        // Телепорт/смена измерения: переставляем якорь без начисления заряда.
        if (step > MAX_STEP) {
            setAnchor(stack, player);
            return;
        }
        double current = tag.getDouble(TAG_CHARGE);
        double next = current;
        if (current < chargeUnits()) {
            next = Math.min(chargeUnits(), current + step / speedModifier());
        }
        // Одна запись: и обновлённый заряд, и новая позиция-якорь.
        writeState(stack, next, player);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isCharged(stack)) {
            return InteractionResultHolder.fail(stack);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 0.55F);

        if (!level.isClientSide) {
            SpectralCapacitorEntity grenade = new SpectralCapacitorEntity(level, player);
            grenade.shootFromRotation(player, player.getXRot(), player.getYRot(),
                    0.0F, THROW_SPEED, THROW_INACCURACY);
            // Тяжёлая граната не «подхватывается» скоростью бегущего игрока.
            grenade.setDeltaMovement(grenade.getDeltaMovement().add(
                    player.getDeltaMovement().x * 0.2D, 0.0D, player.getDeltaMovement().z * 0.2D));
            level.addFreshEntity(grenade);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Заряженная граната светится зачарованием — видно в инвентаре. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return isCharged(stack);
    }

    /**
     * Заряд меняется КАЖДЫЙ тик, поэтому стак в руке постоянно «новый».
     * Без этого переопределения рендер руки считал бы, что предмет достали
     * заново, и бесконечно проигрывал анимацию подъёма руки («нимция use»).
     *
     * <p>Возвращаем {@code false}, пока предмет остаётся тем же конденсатором:
     * тогда смена NBT с зарядом анимацию не вызывает. Смена слота
     * ({@code slotChanged}) по-прежнему её запускает — как у всех предметов.
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged) {
            return true;
        }
        if (oldStack.isEmpty() || newStack.isEmpty()) {
            return true;
        }
        return !(oldStack.getItem() instanceof SpectralCapacitorItem
                && newStack.getItem() instanceof SpectralCapacitorItem);
    }

    /**
     * Подсказка статична: динамический процент заряда убран намеренно,
     * чтобы тултип не «дёргался» на каждом тике ходьбы (прогресс виден
     * полоской на слоте).
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (isCharged(stack)) {
            tooltip.add(Component.translatable("tooltip.hell_yeah_stuff.spectral_capacitor.charged")
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
