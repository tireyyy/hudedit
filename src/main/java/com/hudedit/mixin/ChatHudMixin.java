package com.hudedit.mixin;

import com.hudedit.ChatOpacity;
import com.hudedit.HudConfig;
import com.hudedit.HudEditClient;
import com.hudedit.HudElement;
import com.hudedit.HudLayout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void hudedit$head(CallbackInfo ci) {
        // защита от вложенных перегрузок render
        if (HudEditClient.chatDepth++ > 0) return;

        DrawContext ctx = HudEditClient.context;
        if (ctx == null) return;

        HudElement e = HudConfig.get().chat;
        float ay = MinecraftClient.getInstance().getWindow().getScaledHeight() - 40f;
        HudLayout.apply(ctx, e, 0f, ay);
        ChatOpacity.begin(e.opacity);
        HudEditClient.chatPushed = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void hudedit$tail(CallbackInfo ci) {
        if (--HudEditClient.chatDepth > 0) return;
        HudEditClient.chatDepth = 0;

        if (HudEditClient.chatPushed) {
            HudEditClient.chatPushed = false;
            ChatOpacity.end();
            DrawContext ctx = HudEditClient.context;
            if (ctx != null) HudLayout.pop(ctx);
        }
    }
}
