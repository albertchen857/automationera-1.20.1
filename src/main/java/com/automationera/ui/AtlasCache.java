package com.automationera.ui;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.texture.Sprite;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;

public enum AtlasCache implements SimpleSynchronousResourceReloadListener {
    INSTANCE;
    private static final Identifier ID = Identifier.of("automationera","fluid_sprite_loader");
    public static volatile Sprite FLUID_SPRITE; // 线程可见
    public static volatile GpuTextureView BLOCKS_VIEW;
    public static volatile boolean DIRTY = true;

    @Override public Identifier getFabricId() { return ID; }
    @Override public Collection<Identifier> getFabricDependencies() {
        return List.of(ResourceReloadListenerKeys.MODELS, ResourceReloadListenerKeys.TEXTURES);
    }

    @Override public void reload(ResourceManager manager) {
        FLUID_SPRITE = null;
        BLOCKS_VIEW  = null;
        DIRTY        = true;
    }
}
