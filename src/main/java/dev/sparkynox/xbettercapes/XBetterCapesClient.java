package dev.sparkynox.xbettercapes;

import dev.sparkynox.xbettercapes.config.CapeConfig;
import dev.sparkynox.xbettercapes.gui.CapeSelectScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class XBetterCapesClient implements ClientModInitializer {

    public static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        // Load saved config
        CapeConfig.load();

        // Register G keybind
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.xbettercapes.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.xbettercapes"
        ));

        // Single tick listener — only opens screen, no heavy work
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openGuiKey.wasPressed()) {
                client.setScreen(new CapeSelectScreen());
            }
        });
    }
}
