package com.wandzz.world;

import com.wandzz.block.ModBlocks;
import com.wandzz.entity.ArcaneSprite;
import com.wandzz.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Magiczne drzewa odzyskuja ducha.
 *
 * Bez tego cala sciezka "swiete patyki -> magiczna rozdzka" (patrz README, punkt
 * "Magiczna rozdzka") wysycha po pierwszym wyciu drzewa: duch jest jeden, da sie
 * zabic albo odganac, a nastepny generuje sie tylko przy nowym drzewie. Stad
 * krotka procedura raz na 20 sekund, wokol kazdego gracza:
 *
 *   wierzcholek pnia arkanskiego -> korona z lisci -> czy wisi duch?
 *     nie -> szansa 10% -> posad nowego na krawedzi korony
 *     tak -> nic nie robie
 *
 * Dlaczego skan wokol gracza, a nie lista drzew? Wlasna mapa "gdzie sa drzewa"
 * potrzebowalaby {@code SavedData}, ktora w 1.21.11 wymaga zamknietego
 * {@code DataFixTypes} (zabronione w tym projekcie), a cache w pamieci gubilby
 * dane przy restarcie i udawalaby stan, ktorego nie ma. Skan nie ma stanu,
 * wiec nie ma czego psuc; placimy za to ~12 tys. odczytow bloku na gracza raz
 * na 20 s, czyli mniej niz jeden chunk przy generowaniu.
 */
public final class SpriteRespawner {

    /** Czestotliwosc przelotu (20 s). Rzadziej = gracz nie zobaczy odrodzenia. */
    private static final int PERIOD_TICKS = 400;
    /** Szansa na powrot ducha na jednym drzewie w jednym przelocie. */
    private static final float RESPAWN_CHANCE = 0.10F;
    /** Przeswietlany slup: 25x25x19 wzgledem pozycji gracza. */
    private static final int SCAN_XZ = 12;
    private static final int SCAN_DOWN = 6;
    private static final int SCAN_UP = 12;

    private static int tickCounter;

    /** Wolane z END_SERVER_TICK, obok regeneracji many. */
    public static void tick(final MinecraftServer server) {
        if (++tickCounter % PERIOD_TICKS != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                maybeReturnToTree(level, player.blockPosition());
            }
        }
    }

    /**
     * Jedno drzewo na przelot gracza - wiecej byloby widoczne jako "las nagle
     * peknal duhami", a o to nie chodzilo.
     */
    private static void maybeReturnToTree(final ServerLevel level, final BlockPos center) {
        final RandomSource random = level.getRandom();
        for (int dy = SCAN_UP; dy >= -SCAN_DOWN; dy--) {
            for (int dx = -SCAN_XZ; dx <= SCAN_XZ; dx++) {
                for (int dz = -SCAN_XZ; dz <= SCAN_XZ; dz++) {
                    final BlockPos trunk = center.offset(dx, dy, dz);
                    if (isTrunkTop(level, trunk) && tryRespawn(level, random, trunk.above())) {
                        return;
                    }
                }
            }
        }
    }

    /** Pien, nad ktorym zaczyna sie korona - czyli ostatni blok pnia. */
    private static boolean isTrunkTop(final ServerLevel level, final BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.ARCANE_LOG)
                && !level.getBlockState(pos.above()).is(ModBlocks.ARCANE_LOG);
    }

    private static boolean tryRespawn(final ServerLevel level, final RandomSource random, final BlockPos crown) {
        final AABB treeArea = new AABB(crown).inflate(7.5, 9.0, 7.5);
        if (!level.getEntitiesOfClass(ArcaneSprite.class, treeArea, ArcaneSprite::isPerched).isEmpty()) {
            return false;
        }
        if (random.nextFloat() > RESPAWN_CHANCE) {
            return true;
        }
        final @Nullable BlockPos perch = pickPerch(level, random, crown);
        if (perch == null) {
            return true;
        }
        final @Nullable ArcaneSprite sprite = ModEntities.ARCANE_SPRITE.create(level, EntitySpawnReason.STRUCTURE);
        if (sprite == null) {
            return true;
        }
        // -0.3 = pod dolna krawedzia liscia, zeby duch wisial POD korona
        sprite.snapTo(perch.getX() + 0.5, perch.getY() - 0.3, perch.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);
        sprite.startPerching();
        level.addFreshEntity(sprite);
        return true;
    }

    /**
     * Lisc z powietrzem pod soba, 8 prob. To samo kryterium co przy generowaniu
     * drzewa (patrz {@code ArcaneStranglerFeature#pickPerch}) - inaczej duch na
     * odrodzonym drzewie wisialby srodku korony, a na starym pod nia.
     */
    private static @Nullable BlockPos pickPerch(final ServerLevel level, final RandomSource random,
            final BlockPos crownTop) {
        for (int attempt = 0; attempt < 8; attempt++) {
            final int dx = random.nextInt(7) - 3;
            final int dz = random.nextInt(7) - 3;
            for (int dy = 0; dy <= 3; dy++) {
                final BlockPos cand = crownTop.above(dy).offset(dx, 0, dz);
                final BlockState state = level.getBlockState(cand);
                if (state.is(ModBlocks.ARCANE_LEAVES) && level.getBlockState(cand.below()).isAir()) {
                    return cand;
                }
            }
        }
        return null;
    }

    private SpriteRespawner() {
    }
}
