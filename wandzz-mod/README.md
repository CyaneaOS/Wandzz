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
- **`wand/WandData`** – dane różdżki (materiał + zainstalowane core'y) jako
  data component na ItemStacku.
- **`mana/`** – mana jako Fabric Data Attachment, z regeneracją zależną od core'a.
- **`network/`** – klient rozpoznaje gest lokalnie (płynność), ale **serwer**
  ostatecznie weryfikuje, czy różdżka ma wymagany core i czy starcza many,
  zanim zaklęcie faktycznie zadziała (`CastingHandler`).
- **`client/CastingScreen`** – ekran otwierany na czas trzymania PPM z różdżką,
  zbierający pozycje myszy i rysujący ślad gestu na ekranie.

## Budowanie

Wymagania: **JDK 21** i internet (Gradle ściąga Loom, Fabric Loader/API oraz
artefakty Mojang). Wrapper Gradle jest w repo (`gradle/wrapper` → Gradle 9.5.1),
więc nie trzeba instalować Gradle'a osobno.

```bash
cd wandzz-mod
./gradlew build        # -> build/libs/wandzz-0.1.0.jar
./gradlew runClient    # testy w grze (klient + wbudowany serwer deweloperski)
```

Na Windows: `gradlew.bat build`.

Wersje narzędzi (`gradle.properties`) są zgodne z oficjalnym
`fabric-example-mod` w gałęzi `1.21.11`: Loom `1.17.20`, Loader `0.19.3`,
Fabric API `0.141.6+1.21.11`. Przed przenoszeniem na nowszy MC sprawdź
<https://fabricmc.net/develop/>.

## Co zostało naprawione (błąd `compileJava FAILED`)

Kod był napisany pod starsze nazwy Mojmapa. W Minecraft **1.21.11** (ostatniej
wersji zamaskowanej, po której gra jest już nieobfuscowana) Mojang
przemianował/przeniósł sporo klas, a Fabric wygasił Yarn — projekt używa
`mappings loom.officialMojangMappings()`, więc wszystkie nazwy musiały zostać
dopasowane:

| Problem | Fix w 1.21.11 |
|---|---|
| `cannot find symbol: ResourceLocation` | klasa nazywa się `net.minecraft.resources.Identifier` (renamed w 1.21.11); `fromNamespaceAndPath` zostaje |
| `cannot find symbol: SmallFireball`, `DragonFireball` | `net.minecraft.world.entity.projectile` rozpadł się na podpakiety → `...projectile.hurtingprojectile.*` |
| `UseItemCallback#interact` zwracał `InteractionResultHolder` | zwraca `InteractionResult` (`PASS` / `SUCCESS`) |
| `Screen#mouseReleased(double,double,int)` | input przeszedł na obiekty zdarzeń: `mouseReleased(MouseButtonEvent)`, `mouseClicked(MouseButtonEvent, boolean)`; `mouseMoved(double,double)` jest `void` |
| `RenderSystem.enableBlend()/disableBlend()` | usunięte przez rewrite renderowania (`RenderTypes`/`GpuDevice`) — ślad gestu rysowany jest bez blendu |
| `Item#appendHoverText(ItemStack, TooltipContext, List, TooltipFlag)` | sygnatura to teraz 5 argumentów z `TooltipDisplay` i `Consumer`, metoda jest `@Deprecated` → zwykła linia tooltipa idzie przez komponent `DataComponents.LORE` (`ItemLore`) |
| `AttachmentRegistry.builder()` | oznaczone `@Deprecated` → `AttachmentRegistry.create(id, builder -> ...)` |
| przedmiotów nie dało się wziąć w kreatywie | dodana zakładka `Wandzz` (`ModItemGroups`, `FabricItemGroup.builder()` + rejestracja w `BuiltInRegistries.CREATIVE_MODE_TAB`, `title` z klucza `itemGroup.wandzz.wandzz`) |
| crash przy starcie: `NullPointerException: Item id not set` | od 1.21.2 sam `Item.Properties` musi znać swój `ResourceKey<Item>` (na jego podstawie liczone jest `descriptionId` i `ITEM_MODEL`), więc `properties.setId(key)` trzeba wywołać **przed** konstruktorem przedmiotu — teraz robi to helper w `ModItems` (dokładnie jak vanilla `Items#registerItem`) |
| pakiet `wandzz:cast` niezarejestrowany | Fabric wymaga `PayloadTypeRegistry.playC2S().register(...)` **przed** `registerGlobalReceiver`, po obu stronach — rejestracja przeniesiona do common entrypointu; usunięty błędny odbiornik klienta dla pakietu C2S (run-time `IllegalArgumentException`) |
| `Entity#hurtServer` | `hurtServer` jest na `LivingEntity` → wzorzec `instanceof LivingEntity` zamiast rzutowania na `Entity` |
| rzutowanie `(ServerLevel) player.level()` | `instanceof ServerLevel` (bez ryzyka `ClassCastException`) |
| `server().execute(...)` w handlerze | zbędne — `PlayPayloadHandler` jest już wywoływany na wątku serwera |
| brak wrappera w repo | dodane `gradlew`, `gradlew.bat`, `gradle/wrapper/*` (Gradle 9.5.1), `.gitattributes`, `.gitignore`, `LICENSE` |
| `loom_version=1.17-SNAPSHOT` | przypięte do `1.17.20` (ten sam plugin, konkretny build zamiast ruchomego snapshotu) |
| `id 'fabric-loom' version "${project.loom_version}"` | **Gradle 9** (wrapper 9.5.1) odrzuca `project.` w bloku `plugins {}` → `version "${loom_version}"`, inaczej startup fail: „argument list must be exactly 1 literal String or String with property replacement" |
| $1 `scaleToSquare` skalował X i Y **osobno** | niejednorodne skalowanie rozciągało drżenie myszy (np. 3 px w Y) do pełnego kwadratu 250 – szum był większy niż kształt, więc gesty nie przechodziły progu. Teraz unitarne skalowanie przez dłuższy bbox, jak w oryginalnym $1 |
| Golden Section Search w rozpoznawaniu | GSS zakłada unimodalność kosztu; trójkąt i inne figury symetryczne mają po 3 minima i wyszukiwanie grzęzło (trójkąt: 0.56 własnego vs 0.61 obcego → odrzucony). Zamienione na przeszukiwanie siatkowe (±45° co 6° dla kresek, ±180° co 15° dla figur zamkniętych) + doszlifowanie co 1° |
| gest `strike` = pozioma kreska | $1 jest odporny na obrót, więc „kreska" i „kreska z hakiem" (torch) to po normalizacji ten sam kształt – czary myliły się między sobą. `strike` to teraz czkawka (V) |
| cast odrzucany po cichu (`return` bez komunikatu) | każdy przypadek odmowy (brak różdżki / brak rdzenia / za mało many / nierozpoznany gest) dostaje teraz action bar; poza tym testowany zbiór wzorców daje 100% trafień przy szumie 6 px i obrocie ±63° |
| `build.gradle` bez `publishing`/`jar`/`encoding` | uzupełnione wg oficjalnego template'u + `options.encoding = "UTF-8"` |

Wszystkie użyte nazwy klas i metod zostały sprawdzone bezpośrednio na źródłach
Minecraft 1.21.11 z oficjalnymi mapowaniami Mojanga oraz na źródłach
`FabricMC/fabric` w gałęzi `1.21.11` (networking + data attachment API).
Samo budowanie nie jest tu możliwe (sandbox ma zablokowany `maven.fabricmc.net`
i `services.gradle.org`), więc weryfikacja idzie w pętli z maszyną developera:
`./gradlew build` oraz `./gradlew runClient` przechodzą obecnie na 1.21.11
(loader 0.19.3, fabric-api 0.141.6+1.21.11, JDK 21). Składnia wszystkich
plików Javy jest dodatkowo sprawdzana parserem, a wszystkie JSONy są parsowane.

Dodatkowo w `lang/en_us.json` i `lang/pl_pl.json` dopisano nazwy wszystkich
15 core'ów oraz klucz `wandzz.core.level` (używany przez tooltip rdzeni).

## Jak to działa w grze

### Drewno „arkany" (bez generowania drzew)

| id | co to | jak zdobyć |
|---|---|---|
| `wandzz:arcane_log` | blok kłody (`RotatedPillarBlock`, oś X/Y/Z) | tylko crafting / kreatywa — **brak saplingu i `worldgen`**, świadomie |
| `wandzz:arcane_planks` | blok desek | 1× `arcane_log` → 4 deski (shape' `["#"]`, jak w vanilla) |
| `wandzz:arcane_stick` | przedmiot (patyk z tego drewna) | 2× `arcane_planks` w słupku → 4 patyki |

Deski i kłoda są dopisane do vanilla tagów `#minecraft:planks`, `#minecraft:logs`
i `#minecraft:mineable/axe` (przez `data/minecraft/tags/item/...`, bez `replace`),
więc siekiera je kopie, a vanilla przepisy je akceptują.

### Rozdżki — im lepsze drewno, tym więcej rdzeni

| wynik | slots | przepis |
|---|---|---|
| `wand_normal` | 1 | 2× `#minecraft:planks` + `minecraft:stick` po przekątnej |
| `wand_custom` | 2 | **3× `wandzz:arcane_stick` na skos** — `["  A", " A ", "A  "]` |
| `wand_rare` | 4 | 3× `arcane_stick` na skos + `minecraft:echo_shard` w rogu |
| `wand_*_magic` | 1 / 3 / 6 | shapeless: różdżka bazowa + 2× `glowstone_dust` |

Uwaga: magiczny upgrade tworzy nową różdżkę, więc **rdzeni się nie przenoszą**
(wynik przepisu data-driven nie widzi komponentów bazy — tylko własny przepis
w kodzie może je skopiować, patrz niżej).

### Rdzenie: wkładanie w stole kowalskim

`data/wandzz/recipe/wand_core_smithing.json` + `WandCoreSmithingRecipe`
(własny `RecipeSerializer` `wandzz:wand_core_smithing`):

```
slot szablonu : pusty
slot base     : dowolna różdżka   (#wandzz:wands)
slot addition : dowolny rdzeń     (#wandzz:cores)
wynik         : ta sama różdżka + jeden rdzeń w wolnym slocie
```

Dlaczego własna klasa przepisu, a nie `minecraft:smithing_transform`:

- wynik zależy od tego, co **już jest** w różdżce — data-driven `result` jest
  statyczne i przy każdej rozbudowie skasowałoby wcześniejsze rdzenie;
- `SmithingRecipe#templateIngredient()` zwraca `Optional`, więc pusty szablon
  jest w pełni legalny (match idzie przez `Ingredient#testOptionalIngredient`) —
  nie trzeba wymyślać przedmiotu-szablonu na siłę;
- `RecipeType` bierze się z `default RecipeType getType()` w `SmithingRecipe`,
  więc menu smithingu znajduje ten przepis mimo obcego `type` w JSON.

Wolny slot jest warunkiem `matches`, więc przy pełnej różdżce okno wyniku po
prostu zostaje puste (bez „cichego" braku efektu).

**Wyjmowanie**: PPM rdzeniem, gdy różdżka jest w którejś ręce (`WandInteractions`) —
rdzeń wraca do ekwipunku, a w kreatywie nic się nie zużywa.

### Rzucanie

PPM z różdżką → rysujesz gest → puszczenie PPM = wysyłka na serwer. Każde
odrzucone rzucenie mówi dlaczego (brak różdżki / różdżka bez rdzenia / rdzeń za
niskiego poziomu / za mało many / gest nierozpoznany).

## Czego brakuje / co warto dopracować dalej

- Pozostałe 13 core'ów ma tylko nazwę i poziom – potrzebują własnych zaklęć
  i efektów (analogicznie do Feather/Dragon Breath).
- Drzewo „arkany" nie generuje się w świecie: brak `arcane_sapling`, brak
  `worldgen_configured_feature` / `placed_feature` / `biome_modifier` i brak
  logów w liściach. Drewno jest na razie tylko z craftingu (patrz wyżej).
- Modele `arcane_log` / `arcane_planks` dziedziczą tekstury dębu
  (`minecraft:block/oak_*`) — własne PNG w `assets/wandzz/textures/block/`.
- Modele itemów są już na miejscu (`assets/wandzz/items/*.json` jako definicje
  klienta 1.21.4+ plus `assets/wandzz/models/item/*.json`), ale to **placeholdery**
  na teksturach vanilla: różdżki renderują się jako patyk / `warped_fungus_on_a_stick`
  / `blaze_rod`, a core'y jako pasujące vanillowe przedmioty (feather, dragon_breath,
  echo_shard itd.). Własne tekstury: `assets/wandzz/textures/item/*` i podmiana
  `layer0` w modelach.
- HUD z paskiem many (sama mana jest, ale w action barze widać tylko jej brak).
- Skrót klawiszowy `key.wandzz.cast` ma już wpisy w `lang`, ale sam keybind
  nie jest zarejestrowany (gest uruchamiany jest PPM z różdżką w ręce).

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
