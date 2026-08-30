package com.wandzz.item;

import com.wandzz.core.CoreType;
import com.wandzz.core.WandCoreItem;
import com.wandzz.wand.WandItem;
import com.wandzz.wand.WandMaterial;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {

    public static final Map<WandMaterial, WandItem> WANDS = new EnumMap<>(WandMaterial.class);
    public static final Map<CoreType, WandCoreItem> CORES = new EnumMap<>(CoreType.class);

    public static void bootstrap() {
        for (WandMaterial material : WandMaterial.values()) {
            WandItem wand = new WandItem(material, new Item.Properties().stacksTo(1));
            Registry.register(BuiltInRegistries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("wandzz", material.translationKey()), wand);
            WANDS.put(material, wand);
        }

        for (CoreType core : CoreType.values()) {
            WandCoreItem coreItem = new WandCoreItem(core, new Item.Properties().stacksTo(1));
            Registry.register(BuiltInRegistries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("wandzz", core.translationKey()), coreItem);
            CORES.put(core, coreItem);
        }
    }

    private ModItems() {
    }
}
