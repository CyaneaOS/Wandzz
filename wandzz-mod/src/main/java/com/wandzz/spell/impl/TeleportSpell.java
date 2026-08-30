package com.wandzz.spell.impl;

import com.wandzz.core.CoreType;
import com.wandzz.spell.Spell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Dragon Breath Core (lvl 3) - "teleportacja do okreslonego zasiegu":
 * przenosi gracza do punktu, na ktory patrzy, w granicach MAX_RANGE.
 */
public class TeleportSpell implements Spell {

    private static final double MAX_RANGE = 20.0;

    @Override
    public String id() {
        return "wandzz:teleport";
    }

    @Override
    public int requiredLevel() {
        return 3;
    }

    @Override
    public double manaCost() {
        return 15.0;
    }

    @Override
    public boolean isProvidedBy(CoreType core) {
        // Zaklecie dostepne dla KAZDEGO core'a poziomu 3+, nie tylko Dragon Breath.
        return core.level() >= 3;
    }

    @Override
    public void cast(ServerLevel world, Player caster) {
        HitResult hit = caster.pick(MAX_RANGE, 0.0f, false);
        Vec3 dest = hit.getLocation();
        caster.teleportTo(world, dest.x, dest.y, dest.z,
                java.util.Set.of(), caster.getYRot(), caster.getXRot(), false);
    }
}
