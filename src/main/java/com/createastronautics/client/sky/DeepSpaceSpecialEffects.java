package com.createastronautics.client.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;

/** Deep Space sky: the plain starfield with the Earth below, and no ground plane. */
public class DeepSpaceSpecialEffects extends AbstractSpaceEffects {
    public DeepSpaceSpecialEffects() {
        super(false);
    }

    @Override
    protected void renderCelestialBodies(PoseStack poseStack, Camera camera) {
        EarthRenderer.render(poseStack, EarthRenderer.heightFromDeepSpace(camera.getPosition().y), 1.0F);
    }
}
