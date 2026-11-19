package com.hudedit;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;

import java.util.Collection;

/** Трансформации и геометрия элементов HUD. */
public final class HudLayout {
    private HudLayout() {}

    /** Сдвиг + масштаб вокруг якоря (ax, ay) в GUI-координатах. Обязательно парный pop(). */
    public static void apply(DrawContext ctx, HudElement e, float ax, float ay) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(e.offX, e.offY);
        m.translate(ax, ay);
        m.scale(e.scale, e.scale);
        m.translate(-ax, -ay);
    }

    public static void pop(DrawContext ctx) {
        ctx.getMatrices().popMatrix();
    }

    public static int argb(float alpha, int rgb) {
        int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    // ---------- Боссбар с прозрачностью (свой рендер) ----------

    public static void drawBar(DrawContext ctx, int y, Text name, float pct, int rgb, float alpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth();
        int x = w / 2 - 91;
        ctx.fill(x - 1, y - 1, x + 183, y + 6, argb(alpha * 0.6f, 0x000000));
        ctx.fill(x, y, x + 182, y + 5, argb(alpha, 0x444444));
        ctx.fill(x, y, x + (int) (Math.max(0f, Math.min(1f, pct)) * 182f), y + 5, argb(alpha, rgb));
        int tw = mc.textRenderer.getWidth(name);
        ctx.drawText(mc.textRenderer, name, w / 2 - tw / 2, y - 9, argb(alpha, 0xFFFFFF), true);
    }

    public static void drawBossBars(DrawContext ctx, Collection<ClientBossBar> bars, float alpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int h = mc.getWindow().getScaledHeight();
        int y = 12;
        for (ClientBossBar bar : bars) {
            drawBar(ctx, y, bar.getName(), bar.getPercent(), barColor(bar), alpha);
            y += 19;
            if (y >= h / 3) break;
        }
    }

    private static int barColor(ClientBossBar bar) {
        return switch (bar.getColor().name()) {
            case "PINK" -> 0xE86FB5;
            case "BLUE" -> 0x4A8BEA;
            case "RED" -> 0xE04040;
            case "GREEN" -> 0x55C455;
            case "YELLOW" -> 0xF0D640;
            case "PURPLE" -> 0x9B5FD6;
            default -> 0xF0F0F0;
        };
    }

    // ---------- Рамки элементов на экране (для редактора) ----------

    /** {x, y, w, h} боссбара на экране с учётом смещения и масштаба. */
    public static float[] bossBounds(HudElement e, int sw, int sh, int bars) {
        int n = Math.max(1, bars);
        float localH = 14f + 19f * (n - 1);
        float s = e.scale;
        float x = e.offX + sw / 2f - 91f * s;
        float y = e.offY + 3f * s;
        return new float[]{x, y, 182f * s, localH * s};
    }

    /** {x, y, w, h} чата на экране. Якорь масштабирования — левый нижний угол (0, sh-40). */
    public static float[] chatBounds(HudElement e, int sw, int sh) {
        GameOptions o = MinecraftClient.getInstance().options;
        int cw = (int) (o.getChatWidth().getValue() * 280.0 + 40.0);
        int ch = (int) (o.getChatHeightFocused().getValue() * 160.0 + 20.0);
        float s = e.scale;
        float bottom = e.offY + (sh - 40);
        return new float[]{e.offX, bottom - ch * s, (cw + 8) * s, ch * s + 2f * s};
    }
}
