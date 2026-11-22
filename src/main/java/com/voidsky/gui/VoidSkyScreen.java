package com.voidsky.gui;

import com.voidsky.VoidSkyConfig;
import com.voidsky.sky.Quality;
import com.voidsky.sky.SkyPreset;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Stage-1 menu: preset, quality, enable switch. The full Sky Editor comes in a later stage. */
public class VoidSkyScreen extends Screen {
    private final Screen parent;

    public VoidSkyScreen(Screen parent) {
        super(Text.literal("VoidSky"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        VoidSkyConfig cfg = VoidSkyConfig.get();
        int w = 200, x = this.width / 2 - w / 2, y = this.height / 2 - 70;

        ButtonWidget toggle = ButtonWidget.builder(Text.literal("VoidSky: " + (cfg.enabled ? "ON" : "OFF")), b -> {
            cfg.enabled = !cfg.enabled;
            b.setMessage(Text.literal("VoidSky: " + (cfg.enabled ? "ON" : "OFF")));
        }).dimensions(x, y, w, 20).build();

        ButtonWidget preset = ButtonWidget.builder(Text.literal("Preset: " + cfg.preset), b -> {
            cfg.preset = SkyPreset.byName(cfg.preset).next().name();
            b.setMessage(Text.literal("Preset: " + cfg.preset));
        }).dimensions(x, y + 26, w, 20).build();

        ButtonWidget quality = ButtonWidget.builder(Text.literal("Quality: " + cfg.quality), b -> {
            Quality[] v = Quality.values();
            cfg.quality = v[(Quality.byName(cfg.quality).ordinal() + 1) % v.length].name();
            b.setMessage(Text.literal("Quality: " + cfg.quality));
        }).dimensions(x, y + 52, w, 20).build();

        ButtonWidget black = ButtonWidget.builder(Text.literal("Black sky (ignore day/night): " + (cfg.blackSky ? "ON" : "OFF")), b -> {
            cfg.blackSky = !cfg.blackSky;
            b.setMessage(Text.literal("Black sky (ignore day/night): " + (cfg.blackSky ? "ON" : "OFF")));
        }).dimensions(x, y + 78, w, 20).build();

        ButtonWidget done = ButtonWidget.builder(Text.literal("Done"), b -> close())
            .dimensions(x, y + 114, w, 20).build();

        addDrawableChild(toggle);
        addDrawableChild(preset);
        addDrawableChild(quality);
        addDrawableChild(black);
        addDrawableChild(done);
    }

    @Override
    public void close() {
        VoidSkyConfig.get().save();
        if (this.client != null) this.client.setScreen(parent);
    }
}
