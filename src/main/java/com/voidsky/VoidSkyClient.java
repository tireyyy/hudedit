package com.voidsky;

import com.voidsky.gui.VoidSkyScreen;
import com.voidsky.sky.SkyPlanetRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class VoidSkyClient implements ClientModInitializer {
    private static KeyBinding openMenu;

    @Override
    public void onInitializeClient() {
        VoidSkyConfig.get();
        SkyPlanetRenderer.init();
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.voidsky.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V,
            KeyBinding.Category.create(Identifier.of("voidsky", "main"))));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new VoidSkyScreen(client.currentScreen));
            }
        });
    }
}
