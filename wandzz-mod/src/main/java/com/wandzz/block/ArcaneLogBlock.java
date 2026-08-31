package com.wandzz.block;

import com.wandzz.entity.ArcaneSprite;
import com.wandzz.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Pien arkanskiego drzewa, ktory SAM odpowiada na toporek.
 *
 * Zywice arkanu zdobywa sie przez OKOROWYWANIE, nie przez zabijanie ducha:
 * prawy klik toporkiem w {@code wandzz:arcane_log} zdejmuje korke - blok
 * zamienia sie w {@code wandzz:arcane_log_stripped} (czyli deski zostaje,
 * da sie je zpowrotem postawic i spalic jak zwykle drewno), a z rany wyplywa
 * zywica. To jest ten sam ksztolt mechaniki co vanilla {@code AxeItem}: vanilla
 * robi to przez statyczna mape {@code StripableLog} ktorej MOD nie rozszerzy
 * bez access widenera, wiec zamiast walczyc z mapa reaguje na PPM po stronie
 * BLOKU (patrz {@code ArcaneEmberBlock}: vanilla pyta blok, zanim da klikniecie
 * przedmiotowi, wiec nasza reakcja wygrywa z "nie mam tu czego obdrapac".)
 *
 * Dwa szczegoly, ktore trzymaja to w kupie:
 * <ul>
 *   <li><b>Duch w koronie = gwarantowana zywica.</b> Jesli w promieniu tego pnia
 *       wisi {@link ArcaneSprite}, szansa wynosi 100% (plus 25% na druga krople);
 *       bez ducha to 45%. Zabicie ducha wiec NIE jest droga do zywicy - jest
 *       sposobem na jej utrate, i o to chodzilo.</li>
 *   <li><b>Jedno okorowanie = jeden blok.</b> Licznik "ile razy mozna zbic z tego
 *       pnia" nie istnieje swiadomie: stan nosi sam blok (okorowany albo nie),
 *       wiec nie ma czego desynchronizowac ani zapisywac.</li>
 * </ul>
 */
public class ArcaneLogBlock extends RotatedPillarBlock {

    /** Szansa na krople z pnia, ktory nie ma ducha w koronie. */
    private static final float RESIN_CHANCE = 0.45F;
    /** Druga kropla, jesli duch wisi - nagroda za to, ze zyzego sie nie tyka. */
    private static final float SECOND_DROP_CHANCE = 0.25F;

    public ArcaneLogBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
            final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {

        // Tylko toporek; cokolwiek innego (w tym rozdzka!) musi dostac PASS, zeby
        // nie zjesc PPM sciezce gestu - to jest te samo ograniczenie co w emberze.
        if (!stack.is(ItemTags.AXES)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            // SUCCESS na kliencie = machniecie loretnia; stan zmienia wylacznie
            // serwer. To NIE jest "sukces, ktory nic nie robi" (zakazany, patrz
            // ArcaneEmberBlock) - serwer faktycznie obdziera piec.
            return InteractionResult.SUCCESS;
        }

        final boolean withSpirit = hasSpiritsPerchedNearby(level, pos);
        final boolean yields = withSpirit || level.getRandom().nextFloat() < RESIN_CHANCE;

        level.setBlockAndUpdate(pos, ModBlocks.ARCANE_LOG_STRIPPED.defaultBlockState()
                .setValue(AXIS, state.getValue(AXIS)));

        if (yields) {
            final int count = withSpirit && level.getRandom().nextFloat() < SECOND_DROP_CHANCE ? 2 : 1;
            final ItemEntity drop = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 0.45, pos.getZ() + 0.5,
                    new ItemStack(ModItems.ARCANE_RESIN, count));
            // bez opoznienia: zrywamy kropelke i ma wpasc do ekwipunku, nie w otchlan
            level.addFreshEntity(drop);
        } else {
            player.displayClientMessage(Component.translatable("wandzz.log.stripped_dry"), true);
        }

        level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        if (!player.hasInfiniteMaterials()) {
            stack.hurtAndBreak(1, player, hand);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Czy nad tym pniem wisi zywy duch arkanu. Przeswietlony jest slup
     * 11x29x11 - korona bywa przesunieta wzgledem pnia (dlawiciele rosna krzywo),
     * a sam duch wisi pod krawedzia, nie nad srodkiem.
     */
    private static boolean hasSpiritsPerchedNearby(final Level level, final BlockPos pos) {
        final AABB area = new AABB(pos).inflate(5.5, 14.0, 5.5);
        return !level.getEntitiesOfClass(ArcaneSprite.class, area, ArcaneSprite::isPerched).isEmpty();
    }
}
