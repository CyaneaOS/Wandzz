package com.wandzz.spell;

import com.wandzz.core.CoreType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;

/**
 * Pojedyncze zaklecie. Rozpoznany gest ($1) wskazuje jego identyfikator,
 * a nastepnie sprawdzane sa wymagania (poziom core'a we rozdzce, mana)
 * zanim zostanie ono rzucone (CAST).
 *
 * gest -> $1 -> rozpoznany wzor -> Spell -> sprawdzenie wymagan -> CAST
 */
public interface Spell {

    /** Identyfikator zgodny z id wzorca gestu (GestureTemplate) i wpisem w SpellRegistry. */
    String id();

    /** Minimalny poziom core'a wymagany, by rzucic to zaklecie (patrz CoreType). */
    int requiredLevel();

    /** Koszt many za jedno rzucenie. */
    double manaCost();

    /**
     * Faktyczny efekt zaklecia - wywolywane WYLACZNIE po stronie serwera,
     * po pozytywnej weryfikacji core'ow i many (patrz WandzzCastingHandler).
     */
    void cast(ServerLevel world, Player caster);

    /**
     * Czy dany typ core'a we rozdzce udostepnia to zaklecie.
     * Domyslnie: core musi miec poziom >= requiredLevel().
     * Nadpisz jesli zaklecie ma byc dostepne tylko dla konkretnych core'ow
     * (np. tak jak w dokumentacji: Dragon Breath moze byc dostepny takze
     * dla innych core'ow poziomu 3, a nie tylko dla Dragon Breath Core).
     */
    default boolean isProvidedBy(CoreType core) {
        return core.level() >= requiredLevel();
    }
}
