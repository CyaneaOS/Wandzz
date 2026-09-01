package com.wandzz.block;

import com.wandzz.Wandzz;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

/**
 * Drewno arkanskie (log + deski + liscie + sadzonka) oraz stol arcaniczny.
 *
 * 1.21.11 (Mojmap) - blok, tak jak przedmiot, MUSI znac swoj ResourceKey
 * PRZED konstrukcja: {@code BlockBehaviour.Properties#setId}, inaczej start gry
 * konczy sie {@code NullPointerException: Block id not set} (descriptionId i loot
 * table sa z tego klucza liczane). Dlatego wlasciwosci buduje SIE W SRODKU
 * register(...) i podaje fabryce - dokladnie jak w vanilla {@code Blocks#registerBlock}.
 *
 * Liscie i sadzonka sa podlaczone do swiata wylacznie danymi (JSON) - patrz
 * {@code data/wandzz/worldgen/} i {@code com.wandzz.world.ModWorldgen}.
 */
public final class ModBlocks {

    public static ArcaneLogBlock ARCANE_LOG;
    public static RotatedPillarBlock ARCANE_LOG_STRIPPED;
    public static Block ARCANE_PLANKS;
    public static LeavesBlock ARCANE_LEAVES;
    public static RotatedPillarBlock ARCANE_LOG_BLESSED;
    public static ArcaneSaplingBlock ARCANE_SAPLING;
    public static ArcaneTableBlock ARCANE_TABLE;
    public static ArcaneEmberBlock ARCANE_EMBER;

    public static void bootstrap() {
        // Pien REAGUJE na toporek: okorowywanie jest zrodlem zywicy (patrz
        // ArcaneLogBlock). Okorowany odpowiednik to juz zwykly RotatedPillarBlock -
        // nie daje wiecej zywicy i wlasnie to jest widoczny limit mechaniki. Celowo
        // ZERO licznika uderzen w stanie bloku: stan nosi sam blok, wiec nie ma czego
        // zapisywac, synchronizowac ani psuc przy przesuwaniu drzewa tlumikiem.
        ARCANE_LOG = register("arcane_log",
                props -> new ArcaneLogBlock(props.strength(2.0f, 2.0f)));
        ARCANE_LOG_STRIPPED = register("arcane_log_stripped",
                props -> new RotatedPillarBlock(props.strength(2.0f, 2.0f)));

        // Pien poswiecony: okorowany WTEDY, gdy w koronie wisi duch. Swieci
        // (lightLevel 1) nie po to, zeby oswietlac - po to, zeby byl widoczny w
        // koronie jako "to drzewo jest zajete", bo z niego tnie sie patyki na
        // magiczna rozdzke.
        ARCANE_LOG_BLESSED = register("arcane_log_blessed",
                props -> new RotatedPillarBlock(props.strength(2.0f, 2.0f).lightLevel(state -> 1)));
        ARCANE_PLANKS = register("arcane_planks",
                props -> new Block(props.strength(2.0f, 3.0f)));

        // Liscie: vanilla buduje swoje przez TintedParticleLeavesBlock (szansa na
        // opadajaca czasteczke + tint), my dziedziczymy po nim i odlaczamy sciezke
        // gnicia na stale - patrz ArcaneLeavesBlock. Bez Properties.randomTicks():
        // sluzyl ono wylacznie domyslnej implementacji isRandomlyTicking, ktora u nas
        // jest nadpisana na false (brak random tickow = brak gnicia, nawet dla
        // lisci z starych chunkow, ktore zapisaly persistent=false).
        ARCANE_LEAVES = register("arcane_leaves", props -> new ArcaneLeavesBlock(props
                .mapColor(MapColor.PLANT)
                .strength(0.2f)
                .sound(SoundType.GRASS)
                .noOcclusion()
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY)
                .isSuffocating((state, getter, pos) -> false)
                .isViewBlocking((state, getter, pos) -> false)
                .isRedstoneConductor((state, getter, pos) -> false)));

        // Sadzonka: noCollision + instabreak + randomTicks (SaplingBlock sam
        // sprawdza swiatlo i probuje rozrosnac drzewo).
        ARCANE_SAPLING = register("arcane_sapling", props -> new ArcaneSaplingBlock(props
                .mapColor(MapColor.PLANT)
                .noCollision()
                .randomTicks()
                .instabreak()
                .sound(SoundType.GRASS)
                .pushReaction(PushReaction.DESTROY)));

        ARCANE_TABLE = register("arcane_table", props -> new ArcaneTableBlock(props
                .strength(2.5f, 3.0f)));

        // Arkanny zar (kotwica bramy). Wlasciwosci dobrane pod "znaleziony w
        // lawie", nie pod "wydobyty na zapas":
        //  lightLevel 4/15 - swieci dosyc, by gracza go dostrzegla w jaskini,
        //    a po zapaleniu bylo go widac z daleka;
        //  explosionResistance 1200 + pushReaction(BLOCK) - polaczenie bram jest
        //    liczane z pozycji (patrz GateService), wiec przestawienie bloku
        //    tlumikiem albo wybuchem rozlaczyloby pare;
        //  requiresCorrectToolForDrops + #mineable/pickaxe + #needs_iron_tool -
        //    mozna go przeniesc (awaryjne wyjscie w Arkanum wlasnie na tym polega),
        //    ale nie goia reka.
        ARCANE_EMBER = register("arcane_ember", props -> new ArcaneEmberBlock(props
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(6.0f)
                .explosionResistance(1200.0f)
                .sound(SoundType.AMETHYST)
                .requiresCorrectToolForDrops()
                .pushReaction(PushReaction.BLOCK)
                .lightLevel(state -> state.getValue(ArcaneEmberBlock.LIT) ? 15 : 4)
                .emissiveRendering((state, getter, pos) -> state.getValue(ArcaneEmberBlock.LIT))));
    }

    /** Rejestruje blok i jego BlockItem pod tym samym id (sciezka = nazwa bloku). */
    private static <T extends Block> T register(String path, Function<BlockBehaviour.Properties, T> factory) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, path));
        T block = factory.apply(BlockBehaviour.Properties.of()
                .setId(blockKey)
                .sound(SoundType.WOOD)
                .mapColor(MapColor.WOOD));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, path));
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)));
        return block;
    }

    private ModBlocks() {
    }
}
