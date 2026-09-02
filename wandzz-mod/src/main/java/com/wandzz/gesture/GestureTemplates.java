package com.wandzz.gesture;

import java.util.ArrayList;
import java.util.List;

/**
 * Ksztalty gestow dla 18 zaklec moda.
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
 *   koszyk lvl 1 (6 czarow)             100.0% trafien, 0.0% trafien w obcy czar
 *   koszyk lvl 2 (14 czarow)             97.9% trafien, 0.0% trafien w obcy czar
 *   pelny koszyk (18 czarow)             96.8% trafien, 0.0% trafien w obcy czar
 *   RAZEM                               97.7% trafien, 0.0% trafien w obcy czar
 * </pre>
 *
 * <p>Trzy najnowsze pary (krzyz + schody, strzala + skreslenie, mur + Y) weszly
 * do zestawu po tym samym tescie. Uwaga dla nastepcy: kandydat na gest musial
 * tu przejdz DWA progi, nie jeden. Screening z 18 probami na czar przepuscil tarcze
 * herbowe dla {@code protego}, a ta przy 200 probach na ciasnala 1/800 kolo
 * {@code heal} (18 ksztaltow w koszyku = 0,1%). Dopiero trzy przesuniete kreski
 * daja ksztalt otwarty i waklesly, z ktorego kolka uklad nie da sie zlozyc.
 * Pelna weryfikacja: {@code gesture_eval.py} (40 prob) plus 200 prob na pare.
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

    // ------------------------------------------------------------------
    // partia "_potterowska" (6 czarow) - ksztalty dobrane tym samym pomiarem
    // ------------------------------------------------------------------

    /**
     * Lumos: strzala w gore - trzonek i dwa ramiona grotu, cztery segmenty,
     * dwa zawroty. "Wnosi swiatlo do gory".
     *
     * <p>Zostala wybrana SPOD pomiaru: pochodnia-z-podstawka (T z kreska w dol)
     * kradla 8,3% prob pochodni, a "krzyzyk krotki" nie dawal sie rozpoznac
     * (27,8% trafien) - za malo mas, zeby mysz go nie mylil z ptaszkiem.
     */
    public static List<Point> lumosStroke() {
        return List.of(
                new Point(0, 110),
                new Point(0, -70),
                new Point(-55, -5),
                new Point(0, -70),
                new Point(55, -5)
        );
    }

    /**
     * Nox: skreslone X - dwa ramiona na krzyz i kreska przez nie, piec
     * segmentow. "Zgaszone, skreslone".
     *
     * <p>Uwaga na puapke, ktora wyeliminowalismy pomiarem: samo X (bez kreski)
     * jest dla $1 obrotem krzyza-celownika {@link #revealStroke()} o 45 stopni.
     * $1 szuka obrotu w zakresie +/-45 stopni dla ksztaltow otwartych, wiec
     * "X" i "+" to ten sam gest - stad ta dodatkowa kreska, ktora psuje
     * symetrie i nie pozwala dwom czarom podkradac sie nawzajem.
     */
    public static List<Point> noxStroke() {
        return List.of(
                new Point(-90, -90),
                new Point(90, 90),
                new Point(-90, 90),
                new Point(90, -90),
                new Point(-40, -40),
                new Point(40, 40)
        );
    }

    /**
     * Accio: "miska z uszkiem" - trzy boki prostokata i przekatna w srodku,
     * cztery segmenty. Ksztalt "sciaga" punkty do jednego roda.
     *
     * <p>Kandydaci odrzuceni pomiarem: "lejek" (kradnie 4,4% - za blisko
     * amortyzatora skoku), "hak z kotwica" (55,6% trafien, bo ogon hakow jest
     * dla $1 szumem).
     */
    public static List<Point> accioStroke() {
        return List.of(
                new Point(-90, -90),
                new Point(-90, 90),
                new Point(90, 90),
                new Point(90, -90),
                new Point(-90, 90)
        );
    }

    /**
     * Wingardium Leviosa: daszek nad podloga - dwa ramiona w gore i kreska
     * u dolu, cztery segmenty. "Unies znad ziemi".
     *
     * <p>Ten sam daszek byl kiedys ODRZUCONY jako brama (patrz
     * {@link #barredGateStroke()}), i slusznie - jako LUK podkradal kolo. Tutaj
     * jest to lamany dach z podstawa (ostro, bez zakrzywienia), wiec ksztalt
     * zamyka sie w sobie i nie ma "polowy kola" w srodku.
     */
    public static List<Point> wingardiumStroke() {
        return List.of(
                new Point(-100, 80),
                new Point(0, -80),
                new Point(100, 80),
                new Point(-60, 80),
                new Point(60, 80)
        );
    }

    /**
     * Expelliarmus: dlngie Y - dwa ramiona w gore i dluga noga w dol, cztery
     * segmenty, jedno samoprzeciecie. "Wyrzuca z reki na boki".
     */
    public static List<Point> expelliarmusStroke() {
        return List.of(
                new Point(-90, -90),
                new Point(0, 0),
                new Point(90, -90),
                new Point(0, 0),
                new Point(0, 110)
        );
    }

    /**
     * Protego: mur z trzech kratek - poziome balki, kazdy krotszy i przesuniety,
     * piec segmentow, zadnego domkniecia.
     *
     * <p>ZMIANA WYGROWANA POMIAREM, nie gustem. Pierwotny ksztalt (tarcza
     * herbowa: gora, dwa boki, szpic na dole) wygrywal wszystkie testy screeningowe
     * 18-probowe, a i tak podkradal {@link #healStroke()} - przy 200 probach na
     * model wyszlo 1/800 trafien kola w tarcze. Dlaczego: kazdy CONVEX, domkniety
     * obwod o pieciu wierzcholkach jest po zaokragleniu przez mysz bliski kole, a
     * $1 nie widzi "tarczy", tylko sekwencje katow. Dlatego protego jest teraz
     * ksztaltem otwartym i wkleslym z natury - kola nie da sie z niego zlozyc.
     *
     * <p>To samo dotyczy "domu" (pentagon z daszkiem) i "muru z blankami" - oba
     * odpada na tym samym pomiarze.
     */
    public static List<Point> protegoStroke() {
        return List.of(
                new Point(-100, -80),
                new Point(100, -80),
                new Point(-70, 0),
                new Point(70, 0),
                new Point(-40, 80),
                new Point(40, 80)
        );
    }

    /**
     * Odkrycie: krzyz "celownik" - jedna pionowa kreska, przejazd po skosie w
     * gore i poziomka. Trzy segmenty, zero lukow, zero domkniecia.
     *
     * <p>Dlaczego nie "oko"? Bo ksztalt z dwoma plaskimi luczkami (gorna i dolna
     * powieka) jest dla $1 niczym innym jak micha, a micha jest najlepszym
     * sasiedziem kola - czyli leczenia (patrz punkt 1 w javadoku klasy). Krzyz
     * zostal wybrany pomiarem, nie gustem: w tools/gesture_eval.py kandydaci z
     * rodzinek "luk", "micha", "kolo z kreska" kradli obcy czar, a ten trafil
     * w siebie w 100% prob we wszystkich czterech modelach reki (w tym "uciety"
     * - gracz, ktory puszcza PPM w 72% sciezki).
     *
     * <p>Nie myli sie z pochodnia (T): tam poprzeczka jest NA KONCU trzonka, tu
     * pionowa kreska ja PRZECINA, wiec rozklad mas jest caly czas inny.
     */
    public static List<Point> revealStroke() {
        return List.of(
                new Point(0, -100),
                new Point(0, 100),
                new Point(-100, 0),
                new Point(100, 0)
        );
    }

    /**
     * Niewidzialnosc: schody w dol - trzy poziome stopnie polaczone dwoma
     * pionowymi stopniami. Piec segmentow, zadnego zawracania.
     *
     * <p>Ten ksztalt sprzedaje swoja nazwe: kazdy kolejny stopien jest krotszy i
     * nizej, czyli "znikam krok po kroku". Rysuje sie go naturalnie jednym
     * ruchem rekawki (w dol i w prawo), bez przejezdzania po wczesniejszych
     * kreskach - jako jedyny gest w tym pliku nie wymaga "powrotu", a mimo to
     * mierzy sie lepiej niz wiekszosc prostszych ksztaltow (100% trafien we
     * wszystkich czterech modelach reki, 0% kradziezy w obie strony).
     *
     * <p>Odrzucone kandydaty z tej samej rodziny: "kolo z kreska" (znikajace oko)
     * i "rama z X" - pierwszy wpadal w leczenie, drugi w brame przy ujetym
     * ostatnim boku.
     */
    public static List<Point> invisibilityStroke() {
        return List.of(
                new Point(-110, -70),
                new Point(-40, -70),
                new Point(-40, -10),
                new Point(20, -10),
                new Point(20, 50),
                new Point(90, 50)
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
