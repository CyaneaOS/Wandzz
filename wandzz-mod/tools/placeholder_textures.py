#!/usr/bin/env python3
"""Generuje PLACEHOLDERY tekstur przedmiotow, ktorych jeszcze nie ma na dysku.

Model w `models/item/<nazwa>.json` wskazuje na `wandzz:item/<nazwa>`. Jesli
pliku `textures/item/<nazwa>.png` nie ma, gra renderuje fioletowa kostke
"missingno". Ten skrypt doklada więc brzydkie, ale czytelne zastepstwo - i
NIGDY nie nadpisuje istniejacego pliku, wiec wlasna grafike kladziesz po prostu
obok i ona wygrywa.

    python3 wandzz-mod/tools/placeholder_textures.py            # uzupelnij braki
    python3 wandzz-mod/tools/placeholder_textures.py --check    # tylko wypisz, czego brak

Ksztalt to ukośny patyk (3 px) z obrys - tyle, zeby w ekwipunku bylo widac, ze
to kawek drewna, a nie broń. Kolor = przyblizony odcień danego drewna.
"""
import json
import pathlib
import struct
import sys
import zlib

REPO = pathlib.Path(__file__).resolve().parents[1]
ASSETS = REPO / 'src/main/resources/assets/wandzz'

# przyblizone odcienie drewna (tylko po to, zeby placeholder nie byl jednolity)
ODCIEN = {
    'oak': (166, 124, 78), 'spruce': (109, 80, 48), 'birch': (199, 186, 158),
    'jungle': (164, 128, 76), 'acacia': (180, 67, 51), 'dark_oak': (74, 52, 30),
    'crimson': (106, 50, 71), 'warped': (49, 150, 151), 'cherry': (227, 199, 190),
    'pale_oak': (217, 212, 201), 'mangrove': (128, 84, 66), 'bamboo': (147, 179, 82),
    'arcane': (96, 128, 190),
}
ROZMIAR = 16


def png16(sciezka, rgb):
    """Zapisuje 16x16 RGBA - recznie, bez zadnych bibliotek."""
    ciemne = tuple(max(0, int(c * 0.55)) for c in rgb)
    jasne = tuple(min(255, int(c + (255 - c) * 0.35)) for c in rgb)
    px = [(0, 0, 0, 0)] * (ROZMIAR * ROZMIAR)
    px = [list(p) for p in px]
    for x in range(ROZMIAR):
        for y in range(ROZMIAR):
            # patyk: przekatna z (3,13) do (13,3), czyli x + y = 16
            dist = abs(x + y - 16) / 2 ** 0.5
            w_zasiegu = 2.5 <= x <= 13.5 and 2.5 <= y <= 13.5
            if not w_zasiegu:
                continue
            if dist <= 1.5:
                px[y * ROZMIAR + x] = list(rgb) + [255]
            elif dist <= 2.7:
                px[y * ROZMIAR + x] = list(jasne if y < x else ciemne) + [255]
    surowe = bytearray()
    for y in range(ROZMIAR):
        surowe.append(0)
        for x in range(ROZMIAR):
            surowe += bytes(px[y * ROZMIAR + x])

    def blok(typ, dane):
        return (struct.pack('>I', len(dane)) + typ + dane
                + struct.pack('>I', zlib.crc32(typ + dane) & 0xFFFFFFFF))
    sciezka.parent.mkdir(parents=True, exist_ok=True)
    with open(sciezka, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n')
        f.write(blok(b'IHDR', struct.pack('>IIBBBBB', ROZMIAR, ROZMIAR, 8, 6, 0, 0, 0)))
        f.write(blok(b'IDAT', zlib.compress(bytes(surowe), 9)))
        f.write(blok(b'IEND', b''))


def tekstury_wskazane_przez_modele():
    """[(sciezka_na_dysku, id_tekstury, model)] dla kazdego `wandzz:*` w modelach."""
    braki = []
    for model in sorted(ASSETS.glob('models/**/*.json')):
        try:
            m = json.loads(model.read_text(encoding='utf8'))
        except Exception as e:
            raise SystemExit('zły JSON w %s: %s' % (model, e))
        for warstwa, id_ in (m.get('textures') or {}).items():
            if not isinstance(id_, str) or id_.startswith('minecraft:'):
                continue
            if ':' in id_:
                ns, path = id_.split(':', 1)
                if ns != 'wandzz':
                    continue
            else:
                path = id_
            docel = ASSETS / 'textures' / (path + '.png')
            if not docel.exists():
                braki.append((docel, id_, model))
    return braki


def tekstury_wskazane_w_javie():
    """`textures/entity/coś.png` wystepujace w kodzie - te wskazują renderery mobów."""
    import re
    braki = []
    for java in sorted((REPO / 'src').rglob('*.java')):
        for sciezka in re.findall(r'"(textures/[a-z0-9_/]+)\.png"', java.read_text(encoding='utf8')):
            docel = ASSETS / (sciezka + '.png')
            if not docel.exists():
                braki.append((docel, 'wandzz:' + sciezka, java))
    return braki


WYMAGANY = {'item': 16, 'block': 16, 'entity': 32}    # rozmiar wedlug katalogu


def waliduj():
    """Przechodzi po wszystkich PNG i sprawdza to, co naprawde psuje render."""
    bledy = []
    for plik in sorted((ASSETS / 'textures').rglob('*.png')):
        d = plik.read_bytes()
        rel = plik.relative_to(ASSETS / 'textures')
        katalog = rel.parts[0] if rel.parts else 'item'
        try:
            if d[:8] != b'\x89PNG\r\n\x1a\n':
                raise ValueError('to nie PNG (zla sygnatura)')
            i, idat, hdr = 8, b'', None
            while i + 8 <= len(d):
                if i + 12 > len(d):
                    raise ValueError('plik urwany (brak naglowka chunka przy bajcie %d)' % i)
                ln = struct.unpack('>I', d[i:i + 4])[0]
                if ln < 0 or i + 12 + ln > len(d):
                    raise ValueError('plik urwany: chunk przy bajcie %d deklaruje %d B, a zostało %d'
                                      % (i, ln, max(0, len(d) - i - 12)))
                typ = d[i + 4:i + 8]
                dane = d[i + 8:i + 8 + ln]
                crc = struct.unpack('>I', d[i + 8 + ln:i + 12 + ln])[0]
                if crc != (zlib.crc32(typ + dane) & 0xFFFFFFFF):
                    raise ValueError('CRC chunku %s nie zgadza sie (plik uciety?)' % typ.decode())
                if typ == b'IHDR':
                    hdr = struct.unpack('>IIBBBBB', dane)
                elif typ == b'IDAT':
                    idat += dane
                i += 12 + ln
            if hdr is None:
                raise ValueError('brak IHDR')
            w, h, glebia, typ_k, _c, _f, interlace = hdr
            if (w, h) != (WYMAGANY.get(katalog, 16),) * 2:
                raise ValueError('%dx%d, a dla %s/ oczekiwane %dx%d'
                                 % (w, h, katalog, WYMAGANY.get(katalog, 16), WYMAGANY.get(katalog, 16)))
            if glebia != 8:
                raise ValueError('glebia %d bitow, musi byc 8' % glebia)
            if interlace:
                raise ValueError('interlace (Adam7) - Minecraft tego nie czyta')
            if typ_k not in (2, 3, 4, 6):
                raise ValueError('typ koloru %d (ma byc 2=RGB, 3=paleta, 4=szary+A, 6=RGBA)' % typ_k)
            if typ_k == 3:
                raise ValueError('paleta: dziala, ale Minecraft gubi przezroczystosc tufu - zapisz RGBA')
            surowe = zlib.decompress(idat)
            if len(surowe) != ((w * (3 if typ_k == 2 else 4)) + 1) * h:
                raise ValueError('rozmiar danych IDAT nie pasuje do %dx%d' % (w, h))
        except Exception as e:                       # noqa: BLE001 - raport, nie crash
            bledy.append('%s: %s' % (rel, e))
    for b in bledy:
        print('  BLAD  %s' % b)
    if not bledy:
        print('walidacja: wszystkie PNG w porzadku (%d plikow)'
              % len(list((ASSETS / 'textures').rglob('*.png'))))
    return 1 if bledy else 0


def main():
    if '--validate' in sys.argv:
        return waliduj()
    braki = tekstury_wskazane_przez_modele() + tekstury_wskazane_w_javie()
    if '--check' in sys.argv:
        if not braki:
            print('kompletne: kazda tekstura wskazana przez model istnieje')
            return 0
        print('brakuje %d tekstur:' % len(braki))
        for docel, id_, model in braki:
            print('  %-40s (%s)' % (id_, model.relative_to(REPO)))
        return 1
    zrobione = 0
    for docel, id_, model in braki:
        if docel.exists():
            continue                      # nigdy nie nadpisujemy cudzej grafiki
        drewno = docel.stem.split('_')[0]
        png16(docel, ODCIEN.get(drewno, (150, 150, 150)))
        zrobione += 1
        print('placeholder: %s' % docel.relative_to(REPO))
    print('nowych plikow: %d (istniejacych nie dotknalem)' % zrobione)
    if not zrobione:
        print('nic do uzupelnienia - wszystkie tekstury sa na dysku')
    return 0


if __name__ == '__main__':
    sys.exit(main())
