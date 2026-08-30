package com.wandzz.client;

import com.wandzz.wand.WandItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

/**
 * Wejscie po stronie klienta. Przechwytuje uzycie rozdzki, aby otworzyc
 * CastingScreen zamiast standardowej akcji "use item".
 *
 * Rejestracja payloadu sieciowego (CastPayload) NIE jest tu potrzebna -
 * robimy to w common entrypoincie (Wandzz -> CastingHandler#register), wiec
 * rejestruje sie zarówno po stronie klienta, jak i serwera. Wczesniejsza
 * proba zarejestrowania "odbiornika" CastPayload po stronie klienta byla
 * bledna: to pakiet C2S, wiec ClientPlayNetworking wymagalby rejestracji w
 * playS2C() i rzucil IllegalArgumentException przy starcie.
 */
public class WandzzClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            // UseItemCallback#interact zwraca InteractionResult (nie InteractionResultHolder)
            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }
            if (!(player.getItemInHand(hand).getItem() instanceof WandItem)) {
                return InteractionResult.PASS;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.screen == null) {
                client.setScreen(new CastingScreen());
                // SUCCESS = akcja obsluzona, vanilla nie odpala "use" dla tego itemu
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }
}
