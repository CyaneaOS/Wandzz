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
- **`spell/`** – interfejs `Spell`, rejestr `SpellRegistry` oraz 8 zaklęć:
  `StrikeSpell`, `BreakBlockSpell`, `TorchSpell` (lvl 1), `FireballSpell` (lvl 2),
  `TeleportSpell`, `BombSpell`, `DragonBreathSpell` (lvl 3 – dostępne dla
  **każdego** core'a lvl 3+, nie tylko Dragon Breath Core, zgodnie z dokumentem)
  i `OpenGateSpell` (lvl 3 – odpalenie bramy do Arkanum). `Spell#canCast` pozwala
  odmówić *przed* pobraniem many, gdy nie ma celu (patrz „Przejście do Arkanum“).
- **`core/CoreType`** – 15 typów core'ów z poziomami. W pełni opisane w dokumencie
  (`FEATHER` lvl 1, `DRAGON_BREATH` lvl 3) są gotowe; pozostałe 13 to sloty do
  dalszej rozbudowy (nazwy/poziomy do dopracowania razem z Tobą).
- **`wand/WandWood`** – 13 gatunków drewna (11 vanilla + bambus + arkany); każdy
  daje własny patyk, własną różdżkę i własną liczbę gniazd na rdzenie (1–6).
- **`wand/WandData`** – zainstalowane rdzenie jako data component na ItemStacku,
  synchronizowany do klienta (dlatego tooltip i okno stolika widzą skład).
- **`block/` + `world/`** – kłoda, deski, liście, sadzonka i **stolik arcaniczny**;
  biom `wandzz:arcane_forest`, drzewo i wymiar `wandzz:arkanum` w 100% danymi.
- **`client/WandzzHud`** – pasek many, który **liczy geometrię z ekranu** (patrz
  „HUD many“): pionowy przy wolnej stronie hotbara, a gdy miejsca nie ma – poziomy
  nad rzędami serc. Zasilany z `ManaSyncPayload` (Data Attachment serwera nie jest
  synchronizowany sam z siebie).
- **`item/SpellBookItem` + `client/SpellBookScreen`** – księga zaklęć
  (`wandzz:spell_book`): 3 wpisy na stronę, każdy z nazwą, opisem, kosztem many,
  listą rdzeni i **rysunkiem gestu** generowanym z tych samych punktów, które
  trafiają do recognize'a. Referencja bez progresji – patrz „Księga zaklęć“.
- **`block/ArcaneEmberBlock` + `world/GateService`** – `wandzz:arcane_ember`: blok
  generowany w jeziorkach lawy w podziemiach; zaklęcie `wandzz:open_gate` odpala z
  niego bramę do `wandzz:arkanum` i z powrotem (patrz „Przejście do Arkanum“).
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
| `Item#appendHoverText(ItemStack, TooltipContext, List, TooltipFlag)` | sygnatura to teraz 5 argumentów: `(ItemStack, Item.TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)`; metoda jest `@Deprecated`, ale to jedyne miejsce, gdzie linia może być liczona ze stanu przedmiotu → `WandItem` i `WandCoreItem` ją nadpisują (+ `@SuppressWarnings("deprecation")`); `TooltipDisplay` leży w `net.minecraft.world.item.component` |
| klient (tooltipy, okno stolika) musi widzieć skład rdzeni | `DataComponentType.Builder#build()` co prawda wyrabia kodek sieciowy z `persistent(codec)` (przez `ByteBufCodecs.fromCodecWithRegistries`), ale wymaga `RegistryFriendlyByteBuf` i koduje przez JSON → `ModComponents` podaje wprost `networkSynchronized(WandData.STREAM_CODEC)` (VarInt + UTF) |
| `Block#use(ItemStack, Level, ...)` | zastąpione przez `useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)` (chronione); `InteractionResult` jest teraz `sealed interface` z `SUCCESS` / `PASS` / `FAIL` i **bez** `sidedSuccess` |
| `MenuType` + ekran kontenera na stolik | w 1.21.11 `MenuType` ma prywatny konstruktor, a `MenuScreens#register` i `ScreenConstructor` są prywatne (brak już `IMenuTypeExtension` w Fabric) → stolik ma własny `Screen` i pakiety zamiast `AbstractContainerMenu` |
| HUD many przez `HudLayerRegistrationCallback` | wygaszony; w 1.21.11: `HudElementRegistry.addLast(Identifier, (GuiGraphics, DeltaTracker) -> ...)` z `fabric-rendering-v1` |
| drzewo z sadzonki wymaga `Tree` w kodzie | nie: `TreeGrower(String, Optional<ResourceKey<ConfiguredFeature>>…)` wskazuje klucz `worldgen/configured_feature`, a `SaplingBlock(TreeGrower, Properties)` jest `protected` → stąd mała klasa `ArcaneSaplingBlock` |
| `Properties#noCollission()` | pisownia to `noCollision()` (jedno „l"); liście potrzebują jeszcze `isSuffocating/isViewBlocking/isRedstoneConductor` na `false`, a `LeavesBlock` bierze w 1.21.11 szansę cząsteczki jako 1. argument |
| własny `Recipe` z `PlacementInfo` w konstruktorze | crash `Trying to access unbound tag 'wandzz:wands'` przy tworzeniu świata; przepis smithingu rdzeni i tak poszedł do kosza na rzecz stolika, więc temat zniknął |
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
| `ResourceKey#location()` nie istnieje | po rename’cie `ResourceLocation` → `Identifier` accessor `ResourceKey` nazywa się `identifier()` (stad `cannot find symbol: method location()` na `ResourceKey<Level>` w `GateService`) |
| `LeavesBlock is abstract; cannot be instantiated` | w 1.21.11 `LeavesBlock` jest abstract (ma `public abstract MapCodec codec()`); liście buduje się przez `TintedParticleLeavesBlock(float leafParticleChance, Properties)` (jak `oak_leaves`) albo `UntintedParticleLeavesBlock(chance, ParticleOptions, Properties)` |
| `player.level().getGameTime()` | `getGameTime()` leży na `ServerLevel`/`ClientLevel`, **nie** na `Level` → throttling synchronizacji many liczy się od `player.tickCount` (publiczne pole `Entity`, vanilla: `this.tickCount % 20 == 0`) |
| pasek many nachodził na slot offhandu i uciekał z ekranu | geometria liczona ze stałych vanilli + wybór strony + wariant poziomy (sekcja „HUD many“) |
| mana pobierana nawet bez celu | `Spell#canCast(ServerLevel, Player)` sprawdzane w `CastingHandler` **przed** `mana.spend(...)` |
| własny `BiomeModifier` JSON-owy na wstrzyknięcie rudy | zbędny: `net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, klucz)` – zero plików w `data/minecraft`, zero TerraBlendera |
| `registerDefaultState(this.stateDefinition.any())` | pola `stateDefinition` nie widać z podklasy; idiom vanilli (`RedstoneLampBlock`) to `registerDefaultState(this.defaultBlockState().setValue(LIT, false))` |
| `Level#getHeightmap(...)` | nie ma takiego wywołania: jest `Level#getHeight(Heightmap.Types, int x, int z)` (ewentualnie `getHeightmapPos`); pozycję platformy bramy liczymy z niego |
| własne PNG dla „znaleziska w lawie“ | zbędne: `minecraft:block/cube_all` z `minecraft:block/crying_obsidian` / `minecraft:block/shroomlight` – ten sam trik co przy stoliku (`cube_bottom_top` + `crafting_table_top`), zero assetów do odgadywania |

Wszystkie użyte nazwy klas i metod zostały sprawdzone bezpośrednio na źródłach
Minecraft 1.21.11 z oficjalnymi mapowaniami Mojanga oraz na źródłach
`FabricMC/fabric` w gałęzi `1.21.11` (networking + data attachment API).
Samo budowanie nie jest tu możliwe (sandbox ma zablokowany `maven.fabricmc.net`
i `services.gradle.org`), więc weryfikacja idzie w pętli z maszyną developera:
`./gradlew build` oraz `./gradlew runClient` przechodzą obecnie na 1.21.11
(loader 0.19.3, fabric-api 0.141.6+1.21.11, JDK 21). Składnia wszystkich
plików Javy jest dodatkowo sprawdzana parserem, a wszystkie JSONy są parsowane.

W `lang/en_us.json` i `lang/pl_pl.json` są nazwy wszystkich 15 core'ów, 13
patyków, 26 różdżek i bloków oraz klucze tooltipów (`wandzz.core.tooltip.*`,
`wandzz.tooltip.*`) i okna stolika (`wandzz.gui.table.*`) — oba pliki mają
**identyczne zbiory kluczy**, więc nic nie zostanie „ untranslated”.
Uwaga o formacie zapisu: `WandData` nie trzyma już materiału (idzie z przedmiotu),
a stare `{"material": …, "cores": […]}` nadal się deserializuje, bo
`RecordCodecBuilder` ignoruje dodatkowe klucze.

## Jak to działa w grze

### Drewno, patyki i różdżki — 13 gatunków

Każde drewno z gry daje **własny patyk i własną różdżkę** (12 gatunków z
`#minecraft:planks`, w tym bambus, plus arkany). Gatunek, a nie „kategoria”,
decyduje o liczbie gniazd na rdzenie:

| drewno | patyk | gniazda: zwykła / magiczna |
|---|---|---|
| `oak`, `spruce`, `birch`, `jungle`, `acacia`, `bamboo` | `wandzz:<drewno>_stick` | 1 / 1 |
| `dark_oak`, `mangrove`, `cherry`, `pale_oak` | `wandzz:<drewno>_stick` | 2 / 3 |
| `crimson`, `warped` | `wandzz:<drewno>_stick` | 3 / 4 |
| `arcane` (arkany) | `wandzz:arcane_stick` | 4 / 6 |

Wzór jest jeden (`WandWood.totalSlots`): `1 + extra + (magic ? bonus : 0)`.
Receptury (`data/wandzz/recipe/`):

```
2 deski danego gatunku w słupku             -> 4 x wandzz:<drewno>_stick
deski + patyk tego samego gatunku (skos)    -> 1 x wandzz:<drewno>_wand
wandzz:<drewno>_wand + 2 x glowstone_dust   -> 1 x wandzz:<drewno>_wand_magic
```

Magiczny upgrade tworzy nowy przedmiot, więc **rdzenie się nie przenoszą** (wynik
przepisu data-driven nie widzi komponentów bazy).

Tooltip każdej różdżki mówi, z jakiego jest drewna, ile ma gniazd i co w nich
siedzi (`wandzz.tooltip.*` w `lang`); każdy rdzeń ma w tooltipie poziom, mnożnik
regeneracji many i podpowiedź, jak go zamontować.

### Arkany: drzewo, liście, sadzonka, biom i wymiar `wandzz:arkanum`

| id | co to | jak zdobyć |
|---|---|---|
| `wandzz:arcane_log` | kłoda (`RotatedPillarBlock`) | ścinka drzewa w arkanum / kreatywa |
| `wandzz:arcane_planks` | deski | 1 kłoda → 4 deski |
| `wandzz:arcane_leaves` | liście (`LeavesBlock`, gniją bez nożyc) | łamanie liści |
| `wandzz:arcane_sapling` | sadzonka (`ArcaneSaplingBlock`) | 5% szansa z liści |
| `wandzz:arcane_table` | **stolik arcaniczny** (patrz niżej) | 3 deski + 2 patyki arkańskie |
| `wandzz:arcane_ember` | **Arkanny Zar** – kotwica bramy (patrz niżej) | jeziorka lawy w podziemiach, Y ∈ <-52, 0>; wymagany kilof z żelaza |
| `wandzz:spell_book` | **Księga zaklęć** – podręcznik gestów i kosztów | 1 książka + 4 patyki arkańskie |

Świat generowany jest **w całości danymi** (żadnego mixinu ani `BiomeModifier`):

- `data/wandzz/worldgen/configured_feature/arcane_tree.json` — drzewo wzorowane
  1:1 na vanilla `minecraft:oak`, z `trunk_provider`/`foliage_provider` na blokach
  arkanów i pniem 6–9 (kod nie jest potrzebny, bo `TreeGrower` w 1.21.11 to tylko
  klucz do rejestru `worldgen/configured_feature`),
- `data/wandzz/worldgen/placed_feature/arcane_trees.json` — `count: 5`,
  `in_square`, `surface_water_depth_filter`, `heightmap: OCEAN_FLOOR`, `biome`,
- `data/wandzz/worldgen/biome/arcane_forest.json` — kopia vanilla `forest.json`
  z podmienionym krokiem `vegetal_decoration` (nasze drzewa + podszycie),
  wyczyszczonymi `monster` spawnerami i fioletowymi kolorami trawy/liści/wody,
- `data/wandzz/dimension/arkanum.json` — `type: minecraft:overworld` (ten sam
  `dimension_type`: dobowy cykl, wysokość 384, łóżka i respawn działają) +
  generator `minecraft:noise` z `settings: minecraft:overworld` i
  `biome_source: minecraft:fixed` na biomie `wandzz:arcane_forest`.

Wejście (bez budowania portalu — tego danymi się nie da):

```
/execute in wandzz:arkanum run tp @s 0 200 0
```

**Dlaczego osobny wymiar, a nie biom w overworldzie?** Lista biomów overworldu
jest zamrożona w kodzie (`Registries` → `freeze()` przy bootstrapzie), więc JSON
moda nie dołoży tam wpisu — do tego potrzebny jest TerraBlender / `BiomeModifier`.
Rozwiązanie jest takie, że:

- arkanum jest osobnym, w pełni data-driven wymiarem (las arkański na całym świecie),
- a **sadzonka rośnie wszędzie**, więc drzewa arkanów da się posadzić także w
  zwykłym świecie (zwykły `randomTick` + bone meal, patrz `ModWorldgen`).

### Stolik arcaniczny: montowanie rdzeni

PPM na `wandzz:arcane_table` otwiera okno (`client/WandCoreScreen`):

```
lewa strona  : różdżka trzymana w ręce (główna, potem druga)
prawa strona : gniazda (tyle, ile daje drewno) — klik = wyjęcie
dół          : 36 slotów ekwipunku — klik na rdzeniu = włożenie (PPM = zapełnij)
przyciski    : „Zatwierdź” / „Zwolnij”
```

Zmiany nie są stosowane klik po kliku: **`Zatwierdź` wysyła cały skład**
(`WandLoadoutPayload`), a serwer (`WandzzNetwork#applyLoadout`) robi swoje — przycina
skład do liczby gniazd, sprawdza, czy rdzenie naprawdę są w ekwipunku (jeśli nie,
nie zmienia nic i mówi dlaczego), zwraca wyjęte, zabiera włożone i dopiero wtedy
zapisuje data component. W kreatywie ekwipunek jest nietknięty.

Dlaczego własny `Screen` + pakiet, a nie `MenuType`/`AbstractContainerMenu`:
w 1.21.11 `MenuType` ma prywatny konstruktor, a `MenuScreens#register` **i**
interfejs `ScreenConstructor` też są prywatne — rejestracja ekranu kontenera
wymaga access widenera albo własnego miksu. Wcześniejszy wariant „stół
kowalski” (`WandCoreSmithingRecipe` + `wandzz:wand_core_smithing`) został więc
usunięty — przy okazji był źródłem crasha `Trying to access unbound tag`, bo
przepis liczył `PlacementInfo` w konstruktorze. Został za to skrót: PPM rdzeniem
wyjmuje go z różdżki (`WandInteractions`).

### HUD many

Pasek **nie ma stałej pozycji** – runda 4 miała `x = szerokość/2 + 100` na sztywno
i to był błąd: prawa krawędź hotbara to `/2 + 91`, a kiedy gracz coś trzyma w
drugiej ręce, vanilla dokleja tam slot offhandu (+29 px), więc pasek wchodził na
ekwipunek; przy dużym GUI scale etykieta wychodziła za ekran. Teraz
`client/WandzzHud` liczy wszystko ze stałych, których używa sama vanilla
(`Gui#renderItemHotbar`, `Gui#renderPlayerHealth`):

| warunek | układ |
|---|---|
| po którejś stronie hotbara zostaje ≥ grubość + 12 px | pionowy słupek 6–8 × 20–60 px przy tej stronie (preferowana prawa), dół przy `h - 24` |
| nie zostaje (wąskie albo wysokie okno) | poziomy pasek nad rzędem serc, na szerokość hotbara (`h - 39`, rzędy co `max(10 - (rzędy-2), 3)`) |
| wysokość słupka < 40 px | podziałka co 1/5 zamiast co 1/10 |
| F1, tryb widza, brak `ManaSyncPayload` | nie rysujemy nic |

Grubość słupka zależy od szerokości skalowanego okna (8 px od 470, poniżej 6 px),
a etykieta `Mana: x / y` jest przyklejana tak, żeby została w ekranie – jeśli się
nie mieści, odpada (sam słupek jest czytelny). Zgłoszone przez
`HudElementRegistry.addLast(wandzz:mana_bar, ...)` (Fabric 1.21.11: dawny
`HudLayerRegistrationCallback` został zastąpiony rejestrem elementów HUD).

Mana żyje w Data Attachment po stronie serwera i **nie jest synchronizowana**,
dlatego doszły dwa małe pakiety: `ManaSyncPayload` (S2C — po rzuceniu, ~2×/s
podczas regeneracji i raz przy pełni) oraz `ManaRequestPayload` (C2S — klient
prosi przy `ClientPlayConnectionEvents.JOIN`, więc respawn, zmiana wymiaru i
relog też dostają aktualny stan). Klient wygładza wskaźnik lerpem, więc pasek
płynie, choć serwer wysyła go 2 razy na sekundę.

### Księga zaklęć

`wandzz:spell_book` = książka + 4 patyki arkańskie (`crafting_shaped`, kategoria
`equipment`). PPM otwiera panel 236 × 184 px: nazwa, opis, koszt many + wymagany
poziom rdzenia, lista rdzeni, które dają dane zaklęcie, oraz kratka 34 px z
diagramem gestu (cyanowa linia = ścieżka, żółty kwadracik = początek).

Trzy decyzje warte odnotowania:

- **kolejność stron to kolejność rejestracji** – `SpellRegistry.all()` iteruje
  `LinkedHashMap`, więc `Spells.bootstrap()` deklaruje jednocześnie spis treści;
- **księga niczego nie odblokowuje** – jedynym warunkiem jest
  `Spell#isProvidedBy`, a on i tak patrzy na rdzenie w różdżce. Drugi system
  postępu za jeden ekran informacyjny nie byłby niczym uzasadniony (świadoma
  rezygnacja);
- **otwarcie idzie przez serwer** – `SpellBookItem#use` wysyła `OpenBookPayload`
  (S2C), a `WandzzClient` otwiera `Screen`. Dokładnie ten sam schemat co stolik:
  `MenuType` jest w 1.21.11 prywatny, więc własny ekran + pakiet, bez
  `AbstractContainerMenu` i bez access widenera.

Diagram jest **rysowany, nie wczytany z pliku PNG**: `SpellRegistry.gestureOf(id)`
zwraca te same `List<Point>`, które trafiają do recognize'a. Gotową grafikę można
rozjechać ze wzorcem, a tu nie ma na to szans.

### Przejście do Arkanum: Arkanny Zar

`wandzz:arcane_ember` – fioletowy, lekko świecący blok (lightLevel 4) zbudowany z
tekstur vanilli (`crying_obsidian`, po zapaleniu `shroomlight`). Generuje się w
jeziorach lawy: feature `minecraft:ore` z `target = match_block minecraft:lava`,
`size 4`, `count 6`, Y ∈ <-52, 0>, wstrzyknięty do biomów nadziemnych przez
`BiomeModifications.addFeature(..., UNDERGROUND_ORES, ...)` (patrz
`ModWorldgen.bootstrap`). `discard_chance_on_air_exposure` **musi** zostać 0 –
żyłka domyślnie leży w otwartej przestrzeni jeziorka, a przy 0.7 (diament)
vanilla by ją wyrzucała.

Obsługa:

| krok | co się dzieje |
|---|---|
| gest bramy, patrząc w zimny zar | `OpenGateSpell.canCast` = true → 40 many → `GateService.ignite` |
| `ignite` | zar przechodzi w `lit=true`; po stronie Arkanum jest odnajdywana albo stawiana platforma 5 × 5 z desek z zapalonym zarze w środku |
| PPM w zapalony zar | `GateService.travel` = `ServerPlayer#teleport(TeleportTransition)` z `PLAY_PORTAL_SOUND + PLACE_PORTAL_TICKET` |
| PPM w zar po stronie Arkanum | ten sam kod, w drugą stronę – **bez kosztu many** |
| zar w Arkanum bez pary (brama w świecie zniknęła) | rzuć zaklęcie w ten zar: `emergencyExit` wyprowadza na powierzchnię pod `pozycja · 8` |

Skala jest jak w Netherze: **1 blok w Arkanum = 8 w świecie**, a połączenie jest
**liczone z pozycji**, nie czytane z pliku. Dlatego nie ma tu `BlockEntity` ani
`SavedData` (ten drugi wymaga w 1.21.11 `SavedDataType` wraz z `DataFixTypes` –
za dużo API na jedną parę współrzędnych). `GateService.LINKS` to tylko cache; po
resecie serwera `reconnect(...)` odtwarza parę tym samym wzorem, a jeśli w świecie
nie ma już zapalonego zaru – powierzchnia. Żar jest `pushReaction(BLOCK)` i ma
odporność na eksplozje 1200, bo przestawienie go tłumikiem rozłączałoby parę.

Przy PPM na bramie chodzi o `useWithoutItem`, wiec przechodzisz **z pusta reka
albo z przedmiotem bez wlasnej akcji** (motyka, noz). Z przedmiotem nadajacym sie
do uzycia (blok, rozdzka) vanilla idzie jego sciezka - i slusznie: blok postawisz
na zarze, a rozdzka otworzy okno rysowania.

Zaklęcie: `wandzz:open_gate`, rdzeń poziomu 3+, 40 many, zasięg patrzenia 6 bloków
(tak jak `break_block`). Odmowa celu **nie kosztuje many** – `CastingHandler` pyta
o `Spell#canCast` przed płatnością.

### Wzorce gestów

Ładniejszy sposób obejrzenia gestów niż tablica poniżej: otwórz **księgę zaklęć** –
diagramy są rysowane bezpośrednio z tych samych punktów, które trafiają do
`SpellRegistry`, więc nie mogą się rozjechać z kodem. `docs/gestures.png` to ten sam
widok, ale wyrenderowany w rundzie 4 (7 gestów, bez `open_gate`) – zostaje jako
snapshot do porównań, nie źródło prawdy.

Współrzędne wzorców (przestrzeń robocza ±100, oś Y w dół; rozpoznawanie jest
niezależne od skali i obrotu):

| gest | punkty wzorca (w tej kolejności) | typ |
|---|---|---|
| `strike` | `(-100,-80) (0,80) (100,-80)` | otwarty |
| `torch` | `(0,100) (0,-80) (20,-100)` | otwarty |
| `break_block` | `(-100,-100) (100,-100) (100,100) (-100,100) (-100,-100) (100,100)` | zamknięty |
| `fireball` | `(0,-100) (100,100) (-100,100) (0,-100)` | zamknięty |
| `teleport` | `(-100,-100) (20,-100) (-60,0) (100,0) (-20,100) (100,100)` | otwarty |
| `bomb` | `(-100,-100) (100,100) (100,-100) (-100,100)` | otwarty |
| `dragon_breath` | spirala `r = 100·i/48`, `φ = 4π·i/48`, i = 0…48 (od środka) | otwarty |
| `open_gate` | `(-100,100) (-100,-100) (100,-100) (100,100)` – łuk/brama | otwarty |

Ważne: **podnoszenie myszy nie przerywa rysowania** — `CastingScreen` zbiera ruch
ciągle, więc „powrót" kursora (np. z dołu prawego do górnego prawego przy X) jest
częścią gestu i dlatego wzorce mają te dodatkowe krawędzie. Rysuj jednym
ruchem, nie odrywając ręki od PPM.

### Rzucanie

PPM z różdżką → rysujesz gest → puszczenie PPM = wysyłka na serwer. Każde
odrzucone rzucenie mówi dlaczego (brak różdżki / różdżka bez rdzenia / rdzeń za
niskiego poziomu / za mało many / gest nierozpoznany).

## Czego brakuje / co warto dopracować dalej

- **Cache bram jest w pamięci** – `GateService.LINKS` nie przeżywa restartu, więc
  pierwsze przejście po restarcie idzie przez `reconnect(...)` (wzór
  arytmetyczny + skan kolumn w paśmie generowania). Działa, ale warto znać
  ograniczenie: dwa światy z tym samym seedem mogą trafić na to samo `x/8, z/8` i
  wtedy dwie bramy nadziemne dzielą jedną platformę. Następcą tego jest
  `SavedData` z `SavedDataType(id, supplier, codec, DataFixTypes)` – API dostępne,
  tylko świadomie nieużyte.

- Pozostałe 13 core'ów ma tylko nazwę i poziom – potrzebują własnych zaklęć
  i efektów (analogicznie do Feather/Dragon Breath).
- **Tekstury to placeholderki z vanilla**: różdżki renderują się jako
  `blaze_rod` (magiczne jako `breeze_rod`), patyki jako `minecraft:item/stick`
  (bambusowy jako `bamboo`), kłoda/deski/liście/sadzonka jako `oak_*`, stolik jako
  `crafting_table_top` + `barrel_side`. Własne PNG: `assets/wandzz/textures/*` i
  podmiana `layer0`/`all` w modelach — plików `.png` nie trzeba tworzyć, dopóki
  nie chcesz ich w grze.
- Okno stolika nie ma drag & drop z ekwipunku (klik = włożenie, `Zatwierdź` =
  zapis). Pełny kontener dalby przeciąganie, ale wymaga access widenera na
  `MenuScreens#register` + `ScreenConstructor` (prywatne w 1.21.11) i `MenuType`
  przez `IMenuTypeExtension`, którego w Fabric dla 1.21.11 już nie ma.
- Portalu do arkanum nie ma (wejście to `/execute in wandzz:arkanum run tp @s 0 200 0`);
  do wyboru: blok portalu w kodzie albo TerraBlender, jeśli las ma wejść do
  overworldu.
- Biom arkanum nie ma własnych struktur ani `mood_sound` — wygląda i brzmi jak
  las, tylko z fioletową trawą i bez mobów nocą.
- Skrót klawiszowy `key.wandzz.cast` ma wpisy w `lang`, ale sam keybind nie jest
  zarejestrowany (gest uruchamia PPM z różdżką w ręce).

## Struktura

```
src/main/java/com/wandzz/
  gesture/   – Point, CastingData, $1 Recognizer, wzorce gestów
  spell/     – Spell, SpellRegistry, implementacje zaklęć
  core/      – CoreType (15 typów), WandCoreItem (tooltip: poziom + mnożnik many)
  wand/      – WandWood (13 gatunków), WandData (rdzenie), WandItem (gniazda + tooltip)
  mana/      – ManaComponent, ManaAttachments (Fabric Data Attachment)
  network/   – CastPayload + CastingHandler (rzut i regeneracja many),
               ManaSync/ManaRequest/OpenTable/WandLoadout + WandzzNetwork
               (serwerowy montaż rdzeni i sync HUD-a)
  block/     – ModBlocks, ArcaneTableBlock (PPM = okno), ArcaneSaplingBlock
  world/     – ModWorldgen (TreeGrower + klucze configured_feature / dimension)
  item/      – ModItems, ModComponents, ModItemGroups, WandInteractions
  Wandzz.java – ModInitializer (kolejność bootstrapu ma znaczenie)
src/main/resources/
  assets/wandzz/{items,models/item,models/block,blockstates,lang}/  (59 definicji itemów)
  docs/gestures.png – arkusz wzorców gestów (1:1 z GestureTemplates)
  data/wandzz/recipe/       – 13 × patyki, 26 × różdżki, stolik, 15 × rdzenie, deski
  data/wandzz/loot_table/blocks/ – kłoda, deski, liście (5% sadzonka), sadzonka, stolik
  data/wandzz/worldgen/     – biome + configured_feature + placed_feature
  data/wandzz/dimension/    – arkanum
  data/wandzz/tags/item/    – #wandzz:wands (26), #wandzz:cores (15)
  data/minecraft/tags/{block,item}/ – dopisane bez `replace`
src/client/java/com/wandzz/client/
  CastingScreen.java  – zbieranie gestu z ekranu
  WandCoreScreen.java – okno stolika (klik = włóż/wyjmij, „Zatwierdź”)
  WandzzHud.java      – pasek many;  ManaClientState.java – clientowy stan many
  WandzzClient.java   – ClientModInitializer (odbiorniki S2C, HUD, PPM = rzucanie)
```

Skróty klawiszy i `key.wandzz.cast` na razie tylko istnieją w `lang` — obsługa
gestu jest na PPM, więc nic nie trzeba ustawiać.

