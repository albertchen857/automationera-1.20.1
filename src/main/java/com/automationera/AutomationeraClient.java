package com.automationera;

import com.automationera.keybinding.ModKeyBinding;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Identifier -> Atlas
*minecraft:textures/atlas/blocks.png -> minecraft:blocks
*[22:06:28] [Render thread/INFO] (AutomationEraClient) minecraft:textures/atlas/signs.png -> minecraft:signs
*[22:06:28] [Render thread/INFO] (AutomationEraClient) minecraft:textures/atlas/shield_patterns.png -> minecraft:shield_patterns
*[22:06:28] [Render thread/INFO] (AutomationEraClient) minecraft:textures/atlas/banner_patterns.png -> minecraft:banner_patterns
*[22:06:28] [Render thread/INFO] (AutomationEraClient) minecraft:textures/atlas/armor_trims.png -> minecraft:armor_trims
*/
public class AutomationeraClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutomationEraClient");
    @Override
    public void onInitializeClient() {
        ModKeyBinding.register();
    }
}
