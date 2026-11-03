package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.entity.SyringeBoltEntity;
import com.example.hell_yeah_stuff.item.SyringeEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Рендер шприц-болта в два слоя — как у предмета с «potion_content».
 *
 * <ul>
 *   <li>{@code syringe_bolt.png} — корпус (не тонируется);</li>
 *   <li>{@code syringe_bolt_content.png} — жидкость, тонируется цветом эффекта.</li>
 * </ul>
 *
 * <p>Пустой болт рисуется только корпусом. Обе текстуры — плейсхолдеры:
 * раскладка UV совпадает с ванильной стрелой (32x32), поэтому арт можно
 * заменить, не трогая код.
 */
public class SyringeBoltRenderer extends ArrowRenderer<SyringeBoltEntity> {

    /** Корпус болта (слой не тонируется). */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            HellYeahStuffMod.MODID, "textures/entity/projectiles/syringe_bolt.png");
    /** Слой жидкости (тонируется цветом эффекта). */
    public static final ResourceLocation CONTENT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            HellYeahStuffMod.MODID, "textures/entity/projectiles/syringe_bolt_content.png");

    /** Текущий проход: корпус или жидкость. */
    private boolean contentPass;
    /** Цвет вершин текущего прохода (-1 — без тонировки, белый). */
    private int tint = -1;

    public SyringeBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SyringeBoltEntity entity) {
        return this.contentPass ? CONTENT_TEXTURE : TEXTURE;
    }

    @Override
    public void render(SyringeBoltEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        int color = entity.getColor();

        // Слой 1: корпус, без тонировки.
        this.contentPass = false;
        this.tint = -1;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        // Слой 2: жидкость, только если болт заряжен.
        if (color != SyringeEffects.NO_COLOR) {
            this.contentPass = true;
            this.tint = color;
            try {
                super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            } finally {
                this.contentPass = false;
                this.tint = -1;
            }
        }
    }

    /** Тот же квад, что у ванильного ArrowRenderer, но с цветом слоя. */
    @Override
    public void vertex(PoseStack.Pose pose, VertexConsumer consumer, int x, int y, int z,
                       float u, float v, int normalX, int normalY, int normalZ, int packedLight) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(this.tint)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, (float) normalX, (float) normalY, (float) normalZ);
    }
}


