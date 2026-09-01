"""Czytanie i zapisywanie PNG w czystym Pythonie - bez PIL, bez zaleznosci.

Po co osobny modul: PNG NIE trzyma pikseli "jak leca". Kazdy wiersz strumienia
jest najpierw przeksztalcony jednym z filtrow (0=None, 1=Sub, 2=Up, 3=Average,
4=Paeth), a `zlib.decompress` zdejmuje TYLKO kompresje. Ktos, kto po
dekompresji czyta bajty jako RGBA, dostaje zupelnie inne kolory i alfa - i na
tym zbudowal kiedys "naprawe alfY", ktora popsuia tekstury (patrz historia
narzedzi w tym katalogu). Ten modul odswieza filtry poprawnie.

    from png import czytaj, zapisz
    w, h, kana, piksele = czytaj(ścieżka)     # piksele = [r,g,b,a] albo [r,g,b] ...
    zapisz(sciezka, w, h, kana, piksele)      # wiersze z filtrem 0 (kanonicznie)
"""
import struct
import zlib

__all__ = ['czytaj', 'zapisz', 'dekoduj', 'koduj', 'blok']


def _paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    return b if pb <= pc else c


def dekoduj(surowe, w, h, kana):
    """Odwrotna operacja filtrow -> linie[y] = lista wartosci (w*kana bajtow).

    Filtry (PNG spec, rozdzial 6.3) licza sie wzgledem juz odszyfrowanych
    sasiadow: `a` = ten sam kanal piksel w lewo, `b` = ten sam kanal wiersz
    wyzej, `c` = po skosie w lewo-gore. Powyzej krawedzi = 0.
    """
    krok = 1 + w * kana
    if len(surowe) < krok * h:
        raise ValueError('skrocony IDAT: %d bajtow, potrzeba %d' % (len(surowe), krok * h))
    linie = []
    for y in range(h):
        filtr = surowe[y * krok]
        dane = surowe[y * krok + 1:(y + 1) * krok]
        prev = linie[y - 1] if y else None
        out = [0] * (w * kana)
        for x in range(w * kana):
            a = out[x - kana] if x >= kana else 0
            b = prev[x] if prev else 0
            c = prev[x - kana] if (prev and x >= kana) else 0
            if filtr == 0:
                val = dane[x]
            elif filtr == 1:
                val = dane[x] + a
            elif filtr == 2:
                val = dane[x] + b
            elif filtr == 3:
                val = dane[x] + ((a + b) >> 1)
            elif filtr == 4:
                val = dane[x] + _paeth(a, b, c)
            else:
                raise ValueError('nieznany filtr wiersza %d: %d' % (y, filtr))
            out[x] = val & 0xFF
        linie.append(out)
    return linie


def koduj(w, h, kana, linie):
    """Zapisuje wiersze z filtrem 0 (None) - najprosciej i bez bledow."""
    out = bytearray()
    for y in range(h):
        out.append(0)
        out += bytes(linie[y])
    return bytes(out)


def blok(typ, dane):
    return (struct.pack('>I', len(dane)) + typ + dane
            + struct.pack('>I', zlib.crc32(typ + dane) & 0xFFFFFFFF))


def czytaj(sciezka):
    """(w, h, kanaly, [piksele]) gdzie piksel = krotka wartosci wedlug typu koloru.

    Obsluje: 8 bitow, typy 0/2/3/4/6, paleta PLTE + tRNS, brak interlace'u.
    Rzuca ValueError z ludzkim komunikatem, gdy plik jest inny - swiadomie:
    zadne z naszych narzedzi nie chce po cichu czytac smieci.
    """
    d = sciezka.read_bytes() if hasattr(sciezka, 'read_bytes') else sciezka
    if d[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError('to nie PNG (zla sygnatura)')
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
        elif typ == b'IEND':
            break
        crc = struct.unpack('>I', d[i + 8 + ln:i + 12 + ln])[0]
        if crc != (zlib.crc32(typ + dane) & 0xFFFFFFFF):
            raise ValueError('CRC chunku %s sie nie zgadza' % typ.decode(errors='replace'))
        i += 12 + ln
    if hdr is None:
        raise ValueError('brak IHDR')
    w, h, glebia, typ_k, _komp, _flt, interlace = hdr
    if glebia != 8:
        raise ValueError('glebia %d bitow na kanale (narzedzia licza na 8)' % glebia)
    if interlace:
        raise ValueError('interlace Adam7 - najpierw eksport bez niego')
    if typ_k not in (0, 2, 3, 4, 6):
        raise ValueError('typ koloru %d' % typ_k)
    if paleta is not None and trns is not None:
        paleta = [p + (trns[k] if k < len(trns) else 255) for k, p in enumerate(paleta)]
    kana = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[typ_k]
    linie = dekoduj(zlib.decompress(idat), w, h, kana)
    piksele = []
    for y in range(h):
        for x in range(w):
            if typ_k == 3:
                idx = linie[y][x]
                piksele.append(paleta[idx] if paleta and idx < len(paleta) else (255, 0, 255, 255))
            else:
                piksele.append(tuple(linie[y][x * kana:(x + 1) * kana]))
    return w, h, kana, piksele


def zapisz(sciezka, w, h, kana, piksele, typ_k=None):
    """Zapis RGBA (typ 6) lub RGB (typ 2)/G (typ 0) - wiersze z filtrem 0."""
    if typ_k is None:
        typ_k = {1: 0, 2: 4, 3: 2, 4: 6}[kana]
    linie = []
    for y in range(h):
        wiersz = []
        for x in range(w):
            wiersz += list(piksele[y * w + x])
        linie.append(wiersz)
    dane = zlib.compress(koduj(w, h, kana, linie), 9)
    ciagi = [blok(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, typ_k, 0, 0, 0)), blok(b'IDAT', dane),
             blok(b'IEND', b'')]
    bajty = b'\x89PNG\r\n\x1a\n' + b''.join(ciagi)
    if hasattr(sciezka, 'write_bytes'):
        sciezka.write_bytes(bajty)
    else:
        with open(sciezka, 'wb') as f:
            f.write(bajty)
    return len(bajty)
