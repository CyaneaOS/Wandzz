package com.wandzz.core;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/**
 * Przedmiot reprezentujacy jeden core - wklada sie go do wolnego slotu
 * rozdzki (patrz WandData / WandItem). Sam w sobie nie robi nic poza
 * przenoszeniem CoreType.
 *
 * Minecraft 1.21.11: {@code Item#appendHoverText(ItemStack, TooltipContext,
 * List, TooltipFlag)} zostal zastapiony przez {@code appendHoverText(ItemStack,
 * TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)} i jest
 * oznaczony jako {@code @Deprecated}. Zwykla, "deklaracyjna" linia tooltipa
 * nie potrzebuje nadpisywania metody - wystarczy komponent LORE.
 */
public class WandCoreItem extends Item {

    private final CoreType coreType;

    public WandCoreItem(CoreType coreType, Properties properties) {
        super(properties.component(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("wandzz.core.level", coreType.level())))));
        this.coreType = coreType;
    }

    public CoreType coreType() {
        return coreType;
    }
}
