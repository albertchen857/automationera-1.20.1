package com.automationera.keybinding;

import com.automationera.ui.TutorialMainScreen;
import net.minecraft.client.option.KeyBinding;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.util.InputUtil;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.lwjgl.glfw.GLFW;
import com.automationera.ui.TutorialGroupScreen;

public class ModKeyBinding {
    public static KeyBinding openTutorialKey;

    public static void register() {
        openTutorialKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automationera.open_tutorial_ui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.automationera"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openTutorialKey.wasPressed()) {
                client.setScreen(new TutorialMainScreen());
            }
        });
    }
}

