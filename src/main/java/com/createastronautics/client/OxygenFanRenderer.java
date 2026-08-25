package com.createastronautics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Mirrors {@code com.simibubi.create.content.kinetics.fan.EncasedFanRenderer} exactly - same speed clamping,
 * animation math, and even the same propeller model ({@link AllPartialModels#ENCASED_FAN_INNER}), since the
 * fan looks and behaves identically to Create's own Encased Fan. The only reason this class exists at all
 * instead of reusing Create's renderer directly is that Create's is hardcoded to its own block entity type.
 * <p>
 * This "safe"/legacy renderer only actually draws anything when Flywheel visualization is unavailable for
 * the level - when it is available, {@link OxygenFanVisual} (registered alongside this one) takes over
 * instead, matching how Create itself splits the two rendering paths for its own Encased Fan.
 */
public class OxygenFanRenderer extends KineticBlockEntityRenderer<EncasedFanBlockEntity> {
    public OxygenFanRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(EncasedFanBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) {
            return;
        }

        Direction direction = be.getBlockState().getValue(BlockStateProperties.FACING);
        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
        int lightBehind = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(direction.getOpposite()));
        int lightInFront = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(direction));
        SuperByteBuffer shaftHalf = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction.getOpposite());
        SuperByteBuffer fanInner = CachedBuffers.partialFacing(AllPartialModels.ENCASED_FAN_INNER, be.getBlockState(), direction.getOpposite());

        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float speed = be.getSpeed() * 5.0F;
        if (speed > 0.0F) {
            speed = Mth.clamp(speed, 80.0F, 1280.0F);
        }
        if (speed < 0.0F) {
            speed = Mth.clamp(speed, -1280.0F, -80.0F);
        }

        float angle = time * speed * 3.0F / 10.0F % 360.0F;
        angle = angle / 180.0F * (float) Math.PI;
        standardKineticRotationTransform(shaftHalf, be, lightBehind).renderInto(ms, vb);
        kineticRotationTransform(fanInner, be, direction.getAxis(), angle, lightInFront).renderInto(ms, vb);
    }
}
