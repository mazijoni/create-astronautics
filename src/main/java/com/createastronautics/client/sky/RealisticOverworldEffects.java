package com.createastronautics.client.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Replaces the Overworld's flat, uniformly-dark night sky with a gradient dome (darker at the zenith, a bit
 * lighter toward the horizon like real atmospheric scattering) and a varied star field, while leaving every
 * other time of day - dawn, day, dusk, rain - to vanilla untouched. Registered over vanilla's own Overworld
 * effects ({@code minecraft:overworld}) rather than a new dimension, so it applies to the actual Overworld.
 *
 * Only takes over once {@link ClientLevel#getSkyDarken} is at its night-time maximum; every other condition
 * that would normally suppress vanilla's sky (fog, lava/powder snow in the camera, blindness/darkness) is
 * checked here too, since returning {@code true} skips vanilla's own checks for those entirely.
 */
public class RealisticOverworldEffects extends DimensionSpecialEffects.OverworldEffects {
    private static final ResourceLocation MOON_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
    private static final float NIGHT_THRESHOLD = 0.99F;

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f frustumMatrix, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable skyFogSetup) {
        if (isFoggy || level.getSkyDarken(partialTick) < NIGHT_THRESHOLD || blocksSky(camera)) {
            return false;
        }
        FogType fluidInCamera = camera.getFluidInCamera();
        if (fluidInCamera == FogType.LAVA || fluidInCamera == FogType.POWDER_SNOW) {
            return false;
        }

        skyFogSetup.run();

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);

        RenderSystem.depthMask(false);
        Vec3 horizonColor = level.getSkyColor(camera.getPosition(), partialTick);
        renderGradientDome(poseStack, horizonColor);

        RenderSystem.enableBlend();
        renderMoon(level, partialTick, poseStack);

        float starIntensity = level.getStarBrightness(partialTick) * (1.0F - level.getRainLevel(partialTick));
        RealisticStarField.render(poseStack, projectionMatrix, skyFogSetup, starIntensity);

        renderDarkPlaneBelowHorizon(level, camera, poseStack, projectionMatrix);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        return true;
    }

    private static boolean blocksSky(Camera camera) {
        return camera.getEntity() instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS));
    }

    private static void renderGradientDome(PoseStack poseStack, Vec3 horizonColor) {
        // The zenith is darker than the horizon, like real atmospheric scattering at night - vanilla's dome
        // is a single flat color, which is what made the night sky look flat in the first place.
        Vec3 zenithColor = horizonColor.scale(0.35);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f pose = poseStack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(pose, 0.0F, 16.0F, 0.0F).setColor((float) zenithColor.x, (float) zenithColor.y, (float) zenithColor.z, 1.0F);
        for (int angle = -180; angle <= 180; angle += 45) {
            float x = 512.0F * Mth.cos(angle * Mth.DEG_TO_RAD);
            float z = 512.0F * Mth.sin(angle * Mth.DEG_TO_RAD);
            builder.addVertex(pose, x, 16.0F, z).setColor((float) horizonColor.x, (float) horizonColor.y, (float) horizonColor.z, 1.0F);
        }
        BufferUploader.drawWithShader(builder.build());
    }

    private static void renderMoon(ClientLevel level, float partialTick, PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));
        Matrix4f pose = poseStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, MOON_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int phase = level.getMoonPhase();
        int column = phase % 4;
        int row = phase / 4 % 2;
        float u0 = column / 4.0F;
        float v0 = row / 2.0F;
        float u1 = (column + 1) / 4.0F;
        float v1 = (row + 1) / 2.0F;

        float size = 20.0F;
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(pose, -size, -100.0F, size).setUv(u1, v1);
        builder.addVertex(pose, size, -100.0F, size).setUv(u0, v1);
        builder.addVertex(pose, size, -100.0F, -size).setUv(u0, v0);
        builder.addVertex(pose, -size, -100.0F, -size).setUv(u1, v0);
        BufferUploader.drawWithShader(builder.build());

        poseStack.popPose();
    }

    private static void renderDarkPlaneBelowHorizon(ClientLevel level, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix) {
        double heightAboveHorizon = camera.getPosition().y - level.getLevelData().getHorizonHeight(level);
        if (heightAboveHorizon >= 0.0) {
            return;
        }

        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        poseStack.pushPose();
        poseStack.translate(0.0F, 12.0F, 0.0F);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        Matrix4f pose = poseStack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        builder.addVertex(pose, 0.0F, -16.0F, 0.0F);
        for (int angle = -180; angle <= 180; angle += 45) {
            builder.addVertex(pose, -512.0F * Mth.cos(angle * Mth.DEG_TO_RAD), -16.0F, 512.0F * Mth.sin(angle * Mth.DEG_TO_RAD));
        }
        BufferUploader.drawWithShader(builder.build());
        poseStack.popPose();
    }
}
