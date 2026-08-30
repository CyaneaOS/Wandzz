package com.wandzz.spell.impl;

import com.wandzz.spell.Spell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

/**
 * Przykladowe zaklecie z diagramu w dokumencie projektowym:
 * gracz rysuje gest -> $1 -> rozpoznany wzor -> Fireball -> sprawdzenie
 * wymagan -> CAST. Powiazane z core'em FLAME (lvl 2).
 */
public class FireballSpell implements Spell {

    @Override
    public String id() {
        return "wandzz:fireball";
    }

    @Override
    public int requiredLevel() {
        return 2;
    }

    @Override
    public double manaCost() {
        return 12.0;
    }

    @Override
    public void cast(ServerLevel world, Player caster) {
        Vec3 look = caster.getViewVector(1.0f);
        SmallFireball fireball = new SmallFireball(world, caster, look.scale(1.5));
        fireball.setPos(caster.getX(), caster.getEyeY(), caster.getZ());
        world.addFreshEntity(fireball);
    }
}
