package com.wandzz.core;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Przedmiot reprezentujacy jeden core - wklada sie go do wolnego slotu
 * rozdzki (patrz WandData / WandItem). Sam w sobie nie robi nic poza
 * przenoszeniem CoreType.
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
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                 List<Component> tooltip, TooltipFlag type) {
        tooltip.add(Component.translatable("wandzz.core.level", coreType.level()));
    }
}
