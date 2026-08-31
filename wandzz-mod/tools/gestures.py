"""
Port $1 z `gesture/DollarOneRecognizer.java` - SLUZY DO POMIARU, nie do gry.

Mus byc WIERNY (te same stale, te same uproszczenia, te same zakresy katan):
jesli tu sie roznia, testujemy inny algorytm niz ten, ktory dziala w grze, i
liczby sa bez wartosci.

    python3 tools/gesture_set.py        # ocena zestawu z GestureTemplates.java
    python3 tools/gesture_set.py --sync # wyciagnij ksztalty z Java i przelicz

Uwaga o --sync: listy punktow sa parsowane z Jeavy, wiec kazdy ksztalt musi byc
zapisany jako List.of(new Point(x, y), ...) - generatory (kola, fale, luki) sa
tu odtworzone jako funkcje, nie jako dane.
"""
import math, random

RESAMPLE = 64
SQUARE = 250.0
RANGE_OPEN, STEP_OPEN = 45.0, 6.0
RANGE_CLOSED, STEP_CLOSED = 180.0, 15.0
MIN_SCORE = 0.72
AMBIGUITY_MARGIN = 0.035   # DollarOneRecognizer.AMBIGUITY_MARGIN
HALF_DIAG = 0.5 * math.sqrt(SQUARE * SQUARE + SQUARE * SQUARE)

def dist(a, b): return math.hypot(a[0] - b[0], a[1] - b[1])

def path_len(pts): return sum(dist(pts[i-1], pts[i]) for i in range(1, len(pts)))

def centroid(pts):
    return (sum(p[0] for p in pts)/len(pts), sum(p[1] for p in pts)/len(pts))

def resample(points, n=RESAMPLE):
    total = path_len(points)
    interval = total / (n - 1)
    if interval <= 0:
        return [points[0]] * n
    src = list(points); acc = 0.0; out = [src[0]]; i = 1
    while i < len(src):
        prev, curr = src[i-1], src[i]
        d = dist(prev, curr)
        if acc + d >= interval:
            t = (interval - acc) / d if d else 0.0
            q = (prev[0] + t*(curr[0]-prev[0]), prev[1] + t*(curr[1]-prev[1]))
            out.append(q); src.insert(i, q); acc = 0.0
        else:
            acc += d
        i += 1
    while len(out) < n: out.append(src[-1])
    return out[:n]

def rotate_by(pts, rad, center=None):
    c = center or centroid(pts)
    cos, sin = math.cos(rad), math.sin(rad)
    return [( (p[0]-c[0])*cos - (p[1]-c[1])*sin + c[0], (p[0]-c[0])*sin + (p[1]-c[1])*cos + c[1]) for p in pts]

def indicative_angle(pts):
    c = centroid(pts); f = pts[0]
    return math.atan2(f[1]-c[1], f[0]-c[0])

def scale_to_square(pts, size=SQUARE):
    xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
    mnx, mxx, mny, mxy = min(xs), max(xs), min(ys), max(ys)
    s = max(mxx-mnx, mxy-mny) or 1.0
    return [((p[0]-mnx)*(size/s), (p[1]-mny)*(size/s)) for p in pts]

def translate_origin(pts):
    c = centroid(pts)
    return [(p[0]-c[0], p[1]-c[1]) for p in pts]

def normalize(raw):
    p = resample(raw)
    p = rotate_by(p, -indicative_angle(p))
    p = scale_to_square(p)
    return translate_origin(p)

def is_open(pts):
    dx = pts[0][0]-pts[-1][0]; dy = pts[0][1]-pts[-1][1]
    mean = sum(dist(pts[i-1], pts[i]) for i in range(1, len(pts))) / (len(pts)-1)
    return math.hypot(dx, dy) > 2.0*mean

def distance_at_angle(a, b, rad):
    r = rotate_by(a, rad)
    n = min(len(r), len(b))
    return sum(dist(r[i], b[i]) for i in range(n))/n

def best_angle_distance(points, template, open_shape):
    rng, step = (RANGE_OPEN, STEP_OPEN) if open_shape else (RANGE_CLOSED, STEP_CLOSED)
    best_d, best_a = float('inf'), 0.0
    deg = -rng
    while deg <= rng:
        d = distance_at_angle(points, template, math.radians(deg))
        if d < best_d: best_d, best_a = d, deg
        deg += step
    for i in range(-4, 5):
        d = distance_at_angle(points, template, math.radians(best_a+i))
        if d < best_d: best_d = d
    return best_d

def score(raw, template_norm):
    cand = normalize(raw)
    return 1.0 - best_angle_distance(cand, template_norm, is_open(cand))/HALF_DIAG

def smooth_draw(points, jitter, n=140, rot=0.0, scale=1.0, tail=1.0):
    """Symulacja ruchu mysza: gest po sciezce wzorca + szum + rotacja/skala."""
    dens = []
    for i in range(len(points)-1):
        for t in [k/12.0 for k in range(12)]:
            dens.append((points[i][0]+(points[i+1][0]-points[i][0])*t,
                         points[i][1]+(points[i+1][1]-points[i][1])*t))
    dens.append(points[-1])
    cut = max(3, int(len(dens)*tail))
    dens = dens[:cut]
    c = centroid(dens)
    out = []
    for p in dens:
        x, y = (p[0]-c[0])*scale, (p[1]-c[1])*scale
        x, y = x*math.cos(rot)-y*math.sin(rot), x*math.sin(rot)+y*math.cos(rot)
        out.append((x + random.gauss(0, jitter), y + random.gauss(0, jitter)))
    return out
