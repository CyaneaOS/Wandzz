package com.wandzz.gesture;

/**
 * Pojedynczy punkt gestu - odpowiada pozycji kursora myszy w danej klatce
 * podczas rysowania zaklecia. Wspolrzedne sa w przestrzeni ekranu (px).
 */
public record Point(double x, double y) {

    public double distanceTo(Point other) {
        double dx = other.x - this.x;
        double dy = other.y - this.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
