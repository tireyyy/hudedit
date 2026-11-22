package com.voidsky.sky;

/** Built-in scenes. Body: azimuth/elevation (deg), radius (world units at sky distance 100), type, variant. */
public enum SkyPreset {
    BLACK_HOLE(new Body(30, 22, 26, 4, 0)),
    EARTH(new Body(35, 28, 26, 0, 0)),
    VENUS(new Body(35, 28, 26, 1, 0)),
    MARS(new Body(35, 28, 26, 2, 0)),
    SATURN(new Body(35, 26, 18, 3, 0)),
    DEEP_SPACE(new Body(20, 35, 9, 2, 0), new Body(-70, 22, 12, 0, 0), new Body(150, 40, 7, 1, 0), new Body(-140, 55, 6, 3, 0));

    public record Body(float azimuth, float elevation, float radius, int type, int variant) { }

    public final Body[] bodies;

    SkyPreset(Body... bodies) {
        this.bodies = bodies;
    }

    public static SkyPreset byName(String n) {
        try { return valueOf(n); } catch (Exception e) { return BLACK_HOLE; }
    }

    public SkyPreset next() {
        SkyPreset[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
