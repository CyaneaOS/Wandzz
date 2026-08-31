#!/usr/bin/env python3
"""Pomiar jakosci zestawu gestow moda.

Uruchom po kazdej zmianie w `gesture/GestureTemplates.java` albo po dodaniu
czaru. Wypisuje dla kazdego ksztaltu:
  trafienia  - % prob, w ktorych narysowany ksztalt zostal przypisany DO SIEBIE
  odmowy     - % prob odrzuconych (ponizej progu albo wicelider za blisko)
  zly        - % prob, w ktorych rzucony zostal OBCY czar (to jest najgorsza
               pozycja: gracz nie wie, ze cos poszlo nie tak)

Zasada, ktorej ten pomiar pilnuje: przy 10 ksztaltach jednopociagowych $1 NIE
potrafi ich rozdzielic z szumem myszy - dlatego rozpoznawanie w grze jest
zawezane do czarow z rdzeni trzymanej rozdzki (CastingScreen#castableIds) i ma
margines niejednoznacznosci. Tutaj mierzymy oba te zachowania osobno: koszyk
pelny (ksiega zaklec) i koszyki lvl1/lvl2 (normalna gra).
"""
import math, random, sys, re, pathlib
import gestures as G

ROOT = pathlib.Path(__file__).resolve().parent.parent / 'src/main/java/com/wandzz/gesture/GestureTemplates.java'

def sine(arms, amp, n=44):
    return [(-100 + 200*i/n, amp*math.sin(arms*math.pi*i/n)) for i in range(n+1)]
def circle(n=32, r=100.0):
    return [(r*math.cos(2*math.pi*i/n), r*math.sin(2*math.pi*i/n)) for i in range(n+1)]
def arch(n=22):
    return [(-95 + 190*i/n, -70*math.sqrt(max(0.0, 1 - ((-95 + 190*i/n)/95.0)**2))) for i in range(n+1)]
def flame(n=22):
    return ([(70*math.sin(math.pi*i/n), -100 + 190*(1-math.cos(math.pi*i/n))/2) for i in range(n+1)]
            + [(-70*math.sin(math.pi*i/n), -100 + 190*(1-math.cos(math.pi*i/n))/2) for i in range(n, -1, -1)])

# Ksztalty MUSZA odpowiadac GestureTemplates.java - patrz --sync ponizej.
SHAPES = {
    'strike':        [(-100, -80), (0, 80), (100, -80)],
    'break_block':   [(-90, -40), (90, -40), (-60, -40), (-60, 40), (60, 40), (60, -40), (90, -40)],
    'torch':         [(-40, 90), (40, 90), (0, -90), (-40, 90), (0, 90)],
    'leap':          [(-110, 60), (-40, 60), (-10, -70), (20, 60), (110, 60)],
    'heal':          circle(32),
    'fireball':      [(0, -110), (30, -30), (110, -10), (40, 20), (60, 100), (0, 50),
                      (-60, 100), (-40, 20), (-110, -10), (-30, -30), (0, -110)],
    'dragon_breath': sine(2, 80),
    'open_gate':     arch(22),
    'teleport':      [(-175, 60), (-175, -60), (-65, -60), (-65, 60), (65, 60),
                      (65, -60), (175, -60), (175, 60), (65, 60)],
    'bomb':          [(-100, 0), (0, -100), (100, 0), (0, 100), (-100, 0), (0, -40), (0, 40)],
}
# Koszyki jak w grze: Spell#requiredLevel + Spell#isProvidedBy
BASKETS = {
    'lvl1 (rdzen 1)': ['strike', 'break_block', 'torch', 'leap'],
    'lvl2 (rdzen 2)': ['strike', 'break_block', 'torch', 'leap', 'fireball', 'heal', 'dragon_breath', 'open_gate'],
    'pelny (10)':     list(SHAPES),
}
TRIALS, SEV, MARGIN = 40, 16.0, G.AMBIGUITY_MARGIN if hasattr(G, 'AMBIGUITY_MARGIN') else 0.035


def hand(points, sev, rng, tail=1.0, rot=0.0, scale=1.0):
    """Symulacja ruchu mysza: gest gesty, ale z DRYFEM (nie bialym szumem).

    Reka bledzi wolno zmiennym przesunieciem - to jest to, co psuje $1, a nie
    "ziarno" na kazdym punkcie. Zaokraglenie narozy jest wprost z predkosci:
    w narozu mysz jedzie dalej, niz rysuje sie kat.
    """
    pts = []
    for i in range(len(points) - 1):
        for k in range(12):
            t = k / 12.0
            pts.append((points[i][0] + (points[i+1][0]-points[i][0])*t,
                        points[i][1] + (points[i+1][1]-points[i][1])*t))
    pts.append(points[-1])
    n, drift, out = len(pts), [0.0, 0.0], []
    for i, (x, y) in enumerate(pts):
        drift[0] = drift[0]*0.86 + rng.gauss(0, sev*0.16)
        drift[1] = drift[1]*0.86 + rng.gauss(0, sev*0.16)
        nx, ny = pts[(i + 1) % n]
        k = 0.06*sev/max(1.0, math.hypot(nx-x, ny-y))
        x, y = (x + (nx-x)*k)*scale, (y + (ny-y)*k)*scale
        c, s = math.cos(rot), math.sin(rot)
        out.append((x*c - y*s + drift[0], x*s + y*c + drift[1]))
    return out[:max(4, int(len(out)*tail))]


def evaluate(shapes, trials=TRIALS, sev=SEV, seed=17):
    rng = random.Random(seed)
    keys = list(shapes)
    norms = {k: G.normalize(shapes[k]) for k in keys}
    draws = {k: [hand(shapes[k], sev, rng, tail=rng.uniform(.8, .99),
                      rot=rng.uniform(-.45, .45), scale=rng.uniform(.5, 1.7))
                 for _ in range(trials)] for k in keys}
    norms_drawn = {k: [G.normalize(d) for d in draws[k]] for k in keys}
    return keys, norms, norms_drawn


def run(shapes, label):
    keys, norms, norms_drawn = evaluate(shapes)
    print('== %s ==' % label)
    worst = 1.0
    for basket, slots in BASKETS.items():
        ok = ref = wrong = 0
        for sd in slots:
            for i in range(TRIALS):
                nd = norms_drawn[sd][i]
                ranked = []
                for st in slots:
                    ranked.append((1.0 - G.best_angle_distance(nd, norms[st], G.is_open(nd))/G.HALF_DIAG, st))
                ranked.sort(reverse=True)
                top, tid = ranked[0]
                second = ranked[1][0] if len(ranked) > 1 else -9.0
                if top < G.MIN_SCORE or top - second < MARGIN:
                    ref += 1
                elif tid == sd:
                    ok += 1
                else:
                    wrong += 1
        n = len(slots)*TRIALS
        print('   %-16s trafienia %5.1f%% | odmowy %5.1f%% | ZLY CZAR %5.1f%%'
              % (basket, 100*ok/n, 100*ref/n, 100*wrong/n))
        worst = min(worst, ok/n)
    print('   najgorszy koszyk: %.1f%%' % (100*worst))
    return worst


def sync_from_java():
    """Wyciagn List.of(new Point(x, y), ...) z GestureTemplates.java i porownaj."""
    src = ROOT.read_text(encoding='utf8')
    out = {}
    for m in re.finditer(r'public static List<Point> (\w+)\(\) \{\n(.*?)\n    \}', src, re.S):
        name, block = m.group(1), m.group(2)
        pts = [(float(a), float(b)) for a, b in re.findall(r'new Point\((-?[\d.]+),\s*(-?[\d.]+)\)', block)]
        if len(pts) >= 2:
            out[name] = pts
    return out


if __name__ == '__main__':
    if '--sync' in sys.argv:
        java = sync_from_java()
        print('znalezione w Java: %s' % sorted(java))
        for name, pts in java.items():
            key = name.replace('Stroke', '')
            if key in SHAPES:
                same = len(pts) == len(SHAPES[key]) and all(abs(a[0]-b[0]) < .01 and abs(a[1]-b[1]) < .01
                                                             for a, b in zip(pts, SHAPES[key]))
                print('  %-14s %s' % (key, 'zgodny' if same else 'INNY - aktualizuj SHAPES'))
    run(SHAPES, 'zestaw z repo')
