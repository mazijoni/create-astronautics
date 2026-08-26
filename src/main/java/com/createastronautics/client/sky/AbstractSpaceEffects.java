package com.createastronautics.client.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Shared "outer space" sky for the Moon and Deep Space dimensions: an always-dark, always-starry sky
 * with no atmosphere (no blue gradient, no sunrise/sunset colors, no clouds, no sun), since there's no
 * air to scatter light and both dimensions are permanently on their night side.
 *
 * The vanilla sky dome is fully replaced (skyType NONE + {@link #renderSky} always returns true), because
 * vanilla has no data-driven way to keep the starfield visible at all times while suppressing the sun/moon.
 */
public abstract class AbstractSpaceEffects extends DimensionSpecialEffects {
    protected AbstractSpaceEffects(boolean hasGround) {
        super(Float.NaN, hasGround, SkyType.NONE, false, true);
    }

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

        return true;
    }
}
