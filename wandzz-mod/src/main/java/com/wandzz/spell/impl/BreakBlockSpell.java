package com.wandzz.spell.impl;

import com.wandzz.spell.Spell;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Feather Core (lvl 1) - "niszczenie blokow": magiczny odpowiednik kopania,
 * dziala na blok na ktory patrzy gracz (w rozsadnym zasiegu i twardosci).
 */
public class BreakBlockSpell implements Spell {

    private static final double MAX_HARDNESS = 3.0;

    @Override
    public String id() {
        return "wandzz:break_block";
    }

    @Override
    public int requiredLevel() {
        return 1;
    }

    @Override
    public double manaCost() {
        return 3.0;
    }

    @Override
    public void cast(ServerLevel world, Player caster) {
        HitResult hit = caster.pick(6.0, 0.0f, false);
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = world.getBlockState(pos);
            float hardness = state.getDestroySpeed(world, pos);
            if (hardness >= 0 && hardness <= MAX_HARDNESS) {
                world.destroyBlock(pos, true, caster);
            }
        }
    }
}
