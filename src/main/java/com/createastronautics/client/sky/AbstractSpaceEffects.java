package com.createastronautics.client.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
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
    private static final int STAR_COUNT = 2000;
    private static final long STAR_SEED = 231927L;

    @Nullable
    private static VertexBuffer starBuffer;

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
        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);
        setupFog.run();

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(modelViewMatrix);

        renderStars(poseStack, projectionMatrix);

        if (rendersSun()) {
            renderSun(level, partialTick, poseStack, projectionMatrix);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        return true;
    }

    private static void renderStars(PoseStack poseStack, Matrix4f projectionMatrix) {
        if (starBuffer == null) {
            starBuffer = buildStarBuffer();
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        ShaderInstance shader = RenderSystem.getShader();
        starBuffer.bind();
        starBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
        VertexBuffer.unbind();
    }

    private static void renderSun(ClientLevel level, float partialTick, PoseStack poseStack, Matrix4f projectionMatrix) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));

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

    private static VertexBuffer buildStarBuffer() {
        RandomSource random = RandomSource.create(STAR_SEED);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int placed = 0;
        while (placed < STAR_COUNT) {
            double x = random.nextFloat() * 2.0F - 1.0F;
            double y = random.nextFloat() * 2.0F - 1.0F;
            double z = random.nextFloat() * 2.0F - 1.0F;
            double lengthSq = x * x + y * y + z * z;
            if (lengthSq >= 1.0 || lengthSq <= 0.01) {
                continue;
            }

            double length = 1.0 / Math.sqrt(lengthSq);
            x *= length;
            y *= length;
            z *= length;

            Vec3 dir = new Vec3(x, y, z);
            Vec3 worldUp = Math.abs(y) > 0.99 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            Vec3 right = worldUp.cross(dir).normalize();
            Vec3 up = dir.cross(right).normalize();

            double roll = random.nextDouble() * Math.PI * 2.0;
            double cosRoll = Math.cos(roll);
            double sinRoll = Math.sin(roll);
            Vec3 rightRolled = right.scale(cosRoll).add(up.scale(sinRoll));
            Vec3 upRolled = right.scale(-sinRoll).add(up.scale(cosRoll));

            double size = 0.15 + random.nextFloat() * 0.15;
            double distance = 100.0;
            Vec3 center = dir.scale(distance);
            float brightness = 0.6F + random.nextFloat() * 0.4F;

            addStarQuad(builder, center, rightRolled.scale(size), upRolled.scale(size), brightness);
            placed++;
        }

        return uploadStatic(builder.build());
    }

    private static void addStarQuad(BufferBuilder builder, Vec3 center, Vec3 right, Vec3 up, float brightness) {
        Vec3 p1 = center.subtract(right).subtract(up);
        Vec3 p2 = center.add(right).subtract(up);
        Vec3 p3 = center.add(right).add(up);
        Vec3 p4 = center.subtract(right).add(up);

        builder.addVertex((float) p1.x, (float) p1.y, (float) p1.z).setColor(brightness, brightness, brightness, 1.0F);
        builder.addVertex((float) p2.x, (float) p2.y, (float) p2.z).setColor(brightness, brightness, brightness, 1.0F);
        builder.addVertex((float) p3.x, (float) p3.y, (float) p3.z).setColor(brightness, brightness, brightness, 1.0F);
        builder.addVertex((float) p4.x, (float) p4.y, (float) p4.z).setColor(brightness, brightness, brightness, 1.0F);
    }

    private static VertexBuffer uploadStatic(com.mojang.blaze3d.vertex.MeshData mesh) {
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(mesh);
        VertexBuffer.unbind();
        return buffer;
    }
}
