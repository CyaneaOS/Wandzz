package com.wandzz.block;

import com.wandzz.core.WandCoreItem;
import com.wandzz.world.GateService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
    /**
     * 1.21.11: {@code Block#use} z ItemStackiem zostalo zastapione przez
     * {@code useWithoutItem} (osobna sciezka {@code useItemOn} dla interakcji
     * przedmiotem).
     *
     * KOLEJNOSC klikniecia jest tu cala gra, wiec dokumentuje ja wprost. Vanilla
     * w {@code Minecraft#startUseItem} pyta BLOK zanim da klikniecie przedmiotowi:
     *   MultiPlayerGameMode#useItemOn
     *     -> BlockState#useItemOn (domyslnie TRY_WITH_EMPTY_HAND - patrz BlockBehaviour)
     *     -> BlockState#useWithoutItem (tylko MAIN_HAND i tylko gdy brak sneak+item)
     *     -> jesli wynik to Success (SUCCESS albo CONSUME) startUseItem robi
     *        "return" i gameMode#useItem NIE zostaje wywolane.
     * A to wlasnie useItem odpala UseItemCallback, ktory u nas otwiera CastingScreen
     * (ekran gestu). Dawniej ta metoda zawsze zwrala SUCCESS, wiec kazdy PPM z
     * rozdzka w rece, gdy celownik stal na zarze, kasowal rzucanie czaru - a to
     * jedyny sposob, zeby zar w ogole zapalil. Stad ponizsze priorytety:
     *
     *   sneak                         -> PASS  (chce cos postawic albo wyjac rdzen)
     *   rdzen w ktorejkolwiek rece    -> PASS  (PPM rdzenia = zwrot rdzenia, WandInteractions)
     *   lit=true                      -> nasza brama: travel + SUCCESS
     *   lit=false, obie rece puste    -> podpowiedz w action barze (nic wiecej nie ma co robic)
     *   lit=false, cos w rece         -> PASS  (klik dostaje przedmiot = rozdzka = czar)
     *   lit=false + NASACZONA rozdzka -> zaplenie od reki (CONSUME), bez gestu
     */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final BlockHitResult hitResult) {

        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (holdsCore(player)) {
            return InteractionResult.PASS;
        }

        if (!state.getValue(LIT)) {
            // Rozdzka nasaczona zywica arkanu (WandData#withResin) otwiera brame
            // SAMYM PPM, bez rysowania gestu. Kucanie + PPM wciaz trafia do
            // rozdzki, wiec normalne rzucanie wandzz:open_gate pozostaje
            // dostepne - to wazne, bo bez many skrot i tak nie zadziala.
            if (GateService.quickIgniteReady(player)) {
                if (!level.isClientSide() && level instanceof ServerLevel serverLevel
                        && player instanceof ServerPlayer serverPlayer) {
                    GateService.igniteWithInfusedWand(serverPlayer, serverLevel, pos, state);
                }
                // CONSUME, nie SUCCESS: klik obsluzony, ale bez machania reka -
                // zapalanie zaru nie jest "uzyciem przedmiotu".
                return InteractionResult.CONSUME;
            }
            boolean emptyHands = player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
            if (!emptyHands) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("wandzz.gate.hint"), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            GateService.travel((ServerLevel) level, pos, serverPlayer);
        }
        // SUCCESS po obu stronach - inaczej klient predykcyjnie sprobowalby
        // postawic blok na zarze i dostalby rollback (miganie ekwipunku).
        return InteractionResult.SUCCESS;
    }

    /** Czy w ktorejkolwiek rece jest rdzen - to on jest wlascicielem PPM (patrz wyzej). */
    private static boolean holdsCore(final Player player) {
        return player.getMainHandItem().getItem() instanceof WandCoreItem
                || player.getOffhandItem().getItem() instanceof WandCoreItem;
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
