package com.wandzz.item;

import com.wandzz.Wandzz;
import com.wandzz.core.CoreType;
import com.wandzz.core.WandCoreItem;
import com.wandzz.wand.WandItem;
import com.wandzz.wand.WandMaterial;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.Map;

/**
 * Rejestracja przedmiotow (rozdzek i core'ow).
 *
 * Minecraft 1.21.11 + oficjalne mapowania Mojanga: klasa nazywa sie
 * {@code net.minecraft.resources.Identifier} (wczesniej ResourceLocation),
 * a rejestry to {@code BuiltInRegistries} + {@code Registry#register}.
 */
public final class ModItems {

    public static final Map<WandMaterial, WandItem> WANDS = new EnumMap<>(WandMaterial.class);
    public static final Map<CoreType, WandCoreItem> CORES = new EnumMap<>(CoreType.class);

    public static void bootstrap() {
        for (WandMaterial material : WandMaterial.values()) {
            WandItem wand = new WandItem(material, new Item.Properties().stacksTo(1));
            Registry.register(BuiltInRegistries.ITEM,
                    Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, material.translationKey()), wand);
            WANDS.put(material, wand);
        }

        for (CoreType core : CoreType.values()) {
            WandCoreItem coreItem = new WandCoreItem(core, new Item.Properties().stacksTo(1));
            Registry.register(BuiltInRegistries.ITEM,
                    Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, core.translationKey()), coreItem);
            CORES.put(core, coreItem);
        }
    }

    private ModItems() {
    }
}
