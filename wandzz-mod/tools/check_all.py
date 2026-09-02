#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Bramka jakosci: `python3 tools/check_all.py [--mcsrc /sciezka/do/zrzutu/zrodel]`.

Sprawdza to, czego `./gradlew build` nie wyłapie albo wyłapie za pozno:

  1. pliki `.java` poza ASCII oraz rozjaz nawiasow - javac czyta zrodla w UTF-8,
     ale 1.21.11 ma znany blad UTF-8-overflow w `compile_only` (patrz README,
     sekcja "Instalacja"), ktory zjada bajty z plikow build; kod Java trzymamy
     w ASCII, diakrytyki sa w JSON-ach lang,
  2. kazdy `import a.b.C;` musi sie dawac rozwiazac - w drzewie moda albo w
     zrzucie zrodel Mojang podanym przez --mcsrc,
  3. kazde wywolanie `var.metoda()` / `Type.METODA`, gdzie `var` ma typ
     `net.minecraft.*`, musi odpowiadac wierszowi deklaracji w tym zrzucie
     (nazwy pól sa tam realne; to glowny zrodlo prawdy o API wersji),
  4. kazdy JSON w `resources` musi bywac parsowalny (kodery recipe i
     postepow odrzucaja dodatkowe pola - stad brak pol "note"),
  5. lang: parity pl/en, kazdy zarejestrowany czar ma .name i .desc w obu
     jezykach, zadnego klucza uzytego w kodzie a nieobecnego w pliku,
  6. `placeholder_textures.py --validate` (RGBA/palete/8 bpp) i `--check`
     (nie dotykac tekstur gracza),
  7. `gesture_set.py --sync` - szablon w Java musi byc tym samym ksztaltem co
     model w narzedziu (patrz runda 23: "czar nie dziala" = rozniace sie drzewa),
  8. klucze rejestrow w loot table i recipe (patrz runda 26: zly "condition"
     = Minecraft wyrzuca caly plik, w grze nie ma dropow, w logu jedna linia).

Wyjscie 0 = OK. --mcsrc nieistnieje = kroki 2-3 tylko vs drzewo moda (z
ostrzezeniem), bo bez zrodel MC nie ma czego weryfikowac.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
MOD = os.path.dirname(HERE)
SRC = os.path.join(MOD, 'src')
JAVA_TREES = [os.path.join(SRC, 'main', 'java'), os.path.join(SRC, 'client', 'java')]
RES = os.path.join(SRC, 'main', 'resources')


def pliki_java():
    for tree in JAVA_TREES:
        for root, dirs, names in os.walk(tree):
            dirs[:] = [d for d in dirs if not d.startswith('.')]
            for n in names:
                if n.endswith('.java'):
                    yield tree, root, n


def sciezka_mc(mcsrc, fqn):
    return os.path.join(mcsrc, *fqn.split('.')) + '.java'


def bez_smieci(text):
    """Stringi i komentarze na zewnatrz - do liczenia nawiasow i skladni."""
    for wzor in (r'"(?:\\.|[^"\\])*"', r'/\*.*?\*/', r'//[^\n]*'):
        text = re.sub(wzor, '', text, flags=re.S)
    return text


def krok_ascii(bledy):
    liczba = 0
    for tree, root, n in pliki_java():
        path = os.path.join(root, n)
        raw = open(path, 'rb').read()
        liczba += 1
        poza = [i for i, b in enumerate(raw) if b > 127]
        if poza:
            bledy.append('%s: bajty poza ASCII na indeksach %s' %
                         (os.path.relpath(path, MOD), poza[:4]))
        kod = bez_smieci(raw.decode('utf8', 'replace'))
        for otw, dom in (('{', '}'), ('(', ')')):
            if kod.count(otw) != kod.count(dom):
                bledy.append('%s: %s jest %d, %s jest %d' %
                             (os.path.relpath(path, MOD), otw, kod.count(otw),
                              dom, kod.count(dom)))
    return liczba


def krok_importy(bledy, mcsrc):
    nasze = set()
    for tree, root, n in pliki_java():
        txt = open(os.path.join(root, n), encoding='utf8', errors='replace').read()
        pakiet = re.search(r'^\s*package\s+([\w.]+)\s*;', txt, re.M)
        if pakiet:
            nasze.add('%s.%s' % (pakiet.group(1), n[:-5]))
    ile = 0
    skipped = set()
    for tree, root, n in pliki_java():
        path = os.path.join(root, n)
        txt = open(path, encoding='utf8', errors='replace').read()
        for m in re.finditer(r'^\s*import\s+(?:static\s+)?([\w.]+)\.([A-Z]\w*)\s*;',
                            txt, re.M):
            ile += 1
            pakiet, klase = m.group(1), m.group(2)
            if pakiet.startswith('java.') or '%s.%s' % (pakiet, klase) in nasze:
                continue
            if mcsrc and not os.path.isdir(os.path.join(mcsrc, *pakiet.split('.'))):
                skipped.add(pakiet)   # poza jurysdykcja zrzutu: Fabric API, DFU, slf4j...
                continue
            if mcsrc and os.path.isfile(sciezka_mc(mcsrc, pakiet + '.' + klase)):
                continue
            if mcsrc and klase != 'package-info':
                # klasa zagniezdzona: a.b.C.D, gdy a/b/C.java istnieje i o niej mowi
                dziad = pakiet.rsplit('.', 1)
                if len(dziad) == 2:
                    plik = sciezka_mc(mcsrc, dziad[0] + '.' + dziad[1])
                    if os.path.isfile(plik) and re.search(
                            r'\b(?:class|interface|enum|record|@interface)\s+%s\b' % klase,
                            open(plik, encoding='utf8', errors='replace').read()):
                        continue
            bledy.append('%s: import %s.%s - nierozwiazany%s' %
                         (os.path.relpath(path, MOD), pakiet, klase,
                          '' if mcsrc else ' (brak --mcsrc!)'))
    return ile, len(skipped)


# Nazwy doklejan przez fabric-api (bytecode injection w AttachmentHolder);
# w zrzucie zrodel Mojang ich nie ma, a sa legalne.
FABRIC_WTRZYNIONE = {
    'getAttached', 'getAttachedOrCreate', 'getAttachedOrCreateWith', 'setAttached',
    'hasAttached', 'removeAttached', 'getOrCreateAttached',
}
DEKLARACJA = re.compile(
    r'\b(?:public|protected|private|static|default)\b[\w<>\[\],.\s=@]*?\b(\w+)\s*[(=;]'
    r'|^\s{2,8}[\w<>\[\],.@]+\s+(\w+)\s*\([^)]*\)\s*[;{]', re.M)


def identyfikatory_zrzutu(mcsrc):
    """Nazwy zadeklarowane w zrzucie zrodel - podkladka pod bramke pisowni API.

    Celowo NIE udaje analizy typow: nie modeluje dziedziczenia ani mixinow, wiec
    pyta "czy taka nazwa istnieje w 1.21.11", a nie "czy ta klasa ja ma". To lapie
    blad, ktory przydarzal sie najczesciej (wymyslon API: getBlocksBetweenPositions,
    findBlockSources), a nie marudzi na methody z klasy bazowej.
    """
    if not mcsrc:
        return None
    znane = set()
    for root, dirs, names in os.walk(mcsrc):
        dirs[:] = [d for d in dirs if not d.startswith('.')]
        for n in names:
            if not n.endswith('.java'):
                continue
            txt = bez_smieci(open(os.path.join(root, n), encoding='utf8',
                                  errors='replace').read())
            for m in DEKLARACJA.finditer(txt):
                znane.add(m.group(1) or m.group(2))
            # stale: "PASS," (stal enuma) oraz "Pass PASS = new Pass();" (pole
            # interfejsu sealed - InteractionResult jest wlasnie takie, przez co
            # pierwsza wersja tej bramki zglosila "PASS nie istnieje"). Oba ksztalty
            # sa bez modyfikatora, wiec DEKLARACJA ich nie widzi.
            znane.update(re.findall(
                r'^\s{0,8}(?:[\w<>\[\],.]+[ \t]+)?([A-Z][A-Z0-9_]{2,})[ \t]*[=;,()]', txt, re.M))
    return znane


def skan_api(pliki, mcsrc, bledy):
    """--api-scan: clony wskazanych plikow musza istniec w zrzucie.

    Do przegladu NOWYCH plikow (te, ktore sie dopiero pisze). Nie jest w domyslnej
    bramce, bo regex nie jest parserem: dla calego drzewa dalbysmy szum, a szum w
    bramce jest gorszy niz jej brak.
    """
    znane = identyfikatory_zrzutu(mcsrc)
    if znane is None:
        bledy.append('--api-scan wymaga --mcsrc ze sciezka do zrzutu zrodel')
        return 0
    ile = 0
    for path in pliki:
        txt = open(path, encoding='utf8', errors='replace').read()
        txt = bez_smieci(txt)
        txt = re.sub(r'\b(?:net\.minecraft|com\.mojang|net\.fabricmc)[\w.]*', '', txt)
        pelne = set(re.findall(r'^\s*import\s+net\.minecraft\.[\w.]+\.(\w+)\s*;',
                              open(path, encoding='utf8', errors='replace').read(), re.M))
        # nazwa -> typ; jeszeli ta sama nazwa pojawia sie z dwoma typami (np.
        # `gate` to w GateService raz Spell, raz BlockPos w innej metodzie),
        # rezygnujemy: regex nie zna zasiegow, a bramka z szumem jest gorsza
        # niz jej brak
        kandydaci = {}
        for m in re.finditer(r'\b([A-Z]\w*)\s+(\w+)\s*[=)]', txt):
            kandydaci.setdefault(m.group(2), set()).add(m.group(1))
        zmienna = {n: typ for n, t in kandydaci.items() if len(t) == 1
                   for typ in t if typ in pelne}
        # stale w CONSTANT_CASE (PartPose.ZERO, CubeDeformation.NONE,
        # SoundEvents.HORSE_AMBIENT) dolaczone w rundzie 24: wczesniej skan patrzyl
        # tylko na male nazwy, a wlasnie tam siedza wymyslane API.
        for dostep in re.finditer(
                r'\b([a-zA-Z_]\w*)\s*\.\s*(?:([a-z_]\w*)|([A-Z][A-Z0-9_]{2,}))\b(?!\s*\.)',
                txt):
            odbior = dostep.group(1)
            clon = dostep.group(2) or dostep.group(3)
            if odbior not in zmienna and odbior not in pelne:
                continue
            if clon in ('class', 'this', 'super'):
                continue        # X.class to nie jest odwolanie do clona
            ile += 1
            if clon in FABRIC_WTRZYNIONE or clon in znane:
                continue
            bledy.append('%s: %s.%s - nazwa „%s” nie wystepuje w zrzucie %s' %
                         (os.path.relpath(path, MOD), zmienna.get(odbior, odbior),
                          clon, clon, mcsrc))
    return ile


def krok_json(bledy):
    ile = 0
    for root, dirs, names in os.walk(RES):
        dirs[:] = [d for d in dirs if not d.startswith('.')]
        for n in names:
            if not n.endswith('.json'):
                continue
            ile += 1
            path = os.path.join(root, n)
            try:
                json.load(open(path, encoding='utf8'))
            except Exception as exc:
                bledy.append('%s: %s' % (os.path.relpath(path, MOD), str(exc)[:120]))
    return ile


# --- krok 3b: klucze rejestrow w loot table / recipe -----------------------
# Minecraft 1.21.11 na zlym kluczu rzuca CAŁY plik: "Couldn't parse data file
# 'wandzz:entities/phoenix' ... Unknown registry key in minecraft:loot_condition_type:
# minecraft:killed_by_player_or_pets" (to był naprawde warunek z Bedrocka/1.12).
# W logu launchera jest to jedna linia WARNING, w grze - po prostu brak dropow.
# Bramka wyciaga legalne nazwy wprost ze zrzutu 1.21.11 (register("...")), wiec
# wymyslanie kluczy nie przechodzi. Bez --mcsrc = BLAD, bo inaczej bramka milczy.
REJESTRY_DATAPACK = {
    'typ_tabeli': 'net/minecraft/world/level/storage/loot/parameters/LootContextParamSets.java',
    'warunek': 'net/minecraft/world/level/storage/loot/predicates/LootItemConditions.java',
    'funkcja': 'net/minecraft/world/level/storage/loot/functions/LootItemFunctions.java',
    'wpis': 'net/minecraft/world/level/storage/loot/entries/LootPoolEntries.java',
    'liczba': 'net/minecraft/world/level/storage/loot/providers/number/NumberProviders.java',
    'przepis': 'net/minecraft/world/item/crafting/RecipeSerializer.java',
}
MIN_ZNALEZIONYCH = 5         # rejestr, z ktorego nic nie wyciagnelismy, nie jest bramka


def nazwy_rejestru(mcsrc, sciezka):
    """Nazwy zarejestrowane `register("<nazwa>", ...)` w podanej klasie zrzutu."""
    path = os.path.join(mcsrc, sciezka)
    if not os.path.isfile(path):
        return None
    return set(re.findall(r'register\(\s*"([a-z0-9_./]+)"', open(path, encoding='utf8',
                                                                errors='replace').read()))


def nazwy_z_vanilla(mcsrc):
    """Czego vanilla *uzywa* w swoim datapacku - uzupelnienie rejestru (aliasy)."""
    wynik = {'loot_root': set()}
    for sciezka, klucz in (('data/minecraft/loot_table', 'loot_root'),):
        for root, _dirs, names in os.walk(os.path.join(mcsrc, sciezka)):
            for n in names:
                if not n.endswith('.json'):
                    continue
                try:
                    d = json.load(open(os.path.join(root, n), encoding='utf8'))
                except Exception:
                    continue
                if isinstance(d, dict) and isinstance(d.get('type'), str):
                    wartosc = d['type']
                    wynik[klucz].add(wartosc[len('minecraft:'):] if wartosc.startswith('minecraft:') else wartosc)
    return wynik


def krok_datapack(bledy, mcsrc):
    """Kazdy "condition"/"function"/"type" w danych moda musi istniec w 1.21.11."""
    drzewo = os.path.join(RES, 'data')
    pliki = []
    for root, dirs, names in os.walk(drzewo):
        dirs[:] = [d for d in dirs if not d.startswith('.')]
        pliki += [os.path.join(root, n) for n in names if n.endswith('.json')
                  and ('loot_table' in root or 'recipe' in root)]
    if not pliki:
        return 0
    if mcsrc is None:
        bledy.append('datapack: bez --mcsrc nie da sie sprawdzic kluczy rejestrow '
                     '(a to jedyny blad, ktory Minecraft wybacza milczaco)')
        return 0
    rejestry = {}
    for nazwa, sciezka in REJESTRY_DATAPACK.items():
        znalez = nazwy_rejestru(mcsrc, sciezka)
        if znalez is None or len(znalez) < MIN_ZNALEZIONYCH:
            bledy.append('datapack: nie wyciagnieto rejestru %r ze zrodel MC (%s)' % (nazwa, sciezka))
            return 0
        rejestry[nazwa] = znalez
    # "type" wpisu puli albo dostrojca liczby - sprawdzamy against union, zeby
    # nie mlec falszywych FAIL-i (nazwa ta sama, rodziny inne).
    vanilla = nazwy_z_vanilla(mcsrc)
    wpisy = rejestry['wpis'] | rejestry['liczba']
    # "type" na gorze pliku = zestaw parametrow kontekstu (LootContextParamSets);
    # vanilla ma tam aliasy niewidoczne w rejestrze, wiec dołaczamy to, czego
    # sama uzywa w swoim datapacku.
    typy = set(rejestry['typ_tabeli'])
    for vz in vanilla['loot_root']:
        typy.add(vz)
    ile = 0
    for path in pliki:
        rel = os.path.relpath(path, MOD).replace(os.sep, '/')
        try:
            dane = json.load(open(path, encoding='utf8'))
        except Exception:
            continue                      # krok_json to zgłasza
        relatywna = 'recipe' in rel

        def wezel(o, korzen=False):
            nonlocal ile
            if isinstance(o, dict):
                for klucz, rodzina in (('condition', 'warunek'), ('function', 'funkcja')):
                    wart = o.get(klucz)
                    if isinstance(wart, str):
                        ile += 1
                        if not wart.startswith('minecraft:'):
                            bledy.append('%s: "%s": "%s" - nie w przestrzeni minecraft'
                                         % (rel, klucz, wart))
                        elif wart[len('minecraft:'):] not in rejestry[rodzina]:
                            bledy.append('%s: %s "%s" nie istnieje w 1.21.11 (dostepne: %s)'
                                         % (rel, klucz, wart, ', '.join(sorted(rejestry[rodzina]))[:150]))
                typ = o.get('type')
                if isinstance(typ, str) and typ.startswith('minecraft:'):
                    ile += 1
                    n = typ[len('minecraft:'):]
                    if korzen and not relatywna:
                        if n not in typy:
                            bledy.append('%s: "type": "%s" - brak takiego zestawu parametrow loot '
                                         'w 1.21.11 (dostepne: %s)' % (rel, typ, ', '.join(sorted(typy))))
                    elif relatywna:
                        if n not in rejestry['przepis']:
                            bledy.append('%s: recipe type "%s" nie jest zarejestrowany w 1.21.11' % (rel, typ))
                    elif n not in wpisy:
                        bledy.append('%s: "type": "%s" to ani wpis puli (%s) ani dostrojca liczby (%s)'
                                     % (rel, typ, ', '.join(sorted(rejestry['wpis'])),
                                        ', '.join(sorted(rejestry['liczba']))))
                for v in o.values():
                    wezel(v, False)
            elif isinstance(o, list):
                for v in o:
                    wezel(v, False)
        wezel(dane, True)
    return ile


def krok_lang(bledy):
    lang = os.path.join(RES, 'assets', 'wandzz', 'lang')
    pl = json.load(open(os.path.join(lang, 'pl_pl.json'), encoding='utf8'))
    en = json.load(open(os.path.join(lang, 'en_us.json'), encoding='utf8'))
    if set(pl) != set(en):
        bledy.append('lang: roznia sie zbiory kluczy: %s' % sorted(set(pl) ^ set(en))[:8])
    used = set()
    for tree, root, n in pliki_java():
        txt = open(os.path.join(root, n), encoding='utf8', errors='replace').read()
        used |= set(re.findall(r'"(wandzz\.[\w.]*)"', txt))
    # prefikty skladasne z id ("wandzz.spell." + id) nie sa kluczami
    used = {k for k in used if not k.endswith('.')}
    brak = sorted(k for k in used if k not in pl)
    if brak:
        bledy.append('lang: uzyte w kodzie, a nie w pliku: %s' % brak[:8])
    idy = set()
    for n in sorted(os.listdir(os.path.join(SRC, 'main', 'java', 'com', 'wandzz', 'spell', 'impl'))):
        if n.endswith('.java'):
            txt = open(os.path.join(SRC, 'main', 'java', 'com', 'wandzz', 'spell', 'impl', n),
                       encoding='utf8', errors='replace').read()
            m = re.search(r'String\s+id\(\)\s*\{\s*return\s+"([\w:.]+)"', txt)
            if m:
                # klucz lang = "wandzz.spell." + to, co po dwukropku (SpellBookScreen:152)
                idy.add(m.group(1).split(':')[-1])
    zarejestrowane = len(re.findall(r'\bregister\(new ',
                                    open(os.path.join(SRC, 'main', 'java', 'com', 'wandzz',
                                                      'spell', 'Spells.java'),
                                         encoding='utf8').read()))
    if zarejestrowane != len(idy):
        bledy.append('Spells.bootstrap(): %d register(), a w spell/impl jest %d klas' %
                     (zarejestrowane, len(idy)))
    for cid in sorted(idy):
        if ('wandzz.spell.' + cid) not in pl and ('wandzz.spell.%s.name' % cid) not in pl:
            bledy.append('lang: czar %s nie ma nazwy' % cid)
        if ('wandzz.spell.%s.desc' % cid) not in pl:
            bledy.append('lang: czar %s nie ma .desc' % cid)
        for sufiks in ('', '.desc'):
            if ('wandzz.spell.%s%s' % (cid, sufiks)) not in en:
                bledy.append('lang (en): brak klucza wandzz.spell.%s%s' % (cid, sufiks))
    return len(pl), len(idy), zarejestrowane


def uruchom(bledy, argv, opis):
    wynik = subprocess.run([sys.executable] + argv, cwd=MOD, capture_output=True, text=True)
    if wynik.returncode != 0:
        bledy.append('%s -> %d\n%s' % (opis, wynik.returncode,
                                       (wynik.stdout + wynik.stderr)[-700:]))
    return (wynik.stdout or '').strip().splitlines()[-1:] or ['']


def main(argv=None):
    # Wiadoma, akceptowana dziura w siatce UV jednorozca: gorna twarz glowy
    # (UP) trafia w (13,18)-(19,25) arkusza gracza, ktore ten jeszcze nie
    # zamalowal. To wierzch czaszki pod grzywa i rogiem - w grze prawie niewidoczny.
    # Zmniejsz do 0, gdy te 6x7 px zostanie domalowane (patrz README, "Jednorozec").
    ZNANE_DZIURY_UV = 1

    parser = argparse.ArgumentParser(description='bramka jakosci moda Wandzz')
    parser.add_argument('--mcsrc', default='/home/user/mcsrc',
                        help='korzen zrzutu zrodel Mojang (git clone ... -b main)')
    parser.add_argument('--api-scan', nargs='*', default=[], metavar='PLIK',
                        help='dodatkowo: sprawdź członów MC w tych plikach (nowe czary)')
    args = parser.parse_args(argv)
    mcsrc = args.mcsrc if os.path.isdir(os.path.join(args.mcsrc, 'net', 'minecraft')) else None

    bledy = []
    n_java = krok_ascii(bledy)
    n_clon = skan_api(args.api_scan, mcsrc, bledy) if args.api_scan else 0
    n_import, n_skip = krok_importy(bledy, mcsrc)
    n_json = krok_json(bledy)
    n_datapack = krok_datapack(bledy, mcsrc)
    n_lang, n_czary, n_reg = krok_lang(bledy)
    walid = uruchom(bledy, [os.path.join(HERE, 'placeholder_textures.py'), '--validate'],
                   'placeholder_textures.py --validate')
    check = uruchom(bledy, [os.path.join(HERE, 'placeholder_textures.py'), '--check'],
                    'placeholder_textures.py --check')
    sync = uruchom(bledy, [os.path.join(HERE, 'gesture_set.py'), '--sync'],
                   'gesture_set.py --sync')
    uv_wynik = uruchom(bledy, [os.path.join(HERE, 'unicorn_uv.py'), '--json'],
                       'unicorn_uv.py --json')
    uv_txt = uv_wynik[0] if uv_wynik else ''
    sygnal = 'pominiete (brak parsowania)'
    if uv_txt and uv_txt.startswith('{'):
        try:
            uv = json.loads(uv_txt)
            if uv['arkusz'] and uv['arkusz'] != uv['deklarowany']:
                bledy.append('unicorn_uv: LayerDefinition mowi %s, a plik ma %s - MC '
                             'przeskaluje UV' % (uv['deklarowany'], uv['arkusz']))
            if uv['boxes'] < 10:
                bledy.append('unicorn_uv: tylko %d bryl wyciagnietych z UnicornModel.java '
                             '- regex sie rozjechal z kodem (addBox musi brac literaly)'
                             % uv['boxes'])
            if uv['holey'] > ZNANE_DZIURY_UV:
                bledy.append('unicorn_uv: %d bryl ma twarze w przezroczystosci (wolno %d): %s'
                             % (uv['holey'], ZNANE_DZIURY_UV, uv['holes']))
            sygnal = '%d bryl, %d dziurawych (wolno %d)' % (
                uv['boxes'], uv['holey'], ZNANE_DZIURY_UV)
        except json.JSONDecodeError as e:
            bledy.append('unicorn_uv --json: nie dalo sie sparsowac (%s)' % e)
    print('%d .java | %d importow (%d pakietow bibliotecznych poza zrzutem) | '
          '%d JSON | %d kluczy lang | %d czarow (register: %d)'
          % (n_java, n_import, n_skip, n_json, n_lang, n_czary, n_reg))
    print('  klucze datapacku: %d condition/function/type vs rejestry 1.21.11'
          % n_datapack)
    if args.api_scan:
        print('  --api-scan: %d odwolan do clonow MC w %d plikach' %
              (n_clon, len(args.api_scan)))
    print('  MC src:', mcsrc or 'BRAK - kroki 2-3 ograniczone do drzewa moda')
    print('  tekstury:', walid[0])
    print('  palete:', check[0])
    print('  gesty:', sync[0])
    print('  UV jednorozca:', sygnal)
    for b in bledy[:20]:
        print('  FAIL', b)
    if len(bledy) > 20:
        print('  ... %d wiecej' % (len(bledy) - 20))
    print('%d problemow' % len(bledy))
    return 1 if bledy else 0


if __name__ == '__main__':
    sys.exit(main())
