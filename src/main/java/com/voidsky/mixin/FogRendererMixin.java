package com.voidsky.mixin;

import com.voidsky.VoidSkyConfig;
import net.minecraft.client.render.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the clear/fog colour black so the sky stays space-black at any time of day.
 * Uses require = 0: if the private method changes in a future 1.21.11 build, the mod still loads
 * (the horizon would then keep the vanilla fog colour).
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void voidsky$blackFog(CallbackInfoReturnable<Vector4f> cir) {
        if (VoidSkyConfig.get().spaceMode()) cir.setReturnValue(new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
    }
}
