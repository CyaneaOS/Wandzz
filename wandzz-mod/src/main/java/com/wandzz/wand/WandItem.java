package com.wandzz.wand;

import com.wandzz.item.ModComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Rozdzka. Sama w sobie nie "rzuca" zaklec - to robi CastingHandler po
 * stronie serwera, na podstawie gestu rozpoznanego po stronie klienta
 * (patrz WandzzClient / CastingScreen). WandItem tylko przechowuje material
 * i zainstalowane core'y (WandData) jako data component na ItemStacku.
 */
public class WandItem extends Item {

    private final WandMaterial material;

    public WandItem(WandMaterial material, Properties properties) {
        super(properties.component(ModComponents.WAND_DATA, WandData.empty(material)));
        this.material = material;
    }

    public WandMaterial material() {
        return material;
    }

    public static WandData getData(ItemStack stack) {
        WandData data = stack.get(ModComponents.WAND_DATA);
        return data != null ? data : WandData.empty(WandMaterial.NORMAL);
    }

    /** Proba wlozenia core'a do rozdzki - zwraca true jesli sie udalo (byl wolny slot). */
    public static boolean insertCore(ItemStack wandStack, com.wandzz.core.CoreType core) {
        WandData current = getData(wandStack);
        if (current.freeSlots() <= 0) return false;
        wandStack.set(ModComponents.WAND_DATA, current.withCoreAdded(core));
        return true;
    }
}
