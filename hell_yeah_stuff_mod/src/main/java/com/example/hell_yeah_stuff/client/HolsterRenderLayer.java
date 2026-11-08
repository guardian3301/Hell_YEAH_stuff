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
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/**
 * Слой тактической кобуры: убранный в кобуру предмет висит на правом бедре игрока.
 *
 * <p>Предмет рисуется тем же путём, что и ванильный предмет во вторичной руке
 * (см. {@code ItemInHandLayer#renderArmWithItem}): контекст
 * {@link ItemDisplayContext#THIRD_PERSON_LEFT_HAND} и флаг «левая рука». Поэтому
 * масштаб, наклон и смена модели (заряженный арбалет, натянутый лук) берутся из
 * display-трансформов самого предмета, а не подбираются здесь, и предметы со своими
 * {@code thirdperson_lefthand} (арбалеты мода) выглядят как в руке.
 *
 * <p><b>Все повороты задаются в одном месте — {@link Adjust}</b>, по одной настройке
 * на категорию: {@code CROSSBOW_ADJUST}, {@code SWORD_ADJUST}, {@code TOOL_ADJUST},
 * {@code DEFAULT_ADJUST}.
 * Ванильная база {@code X(-90)} / {@code Y(180)} уже вписана в их углы, поэтому
 * отдельных глобальных поправок нет: иначе одни и те же углы жили бы в двух местах,
 * и правка меча сдвигала бы арбалет.
 *
 * <p>Порядок внутри {@code Adjust}: курс (Y) → тангаж (X) → крен (Z).
 * Курс разворачивает предмет вокруг вертикали тела (переворот «вверх ногами»
 * или поперёк бедра), крен наклоняет его к игроку.
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

    // Правое бедро. Нога-куб занимает X от -0.244 до 0.006 (пивот -1.9 px, ширина 4 px).
    // Значения подобраны в игре.
    private static final float HIP_X = -0.27F;
    private static final float HIP_Y = 0.75F;
    private static final float HIP_Z = 0.1F;
    
    /**
     * Ориентация предмета на бедре: курс, тангаж и крен в градусах.
     *
     * <p>Задаёт полную ориентацию предмета в осях модели тела
     * ({@code +X} — влево, {@code +Y} — вниз, {@code +Z} — назад), а не в осях
     * самого предмета. Ванильная база {@code X(-90)} / {@code Y(180)} уже
     * вписана в углы:
     * <ul>
     *   <li><b>курс</b> ({@code yaw}, ось Y) — поворот вокруг вертикали:
     *       предмет ложится поперёк бедра или вдоль него;</li>
     *   <li><b>тангаж</b> ({@code pitch}, ось X) — наклон вперёд/назад:
     *       рукоять выше или ниже клинка;</li>
     *   <li><b>крен</b> ({@code roll}, ось Z) — вращение вокруг оси «к игроку»:
     *       именно он разворачивает спрайт лицом к телу или от него.</li>
     * </ul>
     *
     * <p>Порядок применения — курс, тангаж, крен. Каждая категория настраивается
     * независимо, поэтому подгонка меча больше не сдвигает арбалет.
     */
    private record Adjust(float yaw, float pitch, float roll) {

        private void apply(PoseStack poseStack) {
            if (this.yaw != 0.0F) {
                poseStack.mulPose(Axis.YP.rotationDegrees(this.yaw));
            }
            if (this.pitch != 0.0F) {
                poseStack.mulPose(Axis.XP.rotationDegrees(this.pitch));
            }
            if (this.roll != 0.0F) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(this.roll));
            }
        }
    }

    /**
     * Арбалеты: лежат поперёк бедра, наклонены к игроку.
     *
     * <p>Углы включают ванильную базу, поэтому читаются как одна поза:
     * курс {@code 180} кладёт арбалет поперёк бедра, тангаж {@code 60}
     * наклоняет вперёд, крен {@code -90} доворачивает к игроку.
     */
    private static final Adjust CROSSBOW_ADJUST = new Adjust(180.0F, 60.0F, -90.0F);
    private static final Adjust SWORD_ADJUST = new Adjust(-90.0F, 90.0F, -90.0F);

    /**
     * Инструменты ({@link DiggerItem}: кирка, топор, лопата, тяпка) — висят
     * головой вниз вдоль бедра, рукоятью вверх.
     *
     * <p>Вынесены отдельно от мечей намеренно: у инструментов длинная рукоять
     * и массивная голова на конце, поэтому одна и та же поза читается иначе.
     * Курс {@code 180} разворачивает голову вниз, тангаж {@code 90} прижимает
     * инструмент к бедру, крен {@code -90} доворачивает рабочей частью к игроку.
     *
     * <p>Значения подобраны «на глаз» и правятся здесь же: меняйте только эту
     * строку — мечи ({@code SWORD_ADJUST}) и арбалеты ({@code CROSSBOW_ADJUST})
     * от неё не поедут.
     */
    private static final Adjust TOOL_ADJUST = new Adjust(180.0F, 90.0F, -90.0F);

    private static final Adjust DEFAULT_ADJUST = new Adjust(-90.0F, 60.0F, -90.0F);

    /**
     * Множитель к масштабу из display-трансформа предмета.
     * Итоговый масштаб = масштаб предмета × {@code SCALE} (0.9), то есть
     * {@code item/generated} — 0.55 × 0.9 ≈ 0.50,
     * {@code item/handheld} — 0.85 × 0.9 ≈ 0.77,
     * арбалеты мода — 0.9 × 0.9 ≈ 0.81 из их собственного {@code thirdperson_lefthand}.
     */
    private static final float SCALE = 0.9F;

    /**
     * Настройка под тип предмета: арбалеты, мечи, инструменты и всё остальное —
     * раздельно, чтобы подгонка одной категории не сдвигала остальные.
     *
     * <p>{@code SwordItem} и {@code DiggerItem} — соседние ветки одной иерархии
     * (оба наследуют {@code TieredItem}), поэтому пересечений между ними нет:
     * порядок проверок на результат не влияет. Инструменты всё равно стоят
     * отдельной веткой до {@code DEFAULT_ADJUST} — иначе кирка с топором попали
     * бы в общий пресет.
     */
    private static Adjust adjustFor(ItemStack stack) {
        if (stack.getItem() instanceof CrossbowItem) return CROSSBOW_ADJUST;
        if (stack.getItem() instanceof SwordItem) return SWORD_ADJUST;
        if (stack.getItem() instanceof DiggerItem) return TOOL_ADJUST;
        return DEFAULT_ADJUST;
    }

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
        // Полная ориентация предмета: курс, тангаж, крен в осях тела.
        // Ванильная база X(-90)/Y(180) уже внутри Adjust, отдельных поворотов нет.
        adjustFor(holstered).apply(poseStack);
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
