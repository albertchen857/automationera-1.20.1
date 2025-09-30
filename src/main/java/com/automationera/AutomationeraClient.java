package com.automationera;

import com.automationera.keybinding.ModKeyBinding;
import com.automationera.ui.AtlasCache;
import com.automationera.ui.TutorialManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.atlas.Atlases;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomationeraClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutomationEraClient");
    @Override
    public void onInitializeClient() {
        ModKeyBinding.register();
        net.fabricmc.fabric.api.resource.ResourceManagerHelper
                .get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(AtlasCache.INSTANCE);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!AtlasCache.DIRTY) return;

            var bmm   = client.getBakedModelManager();
            SpriteAtlasTexture atlas;
            try{
                atlas = bmm.getAtlas(Atlases.BLOCKS);
            } catch (Exception e) {
                LOGGER.warn("THROW ERROR");
                return;
            }

            if (atlas == null) return;

            AtlasCache.BLOCKS_VIEW = atlas.getGlTextureView();

            var texId  = Identifier.of("automationera","block/water_still");
            var sprite = new SpriteIdentifier(Atlases.BLOCKS, texId).getSprite();

            if (sprite.getContents().getId().equals(MissingSprite.getMissingSpriteId())) {
                LOGGER.info("atlas not ready");
                return;
            }

            AtlasCache.FLUID_SPRITE = sprite;
            AtlasCache.DIRTY = false;
        });
    }
}
