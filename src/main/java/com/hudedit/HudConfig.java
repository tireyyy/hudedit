package com.hudedit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Конфиг хранится в .minecraft/config/hudedit.json */
public final class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HudConfig instance = new HudConfig();

    public HudElement bossBar = new HudElement();
    public HudElement chat = new HudElement();

    public static HudConfig get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("hudedit.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                HudConfig loaded = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), HudConfig.class);
                if (loaded != null) {
                    if (loaded.bossBar == null) loaded.bossBar = new HudElement();
                    if (loaded.chat == null) loaded.chat = new HudElement();
                    loaded.bossBar.sanitize();
                    loaded.chat.sanitize();
                    instance = loaded;
                }
            }
        } catch (Exception e) {
            System.err.println("[HudEdit] Failed to load config: " + e);
        }
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[HudEdit] Failed to save config: " + e);
        }
    }
}
