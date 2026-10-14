package com.createastronautics.block;

import com.createastronautics.PlayerEnvironmentHandler;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Watches the block in front of its face for breathable air (see {@link PlayerEnvironmentHandler#hasOxygen})
 * and, while there is some, switches to its lit "detecting" textures and puts out a constant full-strength
 * redstone signal - on every side except the front, the same way Create's own smart observer does. A Display
 * Link reading it shows whether there's oxygen or not (see {@link OxygenDetectorDisplaySource}).
 *
 * Placed and wrenched like Create's own directional blocks: its face points back at whoever placed it, or
 * away from them while sneaking; a wrench rotates it, and sneak-wrenching picks it up (see {@link IWrenchable}).
 *
 * Polled on a scheduled tick rather than through a block entity: the only state it has is the one blockstate
 * property, and how often a room's seal is re-checked already bounds how fast the answer can change.
 */
public class OxygenDetectorBlock extends DirectionalBlock implements IWrenchable {
    public static final MapCodec<OxygenDetectorBlock> CODEC = simpleCodec(OxygenDetectorBlock::new);
    public static final BooleanProperty DETECTING = BooleanProperty.create("detecting");

    private static final int CHECK_INTERVAL_TICKS = 10;

    public OxygenDetectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DETECTING, false));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DETECTING);
    }

    // The face points back at whoever placed it, like a dispenser - so on a wall it looks out into the room
    // rather than into the wall it's mounted on - and sneaking flips it, the same rule Create's own
    // DirectionalKineticBlock places by.
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction looking = context.getNearestLookingDirection();
        boolean sneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        return defaultBlockState().setValue(FACING, sneaking ? looking : looking.getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    // A safety net for the tick chain: if it was ever lost (e.g. the block was moved without its pending
    // tick), any neighbour update starts it back up. Scheduling is a no-op while one is already pending.
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, CHECK_INTERVAL_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean detecting = PlayerEnvironmentHandler.hasOxygen(level, pos.relative(state.getValue(FACING)));
        if (detecting != state.getValue(DETECTING)) {
            level.setBlock(pos, state.setValue(DETECTING, detecting), Block.UPDATE_ALL);
        }
        level.scheduleTick(pos, this, CHECK_INTERVAL_TICKS);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    // `direction` points from the neighbour asking towards this block, so the neighbour sitting in front of
    // the face is the one asking from FACING's opposite.
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(DETECTING) && direction != state.getValue(FACING).getOpposite() ? 15 : 0;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != state.getValue(FACING).getOpposite();
    }
}
