package com.createastronautics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * A near-verbatim port of vanilla's {@code net.minecraft.world.level.block.PointedDripstoneBlock}
 * (1.21.1/NeoForge), with every reference to {@code Blocks.POINTED_DRIPSTONE} swapped for {@link
 * ModBlocks#POINTED_MOONSTONE} and every reference to {@code Blocks.DRIPSTONE_BLOCK} swapped for {@link
 * ModBlocks#MOONSTONE_BLOCK}. A from-scratch reimplementation of placement/thickness/merging got this wrong
 * in ways that weren't obvious until tested in-game (stacking a segment on top of another failed), so this
 * keeps vanilla's actual battle-tested logic instead - including growth (a moonstone tip lengthens the same
 * way dripstone does, from a water source two blocks above a moonstone_block ceiling - dormant on the Moon
 * without a player-supplied water source, since none generates there naturally) and dripping into cauldrons.
 */
public class PointedMoonstoneBlock extends Block implements Fallable, SimpleWaterloggedBlock {
    public static final DirectionProperty TIP_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
    public static final EnumProperty<DripstoneThickness> THICKNESS = BlockStateProperties.DRIPSTONE_THICKNESS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final int MAX_SEARCH_LENGTH_WHEN_CHECKING_DRIP_TYPE = 11;
    private static final int DELAY_BEFORE_FALLING = 2;
    private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK = 0.02F;
    private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK_IF_UNDER_LIQUID_SOURCE = 0.12F;
    private static final int MAX_SEARCH_LENGTH_BETWEEN_STALACTITE_TIP_AND_CAULDRON = 11;
    public static final float WATER_TRANSFER_PROBABILITY_PER_RANDOM_TICK = 0.17578125F;
    public static final float LAVA_TRANSFER_PROBABILITY_PER_RANDOM_TICK = 0.05859375F;
    private static final double MIN_TRIDENT_VELOCITY_TO_BREAK_DRIPSTONE = 0.6;
    private static final float STALACTITE_DAMAGE_PER_FALL_DISTANCE_AND_SIZE = 1.0F;
    private static final int STALACTITE_MAX_DAMAGE = 40;
    private static final int MAX_STALACTITE_HEIGHT_FOR_DAMAGE_CALCULATION = 6;
    private static final float STALAGMITE_FALL_DISTANCE_OFFSET = 2.0F;
    private static final int STALAGMITE_FALL_DAMAGE_MODIFIER = 2;
    private static final float AVERAGE_DAYS_PER_GROWTH = 5.0F;
    private static final float GROWTH_PROBABILITY_PER_RANDOM_TICK = 0.011377778F;
    private static final int MAX_GROWTH_LENGTH = 7;
    private static final int MAX_STALAGMITE_SEARCH_RANGE_WHEN_GROWING = 10;
    private static final float STALACTITE_DRIP_START_PIXEL = 0.6875F;
    private static final VoxelShape TIP_MERGE_SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape TIP_SHAPE_UP = Block.box(5.0, 0.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape TIP_SHAPE_DOWN = Block.box(5.0, 5.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape FRUSTUM_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape MIDDLE_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    private static final VoxelShape BASE_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
    private static final float MAX_HORIZONTAL_OFFSET = 0.125F;
    private static final VoxelShape REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);

    public PointedMoonstoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(TIP_DIRECTION, Direction.UP)
                .setValue(THICKNESS, DripstoneThickness.TIP)
                .setValue(WATERLOGGED, Boolean.valueOf(false))
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP_DIRECTION, THICKNESS, WATERLOGGED);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isValidPointedMoonstonePlacement(level, pos, state.getValue(TIP_DIRECTION));
    }

    @Override
    protected BlockState updateShape(
        BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos
    ) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        if (direction != Direction.UP && direction != Direction.DOWN) {
            return state;
        } else {
            Direction tipDirection = state.getValue(TIP_DIRECTION);
            if (tipDirection == Direction.DOWN && level.getBlockTicks().hasScheduledTick(pos, this)) {
                return state;
            } else if (direction == tipDirection.getOpposite() && !this.canSurvive(state, level, pos)) {
                if (tipDirection == Direction.DOWN) {
                    level.scheduleTick(pos, this, 2);
                } else {
                    level.scheduleTick(pos, this, 1);
                }

                return state;
            } else {
                boolean isMerged = state.getValue(THICKNESS) == DripstoneThickness.TIP_MERGE;
                DripstoneThickness thickness = calculateDripstoneThickness(level, pos, tipDirection, isMerged);
                return state.setValue(THICKNESS, thickness);
            }
        }
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hitResult, Projectile projectile) {
        if (!level.isClientSide) {
            BlockPos pos = hitResult.getBlockPos();
            if (projectile.mayInteract(level, pos)
                && projectile.mayBreak(level)
                && projectile instanceof ThrownTrident
                && projectile.getDeltaMovement().length() > 0.6) {
                level.destroyBlock(pos, true);
            }
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        if (state.getValue(TIP_DIRECTION) == Direction.UP && state.getValue(THICKNESS) == DripstoneThickness.TIP) {
            entity.causeFallDamage(distance + 2.0F, 2.0F, level.damageSources().stalagmite());
        } else {
            super.fallOn(level, state, pos, entity, distance);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (canDrip(state)) {
            float f = random.nextFloat();
            if (!(f > 0.12F)) {
                getFluidAboveStalactite(level, pos, state)
                    .filter(info -> f < 0.02F || canFillCauldron(info.fluid))
                    .ifPresent(info -> spawnDripParticle(level, pos, state, info.fluid));
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isStalagmite(state) && !this.canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        } else {
            spawnFallingStalactite(state, level, pos);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        maybeTransferFluid(state, level, pos, random.nextFloat());
        if (random.nextFloat() < GROWTH_PROBABILITY_PER_RANDOM_TICK && isStalactiteStartPos(state, level, pos)) {
            growStalactiteOrStalagmiteIfPossible(state, level, pos, random);
        }
    }

    public static void maybeTransferFluid(BlockState state, ServerLevel level, BlockPos pos, float random) {
        if (isStalactiteStartPos(state, level, pos)) {
            Optional<FluidInfo> optional = getFluidAboveStalactite(level, pos, state);
            if (!optional.isEmpty()) {
                Fluid fluid = optional.get().fluid;
                net.neoforged.neoforge.fluids.FluidType.DripstoneDripInfo dripInfo = fluid.getFluidType().getDripInfo();
                if (dripInfo != null && !(random >= dripInfo.chance())) {
                    BlockPos tipPos = findTip(state, level, pos, 11, false);
                    if (tipPos != null) {
                        if (optional.get().sourceState.is(Blocks.MUD) && fluid == Fluids.WATER) {
                            BlockState clayState = Blocks.CLAY.defaultBlockState();
                            level.setBlockAndUpdate(optional.get().pos, clayState);
                            Block.pushEntitiesUp(optional.get().sourceState, clayState, level, optional.get().pos);
                            level.gameEvent(GameEvent.BLOCK_CHANGE, optional.get().pos, GameEvent.Context.of(clayState));
                            level.levelEvent(1504, tipPos, 0);
                        } else {
                            BlockPos cauldronPos = findFillableCauldronBelowStalactiteTip(level, tipPos, fluid);
                            if (cauldronPos != null) {
                                level.levelEvent(1504, tipPos, 0);
                                int distance = tipPos.getY() - cauldronPos.getY();
                                int delay = 50 + distance;
                                BlockState cauldronState = level.getBlockState(cauldronPos);
                                level.scheduleTick(cauldronPos, cauldronState.getBlock(), delay);
                            }
                        }
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction lookDirection = context.getNearestLookingVerticalDirection().getOpposite();
        Direction tipDirection = calculateTipDirection(level, pos, lookDirection);
        if (tipDirection == null) {
            return null;
        } else {
            boolean placingTipOnly = !context.isSecondaryUseActive();
            DripstoneThickness thickness = calculateDripstoneThickness(level, pos, tipDirection, placingTipOnly);
            return thickness == null
                ? null
                : this.defaultBlockState()
                    .setValue(TIP_DIRECTION, tipDirection)
                    .setValue(THICKNESS, thickness)
                    .setValue(WATERLOGGED, Boolean.valueOf(level.getFluidState(pos).getType() == Fluids.WATER));
        }
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        DripstoneThickness thickness = state.getValue(THICKNESS);
        VoxelShape shape;
        if (thickness == DripstoneThickness.TIP_MERGE) {
            shape = TIP_MERGE_SHAPE;
        } else if (thickness == DripstoneThickness.TIP) {
            if (state.getValue(TIP_DIRECTION) == Direction.DOWN) {
                shape = TIP_SHAPE_DOWN;
            } else {
                shape = TIP_SHAPE_UP;
            }
        } else if (thickness == DripstoneThickness.FRUSTUM) {
            shape = FRUSTUM_SHAPE;
        } else if (thickness == DripstoneThickness.MIDDLE) {
            shape = MIDDLE_SHAPE;
        } else {
            shape = BASE_SHAPE;
        }

        Vec3 offset = state.getOffset(level, pos);
        return shape.move(offset.x, 0.0, offset.z);
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    protected float getMaxHorizontalOffset() {
        return 0.125F;
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
        if (!fallingBlock.isSilent()) {
            level.levelEvent(1045, pos, 0);
        }
    }

    @Override
    public DamageSource getFallDamageSource(Entity entity) {
        return entity.damageSources().fallingStalactite(entity);
    }

    private static void spawnFallingStalactite(BlockState state, ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos mutablePos = pos.mutable();
        BlockState currentState = state;

        while (isStalactite(currentState)) {
            FallingBlockEntity fallingBlock = FallingBlockEntity.fall(level, mutablePos, currentState);
            if (isTip(currentState, true)) {
                int height = Math.max(1 + pos.getY() - mutablePos.getY(), 6);
                float damage = 1.0F * (float) height;
                fallingBlock.setHurtsEntities(damage, 40);
                break;
            }

            mutablePos.move(Direction.DOWN);
            currentState = level.getBlockState(mutablePos);
        }
    }

    public static void growStalactiteOrStalagmiteIfPossible(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState above1 = level.getBlockState(pos.above(1));
        BlockState above2 = level.getBlockState(pos.above(2));
        if (canGrow(above1, above2)) {
            BlockPos tipPos = findTip(state, level, pos, 7, false);
            if (tipPos != null) {
                BlockState tipState = level.getBlockState(tipPos);
                if (canDrip(tipState) && canTipGrow(tipState, level, tipPos)) {
                    if (random.nextBoolean()) {
                        grow(level, tipPos, Direction.DOWN);
                    } else {
                        growStalagmiteBelow(level, tipPos);
                    }
                }
            }
        }
    }

    private static void growStalagmiteBelow(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos mutablePos = pos.mutable();

        for (int i = 0; i < 10; i++) {
            mutablePos.move(Direction.DOWN);
            BlockState state = level.getBlockState(mutablePos);
            if (!state.getFluidState().isEmpty()) {
                return;
            }

            if (isUnmergedTipWithDirection(state, Direction.UP) && canTipGrow(state, level, mutablePos)) {
                grow(level, mutablePos, Direction.UP);
                return;
            }

            if (isValidPointedMoonstonePlacement(level, mutablePos, Direction.UP) && !level.isWaterAt(mutablePos.below())) {
                grow(level, mutablePos.below(), Direction.UP);
                return;
            }

            if (!canDripThrough(level, mutablePos, state)) {
                return;
            }
        }
    }

    private static void grow(ServerLevel level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (isUnmergedTipWithDirection(neighborState, direction.getOpposite())) {
            createMergedTips(neighborState, level, neighborPos);
        } else if (neighborState.isAir() || neighborState.is(Blocks.WATER)) {
            createDripstone(level, neighborPos, direction, DripstoneThickness.TIP);
        }
    }

    private static void createDripstone(LevelAccessor level, BlockPos pos, Direction tipDirection, DripstoneThickness thickness) {
        BlockState state = ModBlocks.POINTED_MOONSTONE.get()
            .defaultBlockState()
            .setValue(TIP_DIRECTION, tipDirection)
            .setValue(THICKNESS, thickness)
            .setValue(WATERLOGGED, Boolean.valueOf(level.getFluidState(pos).getType() == Fluids.WATER));
        level.setBlock(pos, state, 3);
    }

    private static void createMergedTips(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos upperPos;
        BlockPos lowerPos;
        if (state.getValue(TIP_DIRECTION) == Direction.UP) {
            lowerPos = pos;
            upperPos = pos.above();
        } else {
            upperPos = pos;
            lowerPos = pos.below();
        }

        createDripstone(level, upperPos, Direction.DOWN, DripstoneThickness.TIP_MERGE);
        createDripstone(level, lowerPos, Direction.UP, DripstoneThickness.TIP_MERGE);
    }

    public static void spawnDripParticle(Level level, BlockPos pos, BlockState state) {
        getFluidAboveStalactite(level, pos, state).ifPresent(info -> spawnDripParticle(level, pos, state, info.fluid));
    }

    private static void spawnDripParticle(Level level, BlockPos pos, BlockState state, Fluid fluid) {
        Vec3 offset = state.getOffset(level, pos);
        double x = (double) pos.getX() + 0.5 + offset.x;
        double y = (double) ((float) (pos.getY() + 1) - STALACTITE_DRIP_START_PIXEL) - 0.0625;
        double z = (double) pos.getZ() + 0.5 + offset.z;
        Fluid dripFluid = getDripFluid(level, fluid);
        ParticleOptions particle = dripFluid.getFluidType().getDripInfo() != null ? dripFluid.getFluidType().getDripInfo().dripParticle() : ParticleTypes.DRIPPING_DRIPSTONE_WATER;
        if (particle != null) {
            level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
        }
    }

    @Nullable
    private static BlockPos findTip(BlockState state, LevelAccessor level, BlockPos pos, int maxDistance, boolean acceptMergedTip) {
        if (isTip(state, acceptMergedTip)) {
            return pos;
        } else {
            Direction direction = state.getValue(TIP_DIRECTION);
            BiPredicate<BlockPos, BlockState> continuePredicate = (checkPos, checkState) -> checkState.is(ModBlocks.POINTED_MOONSTONE.get())
                    && checkState.getValue(TIP_DIRECTION) == direction;
            return findBlockVertical(level, pos, direction.getAxisDirection(), continuePredicate, checkState -> isTip(checkState, acceptMergedTip), maxDistance)
                .orElse(null);
        }
    }

    @Nullable
    private static Direction calculateTipDirection(LevelReader level, BlockPos pos, Direction lookDirection) {
        Direction direction;
        if (isValidPointedMoonstonePlacement(level, pos, lookDirection)) {
            direction = lookDirection;
        } else {
            if (!isValidPointedMoonstonePlacement(level, pos, lookDirection.getOpposite())) {
                return null;
            }

            direction = lookDirection.getOpposite();
        }

        return direction;
    }

    @Nullable
    private static DripstoneThickness calculateDripstoneThickness(LevelReader level, BlockPos pos, Direction tipDirection, boolean placingTipOnly) {
        Direction opposite = tipDirection.getOpposite();
        BlockState neighborInTipDirection = level.getBlockState(pos.relative(tipDirection));
        if (isPointedMoonstoneWithDirection(neighborInTipDirection, opposite)) {
            return !placingTipOnly && neighborInTipDirection.getValue(THICKNESS) != DripstoneThickness.TIP_MERGE ? DripstoneThickness.TIP : DripstoneThickness.TIP_MERGE;
        } else if (!isPointedMoonstoneWithDirection(neighborInTipDirection, tipDirection)) {
            return DripstoneThickness.TIP;
        } else {
            DripstoneThickness neighborThickness = neighborInTipDirection.getValue(THICKNESS);
            if (neighborThickness != DripstoneThickness.TIP && neighborThickness != DripstoneThickness.TIP_MERGE) {
                BlockState neighborInOppositeDirection = level.getBlockState(pos.relative(opposite));
                return !isPointedMoonstoneWithDirection(neighborInOppositeDirection, tipDirection) ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE;
            } else {
                return DripstoneThickness.FRUSTUM;
            }
        }
    }

    public static boolean canDrip(BlockState state) {
        return isStalactite(state) && state.getValue(THICKNESS) == DripstoneThickness.TIP && !state.getValue(WATERLOGGED);
    }

    private static boolean canTipGrow(BlockState state, ServerLevel level, BlockPos pos) {
        Direction direction = state.getValue(TIP_DIRECTION);
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (!neighborState.getFluidState().isEmpty()) {
            return false;
        } else {
            return neighborState.isAir() ? true : isUnmergedTipWithDirection(neighborState, direction.getOpposite());
        }
    }

    private static Optional<BlockPos> findRootBlock(Level level, BlockPos pos, BlockState state, int maxDistance) {
        Direction direction = state.getValue(TIP_DIRECTION);
        BiPredicate<BlockPos, BlockState> continuePredicate = (checkPos, checkState) -> checkState.is(ModBlocks.POINTED_MOONSTONE.get())
                && checkState.getValue(TIP_DIRECTION) == direction;
        return findBlockVertical(
            level, pos, direction.getOpposite().getAxisDirection(), continuePredicate, checkState -> !checkState.is(ModBlocks.POINTED_MOONSTONE.get()), maxDistance
        );
    }

    private static boolean isValidPointedMoonstonePlacement(LevelReader level, BlockPos pos, Direction tipDirection) {
        BlockPos supportPos = pos.relative(tipDirection.getOpposite());
        BlockState supportState = level.getBlockState(supportPos);
        return supportState.isFaceSturdy(level, supportPos, tipDirection) || isPointedMoonstoneWithDirection(supportState, tipDirection);
    }

    private static boolean isTip(BlockState state, boolean acceptMergedTip) {
        if (!state.is(ModBlocks.POINTED_MOONSTONE.get())) {
            return false;
        } else {
            DripstoneThickness thickness = state.getValue(THICKNESS);
            return thickness == DripstoneThickness.TIP || acceptMergedTip && thickness == DripstoneThickness.TIP_MERGE;
        }
    }

    private static boolean isUnmergedTipWithDirection(BlockState state, Direction direction) {
        return isTip(state, false) && state.getValue(TIP_DIRECTION) == direction;
    }

    private static boolean isStalactite(BlockState state) {
        return isPointedMoonstoneWithDirection(state, Direction.DOWN);
    }

    private static boolean isStalagmite(BlockState state) {
        return isPointedMoonstoneWithDirection(state, Direction.UP);
    }

    private static boolean isStalactiteStartPos(BlockState state, LevelReader level, BlockPos pos) {
        return isStalactite(state) && !level.getBlockState(pos.above()).is(ModBlocks.POINTED_MOONSTONE.get());
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathType) {
        return false;
    }

    private static boolean isPointedMoonstoneWithDirection(BlockState state, Direction direction) {
        return state.is(ModBlocks.POINTED_MOONSTONE.get()) && state.getValue(TIP_DIRECTION) == direction;
    }

    // AbstractCauldronBlock#canReceiveStalactiteDrip is protected, and vanilla's own PointedDripstoneBlock
    // only gets away with calling it directly by living in the same package. Reflection is the lowest-risk
    // way to reach it from here - the alternative (an access transformer) would mean editing build.gradle
    // and triggering a full Minecraft re-patch just for this one call.
    private static final java.lang.reflect.Method CAN_RECEIVE_STALACTITE_DRIP = findCanReceiveStalactiteDrip();

    private static java.lang.reflect.Method findCanReceiveStalactiteDrip() {
        try {
            java.lang.reflect.Method method = AbstractCauldronBlock.class.getDeclaredMethod("canReceiveStalactiteDrip", Fluid.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static boolean canReceiveStalactiteDrip(AbstractCauldronBlock cauldron, Fluid fluid) {
        try {
            return (boolean) CAN_RECEIVE_STALACTITE_DRIP.invoke(cauldron, fluid);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private static BlockPos findFillableCauldronBelowStalactiteTip(Level level, BlockPos pos, Fluid fluid) {
        Predicate<BlockState> cauldronPredicate = checkState -> checkState.getBlock() instanceof AbstractCauldronBlock cauldron
                && canReceiveStalactiteDrip(cauldron, fluid);
        BiPredicate<BlockPos, BlockState> continuePredicate = (checkPos, checkState) -> canDripThrough(level, checkPos, checkState);
        return findBlockVertical(level, pos, Direction.DOWN.getAxisDirection(), continuePredicate, cauldronPredicate, 11).orElse(null);
    }

    @Nullable
    public static BlockPos findStalactiteTipAboveCauldron(Level level, BlockPos pos) {
        BiPredicate<BlockPos, BlockState> continuePredicate = (checkPos, checkState) -> canDripThrough(level, checkPos, checkState);
        return findBlockVertical(level, pos, Direction.UP.getAxisDirection(), continuePredicate, PointedMoonstoneBlock::canDrip, 11).orElse(null);
    }

    public static Fluid getCauldronFillFluidType(ServerLevel level, BlockPos pos) {
        return getFluidAboveStalactite(level, pos, level.getBlockState(pos))
            .map(info -> info.fluid)
            .filter(PointedMoonstoneBlock::canFillCauldron)
            .orElse(Fluids.EMPTY);
    }

    private static Optional<FluidInfo> getFluidAboveStalactite(Level level, BlockPos pos, BlockState state) {
        return !isStalactite(state) ? Optional.empty() : findRootBlock(level, pos, state, 11).map(rootPos -> {
            BlockPos abovePos = rootPos.above();
            BlockState aboveState = level.getBlockState(abovePos);
            Fluid fluid;
            if (aboveState.is(Blocks.MUD) && !level.dimensionType().ultraWarm()) {
                fluid = Fluids.WATER;
            } else {
                fluid = level.getFluidState(abovePos).getType();
            }

            return new FluidInfo(abovePos, fluid, aboveState);
        });
    }

    private static boolean canFillCauldron(Fluid fluid) {
        return fluid.getFluidType().getDripInfo() != null;
    }

    private static boolean canGrow(BlockState growthSurface, BlockState fluidAbove) {
        return growthSurface.is(ModBlocks.MOONSTONE_BLOCK.get()) && fluidAbove.is(Blocks.WATER) && fluidAbove.getFluidState().isSource();
    }

    private static Fluid getDripFluid(Level level, Fluid fluid) {
        if (fluid.isSame(Fluids.EMPTY)) {
            return level.dimensionType().ultraWarm() ? Fluids.LAVA : Fluids.WATER;
        } else {
            return fluid;
        }
    }

    private static Optional<BlockPos> findBlockVertical(
        LevelAccessor level,
        BlockPos pos,
        Direction.AxisDirection axisDirection,
        BiPredicate<BlockPos, BlockState> continuePredicate,
        Predicate<BlockState> foundPredicate,
        int maxDistance
    ) {
        Direction direction = Direction.get(axisDirection, Direction.Axis.Y);
        BlockPos.MutableBlockPos mutablePos = pos.mutable();

        for (int i = 1; i < maxDistance; i++) {
            mutablePos.move(direction);
            BlockState state = level.getBlockState(mutablePos);
            if (foundPredicate.test(state)) {
                return Optional.of(mutablePos.immutable());
            }

            if (level.isOutsideBuildHeight(mutablePos.getY()) || !continuePredicate.test(mutablePos, state)) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private static boolean canDripThrough(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        } else if (state.isSolidRender(level, pos)) {
            return false;
        } else if (!state.getFluidState().isEmpty()) {
            return false;
        } else {
            VoxelShape shape = state.getCollisionShape(level, pos);
            return !Shapes.joinIsNotEmpty(REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK, shape, BooleanOp.AND);
        }
    }

    private record FluidInfo(BlockPos pos, Fluid fluid, BlockState sourceState) {
    }
}
