package com.example.hell_yeah_stuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.example.hell_yeah_stuff.item.SyringeEffects;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Арбалетная анимация для шприца и шприц-болта.
 *
 * <p>Во время укола (ПКМ) рука держится как при натяжении арбалета. Никаких
 * собственных замахов не добавляется. Готовый к удару (заряженный) предмет
 * никакой особой позы не получает — он лежит в руке как обычный меч.
 *
 * <p>Поза третьего лица ставится ТОЛЬКО на руку с предметом — вручную, в
 * хвосте {@code PlayerModel#setupAnim} (см. {@link SyringeArmAnimator}).
 * Поэтому {@link #getArmPose} всегда возвращает {@link HumanoidModel.ArmPose#ITEM}:
 * он перекрывает ванильный {@code CROSSBOW_CHARGE}, который
 * {@code PlayerRenderer} выставляет по {@code UseAnim.CROSSBOW} и который
 * двуручный (двигал бы и вторую руку).
 *
 * <p>В первом лице ванилла обрабатывает {@code UseAnim.CROSSBOW} только для
 * {@code CrossbowItem}, поэтому для обычного предмета положение руки задаётся
 * здесь же ({@link #applyForgeHandTransform}) — теми же числами, что у
 * натянутого арбалета.
 */
public class SyringeClientExtensions implements IClientItemExtensions {

    /**
     * Подъём шприца при заливке из зелья — 5.5 единицы модели
     * (5.5/16 = 0.34375 блока). В первом лице единица PoseStack равна одному
     * пикселю текстуры предмета, поэтому переводим 5.5 единицы в 5.5/16.
     */
    public static final float FILL_LIFT = 7.6F / 16.0F;

    /**
     * Наклон при заливке — поворот спрайта в плоскости экрана.
     *
     * <p>Наша трансформация применяется ДО display-трансформации предмета
     * (см. {@code ItemRenderer#render}: сначала {@code applyForgeHandTransform},
     * потом {@code handleCameraTransforms}), поэтому ось Z здесь — ось «из
     * экрана», и поворот вокруг неё разворачивает предмет как картинку:
     * один конец поднимается, другой опускается.
     *
     * <p>Отрицательный угол поворачивает спрайт по часовой стрелке, если
     * смотреть на экран: правый верхний угол (там нарисован конец иглы)
     * уходит вниз-влево, к бутылке во второй руке, а хвост поднимается.
     * Именно это и нужно — игла ложится к горлышку зелья.
     *
     * <p>Для левой руки знак зеркалится множителем {@code side}.
     *
     * <p>Если в игре наклон окажется в другую сторону, поменяйте знак.
     */
    public static final float FILL_TILT = -40.0F;

    /**
     * Сдвиг шприца к бутылке во второй руке (в блоках, по X от своей руки
     * к центру экрана). Без него игла не дотягивается до горлышка: руки
     * разнесены на ширину плеч, а предмет маленький.
     */
    public static final float FILL_INWARD = 0.27F;

    /**
     * Поза для руки с предметом. Всегда {@code ITEM}: арбалетную позу одной
     * руки дорисовывает {@link SyringeArmAnimator} после ванильного
     * {@code setupAnim}.
     */
    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return HumanoidModel.ArmPose.ITEM;
    }

    /**
     * Первое лицо.
     *
     * <p>Два разных положения:
     * <ul>
     *   <li><b>заливка из зелья</b> — шприц поднимается на {@link #FILL_LIFT},
     *       сдвигается к бутылке на {@link #FILL_INWARD} и наклоняется на
     *       {@link #FILL_TILT}: конец иглы опускается к горлышку зелья;</li>
     *   <li><b>укол себе</b> — предмет встаёт в позицию натянутого арбалета.</li>
     * </ul>
     *
     * <p>Вне использования возвращаем {@code false} — обычный замах мечом
     * не ломается.
     */
    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                           ItemStack itemInHand, float partialTick, float equipProcess,
                                           float swingProcess) {
        // Условие ровно как у ванильного натянутого арбалета.
        if (!player.isUsingItem() || player.getUseItemRemainingTicks() <= 0) {
            return false;
        }
        InteractionHand usedHand = player.getUsedItemHand();
        if (armFor(player, usedHand) != arm) {
            return false;
        }

        // Заливка из зелья: шприц поднимается, подтягивается к бутылке во
        // второй руке и наклоняется так, чтобы конец иглы лёг к горлышку.
        if (SyringeEffects.canFillFromPotion(player, usedHand)) {
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            applyItemArmTransform(poseStack, arm, equipProcess);
            // К центру экрана (к бутылке) и вверх.
            poseStack.translate(side * -FILL_INWARD, FILL_LIFT, 0.0F);
            // Игла (правый верхний угол спрайта) вниз-влево, хвост вверх.
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * FILL_TILT));
            return true;
        }

        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        // Базовое положение руки (как applyItemArmTransform в ItemInHandRenderer).
        applyItemArmTransform(poseStack, arm, equipProcess);
        // Позиция «натянутого арбалета» — те же числа, что у ванильного CrossbowItem.
        poseStack.translate(i * -0.4785682F, -0.094387F, 0.05731531F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-11.935F));
        poseStack.mulPose(Axis.YP.rotationDegrees(i * 65.3F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(i * -9.785F));
        return true;
    }

    /**
     * Базовое положение руки в первом лице — те же числа, что у ванильного
     * {@code ItemInHandRenderer#applyItemArmTransform}.
     */
    static void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float equipProcess) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(i * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
    }

    /** Рука, соответствующая указанной руке взаимодействия. */
    static HumanoidArm armFor(Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }
}

