package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Слот тактической кобуры в HUD — справа от хотбара.
 *
 * <p>Это зеркальная копия ванильного слота второй руки: тот же спрайт
 * ({@code hud/hotbar_offhand_right}), та же геометрия и та же глубина
 * (см. {@code Gui#renderItemHotbar}). Ванильный слот для правши рисуется слева
 * от хотбара, поэтому правую сторону мы и занимаем.
 *
 * <p>Рендер включается только когда хотбар реально на экране: подписка идёт на
 * слой {@link VanillaGuiLayers#HOTBAR}, поэтому все ванильные условия видимости
 * (F1, открытое меню, отладочный экран) наследуются автоматически.
 * При пустой кобуре не рисуется ничего.
 */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID, value = Dist.CLIENT)
public final class HolsterHudOverlay {

    /** Ванильный спрайт рамки слота для правой стороны. */
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_offhand_right");

    // Геометрия повторяет Gui#renderItemHotbar, чтобы слот встал вплотную к хотбару.
    /** Половина ширины хотбара: правый край = центр + 91. */
    private static final int HOTBAR_HALF_WIDTH = 91;
    /** Размер рамки слота — как у ванильной. */
    private static final int SLOT_WIDTH = 29;
    private static final int SLOT_HEIGHT = 24;
    /** Рамка прижата к низу экрана: {@code guiHeight - 23}. */
    private static final int SLOT_BOTTOM_OFFSET = 23;
    /** Отступ иконки внутри рамки по X (ванильное +10 от края хотбара). */
    private static final int ITEM_OFFSET_X = 10;
    /** Отступ иконки от низа экрана: {@code guiHeight - 16 - 3}. */
    private static final int ITEM_BOTTOM_OFFSET = 19;

    /**
     * На сколько отодвинуть слот, если правую сторону уже заняла ванильная
     * вторая рука: у леворукого игрока она рисуется справа. Сдвиг равен ширине
     * рамки, чтобы слоты не накладывались.
     */
    private static final int SHIFT_FOR_VANILLA_OFFHAND = SLOT_WIDTH;

    /** Глубина, как у ванильного хотбара: {@code Gui#renderItemHotbar} уводит на -90. */
    private static final float Z_TRANSLATE = -90.0F;

    @SubscribeEvent
    static void onRenderHotbar(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        // В наблюдении ванильный хотбар подменяется своим, и слот кобуры там лишний.
        if (mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        ItemStack holstered = ClientHolsterData.get(player.getUUID());
        // Пустая кобура не рисуется — ни рамки, ни иконки.
        if (holstered.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int centerX = graphics.guiWidth() / 2;
        int height = graphics.guiHeight();

        int slotX = centerX + HOTBAR_HALF_WIDTH;
        // Уступаем место ванильной второй руке, если она на этой же стороне.
        if (player.getMainArm() == HumanoidArm.LEFT && !player.getOffhandItem().isEmpty()) {
            slotX += SHIFT_FOR_VANILLA_OFFHAND;
        }
        int slotY = height - SLOT_BOTTOM_OFFSET;
        int itemX = slotX + ITEM_OFFSET_X;
        int itemY = height - ITEM_BOTTOM_OFFSET;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, Z_TRANSLATE);
        RenderSystem.enableBlend();
        graphics.blitSprite(SLOT_SPRITE, slotX, slotY, SLOT_WIDTH, SLOT_HEIGHT);
        RenderSystem.disableBlend();
        graphics.pose().popPose();

        // Анимацию «подпрыгивания» из ванильного renderSlot не повторяем:
        // у стака в кобуре popTime всегда 0, он не лежит в инвентаре.
        graphics.renderItem(player, holstered, itemX, itemY, 0);
        graphics.renderItemDecorations(mc.font, holstered, itemX, itemY);
    }

    private HolsterHudOverlay() {}
}
