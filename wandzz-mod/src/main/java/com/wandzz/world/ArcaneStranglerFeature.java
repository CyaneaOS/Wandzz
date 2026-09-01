package com.wandzz.world;

import com.mojang.serialization.Codec;
import com.wandzz.block.ModBlocks;
import com.wandzz.entity.ArcaneSprite;
import com.wandzz.entity.ModEntities;
import com.wandzz.entity.Phoenix;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Drzewo arkanskie jako DLA WICIEL (strangler fig), a nie jako "deb o
 * fioletowym zabarwieniu". Ten feature robi trzy rzeczy, ktorych nie da sie
 * opisac vanilla {@code minecraft:tree}:
 *
 * <ol>
 *   <li><b>Korona w trzech warstwach z kosmykami</b> - vanilla
 *       {@code blob_foliage_placer} kladzie walec lisci, ktory wyglada jak
 *       ciasto. Tutaj: dwa pierscienie o promieniu 3, jeden o 2, czubek 1,
 *       losowo odpuszczone naroza (brzeg postrzepiony) plus 3-5 "koncowek"
 *       zwisajacych w dol. Te ostatnie sa {@code persistent=true}, bo wisa poza
 *       zasiegiem pnia i bez tego zgnilyby w ciagu kilkudziesieciu tickow.</li>
 *   <li><b>Oplatanie gospodarza</b> - jesli sadzonka kielkuje NA KORONIE innego
 *       drzewa arkanskiego (a kielkuje, bo liscie spadaja wlasnie na liscie),
 *       nowy pien NIE przebija gospodarza na wylot. Schodzi do dolnej krawedzi
 *       jego korony i wspina sie spirala (krok ~35,5 stopnia na blok = pelny
 *       obrot na ~10 blokow) wokol jego pnia, az nad te korone; dopiero tam
 *       stawia wlasna. Logi trafiaja tylko tam, gdzie pozwala
 *       {@link TreeFeature#validTreePos} (powietrze lub liscie) - piec
 *       gospodarza nigdy nie zostaje nadpisany.</li>
 *   <li><b>Duch arkanu</b> - z szansa ~34% w nowo posadzonej koronie spawnuje
 *       sie {@link ArcaneSprite}. Ten sam feature obsluguje wzrost z sadzonki
 *       (patrz {@code ModWorldgen#ARCANE_TREE_GROWER}), wiec "drzewo jest
 *       magiczne, jesli na nim wisi" dziala tak samo w swiecie generowanym, jak
 *       i w posadzonym przez gracza.</li>
 * </ol>
 *
 * Kodek konfiguracji to {@link NoneFeatureConfiguration}: cala "konfiguracja"
 * jest w tym pliku, zeby dalo sie ja czytac jako kod, a nie jako JSON z petla
 * placerow. {@code ModWorldgen} rejestruje te instance pod id
 * {@code wandzz:arcane_strangler}, a {@code configured_feature/arcane_tree.json}
 * jedynie je wywola.
 */
public class ArcaneStranglerFeature extends Feature<NoneFeatureConfiguration> {

    /** Bazowa wysokosc pnia na zwyklym gruncie: 6..9. */
    private static final int TRUNK_BASE = 6;
    private static final int TRUNK_RAND = 4;

    /** Kat przyrostu spirali na jeden blok wysokosci (~35,5 stopnia). */
    private static final double HELIX_STEP = 0.62;
    /** Promien spirali wokol pnia gospodarza (1 = przytulona, 2 = widoczna). */
    private static final int HELIX_RADIUS = 2;
    /** Ile blokow ponad korone gospodarza siegamy spirala. */
    private static final int HELIX_RISE = 3;
    /** Glebokoosc, z jaka wchodzimy w korone gospodarza (od jej dolnej krawedzi). */
    private static final int HELIX_DIVE = 2;

    /**
     * Szansa na ducha arkanu w nowej koronie. Dawniej 0.34, czyli co trzecie
     * drzewo mialo wisiorka - las wygladal jak choinka z dekoracjami. 0.09 to
     * okolo jednego duha na 11 drzew: duha sie szuka, ale da sie go znalezc,
     * a "to drzewo jest magiczne" nadal cos znaczy, bo nie kazde jest.
     */
    private static final double SPRITE_CHANCE = 0.09;

    /**
     * Szansa, ze na krawedzi tej samej korony siada feniks. Nizsza niz
     * SPRITE_CHANCE i od niej niezalezna: feniks jest zrodlem pior na rdzen
     * poziomu 3, wiec ma byc rzadki, ale nie az rzadki, zebys czekal na nowe
     * drzewo.
     */
    private static final double PHOENIX_CHANCE = 0.035;

    /** Zasieg szukania pnia gospodarza w dol, przez liscie. */
    private static final int HOST_LOOKDOWN = 24;
    /** Promien, w ktorym na danym pietrze szukamy pnia (korona bywa przesunieta). */
    private static final int HOST_SCAN_RADIUS = 2;

    public ArcaneStranglerFeature(final Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        final WorldGenLevel level = context.level();
        final RandomSource random = context.random();
        final BlockPos origin = context.origin();

        final @Nullable BlockPos host = findHostTrunk(level, origin);
        final int crownY;
        final int crownX;
        final int crownZ;

        if (host == null) {
            // normalne drzewo: pien w gore, korona na jego czubku
            crownX = origin.getX();
            crownZ = origin.getZ();
            crownY = origin.getY() + TRUNK_BASE + random.nextInt(TRUNK_RAND + 1);
            placeTrunk(level, origin, crownY - origin.getY() + 1);
        } else {
            // dlawiciel: spirala wokol korony gospodarza, nasza korona nad nia
            int top = host.getY();
            while (isLog(level.getBlockState(new BlockPos(host.getX(), top + 1, host.getZ())))) {
                top++;
            }
            crownX = host.getX();
            crownZ = host.getZ();
            final int helixTop = top + HELIX_RISE;
            placeStrangler(level, random, origin, crownX, crownZ, top - HELIX_DIVE, helixTop);
            crownY = helixTop + 1;
        }

        final List<BlockPos> rim = new ArrayList<>();
        placeCanopy(level, random, new BlockPos(crownX, crownY, crownZ), rim);
        // kotwica duha = brzeg korony, nie srodek - patrz pickPerch
        maybeSpawnSprite(level, random,
                pickPerch(level, random, rim, new BlockPos(crownX, crownY - 2, crownZ)));

        // Feniks siada na TEJ SAMEJ koronie, ale na wierzchu (patrz
        // maybeSpawnPhoenix): drzewo z duhem i ptakiem to drzewo, na ktorym jest
        // wszystko, czego potrzeba do dwoch magicznych receptur.
        maybeSpawnPhoenix(level, random,
                pickPerch(level, random, rim, new BlockPos(crownX, crownY - 2, crownZ)));
        return true;
    }

    // ------------------------------------------------------------------
    // Pien
    // ------------------------------------------------------------------

    /** Prosty pien od sadzonki w gore, {@code height} blokow. */
    private void placeTrunk(final WorldGenLevel level, final BlockPos origin, final int height) {
        for (int i = 0; i < height; i++) {
            place(level, origin.above(i), logState());
        }
    }

    /**
     * Wezel: wchodzi pod dolna krawedz korony gospodarza, wspina sie spirala
     * wokol jego pnia i konczy krotkim "dociagnieciem" do sadzonki, zeby nowy
     * pien nie wisial w powietrzu. Ruch dociagajacy idzie po osiach (bez
     * przekatnych) - dokladnie tak, jak vanilla sklada pnie.
     */
    private void placeStrangler(final WorldGenLevel level, final RandomSource random, final BlockPos origin,
            final int hostX, final int hostZ, final int fromY, final int toY) {

        double angle = random.nextDouble() * (2.0 * Math.PI);
        @Nullable BlockPos last = null;

        for (int y = fromY; y <= toY; y++) {
            final int dx = (int) Math.round(Math.cos(angle) * HELIX_RADIUS);
            final int dz = (int) Math.round(Math.sin(angle) * HELIX_RADIUS);
            final BlockPos pos = new BlockPos(hostX + dx, y, hostZ + dz);
            if (place(level, pos, logState())) {
                last = pos;
            }
            angle += HELIX_STEP;
        }

        if (last == null) {
            return;
        }
        BlockPos cursor = origin;
        for (int i = 0; i < 4 && !cursor.equals(last); i++) {
            final int stepX = Integer.compare(last.getX(), cursor.getX());
            final int stepZ = Integer.compare(last.getZ(), cursor.getZ());
            final int stepY = Integer.compare(last.getY(), cursor.getY());
            final int nextY = cursor.getY() + stepY;
            final BlockPos next = stepX != 0
                    ? new BlockPos(cursor.getX() + stepX, nextY, cursor.getZ())
                    : stepZ != 0 ? new BlockPos(cursor.getX(), nextY, cursor.getZ() + stepZ) : last;
            place(level, next, logState());
            cursor = next;
        }
    }

    // ------------------------------------------------------------------
    // Korona
    // ------------------------------------------------------------------

    /**
     * {@code center} to slupek ostatniego loga; korona schodzi w dol od niego o
     * trzy warstwy i ma czubek jeden blok wyzej.
     */
    private void placeCanopy(final WorldGenLevel level, final RandomSource random, final BlockPos center,
            final List<BlockPos> rimOut) {

        for (int layer = 0; layer < 4; layer++) {
            final int radius = layer <= 1 ? 3 : layer == 2 ? 2 : 1;
            final BlockPos base = center.below(layer);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    final boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                    if (corner && random.nextFloat() < (layer == 3 ? 0.9F : 0.45F)) {
                        continue;
                    }
                    final BlockPos pos = base.offset(dx, 0, dz);
                    if (layer >= 2 && place(level, pos, leafState())) {
                        rimOut.add(pos);
                    }
                }
            }
        }
        place(level, center.above(1), leafState());

        // "koncowki" zwisajace z krawedzi - to one sprzedaja ksztalt drzewa
        // magicznego, a nie debowego
        final int strands = 3 + random.nextInt(3);
        for (int i = 0; i < strands; i++) {
            final double a = random.nextDouble() * (2.0 * Math.PI);
            final int sx = (int) Math.round(Math.cos(a) * 3.0);
            final int sz = (int) Math.round(Math.sin(a) * 3.0);
            BlockPos pos = center.below(2).offset(sx, 0, sz);
            final int length = 1 + random.nextInt(3);
            for (int d = 0; d < length; d++) {
                pos = pos.below();
                if (!place(level, pos, leafState())) {
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Duch arkanu
    // ------------------------------------------------------------------

    /**
     * Wybor bloku, na ktorym duch wisi: interesuje nas lisc z POWIETRZEM pod
     * soba, bo to jest dolna krawedz korony - tylko tam sylwetka czyta sie jako
     * "zwisa z lisci", a nie "stoi w drzewie". Kandydaci pochodza z warstw 2-3
     * (patrz placeCanopy), wiec probujemy osiem razy, az trafimy w wolna
     * przestrzen; jak korona jest cala zbite, bierzemy ostatniego kandydata, a
     * dopiero gdy ich nie ma - punkt awaryjny dwa bloki pod szczytem.
     */
    private static BlockPos pickPerch(final WorldGenLevel level, final RandomSource random,
            final List<BlockPos> rim, final BlockPos fallback) {

        if (rim.isEmpty()) {
            return fallback;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            final BlockPos cand = rim.get(random.nextInt(rim.size()));
            if (level.getBlockState(cand.below()).isAir()) {
                return cand;
            }
        }
        return rim.get(rim.size() - 1);
    }

    /**
     * Spawn w swiecie generowanym - dokladnie ta sama sciezka, ktora idzie
     * vanilla dla krysztalow w {@code SpikeFeature}: encje tworzymy przez
     * {@code EntityType#create(level.getLevel(), reason)} i dodajemy przez
     * {@code WorldGenLevel#addFreshEntity}. {@code Mob#finalizeSpawn} jest
     * swiadomie pominiety - ten mob nie ma grupy ani ekwipunku.
     */
    private void maybeSpawnSprite(final WorldGenLevel level, final RandomSource random, final BlockPos perch) {
        if (random.nextFloat() > SPRITE_CHANCE) {
            return;
        }
        final ServerLevel serverLevel = level.getLevel();
        // Jedno drzewo = jeden duch. Sadzonka kielkuje na koronie i potrafi
        // dorzucic drugiego duha do TEJ samej korony - stad sprawdzenie, czy juz
        // ktorys wisi (inaczej mialbysmy dwa wisiorka na jednym drzewie).
        if (!serverLevel.getEntitiesOfClass(ArcaneSprite.class, new AABB(perch).inflate(7.5, 9.0, 7.5),
                ArcaneSprite::isPerched).isEmpty()) {
            return;
        }
        final @Nullable ArcaneSprite sprite =
                ModEntities.ARCANE_SPRITE.create(serverLevel, EntitySpawnReason.STRUCTURE);
        if (sprite == null) {
            return;
        }
        // perch to srodek bloku liscia; -0.3 znaczy "pod dolna krawedzia", wiec
        // duch wisi PO korona, a nie jest w niej zakopany
        sprite.snapTo(perch.getX() + 0.5, perch.getY() - 0.3, perch.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);
        sprite.startPerching();
        level.addFreshEntity(sprite);
    }

    // ------------------------------------------------------------------
    // Materialy i drobiazgi
    // ------------------------------------------------------------------

    /**
     * Gniazdo feniksa: ten sam mechanizm co przy duchu (dedupe w obrebie korony),
     * tylko encja i szansa inne. Brak dedupe dawalby para feniksow na jednym
     * drzewie i farme pior w jednym chunku.
     */
    private void maybeSpawnPhoenix(final WorldGenLevel level, final RandomSource random, final BlockPos perch) {
        if (random.nextFloat() > PHOENIX_CHANCE) {
            return;
        }
        final ServerLevel serverLevel = level.getLevel();
        if (!serverLevel.getEntitiesOfClass(Phoenix.class, new AABB(perch).inflate(9.5, 10.0, 9.5)).isEmpty()) {
            return;
        }
        final @Nullable Phoenix bird = ModEntities.PHOENIX.create(serverLevel, EntitySpawnReason.STRUCTURE);
        if (bird == null) {
            return;
        }
        // nad korona, nie pod nia: feniks siada na liscciach, a duch wisi pod
        // nimi - te dwie sylwetki na tym samym drzewie czyta sie bez bledu
        bird.snapTo(perch.getX() + 0.5, perch.getY() + 1.05, perch.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(bird);
    }

    private static boolean isLog(final BlockState state) {
        return state.is(ModBlocks.ARCANE_LOG);
    }

    private static boolean isLeaves(final BlockState state) {
        return state.is(ModBlocks.ARCANE_LEAVES);
    }

    /**
     * Znajdowanie pnia "gospodarza": od bloku pod sadzonka w dol, przez wylacznie
     * liscie; na kazdym pietrze sprawdzamy kwadrat 5x5. Pierwszy znaleziony pien
     * jest tym, wokol ktorego oplatamy.
     *
     * @return pozycja bloku pnia gospodarza albo null, jesli sadzonka stoi na
     *         ziemi - wowczas rosniemy jak zwykle drzewo
     */
    private static @Nullable BlockPos findHostTrunk(final WorldGenLevel level, final BlockPos origin) {
        BlockPos cursor = origin.below();
        for (int i = 0; i < HOST_LOOKDOWN; i++) {
            // 1.21.11: LevelHeightAccessor ma getMinY() (nie getMinBuildHeight())
            if (cursor.getY() <= level.getMinY() + 1) {
                return null;
            }
            for (int dx = -HOST_SCAN_RADIUS; dx <= HOST_SCAN_RADIUS; dx++) {
                for (int dz = -HOST_SCAN_RADIUS; dz <= HOST_SCAN_RADIUS; dz++) {
                    final BlockPos pos = cursor.offset(dx, 0, dz);
                    if (isLog(level.getBlockState(pos))) {
                        return pos;
                    }
                }
            }
            if (!isLeaves(level.getBlockState(cursor))) {
                return null;
            }
            cursor = cursor.below();
        }
        return null;
    }

    private static BlockState logState() {
        return ModBlocks.ARCANE_LOG.defaultBlockState();
    }

    /**
     * Kazdy lisc drzewa arkanskiego: persistent I distance=1.
     *
     * Persistent to warstwa dodatkowa - wlasciwego gnicia nie ma juz od kiedy
     * {@link com.wandzz.block.ArcaneLeavesBlock} nadpisuje isRandomlyTicking i
     * decaying na false. Zostaje tu, bo vanilla liczy gnicie z pary
     * DISTANCE==7 && !PERSISTENT: feature kladacy blok przez
     * LevelWriter#setBlock nie przechodzi przez onPlace/updateDistance, wiec
     * DISTANCE zostaloby 7, a kazdy stan zapisany poza ta funkcja (struktura,
     * inny mod, kopiowanie chunka) glodaloby od razu. distance=1 oznacza "lisc
     * przy pniu" - wtedy nawet sciezka vanilla nie ma o co zaczepic.
     */
    private static BlockState leafState() {
        return ModBlocks.ARCANE_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true)
                .setValue(LeavesBlock.DISTANCE, 1);
    }

    /**
     * Jedyne miejsce, ktore decyduje "czy tu mozna postawic": powietrze albo
     * {@code #replaceable_by_trees} (a wiec takze liscie - stad dlawiciel nie
     * tnie gospodarza ani nie nadpisuje wodnego loga). Flagi synchronizacji
     * ustawia juz {@code Feature#setBlock(LevelWriter, ...)}.
     */
    private boolean place(final WorldGenLevel level, final BlockPos pos, final BlockState state) {
        if (!TreeFeature.validTreePos(level, pos)) {
            return false;
        }
        this.setBlock(level, pos, state);
        return true;
    }
}
