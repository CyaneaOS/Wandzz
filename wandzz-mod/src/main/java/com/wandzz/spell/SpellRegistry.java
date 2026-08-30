package com.wandzz.spell;

import com.wandzz.gesture.DollarOneRecognizer;
import com.wandzz.gesture.Point;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

/**
 * Centralny rejestr zaklec. Kazde zaklecie jest rejestrowane razem ze swoim
 * wzorcem gestu (lista punktow), ktory trafia do wspoldzielonego
 * DollarOneRecognizer.
 */
public final class SpellRegistry {

    private static final Map<String, Spell> SPELLS = new LinkedHashMap<>();
    private static final DollarOneRecognizer RECOGNIZER = new DollarOneRecognizer();

    private SpellRegistry() {
    }

    public static void register(Spell spell, List<Point> gestureTemplate) {
        SPELLS.put(spell.id(), spell);
        RECOGNIZER.addTemplate(spell.id(), gestureTemplate);
    }

    public static Optional<Spell> get(String id) {
        return Optional.ofNullable(SPELLS.get(id));
    }

    public static DollarOneRecognizer recognizer() {
        return RECOGNIZER;
    }

    /** Narysowany gest -> $1 -> rozpoznany wzor -> Spell (bez sprawdzania wymagan). */
    public static Optional<Spell> recognize(List<Point> drawnPoints) {
        return RECOGNIZER.recognize(drawnPoints)
                .flatMap(result -> get(result.templateId()));
    }
}
