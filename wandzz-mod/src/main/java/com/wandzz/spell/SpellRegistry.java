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

    /** Jak wyzej, ale tylko wsrod czarow, ktore dany koszyk core'ow udostepnia. */
    public static Optional<Spell> recognize(List<Point> drawnPoints, java.util.Collection<String> allowedIds) {
        java.util.Set<String> allowed = new java.util.HashSet<>(allowedIds);
        return RECOGNIZER.recognize(drawnPoints, allowed::contains)
                .flatMap(result -> get(result.templateId()));
    }

    /** Najblizszy wzorzec w danym koszyku, bez progu - do komunikatu zwrotnego. */
    public static Optional<DollarOneRecognizer.Result> bestMatch(List<Point> drawnPoints,
            java.util.Collection<String> allowedIds) {
        java.util.Set<String> allowed = new java.util.HashSet<>(allowedIds);
        return RECOGNIZER.bestMatch(drawnPoints, allowed::contains);
    }

    /**
     * Ktore czary da sie rzucic rdzeniami z listy. To jest TEN SAM warunek co po
     * stronie serwera ({@code Spell#isProvidedBy}), wiec klient nie zgaduje -
     * odfiltrowuje dokladnie to, za co serwer i tak by zaplacil.
     */
    public static java.util.Collection<String> castableBy(java.util.Collection<com.wandzz.core.CoreType> cores) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (Spell spell : SPELLS.values()) {
            for (com.wandzz.core.CoreType core : cores) {
                if (spell.isProvidedBy(core)) {
                    ids.add(spell.id());
                    break;
                }
            }
        }
        return ids;
    }
}
