package com.example.hell_yeah_stuff.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Горизонтальная частица-ПЛЕЙСХОЛДЕР, остающаяся на месте упавшей после
 * разряда гранаты.
 *
 * <p>Рисуется как «пятно» на земле: квад фиксированно повёрнут нормалью
 * вверх, поэтому камера на ориентацию не влияет (см. {@link #renderRotatedQuad}).
 * В отличие от {@link SpectralRuptureParticle} это не анимация из 10 кадров,
 * а один статичный спрайт, который плавно гаснет — так плейсхолдер легко
 * заменить на финальный арт, не трогая код.
 *
 * <p>Спрайты берутся ванильные ({@code particles/spectral_aftermath.json}),
 * поэтому замена текстуры сводится к правке одного JSON и одной PNG.
 */
public class SpectralAftermathParticle extends TextureSheetParticle {

    /** Полуразмер квада: пятно примерно 2.4 блока в поперечнике. */
    private static final float SIZE = 1.2F;
    /** Сколько тиков живёт пятно (1 секунда). */
    private static final int LIFETIME = 20;

    /** Нормаль квада по умолчанию — он смотрит в +Z. */
    private static final Vector3f QUAD_NORMAL = new Vector3f(0.0F, 0.0F, 1.0F);
    /** Фиксированный поворот «+Z → +Y»: квад ложится горизонтально. */
    private static final Quaternionf HORIZONTAL =
            new Quaternionf().rotationTo(QUAD_NORMAL, new Vector3f(0.0F, 1.0F, 0.0F));

    private final SpriteSet sprites;

    protected SpectralAftermathParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = LIFETIME;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.quadSize = SIZE;
        this.setColor(0.80F, 0.95F, 1.0F);
        // ОБЯЗАТЕЛЬНО: без спрайта getU0() бросит NPE в рендер-потоке.
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        // Плавное угасание без изменения размера: пятно «впечатывается» в пол.
        float progress = (float) this.age / (float) this.lifetime;
        this.alpha = Mth.clamp(1.0F - progress, 0.0F, 1.0F);
    }

    /** Игнорируем поворот камеры: пятно всегда лежит горизонтально. */
    @Override
    protected void renderRotatedQuad(VertexConsumer buffer, Quaternionf cameraRotation,
                                     float x, float y, float z, float partialTick) {
        super.renderRotatedQuad(buffer, HORIZONTAL, x, y, z, partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new SpectralAftermathParticle(level, x, y, z, this.sprites);
        }
    }
}