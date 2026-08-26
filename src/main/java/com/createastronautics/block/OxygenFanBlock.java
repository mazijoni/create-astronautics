package com.createastronautics.block;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * A reskinned clone of Create's own Encased Fan - kinetic-driven air current, entity pushing, item
 * processing (smoking/blasting/splashing/haunting), and the default wrench behaviour (plain click re-faces
 * the block, sneak-click picks it up) all come straight from {@link EncasedFanBlock} unchanged.
 *
 * On top of that, {@link #PIPE_ROTATION} is the axis Create's own wrench-rotatable blocks usually have that
 * a single {@code FACING} alone doesn't cover: a spin around the {@code FACING} axis itself, picking which
 * of the 4 side faces (relative to {@code FACING}) shows the pipe port. Since it only ever selects between
 * 4 pre-baked model variants that each move the port to a different local side, and {@code FACING}'s own
 * rotation always carries the model's top/bottom onto the fan/shaft axis (never a side), spinning this axis
 * can never land the port on the fan/shaft faces, and - just as importantly - never moves the fan/shaft
 * parts themselves, since {@link com.createastronautics.client.OxygenFanRenderer}/{@code OxygenFanVisual}
 * only ever read {@code FACING}.
 *
 * Matching how every other directional Create block handles wrenching: clicking one of the 4 side faces
 * re-faces the whole block to point that way (the default {@link EncasedFanBlock} behaviour, unchanged),
 * while clicking either end of the current {@code FACING} axis - the fan opening or the shaft/back face,
 * i.e. the two faces a re-face can't usefully point at since the block already points at one of them -
 * instead spins {@link #PIPE_ROTATION} to the next of its 4 sides in place.
 */
public class OxygenFanBlock extends EncasedFanBlock {
    public static final DirectionProperty PIPE_ROTATION = DirectionProperty.create("pipe_rotation", Direction.Plane.HORIZONTAL);

    public OxygenFanBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PIPE_ROTATION, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PIPE_ROTATION);
    }

    @Override
    public BlockEntityType<? extends EncasedFanBlockEntity> getBlockEntityType() {
        return ModBlockEntities.OXYGEN_FAN.get();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Direction facing = state.getValue(FACING);
        Direction clickedFace = context.getClickedFace();
        if (clickedFace != facing && clickedFace != facing.getOpposite()) {
            return super.onWrenched(state, context);
        }

        Level level = context.getLevel();
        if (!level.isClientSide) {
            Direction next = state.getValue(PIPE_ROTATION).getClockWise();
            level.setBlock(context.getClickedPos(), state.setValue(PIPE_ROTATION, next), Block.UPDATE_ALL);
            level.invalidateCapabilities(context.getClickedPos());
            IWrenchable.playRotateSound(level, context.getClickedPos());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Where {@link #PIPE_ROTATION} (a side of the raw, unrotated model) ends up in world space once the
     * blockstate's per-{@code FACING} rotation is applied - mirrors the exact x/y rotation values in
     * {@code blockstates/oxygen_fan.json}. Since {@code PIPE_ROTATION} only ever selects one of the 4 side
     * faces and {@code FACING}'s rotation always carries the model's own top/bottom faces onto the
     * fan/shaft axis (never a side), this can never resolve to {@code FACING} or its opposite.
     */
    public static Direction pipeWorldDirection(Direction facing, Direction pipeRotation) {
        return switch (facing) {
            case UP -> pipeRotation;
            case DOWN -> switch (pipeRotation) {
                case NORTH -> Direction.SOUTH;
                case SOUTH -> Direction.NORTH;
                default -> pipeRotation;
            };
            case NORTH -> switch (pipeRotation) {
                case NORTH -> Direction.DOWN;
                case SOUTH -> Direction.UP;
                case EAST -> Direction.EAST;
                default -> Direction.WEST;
            };
            case SOUTH -> switch (pipeRotation) {
                case NORTH -> Direction.DOWN;
                case SOUTH -> Direction.UP;
                case EAST -> Direction.WEST;
                default -> Direction.EAST;
            };
            case EAST -> switch (pipeRotation) {
                case NORTH -> Direction.DOWN;
                case SOUTH -> Direction.UP;
                case EAST -> Direction.SOUTH;
                default -> Direction.NORTH;
            };
            case WEST -> switch (pipeRotation) {
                case NORTH -> Direction.DOWN;
                case SOUTH -> Direction.UP;
                case EAST -> Direction.NORTH;
                default -> Direction.SOUTH;
            };
        };
    }
}
