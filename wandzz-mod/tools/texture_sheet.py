#!/usr/bin/env python3
"""Arkusz "jak te grafiki widzi gra" (docs/textures.png).

Powod, dla ktorego to istnieje: 16x16 w Aseprite to dwie plamki na monitorze, a
po imporcie potrafi byc np. o piksel za cienkie, z tlem zamiast alfY albo w
ksztalcie, ktory na jasnym tle ekwipunku znika. Ten skrypt sklada wszystkie PNG
z `assets/wandzz/textures/item/` na jednej kartce: kazdy tekstura dwa razy,
powiekszona nearest-neighbor - najpierw na szachownicy (widac przezroczystosc),
potem na kamieniu (widac, czy ksztaft nie ginie na jasnym tle).

    python3 wandzz-mod/tools/texture_sheet.py [sciezka-wyjscia]

Kolejnosc to alfabetyczna kolejnosc plikow w katalogu - te nazwy skrypt wypisuje
na standardowe wyjscie, linia po linii, wiec komorek nie trzeba podpisywac.
Rysowanie czystym Pythonem (PNG zapisany recznie), zero zaleznosci.
"""
import pathlib
import struct
import sys
import zlib

REPO = pathlib.Path(__file__).resolve().parents[1]
TEX = REPO / 'src/main/resources/assets/wandzz/textures/item'
KOLUMNY = 5
POWIEKSZ = 8
ODSTEP = 10
SIATKA_SZACH = 8
SZACH_A = (204, 204, 208)
SZACH_B = (152, 152, 158)
KAMIEN = (122, 122, 126)
RAMA = (60, 60, 66)


def czytaj_png(sciezka):
    """(w, h, [(r,g,b,a), ...]) wierszami od gory; RGBA, 8 bitow, bez interlace'u."""
    d = sciezka.read_bytes()
    if d[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError('to nie PNG')
    i, idat, hdr, paleta, trns = 8, b'', None, None, None
    while i + 8 <= len(d):
        ln = struct.unpack('>I', d[i:i + 4])[0]
        typ = d[i + 4:i + 8]
        dane = d[i + 8:i + 8 + ln]
        if typ == b'IHDR':
            hdr = struct.unpack('>IIBBBBB', dane)
        elif typ == b'IDAT':
            idat += dane
        elif typ == b'PLTE':
            paleta = [tuple(dane[3 * k:3 * k + 3]) for k in range(len(dane) // 3)]
        elif typ == b'tRNS':
            trns = dane
        i += 12 + ln
    w, h, glebia, typ_k, _komp, _flt, interlace = hdr
    if glebia != 8:
        raise ValueError('glebia %d bitow - arkusz liczy na 8' % glebia)
    if interlace:
        raise ValueError('interlace Adam7 - najpierw wyeksportuj bez niego')
    kana = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[typ_k]
    surowe = zlib.decompress(idat)
    krok = 1 + w * kana
    if paleta is not None and trns is not None:
        paleta = [p + (trns[k] if k < len(trns) else 255) for k, p in enumerate(paleta)]
    piksele = []
    for y in range(h):
        wiersz = surowe[y * krok + 1:(y + 1) * krok]
        for x in range(w):
            if typ_k == 3:
                idx = wiersz[x]
                if paleta and idx < len(paleta):
                    piksele.append(paleta[idx] if len(paleta[idx]) == 4 else paleta[idx] + (255,))
                else:
                    piksele.append((255, 0, 255, 255))
            else:
                px = wiersz[x * kana:(x + 1) * kana]
                if kana == 1:
                    piksele.append((px[0], px[0], px[0], 255))
                elif kana == 2:
                    piksele.append((px[0], px[0], px[0], px[1]))
                elif kana == 3:
                    piksele.append((px[0], px[1], px[2], 255))
                else:
                    piksele.append((px[0], px[1], px[2], px[3]))
    return w, h, piksele


class Pplotno:
    def __init__(self, w, h, tlo=(245, 245, 245)):
        self.w, self.h = w, h
        self.px = [[tlo for _ in range(w)] for _ in range(h)]

    def punkt(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = c

    def prostokat(self, x, y, w, h, c):
        for j in range(max(0, y), min(self.h, y + h)):
            for i in range(max(0, x), min(self.w, x + w)):
                self.px[j][i] = c

    def png(self, sciezka):
        surowe = bytearray()
        for y in range(self.h):
            surowe.append(0)
            for x in range(self.w):
                surowe += bytes(self.px[y][x][:3])

        def blok(typ, dane):
            return (struct.pack('>I', len(dane)) + typ + dane
                    + struct.pack('>I', zlib.crc32(typ + dane) & 0xFFFFFFFF))
        sciezka.parent.mkdir(parents=True, exist_ok=True)
        with open(sciezka, 'wb') as f:
            f.write(b'\x89PNG\r\n\x1a\n')
            f.write(blok(b'IHDR', struct.pack('>IIBBBBB', self.w, self.h, 8, 2, 0, 0, 0)))
            f.write(blok(b'IDAT', zlib.compress(bytes(surowe), 9)))
            f.write(blok(b'IEND', b''))


def narysuj(pl, tekstura, x0, y0, tlo_fn):
    """Jedna tekstura, powiekszona, na wskazanym tle (x0,y0 = lewy gorny rog w pikselach arkusza)."""
    w, h, piksele = tekstura
    for y in range(h):
        for x in range(w):
            r, g, b, a = piksele[y * w + x]
            kolor = (r, g, b) if a > 127 else tlo_fn(x0 + x * POWIEKSZ, y0 + y * POWIEKSZ)
            for j in range(POWIEKSZ):
                for i in range(POWIEKSZ):
                    pl.punkt(x0 + x * POWIEKSZ + i, y0 + y * POWIEKSZ + j, kolor)


def zapisz(docel):
    docel = pathlib.Path(docel)
    pliki = sorted(TEX.glob('*.png'))
    if not pliki:
        raise SystemExit('pusty katalog tekstur: %s' % TEX)
    bok = 16 * POWIEKSZ
    szer_komorki = 2 * bok + ODSTEP            # szachownica + kamien
    kolumny = min(KOLUMNY, len(pliki))
    wiersze = (len(pliki) + kolumny - 1) // kolumny
    pl = Pplotno(kolumny * (szer_komorki + ODSTEP) + ODSTEP,
                 wiersze * (bok + 2 * ODSTEP) + ODSTEP)
    pominiete = []
    for idx, plik in enumerate(pliki):
        try:
            tekstura = czytaj_png(plik)
        except Exception as e:                   # noqa: BLE001 - raport, nie crash
            pominiete.append('%s: %s' % (plik.name, e))
            continue
        x = ODSTEP + (idx % kolumny) * (szer_komorki + ODSTEP)
        y = ODSTEP + (idx // kolumny) * (bok + 2 * ODSTEP)
        for k in range(2):
            pl.prostokat(x + k * (bok + ODSTEP) - 2, y - 2, bok + 4, bok + 4, RAMA)
        narysuj(pl, tekstura, x, y, lambda i, j: SZACH_A if (i // SIATKA_SZACH + j // SIATKA_SZACH) % 2 == 0 else SZACH_B)
        narysuj(pl, tekstura, x + bok + ODSTEP, y, lambda i, j: KAMIEN)
    pl.png(docel)
    print('%s: %d tekstur x2 (szachownica, kamien), %dx%d px'
          % (docel, len(pliki) - len(pominiete), pl.w, pl.h))
    print('kolejnosc komorek (wierszami):')
    for i in range(0, len(pliki), kolumny):
        print('  ' + '  |  '.join(p.stem for p in pliki[i:i + kolumny]))
    for b in pominiete:
        print('  POMINELIEM  %s' % b)
    return 1 if pominiete else 0


if __name__ == '__main__':
    sys.exit(zapisz(sys.argv[1] if len(sys.argv) > 1 else str(REPO / 'docs/textures.png')))
