package com.hudedit;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HudEditClient implements ClientModInitializer {
    /** DrawContext текущего кадра (запоминается в InGameHudMixin). */
    public static DrawContext context;

    // состояние миксинов
    public static boolean bossPushed = false;
    public static boolean chatPushed = false;
    public static int chatDepth = 0;

    public static KeyBinding openKey;

    private static Field barsField;

    @Override
    public void onInitializeClient() {
        HudConfig.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hudedit.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null && client.world != null) {
                    client.setScreen(new HudEditScreen());
                }
            }
        });
    }

    /** Достаёт список боссбаров из BossBarHud (по типу поля, без привязки к имени). */
    @SuppressWarnings("unchecked")
    public static Collection<ClientBossBar> bossBars(BossBarHud hud) {
        try {
            if (barsField == null) {
                for (Field f : BossBarHud.class.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        barsField = f;
                        break;
                    }
                }
            }
            if (barsField != null && hud != null) {
                Map<UUID, ClientBossBar> map = (Map<UUID, ClientBossBar>) barsField.get(hud);
                if (map != null) return map.values();
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }
}
