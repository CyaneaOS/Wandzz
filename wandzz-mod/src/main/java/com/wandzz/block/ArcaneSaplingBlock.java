package com.wandzz.block;

import com.wandzz.world.ModWorldgen;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Sadzonka arkanu. Nadpisuje tylko konstruktor, bo w 1.21.11
 * {@code SaplingBlock(TreeGrower, Properties)} jest {@code protected} -
 * vanilla buduje swoje sadzonki lambda w {@code Blocks}, a mod potrzebuje
 * wlasnej klasy (lub dostepu przez access widener, czego swiadomie unikamy).
 */
public class ArcaneSaplingBlock extends SaplingBlock {

    public ArcaneSaplingBlock(BlockBehaviour.Properties properties) {
        super(ModWorldgen.ARCANE_TREE_GROWER, properties);
    }
}
