package com.automationera;

import com.automationera.keybinding.ModKeyBinding;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AutomationeraClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutomationEraClient");
    @Override
    public void onInitializeClient() {
        ModKeyBinding.register();
    }
}
