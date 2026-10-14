package com.createastronautics.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * A repeater-shaped redstone component that turns whatever signal reaches its back into one fixed strength
 * out of its front - the strength is picked from 0 to 15 with the value slider on top of the block (see
 * {@link RedstoneRegulatorBlockEntity}). Everything about being a diode - placement, the one-tick delay,
 * powering the block in front, popping off when its support goes - is vanilla's own {@link DiodeBlock}.
 *
 * The chosen strength is mirrored into {@link #SIGNAL} so that it's part of the block state: changing it
 * then updates the redstone in front like any other state change would, and the renderer that colours the
 * block by it (RedstoneRegulatorRenderer) needs nothing synced beyond the state itself.
 *
 * Looks exactly like Create's Pulse Repeater: the models and textures under {@code redstone_regulator/} are
 * unmodified copies of Create's (MIT-licensed) pulse repeater assets, kept in this mod so they can be
 * changed freely later.
 */
public class RedstoneRegulatorBlock extends DiodeBlock implements IBE<RedstoneRegulatorBlockEntity>, IWrenchable {
    public static final MapCodec<RedstoneRegulatorBlock> CODEC = simpleCodec(RedstoneRegulatorBlock::new);

    public static final int MAX_SIGNAL = 15;
    public static final IntegerProperty SIGNAL = IntegerProperty.create("signal", 0, MAX_SIGNAL);

    public RedstoneRegulatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false).setValue(SIGNAL, MAX_SIGNAL));
    }

    @Override
    protected MapCodec<? extends DiodeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, SIGNAL);
    }

    @Override
    protected int getDelay(BlockState state) {
        return 2;
    }

    @Override
    protected int getOutputSignal(BlockGetter level, BlockPos pos, BlockState state) {
        return state.getValue(SIGNAL);
    }

    /** @return the strength actually coming out of the front right now - 0 unless something is powering the back. */
    public static int currentOutput(BlockState state) {
        return state.getValue(POWERED) ? state.getValue(SIGNAL) : 0;
    }


    /** Called by the block entity when the slider is moved. */
    public static void setSignal(Level level, BlockPos pos, int signal) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof RedstoneRegulatorBlock && state.getValue(SIGNAL) != signal) {
            // A plain state change is enough to re-power the block in front - DiodeBlock#onPlace does it.
            level.setBlock(pos, state.setValue(SIGNAL, signal), Block.UPDATE_ALL);
        }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction side) {
        return side != null && side.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public Class<RedstoneRegulatorBlockEntity> getBlockEntityClass() {
        return RedstoneRegulatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RedstoneRegulatorBlockEntity> getBlockEntityType() {
        return ModBlockEntities.REDSTONE_REGULATOR.get();
    }
}
