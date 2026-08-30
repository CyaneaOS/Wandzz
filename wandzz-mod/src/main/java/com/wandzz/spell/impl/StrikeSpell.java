package com.wandzz.spell.impl;

import com.wandzz.spell.Spell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Feather Core (lvl 1) - "uderzenie": prosty raycast-owy atak na cel,
 * zaklinany zamiennik zwyklego LPM.
 */
public class StrikeSpell implements Spell {

    public static final double DAMAGE = 3.0;

    @Override
    public String id() {
        return "wandzz:strike";
    }

    @Override
    public int requiredLevel() {
        return 1;
    }

    @Override
    public double manaCost() {
        return 4.0;
    }

    @Override
    public void cast(ServerLevel world, Player caster) {
        HitResult hit = caster.pick(6.0, 0.0f, false);
        // hurtServer jest na LivingEntity, a EntityHitResult zwraca ogolne Entity.
        if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity target) {
            target.hurtServer(world, world.damageSources().playerAttack(caster), (float) DAMAGE);
        }
    }
}
