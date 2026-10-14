package com.createastronautics.client.sky;

import com.createastronautics.config.Config;
import com.mojang.blaze3d.platform.GlStateManager;
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
 * Only takes over once {@link ClientLevel#getSkyDarken} is down at its night-time minimum; every other condition
 * that would normally suppress vanilla's sky (fog, lava/powder snow in the camera, blindness/darkness) is
 * checked here too, since returning {@code true} skips vanilla's own checks for those entirely.
 */
public class RealisticOverworldEffects extends DimensionSpecialEffects.OverworldEffects {
    private static final ResourceLocation MOON_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
    private static final ResourceLocation SUN_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    // ClientLevel#getSkyDarken is, despite the name, how bright the sky is: 1 at noon, falling to exactly
    // this at full night. A hair above it so the check isn't at the mercy of float rounding.
    private static final float NIGHT_SKY_BRIGHTNESS = 0.2F;
    private static final float NIGHT_THRESHOLD = NIGHT_SKY_BRIGHTNESS + 0.01F;

    // The sky dome, as rings of quads at these elevations above the horizon (degrees) - packed closer
    // together near the horizon, where the colour changes fastest. It dips a little below the horizon so
    // there's no gap to see past its rim.
    private static final float[] DOME_RING_ELEVATIONS = {90.0F, 70.0F, 50.0F, 35.0F, 25.0F, 18.0F, 12.0F, 7.0F, 3.0F, 0.0F, -10.0F};
    private static final int DOME_SEGMENTS = 24;
    private static final float DOME_RADIUS = 100.0F;
    // Below this elevation the sky colour is blending into the fog colour at the horizon.
    private static final float DOME_HAZE_TOP = 30.0F;

    // Three cloud decks: vanilla's own height, then two more above it. Per deck: where on the cloud texture
    // it starts {x, z, in blocks} and how fast it drifts {blocks per tick; vanilla's single deck does 0.03}.
    private static final float CLOUD_BASE_HEIGHT = 192.0F;
    private static final float CLOUD_LAYER_SPACING = 40.0F;
    private static final float CLOUD_OPACITY = 0.7F;
    private static final double[][] CLOUD_LAYERS = {
            {0.0, 0.0, 0.03},
            {1004.0, 1708.0, 0.05},
            {-1900.0, 612.0, 0.018},
    };

    private final CloudLayerRenderer clouds = new CloudLayerRenderer(CLOUD_BASE_HEIGHT, CLOUD_LAYER_SPACING, CLOUD_OPACITY, CLOUD_LAYERS);
    private static final float NIGHT_ZENITH_SCALE = 0.35F;
    // The fade to black is squeezed into a shorter band if the transition height is configured very low.
    private static final double SPACE_FADE_MAX_RANGE = 8000.0;

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f frustumMatrix, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable skyFogSetup) {
        float spaceFade = spaceFade(camera.getPosition().y);
        boolean night = level.getSkyDarken(partialTick) <= NIGHT_THRESHOLD;
        if (isFoggy || (!night && spaceFade <= 0.0F) || blocksSky(camera)) {
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
        Vec3 skyColor = level.getSkyColor(camera.getPosition(), partialTick).scale(1.0F - spaceFade);
        // By day the dome starts out flat like vanilla's (so taking over from it isn't visible) and only
        // picks up the darker zenith as the air thins out.
        renderGradientDome(poseStack, skyColor, night ? NIGHT_ZENITH_SCALE : Mth.lerp(spaceFade, 1.0F, NIGHT_ZENITH_SCALE));

        RenderSystem.enableBlend();
        if (!night) {
            // Vanilla's own sun is skipped along with the rest of its sky whenever this returns true.
            renderSun(level, partialTick, poseStack);
        }
        renderMoon(level, partialTick, poseStack);

        float starIntensity = level.getStarBrightness(partialTick) * (1.0F - level.getRainLevel(partialTick));
        RealisticStarField.render(poseStack, projectionMatrix, skyFogSetup, Math.max(starIntensity, spaceFade));

        renderDarkPlaneBelowHorizon(level, camera, poseStack, projectionMatrix);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        return true;
    }

    private static boolean blocksSky(Camera camera) {
        return camera.getEntity() instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS));
    }

    /**
     * How far the camera is into the thinning upper atmosphere on the way up to Deep Space: 0 anywhere near
     * the ground, rising to 1 at {@link Config#SPACE_TRANSITION_HEIGHT}, where the sky has to already be
     * the black, fully starry one Deep Space has - otherwise it would visibly pop at the dimension change.
     */
    public static float spaceFade(double cameraY) {
        double top = Config.SPACE_TRANSITION_HEIGHT.get();
        double range = Math.min(SPACE_FADE_MAX_RANGE, top * 0.4);
        return (float) Mth.clamp((cameraY - (top - range)) / range, 0.0, 1.0);
    }

    private static void renderSun(ClientLevel level, float partialTick, PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));
        Matrix4f pose = poseStack.last().pose();

        // Additive, same as vanilla's sun - its texture is a bright square on black rather than cut out.
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SUN_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F - level.getRainLevel(partialTick));

        float size = 30.0F;
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(pose, -size, 100.0F, -size).setUv(0.0F, 0.0F);
        builder.addVertex(pose, size, 100.0F, -size).setUv(1.0F, 0.0F);
        builder.addVertex(pose, size, 100.0F, size).setUv(1.0F, 1.0F);
        builder.addVertex(pose, -size, 100.0F, size).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(builder.build());

        RenderSystem.defaultBlendFunc();
        poseStack.popPose();
    }

    /**
     * A full dome of sky running smoothly from the zenith colour overhead, through the plain sky colour,
     * down into the fog colour exactly at the horizon. The fog colour is also what the rest of the screen
     * below the horizon is cleared to, so the dome has no visible edge - unlike a flat-coloured cap, which
     * leaves a hard line wherever it stops and the fog colour begins.
     */
    private static void renderGradientDome(PoseStack poseStack, Vec3 skyColor, float zenithScale) {
        // The zenith is darker than the rest of the sky, like real atmospheric scattering - vanilla's dome
        // is a single flat color, which is what made the night sky look flat in the first place.
        Vec3 zenithColor = skyColor.scale(zenithScale);
        float[] fog = RenderSystem.getShaderFogColor();
        Vec3 fogColor = new Vec3(fog[0], fog[1], fog[2]);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f pose = poseStack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int ring = 0; ring < DOME_RING_ELEVATIONS.length - 1; ring++) {
            float upper = DOME_RING_ELEVATIONS[ring];
            float lower = DOME_RING_ELEVATIONS[ring + 1];
            Vec3 upperColor = domeColor(upper, zenithColor, skyColor, fogColor);
            Vec3 lowerColor = domeColor(lower, zenithColor, skyColor, fogColor);
            for (int segment = 0; segment < DOME_SEGMENTS; segment++) {
                float angle0 = Mth.TWO_PI * segment / DOME_SEGMENTS;
                float angle1 = Mth.TWO_PI * (segment + 1) / DOME_SEGMENTS;
                domeVertex(builder, pose, upper, angle0, upperColor);
                domeVertex(builder, pose, lower, angle0, lowerColor);
                domeVertex(builder, pose, lower, angle1, lowerColor);
                domeVertex(builder, pose, upper, angle1, upperColor);
            }
        }
        // Seen from inside, the dome's quads face away from the camera.
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.enableCull();
    }

    /** Fog colour at and below the horizon, easing up into the sky colour, then on up into the zenith colour. */
    private static Vec3 domeColor(float elevation, Vec3 zenithColor, Vec3 skyColor, Vec3 fogColor) {
        if (elevation <= 0.0F) {
            return fogColor;
        }
        if (elevation < DOME_HAZE_TOP) {
            float t = elevation / DOME_HAZE_TOP;
            return fogColor.lerp(skyColor, t * t * (3.0F - 2.0F * t));
        }
        return skyColor.lerp(zenithColor, (elevation - DOME_HAZE_TOP) / (90.0F - DOME_HAZE_TOP));
    }

    private static void domeVertex(BufferBuilder builder, Matrix4f pose, float elevation, float angle, Vec3 color) {
        float horizontal = DOME_RADIUS * Mth.cos(elevation * Mth.DEG_TO_RAD);
        builder.addVertex(pose, horizontal * Mth.cos(angle), DOME_RADIUS * Mth.sin(elevation * Mth.DEG_TO_RAD), horizontal * Mth.sin(angle))
                .setColor((float) color.x, (float) color.y, (float) color.z, 1.0F);
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

    /** Replaces vanilla's single cloud deck with {@link CloudLayerRenderer}'s three. */
    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        clouds.render(level, ticks, partialTick, modelViewMatrix, projectionMatrix, camX, camY, camZ);
        return true;
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
