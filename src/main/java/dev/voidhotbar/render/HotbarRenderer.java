package dev.voidhotbar.render;

import dev.voidhotbar.config.AnimationType;
import dev.voidhotbar.config.ConfigManager;
import dev.voidhotbar.config.VoidConfig;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Replaces the vanilla hotbar element. Draws separate rounded slots, the selection
 * highlight, items (count / durability / pop animation via vanilla helpers), the offhand
 * slot and the hotbar-style attack indicator.
 */
public final class HotbarRenderer {
    public static final int SLOT = 20;
    private static final int GAP = 6;

    private final AnimationController hudAnim = new AnimationController();
    private final AnimationController previewAnim = new AnimationController();

    /** Called from the HUD element that replaces VanillaHudElements.HOTBAR. */
    public void renderHud(GuiGraphics g, DeltaTracker dt, Minecraft mc, Player p) {
        hudAnim.update(p.getInventory().getSelectedSlot(), System.currentTimeMillis());
        draw(g, g.guiWidth() / 2, g.guiHeight() - 2, mc, p, hudAnim,
                dt.getGameTimeDeltaPartialTick(false));
    }

    /** Used by the settings screen: same drawing code, selection cycles on its own. */
    public void renderPreview(GuiGraphics g, int centerX, int bottomY, Minecraft mc, Player p, float partial) {
        long now = System.currentTimeMillis();
        previewAnim.update((int) ((now / 1200L) % 9L), now);
        draw(g, centerX, bottomY, mc, p, previewAnim, partial);
    }

    private void draw(GuiGraphics g, int cx, int bottom, Minecraft mc, Player p,
                      AnimationController anim, float partial) {
        VoidConfig c = ConfigManager.get();
        int pitch = SLOT + c.spacing;
        int total = 9 * SLOT + 8 * c.spacing;
        int baseA = Math.round(c.opacity * 255f);
        int main = ColorManager.argb(c.mainColor, baseA);
        int activeA = Math.min(255, baseA + 40);

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, bottom);
        pose.scale(c.size, c.size);
        pose.translate(-(total / 2), -SLOT);

        int sel = anim.selected();

        // 1) slot backgrounds, selected one last so a scaled slot overlaps its neighbours
        for (int i = 0; i < 9; i++) {
            if (i != sel) drawSlotBackground(g, c, anim, i, i * pitch, main, activeA);
        }
        if (sel >= 0) drawSlotBackground(g, c, anim, sel, sel * pitch, main, activeA);

        // 2) sliding indicator
        if (c.animation == AnimationType.SLIDE) {
            int x = Math.round(anim.slidePos() * pitch);
            fillRounded(g, x, 0, SLOT, SLOT, c.radius, ColorManager.argb(c.activeColor, activeA));
        }

        // 3) items
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            float s = anim.scale(i);
            boolean scaled = Math.abs(s - 1f) > 0.001f;
            int x = i * pitch;
            if (scaled) pushAround(g, x + SLOT / 2f, SLOT / 2f, s);
            drawItem(g, mc, p, stack, x + 2, 2, partial, i + 1);
            if (scaled) pose.popMatrix();
        }

        // 4) offhand + attack indicator
        boolean leftHanded = p.getMainArm() == HumanoidArm.LEFT;
        int offhandX = leftHanded ? total + GAP : -(SLOT + GAP);
        int attackX = leftHanded ? -(SLOT + GAP) : total + GAP;

        ItemStack off = p.getOffhandItem();
        if (!off.isEmpty()) {
            fillRounded(g, offhandX, 0, SLOT, SLOT, c.radius, main);
            drawItem(g, mc, p, off, offhandX + 2, 2, partial, 10);
        }

        if (mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
            float str = p.getAttackStrengthScale(0f);
            if (str < 1f) {
                fillRounded(g, attackX, 0, SLOT, SLOT, c.radius, main);
                int fh = Math.round(str * SLOT);
                if (fh > 0) {
                    fillRounded(g, attackX, SLOT - fh, SLOT, fh, Math.min(c.radius, fh / 2),
                            ColorManager.argb(c.activeColor, activeA));
                }
            }
        }

        pose.popMatrix();
    }

    private void drawSlotBackground(GuiGraphics g, VoidConfig c, AnimationController anim,
                                    int i, int x, int main, int activeA) {
        float s = anim.scale(i);
        boolean scaled = Math.abs(s - 1f) > 0.001f;
        if (scaled) pushAround(g, x + SLOT / 2f, SLOT / 2f, s);

        fillRounded(g, x, 0, SLOT, SLOT, c.radius, main);

        float h = anim.highlight(i);
        if (h > 0.003f) {
            if (c.animation == AnimationType.GLOW) {
                // soft halo: three faint layers, outermost first (alpha accumulates towards the slot)
                int[] alpha = {14, 20, 28};
                for (int k = 3; k >= 1; k--) {
                    fillRounded(g, x - k, -k, SLOT + 2 * k, SLOT + 2 * k, c.radius + k,
                            ColorManager.argb(c.glowColor, Math.round(alpha[3 - k] * h)));
                }
            }
            fillRounded(g, x, 0, SLOT, SLOT, c.radius, ColorManager.argb(c.activeColor, Math.round(activeA * h)));
        }

        if (scaled) g.pose().popMatrix();
    }

    private static void pushAround(GuiGraphics g, float px, float py, float s) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(px, py);
        pose.scale(s, s);
        pose.translate(-px, -py);
    }

    /** Mirrors vanilla Gui#renderSlot: pop animation, item, then count/durability decorations. */
    private static void drawItem(GuiGraphics g, Minecraft mc, Player p, ItemStack stack,
                                 int x, int y, float partial, int seed) {
        float pop = stack.getPopTime() - partial;
        var pose = g.pose();
        if (pop > 0f) {
            float k = 1f + pop / 5f;
            pose.pushMatrix();
            pose.translate(x + 8, y + 12);
            pose.scale(1f / k, (k + 1f) / 2f);
            pose.translate(-(x + 8), -(y + 12));
        }
        g.renderItem(p, stack, x, y, seed);
        if (pop > 0f) pose.popMatrix();
        g.renderItemDecorations(mc.font, stack, x, y);
    }

    /** Rounded rectangle built from horizontal runs; only a handful of fill() calls per shape. */
    public static void fillRounded(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if ((color >>> 24) == 0 || w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        int start = 0;
        int cur = inset(0, h, r);
        for (int row = 1; row <= h; row++) {
            int ins = row == h ? -1 : inset(row, h, r);
            if (ins != cur) {
                g.fill(x + cur, y + start, x + w - cur, y + row, color);
                start = row;
                cur = ins;
            }
        }
    }

    private static int inset(int row, int h, int r) {
        int k = Math.min(row, h - 1 - row);
        if (k >= r) return 0;
        double dy = r - k - 0.5;
        return (int) Math.round(r - Math.sqrt(r * r - dy * dy));
    }
}
