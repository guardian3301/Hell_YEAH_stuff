package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.item.SpectralCapacitorItem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

/**
 * Полоска заряда спектрального конденсатора — та же плашка, что у
 * арбалетов ({@link MagazineBarDecorator}), но нарисованная РОВНО на месте
 * ванильной шкалы прочности ({@code y + 13}): у конденсатора прочности нет,
 * поэтому место свободно и полоска выглядит как «родная».
 *
 * <p>Длина заливки = прогресс движения (0..100 %). Незаряженная граната
 * показывает пустую (но видимую) плашку-подсказку, заряженная — полностью
 * залитую бирюзовым.
 */
public final class CapacitorBarDecorator implements IItemDecorator {

    private static final int CHARGING = 0xFF40E0D0; // бирюзовый — идёт заряд
    private static final int CHARGED = 0xFF6FF7FF;  // яркий — готово
    private static final int BG = 0xFF000000;

    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        float progress = SpectralCapacitorItem.chargeProgress(stack);
        boolean charged = SpectralCapacitorItem.isCharged(stack);

        // Те же координаты, что у ванильной шкалы прочности: x+2, y+13.
        int x = xOffset + 2;
        int y = yOffset + 13;
        // Поверх предмета (иначе полоска прячется за спрайтом).
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 200);
        // Чёрная подложка 13x2 — как у ванильной шкалы прочности.
        guiGraphics.fill(x, y, x + 13, y + 2, BG);
        // Заливка: минимум 1px, чтобы прогресс был заметен сразу.
        int width = charged ? 13 : Math.max(1, Math.round(progress * 13.0F));
        guiGraphics.fill(x, y, x + width, y + 1, charged ? CHARGED : CHARGING);
        guiGraphics.pose().popPose();
        return true;
    }
}