package com.wandzz.gesture;

import java.util.ArrayList;
import java.util.List;

/**
 * Ksztalty gestow dla 10 zaklec moda.
 *
 * TO NIE JEST zbiornik "ladnych" ikon. {@code $1} jest nieczuly na obroty, skale
 * i predkosc rysowania - ale wlasnie dlatego dwa ksztalty o tej samej liczbie
 * rameion i zblizonych katach sa dla niego IDENTYCZNE. Dawniej zestaw byl
 * dobierany "na oko" i gracz to czul: kolo (heal) kradnie trojkat (fireball),
 * "V" (strike) kradnie "V" (leap), kwadrat z przekatna (break_block) kradnie
 * trojkat. Zmierzony wynik starego zestawu: 70% trafien i 11% RZUCANYCH ZLYCH
 * czarow. Ten jest dobrany przeszukiwaniem (port tego rozpoznawacza w Pythonie,
 * ~34 "narysowanych" prob na ksztalt, z dryfem brownowskim reki, losowa skala
 * 0.5-1.7 i ucietym ogonem) - patrz sekcja "Gestys" w README:
 *
 *   pelny koszyk 10 ksztaltow: 92% trafien, 8% odmowa, 0% zly czar
 *   koszyk rdzeni lvl 1-2:     95-100% trafien, 0% zly czar
 *
 * Dwie rzeczy trzymaja te liczby:
 *  <ol>
 *    <li>kazdy ksztalt ma inna LICZBE segmentow lub inna sygnature katow - nie
 *        "inny ksztalt" ogolnie, bo po normalizacji ksztalt to wlasnie katy,</li>
 *    <li>{@code DollarOneRecognizer.AMBIGUITY_MARGIN} - przy zbyt malym
 *        wskazaniu nad wiceliderem gest jest odrzucany zamiast rzucic co innego.</li>
 *  </ol>
 *
 * Rysowanie: wszystko JEDNYM ciagiem (PPM trzymany), bez odrywania rekawki od
 * myszy. Kolejnosci punktow sa w przestrzeni "szablonowej" +/- 200 jednostek;
 * Recognizer sam to normalizuje, te liczby sluza tylko do odczytu ksztaltu.
 */
public final class GestureTemplates {

    private GestureTemplates() {
    }

    // ------------------------------------------------------------------
    // lvl 1
    // ------------------------------------------------------------------

    /**
     * Uderzenie: "V" - dwa ramiona, jeden ostry kat (100% w koszyku lvl 1).
     * Rysuj w dol i w gore, jakbyles przykladnal mlotkiem o podloge.
     */
    public static List<Point> strikeStroke() {
        return List.of(
                new Point(-100, -80),
                new Point(0, 80),
                new Point(100, -80)
        );
    }

    /**
     * Niszczenie bloku: kratka "#" - cztery segmenty w dwoch rzedach, rysowane
     * jednym ciagiem (gora w prawo, skos w dol, dol w prawo). Wybrane zamiast
     * "kwadratu z przekatna": tamten ksztalt to po normalizacji trojkat plus
     * ogon i $1 mylil go z kula ognia w 58% prob.
     */
    public static List<Point> breakBlockStroke() {
        return List.of(
                new Point(-90, -40),
                new Point(90, -40),
                new Point(-60, -40),
                new Point(-60, 40),
                new Point(60, 40),
                new Point(60, -40),
                new Point(90, -40)
        );
    }

    /**
     * Pochodnia: swieczka - podstawa w prawo, plomien w gore, powrot na srodek.
     * Cztery segmenty z jednym samoprzecinajacym sie wierzcholkiem, wiec nie myli
     * sie z "V" (dwa) ani z kreska z hakiem (dwa).
     */
    public static List<Point> torchStroke() {
        return List.of(
                new Point(-40, 90),
                new Point(40, 90),
                new Point(0, -90),
                new Point(-40, 90),
                new Point(0, 90)
        );
    }

    /**
     * Skok: "przeskoczenie przeszkody" - plaski podbieg, garb, plaskie ladowanie.
     * Cztery segmenty i dwa katy rozwarte: uderzenie ma dwa segmenty i jeden kat.
     * Zastapilo "V z ogonem", ktore bylo DOKLADNIE tym samym ksztaltem co uderzenie
     * (gracz trafil w strike zamiast w leap w 90% prob - to byl jego zgloszony blad).
     */
    public static List<Point> leapStroke() {
        return List.of(
                new Point(-110, 60),
                new Point(-40, 60),
                new Point(-10, -70),
                new Point(20, 60),
                new Point(110, 60)
        );
    }

    // ------------------------------------------------------------------
    // lvl 2
    // ------------------------------------------------------------------

    /**
     * Leczenie: kolko - ksztalt wybrany przez gracza, nie przez nas. Zmierzony
     * rezultat: 85% trafien w pelnym koszyku dziesieciu ksztaltow i 100% w koszyku
     * rdzeni poziomu 1-2. Ryzyko jest jedno: kolko i luk bramy to ten sam rod
     * krzywizn, dlatego brama dostala luk OTWARTY (nie domkniety) a rozpoznawanie
     * ma jeszcze margines niejednoznacznosci.
     */
    public static List<Point> healStroke() {
        return circle(32);
    }

    /** Kola, trojkaty i spirale liczone z tych samych wzorow - stad helper. */
    public static List<Point> circle(int samples) {
        List<Point> pts = new ArrayList<>();
        for (int i = 0; i <= samples; i++) {
            double t = 2 * Math.PI * i / samples;
            pts.add(new Point(Math.cos(t) * 100, Math.sin(t) * 100));
        }
        return pts;
    }

    /**
     * Kula ognia: "burst" - dziesieciokat wklesly z piecioma wyzlobieniami.
     * Wybrane zamiast trojkata, bo trojkat po domkneciu daje te same cztery boki co
     * brama i $1 nie widzial roznicy (trafienia 42%). Pieciu wglebien nie ma zadny
     * inny gest w zestawie - to jest jego sygnatura.
     */
    public static List<Point> burst() {
        return List.of(
                new Point(0, -110), new Point(30, -30), new Point(110, -10),
                new Point(40, 20), new Point(60, 100), new Point(0, 50),
                new Point(-60, 100), new Point(-40, 20), new Point(-110, -10),
                new Point(-30, -30), new Point(0, -110)
        );
    }

    /**
     * Oddech smoka: fala z dwoma garbami. Spirala byla za droga dla $1: po
     * resamplingu 64 punktami jej ksztalt zalezy od tego, KIEDY gracz przestal
     * rysowac, i sredni wynik spadl do 0.68, czyli ponizeg progu - czaru praktycznie
     * nie dalo sie rzucic. Fala ma garby zawsze, wiec jest stabilna.
     */
    public static List<Point> breathWave() {
        List<Point> pts = new ArrayList<>();
        for (int i = 0; i <= 44; i++) {
            pts.add(new Point(-100.0 + 200.0 * i / 44.0, 80.0 * Math.sin(2.0 * Math.PI * i / 44.0)));
        }
        return pts;
    }

    /**
     * Brama: luk nad ziemia (polokolko, otwarte od dolu). Pelny prostokat
     * odpuilismy, bo po normalizacji to samo co kolko leczenia; luk ma te same
     * boki, ale niesymetryczne domkniecie - i ewentualna kolizje wylapuje
     * margines, nie rzucenie zlego czaru.
     */
    public static List<Point> archStroke() {
        List<Point> pts = new ArrayList<>();
        for (int i = 0; i <= 22; i++) {
            double x = -95.0 + 190.0 * i / 22.0;
            double under = 1.0 - (x / 95.0) * (x / 95.0);
            pts.add(new Point(x, -70.0 * Math.sqrt(Math.max(0.0, under))));
        }
        return pts;
    }

    // ------------------------------------------------------------------
    // lvl 3+
    // ------------------------------------------------------------------

    /**
     * Teleportacja: DWA KWADRATY POLACZONE KRESKA - dokladnie to, o co prosiles.
     * Rysuj tak: lewy kwadrat (cztery boki, konczysz w prawym dolnym rogu),
     * kreska w prawo, prawy kwadrat (cztery boki, domknij). Dziewiec odcinkow -
     * najdluzszy gest w zestawie i przez to nie do pomylenia z czymkolwiek innym,
     * poza nim samym skroconym o pol - dlatego dostal takze margines, nie tylko
     * punktacje. (1.7 raza czulszy na uciecie ogona niz reszta zestawu.)
     */
    public static List<Point> twinSquares() {
        return List.of(
                new Point(-175, 60), new Point(-175, -60), new Point(-65, -60), new Point(-65, 60),
                new Point(65, 60),
                new Point(65, -60), new Point(175, -60), new Point(175, 60), new Point(65, 60)
        );
    }

    /**
     * Bomba: romb z kreska w srodku. Litera "X" poszla do uderzenia? Nie - X
     * wypadla z zestawu, bo po ucieciu ogona (gracz puszcza PPM sekunde wczesniej)
     * zostaje z niej "V", czyli uderzenie. Romb + pion to piec segmentow i domknieta
     * obwodu, czego nie ma zadny inny gest.
     */
    public static List<Point> diamondTick() {
        return List.of(
                new Point(-100, 0), new Point(0, -100), new Point(100, 0),
                new Point(0, 100), new Point(-100, 0),
                new Point(0, -40), new Point(0, 40)
        );
    }

    /** Zygzak zostaje w rejestrze jako ksztalt "awaryjny" (patrz README, Gestury). */
    public static List<Point> zigzag() {
        return List.of(
                new Point(-100, -100), new Point(20, -100), new Point(-60, 0),
                new Point(100, 0), new Point(-20, 100), new Point(100, 100)
        );
    }

    /** Kolko z petla (1.5 obrotu) - awaryjny wariant leczenia, jesli kolo kradnie luk. */
    public static List<Point> snail(int samples) {
        List<Point> pts = new ArrayList<>();
        for (int i = 0; i <= samples; i++) {
            double t = 1.5 * 2 * Math.PI * i / samples;
            pts.add(new Point(Math.cos(t) * 100, Math.sin(t) * 100));
        }
        return pts;
    }
}
