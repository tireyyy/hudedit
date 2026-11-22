package dev.voidhotbar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Logger LOG = LoggerFactory.getLogger("VoidHotbar");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("voidhotbar.json");

    private static VoidConfig config = new VoidConfig();

    private ConfigManager() {}

    public static VoidConfig get() {
        return config;
    }

    public static void load() {
        if (Files.exists(FILE)) {
            try {
                VoidConfig loaded = GSON.fromJson(Files.readString(FILE), VoidConfig.class);
                if (loaded != null) config = loaded;
            } catch (Exception e) {
                LOG.warn("Could not read {}, using defaults", FILE, e);
                config = new VoidConfig();
            }
        }
        config.sanitize();
        save();
    }

    public static void save() {
        try {
            config.sanitize();
            Files.writeString(FILE, GSON.toJson(config));
        } catch (IOException e) {
            LOG.warn("Could not write {}", FILE, e);
        }
    }

    public static void reset() {
        config = new VoidConfig();
        save();
    }
}
