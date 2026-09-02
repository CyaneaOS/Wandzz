#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Siatka UV modelu koniowatego vs. arkusz tekstury: `python3 tools/unicorn_uv.py`.

Po to jest: MC nie ma podgladu modelu, a najczestszy blad "tekstury robi gracz"
to nie zle piksele, tylko zle *wspolrzedne*. Narzedzie czyta wartosci prosto z
UnicornModel.java (drugiej, rekopisowej tabeli nie ma, wiec nie moze sie
rozjesc) i mowi, ktora twarz bryly trafia w zamalowany fragment arkusza, a ktora
w przezroczystosc - a wiec zniknie w grze.

Formule rozwinienc bierze wprost z `net.minecraft.client.model.geom.ModelPart$Cube`
(zrzut zrodel 1.21.11): dla boxu (w, h, d) w punkcie (u, v)
    u1 = u + d       u2 = u + d + w     u22 = u + d + 2w
    u3 = u + 2d + w  u4 = u + 2d + 2w   v1 = v + d   v2 = v + d + h
    DOWN (u1..u2, v0..v1)   UP (u2..u22, v0..v1)
    WEST (u0..u1, v1..v2)   NORTH (u1..u2, v1..v2)
    EAST (u2..u3, v1..v2)   SOUTH (u3..u4, v1..v2)
Stad cala bryla zajmuje (2d + 2w) x (d + h) pikseli - dlatego "tuluw 10x10x22"
potrzebuje paska 64x32, a nie 22x10. I dlatego ten tuluw jest w modelu rozbity na
dwie bryly po 11 kratek: dwoch nie wolno nakladac na siebie geometrycznie, ale
ich UV moze brac te same piksele.

Tryby:
    --report             (domylnie) tabela + lista twarzy bez pikseli
    --png WYJSCIE        podglad: Twoja tekstura x skala + obwiednie bryl z numerem
    --dopasuj            sugestie UV, ktore nie wchodza w przezroczystosc
    --model, --tekstura  nadpisanie sciezek (inny model, inny arkusz)

Podglad NIGDY nie dotyka pliku wejsciowego - pisze tylko do --png.
"""
from __future__ import annotations

import argparse
import os
import pathlib
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
MOD = os.path.dirname(HERE)
sys.path.insert(0, HERE)
import png  # noqa: E402  (tools/png.py - minimalny de/inkoder PNG bez zaleznosci)

MODEL = os.path.join(MOD, 'src', 'client', 'java', 'com', 'wandzz', 'client', 'UnicornModel.java')
TEKSTURA = os.path.join(MOD, 'src', 'main', 'resources', 'assets', 'wandzz',
                        'textures', 'entity', 'unicorn_txt.png')

# kolory obwiedni (RGB) - kolejne bryle roznia sie wyraznie nawet przy 1 px
PALETA = [(255, 60, 60), (60, 220, 60), (70, 130, 255), (255, 210, 40),
          (255, 120, 240), (60, 230, 220), (255, 160, 60), (170, 90, 255),
          (90, 255, 140), (255, 90, 170), (120, 180, 255), (230, 230, 90)]
CYFRY = {
    '0': ('###', '#.#', '#.#', '#.#', '###'), '1': ('.#.', '##.', '.#.', '.#.', '###'),
    '2': ('###', '..#', '###', '#..', '###'), '3': ('###', '..#', '###', '..#', '###'),
    '4': ('#.#', '#.#', '###', '..#', '..#'), '5': ('###', '#..', '###', '..#', '###'),
    '6': ('###', '#..', '###', '#.#', '###'), '7': ('###', '..#', '..#', '..#', '..#'),
    '8': ('###', '#.#', '###', '#.#', '###'), '9': ('###', '#.#', '###', '..#', '###'),
}


def bryle_z_javy(sciezka):
    """Lista (nazwa, u, v, w, h, d) dla kazdego addBox + rozmiar arkusza.

    Parsujemy Java zamiast trzymac dubel w Pythonie: dokladnie ta lekcja, co
    gesture_set.py --sync. Wymaganie techniczne: we wskazanych addBox moga byc
    WYLACZNIE literaly (zadnych operatorow ?: i zadnych zmiennych), patrz tez
    komentarz przy nogach w UnicornModel.
    """
    tekst = open(sciezka, encoding='utf8').read()
    m = re.search(r'LayerDefinition\.create\(\s*\w+\s*,\s*(\d+)\s*,\s*(\d+)\s*\)', tekst)
    if not m:
        raise SystemExit('w %s nie ma LayerDefinition.create(mesh, W, H)' % sciezka)
    arkusz = (int(m.group(1)), int(m.group(2)))

    token = re.compile(
        r'''addOrReplaceChild\(\s*"(?P<nazwa>[\w_]+)"'''
        r'''|texOffs\(\s*(?P<u>-?\d+)\s*,\s*(?P<v>-?\d+)\s*\)'''
        r'''|addBox\(\s*(-?\d+(?:\.\d+)?F?)\s*,\s*(-?\d+(?:\.\d+)?F?)\s*,\s*'''
        r'''(-?\d+(?:\.\d+)?F?)\s*,\s*(-?\d+(?:\.\d+)?F?)\s*,\s*'''
        r'''(-?\d+(?:\.\d+)?F?)\s*,\s*(-?\d+(?:\.\d+)?F?)''')
    wynik = []
    biezaca = 'root'
    uv = (0, 0)
    for m in token.finditer(tekst):
        if m.group('nazwa'):
            biezaca = m.group('nazwa')
        elif m.group('u') is not None:
            uv = (int(m.group('u')), int(m.group('v')))
        else:
            # grupy: (nazwa, u, v) + szesc liczb boxu -> addBox(x, y, z, w, h, d)
            *_, w, h, d = (float(g.rstrip('F')) for g in m.groups()[3:9])
            wynik.append((biezaca, uv[0], uv[1], w, h, d))
    if len(wynik) < 5:
        raise SystemExit('znalezione tylko %d bryl - regex nie trafia w addBox? '
                         'Czy w addBox sa same literaly?' % len(wynik))
    return wynik, arkusz


def twarze(u, v, w, h, d):
    """Szesc prostokatow rozwinienc - dokladnie wg ModelPart$Cube."""
    u1, u2, u22 = u + d, u + d + w, u + d + 2 * w
    u3, u4 = u + 2 * d + w, u + 2 * d + 2 * w
    v1, v2 = v + d, v + d + h
    return {'DOWN': (u1, v, u2, v1), 'UP': (u2, v, u22, v1), 'WEST': (u, v1, u1, v2),
            'NORTH': (u1, v1, u2, v2), 'EAST': (u2, v1, u3, v2), 'SOUTH': (u3, v1, u4, v2)}


def czytaj_arkusz(tekstura):
    """(w, h, alpha[x, y]) albo None, gdy pliku nie ma."""
    if not os.path.isfile(tekstura):
        return None
    w, h, kana, piksele = png.czytaj(pathlib.Path(tekstura))
    if kana == 4:
        alfa = [[piksele[y * w + x][3] for x in range(w)] for y in range(h)]
    else:                                   # RGB/G: brak alfa = caly arkusz zamalowany
        alfa = [[255 for _ in range(w)] for _ in range(h)]
    return w, h, alfa


def pokrycie(uu, vv, bw, bh, bd, arkusz, krok=4):
    """(pokryte / wszystkich) punktow probkowych na szesciu twarzach."""
    if arkusz is None:
        return None
    w, h, alfa = arkusz
    traf = allke = 0
    for (fx0, fy0, fx1, fy1) in twarze(uu, vv, int(bw), int(bh), int(bd)).values():
        x0, y0 = int(fx0), int(fy0)
        x1, y1 = max(x0, int(fx1)), max(y0, int(fy1))
        dx = max(1, (x1 - x0) // krok)
        dy = max(1, (y1 - y0) // krok)
        for y in range(y0, y1, dy):
            for x in range(x0, x1, dx):
                allke += 1
                if 0 <= x < w and 0 <= y < h and alfa[y][x] > 24:
                    traf += 1
    return traf / allke if allke else None


def analiza(bryle, arkusz):
    """Raport: ktore twarze danej bryly sa w wiekszosci puste."""
    raport = []
    for nazwa, u, v, bw, bh, bd in bryle:
        puste = []
        if arkusz is not None:
            w, h, alfa = arkusz
            for tw, (x0, y0, x1, y1) in twarze(u, v, bw, bh, bd).items():
                x1, y1 = max(x0, x1), max(y0, y1)
                ile = max(1, int(x1 - x0) * int(y1 - y0))
                zam = sum(1 for y in range(int(y0), int(y1))
                          for x in range(int(x0), int(x1))
                          if 0 <= x < w and 0 <= y < h and alfa[y][x] > 24)
                if zam * 2 < ile:
                    puste.append('%s(%d,%d-%d,%d)' % (tw, x0, y0, x1, y1))
        x1 = int(u + 2 * bd + 2 * bw)
        y1 = int(v + bd + bh)
        raport.append({'nazwa': nazwa, 'uv': (int(u), int(v)),
                       'prostokat': (int(u), int(v), x1, y1),
                       'wymiary': (bw, bh, bd), 'puste': puste})
    return raport


def dopasuj(bryle, arkusz, dozwol_nakladanie=False, roi=None):
    """Sugestie UV: dla kazdej bryly szuka przesuniecia o najwiekszym pokryciu, z kara
    za wchodzenie na pole bryly juz rozlokowanej (marnuje detal grafiki).

    To NIE jest "naprawianie Twojej tekstury" - model zostaje ten sam, zmieniaja
    sie tylko wskazniki w Java, i to tylko wtedy, gdy na to pozwolisz (skrypt
    nic nie zapisuje).
    """
    sugestie = []
    zajete = []
    w, h = arkusz[0], arkusz[1]
    if roi:
        # OCR projektanta: "tu jest rozowe wlosy, tu kopytka" - wyszukiwanie
        # zwraca wtedy wyłącznie pozycje wewn. tego okna, zeby np. grzywa nie
        # wrosla w lawende tuluwia (przy rzadkim arkuszu pelne pokrycie da sie
        # osiagnac prawie wszedzie, a sens graficzny tylko w jednym miejscu)
        rx0, ry0, rx1, ry1 = roi
        w, h = rx1, ry1
    for nazwa, u, v, bw, bh, bd in bryle:
        fw, fh = int(2 * bd + 2 * bw), int(bd + bh)
        if fw > w or fh > h:
            sugestie.append((nazwa, u, v, None, 0.0, 'bryla wieksza niz arkusz'))
            continue
        najlepszy = None
        for vv in range((roi[1] if roi else 0), h - fh + 1):
            for uu in range((roi[0] if roi else 0), w - fw + 1):
                pok = pokrycie(uu, vv, bw, bh, bd, arkusz)
                if pok is None:
                    continue
                # przy rzadkim arkuszu (kilka pelnych prostokatow na cala siatke)
                # nie da sie uniknac nakladania - dwie bryle moga brac te same
                # piksele, to nie blad, tylko oszczednosc. --nakladaj to wlacza.
                kolizja = 0 if dozwol_nakladanie else sum(
                    1 for (zx0, zy0, zx1, zy1) in zajete
                    if uu < zx1 and uu + fw > zx0 and vv < zy1 and vv + fh > zy0)
                ocena = (pok, -0.25 * kolizja, -(uu + vv) / 1000.0)
                if najlepszy is None or ocena > najlepszy[0]:
                    najlepszy = (ocena, uu, vv, pok)
        if najlepszy is None:
            sugestie.append((nazwa, u, v, None, 0.0, 'brak punktow do oceny'))
            continue
        _, uu, vv, pok = najlepszy
        zajete.append((uu, vv, uu + fw, vv + fh))
        sugestie.append((nazwa, u, v, (uu, vv), pok,
                         'zostaje' if (uu, vv) == (int(u), int(v)) else 'PRZENIES na'))
    print('\nszukanie siatki pod arkusz %dx%d (pokrycie 100%% = zero dziur):' % (w, h))
    for nazwa, u, v, dokad, pok, komentarz in sugestie:
        if dokad is None:
            print('  %-16s (%2d,%2d)  %s' % (nazwa, u, v, komentarz))
        else:
            print('  %-16s (%2d,%2d)  pokrycie %5.1f%%  %s (%d,%d)'
                  % (nazwa, u, v, pok * 100, komentarz, dokad[0], dokad[1]))
    return sugestie


def podglad(bryle, tekstura, arkusz, wyjscie, skala):
    """Twoja tekstura x SKALA + obwiednie bryl z numerem. Plik wejsciowy nietkniety."""
    if arkusz is not None:
        w, h, alfa = arkusz
        _, _, _, piksele = png.czytaj(pathlib.Path(tekstura))
    else:                                   # brak arkusza = szachownica, tylko geometria
        w = h = 64
        piksele = [((200, 200, 200, 255) if ((x // 4 + y // 4) % 2) else (160, 160, 160, 255))
                   for y in range(h) for x in range(w)]
    W, H = w * skala, h * skala
    obraz = [(0, 0, 0, 0)] * (W * H)
    # przezroczystosc rysujemy szachownica, nie czernia: w pliku RGBA piksel
    # "pusty" to (0,0,0,0), a na podgladzie musi byc widac, ze to NIE kolor
    for y in range(H):
        for x in range(W):
            p = piksele[(y // skala) * w + (x // skala)]
            if len(p) >= 4 and p[3] <= 24:
                kratek = ((x // skala) // 2 + (y // skala) // 2) % 2
                o = 232 if kratek else 200
                obraz[y * W + x] = (o, o, o, 255)
            else:
                obraz[y * W + x] = (p[0], p[1], p[2], 255)

    def punkt(x, y, rgb):
        if 0 <= x < W and 0 <= y < H:
            obraz[y * W + x] = (rgb[0], rgb[1], rgb[2], 255)

    def kreska(x0, y0, x1, y1, rgb, grub):
        for t in range(grub):
            for x in range(int(x0) * skala - t, int(x1) * skala + t + 1):
                punkt(x, int(y0) * skala - t, rgb)
                punkt(x, int(y1) * skala + t, rgb)
            for y in range(int(y0) * skala - t, int(y1) * skala + t + 1):
                punkt(int(x0) * skala - t, y, rgb)
                punkt(int(x1) * skala + t, y, rgb)

    for i, (nazwa, u, v, bw, bh, bd) in enumerate(bryle):
        u, v, bw, bh, bd = int(u), int(v), int(bw), int(bh), int(bd)
        kolor = PALETA[i % len(PALETA)]
        prost = twarze(u, v, bw, bh, bd)
        kreska(u, v, u + 2 * bd + 2 * bw, v + bd + bh, kolor, 2)
        for tw in ('DOWN', 'UP'):                       # gorny pas: wierzch i spod
            x0, y0, x1, y1 = prost[tw]
            kreska(x0, y0, x1, y1, kolor, 1)
        # numer bryly - nad jej prostokatem, zeby nie zakrywal pikseli
        for nr, znak in enumerate(str(i + 1)):
            for dy, wiersz in enumerate(CYFRY[znak]):
                for dx, kropka in enumerate(wiersz):
                    if kropka != '#':
                        continue
                    for ox in range(max(1, skala // 3)):
                        for oy in range(max(1, skala // 3)):
                            punkt(u * skala + 2 + (nr * 4 + dx) * (skala // 3) + ox,
                                  max(0, v - 3) * skala + dy * (skala // 3) + oy, kolor)
    pathlib.Path(wyjscie).parent.mkdir(parents=True, exist_ok=True)
    png.zapisz(pathlib.Path(wyjscie), W, H, 4, obraz)
    return W, H


def main(argv=None):
    parser = argparse.ArgumentParser(description='siatka UV UnicornModel vs arkusz tekstury')
    parser.add_argument('--model', default=MODEL)
    parser.add_argument('--tekstura', default=TEKSTURA)
    parser.add_argument('--png', metavar='WYJSCIE',
                        help='zapisz podglad (np. docs/unicorn_uv.png)')
    parser.add_argument('--skala', type=int, default=8)
    parser.add_argument('--dopasuj', action='store_true',
                        help='zasugeruj UV, ktore nie wchodza w przezroczystosc')
    parser.add_argument('--nakladaj', action='store_true',
                        help='w --dopasuj: zezwol, by dwie bryle braly te same piksele')
    parser.add_argument('--roi', metavar='X0,Y0-X1,Y1',
                        help='w --dopasuj: przeszukaj tylko to okno arkusza')
    parser.add_argument('--json', action='store_true',
                        help='wyjscie maszynowe dla tools/check_all.py i nic wiecej')
    args = parser.parse_args(argv)

    bryle, arkusz_dekl = bryle_z_javy(args.model)
    arkusz = czytaj_arkusz(args.tekstura)
    if args.json:
        # jedno slowo, jeden fakt: bramka nie ma znać formatu raportu
        import json as _json
        r = analiza(bryle, arkusz)
        print(_json.dumps({
            'boxes': len(r),
            'holey': sum(1 for b in r if b['puste']),
            'holes': [(b['nazwa'], b['puste']) for b in r if b['puste']],
            'arkusz': list(arkusz[:2]) if arkusz else None,
            'deklarowany': list(arkusz_dekl),
        }, ensure_ascii=False))
        return 0
    print('model: %s' % os.path.relpath(args.model, MOD))
    print('arkusz deklarowany: %dx%d, plik: %s' % (
        arkusz_dekl[0], arkusz_dekl[1],
        ('%dx%d' % (arkusz[0], arkusz[1])) if arkusz else 'BRAK'))
    if arkusz and (arkusz_dekl[0] != arkusz[0] or arkusz_dekl[1] != arkusz[1]):
        print('  UWAGA: LayerDefinition mowi %dx%d, a plik ma %dx%d - MC przeskaluje '
              'UV i ksztalt sie rozjedzie' % (arkusz_dekl[0], arkusz_dekl[1],
                                              arkusz[0], arkusz[1]))
    raport = analiza(bryle, arkusz)
    print('\n  %-15s %-9s %-12s %s' % ('czesc', 'UV', 'pole', 'twarze bez pikseli'))
    kandydaci = 0
    for b in raport:
        if b['puste']:
            kandydaci += 1
        x0, y0, x1, y1 = b['prostokat']
        print('  %-15s (%2d,%2d)  x%d..%d y%d..%d  %s' % (
            b['nazwa'], b['uv'][0], b['uv'][1], x0, x1, y0, y1,
            ', '.join(b['puste']) or '-'))
        if b['puste']:
            print('  %17s %s' % ('', '  ^ potrzebujesz pikseli w: '
                                 + ', '.join(b['puste'])))
    if arkusz is None:
        print('\n  (brak arkusza %s - raport tylko o wspolrzednych)' % args.tekstura)
    elif kandydaci:
        print('\n  %d z %d bryl ma twarze w przezroczystosci - w grze beda dziurawe.'
              % (kandydaci, len(raport)))
        print('  Nie poprawiam arkusza: domaluj wskazane prostokaty, albo przenies UV')
        print('  w UnicornModel.java (tryb --dopasuj podaje gotowe liczby).')
    else:
        print('\n  wszystkie twarze trafia w zamalowane piksele')
    if args.dopasuj:
        if arkusz is None:
            print('\n  --dopasuj: brak arkusza, nie ma czego szukac')
        else:
            roi = None
            if args.roi:
                m = re.match(r'(\d+),(\d+)-(\d+),(\d+)$', args.roi)
                if not m:
                    raise SystemExit('--roi chce formatu X0,Y0-X1,Y1')
                roi = tuple(int(g) for g in m.groups())
            dopasuj(bryle, arkusz, args.nakladaj, roi)
    if args.png:
        W, H = podglad(bryle, args.tekstura, arkusz, args.png, args.skala)
        print('podglad: %s (%dx%d px, powiekszenie x%d, bryl: %d)'
              % (os.path.relpath(args.png, MOD), W, H, args.skala, len(bryle)))
    return 0


if __name__ == '__main__':
    sys.exit(main())
