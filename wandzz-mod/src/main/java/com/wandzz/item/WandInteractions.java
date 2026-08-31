package com.wandzz.item;

import com.wandzz.core.WandCoreItem;
import net.minecraft.world.entity.player.Player;
import com.wandzz.wand.WandItem;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * Wyjmowanie rdzeni z rozdzki. Wkladanie idzie przez stol arcaniczny
 * (patrz {@code ArcaneTableBlock}, {@code WandCoreScreen} i
 * {@code WandzzNetwork#applyLoadout}) - tutaj zostal sam zwrot, zeby
 * gracz mogl odziemic rozdzke bez otwartego okna i bez "/give".
 *
 *   PPM rdzeniem (rozdzka w ktorejkolwiek rece) = wyjmi TEN rdzen z rozdzki
 *
 * Rejestracja w common entrypoincie, nie w kliencie: sklad rozdzki to data
 * component na ItemStacku, a te istnieja w tej samej postaci tylko po stronie
 * serwera. Klient dostaje rezultat przez normalna synchronizacje ekwipunku
 * (komponent jest `networkSynchronized`, wiec widza go rowniez tooltipy).
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

            // Zywica arkanu w rece: PPM = nasacenie rozdzki (+1 gniazdo, +20%
            // regenu, brama bez gestu). Przed ponizszym sprawdzeniem, bo zywica
            // rdzeniem nie jest i "PASS, nie moj klik" dla rdzeni by ja zabil.
            if (used.getItem() == ModItems.ARCANE_RESIN) {
                return infuse(player, used) ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }

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

    /**
     * Nasacenie: rozdzka w ktorejkolwiek rece + zywica w rece = +1 gniazdo na
     * rdzen, szybszy regen i brama otwierana PPM bez gestu.
     *
     * @return false, jesli nie ma co nasaczyc (brak rozdzki / juz nasaczona) -
     *         wowczas callback zwraca PASS i vanilla moze z tym stackiem zrobic
     *         swoje (na razie nic: ARCANE_RESIN nie nadpisuje Item#use)
     */
    private static boolean infuse(Player player, ItemStack resin) {
        ItemStack wand = WandItem.findWand(player);
        if (wand == null) {
            player.displayClientMessage(Component.translatable("wandzz.wand.needed"), true);
            return false;
        }
        if (!WandItem.applyResin(wand)) {
            player.displayClientMessage(Component.translatable("wandzz.wand.already_resinated"), true);
            return false;
        }
        if (!player.isCreative()) {
            resin.shrink(1);
        }
        player.displayClientMessage(Component.translatable("wandzz.wand.resinated"), true);
        return true;
    }
}
