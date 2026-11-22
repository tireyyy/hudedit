package com.voidsky.mixin;

import com.voidsky.VoidSkyConfig;
import com.voidsky.sky.SkyPlanetRenderer;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks (signatures verified against yarn 1.21.11+build.4):
 *  - hide vanilla sun/moon, sky dome, dark disc and sunrise glow -> always black space, day/night is ignored,
 *  - force full star brightness,
 *  - draw VoidSky bodies at the end of the celestial pass.
 */
@Mixin(SkyRendering.class)
public class SkyRenderingMixin {
    @Inject(method = "renderSun", at = @At("HEAD"), cancellable = true)
    private void voidsky$hideSun(float alpha, MatrixStack matrices, CallbackInfo ci) {
        if (VoidSkyConfig.get().enabled) ci.cancel();
    }

    @Inject(method = "renderMoon", at = @At("HEAD"), cancellable = true)
    private void voidsky$hideMoon(MoonPhase phase, float alpha, MatrixStack matrices, CallbackInfo ci) {
        if (VoidSkyConfig.get().enabled) ci.cancel();
    }

    @Inject(method = "renderTopSky", at = @At("HEAD"), cancellable = true)
    private void voidsky$noSkyDome(int color, CallbackInfo ci) {
        if (VoidSkyConfig.get().spaceMode()) ci.cancel();
    }

    @Inject(method = "renderSkyDark", at = @At("HEAD"), cancellable = true)
    private void voidsky$noDarkDisc(CallbackInfo ci) {
        if (VoidSkyConfig.get().spaceMode()) ci.cancel();
    }

    @Inject(method = "renderGlowingSky", at = @At("HEAD"), cancellable = true)
    private void voidsky$noSunrise(MatrixStack matrices, float solarAngle, int color, CallbackInfo ci) {
        if (VoidSkyConfig.get().spaceMode()) ci.cancel();
    }

    /** 5th float argument of renderCelestialBodies is starBrightness. */
    @ModifyVariable(method = "renderCelestialBodies", at = @At("HEAD"), argsOnly = true, ordinal = 4)
    private float voidsky$alwaysStars(float starBrightness) {
        return VoidSkyConfig.get().spaceMode() ? 1.0f : starBrightness;
    }

    @Inject(method = "renderCelestialBodies", at = @At("RETURN"))
    private void voidsky$drawBodies(MatrixStack matrices, float sunAngle, float moonAngle, float starAngle,
                                    MoonPhase moonPhase, float alpha, float starBrightness, CallbackInfo ci) {
        SkyPlanetRenderer.render(matrices);
    }
}
