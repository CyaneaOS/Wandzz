package com.wandzz.world;

import com.wandzz.block.ArcaneEmberBlock;
import com.wandzz.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logika bramy do wymiaru {@code wandzz:arkanum}.
 *
 * Sciezka gracza: znajdz Arkanny Zar w jeziorze lawy -> rzuc wandzz:open_gate
 * (gest "brama") patrzac w niego -> zar zapala sie i otwiera pare w Arkanum ->
 * PPM w zapalony zar = przejscie; PPM w zar po drugiej stronie = powrot.
 *
 * Dwie decyzje projektowe warte odnotowania:
 *
 *  1. Polaczenie jest LICZONE z pozycji, nie czytane z pliku. 1 blok w Arkanum
 *     odpowiada 8 w swiecie nadziemnym (jak w Netherze), czyli
 *     {@code (x, z) -> (floor(x/8), floor(z/8))}. Dzieki temu nie ma tu
 *     BlockEntity ani SavedData - ten drugi wymaga w 1.21.11 {@code
 *     SavedDataType} wraz z {@code DataFixTypes}, a to zbedny kawalek API do
 *     obslugi jednej pary wspolrzednych,
 *     a brama dziala rowniez po restarcie serwera: cache ({@link #LINKS}) jest
 *     tylko optymalizacja, a "zimny start" i tak trafia w to samo miejsce albo
 *     znajduje istniejaca bramke skanem (patrz {@link #reconnect}).
 *  2. Powrot jest ZA DARMO (brak kosztu many). Awaryjne wyjscie musi byc
 *     zawsze osiagalne - inaczej brak many = mecz w obcym wymiarze.
 *
 * Wszystko tutaj jest po stronie serwera (teleporty nie sa predykcyjne).
 */
public final class GateService {

    /** Skala bramy: 1 blok w Arkanum = 8 w swiecie. */
    private static final int SCALE = 8;
    /** Platforma w Arkanum to (2*R+1)^2 desek arkanskich. */
    private static final int PLATFORM_RADIUS = 2;
    /** Przeszukiwanie Arkanum pod katem istniejacej bramy (w blokach). */
    private static final int REUSE_RADIUS = 3;

    /**
     * Cache par ( obie strony). Klucz = (wymiar, pozycja) zarzu
     * w danym stanie ZAPALONYM. Swiadomie {@code Map} bez serializacji: patrz
     * punkt 1 w javadoku klasy - {@link #reconnect} odtwarza to samo.
     */
    private static final Map<Link, Link> LINKS = new ConcurrentHashMap<>();

    private GateService() {
    }

    /** Para (wymiar, pozycja) - wspolrzedne sa bezwzgledne, blok musi byc zarzem. */
    public record Link(ResourceKey<Level> dimension, int x, int y, int z) {

        static Link of(final Level level, final BlockPos pos) {
            return new Link(level.dimension(), pos.getX(), pos.getY(), pos.getZ());
        }

        BlockPos pos() {
            return new BlockPos(this.x, this.y, this.z);
        }
    }

    // ------------------------------------------------------------------
    // Odpalanie bramy (wola to OpenGateSpell, mana jest juz pobrana)
    // ------------------------------------------------------------------

    /**
     * @return true jesli zar zapalil sie i brama dziala (false = zar byl juz
     *         zapalony albo nie udalo sie polaczyc z drugim wymiarem)
     */
    public static boolean ignite(final ServerLevel level, final BlockPos pos, final BlockState state,
            final ServerPlayer player) {

        if (state.getValue(ArcaneEmberBlock.LIT)) {
            tell(player, "wandzz.gate.lit");
            return false;
        }

        level.setBlockAndUpdate(pos, state.setValue(ArcaneEmberBlock.LIT, true));
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0f, 0.85f);
        level.sendParticles(ParticleTypes.PORTAL,
                pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 60, 0.5, 0.5, 0.5, 0.2);

        Link from = Link.of(level, pos);
        @Nullable Link to = inArkanum(level)
                ? emergencyExit(level.getServer(), from)
                : openArkanum(level.getServer(), from);

        if (to == null) {
            // Odwroc zmiany stanu: bez drugiego konca zapalony zar to tylko dekoracja.
            level.setBlockAndUpdate(pos, state.setValue(ArcaneEmberBlock.LIT, false));
            tell(player, "wandzz.gate.no_destination");
            return false;
        }

        LINKS.put(from, to);
        LINKS.put(to, from);
        tell(player, inArkanum(level) ? "wandzz.gate.escape" : "wandzz.gate.opened");
        return true;
    }

    /**
     * Drugi koniec dla bramy ze swiata nadziemnego: istnieje -> uzyj, nie ma ->
     * zbuduj platforme 5x5 z desek arkanskich i zapalony zar w srodku.
     */
    private static @Nullable Link openArkanum(final @Nullable MinecraftServer server, final Link from) {
        if (server == null) {
            return null;
        }
        ServerLevel dest = server.getLevel(ModWorldgen.ARKANUM_LEVEL);
        if (dest == null) {
            return null;
        }

        int dx = Mth.floor(from.x() / (float) SCALE);
        int dz = Mth.floor(from.z() / (float) SCALE);
        int ground = dest.getHeight(Heightmap.Types.MOTION_BLOCKING, dx, dz);

        @Nullable BlockPos existing = findEmberNear(dest, dx, dz, ground);
        if (existing != null) {
            return Link.of(dest, existing);
        }

        for (int x = dx - PLATFORM_RADIUS; x <= dx + PLATFORM_RADIUS; x++) {
            for (int z = dz - PLATFORM_RADIUS; z <= dz + PLATFORM_RADIUS; z++) {
                dest.setBlockAndUpdate(new BlockPos(x, ground - 1, z), ModBlocks.ARCANE_PLANKS.defaultBlockState());
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos air = new BlockPos(x, ground + dy, z);
                    if (!dest.isEmptyBlock(air)) {
                        dest.setBlockAndUpdate(air, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        BlockPos gate = new BlockPos(dx, ground, dz);
        dest.setBlockAndUpdate(gate, ModBlocks.ARCANE_EMBER.defaultBlockState().setValue(ArcaneEmberBlock.LIT, true));
        return Link.of(dest, gate);
    }

    /**
     * Awaryjne wyjscie: odpalenie zarzu PRZENIESIONEGO do Arkanum (gracz musial
     * przyniesc blok ze soba) przenosi na powierzchnie swiata okolo
     * {@code pozycja * 8}. Bez tego najgorszy scenariusz (brama zniszczona,
     * zimny cache) = wycieczka tylko w jedna strone.
     */
    private static @Nullable Link emergencyExit(final @Nullable MinecraftServer server, final Link from) {
        if (server == null) {
            return null;
        }
        ServerLevel over = server.getLevel(Level.OVERWORLD);
        if (over == null) {
            return null;
        }
        int x = Mth.clamp(from.x() * SCALE + SCALE / 2, -29_999_984, 29_999_984);
        int z = Mth.clamp(from.z() * SCALE + SCALE / 2, -29_999_984, 29_999_984);
        int y = over.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        return new Link(Level.OVERWORLD, x, y, z);
    }

    // ------------------------------------------------------------------
    // Przejscie przez brame
    // ------------------------------------------------------------------

    public static void travel(final ServerLevel level, final BlockPos pos, final ServerPlayer player) {
        Link here = Link.of(level, pos);
        @Nullable Link other = LINKS.get(here);
        if (other == null) {
            other = reconnect(level, here);
        }
        if (other == null) {
            tell(player, "wandzz.gate.broken");
            return;
        }

        MinecraftServer server = level.getServer();
        ServerLevel dest = server == null ? null : server.getLevel(other.dimension());
        if (dest == null) {
            tell(player, "wandzz.gate.missing", other.dimension().location().toString());
            return;
        }

        // Na zarze nie stoi sie "w nim", tylko nad nim; adjustSpawnLocation to
        // vanilla helper, ktory przesuwa na najblizsze wolne miejsce (bez dudzenia
        // w sciene, jesli ktos zdazyl cos postawic po drugiej stronie).
        BlockPos standing = other.pos().above();
        if (!dest.isEmptyBlock(standing) || !dest.isEmptyBlock(standing.above())) {
            standing = player.adjustSpawnLocation(dest, standing);
        }

        Entity arrived = player.teleport(new TeleportTransition(
                dest,
                Vec3.atBottomCenterOf(standing),
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET)));

        dest.sendParticles(ParticleTypes.REVERSE_PORTAL,
                standing.getX() + 0.5, standing.getY() + 0.6, standing.getZ() + 0.5, 30, 0.4, 0.6, 0.4, 0.05);
        dest.playSound(null, standing, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.6f, 1.1f);

        if (arrived instanceof ServerPlayer sp) {
            tell(sp, "wandzz.gate.arrived", prettyLevel(dest.dimension().location()));
        }
    }

    /**
     * "Zimny start": po restarcie serwera {@link #reconnect} jako jedyny zna
     * druga strone. Odtwarzamy polaczenie tym samym wzorem co przy zapalaniu, a
     * jesli w swiecie nie ma juz zapalonego zarzu (zalany lawa, wykopany) -
     * zostaje powierzchnia spod {@code *8}.
     */
    private static @Nullable Link reconnect(final ServerLevel level, final Link here) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return null;
        }

        if (inArkanum(level)) {
            ServerLevel over = server.getLevel(Level.OVERWORLD);
            if (over == null) {
                return null;
            }
            int baseX = here.x() * SCALE;
            int baseZ = here.z() * SCALE;
            for (int x = baseX; x < baseX + SCALE; x++) {
                for (int z = baseZ; z < baseZ + SCALE; z++) {
                    @Nullable BlockPos ember = findEmberColumn(over, x, z);
                    if (ember != null) {
                        Link found = Link.of(over, ember);
                        LINKS.put(here, found);
                        LINKS.put(found, here);
                        return found;
                    }
                }
            }
            return emergencyExit(server, here);
        }

        @Nullable Link target = openArkanum(server, here);
        if (target != null) {
            LINKS.put(here, target);
            LINKS.put(target, here);
        }
        return target;
    }

    /** Zar w kolumnie (x, z) w pasmie generowania Feature'u (patrz placed_feature). */
    private static @Nullable BlockPos findEmberColumn(final ServerLevel level, final int x, final int z) {
        for (int y = -52; y <= 8; y += 2) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.ARCANE_EMBER) && state.getValue(ArcaneEmberBlock.LIT)) {
                return pos;
            }
        }
        return null;
    }

    private static @Nullable BlockPos findEmberNear(final ServerLevel level, final int cx, final int cz,
            final int ground) {
        for (int x = cx - REUSE_RADIUS; x <= cx + REUSE_RADIUS; x++) {
            for (int z = cz - REUSE_RADIUS; z <= cz + REUSE_RADIUS; z++) {
                for (int y = ground - 3; y <= ground + 4; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(ModBlocks.ARCANE_EMBER)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------

    private static boolean inArkanum(final Level level) {
        return ModWorldgen.ARKANUM_LEVEL.equals(level.dimension());
    }

    private static String prettyLevel(final Identifier id) {
        return id.getNamespace() + ":" + id.getPath();
    }

    private static void tell(final ServerPlayer player, final String key, final Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }
}
