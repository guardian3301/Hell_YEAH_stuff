package com.example.hell_yeah_stuff.item;

import com.example.hell_yeah_stuff.registry.ModCriteria;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.SimpleTier;

import java.util.List;

/**
 * Боевой шприц — холодное оружие, которое «заряжается» кровью владельца.
 *
 * <p>ПКМ — укол себе: игрок теряет 1 сердце (2 единицы), отыгрывается
 * арбалетная анимация (поза руки — {@link com.example.hell_yeah_stuff.client.SyringeArmAnimator},
 * только для руки со шприцем), после
 * чего в предмет переезжает эффект игрока с уровнем и длительностью целиком
 * (слой жидкости — как у зелья, {@link SyringeEffects}).
 *
 * <p>SHIFT+ПКМ заряженным шприцем — обратный укол: содержимое вкалывается
 * СЕБЕ за {@link #SELF_INJECT_DURATION} тиков (0.6 с), шприц пустеет. Так
 * подобранный лут можно сразу применить, не тратя удар по мобу.
 *
 * <p>ЛКМ — обычный удар мечом (урон 5, скорость 1.7). Если шприц заряжен, эффект
 * накладывается на цель в том же объёме и шприц пустеет.
 */
public class BattleSyringeItem extends SwordItem {

    /**
     * Урон удара. Это модификатор ATTACK_DAMAGE: базовый урон игрока 1.0,
     * итог 1.0 + 4.0 = 5.0 — столько и показывает подсказка предмета.
     */
    public static final float ATTACK_DAMAGE = 4.0F;
    /** Скорость атаки 1.7 — поправка к базовым 4.0 игрока. */
    public static final float ATTACK_SPEED = -2.3F;
    /** Прочность шприца. */
    public static final int DURABILITY = 325;
    /** Длительность «укола» в тиках (как натяжение арбалета). */
    public static final int INJECT_DURATION = 20;
    /**
     * Длительность укола себе заряженным шприцем — 0.6 с (12 тиков).
     * Короче набора крови: колоть чужое содержимое себе быстрее, чем цедить своё.
     */
    public static final int SELF_INJECT_DURATION = 12;
    /** Урон себе при уколе — 1 сердце. */
    public static final float SELF_DAMAGE = 2.0F;

    /**
     * Материал шприца: ровно 325 использований (TieredItem берёт прочность из
     * тира), бонуса к урону нет — урон задан атрибутом предмета.
     */
    public static final SimpleTier TIER = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL,
            DURABILITY, 1.0F, 0.0F, 14, () -> Ingredient.of(Items.IRON_INGOT));

    public BattleSyringeItem(Properties properties) {
        super(TIER, properties);
    }

    /** Атрибуты: урон 5, скорость атаки 1.7. */
    public static Properties createProperties() {
        return new Item.Properties()
                .stacksTo(1)
                .attributes(SwordItem.createAttributes(TIER, ATTACK_DAMAGE, ATTACK_SPEED));
    }

    /**
     * Своей ванильной анимации у предмета нет: позу «натянутого арбалета»
     * ставит {@link SyringeArmAnimator} только держащей руке.
     *
     * <p>Именно поэтому здесь {@code NONE}, а не {@code CROSSBOW}:
     * {@code UseAnim.CROSSBOW} заставил бы {@code PlayerRenderer} выставить
     * двуручную позу {@code CROSSBOW_CHARGE}, которая поднимает ОБЕ руки.
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        // Заряженный шприц колет СЕБЕ быстрее (0.6 с), чем набирает кровь (1 с).
        return SyringeEffects.isCharged(stack) ? SELF_INJECT_DURATION : INJECT_DURATION;
    }

    /**
     * ПКМ: укол себе. Игрок сразу теряет 1 сердце, дальше отыгрывается
     * арбалетная анимация, по её окончании эффект переезжает в шприц.
     *
     * <p>Если во второй руке лежит зелье, режим другой — заливка: сердце
     * не тратится, эффект берётся из зелья целиком, а бутылка пустеет
     * (см. {@link #finishUsingItem}).
     *
     * <p>Заряженный шприц не перезаряжается — сначала нужно потратить эффект
     * ударом или вколоть его себе через SHIFT+ПКМ. Если в крови нет эффектов
     * и зелья во второй руке тоже нет, колоть нечего.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (SyringeEffects.isCharged(stack)) {
            // SHIFT+ПКМ — обратный укол: содержимое уходит в игрока за 0.6 с.
            // Без SHIFT заряженный шприц просто лежит в руке и ждёт удара.
            if (!player.isShiftKeyDown()) {
                return InteractionResultHolder.fail(stack);
            }
            if (player.getCooldowns().isOnCooldown(stack.getItem())) {
                return InteractionResultHolder.fail(stack);
            }
            if (!level.isClientSide) {
                player.getCooldowns().addCooldown(stack.getItem(), SELF_INJECT_DURATION);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        // Заливка из зелья во второй руке: ни укола, ни потери сердца.
        boolean filling = SyringeEffects.canFillFromPotion(player, hand);
        if (!filling && !SyringeEffects.hasTransferable(player)) {
            return InteractionResultHolder.fail(stack);
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            if (!filling) {
                // Укол: 1 сердце. Магия — игла проходит сквозь броню.
                player.hurt(level.damageSources().magic(), SELF_DAMAGE);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.45F, 1.6F);
            }
            // Кулдаун не даёт «доколоть» повторно, если ПКМ держат зажатой.
            player.getCooldowns().addCooldown(stack.getItem(), INJECT_DURATION);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Конец анимации. Три режима:
     * <ul>
     *   <li><b>укол себе</b> — шприц уже заряжен (SHIFT+ПКМ): содержимое
     *       накладывается на игрока, шприц пустеет;</li>
     *   <li><b>заливка</b> — эффект зелья из второй руки переезжает в шприц
     *       целиком (уровни и длительности как есть), зелье становится пустой
     *       бутылкой;</li>
     *   <li><b>набор крови</b> — эффект игрока переезжает в шприц.</li>
     * </ul>
     *
     * <p>Выполняется только на сервере — клиент получает изменения через
     * синхронизацию стака и сущности.
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player) || level.isClientSide) {
            return stack;
        }
        if (SyringeEffects.isCharged(stack)) {
            injectSelf(stack, level, player);
            return stack;
        }
        InteractionHand hand = entity.getUsedItemHand();
        // Рука со шприцем ещё известна: stopUsingItem() вызывается после нас.
        boolean fromPotion = SyringeEffects.canFillFromPotion(player, hand);
        PotionContents contents = fromPotion
                ? SyringeEffects.consumePotion(player, SyringeEffects.opposite(hand))
                : SyringeEffects.extract(player, SyringeEffects.FULL_TRANSFER);
        if (!contents.hasEffects()) {
            return stack;
        }
        SyringeEffects.set(stack, contents);
        SyringeEffects.burst(level, player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D),
                contents.getColor());
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.9F, 1.2F);
        // Рыба-ёж даёт отравление, голод и тошноту — шприц, набранный из крови
        // после её поедания, несёт все три эффекта сразу.
        if (!fromPotion && SyringeEffects.isPufferfishContents(contents)) {
            ModCriteria.trigger(ModCriteria.PUFFERFISH_SYRINGE, player);
        }
        return stack;
    }

    /**
     * Вколоть содержимое заряженного шприца себе.
     *
     * <p>Шприц пустеет, а если состав был скрыт («неизвестная жидкость»),
     * засчитывается достижение «САНЯ, ЭТО ТОЧНО РЯЗАНЬ?» — флаг читается
     * ДО {@link SyringeEffects#clear}, который его снимает.
     */
    private static void injectSelf(ItemStack stack, Level level, Player player) {
        boolean unknown = SyringeEffects.isUnknown(stack);
        SyringeEffects.apply(stack, player, player);
        SyringeEffects.clear(stack);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.9F, 1.3F);
        if (unknown) {
            ModCriteria.trigger(ModCriteria.UNKNOWN_INJECTION, player);
        }
    }

    /** Удар: эффект передаётся цели, шприц пустеет. */
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Только сервер: эффект и опустошение шприца разъедутся по клиенту
        // синхронизацией, а двойное наложение дало бы лишний «скачок».
        if (!attacker.level().isClientSide && SyringeEffects.isCharged(stack)) {
            SyringeEffects.apply(stack, target, attacker);
            SyringeEffects.clear(stack);
            attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
        super.postHurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        SyringeEffects.appendTooltip(stack, context, tooltip, flag);
    }
}
