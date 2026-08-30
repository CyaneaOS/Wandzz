package com.wandzz.wand;

import com.wandzz.core.CoreType;
import com.wandzz.item.ModComponents;
import net.minecraft.world.entity.player.Player;
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

    /** Rozdzka trzymana przez gracza: najpierw reka glowna, potem druga; null jesli brak. */
    public static ItemStack findWand(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof WandItem) return main;
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof WandItem ? off : null;
    }

    /** Proba wlozenia core'a do rozdzki - zwraca true jesli sie udalo (byl wolny slot). */
    public static boolean insertCore(ItemStack wandStack, CoreType core) {
        WandData current = getData(wandStack);
        if (current.freeSlots() <= 0) return false;
        wandStack.set(ModComponents.WAND_DATA, current.withCoreAdded(core));
        return true;
    }

    /** Wyciagniecie core'a (tylko jesli faktycznie go ma) - do sneak+PPM. */
    public static boolean removeCore(ItemStack wandStack, CoreType core) {
        WandData current = getData(wandStack);
        WandData updated = current.withCoreRemoved(core);
        if (updated == current) return false;
        wandStack.set(ModComponents.WAND_DATA, updated);
        return true;
    }
}
