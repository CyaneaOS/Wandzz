package com.wandzz.network;

import com.wandzz.core.CoreType;
import com.wandzz.mana.ManaAttachments;
import com.wandzz.mana.ManaComponent;
import com.wandzz.spell.Spell;
import com.wandzz.spell.SpellRegistry;
import com.wandzz.wand.WandData;
import com.wandzz.wand.WandItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Odbiera CastPayload i wykonuje ostatnia czesc diagramu:
 *
 *   Spell -> Core'y + Mana -> CAST
 *
 * Klient jedynie ROZPOZNAJE gest (dla plynnosci/od razu widocznego feedbacku),
 * ale to serwer ostatecznie sprawdza, czy rozdzka gracza ma odpowiedni core
 * i czy starcza many, zanim faktycznie zastosuje efekt.
 */
public final class CastingHandler {

    private CastingHandler() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(CastPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleCast(player, payload.spellId()));
        });
    }

    private static void handleCast(ServerPlayer player, String spellId) {
        Spell spell = SpellRegistry.get(spellId).orElse(null);
        if (spell == null) return;

        ItemStack wandStack = player.getMainHandItem();
        if (!(wandStack.getItem() instanceof WandItem)) {
            wandStack = player.getOffhandItem();
        }
        if (!(wandStack.getItem() instanceof WandItem)) return;

        WandData wandData = WandItem.getData(wandStack);
        boolean hasRequiredCore = wandData.cores().stream()
                .anyMatch(spell::isProvidedBy);
        if (!hasRequiredCore) {
            return; // rozdzka nie ma odpowiedniego core'a - brak efektu
        }

        ManaComponent mana = player.getAttachedOrCreate(ManaAttachments.MANA);
        if (!mana.has(spell.manaCost())) {
            return; // za malo many
        }

        player.setAttached(ManaAttachments.MANA, mana.spend(spell.manaCost()));
        spell.cast((net.minecraft.server.level.ServerLevel) player.level(), player);
    }

    /** Wywolywane co tick serwera - regeneracja many z uwzglednieniem modyfikatorow core'ow. */
    public static void tickManaRegen(ServerPlayer player) {
        ManaComponent mana = player.getAttachedOrCreate(ManaAttachments.MANA);
        if (mana.current() >= mana.max()) return;

        double multiplier = 1.0;
        ItemStack wandStack = player.getMainHandItem();
        if (wandStack.getItem() instanceof WandItem) {
            WandData data = WandItem.getData(wandStack);
            multiplier = data.cores().stream()
                    .mapToDouble(CoreType::manaRegenMultiplier)
                    .max().orElse(1.0);
        }

        double perTick = (ManaComponent.DEFAULT_REGEN_PER_SECOND * multiplier) / 20.0;
        player.setAttached(ManaAttachments.MANA, mana.regen(perTick));
    }
}
