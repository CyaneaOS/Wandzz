package com.wandzz.spell.impl;

import com.wandzz.spell.Spell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Protego (poziom 2, 12 many): tarcza na 12 s, bez wlasnego liczenia obrazen.
 *
 * <p>Dwa plus jeden efekt vanilla: {@code RESISTANCE} amplituda 1 (-40%),
 * {@code ABSORPTION} amplituda 1 (+8 serc tymczasowej tarczy) i
 * {@code FIRE_RESISTANCE} na 4 s. Nazwa pierwszego jest puapka dla kogos, kto
 * pamta starsze wersje: w 1.21.11 to {@code MobEffects.RESISTANCE}, a
 * {@code DAMAGE_RESISTANCE} znikelo.
 *
 * <p>Czemu nie "blokuje nastepne uderzenie"? Wlasny hook odbralby obrazenia, a
 * wraz z nimi cala sciezke, ktora vanilla liczy przy ciosie: crit, knockback,
 * aggro, {@code GameEvent}, osiagniecia i smierc gracza. Dwa efekty wchodza w te
 * same miejsca bez zadnego mixinu i nie da sie nimi oszukac pancerza.
 *
 * <p>Ognioodpornosc jest dla {@code wandzz:fireball}: najczestsza smierc wlasnie
 * tego gracza, ktory czyta te linijki, to wlasnie wlasny ogien w ciasnym tunelu.
 */
public final class ProtegoSpell implements Spell {

    private static final int TARCZA_TICKS = 240;
    private static final int OGNIOTRWALOSC_TICKS = 80;

    @Override
    public String id() {
        return "wandzz:protego";
    }

    @Override
    public int requiredLevel() {
        return 2;
    }

    @Override
    public double manaCost() {
        return 12.0;
    }

    /**
     * Nie ma sensu placic 12 many za odswiezenie tarczy, ktora wlasnie wisi -
     * stad {@code canCast} (bez komunikatu, ta sama konwencja co w HealSpell i
     * OpenGateSpell: cicha odmowa, ale mana zostaje).
     */
    @Override
    public boolean canCast(final ServerLevel world, final Player caster) {
        return !caster.hasEffect(MobEffects.ABSORPTION) || !caster.hasEffect(MobEffects.RESISTANCE);
    }

    @Override
    public void cast(final ServerLevel world, final Player caster) {
        caster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, TARCZA_TICKS, 1, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, TARCZA_TICKS, 1, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, OGNIOTRWALOSC_TICKS, 0, false, true, true));
        world.sendParticles(ParticleTypes.SOUL,
                caster.getX(), caster.getY() + 1.0, caster.getZ(),
                26, 0.5, 0.8, 0.5, 0.02);
        caster.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 0.7F);
    }
}
