package com.voidsky.sky;

/** Quality level: shader octave count index (0..3) and animation update rate. */
public enum Quality {
    PERFORMANCE(0, 15), BALANCED(1, 30), ULTRA(2, 60), CINEMATIC(3, 60);

    public final int level;
    public final int animFps;

    Quality(int level, int animFps) {
        this.level = level;
        this.animFps = animFps;
    }

    public static Quality byName(String n) {
        try { return valueOf(n); } catch (Exception e) { return BALANCED; }
    }
}
