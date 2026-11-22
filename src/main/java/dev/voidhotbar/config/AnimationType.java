package dev.voidhotbar.config;

public enum AnimationType {
    SLIDE, FADE, SCALE, GLOW, NONE;

    public String translationKey() {
        return "voidhotbar.anim." + name().toLowerCase();
    }

    public AnimationType next() {
        AnimationType[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
