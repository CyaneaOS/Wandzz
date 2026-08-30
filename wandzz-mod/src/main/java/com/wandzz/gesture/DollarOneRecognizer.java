package com.wandzz.gesture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementacja algorytmu rozpoznawania gestow jednorysunkowych ($1 Unistroke
 * Recognizer, Wobbrock/Wilson/Li 2007). Napisana od zera na podstawie
 * publicznie opisanych krokow algorytmu:
 *
 *  1. Resample   - sprowadzenie sciezki do stalej liczby rownomiernie
 *                  rozlozonych punktow (niezaleznosc od predkosci rysowania).
 *  2. Rotate     - obrot tak, by "kat wskazujacy" (od centroidu do 1. punktu)
 *                  wynosil 0 (niezaleznosc od orientacji startu).
 *  3. Scale+Move - przeskalowanie do referencyjnego kwadratu i przesuniecie
 *                  centroidu do (0,0) (niezaleznosc od rozmiaru/pozycji).
 *  4. Recognize  - porownanie ze wzorcami metoda Golden Section Search,
 *                  szukajac najlepszego dopasowania po kacie obrotu.
 *
 * Dzieki temu gracz nie musi rysowac wzoru piksel w piksel.
 */
public final class DollarOneRecognizer {

    private static final int RESAMPLE_POINTS = 64;
    private static final double SQUARE_SIZE = 250.0;
    private static final double GOLDEN_RATIO = 0.5 * (-1.0 + Math.sqrt(5.0));
    private static final double ANGLE_RANGE_DEG = 45.0;
    private static final double ANGLE_PRECISION_DEG = 2.0;
    /** Minimalny wynik (0..1) ponizej ktorego gest jest odrzucany jako nierozpoznany. */
    public static final double MIN_SCORE = 0.75;

    private final List<GestureTemplate> templates = new ArrayList<>();

    public void addTemplate(String id, List<Point> rawPoints) {
        templates.add(new GestureTemplate(id, normalize(rawPoints)));
    }

    public record Result(String templateId, double score) {
    }

    /** Zwraca najlepiej dopasowany wzorzec, jesli wynik przekracza MIN_SCORE. */
    public Optional<Result> recognize(List<Point> rawPoints) {
        if (rawPoints.size() < 2 || templates.isEmpty()) {
            return Optional.empty();
        }
        List<Point> candidate = normalize(rawPoints);

        String bestId = null;
        double bestDistance = Double.MAX_VALUE;

        for (GestureTemplate template : templates) {
            double distance = distanceAtBestAngle(candidate, template.points());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestId = template.id();
            }
        }

        double halfDiagonal = 0.5 * Math.sqrt(SQUARE_SIZE * SQUARE_SIZE + SQUARE_SIZE * SQUARE_SIZE);
        double score = 1.0 - (bestDistance / halfDiagonal);

        if (bestId == null || score < MIN_SCORE) {
            return Optional.empty();
        }
        return Optional.of(new Result(bestId, score));
    }

    // ---- Pipeline normalizacji ----

    private List<Point> normalize(List<Point> raw) {
        List<Point> pts = resample(raw, RESAMPLE_POINTS);
        double indicativeAngle = indicativeAngle(pts);
        pts = rotateBy(pts, -indicativeAngle);
        pts = scaleToSquare(pts, SQUARE_SIZE);
        pts = translateToOrigin(pts);
        return pts;
    }

    private List<Point> resample(List<Point> points, int n) {
        double pathLen = pathLength(points);
        double interval = pathLen / (n - 1);
        if (interval <= 0) {
            // gest bez dlugosci (pojedynczy klik) - powiel punkt
            List<Point> flat = new ArrayList<>();
            Point p = points.get(0);
            for (int i = 0; i < n; i++) flat.add(p);
            return flat;
        }

        double accumulated = 0.0;
        List<Point> src = new ArrayList<>(points);
        List<Point> resampled = new ArrayList<>();
        resampled.add(src.get(0));

        for (int i = 1; i < src.size(); i++) {
            Point prev = src.get(i - 1);
            Point curr = src.get(i);
            double d = prev.distanceTo(curr);
            if (accumulated + d >= interval) {
                double t = (interval - accumulated) / d;
                double nx = prev.x() + t * (curr.x() - prev.x());
                double ny = prev.y() + t * (curr.y() - prev.y());
                Point q = new Point(nx, ny);
                resampled.add(q);
                src.add(i, q); // wstaw punkt posredni, kontynuuj od niego
                accumulated = 0.0;
            } else {
                accumulated += d;
            }
        }
        // domkniecie do dokladnie n punktow (bledy zaokraglen)
        while (resampled.size() < n) {
            resampled.add(src.get(src.size() - 1));
        }
        while (resampled.size() > n) {
            resampled.remove(resampled.size() - 1);
        }
        return resampled;
    }

    private double pathLength(List<Point> points) {
        double len = 0.0;
        for (int i = 1; i < points.size(); i++) {
            len += points.get(i - 1).distanceTo(points.get(i));
        }
        return len;
    }

    private Point centroid(List<Point> points) {
        double sx = 0, sy = 0;
        for (Point p : points) {
            sx += p.x();
            sy += p.y();
        }
        return new Point(sx / points.size(), sy / points.size());
    }

    private double indicativeAngle(List<Point> points) {
        Point c = centroid(points);
        Point first = points.get(0);
        return Math.atan2(first.y() - c.y(), first.x() - c.x());
    }

    private List<Point> rotateBy(List<Point> points, double radians) {
        Point c = centroid(points);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        List<Point> out = new ArrayList<>(points.size());
        for (Point p : points) {
            double dx = p.x() - c.x();
            double dy = p.y() - c.y();
            double nx = dx * cos - dy * sin + c.x();
            double ny = dx * sin + dy * cos + c.y();
            out.add(new Point(nx, ny));
        }
        return out;
    }

    private List<Point> scaleToSquare(List<Point> points, double size) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Point p : points) {
            minX = Math.min(minX, p.x());
            minY = Math.min(minY, p.y());
            maxX = Math.max(maxX, p.x());
            maxY = Math.max(maxY, p.y());
        }
        double width = Math.max(maxX - minX, 1e-9);
        double height = Math.max(maxY - minY, 1e-9);
        List<Point> out = new ArrayList<>(points.size());
        for (Point p : points) {
            double nx = (p.x() - minX) * (size / width);
            double ny = (p.y() - minY) * (size / height);
            out.add(new Point(nx, ny));
        }
        return out;
    }

    private List<Point> translateToOrigin(List<Point> points) {
        Point c = centroid(points);
        List<Point> out = new ArrayList<>(points.size());
        for (Point p : points) {
            out.add(new Point(p.x() - c.x(), p.y() - c.y()));
        }
        return out;
    }

    /** Golden Section Search - znajduje kat obrotu minimalizujacy dystans do wzorca. */
    private double distanceAtBestAngle(List<Point> points, List<Point> template) {
        double thetaA = Math.toRadians(-ANGLE_RANGE_DEG);
        double thetaB = Math.toRadians(ANGLE_RANGE_DEG);
        double thetaDelta = Math.toRadians(ANGLE_PRECISION_DEG);

        double x1 = GOLDEN_RATIO * thetaA + (1 - GOLDEN_RATIO) * thetaB;
        double f1 = distanceAtAngle(points, template, x1);
        double x2 = (1 - GOLDEN_RATIO) * thetaA + GOLDEN_RATIO * thetaB;
        double f2 = distanceAtAngle(points, template, x2);

        while (Math.abs(thetaB - thetaA) > thetaDelta) {
            if (f1 < f2) {
                thetaB = x2;
                x2 = x1;
                f2 = f1;
                x1 = GOLDEN_RATIO * thetaA + (1 - GOLDEN_RATIO) * thetaB;
                f1 = distanceAtAngle(points, template, x1);
            } else {
                thetaA = x1;
                x1 = x2;
                f1 = f2;
                x2 = (1 - GOLDEN_RATIO) * thetaA + GOLDEN_RATIO * thetaB;
                f2 = distanceAtAngle(points, template, x2);
            }
        }
        return Math.min(f1, f2);
    }

    private double distanceAtAngle(List<Point> points, List<Point> template, double radians) {
        List<Point> rotated = rotateBy(points, radians);
        return pathDistance(rotated, template);
    }

    private double pathDistance(List<Point> a, List<Point> b) {
        int n = Math.min(a.size(), b.size());
        double d = 0.0;
        for (int i = 0; i < n; i++) {
            d += a.get(i).distanceTo(b.get(i));
        }
        return d / n;
    }
}
