package com.wandzz.spell.impl;

import com.wandzz.spell.Spell;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Feather Core (lvl 1) - "dzialanie podobne do pochodni": stawia zrodlo
 * swiatla na bloku, na ktory patrzy gracz, bez potrzeby posiadania pochodni
 * w ekwipunku.
 */
public class TorchSpell implements Spell {

    @Override
    public String id() {
        return "wandzz:torch";
    }

    @Override
    public int requiredLevel() {
        return 1;
    }

    @Override
    public double manaCost() {
        return 2.0;
    }

    @Override
    public void cast(ServerLevel world, Player caster) {
        HitResult hit = caster.pick(6.0, 0.0f, false);
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos placeAt = blockHit.getBlockPos().relative(blockHit.getDirection());
            if (world.isEmptyBlock(placeAt)) {
                world.setBlock(placeAt, Blocks.TORCH.defaultBlockState(), 3);
            }
        }
    }
}
