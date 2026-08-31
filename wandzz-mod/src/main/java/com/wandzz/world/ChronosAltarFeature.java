package com.wandzz.world;

import com.mojang.serialization.Codec;
import com.wandzz.block.ModBlocks;
import com.wandzz.entity.ChronosBoss;
import com.wandzz.entity.ModEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Oltarz Chronosa w Arkanum: 9x9 podest z obsydianu, piec filarow i boss na
 * srodku. Struktura jest stawiana blokiem po bloku (jak korona dlawiciela), a
 * nie z szablonu NBT - {@code worldgen/structure} + {@code template_pool}
 * wymagaloby binarnego .nbt w repo, czego nie da sie uczciwie zrecenzowac.
 *
 * Arkanum ma jeden biome ({@code data/wandzz/worldgen/biome/arcane_forest.json}),
 * w ktorego {@code features[4]} (surface_structures) ten feature siedzi - to
 * jedyny sposob, zeby cos generatedo wlasnego wymiaru w ogole sie w nim pojawilo:
 * {@code BiomeModifications} dotyczy biomonow vanilla.
 */
public class ChronosAltarFeature extends Feature<NoneFeatureConfiguration> {

    /** Promien podestu = 4, czyli 9x9. */
    private static final int PAD = 4;
    /** ile wolnego powietrza nad oltarzem, zeby boss nie ugrzazl w skale. */
    private static final int AIR_HEIGHT = 6;

    public ChronosAltarFeature(final Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        final WorldGenLevel level = context.level();
        final RandomSource random = context.random();
        final BlockPos origin = context.origin();

        // podest
        for (int dx = -PAD; dx <= PAD; dx++) {
            for (int dz = -PAD; dz <= PAD; dz++) {
                this.setBlock(level, origin.offset(dx, 0, dz), Blocks.OBSIDIAN.defaultBlockState());
            }
        }
        // filary w rogach + krzyz dookola srodka
        for (int corner = 0; corner < 4; corner++) {
            final int sx = (corner < 2 ? -1 : 1) * PAD;
            final int sz = (corner % 2 == 0 ? -1 : 1) * PAD;
            for (int y = 1; y <= 3; y++) {
                this.setBlock(level, origin.offset(sx, y, sz), Blocks.CRYING_OBSIDIAN.defaultBlockState());
            }
            this.setBlock(level, origin.offset(sx, 4, sz), Blocks.AMETHYST_BLOCK.defaultBlockState());
        }
        // srodek: zaro, ktore pulsuje - to jest znak, ze tu jest boss, z daleka
        this.setBlock(level, origin, ModBlocks.ARCANE_EMBER.defaultBlockState());
        for (int step = 2; step <= PAD; step += 2) {
            this.setBlock(level, origin.offset(step, 1, 0), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            this.setBlock(level, origin.offset(-step, 1, 0), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            this.setBlock(level, origin.offset(0, 1, step), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            this.setBlock(level, origin.offset(0, 1, -step), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        }

        // powietrze nad oltarzem - bez tego boss spawnowalby sie w skale, a
        // zakleszczony w bloku mob to mob, ktorego nie da sie zabic
        final List<BlockPos> free = new ArrayList<>();
        for (int y = 1; y <= AIR_HEIGHT; y++) {
            final BlockPos pos = origin.above(y);
            this.setBlock(level, pos, Blocks.AIR.defaultBlockState());
            // kandydat na start bossa: powietrze bezposrednio na podeiscie
            if (y == 1) {
                free.add(pos);
            }
        }
        return spawnBoss(level, random, free);
    }

    /**
     * Boss rodzi sie RAZ na kolumne - Arkanum ma osiem na osiem, a feature leci
     * przy kazdym ladowaniu chunka, wiec bez tego checka gracz dostalbys
     * drugiego Chronosa po przejsciu brama w tam i z powrotem.
     */
    private boolean spawnBoss(final WorldGenLevel level, final RandomSource random, final List<BlockPos> free) {
        if (free.isEmpty() || !(level.getLevel() instanceof ServerLevel serverLevel)) {
            return false;
        }
        final @Nullable BlockPos spot = free.get(random.nextInt(free.size()));
        if (spot == null) {
            return false;
        }
        if (!serverLevel.getEntitiesOfClass(ChronosBoss.class, new AABB(spot).inflate(20.0, 20.0, 20.0)).isEmpty()) {
            return true;
        }
        final @Nullable ChronosBoss boss = ModEntities.CHRONOS_BOSS.create(serverLevel, EntitySpawnReason.STRUCTURE);
        if (boss == null) {
            return false;
        }
        boss.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(boss);
        return true;
    }
}
