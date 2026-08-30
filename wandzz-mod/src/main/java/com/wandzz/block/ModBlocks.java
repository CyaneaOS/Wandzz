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
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Function;

/**
 * "Arkany" - drewno, z ktorego robi sie LEPSZE rozdzki (receptury
 * WandMaterial.CUSTOM i RARE opieraja sie na patykach z tych desek).
 *
 * Swiadomie BEZ drzewa: brak saplingu, brak plikow `worldgen_configured_feature`
 * / `biome_modifier` - piec bierze sie wylacznie z craftingu, az do
 * osobnego kroku z generowaniem.
 *
 * 1.21.11 (Mojmap) - blok, tak jak przedmiot, MUSI znac swoj ResourceKey
 * PRZED konstrukcja: `BlockBehaviour.Properties#setId`, inaczej start gry
 * konczy sie `NullPointerException: Block id not set` (descriptionId i loot
 * table sa z tego klucza liczane). Dlatego wlasciwosci buduje SIE W SRODKU
 * register(...) i podaje fabryce - dokladnie jak w vanilla `Blocks#registerBlock`.
 */
public final class ModBlocks {

    public static RotatedPillarBlock ARCANE_LOG;
    public static Block ARCANE_PLANKS;

    public static void bootstrap() {
        ARCANE_LOG = register("arcane_log", props -> new RotatedPillarBlock(props.strength(2.0f, 2.0f)));
        ARCANE_PLANKS = register("arcane_planks", props -> new Block(props.strength(2.0f, 3.0f)));
    }

    /** Rejestruje blok i jego BlockItem pod tym samym id (sciezka = nazwa bloku). */
    private static <T extends Block> T register(String path, Function<BlockBehaviour.Properties, T> factory) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, path));
        T block = factory.apply(BlockBehaviour.Properties.of()
                .setId(blockKey)
                .sound(SoundType.WOOD)
                .mapColor(MapColor.WOOD));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, path));
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)));
        return block;
    }

    private ModBlocks() {
    }
}
