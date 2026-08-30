package com.wandzz.gesture;

import java.util.List;

/**
 * Wzorzec gestu - lista punktow reprezentujaca "idealny" ksztalt danego zaklecia.
 * Identyfikator (id) laczy wzorzec z konkretnym Spell w SpellRegistry.
 */
public record GestureTemplate(String id, List<Point> points) {
}
