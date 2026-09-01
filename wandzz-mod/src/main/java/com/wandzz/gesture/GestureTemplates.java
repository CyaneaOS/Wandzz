package com.wandzz.gesture;

import java.util.ArrayList;
import java.util.List;

/**
 * Ksztalty gestow dla 10 zaklec moda.
 *
 * <p>TO NIE JEST zbior "ladnych" ikon. {@code $1} jest nieczuly na obrot, skale i
 * predkosc rysowania, a po normalizacji z ksztaltu zostaja WYLACZNIE kolejnosc
 * katow i proporcje bokow. Dwa gesty o tej samej liczbie ramion i zblizonych
 * katach sa dla rozpoznawacza TYM SAMYM ksztaltem - i to jest zrodlo skarg graczy
 * w stylu "rysowalem leczenie, a rzucila sie pochodnia".
 *
 * <p>Dwie zasady, ktore rzadza tym plikiem (zmierzone narzedziami z
 * {@code wandzz-mod/tools/}, patrz sekcja "Gestury" w README):
 * <ol>
 *   <li><b>zaden gest nie jest lukiem, micha ani trojkatem z ogonem</b>. Kazdy
 *       ksztalt z jednym plaskim "przejazdem" (luk bramy, gorka skoku, swieczka)
 *       jest najblizszym sasiedziem KOLA - tylko ze kolko rysowane mysza prawie
 *       nigdy nie jest domkniete, wiec takie niedokonczone kolo wpadalo do
 *       obcego czaru w 100% prob. Po zamianie luku na "N" i swieczki na "T" ten
 *       sam test daje 0%.</li>
 *   <li><b>im mniej wierzcholkow, tym lepiej</b>. Gwiazda o dziesieciu
 *       wierzcholkach (dawna kula ognia) wymagala "kropkowania" i mierzyla 58%
 *       trafien; prostopadle kreski licza sie w tescie lepiej niz cokolwiek
 *       ozdobnego, a w grze lepiej niz cokolwiek, czego nie da sie narysowac
 *       jednym machniecia.</li>
 * </ol>
 *
 * <p>Wyniki tego zestawu - {@code python3 tools/gesture_eval.py}, cztery modele
 * reki (od rysika do drzacej myszy, w tym "urwany ogon"), trzy koszyki dostepnych
 * czarow, 40 prob na czar:
 * <pre>
 *   koszyk lvl 1 (4 czary)            100.0% trafien, 0.0% trafien w obcy czar
 *   koszyk lvl 2 (8 czarow)            96.4% trafien, 0.0% trafien w obcy czar
 *   pelny koszyk (10 czarow)           94.3% trafien, 0.0% trafien w obcy czar
 * </pre>
 * Ten sam test na zestawie z poprzedniej rundy (swieczka, luk, gwiazda):
 * 93.1% trafien, ale 40-100% RZUCANYCH ZLYCH CZAROW dla leczenia, kuli ognia i
 * oddechu smoka w modelu "urwany ogon". To byl dokladnie blad zgloszony przez
 * gracza. Zero trafien w obcy czar jest wazniejsze niz kilka punktow procentu
 * wiecej - {@code DollarOneRecognizer.AMBIGUITY_MARGIN} zwyklej odrzuci gest,
 * a gracz zobaczy komunikat zamiast rzucic co innego.
 *
 * <p>Rysowanie: wszystko jednym ciagiem (PPM trzymany), bez odrywania rekawki od
 * myszy. Punkty sa w przestrzeni szablonowej +/- 200; rozpoznawacz sam je
 * normalizuje, te wartosci sluza tylko do wyswietlenia ksztaltu w ksiece zaklec.
 */
public final class GestureTemplates {

    private GestureTemplates() {
    }

    // ------------------------------------------------------------------
    // lvl 1
    // ------------------------------------------------------------------

    /**
     * Uderzenie: "ptaszek" - krotka kreska w prawo, dluga w gore i w prawo. Trzy
     * segmenty, jedno zawrocenie.
     *
     * <p>Wczesniej bylo tu "V", ale "V" po normalizacji to to samo co daszek,
     * micha i kazdy inny ksztalt z dwoma ramionami - przy urwanym gescie $1
     * wskakiwal w obcy czar w co czwartej probie. Ptaszek ma asymetryczne ramiona
     * (jedno krotkie, jedno dlugie), wiec nie ma z czym go pomylic.
     */
    public static List<Point> strikeStroke() {
        return List.of(
                new Point(-100, 60),
                new Point(-20, 60),
                new Point(60, -80),
                new Point(100, -20)
        );
    }

    /**
     * Niszczenie bloku: kratka "#" - cztery segmenty w dwoch rzedach, rysowane
     * jednym ciagiem (gora w prawo, skos w dol, dol w prawo). Bez zmian od
     * poprzedniej rundy: w kazdym tescie 100% trafien i zero kradziezy w obie
     * strony.
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
     * Pochodnia: plaska kreska (plmien) nad trzonkiem, czyli litera "T" - trzy
     * segmenty, jedno samoprzeciecie.
     *
     * <p>Swieczka (trojkat z podstawa) zostala WYRZUCONA. Trojkat z ogonem jest
     * najblizszym sasiedziem zarowno kola narysowanego reka, jak i "V" - stad
     * skargi "leczym, a rzuca pochodnie" i "uderzenie rzuca pochodnie albo nic".
     * Tu nie ma luku ani domknietego obwodu, wiec pochodnia nie jest w stanie
     * podkrasc zadnego kola.
     */
    public static List<Point> torchStroke() {
        return List.of(
                new Point(-90, -80),
                new Point(90, -80),
                new Point(0, -80),
                new Point(0, 80)
        );
    }

    /**
     * Skok: "amortyzator" - w gore, w dol, w gore, w dol (cztery segmenty, dwie
     * dlugie pionowe nogi po bokach).
     *
     * <p>Padla "gorka" (plaski podbieg, garb, ladowanie): po zaokragleniu narozy
     * przez mysz to byl dokladnie ten sam ksztalt co plomien kuli ognia, a padle
     * rowniez "V z ogonem", ktore bylo tym samym co uderzenie. Amortyzator ma
     * rozklad mas inny niz jakikolwiek garb, luk czy micha, wiec nie walczy ani z
     * kula ognia, ani z brama.
     */
    public static List<Point> leapStroke() {
        return List.of(
                new Point(-95, 70),
                new Point(-95, -50),
                new Point(0, 50),
                new Point(95, -50),
                new Point(95, 70)
        );
    }

    // ------------------------------------------------------------------
    // lvl 2
    // ------------------------------------------------------------------

    /**
     * Leczenie: kolko - ksztalt wybrany przez gracza, nie przez nas, i zostaje.
     *
     * <p>Wazne: to, ze leczenie "jest mylone z pochodnia", nie bylo wina kola.
     * Wina byla pochodnia (trojkat z ogonem) i brama (luk) - oba sa lepszymi
     * wzorcami dla kola narysowanego reka, a mysz prawie nigdy go nie domyka. Po
     * usunieciu luku i trojkata z zestawu kolo trafia w siebie w 100% prob w
     * trzech z czterech modeli reki, a w czwartym (gest urwany w 28%) bywa
     * odrzucane - odmowa, nie obcy czar.
     */
    public static List<Point> healStroke() {
        return circle(32);
    }

    /**
     * Kolo - 33 punkty co 11.25 stopnia. Ten sam wzor jest w
     * {@code tools/gesture_eval.py}, wiec pomiar w Pythonie i szablon w grze nie
     * moga sie rozjechac.
     *
     * <p>Wariant "kolo z przerwa" (300 stopni) zostal przetestowany i ODRZUCONY:
     * $1 ufa kolejnosci punktow, nie intencji, wiec szablon z przerwa byl gorszym
     * wzorcem dla kol domknietych (58-88% trafien zamiast 100%), a urwane kolo i
     * tak ladowalo u kogos innego.
     */
    public static List<Point> circle(int samples) {
        List<Point> pts = new ArrayList<>();
        for (int i = 0; i <= samples; i++) {
            double t = 2 * Math.PI * i / samples;
            pts.add(new Point(Math.cos(t) * 100, Math.sin(t) * 100));
        }
        return pts;
    }

    /**
     * Kula ognia: plomien w gore, czyli domkniety trojkat (trzy segmenty).
     *
     * <p>Na prosbe gracza - prosciej niz dawna gwiazda z dziesiecioma
     * wierzcholkami, ktora wymagala "kropkowania" i przy kazdym pospiesznym
     * pociagnieciu dawala inny ksztalt (58% trafien). Trojkat rysuje sie sam:
     * baza w prawo, skos w gore, skos w dol. Nie myli sie z "V" (brak podstawy)
     * ani z paska pochodni (tam jest prostopadle ramie w dol, tu zamkniecie).
     */
    public static List<Point> flameTriangle() {
        return List.of(
                new Point(-90, 70),
                new Point(90, 70),
                new Point(0, -80),
                new Point(-90, 70)
        );
    }

    /**
     * Oddech smoka: fala z dwoma garbami, rysowana od lewej do prawej. Spirala
     * odpada (po resamplingu jej ksztalt zalezal od tego, KIEDY gracz przestal
     * rysowac - sredni wynik 0.68, ponizej progu, czyli czaru praktycznie nie dalo
     * sie rzucic). Fala ma garby zawsze.
     */
    public static List<Point> breathWave() {
        List<Point> pts = new ArrayList<>();
        for (int i = 0; i <= 44; i++) {
            pts.add(new Point(-100.0 + 200.0 * i / 44.0, 80.0 * Math.sin(2.0 * Math.PI * i / 44.0)));
        }
        return pts;
    }

    /**
     * Brama: dwa filary i rygiel na skos - litera "N" (trzy segmenty).
     *
     * <p>Poprzedni ksztalt (sam luk nad ziemia) musial padac: luk to polowa kola
     * leczacego, a $1 dla ksztaltow domknietych szuka obrotu w zakresie
     * +/- 180 stopni, wiec niedokonczone kolo wpadalo w brame w 100% prob.
     * "Mostek" (dwie nogi i daszek) byl o jeden krok lepszy, ale wciaz podkradal
     * urwane kolo w 100% prob. "N" ma proste krawedzie i jedno
     * samoprzeciecie - w tescie "urwany ogon" urwane kolo trafia do leczenia albo
     * jest odrzucane, nigdy do bramy.
     */
    public static List<Point> barredGateStroke() {
        return List.of(
                new Point(-80, 80),
                new Point(-80, -80),
                new Point(80, 80),
                new Point(80, -80)
        );
    }

    // ------------------------------------------------------------------
    // lvl 3+
    // ------------------------------------------------------------------

    /**
     * Teleportacja: DWA KWADRATY POLACZONE KRESKA - ksztalt potwierdzony przez
     * gracza jako dzialajacy, wiec zostaje bez zmian. Dziewiec odcinkow to
     * najdluzszy gest w zestawie i przez to nie do pomylenia z czymkolwiek innym.
     *
     * <p>Jedyna slaba strona, zmierzona: w pelnym koszyku (rdzen 3, wszystko
     * dostepne) co czwarty bardzo niedokladny gest jest ODRZUCANY, bo kratka
     * niszczenia jest wiceliderem. To odmowa plus komunikat, nie obcy czar - i o
     * to w tym chodzilo.
     */
    public static List<Point> twinSquares() {
        return List.of(
                new Point(-175, 60), new Point(-175, -60), new Point(-65, -60), new Point(-65, 60),
                new Point(65, 60),
                new Point(65, -60), new Point(175, -60), new Point(175, 60), new Point(65, 60)
        );
    }

    /**
     * Bomba: romb z kreska w srodku - piec segmentow i domkniety obwod, czego nie
     * ma zadny inny gest. Litera "X" zostala odrzucona: po ucieciu jednego
     * ramienia zostaje z niej "V", czyli dawne uderzenie.
     */
    public static List<Point> diamondTick() {
        return List.of(
                new Point(-100, 0), new Point(0, -100), new Point(100, 0),
                new Point(0, 100), new Point(-100, 0),
                new Point(0, -40), new Point(0, 40)
        );
    }
}
