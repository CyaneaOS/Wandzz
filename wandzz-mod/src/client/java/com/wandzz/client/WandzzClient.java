package com.wandzz.client;

import com.wandzz.network.CastPayload;
import com.wandzz.wand.WandItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResultHolder;

/**
 * Wejscie po stronie klienta. Rejestruje siec (CastPayload wysylany do
 * serwera) oraz przechwytuje uzycie rozdzki, aby otworzyc CastingScreen
 * zamiast standardowej akcji "use item".
 */
public class WandzzClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(CastPayload.ID, (payload, context) -> {
            // serwer nie wysyla tego pakietu z powrotem - zarezerwowane pod przyszly feedback
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClientSide()) {
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }
            if (!(player.getItemInHand(hand).getItem() instanceof WandItem)) {
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }

            Minecraft client = Minecraft.getInstance();
            if (client.screen == null) {
                client.setScreen(new CastingScreen());
            }
            return InteractionResultHolder.success(player.getItemInHand(hand));
        });
    }
}
