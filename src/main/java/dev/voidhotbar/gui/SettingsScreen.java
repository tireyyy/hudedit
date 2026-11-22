package dev.voidhotbar.gui;

import dev.voidhotbar.config.AnimationType;
import dev.voidhotbar.config.ConfigManager;
import dev.voidhotbar.config.VoidConfig;
import dev.voidhotbar.render.ColorManager;
import dev.voidhotbar.render.HotbarRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class SettingsScreen extends Screen {
    private static final int COL_W = 150;
    private static final int COL_GAP = 10;
    private static final int ROW = 24;
    private static final int TOP = 72;

    private final Screen parent;
    private final HotbarRenderer preview = new HotbarRenderer();
    private final EditBox[] hexBoxes = new EditBox[3];
    private Button presetButton;
    private Button animButton;
    private boolean syncing;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("voidhotbar.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        VoidConfig c = ConfigManager.get();
        int left = this.width / 2 - COL_W - COL_GAP / 2;
        int right = this.width / 2 + COL_GAP / 2;

        // ---- left column: preset, animation type, three colours ----
        presetButton = addRenderableWidget(Button.builder(presetLabel(), b -> cyclePreset())
                .bounds(left, TOP, COL_W, 20).build());
        animButton = addRenderableWidget(Button.builder(animLabel(), b -> {
            c.animation = c.animation.next();
            animButton.setMessage(animLabel());
        }).bounds(left, TOP + ROW, COL_W, 20).build());

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            int y = TOP + ROW * (i + 2);
            EditBox box = new EditBox(this.font, left + 46, y, 76, 20, Component.empty());
            box.setMaxLength(16);
            box.setValue(ColorManager.toHex(getColor(idx)));
            box.setResponder(s -> onHexChanged(idx, s));
            hexBoxes[i] = addRenderableWidget(box);
        }

        // ---- right column: sliders ----
        addRenderableWidget(new ConfigSlider(right, TOP, "voidhotbar.opacity", 0.2, 1.0, 0.05,
                () -> c.opacity, v -> c.opacity = (float) v, true));
        addRenderableWidget(new ConfigSlider(right, TOP + ROW, "voidhotbar.radius", 0, 8, 1,
                () -> c.radius, v -> c.radius = (int) Math.round(v), false));
        addRenderableWidget(new ConfigSlider(right, TOP + ROW * 2, "voidhotbar.size", 0.8, 1.25, 0.05,
                () -> c.size, v -> c.size = (float) v, true));
        addRenderableWidget(new ConfigSlider(right, TOP + ROW * 3, "voidhotbar.spacing", 0, 8, 1,
                () -> c.spacing, v -> c.spacing = (int) Math.round(v), false));
        addRenderableWidget(new ConfigSlider(right, TOP + ROW * 4, "voidhotbar.speed", 0.25, 3.0, 0.25,
                () -> c.animSpeed, v -> c.animSpeed = (float) v, true));

        // ---- bottom buttons ----
        int by = TOP + ROW * 5 + 6;
        addRenderableWidget(Button.builder(Component.translatable("voidhotbar.reset"), b -> {
            ConfigManager.reset();
            rebuildWidgets();
        }).bounds(left, by, COL_W, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("voidhotbar.done"), b -> onClose())
                .bounds(right, by, COL_W, 20).build());
    }

    // ---------- colours / presets ----------

    private int getColor(int idx) {
        VoidConfig c = ConfigManager.get();
        return idx == 0 ? c.mainColor : idx == 1 ? c.activeColor : c.glowColor;
    }

    private void setColor(int idx, int rgb) {
        VoidConfig c = ConfigManager.get();
        if (idx == 0) c.mainColor = rgb;
        else if (idx == 1) c.activeColor = rgb;
        else c.glowColor = rgb;
    }

    private void onHexChanged(int idx, String text) {
        if (syncing) return;
        Integer rgb = ColorManager.parse(text);
        hexBoxes[idx].setTextColor(rgb == null ? 0xFFFF5555 : 0xFFE0E0E0);
        if (rgb == null) return;
        setColor(idx, rgb);
        ConfigManager.get().preset = "custom";
        if (presetButton != null) presetButton.setMessage(presetLabel());
    }

    private void cyclePreset() {
        VoidConfig c = ConfigManager.get();
        int next = (ColorManager.indexOf(c.preset) + 1) % ColorManager.PRESETS.size();
        ColorManager.apply(c, ColorManager.PRESETS.get(next));
        syncing = true;
        for (int i = 0; i < 3; i++) {
            hexBoxes[i].setValue(ColorManager.toHex(getColor(i)));
            hexBoxes[i].setTextColor(0xFFE0E0E0);
        }
        syncing = false;
        presetButton.setMessage(presetLabel());
    }

    private Component presetLabel() {
        int i = ColorManager.indexOf(ConfigManager.get().preset);
        Component name = i >= 0 ? Component.literal(ColorManager.PRESETS.get(i).name())
                                : Component.translatable("voidhotbar.custom");
        return Component.translatable("voidhotbar.preset").append(": ").append(name);
    }

    private Component animLabel() {
        AnimationType t = ConfigManager.get().animation;
        return Component.translatable("voidhotbar.animation").append(": ")
                .append(Component.translatable(t.translationKey()));
    }

    // ---------- rendering ----------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        if (this.minecraft != null && this.minecraft.player != null) {
            preview.renderPreview(g, this.width / 2, 58, this.minecraft, this.minecraft.player, partialTick);
        }

        int left = this.width / 2 - COL_W - COL_GAP / 2;
        String[] keys = {"voidhotbar.color.main", "voidhotbar.color.active", "voidhotbar.color.glow"};
        for (int i = 0; i < 3; i++) {
            int y = TOP + ROW * (i + 2);
            g.drawString(this.font, Component.translatable(keys[i]), left, y + 6, 0xFFAAAAAA);
            g.fill(left + 46 + 76 + 6, y + 2, left + 46 + 76 + 22, y + 18, 0xFF000000 | getColor(i));
        }
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    // ---------- slider ----------

    private static final class ConfigSlider extends AbstractSliderButton {
        private final String key;
        private final double min, max, step;
        private final boolean decimal;
        private final DoubleConsumer setter;

        ConfigSlider(int x, int y, String key, double min, double max, double step,
                     DoubleSupplier getter, DoubleConsumer setter, boolean decimal) {
            super(x, y, COL_W, 20, Component.empty(), (getter.getAsDouble() - min) / (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.step = step;
            this.decimal = decimal;
            this.setter = setter;
            updateMessage();
        }

        private double current() {
            double v = min + this.value * (max - min);
            return Math.max(min, Math.min(max, Math.round(v / step) * step));
        }

        @Override
        protected void updateMessage() {
            if (key == null) return; // super constructor may call this before fields are set
            double v = current();
            String text = decimal ? String.format("%.2f", v) : String.valueOf((int) Math.round(v));
            setMessage(Component.translatable(key).append(": " + text));
        }

        @Override
        protected void applyValue() {
            setter.accept(current());
        }
    }
}
