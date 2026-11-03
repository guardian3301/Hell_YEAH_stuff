package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.item.SyringeEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Положение зелья в первом лице при заливке его в шприц.
 *
 * <p>Заливка: во второй руке зелье, в основной — шприц, игрок нажал ПКМ по
 * шприцу. Зелье должно встать туда же, где оно бывает при обычном питье, но
 * без покачивания и без «поднесения ко рту» — статичная поза.
 *
 * <p>Почему через расширение предмета: ванильный {@code ItemInHandRenderer}
 * применяет позу питья только к ТОЙ руке, чей предмет используется
 * ({@code getUsedItemHand() == hand}). Здесь используется шприц, поэтому для
 * зелья во второй руке ванилла берёт обычный замах — его и перекрываем.
 *
 * <p>Если условие не выполнено (обычное питьё, зелье без шприца в другой руке
 * и т.п.), возвращаем {@code false}: дальше работает ванильное поведение,
 * включая полноценную анимацию питья.
 */
public class PotionClientExtensions implements IClientItemExtensions {

    /**
     * Поза «зелье у рта» — это {@code applyEatTransform} ванильного
     * {@code ItemInHandRenderer} при полностью проигранной анимации
     * ({@code f3 = 1}), то есть последний кадр питья. Числа взяты оттуда же.
     */
    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                           ItemStack itemInHand, float partialTick, float equipProcess,
                                           float swingProcess) {
        if (!player.isUsingItem() || player.getUseItemRemainingTicks() <= 0) {
            return false;
        }
        // Заливка идёт только из шприца — иначе это обычное питьё.
        if (!SyringeEffects.isSyringe(player.getUseItem())) {
            return false;
        }
        // Зелье должно быть именно в той руке, которую сейчас рисуем.
        InteractionHand potionHand = SyringeEffects.opposite(player.getUsedItemHand());
        if (SyringeClientExtensions.armFor(player, potionHand) != arm) {
            return false;
        }

        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        // Последний кадр applyEatTransform (f3 = 1) — зелье поднято к лицу.
        poseStack.translate(i * 0.6F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(i * 90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(i * 30.0F));
        // Базовое положение руки — как в ванильном рендере.
        SyringeClientExtensions.applyItemArmTransform(poseStack, arm, equipProcess);
        return true;
    }
}
