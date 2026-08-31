package com.wandzz.network;

import com.wandzz.Wandzz;
import com.wandzz.core.CoreType;
import com.wandzz.core.WandCoreItem;
import com.wandzz.item.ModItems;
import com.wandzz.mana.ManaAttachments;
import com.wandzz.mana.ManaComponent;
import com.wandzz.wand.WandData;
import com.wandzz.wand.WandItem;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Siec wokolo many i stolika arcanicznego (sam rzut zaklecia zostal w
 * {@code CastingHandler}).
 *
 * UWAGA o kolejnosci: {@code PayloadTypeRegistry} MUSI byc wywolany po obu
 * stronach (common entrypoint), PRZED rejestracja odbiornikow - inaczej Fabric
 * rzuca {@code IllegalArgumentException}. Odbiorniki C2S sa serwerowe, a S2C
 * klienckie (patrz WandzzClient); rejestracja odbiornika S2C na serwerze jest
 * takim samym bledem jak odbiornik C2S po stronie klienta.
 */
public final class WandzzNetwork {

    /** Glowny ekwipunek gracza (36 slotow) - hotbar jest jego czescia. */
    private static final int PLAYER_SLOTS = 36;

    private WandzzNetwork() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ManaRequestPayload.ID, ManaRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(WandLoadoutPayload.ID, WandLoadoutPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ManaSyncPayload.ID, ManaSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenTablePayload.ID, OpenTablePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenBookPayload.ID, OpenBookPayload.CODEC);

        // Klient prosi o stan przy dolaczeniu (respawn / zmiana wymiaru / relog).
        ServerPlayNetworking.registerGlobalReceiver(ManaRequestPayload.ID,
                (payload, context) -> syncMana(context.player()));

        ServerPlayNetworking.registerGlobalReceiver(WandLoadoutPayload.ID,
                (payload, context) -> applyLoadout(context.player(), payload.coreIds()));

        // ...i dostaje go od razu po wejsciu na serwer, bez czekania na pierwszy rzut.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (player != null) {
                syncMana(player);
            }
        });
    }

    /** Wyslij aktualna mana do jednego gracza (client rysuje z tego HUD). */
    public static void syncMana(ServerPlayer player) {
        ManaComponent mana = player.getAttachedOrCreate(ManaAttachments.MANA);
        ServerPlayNetworking.send(player, new ManaSyncPayload(mana.current(), mana.max()));
    }

    // ------------------------------------------------------------------
    // "Zatwierdz" w oknie stolika: serwer pozostaje zrodlem prawdy
    // ------------------------------------------------------------------

    private static void applyLoadout(ServerPlayer player, List<String> requested) {
        ItemStack wand = WandItem.findWand(player);
        if (wand == null) {
            tell(player, "wandzz.wand.required");
            return;
        }

        final int capacity = WandItem.capacity(wand);
        List<CoreType> wanted = new ArrayList<>();
        for (String name : requested) {
            CoreType core = byName(name);
            if (core == null) {
                tell(player, "wandzz.wand.unknown_core", name);
                return;
            }
            if (wanted.size() < capacity) {
                wanted.add(core);
            }
        }

        // Rozkladamy zadany sklad na: "zostaje", "wraca do ekwipunku", "ma byc wziety".
        List<CoreType> toTake = new ArrayList<>(wanted);
        List<CoreType> toGiveBack = new ArrayList<>();
        for (CoreType installed : WandItem.getData(wand).cores()) {
            if (!toTake.remove(installed)) {
                toGiveBack.add(installed);
            }
        }

        boolean survival = !player.isCreative();

        if (survival) {
            // Najpierw sprawdz zasobnosc ekwipunku, zeby nie wcisnac polowy skladu.
            List<CoreType> pool = availableCores(player);
            for (CoreType core : toTake) {
                if (!pool.remove(core)) {
                    tell(player, "wandzz.wand.missing_core",
                            Component.translatable("item." + Wandzz.MOD_ID + "." + core.translationKey()));
                    return;
                }
            }

            // 1) to, co bylo w rozdzce a wypadlo, wraca do gracza
            for (CoreType core : toGiveBack) {
                ItemStack recovered = new ItemStack(ModItems.CORES.get(core));
                if (!player.getInventory().add(recovered)) {
                    player.drop(recovered, false);
                }
            }
            // 2) nowe rdzenie schodza z ekwipunku
            for (CoreType core : toTake) {
                int slot = findCoreSlot(player, core);
                if (slot >= 0) {
                    player.getInventory().removeItem(slot, 1);
                }
            }
        }

        // 3) sklad rozdzki = zatwierdzona lista (lista jest wczesniej przycinana do liczby gniazd)
        WandData before = WandItem.getData(wand);
        WandItem.setLoadout(wand, wanted);
        WandData after = WandItem.getData(wand);
        if (before.cores().equals(after.cores())) {
            tell(player, "wandzz.wand.unchanged");
            return;
        }
        player.displayClientMessage(Component.translatable("wandzz.wand.loadout_set",
                after.cores().size(), capacity), true);
    }

    private static List<CoreType> availableCores(ServerPlayer player) {
        List<CoreType> pool = new ArrayList<>();
        for (int slot = 0; slot < PLAYER_SLOTS; slot++) {
            if (player.getInventory().getItem(slot).getItem() instanceof WandCoreItem coreItem) {
                pool.add(coreItem.coreType());
            }
        }
        return pool;
    }

    private static int findCoreSlot(ServerPlayer player, CoreType core) {
        for (int slot = 0; slot < PLAYER_SLOTS; slot++) {
            if (player.getInventory().getItem(slot).getItem() instanceof WandCoreItem coreItem
                    && coreItem.coreType() == core) {
                return slot;
            }
        }
        return -1;
    }

    private static @Nullable CoreType byName(String name) {
        for (CoreType core : CoreType.values()) {
            if (core.name().equalsIgnoreCase(name)) {
                return core;
            }
        }
        return null;
    }

    private static void tell(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }
}
