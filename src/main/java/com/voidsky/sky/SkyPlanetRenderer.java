package com.voidsky.sky;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.voidsky.VoidSkyConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Draws one camera-facing quad per visible body. Planet shading is done fully in the fragment shader,
 * so the CPU only builds 4 vertices per planet and updates two angles at a limited animation rate.
 */
public final class SkyPlanetRenderer {
    private static final float SKY_DIST = 100.0f;
    private static final double TWO_PI = Math.PI * 2.0;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(Identifier.of("voidsky", "pipeline/planet"))
            .withVertexShader(Identifier.of("voidsky", "core/planet"))
            .withFragmentShader(Identifier.of("voidsky", "core/planet"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .build());

    // reusable scratch objects: no allocation in the hot path
    private static final Vector3f DIR = new Vector3f();
    private static final Vector3f RIGHT = new Vector3f();
    private static final Vector3f UP = new Vector3f();
    private static final Vector3f VIEW = new Vector3f();
    private static final Vector3f TMP = new Vector3f();
    private static final Vector4f WHITE = new Vector4f(1, 1, 1, 1);
    private static final Vector3f ZERO = new Vector3f();
    private static final Matrix4f IDENT = new Matrix4f();

    private static double rot, cloud;
    private static long lastNanos, lastAnimNanos;
    private static int rotBits, cloudBits;

    private SkyPlanetRenderer() { }

    /** Touch the class early so the pipeline is registered during client init. */
    public static void init() { }

    private static void animate(VoidSkyConfig cfg, Quality q) {
        long now = System.nanoTime();
        if (lastNanos == 0) { lastNanos = now; lastAnimNanos = now; }
        long minStep = 1_000_000_000L / q.animFps;     // animation rate limit independent of game FPS
        if (now - lastAnimNanos < minStep) return;
        double dt = (now - lastAnimNanos) / 1e9;
        lastAnimNanos = now;
        rot = (rot + dt * cfg.rotationSpeed * 0.02) % TWO_PI;     // ~5 min per revolution
        cloud = (cloud + dt * cfg.cloudSpeed * 0.032) % TWO_PI;
        rotBits = (int) (rot / TWO_PI * 65535.0);
        cloudBits = (int) (cloud / TWO_PI * 65535.0);
    }

    public static void render(MatrixStack matrices) {
        VoidSkyConfig cfg = VoidSkyConfig.get();
        if (!cfg.enabled) return;
        Quality q = Quality.byName(cfg.quality);
        SkyPreset preset = SkyPreset.byName(cfg.preset);
        animate(cfg, q);

        Matrix4f mat = matrices.peek().getPositionMatrix();
        BufferBuilder bb = null;
        int quads = 0;

        for (SkyPreset.Body b : preset.bodies) {
            double az = Math.toRadians(b.azimuth()), el = Math.toRadians(b.elevation());
            DIR.set((float) (Math.cos(el) * Math.sin(az)), (float) Math.sin(el), (float) (-Math.cos(el) * Math.cos(az)));

            // frustum culling: drop bodies behind the camera or far outside the view
            mat.transformPosition(TMP.set(DIR).mul(SKY_DIST), VIEW);
            if (VIEW.z > -1.0f) continue;
            float lim = -VIEW.z * 2.6f;
            if (Math.abs(VIEW.x) > lim || Math.abs(VIEW.y) > lim) continue;

            float radius = b.radius() * cfg.sizeScale;
            float half = radius / (b.type() == 3 ? 0.40f : b.type() == 4 ? 0.50f : 0.74f);

            RIGHT.set(0, 1, 0).cross(DIR);
            if (RIGHT.lengthSquared() < 1e-6f) RIGHT.set(1, 0, 0);
            RIGHT.normalize();
            UP.set(DIR).cross(RIGHT).normalize();

            if (bb == null) bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            float uOff = 4f * b.type() + 64f * q.level;
            float vOff = 4f * b.variant();
            int cr = (rotBits >> 8) & 255, cg = rotBits & 255, cb = (cloudBits >> 8) & 255, ca = cloudBits & 255;
            quad(bb, DIR, half, -1, -1, uOff, vOff, cr, cg, cb, ca);
            quad(bb, DIR, half, 1, -1, uOff, vOff, cr, cg, cb, ca);
            quad(bb, DIR, half, 1, 1, uOff, vOff, cr, cg, cb, ca);
            quad(bb, DIR, half, -1, 1, uOff, vOff, cr, cg, cb, ca);
            quads++;
        }
        if (bb == null) return;

        try (BuiltBuffer built = bb.end()) {
            BuiltBuffer.DrawParameters dp = built.getDrawParameters();
            MinecraftClient mc = MinecraftClient.getInstance();
            RenderSystem.ShapeIndexBuffer idx = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
            GpuBuffer ibo = idx.getIndexBuffer(dp.indexCount());
            GpuBuffer vbo = RenderSystem.getDevice().createBuffer(() -> "voidsky planets", GpuBuffer.USAGE_VERTEX, built.getBuffer());
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().write(mat, WHITE, ZERO, IDENT);
            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "voidsky planets",
                mc.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(),
                mc.getFramebuffer().getDepthAttachmentView(), OptionalDouble.empty())) {
                pass.setPipeline(PIPELINE);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transforms);
                pass.setVertexBuffer(0, vbo);
                pass.setIndexBuffer(ibo, idx.getIndexType());
                pass.drawIndexed(0, 0, dp.indexCount(), 1);
            }
            vbo.close();
        }
    }

    private static void quad(BufferBuilder bb, Vector3f dir, float half, float sx, float sy,
                             float uOff, float vOff, int r, int g, int b, int a) {
        float x = dir.x * SKY_DIST + RIGHT.x * half * sx + UP.x * half * sy;
        float y = dir.y * SKY_DIST + RIGHT.y * half * sx + UP.y * half * sy;
        float z = dir.z * SKY_DIST + RIGHT.z * half * sx + UP.z * half * sy;
        bb.vertex(x, y, z).texture(sx * 0.5f + 0.5f + uOff, sy * 0.5f + 0.5f + vOff).color(r, g, b, a);
    }
}
