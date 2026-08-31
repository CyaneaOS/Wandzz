package com.wandzz.gesture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

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
    private static final double ANGLE_RANGE_DEG = 45.0;
    private static final double ANGLE_PRECISION_DEG = 2.0;
    /** Minimalny wynik (0..1) ponizej ktorego gest jest odrzucany jako nierozpoznany. */
    public static final double MIN_SCORE = 0.72;
    /**
     * O ile najlepiej trafiony wzorzec MUSI byc lepszy od wicelidera, zeby wogole
     * cos rzucic. 0.035 to okolo 6 px na 250-punktowej siatce.
     *
     * Dlaczego to istnieje: przy 10 ksztaltach w jednym koszyku $1 potrafi dac
     * kolowi 0.893, a "dwoch kolom" 0.888 - i gracz dostaje ZLY czar zamiast
     * zadnego. Pomytka jest gorsza niz odmowa, bo odmowe powtarzasz, a zlego
     * czaru sie nie spodziewasz (np. rozwalona sciana w domostwie). Z marginesem
     * jest "narysuj dokladniej", nigdy "to nie to, o co prosilem".
     */
    public static final double AMBIGUITY_MARGIN = 0.035;

    private final List<GestureTemplate> templates = new ArrayList<>();

    public void addTemplate(String id, List<Point> rawPoints) {
        templates.add(new GestureTemplate(id, normalize(rawPoints)));
    }

    /**
     * @param runnerUp wynik drugiego najlepszego wzorca (0.0, jesli koszyk mial jeden element)
     */
    public record Result(String templateId, double score, double runnerUp) {

        public boolean ambiguous() {
            return this.score() - this.runnerUp() < DollarOneRecognizer.AMBIGUITY_MARGIN;
        }
    }

    /** Zwraca najlepiej dopasowany wzorzec, jesli wynik przekracza MIN_SCORE. */
    public Optional<Result> recognize(List<Point> rawPoints) {
        return recognize(rawPoints, id -> true);
    }

    /**
     * Jak wyzej, ale w waszym koszyku: {@code allowed} przyjmuje tylko te id,
     * ktore dany rzadca faktycznie moze rzucic (patrz
     * {@code SpellRegistry#castableBy}). $1 ma 10 ksztaltow i nie jest w stanie
     * ich rozdzielic z szumem myszy - na 1.21.11 u nas pelny koszyk trafil 70%,
     * a z filtrem + marginesem 92% przy ZERO trafionych zlych czarov.
     */
    public Optional<Result> recognize(List<Point> rawPoints, Predicate<String> allowed) {
        return bestResult(rawPoints, allowed)
                .filter(r -> r.score() >= MIN_SCORE && !r.ambiguous());
    }

    /**
     * Najlepsze dopasowanie BEZ progu. Sluzy tylko do komunikatu zwrotnego
     * ("co moj gest przypominalo najbardziej"), zeby latwo strojic MIN_SCORE.
     */
    public Optional<Result> bestMatch(List<Point> rawPoints) {
        return bestResult(rawPoints, id -> true);
    }

    public Optional<Result> bestMatch(List<Point> rawPoints, Predicate<String> allowed) {
        return bestResult(rawPoints, allowed);
    }

    private Optional<Result> bestResult(List<Point> rawPoints, Predicate<String> allowed) {
        if (rawPoints.size() < 2 || templates.isEmpty()) {
            return Optional.empty();
        }
        List<Point> candidate = normalize(rawPoints);

        String bestId = null;
        double bestDistance = Double.MAX_VALUE;
        double secondDistance = Double.MAX_VALUE;

        for (GestureTemplate template : templates) {
            if (!allowed.test(template.id())) {
                continue;
            }
            // Ksztalty zamkniete (kolo, trojkat, spirala) mozna zaczac w kazdym
            // wierzcholku -> wskazujacy kat niczego nie normalizuje, wiec
            // przeszukujemy pelne +-180 stopni. Dla otwartych kreskow wystarcza
            // klasyczne +-45 (tansze i mniej podatne na pomyly).
            boolean open = isOpen(candidate);
            double distance = distanceAtBestAngle(candidate, template.points(), open);
            if (distance < bestDistance) {
                secondDistance = bestDistance;
                bestDistance = distance;
                bestId = template.id();
            } else if (distance < secondDistance) {
                secondDistance = distance;
            }
        }

        double halfDiagonal = 0.5 * Math.sqrt(SQUARE_SIZE * SQUARE_SIZE + SQUARE_SIZE * SQUARE_SIZE);
        double score = 1.0 - (bestDistance / halfDiagonal);
        if (bestId == null) {
            return Optional.empty();
        }
        // przy jednym dozwolonym wzorcu "runnerUp" pozostaje MAX_VALUE -> wynik
        // 0.0, wiec ambiguous() NIE wyskoczy (odleglosc ujemna = brak konkurenta)
        double runnerUp = secondDistance == Double.MAX_VALUE
                ? 0.0 : 1.0 - (secondDistance / halfDiagonal);
        return Optional.of(new Result(bestId, score, runnerUp));
    }

    /** Czy sciezka jest "otwarta" (koniec daleko od poczatku) - kryterium z $1+. */
    private static boolean isOpen(List<Point> pts) {
        Point first = pts.get(0);
        Point last = pts.get(pts.size() - 1);
        double dx = first.x() - last.x();
        double dy = first.y() - last.y();
        double mean = 0.0;
        for (int i = 1; i < pts.size(); i++) {
            mean += pts.get(i - 1).distanceTo(pts.get(i));
        }
        mean /= (pts.size() - 1);
        return Math.sqrt(dx * dx + dy * dy) > 2.0 * mean;
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
        // Skalowanie JEDNOLITE (klasyczne $1): dzielimy przez dluzszy bok bboxa.
        // Wczesniejsze skalowanie osobno X i Y rozciagalo np. lekko krzywa
        // kreske (3 px drygu w Y) do pelnego kwadratu 250 - szum myszy
        // byl wtedy wiekszy niz sam ksztalt i zadny gest sie nie zgadzal.
        double scale = Math.max(maxX - minX, maxY - minY);
        if (scale < 1e-9) {
            scale = 1.0;
        }
        List<Point> out = new ArrayList<>(points.size());
        for (Point p : points) {
            double nx = (p.x() - minX) * (size / scale);
            double ny = (p.y() - minY) * (size / scale);
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

    /**
     * Najlepszy dystans do wzorca: przeszukiwanie siatkowe po calym zakresie
     * + lokalne doscienie.
     *
     * Oryginalne $1 uzywa Golden Section Search, ale GSS zaklada, ze funkcja
     * kosztu ma JEDNO minimum. Ksztalty symetryczne (trojkat = 3 minima, kolo
     * = praktycznie stale) lamia ten zalozenie i GSS potrafil zatrzymac sie na
     * zlej "sztorcie" - przez to np. trojkat wypadal z wynikiem 0.56, czyli
     * ponizej progu, mimo ze graczy narysowal poprawnie.
     * Siatka co 15/6 stopni + krok 1 stopnia wokoly najtanszego kata jest
     * przy 7 wzorcach zupealnie darmowa (robimy to raz, po puszczeniu PPM).
     */
    private double distanceAtBestAngle(List<Point> points, List<Point> template, boolean open) {
        double range = open ? ANGLE_RANGE_DEG : 180.0;
        double step = open ? 6.0 : 15.0;
        double bestAngle = 0.0;
        double bestDistance = Double.MAX_VALUE;
        for (double deg = -range; deg <= range; deg += step) {
            double d = distanceAtAngle(points, template, Math.toRadians(deg));
            if (d < bestDistance) {
                bestDistance = d;
                bestAngle = deg;
            }
        }
        for (int i = -4; i <= 4; i++) {
            double d = distanceAtAngle(points, template, Math.toRadians(bestAngle + i));
            if (d < bestDistance) {
                bestDistance = d;
            }
        }
        return bestDistance;
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
