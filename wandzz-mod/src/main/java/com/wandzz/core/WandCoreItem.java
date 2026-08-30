package com.wandzz.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Przedmiot reprezentujacy jeden rdzen - wklada sie go do wolnego gniazda
 * rozdzki (patrz WandData / WandItem / okno stolika arcanicznego).
 *
 * Minecraft 1.21.11: {@code Item#appendHoverText(ItemStack, TooltipContext,
 * List, TooltipFlag)} zostal zastapiony wersja z {@code TooltipDisplay} i
 * {@code Consumer<Component>} (oznaczona {@code @Deprecated}). Uzywamy jej, a
 * nie komponentu LORE, bo linia ma byc wyliczona z {@link CoreType} - inaczej
 * kazdy z 15 przedmiotow musialby miec w konstruktorze inna reka pisana liste.
 */
public class WandCoreItem extends Item {

    private final CoreType coreType;

    public WandCoreItem(CoreType coreType, Properties properties) {
        super(properties);
        this.coreType = coreType;
    }

    public CoreType coreType() {
        return coreType;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(final ItemStack itemStack, final Item.TooltipContext context,
            final TooltipDisplay display, final Consumer<Component> tooltip, final TooltipFlag flag) {

        tooltip.accept(Component.translatable("wandzz.core.tooltip.level", coreType.level())
                .withStyle(coreType.level() >= 3 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("wandzz.core.tooltip.mana",
                        String.format("%.0f%%", coreType.manaRegenMultiplier() * 100))
                .withStyle(ChatFormatting.GREEN));
        tooltip.accept(Component.translatable("wandzz.core.tooltip.use")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
