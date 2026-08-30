package com.wandzz.item;

import com.wandzz.wand.WandItem;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * Wyjmowanie rdzeni z rozdzki. WKladanie idzie przez stol kowalski
 * (patrz {@code WandCoreSmithingRecipe}) - tutaj zostal sam zwrot, zeby
 * gracz mogl odziemic rozdzke bez GUI i bez "/give".
 *
 *   PPM rdzeniem (rozdzka w ktorejkolwiek rece) = wyjmi TEN rdzen z rozdzki
 *
 * Rejestracja w common entrypoincie, nie w kliencie: sklad rozdzki to data
 * component na ItemStacku, a te istnieja w tej samej postaci tylko po stronie
 * serwera. Klient dostaje rezultat przez normalna synchronizacje ekwipunku.
 *
 * Dlaczego nie PPM trzymajac rozdzke w rece glownej? Ten klik przechwytuje
 * WandzzClient (ekran rysowania gestu), wiec wlascicielem interakcji musi byc
 * trzymany rdzen.
 */
public final class WandInteractions {

    private WandInteractions() {
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }
            ItemStack used = player.getItemInHand(hand);
            if (!(used.getItem() instanceof WandCoreItem coreItem)) {
                return InteractionResult.PASS;
            }

            ItemStack wand = WandItem.findWand(player);
            if (wand == null) {
                player.displayClientMessage(Component.translatable("wandzz.wand.needed"), true);
                return InteractionResult.FAIL;
            }

            if (!WandItem.removeCore(wand, coreItem.coreType())) {
                player.displayClientMessage(Component.translatable("wandzz.wand.no_such_core"), true);
                return InteractionResult.FAIL;
            }

            if (!player.isCreative()) {
                // Wyjety rdzen wraca do gracza (PPM to zwrot, nie konsumpcja).
                ItemStack recovered = new ItemStack(coreItem);
                if (!player.getInventory().add(recovered)) {
                    player.drop(recovered, false);
                }
            }
            player.displayClientMessage(Component.translatable("wandzz.wand.core_removed"), true);
            return InteractionResult.SUCCESS;
        });
    }
}
