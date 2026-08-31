package com.wandzz.world;

import com.mojang.serialization.Codec;
import com.wandzz.block.ModBlocks;
import com.wandzz.entity.ArcaneSprite;
import com.wandzz.entity.ModEntities;
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

    /** Szansa na ducha arkanu w nowej koronie. */
    private static final double SPRITE_CHANCE = 0.34;

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

        placeCanopy(level, random, new BlockPos(crownX, crownY, crownZ));
        maybeSpawnSprite(level, random, new BlockPos(crownX, crownY + 1, crownZ));
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
    private void placeCanopy(final WorldGenLevel level, final RandomSource random, final BlockPos center) {

        for (int layer = 0; layer < 4; layer++) {
            final int radius = layer <= 1 ? 3 : layer == 2 ? 2 : 1;
            final BlockPos base = center.below(layer);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    final boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                    if (corner && random.nextFloat() < (layer == 3 ? 0.9F : 0.45F)) {
                        continue;
                    }
                    place(level, base.offset(dx, 0, dz), leafState(false));
                }
            }
        }
        place(level, center.above(1), leafState(false));

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
                if (!place(level, pos, leafState(true))) {
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Duch arkanu
    // ------------------------------------------------------------------

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
        final @Nullable ArcaneSprite sprite =
                ModEntities.ARCANE_SPRITE.create(serverLevel, EntitySpawnReason.STRUCTURE);
        if (sprite == null) {
            return;
        }
        sprite.snapTo(perch.getX() + 0.5, perch.getY() + 0.5, perch.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);
        sprite.startPerching();
        level.addFreshEntity(sprite);
    }

    // ------------------------------------------------------------------
    // Materialy i drobiazgi
    // ------------------------------------------------------------------

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
            if (cursor.getY() <= level.getMinBuildHeight() + 1) {
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

    private static BlockState leafState(final boolean persistent) {
        return ModBlocks.ARCANE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, persistent);
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
