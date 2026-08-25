package com.createastronautics.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Mirrors {@code com.simibubi.create.content.kinetics.fan.FanVisual} - this is the Flywheel-backed render
 * path (used whenever Flywheel visualization is active for the level) responsible for the spinning
 * shaft/propeller, using the same {@link AllPartialModels#ENCASED_FAN_INNER} model Create's own Encased Fan
 * uses. {@link OxygenFanRenderer} covers the fallback "safe" path for when Flywheel isn't active, since
 * Create's own {@code FanVisual} is hardcoded to Create's own block entity type and can't be reused directly.
 */
public class OxygenFanVisual extends KineticBlockEntityVisual<EncasedFanBlockEntity> {
    protected final RotatingInstance shaft;
    protected final RotatingInstance fan;
    private final Direction direction = blockState.getValue(BlockStateProperties.FACING);
    private final Direction opposite = direction.getOpposite();

    public OxygenFanVisual(VisualizationContext context, EncasedFanBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        shaft = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
                .createInstance();
        fan = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.ENCASED_FAN_INNER))
                .createInstance();
        shaft.setup(blockEntity).setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, opposite).setChanged();
        fan.setup(blockEntity, getFanSpeed()).setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, opposite).setChanged();
    }

    private float getFanSpeed() {
        float speed = blockEntity.getSpeed() * 5.0F;
        if (speed > 0.0F) {
            speed = Mth.clamp(speed, 80.0F, 1280.0F);
        }
        if (speed < 0.0F) {
            speed = Mth.clamp(speed, -1280.0F, -80.0F);
        }
        return speed;
    }

    @Override
    public void update(float pt) {
        shaft.setup((KineticBlockEntity) blockEntity).setChanged();
        fan.setup((KineticBlockEntity) blockEntity, getFanSpeed()).setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        BlockPos behind = pos.relative(opposite);
        relight(behind, shaft);
        BlockPos inFront = pos.relative(direction);
        relight(inFront, fan);
    }

    @Override
    protected void _delete() {
        shaft.delete();
        fan.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft);
        consumer.accept(fan);
    }
}
