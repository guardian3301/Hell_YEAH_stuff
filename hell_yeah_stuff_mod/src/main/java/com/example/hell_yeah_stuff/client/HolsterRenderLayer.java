package com.example.hell_yeah_stuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Слой тактической кобуры: убранный в кобуру предмет висит на правом бедре игрока.
 *
 * <p>Предмет рисуется ровно так же, как ванильный предмет во вторичной руке
 * (см. {@code ItemInHandLayer#renderArmWithItem}): контекст
 * {@link ItemDisplayContext#THIRD_PERSON_LEFT_HAND}, флаг «левая рука» и повороты
 * {@code X(-90)} / {@code Y(180)}. Поэтому масштаб, наклон и смена модели
 * (заряженный арбалет, натянутый лук) берутся из display-трансформов самого предмета,
 * а не подбираются здесь, и предметы со своими
 * {@code thirdperson_lefthand} (арбалеты мода) выглядят как в руке.
 *
 * <p>Поверх ванильной цепочки добавлен переворот «вверх ногами» — {@code Z(180)}.
 * После поворотов {@code X(-90)} и {@code Y(180)} ось Z рабочего пространства
 * смотрит вертикально вверх в системе координат тела, то есть совпадает с
 * нормалью плоскости, в которой лежит спрайт (в третьем лице ванильные предметы
 * рисуются плашмя, лицом вверх). Поэтому {@code Z(180)} разворачивает предмет
 * вокруг вертикали: верх спрайта уходит назад вместо вперёд, а наклон из
 * display-трансформа сохраняется. Для {@code item/generated} это ровно
 * переворот в плоскости спрайта; для {@code item/handheld} — разворот на 180°
 * с сохранением угла наклона (клинок/рукоять меняются местами).
 *
 * <p>Якорь задан в системе координат модели тела ({@code PlayerModel#body},
 * точка крепления — плечи, {@code y = 0}):
 * <ul>
 *   <li>{@code +X} — влево игрока (правая сторона — минус по X),</li>
 *   <li>{@code +Y} — вниз (таз на {@code 0.75}, бедро ниже),</li>
 *   <li>{@code +Z} — назад (как у ванильного плаща: {@code z = +0.125} за спиной),</li>
 *   <li>единица — блок (16 пикселей модели).</li>
 * </ul>
 * Все числа собраны в константах ниже — подгонять внешний вид надо здесь.
 */
public class HolsterRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** Контекст отрисовки вторичной руки — как у ванильного offhand-предмета. */
    private static final ItemDisplayContext OFFHAND_CONTEXT = ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    /** Ванильное зеркало display-трансформа: {@code true} для левой (вторичной) руки. */
    private static final boolean OFFHAND_MIRROR = true;

    // Правое бедро: сторона правой ноги — минус по X (куб ноги занимает
    // X от -0.244 до 0.006, пивот -1.9 px, ширина 4 px), таз на 0.75.
    // Значение подобрано в игре: центр спрайта у внешней поверхности бедра.
    private static final float HIP_X = -0.27F;
    // Чуть ниже пояса: таз на 0.75 (пивот ноги), бедро идёт вниз.
    private static final float HIP_Y = 0.85F;
    private static final float HIP_Z = 0.0F;

    // Ванильная цепочка поворотов вторичной руки (ItemInHandLayer#renderArmWithItem).
    private static final float OFFHAND_ROT_X_DEGREES = -90.0F;
    private static final float OFFHAND_ROT_Y_DEGREES = 180.0F;

    // Переворот «вверх ногами»: 180° вокруг нормали спрайта, лицевая грань не меняется.
    private static final float UPSIDE_DOWN_DEGREES = 180.0F;

    /**
     * Доворот арбалетов вокруг вертикали (ординаты) на 90°.
     * Применяется в системе координат тела, то есть вращает предмет вокруг
     * вертикальной оси игрока — арбалет разворачивается поперёк бедра,
     * а не вдоль него.
     *
     * <p>Знак отрицательный: кобура на правом бедре ({@code -X}), наружу смотрит
     * {@code -X}, а лицевая грань спрайта односторонняя. При {@code +90} она
     * развернулась бы в тело и арбалет стал бы невидим снаружи.
     */
    private static final float CROSSBOW_ROT_Y_DEGREES = -90.0F;

    /**
     * Множитель к масштабу из display-трансформа предмета.
     * Итоговый масштаб = масштаб предмета × {@code SCALE} (0.9), то есть
     * {@code item/generated} — 0.55 × 0.9 ≈ 0.50,
     * {@code item/handheld} — 0.85 × 0.9 ≈ 0.77,
     * арбалеты мода — 0.9 × 0.9 ≈ 0.81 из их собственного {@code thirdperson_lefthand}.
     */
    private static final float SCALE = 0.9F;

    public HolsterRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack holstered = ClientHolsterData.get(player.getUUID());
        if (holstered.isEmpty() || player.isInvisible()) return;

        poseStack.pushPose();
        // Якорь к телу: поворачивается вместе с корпусом и наклоняется при приседе.
        this.getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(HIP_X, HIP_Y, HIP_Z);
        // Арбалеты доворачиваются вокруг вертикали (ординаты) — поперёк бедра.
        // Поворот ставится до ванильной цепочки, поэтому вращается весь предмет
        // вокруг оси Y модели тела, а не вокруг его собственных осей.
        if (holstered.getItem() instanceof CrossbowItem) {
            poseStack.mulPose(Axis.YP.rotationDegrees(CROSSBOW_ROT_Y_DEGREES));
        }
        // Ванильная цепочка вторичной руки.
        poseStack.mulPose(Axis.XP.rotationDegrees(OFFHAND_ROT_X_DEGREES));
        poseStack.mulPose(Axis.YP.rotationDegrees(OFFHAND_ROT_Y_DEGREES));
        // Переворот верх ногами — последним, уже в плоскости спрайта.
        poseStack.mulPose(Axis.ZP.rotationDegrees(UPSIDE_DOWN_DEGREES));
        poseStack.scale(SCALE, SCALE, SCALE);
        // Владелец передаётся как сущность, чтобы предикаты моделей
        // (заряд арбалета, натяжение лука) считались по нему, как у предмета в руке.
        // Seed считается так же, как в ItemInHandRenderer#renderItem.
        Minecraft.getInstance().getItemRenderer().renderStatic(
                player,
                holstered,
                OFFHAND_CONTEXT,
                OFFHAND_MIRROR,
                poseStack,
                buffer,
                player.level(),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                player.getId() + OFFHAND_CONTEXT.ordinal());
        poseStack.popPose();
    }
}
