package com.createastronautics.client.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws the Overworld's clouds as several decks at different heights instead of vanilla's one. Each deck
 * reads a different part of the cloud texture and drifts at its own speed, so they never line up into
 * copies of each other.
 *
 * This replaces the game's cloud drawing outright rather than calling it once per deck: other renderers
 * (Sodium, for one) swap that code for their own, which takes the camera's real position instead of the one
 * it's handed, so decks drawn through it can be raised but not shifted sideways - and all come out
 * identical. The look is still vanilla's: the same texture, shader, block sizes, face shading and
 * depth-then-colour passes as {@code LevelRenderer#renderClouds}.
 *
 * A deck's mesh covers a fixed square of cloud cells around the camera and only depends on which cell the
 * camera is over, so it's rebuilt when the camera (or the drifting clouds) crosses into the next cell, not
 * every frame. The cloud colour for the time of day is applied when drawing, not baked in.
 */
public final class CloudLayerRenderer {
    // One texel of the cloud texture is one cell: 12x12 blocks across and 4 thick, same as vanilla.
    private static final float CELL_SIZE = 12.0F;
    private static final float THICKNESS = 4.0F;
    private static final int TEXTURE_SIZE = 256;
    private static final float TEXEL = 1.0F / TEXTURE_SIZE;
    // Faces that would otherwise share a plane with their neighbour's are pulled in by this much.
    private static final float SEAM = 9.765625E-4F;

    // The mesh is built from square tiles of TILE cells, HALF_TILES of them out from the camera each way -
    // 432 blocks, a little further than vanilla's fancy clouds reach.
    private static final int TILE = 8;
    private static final int HALF_TILES = 4;

    // Face brightness, same values vanilla shades its cloud boxes with.
    private static final float SHADE_TOP = 1.0F;
    private static final float SHADE_BOTTOM = 0.7F;
    private static final float SHADE_X = 0.9F;
    private static final float SHADE_Z = 0.8F;
    private static final float VANILLA_ALPHA = 0.8F;

    private final Layer[] layers;
    private final float opacity;

    /**
     * @param baseHeight the Y level of the lowest deck
     * @param spacing    how far apart the decks are, in blocks
     * @param opacity    multiplied onto vanilla's own 80% cloud opacity
     * @param layers     per deck: {x offset, z offset, drift speed} - offsets in blocks into the texture
     *                   (which repeats every 3072), speed in blocks per tick along X (vanilla's is 0.03)
     */
    public CloudLayerRenderer(float baseHeight, float spacing, float opacity, double[][] layers) {
        this.opacity = opacity;
        this.layers = new Layer[layers.length];
        for (int i = 0; i < layers.length; i++) {
            this.layers[i] = new Layer(baseHeight + spacing * i, layers[i][0], layers[i][1], layers[i][2]);
        }
    }

    public void render(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Matrix4f projectionMatrix,
                       double camX, double camY, double camZ) {
        CloudStatus status = Minecraft.getInstance().options.getCloudsType();
        if (status == CloudStatus.OFF) {
            return;
        }
        boolean fancy = status == CloudStatus.FANCY;

        Vec3 color = level.getCloudColor(partialTick);
        FogRenderer.levelFogColor();
        RenderSystem.setShaderColor((float) color.x, (float) color.y, (float) color.z, opacity);

        // Translucent decks have to go down furthest-first to blend properly: top-down from below the
        // clouds, bottom-up from above them.
        boolean fromAbove = camY > (layers[0].height + layers[layers.length - 1].height) / 2.0F;
        for (int i = 0; i < layers.length; i++) {
            Layer layer = layers[fromAbove ? i : layers.length - 1 - i];
            layer.render(ticks + partialTick, fancy, modelViewMatrix, projectionMatrix, camX, camY, camZ);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static final class Layer {
        private final float height;
        private final double offsetX;
        private final double offsetZ;
        private final double speed;

        private VertexBuffer buffer;
        private int builtCellX = Integer.MIN_VALUE;
        private int builtCellZ = Integer.MIN_VALUE;
        private boolean builtFancy;

        Layer(float height, double offsetX, double offsetZ, double speed) {
            this.height = height;
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
            this.speed = speed;
        }

        void render(float time, boolean fancy, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, double camX, double camY, double camZ) {
            // Where the camera is over this deck's texture, in cells.
            double cellX = (camX + offsetX + time * speed) / CELL_SIZE;
            double cellZ = (camZ + offsetZ) / CELL_SIZE + 0.33;
            int wholeX = Mth.floor(cellX);
            int wholeZ = Mth.floor(cellZ);

            if (buffer == null || wholeX != builtCellX || wholeZ != builtCellZ || fancy != builtFancy) {
                rebuild(wholeX, wholeZ, fancy);
            }

            PoseStack poseStack = new PoseStack();
            poseStack.mulPose(modelViewMatrix);
            poseStack.translate(
                    -(float) (cellX - wholeX) * CELL_SIZE,
                    (float) (height - camY + 0.33),
                    -(float) (cellZ - wholeZ) * CELL_SIZE);
            poseStack.scale(CELL_SIZE, 1.0F, CELL_SIZE);
            Matrix4f pose = poseStack.last().pose();

            buffer.bind();
            // Fancy clouds lay down depth alone first, so the faces of one deck hide each other properly
            // instead of all showing through one another; flat ones have nothing to hide.
            for (int pass = fancy ? 0 : 1; pass < 2; pass++) {
                RenderType renderType = pass == 0 ? RenderType.cloudsDepthOnly() : RenderType.clouds();
                renderType.setupRenderState();
                buffer.drawWithShader(pose, projectionMatrix, RenderSystem.getShader());
                renderType.clearRenderState();
            }
            VertexBuffer.unbind();
        }

        /**
         * Builds every face of every tile, with no texture knowledge at all: each tile's top and bottom are
         * one quad stretched over its 8x8 texels, and its sides are one tall strip per row and column of
         * cells, each sampling the middle of the texel it belongs to. Empty texels are transparent, so
         * whatever faces fall on them simply don't show - which is how vanilla does it too.
         */
        private void rebuild(int cellX, int cellZ, boolean fancy) {
            builtCellX = cellX;
            builtCellZ = cellZ;
            builtFancy = fancy;

            // Kept within one repeat of the texture so the UVs stay small and exact.
            float u0 = Math.floorMod(cellX, TEXTURE_SIZE) * TEXEL;
            float v0 = Math.floorMod(cellZ, TEXTURE_SIZE) * TEXEL;

            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
            for (int tileX = -HALF_TILES; tileX < HALF_TILES; tileX++) {
                for (int tileZ = -HALF_TILES; tileZ < HALF_TILES; tileZ++) {
                    float x = tileX * TILE;
                    float z = tileZ * TILE;
                    float uMin = x * TEXEL + u0;
                    float uMax = (x + TILE) * TEXEL + u0;
                    float vMin = z * TEXEL + v0;
                    float vMax = (z + TILE) * TEXEL + v0;

                    // Bottom.
                    vertex(builder, x, 0.0F, z + TILE, uMin, vMax, SHADE_BOTTOM, 0, -1, 0);
                    vertex(builder, x + TILE, 0.0F, z + TILE, uMax, vMax, SHADE_BOTTOM, 0, -1, 0);
                    vertex(builder, x + TILE, 0.0F, z, uMax, vMin, SHADE_BOTTOM, 0, -1, 0);
                    vertex(builder, x, 0.0F, z, uMin, vMin, SHADE_BOTTOM, 0, -1, 0);
                    if (!fancy) {
                        continue;
                    }

                    // Top.
                    float top = THICKNESS - SEAM;
                    vertex(builder, x, top, z + TILE, uMin, vMax, SHADE_TOP, 0, 1, 0);
                    vertex(builder, x + TILE, top, z + TILE, uMax, vMax, SHADE_TOP, 0, 1, 0);
                    vertex(builder, x + TILE, top, z, uMax, vMin, SHADE_TOP, 0, 1, 0);
                    vertex(builder, x, top, z, uMin, vMin, SHADE_TOP, 0, 1, 0);

                    for (int i = 0; i < TILE; i++) {
                        float u = (x + i + 0.5F) * TEXEL + u0;
                        float v = (z + i + 0.5F) * TEXEL + v0;

                        // West and east faces of the cells in column i.
                        float west = x + i;
                        vertex(builder, west, 0.0F, z + TILE, u, vMax, SHADE_X, -1, 0, 0);
                        vertex(builder, west, THICKNESS, z + TILE, u, vMax, SHADE_X, -1, 0, 0);
                        vertex(builder, west, THICKNESS, z, u, vMin, SHADE_X, -1, 0, 0);
                        vertex(builder, west, 0.0F, z, u, vMin, SHADE_X, -1, 0, 0);
                        float east = x + i + 1.0F - SEAM;
                        vertex(builder, east, 0.0F, z + TILE, u, vMax, SHADE_X, 1, 0, 0);
                        vertex(builder, east, THICKNESS, z + TILE, u, vMax, SHADE_X, 1, 0, 0);
                        vertex(builder, east, THICKNESS, z, u, vMin, SHADE_X, 1, 0, 0);
                        vertex(builder, east, 0.0F, z, u, vMin, SHADE_X, 1, 0, 0);

                        // North and south faces of the cells in row i.
                        float north = z + i;
                        vertex(builder, x, THICKNESS, north, uMin, v, SHADE_Z, 0, 0, -1);
                        vertex(builder, x + TILE, THICKNESS, north, uMax, v, SHADE_Z, 0, 0, -1);
                        vertex(builder, x + TILE, 0.0F, north, uMax, v, SHADE_Z, 0, 0, -1);
                        vertex(builder, x, 0.0F, north, uMin, v, SHADE_Z, 0, 0, -1);
                        float south = z + i + 1.0F - SEAM;
                        vertex(builder, x, THICKNESS, south, uMin, v, SHADE_Z, 0, 0, 1);
                        vertex(builder, x + TILE, THICKNESS, south, uMax, v, SHADE_Z, 0, 0, 1);
                        vertex(builder, x + TILE, 0.0F, south, uMax, v, SHADE_Z, 0, 0, 1);
                        vertex(builder, x, 0.0F, south, uMin, v, SHADE_Z, 0, 0, 1);
                    }
                }
            }

            if (buffer == null) {
                buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            }
            buffer.bind();
            buffer.upload(builder.buildOrThrow());
            VertexBuffer.unbind();
        }

        private static void vertex(BufferBuilder builder, float x, float y, float z, float u, float v, float shade,
                                   float normalX, float normalY, float normalZ) {
            builder.addVertex(x, y, z).setUv(u, v).setColor(shade, shade, shade, VANILLA_ALPHA).setNormal(normalX, normalY, normalZ);
        }
    }
}
