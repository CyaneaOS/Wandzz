package com.wandzz.network;

import com.wandzz.core.CoreType;
import com.wandzz.mana.AttunementComponent;
import com.wandzz.mana.ManaAttachments;
import com.wandzz.mana.ManaComponent;
import com.wandzz.spell.Spell;
import com.wandzz.spell.SpellRegistry;
import com.wandzz.wand.WandData;
import com.wandzz.wand.WandItem;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
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

        // Cel/warunki musza byc sprawdzone PRZED platnoscia za mane - inaczej
        // samo celowanie "w nic" kosztowaloby 40 many (patrz OpenGateSpell).
        if (!spell.canCast(level, player)) {
            return;
        }

        // Zgranie (patrz AttunementComponent): rabat liczony PRZED proba platnosci,
        // bo komunikat "za malo many" ma mowic tyle, ile faktycznie trzeba.
        AttunementComponent attune = player.getAttachedOrCreate(ManaAttachments.ATTUNEMENT);
        int tier = attune.tier(spellId);
        double cost = spell.manaCost() * AttunementComponent.costMultiplier(tier);

        ManaComponent mana = player.getAttachedOrCreate(ManaAttachments.MANA);
        if (!mana.has(cost)) {
            tell(player, "wandzz.spell.not_enough_mana",
                    (int) Math.ceil(cost), (int) Math.floor(mana.current()));
            return;
        }

        player.setAttached(ManaAttachments.MANA, mana.spend(cost));
        AttunementComponent next = attune.afterCast(spellId);
        player.setAttached(ManaAttachments.ATTUNEMENT, next);
        // HUD many zyje z wlasnego pakietu S2C (attachment gracza nie jest
        // synchronizowane przez vanilla), wiec po wydaniu many wysylamy stan.
        WandzzNetwork.syncMana(player);
        spell.cast(level, player);

        // Wejscie na poziom musi byc slyszalne i widoczne - inaczej progresja
        // jest statystyka, ktorej nikt nie zauwaza.
        int nextTier = next.tier(spellId);
        if (nextTier > tier) {
            tell(player, "wandzz.attune.up", AttunementComponent.roman(nextTier));
            player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.35F + 0.1F * nextTier);
        }
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
            // Rozdzka nasaczona zywica arkanu (drop ducha, patrz WandData#withResin):
            // +20% regenu. Mnozone PO doborze rdzeni, a nie dodane do wartosci -
            // inaczej bonus ginie przy slabym skladzie, gdzie jest najbardziej
            // potrzebny (bez zadnego rdzenia multiplier = 1.0 * 1.2).
            if (data.resinated()) {
                multiplier *= 1.2;
            }
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
