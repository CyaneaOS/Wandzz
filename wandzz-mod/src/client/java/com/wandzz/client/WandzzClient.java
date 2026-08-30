package com.wandzz.client;

import com.wandzz.network.ManaRequestPayload;
import com.wandzz.network.ManaSyncPayload;
import com.wandzz.network.OpenTablePayload;
import com.wandzz.wand.WandItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

/**
 * Wejscie po stronie klienta. Dwie role:
 *
 *  1. Przechwycenie uzycia rozdzki, aby otworzyc CastingScreen (rysowanie gestu)
 *     zamiast standardowej akcji "use item".
 *  2. Odbiorniki S2C (mana, otwarcie stolika) + HUD many.
 *
 * Rejestracja TYPOW pakietow (PayloadTypeRegistry) NIE jest tu potrzebna -
 * robimy to w common entrypoincie (Wandzz -> CastingHandler / WandzzNetwork
 * #register), wiec znaja je obie strony. Tutaj rejestrujemy tylko ODBIORNIKI
 * pakietow S2C, a te moga istniec wylacznie po stronie klienta (dla C2S byloby to
 * {@code IllegalArgumentException}).
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

        // Serwer powiedzial, jaka jest mana -> HUD ma co rysowac.
        ClientPlayNetworking.registerGlobalReceiver(ManaSyncPayload.ID,
                (payload, context) -> ManaClientState.update(payload.current(), payload.max()));

        // PPM na stoliku: to serwer decyduje i wysyla pakiet S2C -> otwieramy okno.
        ClientPlayNetworking.registerGlobalReceiver(OpenTablePayload.ID,
                (payload, context) -> Minecraft.getInstance().setScreen(new WandCoreScreen()));

        // Po wejsciu na swiat prosimy o stan many (respawn i zmiana wymiaru tez
        // tu wpadaja) - wczesniej HUD zostawal "pusty" do pierwszego rzucenia czaru.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ManaClientState.reset();
            ClientPlayNetworking.send(new ManaRequestPayload());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ManaClientState.reset());

        WandzzHud.register();
    }
}
