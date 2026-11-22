package dev.voidhotbar.render;

import dev.voidhotbar.config.VoidConfig;

import java.util.List;

public final class ColorManager {
    public record Preset(String id, String name, int main, int active, int glow) {}

    public static final List<Preset> PRESETS = List.of(
            new Preset("void_purple", "Void Purple", 0x15151B, 0x7C5CC4, 0x7C5CC4),
            new Preset("crimson",     "Crimson",     0x161214, 0xB02A3C, 0xB02A3C),
            new Preset("ice",         "Ice",         0x111519, 0x4A9FCB, 0x4A9FCB),
            new Preset("emerald",     "Emerald",     0x111614, 0x2FA36B, 0x2FA36B),
            new Preset("pure_black",  "Pure Black",  0x000000, 0x3A3A3A, 0x555555)
    );

    private ColorManager() {}

    public static void apply(VoidConfig cfg, Preset p) {
        cfg.preset = p.id();
        cfg.mainColor = p.main();
        cfg.activeColor = p.active();
        cfg.glowColor = p.glow();
    }

    public static int indexOf(String id) {
        for (int i = 0; i < PRESETS.size(); i++) {
            if (PRESETS.get(i).id().equals(id)) return i;
        }
        return -1;
    }

    /** rgb = 0xRRGGBB, alpha 0..255 -> 0xAARRGGBB */
    public static int argb(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    public static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    /** Accepts "#RRGGBB", "RRGGBB" or "R,G,B" / "R G B" (0-255). Returns null when invalid. */
    public static Integer parse(String text) {
        if (text == null) return null;
        String s = text.trim();
        try {
            if (s.contains(",") || s.contains(" ")) {
                String[] parts = s.split("[,\\s]+");
                if (parts.length != 3) return null;
                int r = Integer.parseInt(parts[0]);
                int g = Integer.parseInt(parts[1]);
                int b = Integer.parseInt(parts[2]);
                if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) return null;
                return (r << 16) | (g << 8) | b;
            }
            if (s.startsWith("#")) s = s.substring(1);
            if (s.length() != 6) return null;
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
