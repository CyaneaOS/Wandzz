package com.wandzz.gesture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Przechowuje sekwencje punktow rejestrowana od momentu wcisniecia PPM
 * (startCasting) do jego puszczenia (stopCasting).
 *
 * Mysz -> MouseInput -> CastingData (List<Point>) -> $1 Recognizer
 */
public class CastingData {

    /** Minimalny dystans (px) miedzy kolejnymi zarejestrowanymi punktami - filtruje szum. */
    private static final double MIN_POINT_SPACING = 2.0;

    private final List<Point> points = new ArrayList<>();
    private boolean casting = false;

    public void startCasting() {
        points.clear();
        casting = true;
    }

    /** Wywolywane przy kazdym ruchu myszy podczas rysowania. */
    public void addPoint(double x, double y) {
        if (!casting) return;
        if (!points.isEmpty()) {
            Point last = points.get(points.size() - 1);
            if (last.distanceTo(new Point(x, y)) < MIN_POINT_SPACING) {
                return;
            }
        }
        points.add(new Point(x, y));
    }

    /** Konczy rysowanie i zwraca zebrana sciezke (kopia, niemodyfikowalna). */
    public List<Point> stopCasting() {
        casting = false;
        List<Point> result = List.copyOf(points);
        points.clear();
        return result;
    }

    public boolean isCasting() {
        return casting;
    }

    public List<Point> currentPoints() {
        return Collections.unmodifiableList(points);
    }

    public int size() {
        return points.size();
    }
}
