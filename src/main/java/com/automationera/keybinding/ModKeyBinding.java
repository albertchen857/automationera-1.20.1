package com.automationera.keybinding;

import com.automationera.ui.IsometricRenderState;
import com.automationera.ui.TutorialMainScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.util.InputUtil;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.lwjgl.glfw.GLFW;
import com.automationera.ui.TutorialGroupScreen;
import org.slf4j.Logger;

public class ModKeyBinding {
    public static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("AutomationEraKeyBinding");
    public static KeyBinding openTutorialKey;
    public static KeyBinding leftKey;
    public static KeyBinding rightKey;
    public static KeyBinding upKey;
    public static KeyBinding downKey;
    public static KeyBinding inKey;
    public static KeyBinding outKey;
    public static KeyBinding nextKey;
    public static KeyBinding prevKey;
    public static KeyBinding nextTKey;
    public static KeyBinding prevTKey;
    public static KeyBinding resetKey;

    public static void register() {
         openTutorialKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automationera.open_tutorial_ui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.automationera"
        ));
        leftKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automationera.tutorial_rotate_left",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_A,
                "category.automationera"
        ));
        rightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automationera.tutorial_rotate_right",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_D,
                "category.automationera"
        ));
        upKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automationera.tutorial_move_up",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_W,
                "category.automationera"
        ));
        downKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automationera.tutorial_move_down",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_S,
                "category.automationera"
        ));
        inKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automationera.tutorial_zoom_in",
                InputUtil.Type.KEYSYM,
                -1,
                "category.automationera"
        ));
        outKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automationera.tutorial_zoom_out",
                InputUtil.Type.KEYSYM,
                -1,
                "category.automationera"
        ));
        nextKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automationera.tutorial_next_step",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_EQUAL,
                "category.automationera"
        ));
        prevKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automationera.tutorial_last_step",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_MINUS,
                "category.automationera"
        ));
        nextTKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automationera.tutorial_next_tutorial",
                InputUtil.Type.KEYSYM,
                -1,
                "category.automationera"
        ));
        prevTKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automationera.tutorial_last_tutorial",
                InputUtil.Type.KEYSYM,
                -1,
                "category.automationera"
        ));
        resetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automationera.tutorial_reset",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.automationera"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openTutorialKey.wasPressed()) {
                client.setScreen(new TutorialMainScreen());
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (leftKey.wasPressed()) {
                LOGGER.info("Rf key pressed");
                Screen current = MinecraftClient.getInstance().currentScreen;
                if (current instanceof TutorialGroupScreen) {
                    ((TutorialGroupScreen) current).rotateKey(-10f);
                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (rightKey.wasPressed()) {
                LOGGER.info("Rr key pressed");
                Screen current = MinecraftClient.getInstance().currentScreen;
                if (current instanceof TutorialGroupScreen) {
                    ((TutorialGroupScreen) current).rotateKey(10f);
                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (upKey.wasPressed()) {
                LOGGER.info("Yu key pressed");
                Screen current = MinecraftClient.getInstance().currentScreen;
                if (current instanceof TutorialGroupScreen) {
                    ((TutorialGroupScreen) current).ycKey(2.5f);
                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (downKey.wasPressed()) {
                LOGGER.info("Yd key pressed");
                Screen current = MinecraftClient.getInstance().currentScreen;
                if (current instanceof TutorialGroupScreen) {
                    ((TutorialGroupScreen) current).ycKey(-2.5f);
                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (inKey.wasPressed()) {
                LOGGER.info("Zin pressed");
                Screen current = MinecraftClient.getInstance().currentScreen;
                if (current instanceof TutorialGroupScreen) {
                    ((TutorialGroupScreen) current).zoomKey(0.2f);
                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (outKey.wasPressed()) {
                LOGGER.info("Zou pressed");
                Screen current = MinecraftClient.getInstance().currentScreen;
                if (current instanceof TutorialGroupScreen) {
                    ((TutorialGroupScreen) current).zoomKey(-0.2f);
                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (nextKey.wasPressed()) {
                LOGGER.info("Next step");
                Screen current = MinecraftClient.getInstance().currentScreen;
                if (current instanceof TutorialGroupScreen) {
                    ((TutorialGroupScreen) current).nextStep();

                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (prevKey.wasPressed()) {
                LOGGER.info("Prev step");
                Screen current = MinecraftClient.getInstance().currentScreen;
                if (current instanceof TutorialGroupScreen) {
                    ((TutorialGroupScreen) current).lastStep();
                }
            }
        });ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (nextTKey.wasPressed()) {
                LOGGER.info("Next Tut");
                Screen current = MinecraftClient.getInstance().currentScreen;
                if (current instanceof TutorialGroupScreen) {
                    ((TutorialGroupScreen) current).nextTutorialKey();
                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (prevTKey.wasPressed()) {
                LOGGER.info("Prev Tut");
                Screen current = MinecraftClient.getInstance().currentScreen;
                if (current instanceof TutorialGroupScreen) {
                    ((TutorialGroupScreen) current).prevTutorialKey();
                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (resetKey.wasPressed()) {
                LOGGER.info("Restart");
                Screen current = MinecraftClient.getInstance().currentScreen;
                if (current instanceof TutorialGroupScreen) {
                    ((TutorialGroupScreen) current).resetKey();
                }
            }
        });

    }
}

