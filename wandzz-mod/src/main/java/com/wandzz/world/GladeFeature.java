package com.wandzz.world;

import com.mojang.serialization.Codec;
import com.wandzz.entity.ModEntities;
import com.wandzz.entity.Unicorn;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Glada jednorozcow: plama "jasnego" runa z kwiatami + 1-2 jednorozce.
 *
 * Dlaczego feature, a NIE nowy biom w overworldzie? Swiat nadziemny ma
 * wieloszumowe BiomeSource, ktorego lista biomonow NIE jest ani JSON-em, ani
 * API Fabric API - {@code BiomeModifications} doklada do biomonu feature'i,
 * carvery i spawny, ale NIE potrafi wlozyc do nadziemnego swiata nowego biomonu.
 * To jest teren mixinow albo wlasnego wymiaru. Glada daje dokladnie to, czego
 * gracz szuka: pewien, rzadki i widoczny kawaek swiata, w ktorym jednorozce sa
 * - i tylko tam, wiec "nowy biom" nadal cos znaczy.
 *
 * Feature jest stawiany na wysokosci WORLD_SURFACE_WG (patrz
 * data/wandzz/worldgen/placed_feature/unicorn_glade.json), wiec nadziemny teren
 * jest tu gotowy, a nie budowany.
 */
public class GladeFeature extends Feature<NoneFeatureConfiguration> {

    /** Kwiaty, po ktorych glade da sie poznac z daleka (wszystkie z vanilla). */
    private static final Block[] FLOWERS = {
            Blocks.WHITE_TULIP, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET, Blocks.PEONY,
    };
    /** Szansa, ze zamiast ziemi bedzie mech - to on robi "plame" widoczna z gory. */
    private static final float MOSS_CHANCE = 0.28F;
    /** Szansa na kwiatek na wolnym bloku. */
    private static final float FLOWER_CHANCE = 0.4F;

    public GladeFeature(final Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        final WorldGenLevel level = context.level();
        final RandomSource random = context.random();
        final BlockPos origin = context.origin();

        // DLACZEGO wczesniej glada sie nie respi: placement typu "heightmap" podaje
        // Y PIERWSZEGO POWIETRZA nad powierzchnia, a nie bloku tej powierzchni - a
        // ja pytalem o blok w tym punkcie, czyli zawsze o powietrze. Stad skan w dol
        // (maks. 8 blokow), zeby snieg, lod i sciolka lisci nie odcinaly glady od
        // swiata - to one sa najczestszym "powodem, dla ktorego nic nie wyszlo".
        final @Nullable BlockPos surface = findGround(level, origin);
        if (surface == null) {
            // nie ma ziemi (skaa, woda, urwisko) - nie bedziemy malowac w powietrzu
            return false;
        }

        final int radius = 5 + random.nextInt(3);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                final double dist = Math.hypot(dx, dz);
                if (dist > radius) {
                    continue;
                }
                final BlockPos groundPos = surface.offset(dx, 0, dz);
                final BlockPos above = groundPos.above();
                if (!level.getBlockState(groundPos).is(Blocks.GRASS_BLOCK)
                        || !level.getBlockState(above).isAir()) {
                    continue;
                }
                // brzeg jest "miekki": mech gestnieje ku srodkowi, stad ksztalt plamy
                final float edge = (float) (1.0 - dist / (radius + 1.0));
                if (random.nextFloat() < MOSS_CHANCE * (0.4F + edge)) {
                    this.setBlock(level, groundPos, Blocks.MOSS_BLOCK.defaultBlockState());
                    continue;
                }
                if (random.nextFloat() < FLOWER_CHANCE * (0.3F + edge)) {
                    final Block flower = FLOWERS[random.nextInt(FLOWERS.length)];
                    if (TreeFeature.validTreePos(level, above)) {
                        this.setBlock(level, above, flower.defaultBlockState());
                    }
                }
            }
        }

        spawnHerd(level, random, surface, radius);
        return true;
    }

    /**
     * Czy ten blok nadaje sie na runo glady. Celowo szeroko: mech, podzol i ziemia
     * to wlasnie te podloza, na ktorych jednorozce maja sens. W piasku albo na
     * skale plama wygladalaby jak blad generacji, wiec wole jej nie stawiac.
     */
    private static boolean isGladeGround(final BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.MOSS_BLOCK);
    }

    /**
     * Pierwszy blok ziemi w kolumnie, od punktu placementu w dol (patrz uwaga o
     * heightmap wyzej). Null = nie ma na czym postawic glady.
     */
    private static @Nullable BlockPos findGround(final WorldGenLevel level, final BlockPos from) {
        for (int dy = 0; dy >= -8; dy--) {
            final BlockPos pos = new BlockPos(from.getX(), from.getY() + dy, from.getZ());
            if (isGladeGround(level.getBlockState(pos))) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Stadko wchodzi na wolna przestrzen wewnatrz glady. Sprawdzam najpierw, czy
     * w okolicy juz ktos jest - sadzonka arkanu potrafi wygenerowac glade obok
     * drugiego, juz zasiedlonego placka (dokladnie ten sam problem co z duhami na
     * jednym drzewie, patrz {@code ArcaneStranglerFeature#maybeSpawnSprite}).
     */
    private void spawnHerd(final WorldGenLevel level, final RandomSource random,
            final BlockPos origin, final int radius) {

        if (!(level.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        final AABB area = new AABB(origin).inflate(radius + 4.0, 8.0, radius + 4.0);
        final int already = serverLevel.getEntitiesOfClass(Unicorn.class, area).size();
        final List<BlockPos> spots = new ArrayList<>();
        for (int attempt = 0; attempt < 12; attempt++) {
            final BlockPos pos = origin.offset(
                    random.nextInt(radius) - radius / 2, 1, random.nextInt(radius) - radius / 2);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isRedstoneConductor(level, pos.below())) {
                spots.add(pos);
            }
        }
        int herd = 1 + random.nextInt(2);
        for (BlockPos spot : spots) {
            if (herd <= 0 || already > 3) {
                break;
            }
            final @Nullable Unicorn unicorn = ModEntities.UNICORN.create(serverLevel, EntitySpawnReason.STRUCTURE);
            if (unicorn == null) {
                continue;
            }
            unicorn.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(unicorn);
            herd--;
        }
    }
}
