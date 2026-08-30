package com.wandzz.item;

import com.wandzz.Wandzz;
import com.wandzz.core.CoreType;
import com.wandzz.core.WandCoreItem;
import com.wandzz.wand.WandItem;
import com.wandzz.wand.WandMaterial;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.Map;

/**
 * Rejestracja przedmiotow (rozdziek i core'ow).
 *
 * Minecraft 1.21.11 + oficjalne mapowania Mojanga:
 *   - {@code net.minecraft.resources.Identifier} (wczesniej ResourceLocation),
 *   - rejestry przez {@code BuiltInRegistries} + {@code Registry#register},
 *   - od 1.21.2 kazdy przedmiot musi znac swoj ResourceKey PRZED konstrukcja,
 *     bo Item#&lt;init&gt; wylicza z niego descriptionId i model (patrz register nizej).
 */
public final class ModItems {

    public static final Map<WandMaterial, WandItem> WANDS = new EnumMap<>(WandMaterial.class);
    public static final Map<CoreType, WandCoreItem> CORES = new EnumMap<>(CoreType.class);

    public static void bootstrap() {
        for (WandMaterial material : WandMaterial.values()) {
            ResourceKey<Item> key = itemKey(material.translationKey());
            WandItem wand = new WandItem(material, baseProperties(key));
            Registry.register(BuiltInRegistries.ITEM, key, wand);
            WANDS.put(material, wand);
        }

        for (CoreType core : CoreType.values()) {
            ResourceKey<Item> key = itemKey(core.translationKey());
            WandCoreItem coreItem = new WandCoreItem(core, baseProperties(key));
            Registry.register(BuiltInRegistries.ITEM, key, coreItem);
            CORES.put(core, coreItem);
        }
    }

    /**
     * Klucz zasobu przedmiotu, np. {@code wandzz:wand_normal}. Vanilla robi
     * dokladnie to samo w {@code Items#vanillaItemId} (Registries.ITEM + ResourceKey#create).
     */
    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, path));
    }

    /**
     * Bez {@code setId(...)} gra crashuje przy starcie:
     * NullPointerException "Item id not set" w Item.Properties#effectiveDescriptionId,
     * bo konstruktor Item czyta id, zeby zbudowac klanges opisu i nazwe modelu.
     * Dlatego wlasciwosc ustawiamy ZANIM powstanie obiekt przedmiotu - tak jak
     * w vanilla, gdzie Items#registerItem wola {@code factory.apply(properties.setId(key))}.
     */
    private static Item.Properties baseProperties(ResourceKey<Item> key) {
        return new Item.Properties().stacksTo(1).setId(key);
    }

    private ModItems() {
    }
}
