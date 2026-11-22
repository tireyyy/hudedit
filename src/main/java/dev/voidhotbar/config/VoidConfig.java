package dev.voidhotbar.config;

/** Plain data holder, serialized to config/voidhotbar.json by Gson. Colors are 0xRRGGBB. */
public final class VoidConfig {
    public boolean enabled = true;
    public String preset = "void_purple";

    public int mainColor = 0x15151B;
    public int activeColor = 0x7C5CC4;
    public int glowColor = 0x7C5CC4;

    public float opacity = 0.85f;   // 0.2 .. 1.0
    public int radius = 3;          // 0 .. 8
    public float size = 1.0f;       // 0.8 .. 1.25
    public int spacing = 2;         // 0 .. 8

    public float animSpeed = 1.0f;  // 0.25 .. 3.0
    public AnimationType animation = AnimationType.SLIDE;

    public void sanitize() {
        opacity = clamp(opacity, 0.2f, 1.0f);
        radius = Math.max(0, Math.min(8, radius));
        size = clamp(size, 0.8f, 1.25f);
        spacing = Math.max(0, Math.min(8, spacing));
        animSpeed = clamp(animSpeed, 0.25f, 3.0f);
        if (animation == null) animation = AnimationType.SLIDE;
        if (preset == null) preset = "custom";
        mainColor &= 0xFFFFFF;
        activeColor &= 0xFFFFFF;
        glowColor &= 0xFFFFFF;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
