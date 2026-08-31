package com.wandzz.spell.impl;

import com.wandzz.spell.Spell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Skok: jump boost + powolne opadanie, bez rzucania graczem przez mape.
 *
 * Zaklecie celowo NIE jest "ender pearl w przod" - do tego masz teleportacje.
 * Tu efekt jest taki, zebys wyszedl z opresji bez czytania liczb:
 * {@code JUMP_BOOST} podnosi, {@code SLOW_FALLING} daje czas na korekte, a
 * zerowanie {@code fallDistance} odbiera kare za to, ze gracz polecil za daleko.
 */
public final class LeapSpell implements Spell {

    private static final int DURATION_TICKS = 400;
    /** Impuls w gore, gdy gracz stoi na ziemi - dokladnie tyle, ile daje vanilla skok. */
    private static final double GROUND_HOP = 0.42;

    @Override
    public String id() {
        return "leap";
    }

    @Override
    public int requiredLevel() {
        return 1;
    }

    @Override
    public double manaCost() {
        return 6.0;
    }

    @Override
    public void cast(final ServerLevel world, final Player caster) {
        caster.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, DURATION_TICKS, 1));
        caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, DURATION_TICKS, 0));
        if (caster.onGround()) {
            final Vec3 motion = caster.getDeltaMovement();
            caster.setDeltaMovement(motion.x, GROUND_HOP, motion.z);
        }
        // skok ma byc slyszalny i widoczny, bo bez tego "rzucilem" i "nic sie
        // nie stalo" sa tym samym - a to najgorszy bug, jaki zna gracz
        caster.resetFallDistance();
        world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                caster.getX(), caster.getY() + 0.2, caster.getZ(),
                16, 0.4, 0.15, 0.4, 0.02);
        caster.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, 0.7F);
    }
}
