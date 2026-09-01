package org.first.wandzz;

import net.fabricmc.api.ModInitializer;
import org.first.wandzz.item.ModItems;

public class Wandzz implements ModInitializer {

    @Override
    public void onInitialize() {

        ModItems.registerItems();
    }
}
