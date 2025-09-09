package com.automationera;

import com.automationera.keybinding.ModKeyBinding;
import net.fabricmc.api.ClientModInitializer;

public class AutomationeraClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModKeyBinding.register();
    }
}
