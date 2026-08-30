package com.wandzz.world;

import com.wandzz.Wandzz;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Optional;

/**
 * Klucze swiata generowanego przez dane (JSON), nie przez kod.
 *
 * 1.21.11: drzewo z sadzonki nie jest klasa `Tree`, tylko odwolaniem do
 * `minecraft:worldgen/configured_feature` - wiec `TreeGrower` potrzebuje
 * WYLACZNIE klucza, a sam feature opisujemy w `data/wandzz/worldgen/`
 * ( configured_feature/arcane_tree.json ). Brak rejestracji w kodzie = brak
 * ryzyka, ze cos sie nie zdazy przed ladowaniem data-packow.
 */
public final class ModWorldgen {

    /** `wandzz:arcane_tree` w rejestrze `worldgen/configured_feature`. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> ARCANE_TREE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, id("arcane_tree"));

    /**
     * Sadzonka arkanska: jedno drzewo (bez wariantu "mega", bez kwiatow).
     * Konstruktor 4-argumentowy = (nazwa, drzewoMega, drzewo, kwiaty).
     */
    public static final TreeGrower ARCANE_TREE_GROWER = new TreeGrower(
            "arcane_tree", Optional.empty(), Optional.of(ARCANE_TREE), Optional.empty());

    /** Wymiar `wandzz:arkanum` (data/wandzz/dimension/arkanum.json). */
    public static final ResourceKey<LevelStem> ARKANUM = ResourceKey.create(
            Registries.LEVEL_STEM, id("arkanum"));

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, path);
    }

    private ModWorldgen() {
    }
}
