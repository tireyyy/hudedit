package dev.voidhotbar;

import com.mojang.blaze3d.platform.InputConstants;
import dev.voidhotbar.config.ConfigManager;
import dev.voidhotbar.gui.SettingsScreen;
import dev.voidhotbar.render.HotbarRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.lwjgl.glfw.GLFW;

public final class VoidHotbarClient implements ClientModInitializer {
    public static final String MOD_ID = "voidhotbar";

    private static final HotbarRenderer RENDERER = new HotbarRenderer();

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        KeyMapping openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.voidhotbar.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                mc.setScreen(new SettingsScreen(mc.screen));
            }
        });

        // Replace (not overlay) the vanilla hotbar element. Everything else vanilla draws
        // (health, food, XP, held-item name, crosshair) is untouched.
        HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, original -> (graphics, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!ConfigManager.get().enabled
                    || mc.gameMode == null
                    || mc.gameMode.getPlayerMode() == GameType.SPECTATOR
                    || !(mc.getCameraEntity() instanceof Player player)) {
                original.render(graphics, delta);
                return;
            }
            RENDERER.renderHud(graphics, delta, mc, player);
        });
    }
}
