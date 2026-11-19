package com.hudedit;

/** Настройки одного элемента HUD: смещение, масштаб, прозрачность. */
public class HudElement {
    public float offX = 0f;
    public float offY = 0f;
    public float scale = 1f;
    public float opacity = 1f;

    public void reset() {
        offX = 0f;
        offY = 0f;
        scale = 1f;
        opacity = 1f;
    }

    public void sanitize() {
        scale = Math.max(0.3f, Math.min(4f, scale));
        opacity = Math.max(0.1f, Math.min(1f, opacity));
    }
}
