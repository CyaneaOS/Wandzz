package com.wandzz.spell.impl;

import com.wandzz.spell.Spell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Niewidzialnosc (rdzen poziomu 3): 45 s znikania, BEZ ZADNYCH BABELKOW.
 *
 * <p>"Bez babelkow" znaczy tu trzy konkretne rzeczy i wszystkie sa w jednym
 * ctorze {@link MobEffectInstance}:
 * <ol>
 *   <li>{@code visible=false} - zadnych fioletowych babelkow wokolo modelu. To
 *       nie jest "upiekszenie", tylko wylaczenie zrodla: w 1.21.11 liste czastek
 *       efektu synchronizuje {@code LivingEntity#updateSynchronizedMobEffect-
 *       Particles()}, ktora filtruje wlasnie po {@code MobEffectInstance#
 *       isVisible()} i wynik wrzuca do {@code DATA_EFFECT_PARTICLES}. Flaga
 *       {@code false} znaczy wiec "nie ma ich" nie tylko u mnie, ale i u
 *       kazdego, kto na nas patrzy - a nie "sa, tylko przeroczyste". Same
 *       kleki sklada {@code MobEffect} jako {@code ColorParticleOption
 *       (ParticleTypes.ENTITY_EFFECT, kolor)}, czyli bez tego filtra byloby je
 *       wida. Dla czarow rzucanych z rozdzki sa mylace: wygladaja jak
 *       mikstura, ktora wlasnie sie wypilo.</li>
 *   <li>{@code showIcon=true} - ikona z licznikiem w HUD ZOSTAJE. Latwo sie o to
 *       potknac: piecioargumentowy ctor MobEffectInstance ustawia
 *       {@code showIcon = visible} (vanilla {@code MobEffectInstance}, ctor
 *       piecioargumentowy deleguje do szescioargumentowego), wiec samo
 *       "wylacz czastki" przez {@code visible=false} wylaczyloby takze ikone i
 *       gracz nie widzialby, kiedy mu niewidzialnosc minie. Dlatego podajemy
 *       jawnie szosty argument.</li>
 *   <li>{@code ambient=false} - przy {@code visible=false} nie ma to zadnego
 *       efektu wizualnego (alpha 255 vs 50 dotyczy wlasnie tych czastek), ale
 *       zostaje ustawione swiadomie: ambient to flaga "efekt z tla, nie z
 *       mikstury", a tu nie ma mikstury.</li>
 * </ol>
 *
 * <p>W ekwipunku nic sie nie pojawia: nie ma {@code DataComponents.POTION_CONTENTS},
 * nie ma koloru kieliszka, nie ma czego rysowac przy HUD - czar jest czystym
 * efektem, wiec nie walczy z prawdziwymi miksturami o te same sloty.
 *
 * <p>Czego czar NIE robi i nie udaje: zbroja i trzymana rozdzka zostaja
 * widoczne (tak samo dziala vanilla - {@code INVISIBILITY} chowa model bytu i
 * pancerz, ale nie przedmiot w rece), a nick nad glowa nadal widza inni gracze.
 * Za to {@code wandzz:reveal} ten czar kontruje: obrys z {@code glowing} jest
 * rysowany niezaleznie od niewidzialnosci, wiec "niewidzialny, ale swiecacy" to
 * nie blad, tylko swiadoma gra dwoch zaklec.
 */
public final class InvisibilitySpell implements Spell {

    /** 45 s. Mikstura trwa 180 s, ale tam placisz skladnikami; tu placa jest mana. */
    private static final int INVIS_TICKS = 900;

    @Override
    public String id() {
        return "wandzz:invisibility";
    }

    @Override
    public int requiredLevel() {
        return 3;
    }

    @Override
    public double manaCost() {
        return 22.0;
    }

    /**
     * Odswiezenie jest dozwolone i placa tyle samo - {@code addEffect} w vanilla
     * nadpisuje czas (ten sam poziom go wydluza, wyzszy poziom wygrywa). Blokada
     * "juz jestes niewidzialny" dawalaby ciche odmowy bez zadnego komunikatu,
     * bo {@code canCast} jest sprawdzana w {@code CastingHandler} i tam nikt
     * nie wysyla message'a - a to jest gorsza zabawa niz wydatek many.
     */
    @Override
    public void cast(final ServerLevel world, final Player caster) {
        caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVIS_TICKS, 0,
                false, false, true));
        // Jedna chmurka zamiast ciaglego bulgotania: gracz ma slyszec i widziec, ze
        // czar WYSZEDL, a nie byc obsypany czastkami przez pol minuty.
        world.sendParticles(ParticleTypes.CLOUD,
                caster.getX(), caster.getY() + 1.0, caster.getZ(),
                22, 0.42, 0.9, 0.42, 0.02);
        caster.playSound(SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.5F);
    }
}
