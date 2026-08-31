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
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
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

    public static RotatedPillarBlock ARCANE_LOG;
    public static Block ARCANE_PLANKS;
    public static LeavesBlock ARCANE_LEAVES;
    public static ArcaneSaplingBlock ARCANE_SAPLING;
    public static ArcaneTableBlock ARCANE_TABLE;

    public static void bootstrap() {
        ARCANE_LOG = register("arcane_log",
                props -> new RotatedPillarBlock(props.strength(2.0f, 2.0f)));
        ARCANE_PLANKS = register("arcane_planks",
                props -> new Block(props.strength(2.0f, 3.0f)));

        // Liscie: 1.21.11 ma abstract LeavesBlock, vanilla buduje swoje przez
        // TintedParticleLeavesBlock (szansa na opadajaca czasteczke + tint lisci).
        // Wzor na vanilla leavesProperties(...): PLANT, 0.2, randomTicks (gnicie),
        // noOcclusion + wyjete predykaty "litych" blokow.
        ARCANE_LEAVES = register("arcane_leaves", props -> new TintedParticleLeavesBlock(0.02f, props
                .mapColor(MapColor.PLANT)
                .strength(0.2f)
                .randomTicks()
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
