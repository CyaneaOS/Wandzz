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
| pakiet `wandzz:cast` niezarejestrowany | Fabric wymaga `PayloadTypeRegistry.playC2S().register(...)` **przed** `registerGlobalReceiver`, po obu stronach — rejestracja przeniesiona do common entrypointu; usunięty błędny odbiornik klienta dla pakietu C2S (run-time `IllegalArgumentException`) |
| `Entity#hurtServer` | `hurtServer` jest na `LivingEntity` → wzorzec `instanceof LivingEntity` zamiast rzutowania na `Entity` |
| rzutowanie `(ServerLevel) player.level()` | `instanceof ServerLevel` (bez ryzyka `ClassCastException`) |
| `server().execute(...)` w handlerze | zbędne — `PlayPayloadHandler` jest już wywoływany na wątku serwera |
| brak wrappera w repo | dodane `gradlew`, `gradlew.bat`, `gradle/wrapper/*` (Gradle 9.5.1), `.gitattributes`, `.gitignore`, `LICENSE` |
| `loom_version=1.17-SNAPSHOT` | przypięte do `1.17.20` (ten sam plugin, konkretny build zamiast ruchomego snapshotu) |
| `build.gradle` bez `publishing`/`jar`/`encoding` | uzupełnione wg oficjalnego template'u + `options.encoding = "UTF-8"` |

Wszystkie użyte nazwy klas i metod zostały sprawdzone bezpośrednio na źródłach
Minecraft 1.21.11 z oficjalnymi mapowaniami Mojanga oraz na źródłach
`FabricMC/fabric` w gałęzi `1.21.11` (networking + data attachment API).
**Build nie został odpalony w tym środowisku** — sandbox ma zablokowany dostęp
do `maven.fabricmc.net` i `services.gradle.org`, więc zależności nie da się
pobrać. Składnia wszystkich plików jest zweryfikowana parserem Javy.

Dodatkowo w `lang/en_us.json` i `lang/pl_pl.json` dopisano nazwy wszystkich
15 core'ów oraz klucz `wandzz.core.level` (używany przez tooltip rdzeni).

## Czego brakuje / co warto dopracować dalej

- Pozostałe 13 core'ów ma tylko nazwę i poziom – potrzebują własnych zaklęć
  i efektów (analogicznie do Feather/Dragon Breath).
- Modele/tekstury itemów (`assets/wandzz/models`, `assets/wandzz/items`,
  `assets/wandzz/textures`) — bez nich przedmioty będą widoczne w ekwipunku,
  ale gra zaloguje brak modelu i nie wyrenderuje ikony.
- GUI do wkładania core'ów w sloty różdżki (obecnie `WandItem.insertCore` to
  gotowa metoda, ale brak ekranu/przepisu craftingowego, który by z niej korzystał).
- HUD z paskiem many.
- Recipe (crafting) dla różdżek i core'ów – brak plików `data/wandzz/recipe`.
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
