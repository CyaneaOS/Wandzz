package com.wandzz;

import com.wandzz.item.ModComponents;
import com.wandzz.item.ModItems;
import com.wandzz.network.CastingHandler;
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
        ModItems.bootstrap();
        Spells.bootstrap();
        CastingHandler.register();

        // Regeneracja many wszystkich graczy co tick serwera.
        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerList().getPlayers()
                        .forEach(CastingHandler::tickManaRegen));
    }
}
