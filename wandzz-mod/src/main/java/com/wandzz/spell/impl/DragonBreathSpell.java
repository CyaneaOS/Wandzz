package com.wandzz.spell.impl;

import com.wandzz.core.CoreType;
import com.wandzz.spell.Spell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.phys.Vec3;

/**
 * Dragon Breath Core (lvl 3) - "Dragon Breath": wystrzeliwuje smoczy pocisk
 * ognisty w kierunku patrzenia gracza. Zgodnie z dokumentem projektowym ten
 * efekt moze byc dostepny takze dla INNYCH core'ow poziomu 3, dlatego
 * isProvidedBy nie ogranicza go wylacznie do CoreType.DRAGON_BREATH.
 */
public class DragonBreathSpell implements Spell {

    @Override
    public String id() {
        return "wandzz:dragon_breath";
    }

    @Override
    public int requiredLevel() {
        return 3;
    }

    @Override
    public double manaCost() {
        return 25.0;
    }

    @Override
    public boolean isProvidedBy(CoreType core) {
        return core.level() >= 3;
    }

    @Override
    public void cast(ServerLevel world, Player caster) {
        Vec3 look = caster.getViewVector(1.0f);
        DragonFireball fireball = new DragonFireball(world, caster, look.scale(2.0));
        fireball.setPos(caster.getX(), caster.getEyeY(), caster.getZ());
        world.addFreshEntity(fireball);
    }
}
