# Wandzz

Mod do Minecrafta (Fabric, 1.21.11) implementujący magię opartą na gestach
rysowanych myszką, zgodnie z dokumentem projektowym:

```
Mysz -> MouseInput -> CastingData (List<Point>) -> $1 Recognizer
     -> Spell -> Core'y + Mana -> CAST
```

## Co jest gotowe

- **`gesture/Point`, `gesture/CastingData`** – zbieranie ścieżki myszy podczas rysowania.
- **`gesture/DollarOneRecognizer`** – własna implementacja algorytmu **$1 Unistroke
  Recognizer** (Wobbrock/Wilson/Li 2007): resample → rotate → scale+translate →
  golden-section search po kącie obrotu. Napisana od zera na podstawie opisu
  algorytmu, nie skopiowana z żadnego repozytorium.
- **`spell/`** – interfejs `Spell`, rejestr `SpellRegistry`, oraz 7 przykładowych
  zaklęć: `StrikeSpell`, `BreakBlockSpell`, `TorchSpell` (Feather Core, lvl 1),
  `FireballSpell` (lvl 2), `TeleportSpell`, `BombSpell`, `DragonBreathSpell`
  (lvl 3 – dostępne dla **każdego** core'a lvl 3+, nie tylko Dragon Breath Core,
  zgodnie z dokumentem).
- **`core/CoreType`** – 15 typów core'ów z poziomami. W pełni opisane w dokumencie
  (`FEATHER` lvl 1, `DRAGON_BREATH` lvl 3) są gotowe; pozostałe 13 to sloty do
  dalszej rozbudowy (nazwy/poziomy do dopracowania razem z Tobą).
- **`wand/WandMaterial`** – 6 rodzajów drewna z liczbą slotów na core'y dokładnie
  wg tabeli z dokumentu (0/0/1/2/3/5 dodatkowych slotów).
- **`wand/WandData`** – dane rozdzki (materiał + zainstalowane core'y) jako
  data component na ItemStacku.
- **`mana/`** – mana jako Fabric Data Attachment, z regeneracją zależną od core'a.
- **`network/`** – klient rozpoznaje gest lokalnie (płynność), ale **serwer**
  ostatecznie weryfikuje, czy rozdzka ma wymagany core i czy starcza many,
  zanim zaklęcie faktycznie zadziała (`CastingHandler`).
- **`client/CastingScreen`** – ekran otwierany na czas trzymania PPM z rozdzką,
  zbierający pozycje myszy i rysujący ślad gestu na ekranie.

## Czego brakuje / co warto dopracować dalej

- Pozostałe 13 core'ów ma tylko nazwę i poziom – potrzebują własnych zaklęć
  i efektów (analogicznie do Feather/Dragon Breath).
- Modele/tekstury itemów (`assets/wandzz/models`, `textures`) – w projekcie są
  tylko puste katalogi, trzeba dodać własne tekstury i pliki modeli.
- GUI do wkładania core'ów w sloty rozdzki (obecnie `WandItem.insertCore` to
  gotowa metoda, ale brak ekranu/przepisu craftingowego, który by z niej korzystał).
- HUD z paskiem many.
- Recipe (crafting) dla rozdzek i core'ów – brak plików `data/wandzz/recipe`.

## Budowanie

Projekt **nie był kompilowany w tym środowisku** – sandbox, w którym go
napisano, nie ma dostępu do repozytoriów Fabric/Mojang (maven.fabricmc.net,
libraries.minecraft.net), więc nie dało się pobrać zależności i odpalić
build Gradle.

**Ważne:** od Minecrafta 1.21.11 Fabric przestał wspierać Yarn jako mapowania
domyślne – projekt używa **oficjalnych mapowań Mojanga**
(`mappings loom.officialMojangMappings()`), więc kod korzysta z nazw typu
`Player`, `ServerLevel`, `Level`, `Component`, `ResourceLocation` itd.
zamiast Yarn-owych `PlayerEntity`, `ServerWorld`, `World`, `Text`, `Identifier`.

Żeby zbudować mod:

1. Zainstaluj JDK 21.
2. Otwórz folder w IntelliJ IDEA (File → Open → wskaż `build.gradle`) i poczekaj,
   aż Gradle pobierze zależności (wymaga internetu) – kliknij ikonę odświeżenia
   w panelu Gradle po prawej, jeśli import nie ruszy automatycznie.
3. W `gradle.properties` sprawdź, czy `loader_version`/`fabric_version` wciąż
   są aktualne (https://fabricmc.net/develop/) – Fabric wydaje nowe buildy
   dość często.
4. Uruchom task Gradle `runClient` (Gradle panel → wandzz → Tasks → fabric →
   runClient) do testów.

Ponieważ kod pisany był bez możliwości kompilacji, **niektóre nazwy metod
(np. dokładna sygnatura `Entity#pick`, `Player#teleportTo`, metody Fabric
Attachment API) mogą się nieznacznie różnić** względem dokładnej wersji
1.21.11 – IDE z podpowiadaniem (Ctrl+klik na klasę, Alt+Enter na błędzie)
szybko to pokaże i poprawi. To zwykle kwestia drobnej zmiany nazwy metody,
nie architektury.

## Struktura

```
src/main/java/com/wandzz/
  gesture/   – Point, CastingData, $1 Recognizer, wzorce gestów
  spell/     – Spell, SpellRegistry, implementacje zaklęć
  core/      – CoreType (15 typów), WandCoreItem
  wand/      – WandMaterial, WandData, WandItem
  mana/      – ManaComponent, ManaAttachments
  network/   – CastPayload, CastingHandler (serwerowa weryfikacja + CAST)
  item/      – ModItems, ModComponents
  Wandzz.java – ModInitializer
src/client/java/com/wandzz/client/
  CastingScreen.java – zbieranie gestu z ekranu
  WandzzClient.java  – ClientModInitializer
```
