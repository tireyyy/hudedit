package dev.voidhotbar.render;

import dev.voidhotbar.config.AnimationType;
import dev.voidhotbar.config.ConfigManager;

/**
 * Time-based (real milliseconds) selection animation, independent of FPS and game ticks.
 * One instance per hotbar being drawn (HUD, settings preview).
 */
public final class AnimationController {
    private static final float BASE_MS = 180f;
    private static final float SCALE_POP = 0.08f;

    private int selected = -1;
    private int from = -1;
    private float slideFrom;
    private long start;
    private long now;

    public void update(int newSelected, long nowMs) {
        this.now = nowMs;
        if (selected == -1) {
            selected = from = newSelected;
            slideFrom = newSelected;
            start = nowMs - 100_000L;
            return;
        }
        if (newSelected != selected) {
            slideFrom = slidePos();   // continue smoothly if interrupted mid-animation
            from = selected;
            selected = newSelected;
            start = nowMs;
        }
    }

    public int selected() {
        return selected;
    }

    /** Eased progress 0..1 of the current switch. */
    private float ease() {
        if (ConfigManager.get().animation == AnimationType.NONE) return 1f;
        float duration = BASE_MS / ConfigManager.get().animSpeed;
        float t = Math.max(0f, Math.min(1f, (now - start) / duration));
        float inv = 1f - t;
        return 1f - inv * inv * inv; // easeOutCubic
    }

    /** Fractional slot index of the sliding indicator. */
    public float slidePos() {
        return slideFrom + (selected - slideFrom) * ease();
    }

    /** 0..1 strength of the active-colour fill for a slot. */
    public float highlight(int slot) {
        switch (ConfigManager.get().animation) {
            case SLIDE:
                return 0f; // the moving indicator is drawn separately
            case FADE:
            case GLOW: {
                float e = ease();
                if (slot == selected) return e;
                if (slot == from && from != selected) return 1f - e;
                return 0f;
            }
            case SCALE:
            case NONE:
            default:
                return slot == selected ? 1f : 0f;
        }
    }

    public float scale(int slot) {
        if (ConfigManager.get().animation != AnimationType.SCALE) return 1f;
        float e = ease();
        if (slot == selected) return 1f + SCALE_POP * e;
        if (slot == from && from != selected) return 1f + SCALE_POP * (1f - e);
        return 1f;
    }
}
