package com.createastronautics.block;

import com.createastronautics.fluid.ModFluids;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Adds a small oxygen-only fluid buffer to the otherwise-untouched {@link EncasedFanBlockEntity}, exposed on
 * exactly one face - whichever one the "pipe_side" texture on the block model ends up facing, see
 * {@link #pipeSide}. Wrenching the block to a different orientation moves both the textured face and the
 * capability together, since both are derived the same way from {@code FACING}. The tank only ever accepts
 * fluid, never gives it back out through a pipe - see the {@code drain} overrides below.
 */
public class OxygenFanBlockEntity extends EncasedFanBlockEntity {
    public static final int TANK_CAPACITY_MB = 1000;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY_MB, this::isOxygen) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    public OxygenFanBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private boolean isOxygen(FluidStack stack) {
        return stack.getFluid().isSame(ModFluids.OXYGEN.get());
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return side == null || side == pipeSide() ? tank : null;
    }

    /**
     * The world-space direction the model's "pipe_side" textured face currently points, derived from the
     * exact rotation the blockstate applies to the raw model (local north) for each {@code FACING} value -
     * i.e. this must stay in lockstep with {@code blockstates/oxygen_fan.json}'s per-facing x/y rotations.
     */
    private Direction pipeSide() {
        Direction facing = getBlockState().getValue(EncasedFanBlock.FACING);
        return switch (facing) {
            case UP -> Direction.NORTH;
            case DOWN -> Direction.SOUTH;
            case NORTH, SOUTH -> Direction.DOWN;
            case EAST -> Direction.WEST;
            case WEST -> Direction.EAST;
        };
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("OxygenTank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("OxygenTank")) {
            tank.readFromNBT(registries, tag.getCompound("OxygenTank"));
        }
    }
}
