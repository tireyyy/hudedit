package com.hudedit.mixin;

import com.hudedit.HudConfig;
import com.hudedit.HudEditClient;
import com.hudedit.HudElement;
import com.hudedit.HudLayout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hudedit$head(CallbackInfo ci) {
        DrawContext ctx = HudEditClient.context;
        if (ctx == null) return;

        HudElement e = HudConfig.get().bossBar;
        float cx = MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;
        HudLayout.apply(ctx, e, cx, 0f);

        if (e.opacity < 0.99f) {
            // прозрачность: ванильные текстуры не умеют alpha, рисуем свою версию
            HudLayout.drawBossBars(ctx, HudEditClient.bossBars((BossBarHud) (Object) this), e.opacity);
            HudLayout.pop(ctx);
            ci.cancel();
            return;
        }
        HudEditClient.bossPushed = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void hudedit$tail(CallbackInfo ci) {
        if (HudEditClient.bossPushed) {
            HudEditClient.bossPushed = false;
            DrawContext ctx = HudEditClient.context;
            if (ctx != null) HudLayout.pop(ctx);
        }
    }
}
