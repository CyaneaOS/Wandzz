package com.wandzz.item;

import com.wandzz.core.CoreType;
import com.wandzz.core.WandCoreItem;
import com.wandzz.wand.WandItem;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Wkladanie i wyciaganie core'ow bez zadnego GUI - przez uzycie przedmiotu.
 *
 *   core w rece glownej + rozdzka w drugiej -> PPM   = wloz core
 *   to samo + SHIFT                        -> PPM   = wyjmi core
 *
 * Rejestracja w common entrypoincie (nie w kliencie!), bo sklad rozdzki to
 * data component na ItemStacku - a te sa zrodlem prawdy tylko po stronie
 * serwera. Klient dostaje wynik przez normalna synchronizacja ekwipunku.
 *
 * Dlaczego nie PPM trzymajac rozdzke w glownej rece? Bo ten klik przechwytuje
 * WandzzClient (otwiera ekran rysowania gestu), wiec wlascicielem interakcji
 * jest przedmiot trzymany w glownej rece - stad core wlasnie tam.
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

            ItemStack wand = otherHand(player, hand);
            if (!(wand.getItem() instanceof WandItem)) {
                player.displayClientMessage(Component.translatable("wandzz.wand.needed"), true);
                return InteractionResult.FAIL;
            }

            CoreType core = coreItem.coreType();
            boolean removing = player.isShiftKeyDown();
            boolean done = removing ? WandItem.removeCore(wand, core) : WandItem.insertCore(wand, core);
            if (!done) {
                player.displayClientMessage(Component.translatable(
                        removing ? "wandzz.wand.no_such_core" : "wandzz.wand.full"), true);
                return InteractionResult.FAIL;
            }

            if (!removing && !player.isCreative()) {
                used.shrink(1);
            }
            player.displayClientMessage(Component.translatable(
                    removing ? "wandzz.wand.core_removed" : "wandzz.wand.core_inserted"), true);
            return InteractionResult.SUCCESS;
        });
    }

    private static ItemStack otherHand(Player player, InteractionHand hand) {
        return player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND);
    }
}
