#!/usr/bin/env bash
# Wrzuca swiezo zbudowany mod do katalogu `mods/` wskazanej instancji.
# Znajduje instancje sam (PrismLauncher, ewentualnie ~/.minecraft), sprawdza,
# czy to NAPRAWDE mod (musi byc fabric.mod.json + assets/wandzz), sprzata
# pliki, ktore nie sa modami, i ostrzega, gdy brakuje Fabric API.
#
#   ./gradlew build && ./tools/install_mod.sh
#   ./tools/install_mod.sh -i '~/.local/share/PrismLauncher/instances/1.21.11(1)'
#   ./tools/install_mod.sh --dry-run
#   MC=1.21.11 ./tools/install_mod.sh          # inna wersja do wyszukania
set -euo pipefail

MOD=wandzz
MC="${MC:-1.21.11}"
CERTA=""
SUCHO=0
ROOTS=("$HOME/.local/share/PrismLauncher"
        "$HOME/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher"
        "$HOME/.minecraft")

while [ $# -gt 0 ]; do
    case "$1" in
        -i|--instance) CERTA="$2"; shift 2 ;;
        --dry-run|-n)  SUCHO=1; shift ;;
        -h|--help)     sed -n '2,12p' "$0"; exit 0 ;;
        *) echo "nie znam argumentu: $1" >&2; exit 2 ;;
    esac
done

BLIB="$(cd "$(dirname "$0")/.." && pwd)/build/libs"
JAR=""
# najnowszy sensowny jar (bez zrodel/dev/javadoc); find+read, zeby spacje w
# sciezce nie rozwalaly petli
while IFS= read -r kandydat; do
    case "$kandydat" in
        *-sources.jar|*-dev.jar|*javadoc*) continue ;;
    esac
    JAR="$kandydat"; break
done < <(find "$BLIB" -maxdepth 1 -name "$MOD-*.jar" -printf '%T@\t%p\n' 2>/dev/null | sort -rn | cut -f2-)
if [ -z "$JAR" ]; then
    echo "BRAK JARA w $BLIB - wpierw: ./gradlew build" >&2
    exit 1
fi
if ! unzip -l "$JAR" 2>/dev/null | grep -q '^.*fabric\.mod\.json$'; then
    echo "ODRZUCONE: w $JAR nie ma fabric.mod.json, to nie jest mod." >&2
    exit 1
fi

# --- cel: katalog z mods/ ---------------------------------------------------
CELE=()
if [ -n "$CERTA" ]; then
    CERTA="${CERTA/#\~/$HOME}"
    [ -d "$CERTA" ] || { echo "podana instancja nie istnieje: $CERTA" >&2; exit 1; }
    CELE+=("$CERTA")
else
    for root in "${ROOTS[@]}"; do
        [ -d "$root/instances" ] || continue
        while IFS= read -r inst; do CELE+=("$inst"); done \
            < <(find "$root/instances" -maxdepth 1 -mindepth 1 -type d -name "*$MC*" | sort)
    done
    if [ ${#CELE[@]} -eq 0 ]; then
        for root in "${ROOTS[@]}"; do
            [ -d "$root/versions" ] && CELE+=("$root")     # ~/.minecraft - jeden wspolny mods/
        done
    fi
fi
if [ ${#CELE[@]} -eq 0 ]; then
    echo "Nie znalazlem instancji dla MC $MC. Podaj reka:" >&2
    echo "  ./tools/install_mod.sh -i /sciezka/do/instancji" >&2
    echo "albo wskaz katalog launcher'a:  ls ~/.local/share/PrismLauncher/instances" >&2
    exit 1
fi
if [ ${#CELE[@]} -gt 1 ]; then
    echo "Kandydaci (wybierz jednego przez -i):" >&2
    printf '  %s\n' "${CELE[@]}" >&2
    exit 1
fi

INST="${CELE[0]}"
MODS="$INST/mods"
if [ ! -d "$INST" ]; then echo "brak katalogu instancji: $INST" >&2; exit 1; fi
echo "instancja : $INST"
echo "jar       : $JAR  ($(stat -c %s "$JAR") B)"
echo "mods      : $MODS"

# --- sprzatanka: pliki, ktore udaja mody ------------------------------------
SZMERY=0
for smiec in "$MODS"/*-sources.jar "$MODS"/*-dev.jar "$MODS"/*SNAPSHOT*.jar "$MODS"/*javadoc*.jar; do
    [ -e "$smiec" ] || continue
    echo "  usun (to nie mod): $(basename "$smiec")"
    SZMERY=$((SZMERY + 1))
done

if [ "$SUCHO" -eq 1 ]; then
    echo "--dry-run: nic nie zapisane."
    exit 0
fi
mkdir -p "$MODS"
for smiec in "$MODS"/*-sources.jar "$MODS"/*-dev.jar "$MODS"/*SNAPSHOT*.jar "$MODS"/*javadoc*.jar; do
    [ -e "$smiec" ] && rm -f "$smiec"
done
for stary in "$MODS"/$MOD-*.jar; do
    [ -e "$stary" ] && [ "$stary" != "$MODS/$(basename "$JAR")" ] && rm -f "$stary"
done
cp "$JAR" "$MODS/"
echo "  zainstalowano: $(basename "$JAR")"

if ! ls "$MODS"/fabric-api-*.jar >/dev/null 2>&1; then
    echo "UWAGA: w mods/ nie ma fabric-api-*.jar - bez niego Wandzz nie wystartuje" >&2
    echo "       (potrzeba wydania z +$MC, np. fabric-api-0.141.6+$MC.jar)." >&2
fi
if [ "$SZMERY" -gt 0 ]; then echo "usunieto plikow-niemodow: $SZMERY"; fi
echo "zawartosc mods/:"
ls -1 "$MODS"
