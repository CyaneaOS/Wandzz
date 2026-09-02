package com.wandzz.spell.impl;

import com.wandzz.spell.Spell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Wingardium Leviosa (poziom 2, 14 many): unies cel i nie daj mu sie rozobic.
 *
 * <p>Dwa efekty zamiast jednego, i to nie dla urody. {@code LEVITATION} ciagne
 * w gore przez caly czas trwania, a sam {@code SLOW_FALLING} nie zatrzymuje
 * wznoszenia - wiec 30 tickow lewitacji plus 260 tickow miekkiego ladowania
 * daja winda, a nie "smiertelny upadek z 30 kratek". Wlasnie dlatego czar moze
 * dzialac rowniez na innych graczy: nikogo nie zabija, niczego nie zdejmuje z
 * ekwipunku, nie da sie nim utkwic ofiary na stale (po 1,5 s leci w dol sama).
 *
 * <p>Czas lewitacji jest krotki swiadomie. Przy dluzszym uniesieniu ofiara
 * wpadalaby w chmury albo w korony drzew - a "zgubiony gracz w lisciu" to jest
 * dokladnie ten rodzaj bledu, ktory serwery nazywaja zgloszeniem.
 */
public final class WingardiumSpell implements Spell {

    /** Zasieg patrzenia - wiekszy niz u {@code strike}, bo "pioro" rzuca sie z dystansu. */
    private static final double RANGE = 14.0;
    private static final int LEVITACJA_TICKS = 30;
    private static final int LADOWANIE_TICKS = 260;

    @Override
    public String id() {
        return "wandzz:wingardium_leviosa";
    }

    @Override
    public int requiredLevel() {
        return 2;
    }

    @Override
    public double manaCost() {
        return 14.0;
    }

    @Override
    public boolean canCast(final ServerLevel world, final Player caster) {
        return cel(world, caster) != null;
    }

    @Override
    public void cast(final ServerLevel world, final Player caster) {
        final LivingEntity cel = cel(world, caster);
        if (cel == null) {
            return;
        }
        cel.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LEVITACJA_TICKS, 0, false, true, true));
        cel.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, LADOWANIE_TICKS, 0, false, true, true));
        world.sendParticles(ParticleTypes.END_ROD,
                cel.getX(), cel.getY() + cel.getBbHeight() * 0.5, cel.getZ(),
                18, 0.35, 0.5, 0.35, 0.01);
        caster.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.5F, 1.6F);
    }

    /** Na co patrzy rzucajacy; tylko istota zywa, bez "piora w sciane". */
    private static LivingEntity cel(final ServerLevel world, final Player caster) {
        final HitResult hit = caster.pick(RANGE, 0.0F, false);
        if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living
                && living.isAlive()) {
            return living;
        }
        return null;
    }
}
