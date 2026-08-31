package com.wandzz.world;

import com.wandzz.Wandzz;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
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

    /**
     * Typ feature'u {@code wandzz:arcane_strangler} - JEDYNY punkt moda, gdzie
     * worldgen NIE jest czystym JSON-em. Oplatanie korony gospodarza nie da sie
     * opisac vanilla {@code minecraft:tree}, a {@code TrunkPlacerType} ma w
     * 1.21.11 prywatny konstruktor (wlasny placer = access widener, ktorego ten
     * projekt swiadomie nie uzywa). Rejestr {@code FEATURE} nie jest rejestrem
     * datapackowym, wiec wpis idzie przez {@code Registry.register} w kodzie -
     * tak samo, jak vanilla robi to w statycznym bloku {@code Feature}.
     */
    public static final Feature<?> ARCANE_STRANGLER = Registry.register(
            BuiltInRegistries.FEATURE,
            ResourceKey.create(Registries.FEATURE, id("arcane_strangler")),
            new ArcaneStranglerFeature(NoneFeatureConfiguration.CODEC));

    /** Glada jednorozcow (overworld) i oltarz Chronosa (Arkanum). */
    public static final Feature<?> UNICORN_GLADE = Registry.register(
            BuiltInRegistries.FEATURE,
            ResourceKey.create(Registries.FEATURE, id("unicorn_glade")),
            new GladeFeature(NoneFeatureConfiguration.CODEC));

    public static final Feature<?> CHRONOS_ALTAR = Registry.register(
            BuiltInRegistries.FEATURE,
            ResourceKey.create(Registries.FEATURE, id("chronos_altar")),
            new ChronosAltarFeature(NoneFeatureConfiguration.CODEC));

    /**
     * Klucz umieszczania glady. Trafia do biomonow nadziemnych przez
     * {@link BiomeModifications#addFeature} ponizej; oltarz NIE - on siedzi w
     * {@code data/wandzz/worldgen/biome/arcane_forest.json}, bo Arkanum ma wlasny,
     * `fixed` biome i addFeature by go nie dotknal.
     */
    public static final ResourceKey<PlacedFeature> UNICORN_GLADE_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE, id("unicorn_glade"));

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

        // WEGETAL_DECORATION, nie UNDERGROUND_ORES: glada to cos na powierzchni i
        // musi isc PO terenie, inaczej kwiaty roslyby w powietrzu nad urwiskiem.
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.VEGETAL_DECORATION, UNICORN_GLADE_PLACED);
        Wandzz.LOGGER.info("Wandzz: arcane_ember wtrysniety do biomesow nadziemnych (UNDERGROUND_ORES)");
        // ARCANE_STRANGLER zarejestrowal sie przy ladowaniu tej klasy; logujemy
        // klucz, bo to on decyduje, czy configured_feature/arcane_tree.json w ogole
        // sie zdekoduje (zly id = "Failed to load registries due to errors").
        Wandzz.LOGGER.info("Wandzz: typ feature'u {} zarejestrowany",
                String.valueOf(BuiltInRegistries.FEATURE.getKey(ARCANE_STRANGLER)));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, path);
    }

    private ModWorldgen() {
    }
}
