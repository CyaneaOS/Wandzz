#!/usr/bin/env python3
"""Generuje PLACEHOLDERY tekstur przedmiotow, ktorych jeszcze nie ma na dysku.

Model w `models/item/<nazwa>.json` wskazuje na `wandzz:item/<nazwa>`. Jesli
pliku `textures/item/<nazwa>.png` nie ma, gra renderuje fioletowa kostke
"missingno". Ten skrypt doklada wiec brzydkie, ale czytelne zastepstwo - i
NIGDY nie nadpisuje istniejacego pliku, wiec wlasna grafike kladziesz po prostu
obok i ona wygrywa.

    python3 wandzz-mod/tools/placeholder_textures.py            # uzupelnij braki
    python3 wandzz-mod/tools/placeholder_textures.py --check    # tylko wypisz, czego brak

Ksztalt to ukosny patyk (3 px) z obrys - tyle, zeby w ekwipunku bylo widac, ze
to kawek drewna, a nie bron. Kolor = przyblizony odcien danego drewna.
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
    # te dwa po pelnej nazwie, bo dziel przedrostek, a musza sie roznic:
    # to dokladnie ten blad, przez ktory "swiety patyk" wygladal jak zwyczajny
    'arcane_stick': (96, 128, 190), 'arcane_blessed_stick': (212, 176, 74),
    'arcane_resin': (198, 140, 40),
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
            raise SystemExit('zly JSON w %s: %s' % (model, e))
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
    """`textures/entity/cos.png` wystepujace w kodzie - te wskazuja renderery mobow."""
    import re
    braki = []
    for java in sorted((REPO / 'src').rglob('*.java')):
        for sciezka in re.findall(r'"(textures/[a-z0-9_/]+)\.png"', java.read_text(encoding='utf8')):
            docel = ASSETS / (sciezka + '.png')
            if not docel.exists():
                braki.append((docel, 'wandzz:' + sciezka, java))
    return braki


WYMAGANY = {'item': 16, 'block': 16, 'entity': 32}    # rozmiar, ktorego sie trzymamy
OK_ROZMIARY = (8, 16, 32, 64, 128, 256)               # kwadraty, ktore MC lyka bez marudzenia
# tekstury, ktore BEZ alfY beda mialy lity prostokat zamiast wycietego ksztaltu
WYMAGA_ALFY = ('leaves', 'sapling', 'flower', 'crop', 'hair', 'feather')


def waliduj():
    """Przechodzi po wszystkich PNG: bledy = nie wczyta sie; ostrzezenia = bedzie brzydko."""
    bledy, ostrzezenia = [], []
    for plik in sorted((ASSETS / 'textures').rglob('*.png')):
        d = plik.read_bytes()
        rel = str(plik.relative_to(ASSETS / 'textures'))
        katalog = rel.split('/')[0] if '/' in rel else 'item'
        try:
            if d[:8] != b'\x89PNG\r\n\x1a\n':
                raise ValueError('to nie PNG (zla sygnatura - moze to SVG albo zle skonczone?)')
            i, idat, hdr = 8, b'', None
            while i + 8 <= len(d):
                if i + 12 > len(d):
                    raise ValueError('plik urwany (brak naglowka chunka przy bajcie %d)' % i)
                ln = struct.unpack('>I', d[i:i + 4])[0]
                typ = d[i + 4:i + 8]
                if ln < 0 or i + 12 + ln > len(d):
                    raise ValueError('plik urwany: chunk przy bajcie %d deklaruje %d B, a zostalo %d'
                                     % (i, ln, max(0, len(d) - i - 12)))
                dane = d[i + 8:i + 8 + ln]
                crc = struct.unpack('>I', d[i + 8 + ln:i + 12 + ln])[0]
                if crc != (zlib.crc32(typ + dane) & 0xFFFFFFFF):
                    raise ValueError('CRC chunku %s sie nie zgadza (edytowany poza edytorem?)' % typ.decode())
                if typ == b'IHDR':
                    hdr = struct.unpack('>IIBBBBB', dane)
                elif typ == b'IDAT':
                    idat += dane
                i += 12 + ln
            if hdr is None:
                raise ValueError('brak IHDR')
            w, h, glebia, typ_k, _c, _f, interlace = hdr
            if glebia != 8:
                raise ValueError('glebia %d bitow na kanale - SpriteLoader oczekuje 8' % glebia)
            if interlace:
                raise ValueError('interlace (Adam7) - Minecraft nie dekoduje warstwowo, bedzie kostka')
            if typ_k not in (2, 3, 4, 6):
                raise ValueError('typ koloru %d (dozwolone 2=RGB, 3=paleta, 4=szary+A, 6=RGBA)' % typ_k)
            if (w, h) != (h, w) or w not in OK_ROZMIARY:
                ostrzezenia.append('%s: %dx%d, oczekuje sie kwadratu bedacego potega dwojku %s' % (rel, w, h, list(OK_ROZMIARY)))
            elif katalog in WYMAGANY and w != WYMAGANY[katalog]:
                ostrzezenia.append('%s: %dx%d (dla %s/ trzymamy sie %d, inaczej UV encji sie rozjedzie)'
                                   % (rel, w, h, katalog, WYMAGANY[katalog]))
            if typ_k == 3:
                ostrzezenia.append('%s: paleta indexed - przezroczystosc bywa gubiona, zapisz RGBA' % rel)
            if typ_k in (2, 4) and any(k in rel for k in WYMAGA_ALFY):
                ostrzezenia.append('%s: brak kanaalu alfa, a to wyciety ksztalt - bedzie lity prostokat' % rel)
            krok = w * (3 if typ_k == 2 else 4)
            if typ_k == 3:
                krok = w
            if typ_k in (4, 6):
                dane = zlib.decompress(idat)
                kan = 4 if typ_k == 6 else 2
                slabe, zera = 0, 0
                for y in range(h):
                    wiersz = dane[y * (w * kan + 1) + 1:(y + 1) * (w * kan + 1)]
                    for x in range(w):
                        a = wiersz[x * kan + kan - 1]
                        if a == 0:
                            zera += 1
                        elif a <= 24:
                            slabe += 1
                if slabe >= 3:
                    ostrzezenia.append('%s: %d pikseli z alfa 1-24 (0-9%%) - to zwykle warstwa '
                                       'o mocy ~0%% w Asepricie, spłaszczona do a=1; w grze jej NIE widać. '
                                       'Napraw: przywróc warstwie 100%% albo odpal --fix-alpha'
                                       % (rel, slabe))
                if zera == w * h:
                    raise ValueError('sprite calkowicie przezroczysty (export z wyłączoną warstwą?)')
            if len(zlib.decompress(idat)) != (krok + 1) * h:
                raise ValueError('rozmiar IDAT nie pasuje do %dx%d (sklejone dwa zapisy?)' % (w, h))
        except Exception as e:                       # noqa: BLE001 - raport, nie crash
            bledy.append('%s: %s' % (rel, e))
    for b in bledy:
        print('  BLAD        %s' % b)
    for o in ostrzezenia:
        print('  OSTRZEZENIE %s' % o)
    ile = len(list((ASSETS / 'textures').rglob('*.png')))
    if not bledy and not ostrzezenia:
        print('walidacja: %d PNG w porzadku (rozmiar, glebia, kanaly, brak interlace)' % ile)
    elif not bledy:
        print('walidacja: %d PNG wczyta sie poprawnie, %d ostrzezen do przeoczenia' % (ile, len(ostrzezenia)))
    else:
        print('walidacja: %d bledow, %d ostrzezen' % (len(bledy), len(ostrzezenia)))
    return 1 if bledy else 0


def napraw_alfa():
    """a=1..24 -> a=255. Nie dotyka pikseli calkowicie przezroczystych (tlo)."""
    naprawione = []
    for plik in sorted((ASSETS / 'textures').rglob('*.png')):
        d = plik.read_bytes()
        i = 8
        czesci, idat, hdr = [], b'', None
        while i + 8 <= len(d):
            ln = struct.unpack('>I', d[i:i + 4])[0]
            typ = d[i + 4:i + 8]
            czesci.append((typ, d[i + 8:i + 8 + ln]))
            if typ == b'IHDR':
                hdr = struct.unpack('>IIBBBBB', d[i + 8:i + 8 + ln])
            elif typ == b'IDAT':
                idat += d[i + 8:i + 8 + ln]
            i += 12 + ln
        w, h, _bd, typ_k, _c, _f, il = hdr
        if typ_k != 6 or il:
            continue                                  # RGBA bez interlace'u, inaczej nie ruszamy
        surowe = zlib.decompress(idat)
        krok = 1 + w * 4
        slabe = 0
        for y in range(h):
            start = y * krok + 1
            for x in range(w):
                k = start + x * 4 + 3
                if 0 < surowe[k] <= 24:
                    surowe = surowe[:k] + b'\xff' + surowe[k + 1:]
                    slabe += 1
        if not slabe:
            continue
        nowe, out = b'', bytearray()
        for typ, dane in czesci:
            if typ == b'IDAT':
                dane = zlib.compress(bytes(surowe), 9)
            out += struct.pack('>I', len(dane)) + typ + dane
            out += struct.pack('>I', zlib.crc32(typ + dane) & 0xFFFFFFFF)
        plik.write_bytes(b'\x89PNG\r\n\x1a\n' + bytes(out))
        naprawione.append('%s: %d pikseli' % (plik.relative_to(REPO), slabe))
    for n in naprawione:
        print('  naprawione  %s' % n)
    if not naprawione:
        print('nie ma nic do naprawy (zadny plik nie ma pikseli z alfa 1-24)')
    return 0


def main():
    if '--fix-alpha' in sys.argv:
        return napraw_alfa()
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
        drewno = docel.stem if docel.stem in ODCIEN else docel.stem.split('_')[0]
        png16(docel, ODCIEN.get(drewno, (150, 150, 150)))
        zrobione += 1
        print('placeholder: %s' % docel.relative_to(REPO))
    print('nowych plikow: %d (istniejacych nie dotknalem)' % zrobione)
    if not zrobione:
        print('nic do uzupelnienia - wszystkie tekstury sa na dysku')
    return 0


if __name__ == '__main__':
    sys.exit(main())
