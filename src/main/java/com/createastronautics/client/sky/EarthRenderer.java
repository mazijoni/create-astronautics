package com.createastronautics.client.sky;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.config.Config;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Draws the Earth below the camera, at the distance and size it would really have if the Overworld were
 * the top face of a giant cube: seen from Deep Space its surface is {@link Config#SPACE_TRANSITION_HEIGHT}
 * blocks below {@link Config#DEEP_SPACE_EXIT_HEIGHT}, so it fills the view beneath a ship that has just
 * arrived and shrinks into a visible cube as the ship climbs away. The same cube is also faded in beneath
 * a ship still climbing through the Overworld (see {@link SpaceFadeFogHandler}), so it's already there
 * when the dimension changes.
 *
 * It can't be drawn at its true distance (far outside the far plane), so - same trick as Create-Cosmonautics'
 * PlanetRenderer - it's drawn at a fixed sky distance instead and scaled down by the same ratio, which
 * leaves its apparent size and position unchanged.
 *
 * Always centred horizontally under the camera, unlit, and not rotating - placeholder behaviour to go with
 * the placeholder model.
 */
public final class EarthRenderer {
    private static final ResourceLocation EARTH_LOCATION =
            ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "textures/environment/earth.png");

    private static final float SKY_DISTANCE = 100.0F;
    private static final float TEXTURE_SIZE = 64.0F;
    // The overlay cube in the source model is 17 wide against the surface cube's 16.
    private static final float CLOUD_SCALE = 17.0F / 16.0F;

    // Per face {u1, v1, u2, v2} in texels, in FACES order - (u1, v1) is the face's top-left corner seen from
    // outside. Taken from the Blockbench model the texture was painted on (temp/earth.bbmodel).
    private static final float[][] SURFACE_UVS = {
            {32, 16, 16, 0},  // up
            {48, 0, 32, 16},  // down
            {16, 16, 32, 32}, // north
            {32, 16, 48, 32}, // south
            {48, 16, 64, 32}, // west
            {0, 16, 16, 32},  // east
    };
    private static final float[][] CLOUD_UVS = {
            {32, 64, 16, 48},
            {48, 48, 32, 64},
            {16, 32, 32, 48},
            {32, 32, 48, 48},
            {48, 32, 64, 48},
            {0, 32, 16, 48},
    };

    // Per face, its corners on a unit cube in the order top-left, bottom-left, bottom-right, top-right as
    // seen from outside - counter-clockwise, so back faces cull and the two nested cubes need no depth test.
    private static final float[][][] FACES = {
            {{-1, 1, -1}, {-1, 1, 1}, {1, 1, 1}, {1, 1, -1}},     // up
            {{-1, -1, 1}, {-1, -1, -1}, {1, -1, -1}, {1, -1, 1}}, // down
            {{1, 1, -1}, {1, -1, -1}, {-1, -1, -1}, {-1, 1, -1}}, // north
            {{-1, 1, 1}, {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}},     // south
            {{-1, 1, -1}, {-1, -1, -1}, {-1, -1, 1}, {-1, 1, 1}}, // west
            {{1, 1, 1}, {1, -1, 1}, {1, -1, -1}, {1, 1, -1}},     // east
    };

    private EarthRenderer() {}

    /** How far above the Earth's surface a camera at the given Deep Space Y level is. */
    public static double heightFromDeepSpace(double cameraY) {
        return cameraY - Config.DEEP_SPACE_EXIT_HEIGHT.get() + Config.SPACE_TRANSITION_HEIGHT.get();
    }

    public static void render(PoseStack poseStack, double heightAboveSurface, float alpha) {
        double radius = Config.EARTH_RADIUS.get();
        double distanceToCentre = Math.max(1.0, heightAboveSurface) + radius;
        float scale = (float) (radius * SKY_DISTANCE / distanceToCentre);

        poseStack.pushPose();
        poseStack.translate(0.0F, -SKY_DISTANCE, 0.0F);

        RenderSystem.depthMask(false);
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, EARTH_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        drawCube(poseStack.last().pose(), scale, SURFACE_UVS);
        drawCube(poseStack.last().pose(), scale * CLOUD_SCALE, CLOUD_UVS);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        poseStack.popPose();
    }

    private static void drawCube(Matrix4f pose, float halfSize, float[][] uvs) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (int face = 0; face < FACES.length; face++) {
            float[][] corners = FACES[face];
            float u1 = uvs[face][0] / TEXTURE_SIZE;
            float v1 = uvs[face][1] / TEXTURE_SIZE;
            float u2 = uvs[face][2] / TEXTURE_SIZE;
            float v2 = uvs[face][3] / TEXTURE_SIZE;

            vertex(builder, pose, corners[0], halfSize, u1, v1);
            vertex(builder, pose, corners[1], halfSize, u1, v2);
            vertex(builder, pose, corners[2], halfSize, u2, v2);
            vertex(builder, pose, corners[3], halfSize, u2, v1);
        }
        BufferUploader.drawWithShader(builder.build());
    }

    private static void vertex(BufferBuilder builder, Matrix4f pose, float[] corner, float halfSize, float u, float v) {
        builder.addVertex(pose, corner[0] * halfSize, corner[1] * halfSize, corner[2] * halfSize).setUv(u, v);
    }
}
