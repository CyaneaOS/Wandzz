package com.wandzz.spell.impl;

import com.wandzz.core.CoreType;
import com.wandzz.spell.Spell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Leczenie: regeneracja + absrobcja, zadnych liczb w ekwipunku.
 *
 * Dwa efekty zamiast {@code setHealth(...)}: bezposrednie wystawienie zdrowia
 * omija tarcze, jedzenie i efekty, wiec gracz dostalby "darmowe HP" zamiast
 * zaklecia. Regeneracja liczy sie przez te same sciezki co zycie.
 *
 * Absorpcja (nie regeneracja) jest tu po to, zeby czar mial sens w polu walki:
 * +4 tarczy wchodzi natychmiast, regeneracja potrzebuje 10 s, a gracz leczacy
 * sie w srodku walki i tak dostaje w glowe.
 */
public final class HealSpell implements Spell {

    private static final int REGEN_TICKS = 200;
    private static final int SHIELD_TICKS = 600;

    @Override
    public String id() {
        return "wandzz:heal";
    }

    @Override
    public int requiredLevel() {
        return 2;
    }

    @Override
    public double manaCost() {
        return 14.0;
    }

    /**
     * Nie ma sensu placic 14 many za czar, ktory nic nie robi - stad check
     * przed platnoscia (patrz CastingHandler: {@code canCast} leci PRZED
     * {@code spend}). Absorpcja jest sprawdzana, bo +4 tarczy da sie nabijac
     * w nieskonczonosc i to bylby swobodny farm one-key.
     */
    @Override
    public boolean canCast(final ServerLevel world, final Player caster) {
        return caster.getHealth() < caster.getMaxHealth() || caster.getAbsorptionAmount() < 4.0;
    }

    /**
     * Rdzen swiatla i natury (poziom 2) plus kazdy mocniejszy - czyli te same
     * zasady co w reszcie zaklec: poziom rdzenia, nie jego nazwa.
     */
    @Override
    public boolean isProvidedBy(final CoreType core) {
        return core == CoreType.LIGHT || core == CoreType.NATURE || core.level() >= 3;
    }

    @Override
    public void cast(final ServerLevel world, final Player caster) {
        caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, 1));
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, SHIELD_TICKS, 0));
        // END_ROD, nie HAPPY_VILLAGER: te drugie particle w 1.21.11 nazywaja sie
        // inaczej, a END_ROD jest widoczny rowniez w pelnym swietle
        world.sendParticles(ParticleTypes.END_ROD,
                caster.getX(), caster.getY() + 1.1, caster.getZ(),
                24, 0.45, 0.7, 0.45, 0.01);
        caster.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.45F);
    }
}
