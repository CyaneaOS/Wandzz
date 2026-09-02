#!/usr/bin/env python3
"""Arkusz "co rysowac" (docs/gestures.png) rysowany PROSTO z GestureTemplates.java.

Nie ma tu zadnych wspolrzednych przepisanych recznie: ksztalty sa wyciagane z
tego samego pliku Java, ktory czyta gra (te same generatory kola i fali co
`gesture_eval.py --sync`), wiec obrazek nie moze sie rozjechac z kodem.
Renderowanie czystym Pythonem - bez PIL i matplotlib, PNG zapisany recznie.

    python3 wandzz-mod/tools/gesture_sheet.py [sciezka-wyjscia]

Rysunek jest w konwencji ekranu (y w dol) - patrzy sie na niego tak samo, jak
rysuje mysza. Kropka czerwona = gdzie zaczac, niebieska = gdzie zakonczyc. Numery komorek
odpowiadaja tabeli "Co rysowac (10 czarow)" w README.
"""
import pathlib
import re
import struct
import sys
import zlib

sys.path.insert(0, __file__.rsplit('/', 1)[0])
import gesture_eval as E   # pylint: disable=wrong-import-order

REPO = pathlib.Path(__file__).resolve().parents[1]

KSZTALTY = ['strike', 'break_block', 'torch', 'leap', 'heal',
            'fireball', 'dragon_breath', 'open_gate', 'teleport', 'bomb',
            'reveal', 'invisible']

# font 3x5 wystarczy do cyfr 1-12
CYFRY = {
    '0': '###|#.#|#.#|#.#|###', '1': '.#.|##.|.#.|.#.|.##', '2': '###|..#|###|#..|###',
    '3': '###|..#|.##|..#|###', '4': '#.#|#.#|###|..#|..#', '5': '###|#..|###|..#|###',
    '6': '###|#..|###|#.#|###', '7': '###|..#|..#|..#|..#', '8': '###|#.#|###|#.#|###',
    '9': '###|#.#|###|..#|..#',
}
METODA = {
    'strike': 'strikeStroke', 'break_block': 'breakBlockStroke', 'torch': 'torchStroke',
    'leap': 'leapStroke', 'heal': 'healStroke', 'fireball': 'flameTriangle',
    'dragon_breath': 'breathWave', 'open_gate': 'barredGateStroke',
    'teleport': 'twinSquares', 'bomb': 'diamondTick',
    'reveal': 'revealStroke', 'invisible': 'invisibilityStroke',
}


def ksztalty_z_javy():
    """Wylicza wspolrzedne prosto z `GestureTemplates.java`."""
    src = (REPO / 'src/main/java/com/wandzz/gesture/GestureTemplates.java').read_text(encoding='utf8')
    out = {}
    for slot in KSZTALTY:
        m = re.search(r'List<Point> ' + METODA[slot] + r'\(\)\s*\{(.*?)\n    \}', src, re.S)
        if m is None:
            raise SystemExit('w Java nie ma GestureTemplates.' + METODA[slot] + '()')
        body = m.group(1)
        if 'circle(' in body and 'for (' not in body:
            cnt = int(re.search(r'circle\((\d+)\)', body).group(1))
            pts = E.circle(cnt)
        elif 'for (' in body:
            if 'Math.cos' in body:
                cnt = int(re.search(r'i < (\d+)', body).group(1))
                pts = E.circle(cnt)
            else:
                n = int(re.search(r'i <= (\d+)', body).group(1))
                amp = float(re.search(r'(-?[\d.]+) \* Math.sin', body).group(1))
                arms = float(re.search(r'([\d.]+) \* Math.PI', body).group(1))
                pts = E.sine(arms, amp, n)
        else:
            pts = [(float(a), float(b))
                   for a, b in re.findall(r'new Point\(\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*\)', body)]
        out[slot] = pts
    return out


class Pplotno:
    """Biala plansza RGB z kreskami, kolami i cyframi - wiecej tu nie trzeba."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = bytearray(w * h * 3)
        for i in range(w * h):
            self.px[i * 3:i * 3 + 3] = b'\xff\xff\xff'

    def punkt(self, x, y, c):
        x, y = int(x), int(y)
        if 0 <= x < self.w and 0 <= y < self.h:
            i = (y * self.w + x) * 3
            self.px[i:i + 3] = bytes(c)

    def kreska(self, a, b, c, grubosc=3):
        x0, y0 = a
        x1, y1 = b
        n = int(max(abs(x1 - x0), abs(y1 - y0))) * 2 + 1
        for i in range(n + 1):
            x = x0 + (x1 - x0) * i / n
            y = y0 + (y1 - y0) * i / n
            for dx in range(-(grubosc // 2), grubosc // 2 + 1):
                for dy in range(-(grubosc // 2), grubosc // 2 + 1):
                    self.punkt(int(round(x)) + dx, int(round(y)) + dy, c)

    def kolo(self, s, r, c):
        for dx in range(-r, r + 1):
            for dy in range(-r, r + 1):
                if dx * dx + dy * dy <= r * r:
                    self.punkt(round(s[0]) + dx, round(s[1]) + dy, c)

    def cyfra(self, x, y, tekst, c, powieksz=2):
        for k, zn in enumerate(tekst):
            for j, wiersz in enumerate(CYFRY[zn].split('|')):
                for i, pil in enumerate(wiersz):
                    if pil == '#':
                        for a in range(powieksz):
                            for b in range(powieksz):
                                self.punkt(x + (k * 4 + i) * powieksz + a, y + j * powieksz + b, c)

    def png(self, sciezka):
        surowe = bytearray()
        for y in range(self.h):
            surowe.append(0)
            surowe += self.px[y * self.w * 3:(y + 1) * self.w * 3]

        def blok(typ, dane):
            return (struct.pack('>I', len(dane)) + typ + dane
                    + struct.pack('>I', zlib.crc32(typ + dane) & 0xFFFFFFFF))
        with open(sciezka, 'wb') as f:
            f.write(b'\x89PNG\r\n\x1a\n')
            f.write(blok(b'IHDR', struct.pack('>IIBBBBB', self.w, self.h, 8, 2, 0, 0, 0)))
            f.write(blok(b'IDAT', zlib.compress(bytes(surowe), 9)))
            f.write(blok(b'IEND', b''))


def zapisz(sciezka):
    ksztalty = ksztalty_z_javy()
    kolumny = 5
    wiersze = (len(KSZTALTY) + kolumny - 1) // kolumny
    komorka = 190
    pad = 18
    pl = Pplotno(kolumny * komorka + pad, wiersze * (komorka + 16) + pad)
    for idx, slot in enumerate(KSZTALTY):
        pkt = ksztalty[slot]
        x0 = min(p[0] for p in pkt)
        x1 = max(p[0] for p in pkt)
        y0 = min(p[1] for p in pkt)
        y1 = max(p[1] for p in pkt)
        skala = (komorka - 76) / max(x1 - x0, y1 - y0, 1e-9)
        cx = pad / 2 + (idx % kolumny) * komorka + komorka / 2
        cy = pad / 2 + (idx // kolumny) * (komorka + 16) + komorka / 2 + 10
        # KONWENCJA EKRANU: +y to DOL, tak jak mysz w CastingScreen (mouseY rosnie
        # w dol) i tak jak szablon czyta gra. Obrazek ma wiec wygladac dokladnie
        # jak to, co gracz rysuje na ekranie - bez lustra.
        ekran = [(cx + (p[0] - (x0 + x1) / 2) * skala,
                  cy + (p[1] - (y0 + y1) / 2) * skala) for p in pkt]
        for i in range(len(ekran) - 1):
            pl.kreska(ekran[i], ekran[i + 1], (20, 20, 20))
        pl.kolo(ekran[0], 6, (176, 44, 32))
        pl.kolo(ekran[-1], 6, (32, 96, 176))
        pl.cyfra(int(cx - komorka / 2 + 8), int(cy - komorka / 2 + 2), str(idx + 1), (90, 90, 90))
        # linia pod komorka, zeby 10 ksztaltow nie zlelo sie w jedno
        pl.kreska((int(cx - komorka / 2 + 6), int(cy + komorka / 2 - 12)),
                  (int(cx + komorka / 2 - 6), int(cy + komorka / 2 - 12)), (224, 224, 224), 1)
    pl.png(sciezka)
    return pl.w, pl.h


if __name__ == '__main__':
    docel = sys.argv[1] if len(sys.argv) > 1 else str(REPO / 'docs/gestures.png')
    w, h = zapisz(docel)
    print('%s: %d ksztaltow, %dx%d px (wygenerowane z GestureTemplates.java)' % (docel, len(KSZTALTY), w, h))
