package com.wandzz;

import com.wandzz.block.ModBlocks;
import com.wandzz.item.ModComponents;
import com.wandzz.item.ModItemGroups;
import com.wandzz.item.ModItems;
import com.wandzz.item.WandInteractions;
import com.wandzz.mana.ManaAttachments;
import com.wandzz.network.CastingHandler;
import com.wandzz.recipe.ModRecipes;
import com.wandzz.spell.Spells;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wandzz - magia oparta na gestach rysowanych myszka.
 *
 * Architektura (patrz dokument projektowy):
 *   Mysz -> MouseInput -> CastingData (List<Point>) -> $1 Recognizer
 *        -> Spell -> Core'y + Mana -> CAST
 */
public class Wandzz implements ModInitializer {

    public static final String MOD_ID = "wandzz";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Wandzz: inicjalizacja systemu magii gestowej");

        ModComponents.bootstrap();
        ModRecipes.bootstrap();   // serializery przepisu musza byc w rejestrze przed data-packami
        ModBlocks.bootstrap();    // bloki przed ich BlockItemami
        ModItems.bootstrap();
        ModItemGroups.bootstrap(); // zakladka kreatywna - po rejestracji przedmiotow
        Spells.bootstrap();
        ManaAttachments.bootstrap();
        CastingHandler.register();
        WandInteractions.register(); // PPM rdzeniem = zwrot rdzenia z rozdzki

        // Regeneracja many wszystkich graczy co tick serwera.
        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerList().getPlayers()
                        .forEach(CastingHandler::tickManaRegen));
    }
}
