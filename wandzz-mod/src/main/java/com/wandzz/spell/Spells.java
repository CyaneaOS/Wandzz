package com.wandzz.spell;

import com.wandzz.gesture.GestureTemplates;
import com.wandzz.spell.impl.*;

/**
 * Punkt wejscia rejestrujacy wbudowane zaklecia: kazde razem ze swoim
 * wzorcem gestu, zgodnie z diagramem:
 *
 *   narysuj gest -> $1 -> rozpoznanie -> Spell -> sprawdzenie wymagan -> CAST
 */
public final class Spells {

    private Spells() {
    }

    public static void bootstrap() {
        // Feather Core (lvl 1)
        SpellRegistry.register(new StrikeSpell(), GestureTemplates.strikeStroke());
        SpellRegistry.register(new BreakBlockSpell(), GestureTemplates.breakBlockStroke());
        SpellRegistry.register(new TorchSpell(), GestureTemplates.torchStroke());

        // Flame Core (lvl 2) - przykladowe zaklecie z diagramu w dokumencie
        SpellRegistry.register(new FireballSpell(), GestureTemplates.flameTriangle());

        // Dragon Breath Core (lvl 3, ale dostepne dla kazdego core'a lvl 3+)
        SpellRegistry.register(new TeleportSpell(), GestureTemplates.twinSquares());
        SpellRegistry.register(new BombSpell(), GestureTemplates.diamondTick());
        SpellRegistry.register(new DragonBreathSpell(), GestureTemplates.breathWave());

        // Arcane / lvl 3 - nie atak, tylko "przejscie": zapala Arkanny Zar.
        SpellRegistry.register(new OpenGateSpell(), GestureTemplates.barredGateStroke());

        // Rdzen echa (lvl 2, z Wardena) + skok: tanie, przydatne, widoczne.
        // Oba sa w podreczniku gestow automatycznie - lista jest budowana z
        // SpellRegistry, wiec nic tu wiecej nie trzeba dokladac.
        SpellRegistry.register(new HealSpell(), GestureTemplates.healStroke());
        SpellRegistry.register(new LeapSpell(), GestureTemplates.leapStroke());

        // Odkrycie (lvl 2) i niewidzialnosc (lvl 3): para, ktora sie swierza -
        // reveal podswietla takze gracza pod niewidka, bo obrys i model encji to
        // dwie niezalezne warstwy renderowania.
        SpellRegistry.register(new RevealSpell(), GestureTemplates.revealStroke());
        SpellRegistry.register(new InvisibilitySpell(), GestureTemplates.invisibilityStroke());
    }
}
