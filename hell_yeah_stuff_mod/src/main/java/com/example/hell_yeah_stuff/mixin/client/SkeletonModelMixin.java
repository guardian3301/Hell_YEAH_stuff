package com.example.hell_yeah_stuff.mixin.client;

import com.example.hell_yeah_stuff.entity.SkeletonCrossbowSupport;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Поза рук скелета с мульти-арбалетом — как у разбойника: натяжка
 * ({@code CROSSBOW_CHARGE}) и удержание ({@code CROSSBOW_HOLD}).
 *
 * <p>Ванильный {@link SkeletonModel} умеет только «лук и стрела», а после
 * {@code super.setupAnim} ещё и накладывает взмах рук, если скелет агрессивен
 * и в руке не лук. Поэтому поза накладывается в хвосте метода.
 */
@Mixin(SkeletonModel.class)
public abstract class SkeletonModelMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/Mob;FFFFF)V", at = @At("TAIL"))
    private void hys$crossbowArmPose(Mob entity, float limbSwing, float limbSwingAmount,
                                     float ageInTicks, float netHeadYaw, float headPitch,
                                     CallbackInfo ci) {
        if (!entity.isHolding(stack -> stack.getItem() instanceof CrossbowItem)) return;

        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        ModelPart rightArm = model.rightArm;
        ModelPart leftArm = model.leftArm;
        rightArm.zRot = 0.0F;
        leftArm.zRot = 0.0F;

        boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
        if (SkeletonCrossbowSupport.isChargingCrossbow(entity)) {
            AnimationUtils.animateCrossbowCharge(rightArm, leftArm, entity, rightHanded);
        } else {
            AnimationUtils.animateCrossbowHold(rightArm, leftArm, model.head, rightHanded);
        }
    }
}
