#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Przeszukiwanie par (skok, brama) - bo to one podkradaja niedkonczone kolo.

Wniosek z pomiarow (patrz README, sekcja Gestury): $1 po normalizacji widzi
tylko sekwencje kątów, a kazdy KSZTALT ZAGLĘBIONY/lukowaty (garb, micha, luk,
mostek, kolo z przerwa) jest najblizszym sasiedziem kola narysowanego reka -
gracz dostaje wtedy obcy czar zamiast odmowy. Dlatego dozwolone sa tylko ksztalty
lamane z co najmniej jednym samoprzecieciem lub powrotem.

Ten skrypt liczy to, co naprawde interesting:
  * dla kazdego ksztaltu w zestawie: trafienia w 4 modelach reki,
  * "proby": kto przejmuje kolo urwane o 28%, luk 250°, daszek bez podstawy -
    odpowiedzi DOZWOLONE to wlasny czar albo ODMOWA (nie wolno rzucic obcego).
Macierz par (rysunek x szablon) jest liczona raz dla catej puli ksztaltow, wiec
przeszukanie wszystkich polaczen kosztuje tyle co kilka pomiarow.
"""
import math, random, sys, itertools
import gesture_eval as E
import gestures as G

def circle(n=32, r=100.0):
    return [(r*math.cos(2*math.pi*i/n), r*math.sin(2*math.pi*i/n)) for i in range(n+1)]

FIX = {
    'strike':        [(-100, 60), (-20, 60), (60, -80), (100, -20)],
    'break_block':   [(-90, -40), (90, -40), (-60, -40), (-60, 40), (60, 40), (60, -40), (90, -40)],
    'torch':         [(-90, -80), (90, -80), (0, -80), (0, 80)],
    'heal':          circle(32),
    'fireball':      [(-100, 80), (0, -80), (100, 80), (-60, 80), (60, 80)],
    'dragon_breath': E.sine(2, 80.0),
    'teleport':      [(-175, 60), (-175, -60), (-65, -60), (-65, 60), (65, 60),
                      (65, -60), (175, -60), (175, 60), (65, 60)],
    'bomb':          [(-100, 0), (0, -100), (100, 0), (0, 100), (-100, 0), (0, -40), (0, 40)],
}
LEAP = {
 'spring2': [(-95, 70), (-95, -50), (0, 50), (95, -50), (95, 70)],
 'step':    [(-100, 60), (0, 60), (0, -80), (100, -80)],
 'corner':  [(-90, 90), (90, 90), (90, -90)],
 'spring':  [(-100, 80), (-100, -40), (0, 40), (100, -40), (100, 80)],
 'gap':     [(-100, 60), (-40, 60), (-40, -60), (40, -60), (40, 60), (100, 60)],
 'pick':    [(-100, -60), (100, 60), (60, -100), (-60, 100)],
}
GATE = {
 'brama5':  [(-90, 80), (-90, -50), (0, -50), (0, 40), (90, 40), (90, -80)],
 'brama6':  [(-90, 70), (-90, -40), (-15, -40), (-15, 40), (15, 40), (15, -40), (90, -40), (90, 70)],
 'bridge':  [(-100, 70), (-100, -30), (0, -75), (100, -30), (100, 70)],
 'brama2':  [(-90, 80), (-90, -50), (-20, -50), (-20, 20), (20, 20), (20, -50), (90, -50), (90, 80)],
 'N':       [(-80, 80), (-80, -80), (80, 80), (80, -80)],
 'rama':    [(-90, 80), (-90, -70), (90, -70), (90, 80), (-90, 80), (0, 80)],
 'Z':       [(-90, -60), (90, -60), (-90, 60), (90, 60)],
 'M3':      [(-90, 90), (-90, -80), (0, 60), (90, -80), (90, 90)],
}
FIRE = {
 'caret_bar': [(-100, 80), (0, -80), (100, 80), (-60, 80), (60, 80)],
 'spike':     [(-100, 60), (-25, 60), (0, -80), (25, 60), (100, 60)],
 'tri':       [(-90, 70), (90, 70), (0, -80), (-90, 70)],
}
TORCH = {
 'T':       [(-90, -80), (90, -80), (0, -80), (0, 80)],
 'T_short': [(-70, -70), (70, -70), (0, -70), (0, 90)],
}
PUL = dict(FIX)
PUL.update({'L_' + k: v for k, v in LEAP.items()})
PUL.update({'G_' + k: v for k, v in GATE.items()})
PUL.update({'F_' + k: v for k, v in FIRE.items()})
PUL.update({'T_' + k: v for k, v in TORCH.items()})
PROBES = {
 'kolo':      circle(32),
 'kolo_odiete': circle(32)[:24],
 'luk250':    circle(22)[:16],
 'daszek':    [(-100, 80), (0, -80), (100, 80)],
 'zygzak':    [(-90, -60), (90, -60), (-90, 60), (90, 60)],
}
PUL.update({'P_' + k: v for k, v in PROBES.items()})
NAMES = list(PUL)
NORM = {k: G.normalize(v) for k, v in PUL.items()}
TRIALS = 24
MODELS = list(E.VARIANTS)
DRAW = {m: {} for m in MODELS}
for m in MODELS:
    rng = random.Random(4242 + 31*len(m))
    for k in NAMES:
        DRAW[m][k] = [G.normalize(E.hand(PUL[k], rng, m)) for _ in range(TRIALS)]
print('rysy gotowe', flush=True)
SC = {m: {} for m in MODELS}
for m in MODELS:
    for a in NAMES:
        SC[m][a] = [[1.0 - G.best_angle_distance(d, NORM[b], G.is_open(d))/E.HALF for b in NAMES]
                    for d in DRAW[m][a]]
print('macierz gotowa (%d ksztaltow)' % len(NAMES), flush=True)
IDX = {k: i for i, k in enumerate(NAMES)}
BASK = E.BASKETS

def evalset(assign, probes_too=True):
    slot_names = {k: v if k in FIX else None for k, v in assign.items()}
    per = {}
    mx = 0.0
    worst = 101
    for basket, bs in BASK.items():
        for sd in bs:
            key = assign[sd]
            cells = []
            for m in MODELS:
                rows = SC[m][key]
                ok = ref = wrong = 0
                for i in range(TRIALS):
                    r = rows[i]
                    best = -9.0; bid = None; second = -9.0
                    for t in bs:
                        val = r[IDX[assign[t]]]
                        if val > best: second, best, bid = best, val, t
                        elif val > second: second = val
                    if best < E.MIN or best - second < E.MARGIN: ref += 1
                    elif bid == sd: ok += 1
                    else: wrong += 1
                cells.append((100*ok/TRIALS, 100*ref/TRIALS, 100*wrong/TRIALS))
            per[sd] = cells
            worst = min(worst, min(c[0] for c in cells))
            mx = max(mx, max(c[2] for c in cells))
    res = {'worst': worst, 'wrong': mx, 'per': per}
    if probes_too:
        prob = {}
        for pname, pkey in (('kolo', 'P_kolo'), ('odiete', 'P_kolo_odiete'), ('luk', 'P_luk250'),
                            ('daszek', 'P_daszek')):
            tally = {}
            for m in MODELS:
                rows = SC[m][pkey]
                for i in range(TRIALS):
                    r = rows[i]
                    best = -9.0; bid = None; second = -9.0
                    for t in BASK['pelny']:
                        val = r[IDX[assign[t]]]
                        if val > best: second, best, bid = best, val, t
                        elif val > second: second = val
                    if best < E.MIN or best - second < E.MARGIN: bid = 'odmowa'
                    tally[bid] = tally.get(bid, 0) + 1
            prob[pname] = tally
        res['probes'] = prob
    return res

COMBOS = []
for lk, gk in itertools.product(LEAP, GATE):
    for fk in FIRE:
        COMBOS.append(dict(leap='L_' + lk, open_gate='G_' + gk, fireball='F_' + fk))
rows = []
for c in COMBOS:
    assign = dict(FIX); assign.update(c); assign['torch'] = 'T_T'
    for k in FIX:
        assign[k] = k
    r = evalset(assign)
    # kara: kolo / urwane kolo / luk musza trafic w heal albo w odmowe;
    # daszek (kula ognia bez podstawy) moze trafic w fireball albo w odmowe
    ALLOW = {'kolo': {'heal', 'odmowa'}, 'odiete': {'heal', 'odmowa'},
             'luk': {'heal', 'odmowa'}, 'daszek': {'fireball', 'odmowa'}}
    theft = sum(v for k, t in r['probes'].items() for who, v in t.items() if who not in ALLOW[k])
    rows.append((r['wrong']*100 + theft*4 - r['worst']*0.2, r, c))
def segs(shape): return max(0, len(shape) - 1)
def segs_of(c): return sum(segs(PUL[c[k]]) for k in c)
clean = [x for x in rows if x[1]['wrong'] == 0.0 and x[0] == x[1]['wrong']*100 - x[1]['worst']*0.2]
rows.sort(key=lambda x: (1 if x[1]['wrong'] else 0, x[0], segs_of(x[2])))
if clean:
    clean.sort(key=lambda x: (-x[1]['worst']*0.0 + x[1]['wrong']*100 - x[1]['worst']*0.2, segs_of(x[2])))
print('\n%-6s %-8s %-10s | traf.%%  zly.%% | kolo/odiete/luk/daszek (nieleczeni winni byc odmowa)'
      % ('skok', 'brama', 'kula'))
for sc, r, c in rows[:26]:
    pr = r['probes']
    def s(k, allow):
        t = pr[k]
        return '/'.join('%s:%d' % (w, v) for w, v in sorted(t.items(), key=lambda x: -x[1]) if w not in allow) or 'ok'
    nseg = sum(segs(PUL[c[k]]) for k in c)
    print('%-6s %-8s %-10s | %3.0f  %4.1f  %2d seg | odiete[%s] luk[%s] daszek[%s]'
          % (c['leap'][2:], c['open_gate'][2:], c['fireball'][2:], r['worst'], r['wrong'], nseg,
             s('odiete', ('odmowa',)), s('luk', ('odmowa',)), s('daszek', ('odmowa', 'fireball'))))
best = rows[0][1]
print('\nNAJLEPSZY: %s' % dict(rows[0][2]))
for sd, cells in best['per'].items():
    print('   %-14s %s' % (sd, '  '.join('%s %3.0f/%2.0f/%2.0f' % (m[:6], *c) for m, c in zip(MODELS, cells))))
