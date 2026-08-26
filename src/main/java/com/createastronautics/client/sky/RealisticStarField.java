package com.createastronautics.client.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

/**
 * A three-layer parallax star field - a near-dense, mid, and sparse-bright layer drawn at decreasing
 * opacity - rather than one flat field of uniform dots. Built once as persistent GPU buffers (not rebuilt
 * every frame), following the same technique as Northstar-Redux's {@code SpaceEffects}
 * (github.com/Astronauts-of-Create/Northstar-Redux, MIT licensed). Shared between the space dimensions'
 * sky and the Overworld's night sky so they all show the same field of stars.
 */
public final class RealisticStarField {
    private static final VertexBuffer LAYER_1 = build(1500, 10842L);
    private static final VertexBuffer LAYER_2 = build(1800, 64094L);
    private static final VertexBuffer LAYER_3 = build(2500, 92410L);

    private RealisticStarField() {
    }

    /** @param intensity Overall brightness (time of day, rain) applied uniformly across all three layers. */
    public static void render(PoseStack poseStack, Matrix4f projectionMatrix, Runnable setupFog, float intensity) {
        if (intensity <= 0.0F) {
            return;
        }

        poseStack.pushPose();

        FogRenderer.setupNoFog();
        RenderSystem.enableBlend();
        // Additive, matching vanilla's own star blend function exactly (LevelRenderer#renderSky) - a plain
        // alpha blend (ONE_MINUS_SRC_ALPHA) instead darkens whatever's behind each star's semi-transparent
        // edge pixels, which is what a shader pack's sky compositing was picking up as a darker/flickering
        // sky, since it composites against that destination factor rather than just adding light to it.
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionShader);

        Matrix4f pose = poseStack.last().pose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, intensity);
        LAYER_1.bind();
        LAYER_1.drawWithShader(pose, projectionMatrix, GameRenderer.getPositionShader());

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, intensity * 2.0F / 3.0F);
        LAYER_2.bind();
        LAYER_2.drawWithShader(pose, projectionMatrix, GameRenderer.getPositionShader());

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, intensity / 3.0F);
        LAYER_3.bind();
        LAYER_3.drawWithShader(pose, projectionMatrix, GameRenderer.getPositionShader());

        VertexBuffer.unbind();
        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        setupFog.run();
    }

    private static VertexBuffer build(int count, long seed) {
        RandomSource random = RandomSource.create(seed);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        int placed = 0;
        while (placed < count) {
            double dx = random.nextFloat() * 2.0F - 1.0F;
            double dy = random.nextFloat() * 2.0F - 1.0F;
            double dz = random.nextFloat() * 2.0F - 1.0F;
            double lengthSq = dx * dx + dy * dy + dz * dz;
            if (lengthSq >= 1.0 || lengthSq <= 0.01) {
                continue;
            }

            double length = 1.0 / Math.sqrt(lengthSq);
            dx *= length;
            dy *= length;
            dz *= length;

            double x = dx * 100.0;
            double y = dy * 100.0;
            double z = dz * 100.0;

            double yaw = Math.atan2(dx, dz);
            double sinYaw = Math.sin(yaw);
            double cosYaw = Math.cos(yaw);
            double pitch = Math.atan2(Math.sqrt(dx * dx + dz * dz), dy);
            double sinPitch = Math.sin(pitch);
            double cosPitch = Math.cos(pitch);
            double rotation = random.nextDouble() * Math.PI * 2.0;
            double sinRot = Math.sin(rotation);
            double cosRot = Math.cos(rotation);
            double size = 0.15 + random.nextFloat() * 0.1;

            for (int corner = 0; corner < 4; corner++) {
                double rawX = ((corner & 2) - 1) * size;
                double rawY = (((corner + 1) & 2) - 1) * size;
                double rotatedU = rawX * cosRot - rawY * sinRot;
                double rotatedV = rawY * cosRot + rawX * sinRot;
                double vy = rotatedU * sinPitch;
                double depth = -rotatedU * cosPitch;
                double vx = depth * sinYaw - rotatedV * cosYaw;
                double vz = rotatedV * sinYaw + depth * cosYaw;
                builder.addVertex((float) (x + vx), (float) (y + vy), (float) (z + vz));
            }
            placed++;
        }

        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
        return buffer;
    }
}
