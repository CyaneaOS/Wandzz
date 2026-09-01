package org.first.wandzz.client;

import net.fabricmc.api.ClientModInitializer;
import org.first.wandzz.client.input.MouseInput;

public class WandzzClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MouseInput.register();
    }


}
