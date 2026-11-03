package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.item.SyringeEffects;
import com.example.hell_yeah_stuff.registry.ModItems;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Арбалетная поза ТОЛЬКО для руки со шприцем.
 *
 * <p>Ванильные {@code ArmPose.CROSSBOW_CHARGE} и {@code CROSSBOW_HOLD} помечены
 * как двуручные: {@code AnimationUtils.animateCrossbowCharge/Hold} выставляют
 * углы обеим рукам (вторая «натягивает тетиву»). Для шприца это лишнее — поза
 * нужна лишь той руке, в которой он лежит.
 *
 * <p>Поэтому {@code getArmPose} отдаёт обычный {@code ITEM}, а нужные углы
 * ставятся здесь — в хвосте {@code PlayerModel#setupAnim}, после всей
 * ванильной анимации (иначе ванилла перезаписала бы углы).
 *
 * <p>Поза ставится ТОЛЬКО во время укола (ПКМ, {@code UseAnim} завершённого
 * использования): {@code xRot = -0.97079635}, {@code yRot = ∓0.8} — числа из
 * {@code AnimationUtils} для ДЕРЖАЩЕЙ руки. Заряженный (готовый к удару)
 * предмет никакой позы не получает — он лежит в руке как обычный меч.
 *
 * <p>Рукав скина копируется с руки в конце ванильного {@code setupAnim}, то есть
 * ДО нашей правки — поэтому синхронизируем его заново.
 */
public final class SyringeArmAnimator {

    /** Угол руки при уколе (натяжение арбалета). */
    private static final float CHARGE_X_ROT = -0.97079635F;
    /** Боковой разворот руки при уколе. */
    private static final float CHARGE_Y_ROT = 0.8F;

    private SyringeArmAnimator() {
    }

    public static void pose(PlayerModel<?> model, LivingEntity entity) {
        if (entity == null) {
            return;
        }

        // Укол (ПКМ): рука с предметом — в позе натяжения арбалета.
        // Готовый к удару (заряженный) предмет позы не получает.
        //
        // Заливка из зелья — не укол: шприц просто держится в руке, поэтому
        // арбалетная поза туда не применяется (в первом лице он поднимается
        // на несколько единиц, см. SyringeClientExtensions).
        if (entity.isUsingItem() && entity.getUseItemRemainingTicks() > 0
                && isSyringe(entity.getUseItem())
                && !SyringeEffects.canFillFromPotion(entity, entity.getUsedItemHand())) {
            applyCharge(model, entity, entity.getUsedItemHand());
        }
    }

    private static boolean isSyringe(ItemStack stack) {
        return stack.is(ModItems.BATTLE_SYRINGE.get()) || stack.is(ModItems.SYRINGE_BOLT.get());
    }

    /** Углы натяжения арбалета — только держащей руке. */
    private static void applyCharge(PlayerModel<?> model, LivingEntity entity, InteractionHand hand) {
        HumanoidArm arm = armFor(entity, hand);
        ModelPart part = armPart(model, arm);
        boolean right = arm == HumanoidArm.RIGHT;
        part.xRot = CHARGE_X_ROT;
        part.yRot = right ? -CHARGE_Y_ROT : CHARGE_Y_ROT;
        syncSleeve(model, arm, part);
    }

    private static HumanoidArm armFor(LivingEntity entity, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND
                ? entity.getMainArm()
                : entity.getMainArm().getOpposite();
    }

    private static ModelPart armPart(PlayerModel<?> model, HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
    }

    private static void syncSleeve(PlayerModel<?> model, HumanoidArm arm, ModelPart part) {
        ModelPart sleeve = arm == HumanoidArm.RIGHT ? model.rightSleeve : model.leftSleeve;
        sleeve.copyFrom(part);
    }
}
