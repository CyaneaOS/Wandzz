package com.wandzz;

import com.wandzz.block.ModBlocks;
import com.wandzz.entity.ModEntities;
import com.wandzz.item.ModComponents;
import com.wandzz.item.ModItemGroups;
import com.wandzz.item.ModItems;
import com.wandzz.item.WandInteractions;
import com.wandzz.mana.ManaAttachments;
import com.wandzz.network.CastingHandler;
import com.wandzz.network.WandzzNetwork;
import com.wandzz.spell.Spells;
import com.wandzz.world.ModWorldgen;
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
        ModBlocks.bootstrap();    // bloki przed ich BlockItemami
        ModItems.bootstrap();
        ModEntities.bootstrap();   // encja + atrybuty (przed zakladka kreatywna)
        ModItemGroups.bootstrap(); // zakladka kreatywna - po rejestracji przedmiotow
        Spells.bootstrap();
        ModWorldgen.bootstrap(); // wtrysniecie zaru lawy do biomesow (musi byc po blokach)
        ManaAttachments.bootstrap();
        CastingHandler.register();
        WandzzNetwork.register();  // typy pakietow many/stolika musza byc znane obu stronom
        WandInteractions.register(); // PPM rdzeniem = zwrot rdzenia z rozdzki

        // Regeneracja many wszystkich graczy co tick serwera.
        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerList().getPlayers()
                        .forEach(CastingHandler::tickManaRegen));
    }
}
