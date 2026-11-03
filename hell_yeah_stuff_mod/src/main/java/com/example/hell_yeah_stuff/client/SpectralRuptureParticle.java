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
 * Большая спектральная вспышка — «одна большая частица» разряда
 * спектрального конденсатора.
 *
 * <p>Квад ВСЕГДА лежит горизонтально (как «ковёр» на земле): нормаль квада
 * фиксированно повёрнута вверх, поэтому поворот камеры на ориентацию не
 * влияет (см. {@link #renderRotatedQuad}).
 *
 * <p>Анимация — 10 кадров из {@code particles/spectral_rupture.json} на
 * {@value #FPS} кадров в секунду. Minecraft тикает 20 раз в секунду, поэтому
 * кадр считается вручную по возрасту частицы.
 *
 * <p>Размер растёт от {@value #START_SIZE} до {@value #END_SIZE}
 * полуразмера квада (рисуется {@code 2 * quadSize} блоков), а яркость
 * гаснет к концу жизни — получается «раздувающийся и тающий» взрыв.
 */
public class SpectralRuptureParticle extends TextureSheetParticle {

    /** Начальный полуразмер квада (квад 1.6 блока). */
    private static final float START_SIZE = 0.8F;
    /** Конечный полуразмер квада (квад 5 блоков). */
    private static final float END_SIZE = 2.5F;
    /** Кадров в секунду: 10 кадров при 16 fps. */
    private static final float FPS = 16.0F;
    /** Сколько кадров в анимации (spectral_rupture.json). */
    private static final int FRAMES = 10;

    /** Нормаль квада по умолчанию — он смотрит в +Z. */
    private static final Vector3f QUAD_NORMAL = new Vector3f(0.0F, 0.0F, 1.0F);
    /**
     * Фиксированный поворот «+Z → +Y»: квад становится горизонтальным.
     * Считается один раз на класс — в рендер-потоке лишних аллокаций нет.
     */
    private static final Quaternionf HORIZONTAL =
            new Quaternionf().rotationTo(QUAD_NORMAL, new Vector3f(0.0F, 1.0F, 0.0F));

    private final SpriteSet sprites;

    protected SpectralRuptureParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        // 10 кадров / 16 fps = 12.5 тика -> округляем вверх, чтобы досмотреть кадры.
        this.lifetime = Mth.ceil(FRAMES * 20.0F / FPS);
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.quadSize = START_SIZE;
        // Лёгкий бирюзовый оттенок поверх спрайта.
        this.setColor(0.78F, 0.95F, 1.0F);
        // ОБЯЗАТЕЛЬНО: без спрайта getU0() бросит NPE в рендер-потоке.
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        // setSpriteFromAge раскладывает кадры по lifetime; здесь нужен
        // ровный шаг 16 fps, поэтому кадр выбирается по возрасту вручную.
        this.setSprite(this.sprites.get(currentFrame(), FRAMES - 1));

        float progress = (float) this.age / (float) this.lifetime;
        // Быстрое раздувание в первой трети, затем плавное докручивание.
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        this.quadSize = Mth.lerp(eased, START_SIZE, END_SIZE);
        // Гаснет к концу, чтобы исчезновение не было «щелчком».
        this.alpha = Mth.clamp(1.0F - progress * progress, 0.0F, 1.0F);
    }

    /** Индекс кадра при 20 тиках в секунду и {@value #FPS} кадрах в секунду. */
    private int currentFrame() {
        return Mth.clamp((int) (this.age * FPS / 20.0F), 0, FRAMES - 1);
    }

    /**
     * Игнорируем поворот камеры: вспышка всегда лежит горизонтально.
     * Без этого переопределения ванильный рендер развернул бы квад к камере.
     */
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
            return new SpectralRuptureParticle(level, x, y, z, this.sprites);
        }
    }
}
