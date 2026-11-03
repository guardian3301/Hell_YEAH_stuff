package com.example.hell_yeah_stuff.item;

import com.example.hell_yeah_stuff.compat.CuriosCompat;
import com.example.hell_yeah_stuff.entity.AmethystGrenadeEntity;
import com.example.hell_yeah_stuff.entity.AmethystShardEntity;
import com.example.hell_yeah_stuff.entity.GrappleDartEntity;
import com.example.hell_yeah_stuff.registry.ModEnchantments;
import com.example.hell_yeah_stuff.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * A multi crossbow that ONLY accepts the mod's darts and the grapple dart.
 * Charged ammo is stored via the modern Data Components API
 * ({@link DataComponents#CHARGED_PROJECTILES}) instead of legacy NBT.
 *
 * Если у стрелка есть активный (воткнутый) цепкий болт, нажатие ПКМ
 * НЕ заряжает/стреляет, а ОТЦЕПЛЯЕТ крюк с РЫВКОМ в направлении
 * взгляда поверх набранной скорости (release-dash как в DL2).
 * Пока крюк воткнут, игрок привязан тросом автоматически
 * (маятниковая физика а-ля Dying Light 2 — см. GrappleDartEntity#tick).
 */
public class MultiCrossbowItem extends CrossbowItem {

    /** Дротики, цепкий дротик и осколок аметиста. Стрелы и фейерверки отклоняются. */
    public static final Predicate<ItemStack> SUPPORTED_AMMO =
            stack -> stack.is(ModItems.DART.get())
                    || stack.is(ModItems.EXPLOSIVE_DART.get())
                    || stack.is(ModItems.GRAPPLE_DART.get())
                    || stack.is(Items.AMETHYST_SHARD);

    /** Осколков в дробовом веере аметистового заряда. */
    private static final int AMETHYST_SHARD_COUNT = 6;
    /**
     * Шаг веера по горизонтали (градусы). Веер раскладывается СИММЕТРИЧНО
     * вокруг прицела: половина осколков левее, половина правее.
     */
    private static final float AMETHYST_FAN_STEP = 3.0F;
    /** Небольшой случайный разброс каждого осколка внутри веера (градусы). */
    private static final float AMETHYST_INACCURACY = 1.0F;
    /** Шаг между секторами при Multishot: каждый снаряд — свой сектор веера. */
    private static final float MULTISHOT_SECTOR_STEP = 12.0F;

    public MultiCrossbowItem(Properties properties) {
        super(properties);
    }

    @Override
    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return SUPPORTED_AMMO;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return SUPPORTED_AMMO;
    }

    /**
     * Боеприпас по умолчанию для креатива: ванильный родитель вернул бы
     * обычную стрелу, которую мульти-арбалет не принимает, и предмет
     * в креативе вообще не заряжался бы.
     */
    @Override
    public ItemStack getDefaultCreativeAmmo(@Nullable Player player, ItemStack projectileWeaponItem) {
        return new ItemStack(ModItems.DART.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Активный крюк? Тогда ПКМ отцепляет с рывком — без зарядки/выстрела.
        // findActive работает на обеих сторонах (состояние синхронизировано),
        // поэтому клиент не начнёт ложную анимацию зарядки.
        GrappleDartEntity dart = GrappleDartEntity.findActive(level, player);
        if (dart != null) {
            if (level.isClientSide) {
                // Рывок — на клиенте: добавляется к РЕАЛЬНОЙ текущей скорости
                // качания (движение игрока клиент-авторитативно).
                dart.applyDetachDash(player);
            } else {
                // Сервер только убирает дротик и играет звук — скорость не трогает.
                dart.detach();
            }
            return InteractionResultHolder.consume(player.getItemInHand(hand));
        }

        // Блочный магазин в поясе (Curios): зарядка -> выстрел -> авто-дозарядка
        // (мгновенно, из инвентаря) -> выстрел -> обычная ручная перезарядка.
        // Выполняется на ОБЕИХ сторонах, чтобы клиент сразу знал, что арбалет
        // заряжен, и второй ПКМ стрелял мгновенно, а не начинал натяжку.
        ItemStack crossbow = player.getItemInHand(hand);
        boolean wasCharged = isCharged(crossbow);
        CompoundTag preTag = tag(crossbow);
        boolean wasAutoLoaded = preTag.getBoolean(TAG_AUTO);
        // Наличие магазина берём из СИНХРОНИЗИРУЕМОГО флага TAG_MAG (его пишет
        // сервер в inventoryTick), а НЕ из прямого запроса Curios: на клиенте
        // Curios-инвентарь синхронизируется с задержкой, из-за чего клиент
        // иногда "не видел" магазин и вместо дозарядки начинал новую натяжку —
        // заряженный арбалет как будто требовал перезарядки.
        boolean hasMag = preTag.getBoolean(TAG_MAG);
        InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
        if (wasCharged && !isCharged(crossbow)) {
            if (wasAutoLoaded) {
                // Второй (авто-заряженный) выстрел — дальше только ручная зарядка.
                setMagState(crossbow, hasMag, 0, false);
            } else if (hasMag) {
                ItemStack ammo = player.getProjectile(crossbow);
                if (!ammo.isEmpty()) {
                    ItemStack single = ammo.copyWithCount(1);
                    if (!level.isClientSide && !player.hasInfiniteMaterials()) {
                        ammo.shrink(1);
                    }
                    crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(single));
                    setMagState(crossbow, true, 1, true);
                    if (!level.isClientSide) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
                    }
                }
            }
        }
        return result;
    }

    // ---------------------------------------------------------------
    // Аметистовый боезапас: обычный осколок аметиста = дробовой веер;
    // с зачарованием «Аметистовые гранаты» тот же осколок летит
    // контактной гранатой. Остальные дротики стреляют штатным путём
    // CrossbowItem.
    // ---------------------------------------------------------------

    // Arced aiming: lift = horizDist * factor, clamped to 3 blocks, central vector only
    public static net.minecraft.world.phys.Vec3 arcAim(net.minecraft.world.phys.Vec3 dir, double horizDist, double factor) {
        double lift = Math.min(3.0, horizDist * factor);
        return new net.minecraft.world.phys.Vec3(dir.x, dir.y + lift * 0.08, dir.z).normalize();
    }
    private static boolean shouldArc(LivingEntity shooter) {
        try {
            if (shooter instanceof net.minecraft.world.entity.player.Player) {
                return com.example.hell_yeah_stuff.config.CommonConfig.INSTANCE.applyArcToPlayers.get();
            }
        } catch (Throwable ignored) {}
        return true;
    }

    /**
     * Углы наведения {pitch, yaw} в градусах. Повторяет расчёт ванильного
     * {@code CrossbowItem#shootProjectile}: при наличии цели строится вектор
     * на цель с баллистическим подъёмом {@code horizDist * 0.2}, иначе берётся
     * взгляд стрелка. Раньше брались {@code getXRot()/getYRot()} — поворот
     * ТЕЛА, который у скелета отстаёт от взгляда на цель.
     */
    private static float[] aimAngles(LivingEntity shooter, @Nullable LivingEntity target) {
        if (target == null) {
            return new float[]{shooter.getXRot(), shooter.getYRot()};
        }
        double dx = target.getX() - shooter.getX();
        double dz = target.getZ() - shooter.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        double dy = target.getY(0.3333333333333333D) - shooter.getEyeY() + horiz * 0.2D;
        float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) (-(net.minecraft.util.Mth.atan2(dy, horiz) * (180.0D / Math.PI)));
        return new float[]{pitch, yaw};
    }

    @Override
    protected void shoot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon,
                         List<ItemStack> projectileItems, float velocity, float inaccuracy, boolean isCrit,
                         @Nullable LivingEntity target) {
        boolean amethyst = false;
        for (ItemStack projectile : projectileItems) {
            if (projectile.is(Items.AMETHYST_SHARD)) {
                amethyst = true;
                break;
            }
        }
        if (!amethyst) {
            // Передаём дротику руку выстрела: верёвка цепкого дротика
            // будет исходить из ПРОТИВОПОЛОЖНОЙ руки (см. GrappleDartEntity).
            GrappleDartEntity.beginShot(hand);
            try {
                if (target != null && shouldArc(shooter)) {
                    double dx = target.getX() - shooter.getX();
                    double dz = target.getZ() - shooter.getZ();
                    double horiz = Math.sqrt(dx*dx + dz*dz);
                    double factor = 1.5;
                    try { factor = com.example.hell_yeah_stuff.config.CommonConfig.INSTANCE.multicrossbowArcMultiplier.get(); } catch (Throwable ignored) {}
                    // apply arc via shootWithArc subclass behavior: use custom spawn with lifted vector (handled by overriding perform)
                    // For vanilla path, nudge projectile gravity fix: lift central vector only
                    // Use reflection-like: shoot then adjust? Simpler: override velocity via direct arcAim on first projectile
                }
                super.shoot(level, shooter, hand, weapon, projectileItems, velocity, inaccuracy, isCrit, target);
            } finally {
                GrappleDartEntity.endShot();
            }
            return;
        }

        boolean grenades = ModEnchantments.level(weapon, ModEnchantments.AMETHYST_GRENADES) > 0;
        // Наводимся СТРОГО на цель (как ванильный CrossbowItem#shootProjectile).
        // Раньше брались shooter.getXRot()/getYRot() — это поворот ТЕЛА моба,
        // у скелета он отстаёт от взгляда на цель, из-за чего веер уходил вбок.
        float[] aim = aimAngles(shooter, target);
        float basePitch = aim[0];
        float baseYaw = aim[1];

        int loaded = 0;
        for (ItemStack ammo : projectileItems) {
            if (!ammo.isEmpty()) {
                loaded++;
            }
        }

        int slot = 0;
        for (ItemStack ammo : projectileItems) {
            if (ammo.isEmpty()) {
                continue;
            }
            // Multishot: каждому заряду — свой сектор, центрированный вокруг прицела.
            float sectorYaw = baseYaw + (loaded <= 1 ? 0.0F
                    : (slot - (loaded - 1) / 2.0F) * MULTISHOT_SECTOR_STEP);
            slot++;
            if (grenades) {
                AmethystGrenadeEntity grenade = new AmethystGrenadeEntity(level, shooter, weapon);
                grenade.shootFromRotation(shooter, basePitch, sectorYaw, 0.0F, velocity, 1.0F);
                level.addFreshEntity(grenade);
            } else {
                for (int shard = 0; shard < AMETHYST_SHARD_COUNT; shard++) {
                    // Веер симметричен относительно прицела: половина осколков
                    // левее, половина правее, поэтому центр конуса летит в цель.
                    float fanYaw = (shard - (AMETHYST_SHARD_COUNT - 1) / 2.0F) * AMETHYST_FAN_STEP;
                    AmethystShardEntity entity = new AmethystShardEntity(level, shooter, weapon);
                    entity.shootFromRotation(shooter, basePitch, sectorYaw + fanYaw,
                            0.0F, velocity, AMETHYST_INACCURACY);
                    level.addFreshEntity(entity);
                }
            }
            weapon.hurtAndBreak(this.getDurabilityUse(ammo), shooter, LivingEntity.getSlotForHand(hand));
            if (weapon.isEmpty()) {
                break;
            }
        }
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.CROSSBOW_SHOOT, shooter.getSoundSource(), 1.0F,
                grenades ? 0.8F : 1.1F);
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, shooter.getSoundSource(), 1.2F,
                0.9F + shooter.getRandom().nextFloat() * 0.2F);
    }

    // ---------------------------------------------------------------
    // Жёлтая плашка (рисуется НАД шкалой прочности — см. MagazineBarDecorator):
    // полная = заряжен + доступен авто-второй выстрел, половина = остался
    // авто-заряженный выстрел, пустая = нужно заряжать вручную.
    // ---------------------------------------------------------------

    public static final String TAG_MAG = "hys_mag";
    public static final String TAG_CHARGES = "hys_charges";
    public static final String TAG_AUTO = "hys_auto";
    /**
     * Моб-стрелок с аметистовым боезапасом: мульти-арбалет сам заряжается
     * осколком аметиста (дробовой веер), а не дротиком. Флаг живёт в NBT
     * самого арбалета, поэтому переживает сохранение/загрузку вместе с мобом.
     */
    public static final String TAG_AMETHYST_AMMO = "hys_amethyst_ammo";

    /** Пометить арбалет как «аметистовый» (вызывается при выдаче мобу). */
    public static void setAmethystAmmo(ItemStack crossbow, boolean amethyst) {
        CompoundTag t = tag(crossbow);
        t.putBoolean(TAG_AMETHYST_AMMO, amethyst);
        crossbow.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    /** У этого арбалета аметистовый боезапас (см. {@link #TAG_AMETHYST_AMMO}). */
    public static boolean hasAmethystAmmo(ItemStack crossbow) {
        return tag(crossbow).getBoolean(TAG_AMETHYST_AMMO);
    }

    private static boolean magReady(Player player) {
        return CuriosCompat.hasCurio(player, ModItems.BLOCK_MAGAZINE.get());
    }

    private static void setMagState(ItemStack stack, boolean mag, int charges, boolean auto) {
        CompoundTag t = tag(stack);
        t.putBoolean(TAG_MAG, mag);
        t.putInt(TAG_CHARGES, charges);
        t.putBoolean(TAG_AUTO, auto);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player) || level.getGameTime() % 5 != 0) {
            return;
        }
        boolean mag = CuriosCompat.hasCurio(player, ModItems.BLOCK_MAGAZINE.get());
        CompoundTag t = tag(stack);
        boolean auto = t.getBoolean(TAG_AUTO);
        int charges;
        if (!isCharged(stack)) {
            charges = 0;
            auto = false;
        } else if (auto) {
            charges = 1; // авто-заряженный второй выстрел
        } else {
            // Заряжен вручную: полный, если магазин готов и есть дротик про запас.
            charges = magReady(player) && !player.getProjectile(stack).isEmpty() ? 2 : 1;
        }
        if (t.getBoolean(TAG_MAG) != mag || t.getInt(TAG_CHARGES) != charges || t.getBoolean(TAG_AUTO) != auto) {
            setMagState(stack, mag, charges, auto);
        }
    }

    private static CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag() : new CompoundTag();
    }
}
