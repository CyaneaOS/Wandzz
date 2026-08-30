package com.wandzz.block;

import com.wandzz.network.OpenTablePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Stol arcaniczny - punkt wejscia do okladania rozdzki rdzeniami.
 *
 * Blok NIE ma BlockEntity ani zadnego stanu: sklad rozdzki zyje na ItemStacku
 * rozdzki (data component), a ten jest w ekwipunku gracza. Interakcja sprowadza
 * sie wiec do wyslania jednemu graczowi pakietu "otworz okno"; cala logika
 * zmian jest po stronie serwera w {@code WandTableHandler}.
 *
 * Sneak + PPM = PASS, zeby nie zablokowac klasycznego kladzenia blokow na bloku.
 */
public class ArcaneTableBlock extends Block {

    public ArcaneTableBlock(Properties properties) {
        super(properties);
    }

    /**
     * 1.21.11: {@code Block#use(ItemStack, Level, BlockPos, BlockState, Player, BlockHitResult)}
     * zostalo zastapione przez {@code useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)}
     * (osobna sciezka dla interakcji z przedmiotem w rece).
     */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final BlockHitResult hitResult) {

        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, OpenTablePayload.INSTANCE);
        }
        // SUCCESS po obu stronach - inaczej klient predykcyjnie sprobowalby
        // postawic blok na stoliku i dostalby rollback (miganie ekwipunku).
        return InteractionResult.SUCCESS;
    }
}
