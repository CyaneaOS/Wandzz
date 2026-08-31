package com.wandzz.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

/**
 * Zywica arkanu. Przedmiot bez logiki - caly jej ciezar lezy w {@code WandData}
 * (flaga {@code resinated}) i w {@code WandItem#applyResin}; to jest wylacznie
 * miejsce, gdzie tlumaczymy graczowi, po co to komu.
 *
 * Nadpisujemy {@code appendHoverText} w wersji z {@code TooltipDisplay} -
 * 1.21.11 odswiezyla sygnature i stara (List<Component>) juz nie jest wywolywana,
 * a TooltipDisplay jest MUTOWALNY, wiec linie dokladamy przez {@code accept} na
 * dostarczonym konsumentie (wzor: WandItem#appendHoverText).
 */
public class ArcaneResinItem extends Item {

    public ArcaneResinItem(final Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(final ItemStack itemStack, final Item.TooltipContext context,
            final TooltipDisplay display, final Consumer<Component> tooltip, final TooltipFlag flag) {

        tooltip.accept(Component.translatable("wandzz.resin.tooltip"));
        tooltip.accept(Component.translatable("wandzz.resin.source"));
        tooltip.accept(Component.translatable("wandzz.attune.desc"));
    }
}
