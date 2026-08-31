package com.wandzz.network;

import com.wandzz.core.CoreType;
import com.wandzz.mana.ManaAttachments;
import com.wandzz.mana.ManaComponent;
import com.wandzz.spell.Spell;
import com.wandzz.spell.SpellRegistry;
import com.wandzz.wand.WandData;
import com.wandzz.wand.WandItem;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 *
 * Kazde odrzucenie zgloszenia jest sygnalizowane w action barze: wczesniej
 * wszystkie sciezki bledu konczyly sie cichym {@code return}, wiec "czary nie
 * dzialaly" bez zadnej wskazowki, na ktorym etapie sie wysypalo.
 */
public final class CastingHandler {

    /** Co tyle tickow serwera wysylamy stan many do HUD-a (10 = 2 razy na sekunde). */
    private static final long MANA_SYNC_PERIOD = 10L;

    private CastingHandler() {
    }

    /**
     * Wolane z common entrypointu, wiec rejestracja payloadu trafia zarowno do
     * klienta jak i serwera (wymog PayloadTypeRegistry - obie strony musza znac
     * typ pakietu PRZED rejestracja odbiornika).
     */
    public static void register() {
        PayloadTypeRegistry.playC2S().register(CastPayload.ID, CastPayload.CODEC);

        // UWAGA: handler PlayPayloadHandler jest juz wywolywany na watku serwera,
        // wiec dodatkowe server().execute(...) jest zbedne (i dawalo latwe
        // "podwojne odlozenie" akcji o jeden tick).
        ServerPlayNetworking.registerGlobalReceiver(CastPayload.ID,
                (payload, context) -> handleCast(context.player(), payload.spellId()));
    }

    private static void handleCast(ServerPlayer player, String spellId) {
        Spell spell = SpellRegistry.get(spellId).orElse(null);
        if (spell == null) {
            tell(player, "wandzz.spell.unknown");
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        ItemStack wandStack = WandItem.findWand(player);
        if (wandStack == null) {
            tell(player, "wandzz.wand.required");
            return;
        }

        WandData wandData = WandItem.getData(wandStack);
        if (wandData.cores().isEmpty()) {
            tell(player, "wandzz.wand.empty");
            return;
        }
        if (wandData.cores().stream().noneMatch(spell::isProvidedBy)) {
            tell(player, "wandzz.spell.no_core", spell.requiredLevel());
            return;
        }

        ManaComponent mana = player.getAttachedOrCreate(ManaAttachments.MANA);
        if (!mana.has(spell.manaCost())) {
            tell(player, "wandzz.spell.not_enough_mana",
                    (int) Math.ceil(spell.manaCost()), (int) Math.floor(mana.current()));
            return;
        }

        player.setAttached(ManaAttachments.MANA, mana.spend(spell.manaCost()));
        // HUD many zyje z wlasnego pakietu S2C (attachment gracza nie jest
        // synchronizowane przez vanilla), wiec po wydaniu many wysylamy stan.
        WandzzNetwork.syncMana(player);
        spell.cast(level, player);
    }

    /**
     * Wywolywane co tick serwera - regeneracja many z uwzglednieniem modyfikatorow
     * core'ow, plus synchronizacja wartosci do HUD-a klienta.
     *
     * Sync nie idzie co tick: pasek ma 60 pikseli, wiec 2 pakiety na sekunde w
     * pelni wystarcza, a klient wygladza wskaznik wlasnym lerpem (patrz
     * ManaClientState). Ostatni pakiet leci zawsze, gdy regen dojdzie do maksimum
     * - inaczej klient zostalby na "99.9".
     */
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
        ManaComponent updated = mana.regen(perTick);
        player.setAttached(ManaAttachments.MANA, updated);

        boolean full = updated.current() >= updated.max();
        // `tickCount` (pole z Entity) - swiadomie NIE `level().getGameTime()`, bo
        // ta metoda lezy na ServerLevel/ClientLevel, a nie na Level.
        if (full || player.tickCount % MANA_SYNC_PERIOD == 0) {
            WandzzNetwork.syncMana(player);
        }
    }

    /** Krotki komunikat nad hotbarem (action bar) - bez zasmiecania czatu. */
    private static void tell(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }
}
