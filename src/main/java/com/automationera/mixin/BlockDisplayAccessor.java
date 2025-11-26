package com.automationera.mixin;

import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DisplayEntity.BlockDisplayEntity.class)
public interface BlockDisplayAccessor {
    @Invoker("refreshData")
    void invokeRefreshData(boolean shouldLerp, float lerpProgress);
}