#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Test jakosci zestawu gestow - "cztery rece", od rysika do drzacej myszy.

Po kazdej zmianie w `gesture/GestureTemplates.java` (albo po dodaniu czaru)
uruchom:

    python3 tools/gesture_eval.py            # ocena biezacego zestawu + starego
    python3 tools/gesture_eval.py --sync     # czy Java ma te same punkty co tu

Dlaczego cztery modele reki, a nie jeden? Bo $1 na "idealnym" pismie radzi sobie
ze wszystkim - i taki pomiar nic nie mowi. Realny blad myszy jest inny:
    v0  "vertebrae"   - gracz rysuje tylko wierzcholki (pociagniecia proste),
    v1  to samo +    - ale puszcza przycisk, gdy ostatnie 28% sciezki jeszcze
                        nie zostalo narysowane,
    s0  "smooth"      - duzo punktow, za to mocno drza (dryf ~3x wiekszy),
    g0  "gentle"      - rysik/kreska: duzo punktow, male drgniecia.
Ten zestaw modelow odtwarza skargi graczy z 1.21.11: na "obecnym" (swieczka, luk,
gwiazda) v1 rzuca obcy czar w 5.6% prob - leczenie wpadalo w brame, kula ognia w
brame, oddech w uderzenie. Po zamianie ksztaltow: 0.0%.

Kryteria akceptacji (narzedzie zwraca kod 1, jezele sa spelnilo):
  * ZLY CZAR (trafienie w obcy szablon) = 0.0% w kazdym koszyku i modelu,
  * trafienia: pelny koszyk >= 90%, kazdy czar w swoim koszyku >= 85%.
Wartosci odniesienia dla "przyzwoitego" zestawu: 94-100% trafien, 0% zlego czaru.
"""
import math, random, re, sys, pathlib
import gestures as G

HERE = pathlib.Path(__file__).resolve().parent
JAVA = HERE.parent / 'src/main/java/com/wandzz/gesture/GestureTemplates.java'
TRIALS = 40
MARGIN = getattr(G, 'AMBIGUITY_MARGIN', 0.035)
MIN = G.MIN_SCORE
HALF = G.HALF_DIAG

# --------------------------------------------------------------- generatory --
def open_circle(samples=28, gap=60.0, r=100.0, start=50.0):
    """Kolo z przerwa - wzor przegrany w pomiarze; zostal jako OLD_SHAPES."""
    span = 360.0 - gap
    return [(r*math.cos(math.radians(start - span*i/samples)), -r*math.sin(math.radians(start - span*i/samples)))
            for i in range(samples + 1)]

def circle(n=32, r=100.0):
    """Pelne kolo - ten sam wzor co GestureTemplates.circle()."""
    return [(r*math.cos(2*math.pi*i/n), r*math.sin(2*math.pi*i/n)) for i in range(n + 1)]

def sine(arms, amp, n=44):
    return [(-100.0 + 200.0*i/n, amp*math.sin(arms*math.pi*i/n)) for i in range(n + 1)]

# --------------------------------------------------------- ksztalty (nowe) ---
# Metoda w Java -> slot. Punkty musza byc identyczne (--sync to pilnuje).
JAVA_METHODS = {
    'strike': 'strikeStroke', 'break_block': 'breakBlockStroke', 'torch': 'torchStroke',
    'leap': 'leapStroke', 'heal': 'healStroke', 'fireball': 'flameTriangle',
    'dragon_breath': 'breathWave', 'open_gate': 'barredGateStroke',
    'teleport': 'twinSquares', 'bomb': 'diamondTick',
    'reveal': 'revealStroke', 'invisible': 'invisibilityStroke',
}
# Spell#requiredLevel - tu tylko po to, zeby zbudowac koszyki jak w grze.
LEVEL = {'strike': 1, 'break_block': 1, 'torch': 1, 'leap': 1, 'heal': 2,
         'fireball': 2, 'dragon_breath': 2, 'open_gate': 2, 'teleport': 3, 'bomb': 3,
         'reveal': 2, 'invisible': 3}

SHAPES = {
    'strike':        [(-100, 60), (-20, 60), (60, -80), (100, -20)],
    'break_block':   [(-90, -40), (90, -40), (-60, -40), (-60, 40), (60, 40), (60, -40), (90, -40)],
    'torch':         [(-90, -80), (90, -80), (0, -80), (0, 80)],
    'leap':          [(-95, 70), (-95, -50), (0, 50), (95, -50), (95, 70)],
    'heal':          circle(32),
    'fireball':      [(-90, 70), (90, 70), (0, -80), (-90, 70)],
    'dragon_breath': sine(2, 80.0),
    'open_gate':     [(-80, 80), (-80, -80), (80, 80), (80, -80)],
    'teleport':      [(-175, 60), (-175, -60), (-65, -60), (-65, 60), (65, 60),
                      (65, -60), (175, -60), (175, 60), (65, 60)],
    'bomb':          [(-100, 0), (0, -100), (100, 0), (0, 100), (-100, 0), (0, -40), (0, 40)],
    # dwie nowe (runda 21) - dobrane POMIAREM, nie gustem; patrz GestureTemplates
    'reveal':        [(0, -100), (0, 100), (-100, 0), (100, 0)],
    'invisible':     [(-110, -70), (-40, -70), (-40, -10), (20, -10), (20, 50), (90, 50)],
}
# Zestaw z rundy 16 (swieczka / luk / gwiazda) - pozostaje jako porownanie,
# zeby nie dac sie ponownie skusic "ladniejszym" ksztaltom.
OLD_SHAPES = {
    'strike':        [(-100, -80), (0, 80), (100, -80)],
    'break_block':   SHAPES['break_block'],
    'torch':         [(-40, 90), (40, 90), (0, -90), (-40, 90), (0, 90)],
    'leap':          [(-110, 60), (-40, 60), (-10, -70), (20, 60), (110, 60)],
    'heal':          open_circle(32, 0.0),
    'fireball':      [(0, -110), (30, -30), (110, -10), (40, 20), (60, 100), (0, 50),
                      (-60, 100), (-40, 20), (-110, -10), (-30, -30), (0, -110)],
    'dragon_breath': sine(2, 80.0),
    'open_gate':     [(-95 + 190*i/22, -70*math.sqrt(max(0.0, 1 - ((-95 + 190*i/22)/95.0)**2)))
                      for i in range(23)],
    'teleport':      SHAPES['teleport'],
    'bomb':          SHAPES['bomb'],
}
BASKETS = {
    'lvl1':  [k for k in SHAPES if LEVEL[k] <= 1],
    'lvl2':  [k for k in SHAPES if LEVEL[k] <= 2],
    'pelny': list(SHAPES),
}
# per = punkty miedzy wierzcholkami, sev = amplituda dryfu, cut = ile sciezki
# gracz faktycznie rysuje do konca.
VARIANTS = {
    'wierz':   dict(per=1,  sev=10.0, cut=1.00),
    'uciety':  dict(per=1,  sev=10.0, cut=0.72),
    'drzacy':  dict(per=4,  sev=34.0, cut=1.00),
    'rysik':   dict(per=12, sev=12.0, cut=0.96),
}

# ------------------------------------------------------------- reka (symul.) --
def densify(pts, per):
    if per <= 1:
        return list(pts)
    out = []
    for i in range(len(pts) - 1):
        for k in range(per):
            t = k/per
            out.append((pts[i][0] + (pts[i+1][0] - pts[i][0])*t,
                        pts[i][1] + (pts[i+1][1] - pts[i][1])*t))
    out.append(pts[-1])
    return out

def drift(pts, rng, sev):
    out = []; d = [0.0, 0.0]; n = len(pts)
    for i, (x, y) in enumerate(pts):
        d[0] = d[0]*0.86 + rng.gauss(0, sev*0.16)
        d[1] = d[1]*0.86 + rng.gauss(0, sev*0.16)
        nx, ny = pts[(i + 1) % n]
        k = 0.06*sev/max(1.0, math.hypot(nx - x, ny - y))
        out.append((x + (nx - x)*k + d[0], y + (ny - y)*k + d[1]))
    return out

def cut_by_length(pts, frac):
    if frac >= 1.0:
        return pts
    L = [0.0]
    for i in range(1, len(pts)):
        L.append(L[-1] + math.dist(pts[i-1], pts[i]))
    lim = L[-1]*frac
    for i in range(1, len(pts)):
        if L[i] > lim:
            return pts[:max(8, i)]
    return pts

def hand(shape, rng, v):
    kw = VARIANTS[v]
    p = drift(densify(shape, kw['per']), rng, kw['sev'])
    rot = rng.uniform(-0.35, 0.35)
    sc = rng.uniform(0.55, 1.5)
    c, s = math.cos(rot), math.sin(rot)
    p = [(x*sc*c - y*sc*s, x*sc*s + y*sc*c) for x, y in p]
    return cut_by_length(p, kw['cut'])

# ------------------------------------------------------------------- pomiar --
def baskets_for(shapes):
    """Koszyki ZLOZONE Z PODANEGO ZESTAWU, nie z globalnego SHAPES.

    Bez tego OLD_SHAPES (dziesiec czarow z rundy 16) wywolywalo KeyError, bo
    BASKETS liczy sie z biezacych dwunastu - a punkt odniesienia ma zostac
    punktem odniesienia w swojej wlasnej, historyjnej konfiguracji.
    """
    return {'lvl1':  [k for k in shapes if LEVEL.get(k, 3) <= 1],
            'lvl2':  [k for k in shapes if LEVEL.get(k, 3) <= 2],
            'pelny': list(shapes)}


def measure(shapes, trials=TRIALS, seed=4711, baskets=None):
    """Zwraca per[(slot, basket)][model] = (trafienia, odmowy, zly) w procentach."""
    baskets = baskets or baskets_for(shapes)
    slots = list(shapes)
    norms = {k: G.normalize(v) for k, v in shapes.items()}
    per = {}
    for model in VARIANTS:
        rng = random.Random(seed + 7919*len(model))
        for slot in slots:
            for basket, bs in baskets.items():
                if slot not in bs:
                    continue
                tpl = {st: norms[st] for st in bs}
                drawn = [G.normalize(hand(shapes[slot], rng, model)) for _ in range(trials)]
                ok = ref = wrong = 0
                for d in drawn:
                    op = G.is_open(d)
                    ranked = sorted(((1.0 - G.best_angle_distance(d, tpl[st], op)/HALF, st) for st in bs),
                                    reverse=True)
                    top, tid = ranked[0]
                    second = ranked[1][0] if len(ranked) > 1 else -9.0
                    if top < MIN or top - second < MARGIN:
                        ref += 1
                    elif tid == slot:
                        ok += 1
                    else:
                        wrong += 1
                per.setdefault((slot, basket), {})[model] = (100*ok/trials, 100*ref/trials, 100*wrong/trials)
    return per

def report(shapes, label, verbose=False):
    baskets = baskets_for(shapes)
    per = measure(shapes, baskets=baskets)
    print('== %s ==' % label)
    tot = {'ok': 0.0, 'ref': 0.0, 'wrong': 0.0, 'n': 0}
    worst_wrong = 0.0
    for basket in baskets:
        line = []
        b_ok = b_wrong = 0.0
        for slot in baskets[basket]:
            cells = list(per[(slot, basket)].values())
            ok = min(c[0] for c in cells)
            wrong = max(c[2] for c in cells)
            b_ok += sum(c[0] for c in cells)/len(cells)  # suma procentow przez czary
            b_wrong += wrong
            worst_wrong = max(worst_wrong, wrong)
            line.append('%s %3.0f%s' % (slot[:11], ok, ('!%2.0f' % wrong) if wrong else ''))
            if verbose:
                for m, c in per[(slot, basket)].items():
                    print('        %-14s %-8s traf %3.0f odm %3.0f zly %3.0f' % (slot, m, *c))
        n = len(baskets[basket])
        print('   %-6s srednio %5.1f%% trafien | %s' % (basket, b_ok/n, '  '.join(line)))
        tot['ok'] += b_ok; tot['wrong'] += b_wrong; tot['n'] += n
    print('   RAZEM: %.1f%% trafien, max ZLY CZAR %.1f%%  ->  %s'
          % (tot['ok']/tot['n'], worst_wrong, 'OK' if worst_wrong == 0 else 'FAIL'))
    return worst_wrong, tot['ok']/tot['n']

# --------------------------------------------------------------------- sync --
def java_shapes():
    src = JAVA.read_text(encoding='utf8')
    out = {}
    for slot, method in JAVA_METHODS.items():
        m = re.search(r'List<Point> %s\(\)\s*\{\n(.*?)\n    \}' % method, src, re.S)
        if not m:
            out[slot] = None
            continue
        body = m.group(1)
        pts = [(float(a), float(b)) for a, b in
               re.findall(r'new Point\(\s*(-?[\d.]+)(?:\s*\*\s*[\d.]+)?\s*,\s*(-?[\d.]+)', body)]
        if pts:
            out[slot] = ('points', pts)
            continue
        gen = re.search(r'return circle\((\d+)\)', body)
        if gen:
            out[slot] = ('points', circle(int(gen.group(1))))
            continue
        if 'Math.sin' in body and 'PI * i' in body:
            # breathWave(): liczby wziete prosto z petli w Java
            n = int(re.search(r'i <= (\d+)', body).group(1))
            amp = float(re.search(r'([\d.]+)\s*\*\s*Math\.sin', body).group(1))
            arms = float(re.search(r'([\d.]+) \* Math\.PI', body).group(1))
            out[slot] = ('points', sine(arms, amp, n))
            continue
        out[slot] = ('generator', re.search(r'return (\w+)\(([^)]*)\)', body).groups()
                     if re.search(r'return (\w+)\(([^)]*)\)', body) else None)
    return out

def sync():
    j = java_shapes()
    bad = 0
    for slot, method in JAVA_METHODS.items():
        mine = SHAPES[slot]
        got = j.get(slot)
        if not got or got[0] != 'points':
            print('  %-14s %-16s WZOR w Java do sprawdzania recznie: %s()' % (slot, '(generator)', method))
            continue
        pts = got[1]
        same = len(pts) == len(mine) and all(abs(a[0] - b[0]) < .51 and abs(a[1] - b[1]) < .51
                                             for a, b in zip(mine, pts))
        print('  %-14s %s' % (slot, 'ZGODNE' if same else 'ROZJEZDNE'))
        if not same:
            bad += 1
            print('     java: %s' % pts[:6])
            print('     py  : %s' % [tuple(round(c) for c in p) for p in mine[:6]])
    print('--sync: %s' % ('ROZJEZDNE (%d)' % bad if bad
                       else 'ZGODNE (wszystkie %d ksztaltow)' % len(JAVA_METHODS)))
    return bad

if __name__ == '__main__':
    if '--sync' in sys.argv:
        sys.exit(1 if sync() else 0)
    verbose = '-v' in sys.argv
    w_new, ok_new = report(SHAPES, 'biezacy zestaw (proste gesty, kolo + N + trojkat)', verbose)
    w_old, ok_old = report(OLD_SHAPES, 'zestaw z rundy 16 (swieczka, luk, gwiazda) - punktu odniesienia', verbose)
    print()
    print('poprawka: trafienia %+.1f pkt, zly czar %.1f%% -> %.1f%%' % (ok_new - ok_old, w_old, w_new))
    sys.exit(0 if w_new == 0.0 else 1)
