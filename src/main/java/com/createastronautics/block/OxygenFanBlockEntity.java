package com.createastronautics.block;

import com.createastronautics.ModDimensions;
import com.createastronautics.fluid.ModFluids;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.NozzleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Adds a small oxygen-only fluid buffer to the otherwise-untouched {@link EncasedFanBlockEntity}, exposed on
 * exactly one face - wherever {@link OxygenFanBlock#pipeWorldDirection} places the pipe port for the
 * block's current {@code FACING} and {@link OxygenFanBlock#PIPE_ROTATION}. The tank only ever accepts
 * fluid, never gives it back out through a pipe - see the {@code drain} overrides below.
 *
 * On the Moon and in Deep Space, a running fan with oxygen in its tank also fills whatever sealed room it's
 * blowing into with breathable air - see {@link #updateOxygenatedRoom} for how that room is found, and
 * {@link OxygenRoomTracker} for how {@link com.createastronautics.PlayerEnvironmentHandler} finds out a
 * player is standing in one. It never pushes anything (see {@link OxygenAirCurrent}), and a
 * {@code create:nozzle} attached to it won't either - see {@link #getAirCurrent}.
 */
public class OxygenFanBlockEntity extends EncasedFanBlockEntity {
    public static final int TANK_CAPACITY_MB = 1000;

    // How often the sealed-room flood fill re-runs - it's the most expensive thing this block does, so it's
    // deliberately not every tick.
    private static final int ROOM_UPDATE_INTERVAL_TICKS = 20;
    // A bigger fan (higher RPM) can supply a bigger room - see OxygenRoom.maxSizeForSpeed.
    private static final float ROOM_BLOCKS_PER_RPM = 1.5F;
    private static final int MIN_ROOM_BLOCKS = 27;
    private static final int MAX_ROOM_BLOCKS = 3375;
    private static final int PARTICLES_PER_TICK = 2;

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

    @Nullable
    private OxygenRoom.Room oxygenatedRoom;
    private int roomUpdateCooldown;

    public OxygenFanBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.airCurrent = new OxygenAirCurrent(this);
    }

    private boolean isOxygen(FluidStack stack) {
        return stack.getFluid().isSame(ModFluids.OXYGEN.get());
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        BlockState state = getBlockState();
        Direction pipeSide = OxygenFanBlock.pipeWorldDirection(state.getValue(EncasedFanBlock.FACING), state.getValue(OxygenFanBlock.PIPE_ROTATION));
        return side == null || side == pipeSide ? tank : null;
    }

    /**
     * Hides the air current from Create's own {@code NozzleBlockEntity}, which reads a fan's
     * {@link com.simibubi.create.content.kinetics.fan.IAirCurrentSource#getAirCurrent} purely to decide how
     * hard to push things - returning {@code null} makes it treat this fan as having no current to push
     * with, without touching the real {@link #airCurrent} field our own tick logic uses directly.
     */
    @Nullable
    @Override
    public AirCurrent getAirCurrent() {
        return null;
    }

    @Override
    public void tick() {
        super.tick();

        if (roomUpdateCooldown-- <= 0) {
            roomUpdateCooldown = ROOM_UPDATE_INTERVAL_TICKS;
            updateOxygenatedRoom();
        }

        if (level.isClientSide && oxygenatedRoom != null) {
            spawnOxygenParticles();
        }
    }

    /**
     * Floods outward from whatever's directly in front of the fan (skipping over a nozzle, if one's
     * attached - air still flows through it) to find the sealed room it's supplying, if any. Runs
     * identically on both sides: the block entity's synced {@code FACING}/speed/tank state is all this
     * needs, so client and server always agree on the same room without any extra networking.
     */
    private void updateOxygenatedRoom() {
        if (level == null || tank.isEmpty() || getSpeed() == 0.0F
                || (level.dimension() != ModDimensions.MOON && level.dimension() != ModDimensions.DEEP_SPACE)) {
            oxygenatedRoom = null;
        } else {
            Direction facing = getBlockState().getValue(EncasedFanBlock.FACING);
            BlockPos outlet = getBlockPos().relative(facing);
            while (level.getBlockState(outlet).getBlock() instanceof NozzleBlock) {
                outlet = outlet.relative(facing);
            }

            int maxSize = OxygenRoom.maxSizeForSpeed(getSpeed(), ROOM_BLOCKS_PER_RPM, MIN_ROOM_BLOCKS, MAX_ROOM_BLOCKS);
            oxygenatedRoom = OxygenRoom.fill(level, outlet, maxSize);
        }

        if (oxygenatedRoom != null && !level.isClientSide) {
            long tick = level.getGameTime();
            for (Player player : level.players()) {
                if (oxygenatedRoom.contains(player.blockPosition())) {
                    OxygenRoomTracker.markOxygenated(player, tick);
                }
            }
        }
    }

    private void spawnOxygenParticles() {
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            BlockPos pos = oxygenatedRoom.randomPosition(level.random);
            double x = pos.getX() + level.random.nextDouble();
            double y = pos.getY() + level.random.nextDouble();
            double z = pos.getZ() + level.random.nextDouble();
            level.addParticle(ParticleTypes.CLOUD, x, y, z, 0.0, 0.01, 0.0);
        }
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
