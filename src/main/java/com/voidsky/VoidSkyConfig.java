package com.voidsky;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persistent settings (config/voidsky.json). Loaded once, saved on change. */
public final class VoidSkyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("voidsky.json");
    private static VoidSkyConfig instance;

    public boolean enabled = true;
    /** Always-black space sky, independent of the day/night cycle. */
    public boolean blackSky = true;
    public String preset = "BLACK_HOLE";
    public String quality = "BALANCED";
    public float sizeScale = 1.0f;
    public float rotationSpeed = 1.0f;
    public float cloudSpeed = 1.0f;

    /** True when the vanilla sky must be replaced (used by the mixins). */
    public boolean spaceMode() {
        return enabled && blackSky;
    }

    public static VoidSkyConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static VoidSkyConfig load() {
        try {
            if (Files.exists(FILE)) {
                VoidSkyConfig c = GSON.fromJson(Files.readString(FILE), VoidSkyConfig.class);
                if (c != null) return c;
            }
        } catch (Exception ignored) { }
        VoidSkyConfig c = new VoidSkyConfig();
        c.save();
        return c;
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException ignored) { }
    }
}
