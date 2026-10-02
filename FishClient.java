package dev.fishclient;

import dev.fishclient.config.FishConfig;
import dev.fishclient.gui.HudEditorScreen;
import dev.fishclient.hud.FishHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punkt wejścia moda Fish Client (tylko klient).
 */
public class FishClient implements ClientModInitializer {
    public static final String MOD_ID = "fishclient";
    public static final Logger LOGGER = LoggerFactory.getLogger("Fish Client");

    /** Klawisz otwierający edytor HUD-a (domyślnie Prawy Shift, zmienisz go w Opcje -> Sterowanie). */
    public static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        FishConfig.load();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fishclient.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.fishclient"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    client.setScreen(new HudEditorScreen());
                }
            }
        });

        HudRenderCallback.EVENT.register(FishHud::render);
        LOGGER.info("Fish Client załadowany.");
    }
}
