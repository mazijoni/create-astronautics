package com.createastronautics.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A rocket booster that burns furnace fuel while powered by redstone, pushing any Sable ship it's mounted
 * to (see {@link SolidRocketBoosterBlockEntity}). Placeable in any of the 6 directions (sneaking places it
 * in the opposite orientation) and re-orientable afterwards with Create's wrench, courtesy of
 * {@link IWrenchable}'s default FACING-cycling behaviour.
 *
 * Boosters also power each other: any booster touching a powered booster (directly or through a chain of
 * touching boosters) is powered too, so a single redstone signal anywhere in a cluster lights up the whole
 * cluster - see {@link #updateBoosterNetwork}.
 */
public class SolidRocketBoosterBlock extends Block implements IBE<SolidRocketBoosterBlockEntity>, IWrenchable {
    // Safety cap on how large a touching-booster cluster we'll flood-fill in one go, so a pathological
    // build (e.g. a solid cube of boosters) can't stall the server.
    private static final int MAX_NETWORK_SIZE = 4096;

    public static final MapCodec<SolidRocketBoosterBlock> CODEC = simpleCodec(SolidRocketBoosterBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public SolidRocketBoosterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false).setValue(FACING, Direction.DOWN));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            facing = facing.getOpposite();
        }
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Only the intake face accepts items by hand - matches the fuel capability's own face restriction.
        if (hitResult.getDirection() != state.getValue(FACING).getOpposite()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return onBlockEntityUseItemOn(level, pos, be -> be.insertFuelFromPlayer(player, hand, stack));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // Empty-hand right-click on the intake face takes back whatever fuel is queued but not burning.
        if (hitResult.getDirection() != state.getValue(FACING).getOpposite()) {
            return InteractionResult.PASS;
        }
        return onBlockEntityUse(level, pos, be -> be.extractUnusedFuel(player) ? InteractionResult.SUCCESS : InteractionResult.PASS);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock())) {
            updateBoosterNetwork(level, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        updateBoosterNetwork(level, pos);
    }

    /**
     * Flood-fills the cluster of boosters touching {@code origin} and powers the whole cluster if any
     * member has a genuine external redstone signal, or unpowers it otherwise. Reading each member's own
     * {@link Level#hasNeighborSignal} (rather than another booster's cached POWERED value) means the
     * result never depends on stale state, so this converges correctly whether power is being added or
     * removed anywhere in the cluster.
     */
    private void updateBoosterNetwork(Level level, BlockPos origin) {
        if (level.isClientSide) {
            return;
        }

        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        List<BlockPos> members = new ArrayList<>();
        boolean anySignal = false;

        visited.add(origin);
        frontier.add(origin);
        while (!frontier.isEmpty() && members.size() < MAX_NETWORK_SIZE) {
            BlockPos pos = frontier.poll();
            members.add(pos);
            if (level.hasNeighborSignal(pos)) {
                anySignal = true;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = pos.relative(direction);
                if (visited.contains(neighborPos)) {
                    continue;
                }
                if (level.getBlockState(neighborPos).getBlock() instanceof SolidRocketBoosterBlock) {
                    visited.add(neighborPos);
                    frontier.add(neighborPos);
                }
            }
        }

        for (BlockPos memberPos : members) {
            BlockState memberState = level.getBlockState(memberPos);
            if (memberState.getValue(POWERED) != anySignal) {
                level.setBlock(memberPos, memberState.setValue(POWERED, anySignal), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public Class<SolidRocketBoosterBlockEntity> getBlockEntityClass() {
        return SolidRocketBoosterBlockEntity.class;
    }

    @Override
    public @Nullable BlockEntityType<? extends SolidRocketBoosterBlockEntity> getBlockEntityType() {
        return ModBlockEntities.SOLID_ROCKET_BOOSTER.get();
    }
}
