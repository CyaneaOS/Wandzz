package com.wandzz.world;

import com.wandzz.Wandzz;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.Optional;

/**
 * Klucze swiata generowanego przez dane (JSON), nie przez kod - plus jeden
 * wyjatek: {@link #bootstrap()}, ktore wtryskuje feature do biomesow vanilla.
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

    /**
     * Ten sam wymiar, ale jako klucz rejestru `dimension` -ego jest to, czego
     * potrzebuje {@code MinecraftServer#getLevel(...)} przy teleportacji (patrz
     * GateService). {@link #ARKANUM} (LEVEL_STEM) opisuje wpis JSON-a, ten klucz
     * wskazuje na dzialajacy serwer swiata.
     */
    public static final ResourceKey<Level> ARKANUM_LEVEL = ResourceKey.create(
            Registries.DIMENSION, id("arkanum"));

    /** Zily Arkannego Zaru w jeziorach lawy (data/wandzz/worldgen/placed_feature). */
    public static final ResourceKey<PlacedFeature> ARCANE_EMBER_VEIN = ResourceKey.create(
            Registries.PLACED_FEATURE, id("arcane_ember"));

    /**
     * Wtrysniecie feature'u do biomesow vanilla.
     *
     * Dlaczego kod, a nie JSON w data/wandzz? Wlasny `minecraft:biome_modifier`
     * wymagalby wlasnego codec'a zarejestrowanego w `Registries.BIOME_MODIFIER_TYPE`
     * (czyli tego samego rejestru, ktorego nadpisywanie jest wrazliwe na
     * kolejnosc), a TerraBlendera nie ma w zaleznosciach. {@code
     * BiomeModifications.addFeature} robi dokladnie to samo po stronie Fabric API
     * i nie dotyka zadnego pliku JSON.
     *
     * Selektor {@code foundInOverworld()} = tylko biomes, ktore faktycznie
     * generuja sie w swiecie nadziemnym; Arkanum (wlasny, `fixed` biome) zostaje
     * czyste - brama powrotna jest tam stawiana recznie przez GateService.
     */
    public static void bootstrap() {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES, ARCANE_EMBER_VEIN);
        Wandzz.LOGGER.info("Wandzz: arcane_ember wtrysniety do biomesow nadziemnych (UNDERGROUND_ORES)");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, path);
    }

    private ModWorldgen() {
    }
}
