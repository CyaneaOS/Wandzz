package com.wandzz.block;

import com.wandzz.world.GateService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Arkanny zar - blok, ktory generuje sie w jeziorach lawy w podziemiach
 * (data/wandzz/worldgen/configured_feature/arcane_ember.json) i dziala jako
 * kotwica bramy do wymiaru {@code wandzz:arkanum}.
 *
 * Dwa stany:
 *  lit=false - zimny, tylko swieci slabo (4). PPM = podpowiedz, ze odpala go
 *              zaklecie wandzz:open_gate (patrz OpenGateSpell).
 *  lit=true  - brama otwarta. PPM = przejscie (GateService#travel).
 *
 * Blok NIE ma BlockEntity i nie trzyma zadnych danych: polaczenie miedzy
 * stronami bram jest liczane z pozycji (1 blok w Arkanum = 8 w swiecie, jak w
 * Necie) plus pamiec w {@code GateService}, a po restarcie serwera odtwarzane
 * skanem (patrz GateService#reconnect).
 */
public class ArcaneEmberBlock extends Block {

    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public ArcaneEmberBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // Dokladnie idiom vanilli (RedstoneLampBlock): rejestracja stanu w
        // konstruktorze przez defaultBlockState(), NIE przez dostep do pola
        // stateDefinition - to ostatnie nie jest w 1.21.11 widoczne z podklasy.
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    /**
     * 1.21.11: {@code Block#use} z ItemStackiem zostalo zastapione przez
     * {@code useWithoutItem} (osobna sciezka {@code useWithItem} dla interakcji
     * przedmiotem). PPM z przedmiotem w rece schodzi wiec normalnie - np. mozna
     * postawic blok na zarze, o ile to nie on jest celem "uzyj".
     */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final BlockHitResult hitResult) {

        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && state.getValue(LIT) && player instanceof ServerPlayer serverPlayer) {
            GateService.travel((ServerLevel) level, pos, serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    /** Iskry nad otwarta brama - zeby byla widoczna z daleka w jaskini. */
    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos,
            final RandomSource random) {

        if (!state.getValue(LIT) || random.nextFloat() > 0.14f) {
            return;
        }
        double x = pos.getX() + 0.5 + random.nextDouble() - 0.5;
        double y = pos.getY() + 1.02;
        double z = pos.getZ() + 0.5 + random.nextDouble() - 0.5;
        level.addParticle(random.nextBoolean() ? ParticleTypes.END_ROD : ParticleTypes.SMALL_FLAME,
                x, y, z, 0.0, random.nextDouble() * 0.04, 0.0);
    }
}
