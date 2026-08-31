package com.wandzz.spell;

import com.wandzz.gesture.DollarOneRecognizer;
import com.wandzz.gesture.Point;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
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
    /**
     * Surowe (nienormalizowane) wzorce gestow - potrzebne np. ksiezce zaklec,
     * ktora rysuje diagram tak, zeby gracz mogl go odwzorowac myszka. Recognizer
     * normalizuje punkty po swojej stronie (patrz addTemplate), wiec to jest
     * wylacznie kopia do renderowania.
     */
    private static final Map<String, List<Point>> GESTURES = new LinkedHashMap<>();
    private static final DollarOneRecognizer RECOGNIZER = new DollarOneRecognizer();

    private SpellRegistry() {
    }

    public static void register(Spell spell, List<Point> gestureTemplate) {
        SPELLS.put(spell.id(), spell);
        GESTURES.put(spell.id(), List.copyOf(gestureTemplate));
        RECOGNIZER.addTemplate(spell.id(), gestureTemplate);
    }

    /** Wszystkie zaklecia w kolejnosci rejestracji (ksiezka zaklec to iteruje). */
    public static Collection<Spell> all() {
        return SPELLS.values();
    }

    /** Wzorzec gestu do narysowania w UI; null jesli zaklecie nie ma gestu. */
    public static @Nullable List<Point> gestureOf(String spellId) {
        return GESTURES.get(spellId);
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
