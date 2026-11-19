package com.hudedit;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Редактор HUD (как в Lunar): перетаскивай элементы мышью,
 * колесо мыши — размер, ползунки — размер и прозрачность.
 */
public class HudEditScreen extends Screen {
    private static final int PW = 220;
    private static final int PH = 112;

    private final HudConfig cfg = HudConfig.get();

    private HudElement selected;
    private HudElement dragging;
    private boolean wasDown = true; // чтобы зажатая при открытии кнопка не считалась кликом
    private int lastMX, lastMY;
    private int px, py;

    private HudSlider scaleSlider;
    private HudSlider opacitySlider;
    private ButtonWidget bossBtn;
    private ButtonWidget chatBtn;

    public HudEditScreen() {
        super(Text.literal("HUD Editor"));
        this.selected = cfg.bossBar;
    }

    @Override
    protected void init() {
        px = (width - PW) / 2;
        py = (height - PH) / 2;

        bossBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Босс-бар"), b -> select(cfg.bossBar))
                .dimensions(px + 10, py + 6, 98, 20).build());
        chatBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Чат"), b -> select(cfg.chat))
                .dimensions(px + PW - 108, py + 6, 98, 20).build());

        scaleSlider = addDrawableChild(new HudSlider(px + 10, py + 30, PW - 20, 20, true));
        opacitySlider = addDrawableChild(new HudSlider(px + 10, py + 54, PW - 20, 20, false));

        addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить"), b -> {
            selected.reset();
            syncSliders();
        }).dimensions(px + 10, py + 84, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
                .dimensions(px + PW - 108, py + 84, 98, 20).build());

        refreshSelection();
    }

    // ---------- выбор элемента ----------

    private void select(HudElement e) {
        selected = e;
        refreshSelection();
    }

    private void refreshSelection() {
        if (bossBtn != null) bossBtn.setMessage(Text.literal(selected == cfg.bossBar ? "[Босс-бар]" : "Босс-бар"));
        if (chatBtn != null) chatBtn.setMessage(Text.literal(selected == cfg.chat ? "[Чат]" : "Чат"));
        syncSliders();
    }

    private void syncSliders() {
        if (scaleSlider != null) scaleSlider.sync();
        if (opacitySlider != null) opacitySlider.sync();
    }

    // ---------- геометрия ----------

    private int bossCount() {
        return HudEditClient.bossBars(client.inGameHud.getBossBarHud()).size();
    }

    private float[] bounds(HudElement e) {
        if (e == cfg.chat) return HudLayout.chatBounds(e, width, height);
        return HudLayout.bossBounds(e, width, height, bossCount());
    }

    /** Не даём элементу уехать за экран. */
    private void clamp(HudElement e) {
        float[] r = bounds(e);
        if (r[0] + r[2] > width) e.offX -= (r[0] + r[2]) - width;
        if (r[1] + r[3] > height) e.offY -= (r[1] + r[3]) - height;
        r = bounds(e);
        if (r[0] < 0) e.offX -= r[0];
        if (r[1] < 0) e.offY -= r[1];
    }

    private boolean inside(float[] r, int mx, int my) {
        return mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
    }

    private boolean overPanel(int mx, int my) {
        return mx >= px - 4 && mx <= px + PW + 4 && my >= py - 4 && my <= py + PH + 4;
    }

    private HudElement hit(int mx, int my) {
        if (inside(bounds(selected), mx, my)) return selected;
        if (inside(bounds(cfg.chat), mx, my)) return cfg.chat;
        if (inside(bounds(cfg.bossBar), mx, my)) return cfg.bossBar;
        return null;
    }

    // ---------- мышь ----------

    private void handleMouse(int mx, int my) {
        long window = client.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (down && !wasDown) {
            dragging = null;
            if (!overPanel(mx, my)) {
                HudElement h = hit(mx, my);
                if (h != null) {
                    select(h);
                    dragging = h;
                }
            }
            lastMX = mx;
            lastMY = my;
        } else if (down && dragging != null) {
            dragging.offX += mx - lastMX;
            dragging.offY += my - lastMY;
            lastMX = mx;
            lastMY = my;
            clamp(dragging);
        } else if (!down) {
            dragging = null;
        }
        wasDown = down;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!overPanel((int) mouseX, (int) mouseY)) {
            HudElement t = hit((int) mouseX, (int) mouseY);
            if (t == null) t = selected;
            if (t != selected) select(t);
            t.scale += (float) verticalAmount * 0.05f;
            t.sanitize();
            clamp(t);
            syncSliders();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // ---------- отрисовка ----------

    // без @Override намеренно: просто убираем ванильный блюр/затемнение фона
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x30000000);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        handleMouse(mouseX, mouseY);

        // превью боссбара, если реального сейчас нет
        if (bossCount() == 0) {
            HudLayout.apply(ctx, cfg.bossBar, width / 2f, 0f);
            HudLayout.drawBar(ctx, 12, Text.literal("Босс-бар"), 0.6f, 0xE04040, cfg.bossBar.opacity);
            HudLayout.pop(ctx);
        }

        super.render(ctx, mouseX, mouseY, delta);

        drawFrame(ctx, cfg.bossBar, "Босс-бар");
        drawFrame(ctx, cfg.chat, "Чат");

        String hint = "ЛКМ — перетащить   •   Колесо — размер";
        ctx.drawText(textRenderer, Text.literal(hint), (width - textRenderer.getWidth(hint)) / 2, py - 14, 0xFFFFFFFF, true);
    }

    private void drawFrame(DrawContext ctx, HudElement e, String label) {
        float[] r = bounds(e);
        int x = Math.round(r[0]);
        int y = Math.round(r[1]);
        int w = Math.max(4, Math.round(r[2]));
        int h = Math.max(4, Math.round(r[3]));
        boolean sel = e == selected;
        int color = sel ? 0xFF55FF55 : 0xFFFFFFFF;

        ctx.fill(x, y, x + w, y + h, sel ? 0x2255FF55 : 0x15FFFFFF);
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);

        int ty = y >= 11 ? y - 10 : y + h + 2;
        ctx.drawText(textRenderer, Text.literal(label), x, ty, color, true);
    }

    // ---------- прочее ----------

    public boolean shouldPause() {
        return false; // игра не ставится на паузу
    }

    @Override
    public void removed() {
        HudConfig.save();
    }

    /** Ползунок масштаба / прозрачности выбранного элемента. */
    private final class HudSlider extends SliderWidget {
        private final boolean scaleMode;
        private final float min;
        private final float max;

        HudSlider(int x, int y, int w, int h, boolean scaleMode) {
            super(x, y, w, h, Text.empty(), 0.0);
            this.scaleMode = scaleMode;
            this.min = scaleMode ? 0.3f : 0.1f;
            this.max = scaleMode ? 3.0f : 1.0f;
            sync();
        }

        private float read() {
            return scaleMode ? selected.scale : selected.opacity;
        }

        void sync() {
            this.value = (read() - min) / (max - min);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float v = read();
            String s = scaleMode
                    ? "Размер: " + Math.round(v * 100f) + "%"
                    : "Прозрачность: " + Math.round(v * 100f) + "%";
            setMessage(Text.literal(s));
        }

        @Override
        protected void applyValue() {
            float v = min + (float) this.value * (max - min);
            if (scaleMode) {
                selected.scale = v;
            } else {
                selected.opacity = v;
            }
            selected.sanitize();
            if (scaleMode) clamp(selected);
        }
    }
}
