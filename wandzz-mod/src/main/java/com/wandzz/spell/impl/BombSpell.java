package com.wandzz.spell.impl;

import com.wandzz.core.CoreType;
import com.wandzz.spell.Spell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Dragon Breath Core (lvl 3) - "bomba": przywoluje eksplodujacy pocisk
 * w miejscu, na ktore patrzy gracz.
 */
public class BombSpell implements Spell {

    private static final double MAX_RANGE = 15.0;
    private static final int FUSE_TICKS = 30;

    @Override
    public String id() {
        return "wandzz:bomb";
    }

    @Override
    public int requiredLevel() {
        return 3;
    }

    @Override
    public double manaCost() {
        return 20.0;
    }

    @Override
    public boolean isProvidedBy(CoreType core) {
        return core.level() >= 3;
    }

    @Override
    public void cast(ServerLevel world, Player caster) {
        HitResult hit = caster.pick(MAX_RANGE, 0.0f, false);
        Vec3 pos = hit.getLocation();
        PrimedTnt tnt = new PrimedTnt(world, pos.x, pos.y, pos.z, caster);
        tnt.setFuse(FUSE_TICKS);
        world.addFreshEntity(tnt);
    }
}
