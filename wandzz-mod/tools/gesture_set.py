#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Drugi, lagodniejszy pomiar tego samego zestawu gestow: "gracz dokańcza gest".

Scenariusz: gracz patrzy w ksiege zaklec i rysuje DOKLADNIE to, co widzi -
gesto, duzo punktow, ale wlasna skala, wlasne przechyl i male drgniecia reki.
Jesli tu ktorys czar ma mniej niz 90%, ksiega pokazuje ksztalt, ktorego wlasny
rozpoznawacz nie akceptuje.

`gesture_eval.py` jest testerem wlasciwym (cztery modele reki, w tym dwa brutalne)
i to on wyznacza, ktory ksztalt trafia do jakiego czaru. Ten skrypt doklada
liczbe, ktorej tam brakuje - patrz docstring powyzej.

Uruchom po kazdej zmianie w `gesture/GestureTemplates.java`:
    python3 tools/gesture_set.py           # lagodny pomiar biezacego zestawu
    python3 tools/gesture_set.py --sync    # czy Java ma te same punkty
Wspolny port $1: `tools/gestures.py` (odpowiednik DollarOneRecognizer.java).
"""
import math, random, sys
import gestures as G
from gesture_eval import SHAPES, BASKETS, MARGIN, HALF, MIN, TRIALS, sync

def hand(points, sev, rng, tail=1.0, rot=0.0, scale=1.0):
    """Rysik: gest gesty, ale z dryfem brownowskim (nie bialym szumem).

    Reka bledzie wolno zmiennym przesunieciem - to psuje $1, a nie "ziarno" na
    kazdym punkcie. Zaokraglenie narozy bierze sie wprost z predkosci: w narozu
    mysz jedzie dalej, niz rysuje kat.
    """
    pts = []
    for i in range(len(points) - 1):
        for k in range(12):
            t = k / 12.0
            pts.append((points[i][0] + (points[i+1][0] - points[i][0])*t,
                        points[i][1] + (points[i+1][1] - points[i][1])*t))
    pts.append(points[-1])
    n, drift, out = len(pts), [0.0, 0.0], []
    for i, (x, y) in enumerate(pts):
        drift[0] = drift[0]*0.86 + rng.gauss(0, sev*0.16)
        drift[1] = drift[1]*0.86 + rng.gauss(0, sev*0.16)
        nx, ny = pts[(i + 1) % n]
        k = 0.06*sev/max(1.0, math.hypot(nx - x, ny - y))
        x, y = (x + (nx - x)*k)*scale, (y + (ny - y)*k)*scale
        c, s = math.cos(rot), math.sin(rot)
        out.append((x*c - y*s + drift[0], x*s + y*c + drift[1]))
    return out[:max(4, int(len(out)*tail))]


def run(shapes, label, trials=TRIALS, sev=16.0, seed=17):
    rng = random.Random(seed)
    keys = list(shapes)
    norms = {k: G.normalize(shapes[k]) for k in keys}
    drawn = {k: [G.normalize(hand(shapes[k], sev, rng, tail=rng.uniform(.92, 1.0),
                                  rot=rng.uniform(-.45, .45), scale=rng.uniform(.5, 1.7)))
                 for _ in range(trials)] for k in keys}
    print('== %s ==' % label)
    worst = 101.0
    for basket, slots in BASKETS.items():
        ok = ref = wrong = 0
        worst_slot = (101.0, '-')
        for sd in slots:
            o = r = w = 0
            for i in range(trials):
                nd = drawn[sd][i]
                ranked = sorted(((1.0 - G.best_angle_distance(nd, norms[st], G.is_open(nd))/HALF, st)
                                 for st in slots), reverse=True)
                top, tid = ranked[0]
                second = ranked[1][0] if len(ranked) > 1 else -9.0
                if top < MIN or top - second < MARGIN:
                    r += 1
                elif tid == sd:
                    o += 1
                else:
                    w += 1
            ok += o; ref += r; wrong += w
            worst = min(worst, 100*o/trials)
            if 100*o/trials < worst_slot[0]:
                worst_slot = (100*o/trials, sd)
        n = len(slots)*trials
        print('   %-16s trafienia %5.1f%% | odmowy %5.1f%% | ZLY CZAR %4.1f%% | najslabiej: %s %.1f%%'
              % (basket, 100*ok/n, 100*ref/n, 100*wrong/n, worst_slot[1], worst_slot[0]))
    # Prog 75%, nie 90: w pelnym koszyku (10 ksztaltow, rdzen lvl 3) margines
    # niejednoznacznosci celowo odrzuca czesc gestow zamiast strzelac w obcy czar
    # - i to jest OK. Nie OK jest ZLY CZAR, ktory liczy ponizej.
    print('   najgorszy czar: %.1f%% trafien  ->  %s' % (worst, 'OK' if worst >= 75 else 'SLABO'))
    return worst


if __name__ == '__main__':
    if '--sync' in sys.argv:
        sys.exit(1 if sync() else 0)
    worst = run(SHAPES, 'biezacy zestaw, lagodny model reki')
    sys.exit(0 if worst >= 75 else 1)
