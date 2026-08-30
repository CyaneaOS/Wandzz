package com.wandzz.gesture;

import java.util.ArrayList;
import java.util.List;

/**
 * Proste generatory wzorcow gestow uzywane przy rejestracji zaklec w
 * SpellRegistry. $1 Recognizer i tak normalizuje skale/obrot/pozycje,
 * wiec te wzorce moga byc zdefiniowane w dowolnej, wygodnej jednostce.
 */
public final class GestureTemplates {

    private GestureTemplates() {
    }

    /** Kolko rysowane zgodnie z ruchem wskazowek zegara. */
    public static List<Point> circle(int samples) {
        List<Point> pts = new ArrayList<>();
        for (int i = 0; i <= samples; i++) {
            double t = 2 * Math.PI * i / samples;
            pts.add(new Point(Math.cos(t) * 100, Math.sin(t) * 100));
        }
        return pts;
    }

    /** Trojkat - trzy wierzcholki polaczone liniami. */
    public static List<Point> triangle() {
        return List.of(
                new Point(0, -100),
                new Point(100, 100),
                new Point(-100, 100),
                new Point(0, -100)
        );
    }

    /** Zygzak (blyskawica) - uzywany np. dla teleportacji. */
    public static List<Point> zigzag() {
        return List.of(
                new Point(-100, -100),
                new Point(20, -100),
                new Point(-60, 0),
                new Point(100, 0),
                new Point(-20, 100),
                new Point(100, 100)
        );
    }

    /** Litera X - dwie przekatne, uzywana np. dla "bomby". */
    public static List<Point> letterX() {
        List<Point> pts = new ArrayList<>();
        pts.add(new Point(-100, -100));
        pts.add(new Point(100, 100));
        pts.add(new Point(100, -100));
        pts.add(new Point(-100, 100));
        return pts;
    }

    /** Pionowa kreska z krotkim hakiem u gory - jak plomien pochodni. */
    public static List<Point> torchStroke() {
        return List.of(
                new Point(0, 100),
                new Point(0, -80),
                new Point(20, -100)
        );
    }

    /** Pojedyncza krotka kreska pozioma - "uderzenie". */
    public static List<Point> strikeStroke() {
        return List.of(
                new Point(-100, 0),
                new Point(100, 0)
        );
    }

    /** Krotka kreska z krzyzykiem na koncu - "niszczenie bloku". */
    public static List<Point> breakBlockStroke() {
        return List.of(
                new Point(-100, -100),
                new Point(100, -100),
                new Point(100, 100),
                new Point(-100, 100),
                new Point(-100, -100),
                new Point(100, 100)
        );
    }

    /** Spirala - kula ognia smoka. */
    public static List<Point> dragonSpiral(int samples) {
        List<Point> pts = new ArrayList<>();
        for (int i = 0; i <= samples; i++) {
            double t = 4 * Math.PI * i / samples;
            double r = 100.0 * i / samples;
            pts.add(new Point(Math.cos(t) * r, Math.sin(t) * r));
        }
        return pts;
    }
}
