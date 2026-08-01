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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Shared "outer space" sky for the Moon and Deep Space dimensions: an always-dark, always-starry sky
 * with no atmosphere (no blue gradient, no sunrise/sunset colors, no clouds), since there's no air to
 * scatter light. Subclasses only decide whether the sun should also be drawn.
 *
 * The vanilla sky dome is fully replaced (skyType NONE + {@link #renderSky} always returns true), because
 * vanilla has no data-driven way to keep the starfield visible at all times while suppressing only the moon.
 */
public abstract class AbstractSpaceEffects extends DimensionSpecialEffects {
    private static final ResourceLocation SUN_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");

    // The Moon's dimension type pins timeOfDay/lighting to a constant noon via "fixed_time", so the sun's
    // sweep across the sky is driven by the absolute game clock instead - it keeps animating even though
    // the actual lighting never changes.
    private static final long VISUAL_DAY_LENGTH = 24000L;

    protected AbstractSpaceEffects(boolean hasGround) {
        super(Float.NaN, hasGround, SkyType.NONE, false, true);
    }

    /** Whether this dimension should additionally render the sun moving across the sky. */
    protected abstract boolean rendersSun();

    @Override
    public @NotNull Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return Vec3.ZERO;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    @Nullable
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return true;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(modelViewMatrix);

        RealisticStarField.render(poseStack, projectionMatrix, setupFog, 1.0F);

        if (rendersSun()) {
            RenderSystem.disableCull();
            renderSun(level, partialTick, poseStack, projectionMatrix);
            RenderSystem.enableCull();
        }

        return true;
    }

    private static void renderSun(ClientLevel level, float partialTick, PoseStack poseStack, Matrix4f projectionMatrix) {
        float visualTimeOfDay = ((level.getGameTime() % VISUAL_DAY_LENGTH) + partialTick) / (float) VISUAL_DAY_LENGTH;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(visualTimeOfDay * 360.0F));

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SUN_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        float size = 30.0F;
        Matrix4f pose = poseStack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(pose, -size, 100.0F, -size).setUv(0.0F, 0.0F);
        builder.addVertex(pose, size, 100.0F, -size).setUv(1.0F, 0.0F);
        builder.addVertex(pose, size, 100.0F, size).setUv(1.0F, 1.0F);
        builder.addVertex(pose, -size, 100.0F, size).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(builder.build());

        poseStack.popPose();
    }
}
