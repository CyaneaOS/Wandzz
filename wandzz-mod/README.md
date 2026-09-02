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
- **`spell/`** – interfejs `Spell`, rejestr `SpellRegistry` oraz **18 zaklęć**:
  lvl 1: `StrikeSpell`, `BreakBlockSpell`, `TorchSpell`, `LeapSpell`, `LumosSpell`,
  `NoxSpell`; lvl 2: `FireballSpell`, `HealSpell`, `RevealSpell`, `AccioSpell`,
  `WingardiumSpell`, `ProtegoSpell`; lvl 3: `TeleportSpell`, `BombSpell`,
  `DragonBreathSpell`, `OpenGateSpell`, `InvisibilitySpell`, `ExpelliarmusSpell`
  (lvl 3+ jest dostępne dla **każdego** core'a na swoim poziomie, nie tylko dla
  Dragon Breath Core, zgodnie z dokumentem). `Spell#canCast` pozwala
  odmówić *przed* pobraniem many, gdy nie ma celu (patrz „Przejście do Arkanum“).
- **`core/CoreType`** – 16 typów core'ów z poziomami. W pełni opisane w dokumencie
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
./tools/install_mod.sh # ten jar do mods/ instancji (sam ja znajduje)
./gradlew runClient    # testy w grze (klient + wbudowany serwer deweloperski)
```

`install_mod.sh` wyszukuje instancje PrismLaunchera po wersji (`MC=1.21.11`, własna ścieżka przez
`-i /sciezka/do/instancji`), usuwa z `mods/` pliki, ktore modami
nie sa ( `-sources.jar`, `*-dev.jar`, cokolwiek z `SNAPSHOT` w nazwie - to
ostatnie to katalog Looma w `~/.gradle/caches`, nie artefakt moda), zgłasza
brak Fabric API i odmawia instalacji jara bez `fabric.mod.json`. Nie chce
uprawnien i nic nie nadpisuje w `mods/` poza `wandzz-*.jar`.

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
| crash przy wejsciu: `IllegalStateException: Failed to load registries due to errors` | `configured_feature/arcane_ember.json` uzawal `"predicate_type": "minecraft:match_block"` - w 1.21.11 rejestr `RULE_TEST` zna `always_true`, `block_match`, `blockstate_match`, `tag_match`, `random_block_match`, `random_blockstate_match`; poprawna nazwa to **`minecraft:block_match`** (nie ma `match_block` ani w kodzie, ani w 39 uzyciach w `data/minecraft`) |
| PPM na żar kradł rozdzce kliknięcie = brak rzucania czarov | `ArcaneEmberBlock#useWithoutItem` zwracało `SUCCESS` zawsze. W 1.21.11 `Minecraft#startUseItem` pyta BLOK przed przedmiotem (`MultiPlayerGameMode#useItemOn` -> `BlockBehaviour#useItemOn` = `TRY_WITH_EMPTY_HAND` -> `useWithoutItem`), a kazdy `InteractionResult.Success` (rowniez `CONSUME`) konczy metode wczesniej niz `gameMode#useItem` - a to wlasnie `useItem` odpala `UseItemCallback` otwierajacy `CastingScreen`. Teraz: zimny zar przepuszcza klik (PASS) jesli w ktorejkolwiek rece cos jest, `SUCCESS` tylko dla zapalonego (przejscie) i dla pustych rak (podpowiedz); rdzen w rece tez dostaje PASS (PPM = zwrot rdzenia) |
| `LeavesBlock is abstract; cannot be instantiated` | w 1.21.11 `LeavesBlock` jest abstract (ma `public abstract MapCodec codec()`); liście buduje się przez `TintedParticleLeavesBlock(float leafParticleChance, Properties)` (jak `oak_leaves`) albo `UntintedParticleLeavesBlock(chance, ParticleOptions, Properties)` |
| `player.level().getGameTime()` | `getGameTime()` leży na `ServerLevel`/`ClientLevel`, **nie** na `Level` → throttling synchronizacji many liczy się od `player.tickCount` (publiczne pole `Entity`, vanilla: `this.tickCount % 20 == 0`) |
| pasek many nachodził na slot offhandu i uciekał z ekranu | geometria liczona ze stałych vanilli + wybór strony + wariant poziomy (sekcja „HUD many“) |
| mana pobierana nawet bez celu | `Spell#canCast(ServerLevel, Player)` sprawdzane w `CastingHandler` **przed** `mana.spend(...)` |
| własny `BiomeModifier` JSON-owy na wstrzyknięcie rudy | zbędny: `net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, klucz)` – zero plików w `data/minecraft`, zero TerraBlendera |
| `registerDefaultState(this.stateDefinition.any())` | pola `stateDefinition` nie widać z podklasy; idiom vanilli (`RedstoneLampBlock`) to `registerDefaultState(this.defaultBlockState().setValue(LIT, false))` |
| `Level#getHeightmap(...)` | nie ma takiego wywołania: jest `Level#getHeight(Heightmap.Types, int x, int z)` (ewentualnie `getHeightmapPos`); pozycję platformy bramy liczymy z niego |
| własne PNG dla „znaleziska w lawie“ | zbędne: `minecraft:block/cube_all` z `minecraft:block/crying_obsidian` / `minecraft:block/shroomlight` – ten sam trik co przy stoliku (`cube_bottom_top` + `crafting_table_top`), zero assetów do odgadywania |
| …i znikały **nadal**, choć korona już była `persistent` | tamten fix
  działał tylko na drzewa generowane *od tej pory*. `isRandomlyTicking` w
  vanilla to `DISTANCE==7 && !PERSISTENT`, więc każdy liść zapisany z
  `persistent=false` **jest na liście losowego ticka swojej sekcji** i jest
  zjadany po kawaiku przy każdym odświeżaniu terenu; feature nie zachodzi
  drugi raz w stary chunk, więc żadna przebudowa terenu nie pomaga. Teraz
  `ArcaneLeavesBlock` nadpisuje `isRandomlyTicking` i `decaying` na `false`
  oraz `tick` na no-op, `Properties.randomTicks()` jest usunięte, a
  `leafState()` dokłada `DISTANCE=1` — ścieżka gnicia nie ma się o co
  zaczepić ani w starych, ani w nowych chunkach. Cząstki opadających liści
  zostają, bo `animateTick` jest wywoływany z losowych pozycji w
  `LevelRenderer#tickParticles`, a nie z random ticka |
| `MobRenderer<ArcaneSprite, ArcaneSpriteRenderState>` – „wrong number of type arguments; required 3” | W 1.21.9+ `MobRenderer` ma **trzy** parametry: `<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<S>>` (trzeci, by `getModel()` zwracał model o znanym typie). `EntityRenderer` nadal dwa — stąd mylący komunikat. Cztery `@Override does not override` i `cannot find symbol: variable super` były kaskadą po uszkodzonej deklaracji nadklasy, nie osobnymi błędami. `MobRenderState` w 1.21.11 nie istnieje: stan po `LivingEntityRenderState`, tak jak `BatRenderState` |

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

## Zgranie (progresja, ktora zmienia czucie w rozdzce)

Nie ma tu leveli, punktow ani menu - jest licznik pod rzad tego samego zaklecia,
bo to jest jedyna rzecz, ktora sprawi, ze mod "wciąga" zamiast byc ciekawostka
na pol godziny. Dwie nagrody, obie odczuwalne w tej samej sekundzie:

| pod rzad | koszt many | prog gestu (MIN_SCORE) |
|---|---|---|
| 0-2 | x1.00 | 0.72 |
| 3-5 | x0.85 | 0.70 |
| 6-8 | x0.70 | 0.68 |
| 9+  | x0.55 | 0.66 |

- Stan trzyma `AttunementComponent` jako Fabric Data Attachment na graczu (ten
  sam mechanizm co mana, `persistent`, przezywa relog).
- Rabat jest liczony NA SERWERZE przed proba platnosci, wiec komunikat „za malo
  many" podaje kwote, ktora faktycznie jest do zaplacenia.
- Obnizony prog dziata TYLKO w oknie rzucania (`CastingScreen`), nie w ksiedze
  zaklec ani w podgladzie - te rysuja ksztalty i nie moga byc „latwiejsze".
- Zmiana zaklecia zeruje rozbieg: nie ma rozpedu „na wszystko", jest rozped na
  jedno. To swiadomie jest nieprzyjemne - inaczej optymalna gra byloby trzymanie
  jednego czaru na zawsze.
- Wejscie na poziom sygnalizuje dzwiek (`block.amethyst_block.chime`) i action
  bar, a HUD dokleja ` - zgranie II` do etykiety paska many.
- Trzecia linia tooltipa `wandzz:arcane_resin` tlumaczy zasade w grze, bez
  czytania README.
## Gestury: czemu taki kształt, a nie inny

Druga runda poprawek, też wymuszona pomiarem, nie gustem. Poprzedni zestaw
(dobrane w ostatniej rundzie) miał być „rozłączny” i w teście na *dokańczonym*
geście był (99,4% trafień, zero złych rzuceń). W grze było inaczej:

* `heal` (kółko) nadal lądował w `torch`,
* `strike` lądował w `torch` albo w nic,
* `fireball` był nie do narysowania (dziesięciowiechowata gwiazda).

Powód jest jeden i wart zapamiętania: **mysz prawie nigdy nie domyka koła**.
Kółko narysowane przeciągnięciem to łuk 250-330°, a dla `$1` łuk jest
najbliżej *innego łuku*, nie *koła*. Każdy kształt z płaskim przejazdem (łuk
bramy, górka skoku, świeczka-pochodnia = trójkąt z ogonem) był więc dla
urwanego kółka lepszym wzorcem niż samo kółko. Stąd „leczym, a rzuca
pochodnię” i „uderzenie rzuca pochodnię albo nic” - oba objawy tej samej
kolizji, tyle że z różnymi sprawcami.

### Co zmienione

| czar | było | jest | dlaczego |
|---|---|---|---|
| `torch` | świeczka (trójkąt z ogonem) | litera **T**: kreska płomienia + trzonek | trójkąt z ogonem podkradał i kółko, i „V” |
| `strike` | „V” | **ptaszek** (krótkie ramię + długie) | „V” to po normalizacji każdy dwuramienny kształt, łącznie z daszkiem płomienia |
| `leap` | podbieg + garb + lądowanie | **amortyzator** (góra, dół, góra, dół) | garb po zaokrągleniu naroży przez mysz = dokładnie daszek kuli ognia |
| `fireball` | gwiazda 10-wierzchołkowa | **trójkąt w górę** (płomień) | prosba gracza: prosciej; 3 segmenty zamiast 10, i 58% → 100% trafień |
| `open_gate` | łuk nad ziemią | **N**: dwa filary i rygiel na skos | łuk to połowa kółka; „mostek” (dwie nogi + daszek) też je podkradał w 100% urwanych prób |
| `heal` | kółko | kółko (bez zmian) | winne były kształty łukowe, nie kółko; wariant „kółko z przerwą 300°” przegrany w pomiarze |
| `teleport` | dwa kwadraty + kreska | bez zmian | gracz potwierdził, że działa - nie ruszamy |
| `break_block`, `dragon_breath`, `bomb` | kreska z pudłem pod nią, fala, romb z kreską | bez zmian | 100% we wszystkich modelach |

### Jak to jest mierzone

Dwa narzędzia, oba w repo, oba na wiernym porcie `$1` (`tools/gestures.py` to
krok po kroku `DollarOneRecognizer.java`: resampling do 64 punktów, pudełko
250, kąt wskazujący, ±45° dla kształtów otwartych i ±180° dla domkniętych,
`score = 1 - d/176,78`):

```
python3 wandzz-mod/tools/check_all.py               # brama: ASCII, importy, JSON, lang, tekstury, gesty
python3 wandzz-mod/tools/gesture_eval.py            # 4 modele reki, 3 koszyki
python3 wandzz-mod/tools/gesture_set.py             # model „gracz dokańcza gest”
python3 wandzz-mod/tools/gesture_sweep.py            # przeszukanie par (skok, brama)
python3 wandzz-mod/tools/gesture_eval.py --sync      # Java i Python maja te same punkty
```

Cztery modele reki w `gesture_eval.py` to nie jest „szum” - biały szum wygląda
pięknie i nic nie znaczy. To: **żebra** (gracz rysuje tylko wierzchołki, same
pociągnięcia proste), **urwany ogon** (to samo, ale 28% ścieżki na końcu nie
zostaje narysowane - najczęstszy realny błąd), **drżąca mysz** (dużo punktów,
dryf brązowski ~3× większy) i **rysik** (dużo punktów, małe drgnienia). Na
każdym modelu losowa skala 0,55-1,5 i obrót ±20°.

| zestaw | koszyk lvl 1 | koszyk lvl 2 | pełny (18) | złe rzucenia |
|---|---|---|---|---|
| runda 16 (świeczka, łuk, gwiazda) | 100% | 90,5% | 92,4% | **do 100% dla `heal`, `fireball`, `dragon_breath` w modelu „urwany ogon”** |
| ten (+ krzyż, schody, strzała, skreślenie, mur, daszek, Y, 3 kreski) | **100%** | **97,9%** | **96,8%** | **0,0%** |
| ten, model „dokańczony gest” (`gesture_set.py`) | 100% | 98,8% | 96,7% | 0,0% |

Dwie zapory, które trzymają te liczby, zostały z poprzedniej rundy i nadal
robią robotę:

1. **Koszyk.** `CastingScreen#castableIds` czyta komponent `wandzz:wand_data` z
   trzymanej różdżki (serwer i tak go synchronizuje) i rozpoznaje **tylko wśród
   czarów, które twoje rdzenie udostępniają** - ten sam warunek
   `Spell#isProvidedBy`, który sprawdza serwer. 18 kształtów walczy ze sobą;
   4 nie walczą.
2. **Margines.** `DollarOneRecognizer.AMBIGUITY_MARGIN = 0,035`: jeśli lider
   wyprzedza wicelidera o mniej, gest jest **odrzucony** z komunikatem
   `wandzz.gesture.ambiguous` („narysuj większy i wyrazniejszy”), a nie rzucony
   byle jak. Pomyłka jest nie do naprawienia, odmowa jest do naprawienia (rysujesz
   jeszcze raz). Stąd 2,2-5,7% „odmów” zamiast złych czarów.

Obniżony próg ze zgrania **nie** ignoruje marginesu - zgranie pomaga na drżenie
ręki, nie na niejednoznaczność.

### Co rysować (18 czarów)

Ten sam rysunek, który widzi gracz, jest w [`docs/gestures.png`](docs/gestures.png)
(czerwona kropka = początek, niebieska = koniec). Obrazek jest *generowany* ze
środków gry, więc nie może się rozjechać z kodem:

```
python3 wandzz-mod/tools/gesture_sheet.py     # nadpisuje docs/gestures.png
```

| czar | gest | ile segmentów |
|---|---|---|
| `strike` | **ptaszek**: kreska w prawo, długa skos w górę | 3 |
| `break_block` | kreska w prawo, wróć, pudło pod nią (sześciu odcinków) | 6 |
| `torch` | litera **T**: kreska w prawo, wróć na środek, w dół | 3 |
| `leap` | **amortyzator**: w górę, w dół, w górę, w dół | 4 |
| `heal` | **kółko** (najlepiej zamknięte, ale nie musi być) | 32 próbkowane |
| `fireball` | **trójkąt**: baza w prawo, skos w górę, skos w dół | 3 |
| `dragon_breath` | fala: w dół, w górę, w dół - od lewej do prawej | 44 próbki |
| `open_gate` | litera **N**: lewy filar w górę, skos do prawego, prawy filar w dół | 3 |
| `teleport` | **dwa kwadraty połączone kreską** | 9 |
| `bomb` | romb z kreską w środku | 6 |
| `reveal` | **krzyż-celownik**: pion w dół, przejazd do lewej, poziomka w prawo | 3 |
| `invisibility` | **schody w dół**: trzy stopnie, coraz niżej, bez zawracania | 5 |
| `lumos` | **strzała w górę**: trzonek i dwa ramiona grotu | 4 |
| `nox` | **skreślone X**: krzyż plus kreska przez niego | 5 |
| `accio` | **miska z uszkiem**: trzy boki prostokąta i przekątna | 4 |
| `wingardium_leviosa` | **daszek nad podłogą**: dwa ramiona w górę i kreska u dołu | 4 |
| `protego` | **mur z trzech kresek**: trzy poziomy kreski, każda krótsza | 6 |
| `expelliarmus` | **długie Y**: dwa ramiona w górę i jedna noga w dół | 4 |

Kształty są do siebie niepodobne *z konstrukcji*, nie z dekoracji: ptaszek i
pudło mają nachodzące na siebie odcinki, T i N mają prostopadłe ramiona,
amortyzator i trójkąt mają zamknięty obwód (albo dwie wysokie nogi), kółko i fala są
jedynymi gładkimi krzywiznami, a dwa kwadraty są jedynym kształtem z dwoma
oddzielnymi obwodami. To właśnie te cechy `$1` widzi po normalizacji.

### Czego już nie próbować (cztery ofiary, żeby nie powtarzać)

* **Kółko z przerwą (300°) jako `heal`.** Brzmi mądrze („skoro mysz nie domyka,
  to niech szablon też nie”), w pomiarze gorsze: dla *dokańczonych* kółek
  58-88% trafień zamiast 100%, bo `resample` rozkłada 64 punkty na łuku
  inaczej niż na kole, a kąt wskazujący startuje od przerwanej strony.
* **Mostek (dwie nogi + daszek) jako `open_gate`.** Zamiast łuku - też podkrada
  urwane kółko, w 100% prób. Został `N`, bo ściany N są prostopadłe i mają
  samoprzecięcie: kółko ucięte trafia do `heal` albo jest odrzucane.
* **Litera „X” jako `bomb`.** Po ucięciu ogona zostaje z niej „V”, czyli dawne
  `strike`; romb z kreską ma pięć segmentów i zamknięty obwód, więc nie ma
  z czym go pomylić.
* **Sam „X” jako `nox`.** Ten akurat wpuściliśmy, ale dopiero z trzecią kreską:
  krzyż `reveal` i X różni jeden obrót o 45°, a ±45° to dokładnie zakres, w
  jakim $1 szuka obrotu dla gestów otwartych — gołe X kradło `reveal`. Kreska
  przez krzyż („skreślenie światła”) wynosi liczbę segmentów z 4 do 5 i nie
  pozwala żadnemu obrotowi domknąć różnicy.
* **Każdy WYPUKŁY, domknięty obrys jako `protego`** (tarcza herbowa, pentagon,
  „dom z daszkiem”). Wszystkie trzy wygrywały screening 18-próbowy, a przy
  200 próbach na model tarcza i tak kradła 1/800 kółko `heal` — bo $1 nie widzi
  „tarczy”, tylko sekwencję kątów, a wypukły pięciokąt po zaokrągleniu naroży
  przez mysz jest murowanym kandydatem na koło. Dlatego `protego` to dziś trzy
  kreski (otwarty, wklęsły kształt, z którego koła nie da się złożyć), a `nox`
  ma dodatkową kreskę przez X: bez niej X jest obrotem krzyża `reveal` o 45°,
  a `±45°` to dokładnie zakres, w jakim $1 szuka obrotu dla kształtów otwartych.
  **Wniosek praktyczny: screening z małą liczbą prób nie istnieje dla kolizji
  rzadszych niż ~2%; ostateczna bramka to `gesture_eval.py` (40) plus 200 prób
  na parę.**

Jedna rzecz, której nie naprawi żaden kształt: jeśli ktoś narysuje *pusty*
daszek „˄” (bez podstawy płomienia), to nie jest żaden gest z tej listy -
`$1` przyzna mu najbliższy dwuramienny kształt i padnie na `torch`. Księga
zaklec pokazuje podstawę; jak coś takiego się przydarzy, w `latest.log` jest
linia `Wandzz: gest nierozpoznany (najblizej ... ) | koszyk: [...]` z wynikiem
lidera, wiceliderem i listą czarów w koszyku - to jest najszybsza diagnoza.

## Wiązanka jednorożców: czemu się nie respiła

Błąd po naszej stronie, nie w data packu: `minecraft:heightmap` w placed feature
podaje **Y pierwszego powietrza nad powierzchnią**, a nie bloku powierzchni.
`GladeFeature` pytał o blok w tym punkcie, dostawał powietrze i grzecznie
`return false` - na każdym chuście. Naprawione skanem w dół (`findGround`,
maks. 8 bloków w dół; śnieg, lód i warstwa liści nie odrzucają już glady), to samo
w `ChronosAltarFeature` (`findPadGround`, inacej ołtarz unosiłby się blok nad
ziemia i boss wpadał w ścianę).

Częstotliwość podkręcona: `unicorn_glade` 1 na 8 chunków (było 1/48),
`chronos_altar` 1 na 40. **Feature'i generują się tylko w nowych chunkach** - nie
zobaczysz ich w eksplorowanym terenie, leć na dziewiczy albo załóż nowy świat.
Nie ma `/locate`, bo to feature, nie struktura.

## Rdzenie, jednorożec, feniks, Chronos i magiczna różdżka

Runda, w której wszystkie rdzenie mają wreszcie **źródło**, a nie tylko przepis.

### Skąd się bierze każdy rdzeń

| rdzeń | poziom | źródło |
|---|---|---|
| `core_feather` | 1 | crafting: **8 piór + włos jednorożca** (`FFF/FUF/FFF`) |
| `core_earth`, `core_water`, `core_nature`, `core_iron` | 1 | crafting (jak dotąd) |
| `core_flame`, `core_frost`, `core_storm`, `core_shadow`, `core_light` | 2 | crafting (jak dotąd) |
| `core_phoenix` | 3 | **4 pióra feniksa + pręt blaze'a** (`F F / R / F F`) |
| `core_echo` | 2 | **tylko drop z Wardena** (nadpisany loot `data/minecraft/loot_table/entities/warden.json`) |
| `core_dragon_breath`, `core_void`, `core_ender` | 3 | crafting (jak dotąd) |
| `core_chronos` | 4 | **tylko boss Chronos na ołtarzu w Arkanum** (przepis usunięty) |

`core_echo` jest szesnastym rdzeniem: `CoreType.ECHO(2, "core_echo", 1.0)`. Poziom 2
oznacza, że automatycznie udostępnia wszystkie czary lvl ≤ 2 (patrz
`Spell#isProvidedBy`) - nowy rdzeń nie potrzebuje własnego czaru, żeby był czegokolien wart.

### Jednorożec i wiązanka leśna (`wandzz:unicorn_glade`)

`GladeFeature` maluje plamę mchu z kwiatami (tulipany, stokrotki, dzwoneczki, piwonie) i
wpuszcza 1-2 jednorożce. Feature jest wtryskiwany do biomów nadziemnych przez
`BiomeModifications.addFeature(..., VEGETAL_DECORATION, ...)` z `rarity_filter: 48`.

**Dlaczego feature, a nie nowy biom?** Nadziemny `BiomeSource` jest wieloszumowy i jego
lista biomów nie jest ani datapackiem, ani API Fabricu. `BiomeModifications` potrafi do
biomu *dokładać* (features, carvery, spawny), ale nie potrafi *włożyć* nowego biomu w
overworld - to teren mixinów albo osobnego wymiaru. Wiązanka daje to, czego gracz szuka:
pewien, rzadki, widoczny z daleka kawałek świata, w którym jednorożce są i tylko tam.

Strzyżenie: PPM nożycami w jednorożca → 1-2 włosa, odrost po 5 minutach
(`Unicorn.HAIR_REGROW_TICKS`). Skubanie, nie rzeź, bo włos ma być źródłem **odnawialnym** -
rdzeń za zwłokę karałby gracza za słuszną decyzję. API: `Shearable#shear(ServerLevel,
SoundSource, ItemStack)` + `readyForShearing()`, wynik `InteractionResult.SUCCESS_SERVER`.

**Hitbox, krok i animacje (runda 24).** Sylwetka jest koniowata, więc i pudełko musi
być koniowate — dawne `1.0 × 1.1` z czasu, gdy koń był kulą z `FluffModel`, obcinało
łeb i kark (celownik klikał w powietrze nad grzbietem):

| co | wartość | skąd |
|---|---|---|
| hitbox | `.sized(1.3F, 1.5F)` | vanilla `EntityType.HORSE` to `1.3964844 × 1.6`; brane 1:1 na proporcje, 1 kratka w dół na wzrost |
| `eyeHeight` | `1.42F` | koń ma 1.52 przy 1.6 wzrostu — utrzymujemy ten sam ułamek |
| krok | `Attributes.STEP_HEIGHT = 1.0` | **w 1.21.11 nie ma `setMaxUpStep()`** — wysokość pokonywanego progu jest atrybutem (`LivingEntity#maxUpStep` czyta `Attributes.STEP_HEIGHT`) |
| spad | `SAFE_FALL_DISTANCE 6.0`, `FALL_DAMAGE_MULTIPLIER 0.5` | `AbstractHorse.createAttributes()`; bez tego jednorożec rodzący się na koronie łamał sobie nogi o własny pień |

`Builder#sized()` tworzy `EntityDimensions.scalable(...)`, nie `fixed(...)` — czyli
jeżeli kiedyś dojdzie wariant młody, `ageScale` przeskaluje też hitbox, bez kodu.

Animacje (`UnicornModel.setupAnim`) to liczby przepisane z `AbstractEquineModel` i
`QuadrupedModel`: przekątne pary nóg (`cos(faza * 0.6662F)`), zamach 0.8F z przodu i
0.5F z tyłu, mnożnik 0.2F w wodzie, klebienie ogona `cos(ageInTicks * 0.7F)` od
tempa > 0,5, kark pompowany `cos(faza * 0.8F)` od tempa > 0,2, docinanie yaw głowy do
±20°. Dane bierze ze `state.walkAnimationPos/Speed`, które **`LivingEntityRenderer`
wypełnia sam** dla każdej encji `LivingEntity` — nie ma tu ani jednego pakietu, ani
tickowego liczenia fazy po naszej stronie. Przepisane, a nie odziedziczone po
`AbstractEquineModel`, bo ten liczy z `EquineRenderState` (rearing, jedzenie, siodło,
`ageScale` jeźdźca) — nasz jednorożec nie jest `Horse`, więc wszystkie te pola byłyby
zerami i model udawałby mur.

Osobny model ma jeszcze jeden powód: `UnicornModel` to **arkusz 64×64** (tak jak
`LayerDefinition.create(mesh, 64, 64)` dla koniowatych w vanilla), a `FluffModel` to
32×32. Oskubanie chowa grzywę i ogon (`ModelPart.visible = false`), a nie cały tułów — róg zostaje, bo to kość, nie sierść.

#### Geometria: czyja jest i jak wstawić swoją

Bryły w `UnicornModel` **są napisane ręcznie**, nie wyeksportowane z Blockbench.
To nie jest upór, tylko fakt do odnotowania: w repo (żadna gałąź) nie ma pliku
modelu — `git ls-tree -r origin/main` i API GitHuba zwracają z Twoich wgrań
**tylko** `unicorn_txt.png` (64×64), bez `.bbmodel`, bez `geo.*.json`, bez
wyeksportowanego `.java`. Nie ma więc czego „dodać z repo"; dopóki plik nie
trafi do `CyaneaOS/Wandzz`, w grze oglądasz moje przybliżenie sylwetki, a Twoje
piksele są tylko *rozłożone* na moich bryłach (patrz tabela UV niżej).

Żeby to był Twój koń 1:1, wrzuć **jeden** z tych plików do repo (najlepiej do
`wandzz-mod/src/client/resources/assets/wandzz/blockbench/`, ale root też mi
wystarczy — i tak go stamtąd wezmę):

1. **Blockbench → File → Export → Export Model → Java Model (`.java`)** — to ścieżka
   bez strat: liczby w tym pliku są już w przestrzeni Minecrafta (`addBox`,
   `texOffs`, `PartPose`), więc przepisuję je bez żadnego przeliczania i nie ma
   pola na zgadywanie znaków osi;
2. albo sam plik projektu **`.bbmodel`** — wtedy czytam `geometry` (bryły:
   `from`/`to`/`uv`/`origin`/`rotation`/`mirror`) i animacje (`animations →
   animators → keyframes`), ale przeliczenie originów Blockbench (oś Y w górę,
   pivot w kostce) na `PartPose` (oś Y w dół) robię przy Twoim pliku i sprawdzam
   okiem na podglądzie — nie chcę tego zgadywać na ślepo, bo właśnie takie
   zgadywanie dało sylwetkę, która „nie jest ta sama co w Blockbenchu".

Animacji (chód, klepanie ogona, ruch grzywy) nie ruszamy: one są *zachowaniem*,
liczbami z `AbstractEquineModel`, i zostają doklejone do Twoich kości po nazwach
(`head`, `neck`, `leg_front_left`, `tail`, `mane`, …). Jeśli w Blockbenchu
nazwiesz kości inaczej, napisz w komentarzu do commita jakie nazwy masz —
dopiszę mapowanie. Po imporcie odpalam `tools/unicorn_uv.py --dopasuj`, bo nowe
bryły = nowe pola do zamalowania na arkuszu, i `tools/check_all.py`.

### Feniks (`wandzz:phoenix`)

Siada **na wierzchołku korony** drzewa arkanu (duch wisi *pod* koroną - dwie sylwetki na
jednym drzewie czytają się bez błędu). 3,5% szansy na drzewo, dedupe w promieniu korony.
`fireImmune()` na `EntityType.Builder` - w 1.21.11 to flaga rejestru encji, a nie nadpisanie
`hurt`, więc feniks nie spala siebie i nie tonie w lawie.
Pióra (`wandzz:phoenix_feather`, 1–2) leci z `loot_table/entities/phoenix.json`, a
cała pula ma jeden warunek: `minecraft:killed_by_player`. W 1.21.11 znaczy to
dosłownie „ostatnim sprawcą obrażeń był gracz" — implementacja to
`context.hasParameter(LootContextParams.LAST_DAMAGE_PLAYER)`, więc ubicie przez
oswojonego wilka czy innego moba dropów **nie** da. Własnego warunku
„player_or_pets" Java nigdy nie miała (jest tylko `killed_by_player`), a wpisanie
nieistniejącej nazwy wywala cały plik lootu — patrz sekcja o błędach świata. Śmiertelne uderzenie podpala
sprawcę (`die(DamageSource)` + `setSecondsOnFire(4)`) i sypie `SOUL_FIRE_FLAME`.

### Chronos - boss ołtarza w Arkanum (`wandzz:chronos_boss`)

`ChronosAltarFeature` stawia 9×9 podest z obsydianu, cztery filary z płaczącego obsydianu
z amethystem na szczycie, żar arkanu w środku i **spawnowi bossa** (120 HP, pancerz 8,
odporność na odrzut). Ołtarz siedzi w `features[4]` (surface_structures) naszego biomu
`wandzz:arcane_forest`, bo jedyne, co `BiomeModifications` nie dotknie, to własny, `fixed`
biom - a Arkanum właśnie taki ma.

Pasek życia: `ServerBossEvent` trzymany w encji plus `startSeenByPlayer`/`stopSeenByPlayer`
- dokładnie jak `WitherBoss`, bo w 1.21.11 nie ma żadnej "flagi bossa" przy `EntityType`.

**Dlaczego feature, a nie `worldgen/structure` z `/locate`?** `structure_set` +
`template_pool` wymagają szablonu `.nbt`, a to plik binarny: nie da się go uczciwie
zrecenzować w gicie ani poprawić, gdy zmienią się bloki. Jeśli chcesz prawdziwą
strukturę z `/locate`, to jest osobny krok - dorzucimy wtedy `jigsaw` + `template_pool`.

### Magiczna różdżka = trzy patyki na skos, ale z poświęconego pnia

Rezygnacja z żywicy w przepisach (mechanika żywicy zostaje: okorowywanie, +1 slot,
×1,2 regeneracji, szybki zapłon):

1. PPM toporkiem w `wandzz:arcane_log`, **gdy w koronie wisi duch** → blok zmienia się w
   `wandzz:arcane_log_blessed` (światło 1 - znak w koronie, że drzewo jest zajęte);
   bez ducha → `arcane_log_stripped`, jak dotąd.
2. `arcane_log_blessed` → 4 × `wandzz:arcane_blessed_stick` (shapeless).
3. `wandzz:arcane_wand_magic` = **3 poświęcone patyki na skos** (`"  S"," S ","S  "`) -
   dokładnie ten sam kształt co zwykła różdżka, więc "tak samo jak zwykle" jest prawdziwe.

Pozostałe 12 gatunków drewna nadal robi różdżkę magiczną z różdżki zwykłej + 2 pył
światła: tylko drzewa arkanu goszczą duchy, więc tylko one mają poświecone drewno.

Żeby pętla się nie wyczerpała, `SpriteRespawner` (wpinka w `END_SERVER_TICK`, co 20 s,
kolumna 25×25×19 wokół gracza) **zwraca ducha na drzewo**, na którym go zabrakło:
wierzchołek pnia → korona → czy wisi? nie → 10% szansy, że nowy siada na krawędzi. Bez
tego cała ścieżka "poświęcone patyki → magiczna różdżka" wysycha po pierwszym wyciu
drzewa, a mapa drzew w `SavedData` jest w tym projekcie zabroniona (wymagałaby
domkniętego `DataFixTypes`), więc skan jest celowo bezstanowy.

### Najnowsza partia czarów (jest ich 18)

| czar | id | koszt | gest | kto udostępnia |
|---|---|---|---|---|
| Odkrycie | `reveal` | 16 | krzyż-celownik | każdy rdzeń lvl ≥ 2 |
| Niewidzialność | `invisibility` | 22 | schody w dół | każdy rdzeń lvl ≥ 3 |
| Leczenie | `heal` | 14 | kółko | `LIGHT`, `NATURE` i każdy rdzeń lvl ≥ 3 |
| Skok | `leap` | 6 | amortyzator | każdy rdzeń lvl ≥ 1 |

**`reveal`** daje `MobEffects.GLOWING` wszystkim `LivingEntity` w kuli 25 kratek
od klatki piersiowej rzucającego (30 s), bez własnego renderera i bez jednego
własnego pakietu – obrys rysuje klient z flagi encji. Dwadzieścia pięć kratek to
dobrze **poniżej** limitów synchronizacji: vanilla podaje `clientTrackingRange` w
chunkach (potwory 8 = 128 kratek, większość zwierząt 10, gracz 32), a
`simulation-distance` ma domyślnie 10 chunków – więc każdy, kto w ogóle widzi
daną encję, widzi też jej obrys. Większy promień znikałby w zależności od
ustawień klienta i wyglądał jak psujący się czar. Itemy i kule XP są poza
zakresem: efekt noszą tylko istoty żywe, a `Item` nie ma `addEffect` - nie ma
czego podświetlać.

**`invisibility`** to `MobEffects.INVISIBILITY` na 45 s złożone tak, żeby nie było
„bąbelków mikstury”: instancja efektu idzie z `ambient=false, visible=false,
showIcon=true`. `visible=false` wycina cząstki u źródła (vanilla
`LivingEntity#updateSynchronizedMobEffectParticles` filtruje po
`MobEffectInstance#isVisible()` i wynik wrzuca do `DATA_EFFECT_PARTICLES`, więc
znikają u wszystkich, nie tylko u rzucającego), a `showIcon=true` zostawia
licznik w HUD – pięcioargumentowy konstruktor ustawia `showIcon = visible`, więc
bez jawnego szóstego argumentu zostalibyście bez ikony i bez pojęcia, kiedy
niewidzialność minie. Oba czary są parą: `reveal` rysuje obrys niezależnie od
niewidzialności, więc „niewidzialny, ale świecący” nie jest bugiem, tylko grą
dwóch zaklęć.

`heal` celowo nie podnosi zdrowia wprost (`setHealth`) tylko dokłada `REGENERATION` +
`ABSORPTION`: bezpośrednie wystawienie HP omija tarcze, jedzenie i efekty. `canCast`
odmawia, gdy pasek jest pełny i tarczy brak - żeby nie płacić 14 many za czar, który nic
nie robi. `leap` zeruje `fallDistance` (`resetFallDistance()`), bo kara za lot byłaby
dziwną ceną za ratunek.

### Partia inkantacji (lumos, nox, accio, wingardium, protego, expelliarmus)

| czar | id | koszt | gest | kto udostępnia | co robi |
|---|---|---|---|---|---|
| Lumos | `lumos` | 4 | strzała w górę | każdy lvl ≥ 1 | kładzie `minecraft:light` (15/15) na ściance, na którą patrzysz |
| Nox | `nox` | 3 | skreślone X | każdy lvl ≥ 1 | zdejmuje wszystkie takie światła w 12 kr. |
| Accio | `accio` | 8 | miska z uszkiem | każdy lvl ≥ 2 | leżące przedmioty z 24 kr. lecą do Ciebie |
| Wingardium Leviosa | `wingardium_leviosa` | 14 | daszek nad podłogą | każdy lvl ≥ 2 | unosi cel na 30 ticków + miękkie lądowanie 260 ticków |
| Protego | `protego` | 12 | mur z trzech kresek | każdy lvl ≥ 2 | `RESISTANCE` 1 + `ABSORPTION` 1 na 12 s, `FIRE_RESISTANCE` na 4 s |
| Expelliarmus | `expelliarmus` | 20 | długie Y | każdy lvl ≥ 3 | odrzut + wypada trzymany przedmiot (nie na graczy) |

Skąd taka mechanika, a nie „ładniejsza”:

* **`lumos` to blok, nie efekt.** Vanilla nie ma efektu dającego światło, a
  `minecraft:light` jest blokiem bez kolizji, `replaceable`, o twardości −1
  (nie da się go wydobyc w survivalu). Światło widać u każdego klienta, bo
 stawienie bloku to zwykły update chunka — zero własnych pakietów, zero kodu
  po stronie klienta. **Właśnie dlatego istnieje `nox`**: bez drugiego czaru
  każde Lumos byłoby wiecznym śmieciem w świecie (te światła są
  `replaceable`, więc gracz zamiata je sam, kładąc tam jakikolwiek blok).
* **`accio` nie wkłada przedmiotów do ekwipunku sią.** Każdy `ItemEntity` z 24
  kratek dostaje pęd w stronę klatki piersiowej rzucającego, a zbiera go
  vanilla: pełen pasek niczego nie kasuje, stacki z komponentami idą normalną
  ścieżką, a świeży drop (10 ticków opóźnienia_pickupu) wpada chwilę później
  zamiast być wyciągany z ręki innego gracza. Liczenie wolnych slotów i
  resztek w stackach to więcej kodu niż ten impuls jest wart.
* **`wingardium_leviosa` to para efektów.** Samo `LEVITATION` to 30 kratek w
  górę i śmiertelny spadek; `SLOW_FALLING` po nim robi windę. Uniesienie jest
  celowo krótkie (1,5 s), żeby ofiara nie wleciała w chmury ani w korony drzew
  — „zgubiony gracz na liściach” to dokładnie ten błąd, który na serwerach
  nazywa się zgłoszeniem. Działa też na graczy, bo nie może ich zabić.
* **`protego` nie odejmuje obrażeń własnym hakiem.** Własny „bloker” wyciąłby
  cios razem ze wszystkim, co vanilla przy nim liczy (crit, knockback, aggro,
  `GameEvent`, postępy, śmierć), a przy okazji byłby mixinem w kod, który
  zmienia się w każdej wersji. Dwa efekty wchodzą w te same miejsca z zewnątrz.
  Uwaga dla tych, którzy pamiętają starsze wersje: w 1.21.11 to
  `MobEffects.RESISTANCE`, a `DAMAGE_RESISTANCE` już nie istnieje.
* **`expelliarmus` nie rozbraja graczy** — świadomie, jedna linia warunku
  (`cel instanceof ServerPlayer`) i komunikat `wandzz.spell.expelliarmus_no_player`.
  Wypadający z ręki przedmiot to jedyny efekt tego moda, który potrafi zniszczyć
  graczowi rozgrywkę (klucz, mapa, narzędzie nie do podniesienia pod ostrzałem)
  i którego nie da się cofnąć. Łup leci na ziemię przy *ofierze*, nie do
  ekwipunku rzucającego — inaczej byłaby to kradzież z 20 punktami many jako
  ceną wejścia.

**Czego w tej partii nie ma i dlaczego**: trzech klątw niepuszczalnych
(Avada Kedavra, Crucio, Imperio) — instant-kill, tortura i zniewolenie gracza
to nie są „czary do zabawy”, tylko narzędzia do wyganiania ludzi z serwera.
Nie ma też Patronusa (własna encja plus renderer, czyli cały nowy byt dla
jednego efektu) ani czarów zmieniających blok w inny (`Reparo`,
transfiguracja) — to już nie rzucanie czaru, tylko edycja świata z uprawnieniami
operatora. Same nazwy to łacińskie inkantacje jako jednowyrazowe słowa; żaden
fragment tekstu źródłowego nie jest tu przepisany.

### Co jeszcze nie jest zrobione (żeby nie szukać po omacku)

* Prawdziwy **nowy biom** w overworldzie (mieszanka szumów / mixin) - patrz wyżej.
* **Jaja spawnu** jednorożca/feniksa/Chronosa: w 1.21.11 `SpawnEggItem` jest sterowany
  danymi (`DataComponents.SPAWN_EGG`) i wymaga `ResourceKey<Item>` w Properties; da się,
  ale to osobny krok. Na razie `/summon wandzz:unicorn`.
* Naturalne spawny (reguły `SpawnPlacements`) - teraz moby przychodzą wyłącznie z
  feature'ów, więc nie znikną z świata i nie zaśmiecają nocy.
* Osobne modele/tekstury dla każdej encji: feniks i Chronos nadal dzielą `FluffModel`
  (32×32, dwie skale), bo tekstury robisz sam; jednorożec ma od tej rundy własny
  `UnicornModel` (geometria konia, 64×64).

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
| `wandzz:arcane_leaves` | liście (`LeavesBlock`, **nie** gniją – patrz niżej) | łamanie liści |
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

Źródłem prawdy jest Java (`GestureTemplates`), a nie tablica w README – dlatego
żadnych współrzędnych tu nie przepisujemy. Trzy sposoby, żeby je zobaczyć:

* **księga zaklęć** w grze (diagram pod opisem czaru) – rysuje te same punkty,
* `docs/gestures.png` – arkusz 18 kształtów, **generowany** przez
  `python3 wandzz-mod/tools/gesture_sheet.py`, więc nie może się rozjechać,
* `python3 wandzz-mod/tools/check_all.py` – brama przed `git push`: pliki `.java`
  w ASCII i ze zbilansowanymi nawiasami, każdy `import` rozwiązywalny, każdy JSON
  parsowalny, lang w parity z `.name`/`.desc` dla wszystkich 18 czarów i z
  licznikiem `register()` vs `spell/impl/`, walidacja tekstur (bez dotykania
  Twoich plików) i `--sync` gestów.
  Od tej rundy także **klucze rejestrów w datapacku**: każdy `"condition"`,
  `"function"` i `"type"` w `loot_table/**` oraz `"type"` w `recipe/**` musi
  istnieć w rejestrze 1.21.11, wyciągniętym ze zrzutu źródeł
  (`LootItemConditions`, `LootItemFunctions`, `LootPoolEntries`, `NumberProviders`,
  `LootContextParamSets`, `RecipeSerializer`). 108 sprawdzonych kluczy. Sama
  parsowalność JSON nie wystarcza — patrz niżej.
* `python3 wandzz-mod/tools/check_all.py --api-scan $(find src/main/java -name '*.java')`
  – dodatkowo sprawdza pisownię członów Minecrafta, które wołamy, przeciw zrzutowi
  źródeł 1.21.11 (`--mcsrc`, domyślnie `/home/user/mcsrc`). To ten test złapał
  `hit.getFace()` (nazwa Yarnowska) tam, gdzie w Mojang-mappings jest
  `hit.getDirection()` — `./gradlew build` powiedziałby o tym dopiero na Twoim
  dysku. Świadomie nie jest w domyślnej bramce: to heurystyka regexowa, bez
  modelu zasięgów, więc nadaje się do przeglądania *nowych* plików, nie do
  blokowania pusha.
* `python3 wandzz-mod/tools/gesture_eval.py --sync` – sprawdza, że Python i Java
  mają co do punktu te same kształty (używane przy każdej zmianie gestu).

Przydatna różnica, którą widać na rysunkach: kształt **domknięty** (ostatni punkt
= pierwszy: `heal`, `fireball`, `teleport`, `bomb`) jest przeszukiwany obrotami w
pełnym zakresie ±180°, a **otwarty** (`strike`, `break_block`, `torch`, `leap`,
`dragon_breath`, `open_gate`) tylko w ±45°. Stąd tyle kłopotu z łukami: otwarty
kształt nie może obracać się do góry nogami, więc „daszek" i „miska" to dwa różne
gesty – ale mysz i tak zaokrągla naroża, dlatego trzymamy się kanciastych
kształtów.

Ważne: **podnoszenie myszy nie przerywa rysowania** – `CastingScreen` zbiera ruch
ciągle, więc „powrót" kursora (np. z prawego dołu do prawej góry przy kółku) jest
częścią gestu i dlatego wzorce mają te dodatkowe krawędzie. Rysuj jednym ruchem,
nie odrywając ręki od PPM; PPM puszcza się na końcu (to wysyła gest).

### Rzucanie

PPM z różdżką → rysujesz gest → puszczenie PPM = wysyłka na serwer. Każde
odrzucone rzucenie mówi dlaczego (brak różdżki / różdżka bez rdzenia / rdzeń za
niskiego poziomu / za mało many / gest nierozpoznany).

## Drzewa, duch arkanu i żywica

- **Korona to kapelusz, nie placek.** Sześć warstw (`+2` do `-3` względem
  ostatniego bloku pnia): czubek 3×3, garnuszek 5×5, ramię 7×7, **rondo 9×9**,
  podwinięte 7×7 i płaszcz 5×5 przy pniu. Średnio 210 bloków liści na koronę
  (stary profil dawał 120) i – zmierzone symulacją na 200 drzewach – **zero**
  drzew z dziurą nad pniem: kratka `|dx|<=1 && |dz|<=1` nigdy nie jest
  „odczepiana”, więc losowe postrzępienie brzegu nie może odsłonić pnia.
  3–5 wiszących kosmyków zostaje.
- **Liście nie gniją na poziomie bloku, nie na poziomie stanu.** Vanilla liczy
  gnicie z pary `DISTANCE==7 && !PERSISTENT` (`LeavesBlock#isRandomlyTicking` →
  `randomTick` → `removeBlock`), a feature kładący blok przez `LevelWriter#setBlock`
  nie przechodzi przez `onPlace`/`updateDistance`, więc DISTANCE zostaje 7.
  Poprzednia poprawka (`persistent=true` w `leafState()`) zamykała tę ścieżkę
  **tylko dla drzew generowanych po niej** — a liście nadal znikały, bo każdy
  stan zapisany poza tą funkcją (stary chunk, `setblock`, struktura, inny mod)
  miał `persistent=false` i gnicie wracało. Teraz `ArcaneLeavesBlock`
  (dziedziczy po `TintedParticleLeavesBlock`, więc tint i opadające cząstki
  zostają) nadpisuje `isRandomlyTicking` i `decaying` na `false`, a `tick` na
  no-op; `Properties.randomTicks()` usunięte, bo służyło wyłącznie domyślnej
  implementacji `isRandomlyTicking`. `leafState()` dokłada `DISTANCE=1`, żeby
  żaden inny mechanizm nie miał się o co zaczepić.
- **Czego to nie naprawia:** koron, które już zdążyły zgnic w Twoim świecie.
  Chunki się nie regenerują, więc stare drzewa zostają dziurawe – za to przestają
  tracić kolejne liście. Nowe drzewa zobaczysz tylko w nowym terenie (daleko od
  bazy albo nowy świat); `F3+A` nic tu nie zmieni i to jest zachowanie
  oczekiwane.
- Kształt opisuje Java (`ArcaneStranglerFeature`), nie JSON: vanilla
  `blob_foliage_placer` nie umie ani zwisania, ani spirali. Rejestr idzie jako
  `wandzz:arcane_strangler`, a `configured_feature/arcane_tree.json` tylko je
  wywołuje (ten sam feature obsługuje generowanie świata i wzrost z sadzonki).
- Sadzonka na koronie innego drzewa NIE przebija gospodarza: feature schodzi do
  dolnej krawędzi jego korony i wspina się spiralą (~35,5° na blok) dookoła pnia,
  a własną koronę stawia nad tamtą. Logi trafiają tylko tam, gdzie pozwala
  `TreeFeature.validTreePos` — pień gospodarza nigdy nie zostaje nadpisany.
- `wandzz:arcane_sprite`: **9%** nowej korony (było 34% — las wyglądał jak
  dekorowany), maksymalnie **jeden duch na drzewo** (sadzonka na cudzej koronie
  potrafiła dorzucić drugiego), i wisi **pod dolną krawędzią liścia na rancie
  korony**, nie w jej środku — zarówno przy spawnie z feature'a, jak i przy
  powrocie po walce (`ArcaneSprite#hangUnder` liczy `pos.getY() - 0.3`, bo
  model liczony jest od stóp w górę).
- **Żywica bierze się z okorowywania, nie z zabijania.** PPM toporkiem
  (`#minecraft:axes`) w `wandzz:arcane_log` zdejmują korę: blok zamienia się w
  `wandzz:arcane_log_stripped` (odwracalne — da się postawić z powrotem),
  toporek traci 1 trwałość, a z pnia wypływa żywica. Szansa: **100%** (plus
  25% na drugą kroplę), jeśli w koronie wisi żywy duch; **45%**, jeśli nie.
  Zabicie ducha więc NIE jest drogą do żywicy, tylko sposobem jej utraty.
  Loot table ducha po zabiciu jest pusty `{"pools": []}` — świadomie.
- Brak licznika uderzeń w stanie bloku to decyzja, nie niedoróbka: limit wynosi
  „jedno okorowanie na pień", a stan nosi sam blok, więc nie ma czego
  zapisywać, synchronizować ani psuć przemieszczaniem drzewa.
- `wandzz:arcane_resin` ma własną klasę (`ArcaneResinItem`) tylko po to, żeby
  tooltip tłumaczył mechanikę w grze (dwie linie, oba klucze użyte — kontrolka
  wywala martwe klucze lang).
- Placeholdery tekstur (`assets/wandzz/textures/{block,item,entity}`, 37 PNG: 27 `item` + 6 `block` + 4 `entity`) są
  po to, żeby kształt było widać w grze i żeby brak Twojej grafiki nie dawał
  fioletowej kostki. Liście są szare celowo: mnoży je tint z
  `ColorProviderRegistry.BLOCK` w `WandzzClient` — chcesz malować fiolet
  ręcznie, wywal tę jedną linię. Tekstura encji musi mieć 32×32
  (`LayerDefinition.create(mesh, 32, 32)` definiuje skalę UV). Patyki od tej
  rundy NIE są vanilla — patrz „Tekstury: co jest czyje”.

## Tekstury: co jest czyje

Zasada jest jedna: model wskazuje `wandzz:<sciezka>`, a gra szuka pliku
`assets/wandzz/textures/<sciezka>.png`. Nie ma pliku — jest fioletowa kostka,
więc `tools/placeholder_textures.py` dorzuca brzydkie, ale czytelne zastępstwo
(ukośny patyk w odcieniu drewna) i **nigdy nie nadpisuje tego, co już leży**.
Czyli: wrzucasz swoje PNG do `textures/item/` pod taką samą nazwą i wygrywają,
bez jednej zmiany w modelach.

```
python3 wandzz-mod/tools/placeholder_textures.py          # uzupelnij braki
python3 wandzz-mod/tools/placeholder_textures.py --check  # kontrolka: 0 = kompletne
python3 wandzz-mod/tools/placeholder_textures.py --validate   # format kazdego PNG
python3 wandzz-mod/tools/texture_sheet.py                 # docs/textures.png: jak widzi gra
python3 wandzz-mod/tools/unicorn_uv.py                    # siatka UV jednorożca vs Twój arkusz
python3 wandzz-mod/tools/unicorn_uv.py --png docs/unicorn_uv.png --skala 10
```

**Plik `unicorn_txt.png` jest Twój i leży w `textures/entity/`.** Jednorożec nie ma już
placeholdera — model wskazuje dokładnie tę nazwę, którą wrzuciłeś do repo, więc
`placeholder_textures.py` nic tam nie dopisze (skrypt generuje tylko *braki*). Uwaga:
plik wgrany przez WWW na **główny katalog repo** nie jest w zasobach moda; żeby gra go
zobaczyła, musi leżeć w `wandzz-mod/src/main/resources/assets/wandzz/textures/entity/`.

Siatka UV jest w `UnicornModel.java`, a `tools/unicorn_uv.py` ją stamtąd *parsuje*
(drukiego, ręcznego spisu nie ma, więc nie może się rozjechać — ten sam patent co
`gesture_set.py --sync`). Skrypt mówi, gdzie która bryla bierze piksele:

| bryła (UnicornModel) | UV | na co patrzy |
|---|---|---|
| tułów (dwie bryły 10×10×11) | (0,14) | jasny pas w pół arkusza |
| tułów jest rozbity, bo `10×10×22` rozwija się do paska 64×32 | — | patrz komentarz w kodzie |
| nogi (4×11×4, jedno UV) | (23,22) | lawenda + niebieskie kopytka |
| szyja | (13,20) | pasek pod grzywą |
| głowa (6×5×7) | (0,18) | **dwa czarne piksele = oczy na bokach głowy**, dokładnie jak u konia |
| uszy | (1,0) | ciemnoszare pole |
| róg (dwie bryły) | (31,24) | różowe pole obok ogona |
| grzywa | (30,24) | to samo różowe pole |
| ogon (3×11×4) | (24,24) | różowe pole + 4 kratki, których jeszcze nie domalowałeś |
| pysk | (7,7) | lawenda |

Stan zmierzony: **14 z 15 brył ma wszystkie sześć ścian w zamalowanych pikselach**.
Dziura jest jedna — góra głowy (`UP`, prostokąt (13,18)-(19,25)); bramka
`tools/check_all.py` ma ją wpisaną jako dozwoloną (`ZNANE_DZIURY_UV = 1`), więc jak
domalujesz te 6×7 kratek, skrypt zgłosi spadek do 0 i warto wtedy zniżyć licznik.
`--dopasuj [--nakladaj] [--roi x0,y0-x1,y1]` potrafi przeliczyć siatkę pod *nowy*
arkusz: wyszukuje przesunięcie o największym pokryciu, a `--roi` zamyka wyszukiwanie
w oknie ("tu są włosy, tu kopytka"). Dwie bryle mogą brać te same piksele — to nie
błąd, tylko oszczędność rzadkiego arkusza.

`docs/textures.png` to ten sam powiekszony x8 podglad, ktory patrzy na tekstury
dwa razy: na szachownicy (widac, gdzie jest przezroczystosc) i na szarym tle
(widac, czy ksztalt nie ginie w ekwipunku). Kolejnosc komorek = alfabetyczna
kolejnosc plikow, skrypt wypisuje ja na stdout.

**Filtry wierszy: dlaczego narzędzia czytają PNG przez `tools/png.py`.**
W PNG każdy wiersz jest spłaszczony jednym z filtrów (None/Sub/Up/Average/Paeth),
a `zlib` zdejmuje tylko ściskanie — **dekompresja nie zwraca pikseli**. Narzędzie,
które czyta bajty IDAT wprost, widzi inne kolory i inną alfę niż gra. Właśnie tak
kiedyś powstał fałszywy alarm „kreska światła z alfą 1/255”, a następnie jego
„naprawa”: podbicie tych bajtów do 255 realnie **popsuło 10 dobrych tekstur**
(znikająca przezroczystość, czarne piksele). Stan faktyczny był i jest dobry: każdy
patyk to 37 widocznych pikseli w trzech kolorach rampy (cień `#874727`, korpus
`#9F5630`, światło `#B15E34` dla akacji), całość z alfą 255, żadnego śladu
„warstwy na 0%”. Co z tego wynikło:

* pliki są przywrócone bajt w bajt do tego, co wgrałeś (`fb9f55c` to wersja „przed”),
* tryb `--fix-alpha` **zniknął na stałe** — narzędzie nie ma prawa naprawiać czyjejś
  grafiki przez domyślanie się intencji; ma meldować, nie edytować,
* `--validate` liczy alfę i kolory przez `PNG.czytaj` (filtry odświeżone), tak samo
  `tools/texture_sheet.py`, więc `docs/textures.png` wreszcie pokazuje to, co widać
  w grze, a nie surowe bajty delta.

Zasada dla każdego nowego narzędzia w tym katalogu: **nigdy nie czytaj IDAT bez
odfiltrowania wierszy**.

| slot (plik, ktory nadpisujesz) | co to jest | czyj |
|---|---|---|
| `textures/item/*_stick.png` (11 sztuk: `oak`, `spruce`, `birch`, `jungle`, `acacia`, `dark_oak`, `crimson`, `warped`, `cherry`, `pale_oak`, `mangrove`) | patyki do receptur | **Twoje, w repo** (16×16 RGBA, 37 pikseli, rampa trzech kolorów) |
| `textures/item/arcane_stick.png`, `arcane_blessed_stick.png` | drewno duchów („Święty patyk” wygląda identycznie jak zwyczajny, dopóki nie ma swojej tekstury) | placeholder do namalowania |
| `textures/item/arcane_resin.png` | żywica (przedmiot czysto opisowy) | placeholder |
| `textures/block/arcane_log{,_stripped,_blessed,_top}.png`, `arcane_leaves.png`, `arcane_sapling.png` | kłoda, okorowana, poświęcona, góra pnia, liście (tint!), sadzonka | placeholder |
| `textures/entity/unicorn_txt.png` | jednorożec (model koniowaty `UnicornModel`) | **Twoje, w repo** — 64×64 RGBA; placeholdera nie ma i nie będzie |
| `textures/entity/{phoenix,chronos_boss,arcane_sprite}.png` | feniks, boss, duch arkanu | placeholder, **32×32** |
| `textures/item/*_wand.png` (10 sztuk, te same gatunki co patyki) | różdzki zwykłe | **Twoje, w repo** |
| `textures/item/spruce_wand.png`, `bamboo_wand.png`, `arcane_wand.png` | jodła, bambus i arcane — 3 różdzki bez grafiki | placeholder do namalowania |
| 13 × `*_wand_magic.json` | różdzki magiczne | **świadomie bez własnej tekstury** — patrz akapit nizej |

**Różdzki magiczne: na razie bez własnej grafiki, z poświatą zaklęć (tymczasowo).**
Modele `*_wand_magic.json` wskazują **tę samą** teksturę co zwyczajny wariant
gatunku (`wandzz:item/<drewno>_wand`), a `WandItem#isFoil(ItemStack)` zwraca
`true`, gdy flaga `magic` jest ustawiona. Vanilla 1.21.11 decyduje o poświacie w
`ItemStack#hasFoil()`: najpierw patrzy na komponent
`minecraft:enchantment_glint_override`, a dopiero gdy go nie ma — na
`Item#isFoil(ItemStack)` (domyślnie `stack.isEnchanted()`); `BlockModelWrapper`
dokłada wtedy warstwę „foil”. Dlatego jedno nadpisanie metody działa w
ekwipunku, w ręce i na ziemi naraz, bez kodu klienckiego.

Wybrałem nadpisanie zamiast komponentu, choć **obie drogi działają** (komponent
w `Item.Properties` też dosięga stacków leżących w świecie od dawna, bo
`PatchedDataComponentMap#get` dla brakującego klucza zagląda do
`item.components()` — sprawdziłem w source 1.21.11). Powody są praktyczne:
na stacku nie przybywa żadnego zapisywanego/synchronizowanego pola, więc nie wchodzi
w drogę naszym ścieżkom przebudowującym komponenty (`setLoadout`, `withResin`,
naprawa), a definicja zostaje obok flagi `magic` — nie ma jak się rozjechać.
Vanilla używa komponentu tam, gdzie poświata jest cechą *konkretnego* stacka lub
przedmiotu bez klasy (`enchanted_golden_apple`, `written_book`, `experience_bottle`).
`magic || super.isFoil(stack)` zachowuje przy tym normalną semantykę: zwyczajna
różdzka z realnym zaklęciem i tak zaświeci.

Kiedy będziesz miał własne `*_wand_magic.png`: kładziesz je do `textures/item/`,
w `<drewno>_wand_magic.json` zmieniasz `layer0` na
`wandzz:item/<drewno>_wand_magic` i — jeśli poświata ma zniknąć — usuwasz
nadpisanie `isFoil`. Trzy różdzki bez grafiki (`spruce_wand`, `bamboo_wand`,
`arcane_wand`) wskazują placeholder w tym samym schemacie, więc im wystarczy
nadpisać plik.

Format, którego trzymaj się przy eksporcie (to rzeczy, które realnie potrafią
dać fioletową kostkę albo czarny piksel): kwadrat **16×16** dla przedmiotów i bloków,
**32×32** dla encji `FluffModel`, 8 bitów na kanał, RGBA (paleta indexed bywa
gubiona przez `SpriteLoader` — lepiej oddać RGBA), **bez interlace’u** (Adam7),
bez uciętych chunków i bez sklejkowych CRC. `--validate` rozdziela dwie rzeczy:
**błąd** (exit 1) to to, czego `SpriteLoader` nie wczyta — interlace Adam7, 16
bitów na kanał, urwany chunk, zły CRC; **ostrzeżenie** (exit 0) to to, co się
wczyta, ale będzie wyglądać krzywo — rozmiar inny niż 16×16 dla `item/`+`block/`
i 32×32 dla `entity/`, paleta `indexed`, brak kanału alfa przy wycinanym kształcie.
Czyli eksport 32×32 albo 64×64 wejdzie i zagra, tylko dostaniesz szept, że UV
encji może się rozjechać — nie musisz nic skalować przed commitem. Obie kontrolki
są w jednym narzędziu:

```
python3 wandzz-mod/tools/placeholder_textures.py --check      # czy model nie wskazuje w pustke
python3 wandzz-mod/tools/placeholder_textures.py --validate   # czy kazdy PNG jest poprawny
```

## Czego brakuje / co warto dopracować dalej

- **Cache bram jest w pamięci** – `GateService.LINKS` nie przeżywa restartu, więc
  pierwsze przejście po restarcie idzie przez `reconnect(...)` (wzór
  arytmetyczny + skan kolumn w paśmie generowania). Działa, ale warto znać
  ograniczenie: dwa światy z tym samym seedem mogą trafić na to samo `x/8, z/8` i
  wtedy dwie bramy nadziemne dzielą jedną platformę. Następcą tego jest
  `SavedData` z `SavedDataType(id, supplier, codec, DataFixTypes)` – API dostępne,
  tylko świadomie nieużyte.

- **Trzy linie z logu przy tworzeniu świata (2026-09-02), po jednej na każdy
  rodzaj przyczyny**:
  * `com.mojang.text2speech.Narrator$InitializeException: Unable to load library
    'flite'` — **nie nasze**: to narrator Minecrafta chce `libflite` do mowy, a
    na Archu ten pakiet jest osobny. `sudo pacman -S flite` albo
    Opcje → Dostępność → Narrator = wyłącz. Nie wpływa na mod ani na świat.
  * `########## GL ERROR ########## @ Render — 65547: X11: Standard cursor shape
    unavailable` — **nie nasze**: menedżer okien nie ma kursora z nazwy, o który
    prosi GLFW/X11 (temat kursorów bez `Xcursor.theme`). Naprawia się
    `xcursor-breeze`/`xcursor-themes` + `Xcursor.theme` w `~/.Xresources` albo
    startem na Waylandzie. W kodzie moda nie ma czego zmieniać.
  * `Couldn't parse data file 'wandzz:entities/phoenix' from
    'wandzz:loot_table/entities/phoenix.json': Unknown registry key in
    minecraft:loot_condition_type: minecraft:killed_by_player_or_pets` — **nasze
    i naprawione w tej rundzie**: w rejestrze `loot_condition_type` 1.21.11 jest
    19 warunków i `killed_by_player_or_pets` wśród nich nie ma (jest sam
    `killed_by_player`). Rejestr jest *closed*, więc serwer zrzuca cały plik lootu
    jako „Error" i feniks nie wypada **nic** — w grze zero błędu, jedna linia w
    logu przy tworzeniu świata. Poprawka: `minecraft:killed_by_player` (to, czego
    używają już `unicorn.json` i nadpisania `minecraft:loot_table/entities/warden.json`).
    Bramka `tools/check_all.py` liczy teraz klucze lootu/recipes przeciw rejestrom
    ze zrzutu, więc wymyślenie warunku kończy się FAIL-em przed pushem, nie w
    połowie tworzenia świata.

- Pozostałe 13 core'ów ma tylko nazwę i poziom – potrzebują własnych zaklęć
  i efektów (analogicznie do Feather/Dragon Breath).
- **Różdżki nadal udają vanilla**: `*_wand` to `minecraft:item/blaze_rod`,
  `*_wand_magic` to `minecraft:item/breeze_rod` (13 + 13 modeli). Świadomie —
  własne tekstury mają być Twoje, a sloty na nie trzeba najpierw założyć
  (`wandzz:item/<gatunek>_wand`). Patyki są już podpięte pod `wandzz:item/*_stick`,
  kłoda/liście/sadzonka/żywica pod `wandzz:block|item/*`, a stolik jako
  `crafting_table_top` + `barrel_side` został.
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
  assets/wandzz/items/       – 68 definicji itemów (1.21.11: model + overrides)
  assets/wandzz/models/{item,block}/ – 61 + 12 modeli (warstwy -> textures/)
  assets/wandzz/blockstates/ – 8 plikow
  assets/wandzz/lang/         – pl_pl + en_us, 175 kluczy w kazdym
  assets/wandzz/textures/     – 37 PNG (27 item / 6 block / 4 entity; placeholdery + Twoje grafiki nadpisuja)
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
docs/gestures.png  – arkusz wzorców gestów, GENEROWANY przez tools/gesture_sheet.py
tools/             – gesture_eval.py (test 4 rak + --sync), gesture_set.py (gest
                     dokańczany), gesture_sweep.py (przeszukiwanie par ksztaltow),
                     gesture_sheet.py (arkusz PNG), gestures.py (port $1),
                     placeholder_textures.py (uzupełnia braki tekstur, nie nadpisuje),
                     install_mod.sh (jar -> mods/ instancji, --list, --fix-mods)
```

Skróty klawiszy i `key.wandzz.cast` na razie tylko istnieją w `lang` — obsługa
gestu jest na PPM, więc nic nie trzeba ustawiać.

