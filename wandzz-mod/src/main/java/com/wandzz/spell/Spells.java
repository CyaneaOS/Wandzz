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
        SpellRegistry.register(new FireballSpell(), GestureTemplates.triangle());

        // Dragon Breath Core (lvl 3, ale dostepne dla kazdego core'a lvl 3+)
        SpellRegistry.register(new TeleportSpell(), GestureTemplates.zigzag());
        SpellRegistry.register(new BombSpell(), GestureTemplates.letterX());
        SpellRegistry.register(new DragonBreathSpell(), GestureTemplates.dragonSpiral(48));

        // Arcane / lvl 3 - nie atak, tylko "przejscie": zapala Arkanny Zar.
        SpellRegistry.register(new OpenGateSpell(), GestureTemplates.gateStroke());
    }
}
