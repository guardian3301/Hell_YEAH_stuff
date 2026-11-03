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
     * Подъём шприца при заливке из зелья — 4 единицы модели (4/16 = 0.25 блока).
     * В первом лице единица PoseStack равна одному пикселю текстуры предмета,
     * поэтому переводим 4 единицы в 0.25.
     */
    public static final float FILL_LIFT = 4.0F / 16.0F;

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
     *   <li><b>заливка из зелья</b> — шприц поднимается на {@link #FILL_LIFT}
     *       (4 единицы = 4/16 блока) вверх, рука при этом в обычном положении;</li>
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

        // Заливка из зелья: шприц просто поднимается над обычным положением руки.
        if (SyringeEffects.canFillFromPotion(player, usedHand)) {
            applyItemArmTransform(poseStack, arm, equipProcess);
            poseStack.translate(0.0F, FILL_LIFT, 0.0F);
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

