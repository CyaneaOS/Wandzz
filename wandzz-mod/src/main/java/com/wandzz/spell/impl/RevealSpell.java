package com.wandzz.spell.impl;

import com.wandzz.spell.Spell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Odkrycie (rdzen poziomu 2): obrys na WSZYSTKO, co zywe, w promieniu 25 kratek.
 *
 * <p>Czar nie robi nic wlasnego - tylko daje vanilla {@code glowing}
 * ({@link MobEffects#GLOWING}), czyli dokladnie ten sam obrys co po strzale z
 * trujaca mikstura albo po {@code /effect give @s glowing}. Dlatego nie ma tu
 * zadnego renderera ani ani jednego wlasnego pakietu: obrys rysuje klient z
 * flagi encji, a my tylko te flage ustawiamy.
 *
 * <p>Liczba 25 nie jest wziete z sufitu - jest wyraznie PONIZEJ limitow, ktore
 * decyduja, czy klient w ogole zna istnienie encji. Vanilla podaje zasieg
 * synchronizacji w CHUNKACH ({@code EntityType.Builder#clientTrackingRange}): potwory
 * 8 (czyli 128 kratek), wiekszosc zwierzat 10, gracz 32; {@code simulation-distance}
 * ma domylnie 10 chunkow ({@code DistanceManager}). Przy 25 kratkach kazdy, kto widzi
 * dana encje, widzi tez jej obrys - nie ma "dziur" zaleznych od ustawien
 * klienta, a takie zostalyby odebrane jako psujacy sie czar.
 *
 * <p>Zasieg jest liczony od SRODKA KLATKI piersiowej, nie od bloku pod nogami -
 * gracz patrzy w korony drzew i w okna, nie w ziemie.
 */
public final class RevealSpell implements Spell {

    /** Promien w blokach, mierzony od srodka klatki piersiowej rzucajacego. */
    public static final double RADIUS = 25.0;

    /**
     * 30 s obrysu: dosyc dlugo, zeby przejrzec las i wrocic pod oslone, za
     * krotko, zeby zamienil sie w darmowy radar na cala sesje.
     */
    private static final int GLOW_TICKS = 600;

    @Override
    public String id() {
        return "wandzz:reveal";
    }

    @Override
    public int requiredLevel() {
        return 2;
    }

    @Override
    public double manaCost() {
        return 16.0;
    }

    /**
     * Brak czegokolwiek w zasiegu = brak platnosci (ta sama zasada co w
     * {@code OpenGateSpell}): pudlo nie moze kosztowac 16 many.
     */
    @Override
    public boolean canCast(final ServerLevel world, final Player caster) {
        return !targets(world, caster).isEmpty();
    }

    @Override
    public void cast(final ServerLevel world, final Player caster) {
        for (final LivingEntity cel : targets(world, caster)) {
            // ambient=false (nie "tlumione" czastki), visible=true (ikona w HUD i
            // obrys to i tak warstwa renderowana z flagi, nie czastki).
            cel.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, true, true));
        }
        // Sygnatura, ze czar w ogole wyszedl: przy dwudziestu celach latwo
        // pomyslic, ze klikniecie zgubilo sie w animacji.
        caster.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, 1.9F);
        world.sendParticles(ParticleTypes.END_ROD,
                caster.getX(), caster.getY() + 1.1, caster.getZ(),
                30, 0.7, 0.25, 0.7, 0.02);
    }

    /**
     * Wszystkie zywe istoty w kuli, bez rzucajacego.
     *
     * <p>{@link LivingEntity} zamiast {@code Entity}, bo {@code glowing} jest
     * efektem, a efekty nosza tylko istoty zye. Praktycznie: moby, gracze,
     * wiesniacy i stojaki na zbroje (te ostatnie sa LivingEntity, wiec swieca -
     * slusznie, bo to jedyny sposob, zeby zobaczyc, czy ktos nie przestawil dekoracji).
     * Itemy i kule XP sa poza nawiasem: nie da im sie zadnego efektu bez
     * wlasnego renderera po stronie klienta.
     */
    private static List<LivingEntity> targets(final ServerLevel world, final Player caster) {
        final AABB box = caster.getBoundingBox().inflate(RADIUS, RADIUS * 0.6, RADIUS);
        return world.getEntitiesOfClass(LivingEntity.class, box,
                cel -> cel != caster && cel.isAlive());
    }
}
