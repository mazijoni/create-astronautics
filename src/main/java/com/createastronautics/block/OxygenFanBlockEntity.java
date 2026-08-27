package com.createastronautics.block;

import com.createastronautics.ModDimensions;
import com.createastronautics.fluid.ModFluids;
import com.createastronautics.particle.ModParticleTypes;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.NozzleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adds a small oxygen-only fluid buffer to the otherwise-untouched {@link EncasedFanBlockEntity}, exposed on
 * exactly one face - wherever {@link OxygenFanBlock#pipeWorldDirection} places the pipe port for the
 * block's current {@code FACING} and {@link OxygenFanBlock#PIPE_ROTATION}. The tank only ever accepts
 * fluid, never gives it back out through a pipe - see the {@code drain} overrides below.
 *
 * On the Moon and in Deep Space, a running fan with a {@code create:nozzle} on its outlet and oxygen in its
 * tank also fills whatever sealed room it's blowing into with breathable air, consuming its own tank while
 * it does - see {@link #updateOxygenatedRoom}, {@link #candidateOutlets}. Any side of the nozzle chain that
 * opens onto a room counts, not just the straight-through face, so a nozzle poking into a corner or alcove
 * still supplies whatever room it actually touches. Several fans blowing into the same room pool their capacity
 * (see {@link #totalCapacityServicing}), so a room too big for any one fan alone can still work with enough
 * of them. {@link OxygenFanRegistry} is how {@link com.createastronautics.PlayerEnvironmentHandler} finds
 * out any given position is inside one of these rooms, checked live rather than cached per player. The fan
 * never pushes anything itself (see {@link OxygenAirCurrent}), and neither does the nozzle - see
 * {@link #getAirCurrent}.
 */
public class OxygenFanBlockEntity extends EncasedFanBlockEntity {
    public static final int TANK_CAPACITY_MB = 1000;

    // How often the sealed-room flood fill re-runs - it's the most expensive thing this block does, kept
    // short so leaving a room (or a door slamming shut) is noticed quickly rather than a second or more
    // later.
    private static final int ROOM_UPDATE_INTERVAL_TICKS = 5;
    // Hard cap purely to bound the cost of a single flood fill - a real opening to the vacuum blows straight
    // past this almost immediately, so this only ever matters for genuinely enormous sealed builds.
    private static final int ROOM_SAFETY_CEILING = 4096;
    // How many blocks of room a single fan's own capacity covers, scaling with its current RPM.
    private static final float ROOM_BLOCKS_PER_RPM = 1.5F;
    private static final int MIN_ROOM_BLOCKS = 27;
    private static final int MAX_ROOM_BLOCKS = 3375;
    private static final int PARTICLES_PER_TICK = 2;
    private static final int SEAL_BREAK_PARTICLE_COUNT = 16;
    // Half-width of the box particles are scattered across on the two axes perpendicular to the breach
    // direction - roughly a block face - and how far off-axis their velocity can jitter, in blocks/tick.
    private static final double SEAL_BREAK_PARTICLE_SPREAD = 0.8;
    private static final double SEAL_BREAK_PARTICLE_SPEED = 0.35;
    private static final double SEAL_BREAK_PARTICLE_JITTER = 0.06;
    // How much oxygen a room burns through per second per block of its actual current size - not a fan's
    // own potential capacity - so a fan quietly maintaining a small closet costs far less than one straining
    // to keep a cathedral-sized room breathable. When several fans share one room, each pays a share of that
    // total proportional to its own capacity among them (see ownDrainShare/totalCapacityServicing).
    private static final int OXYGEN_DRAIN_INTERVAL_TICKS = 20;
    private static final float OXYGEN_DRAIN_MB_PER_ROOM_BLOCK = 0.03F;

    private static final ResourceLocation SEAL_BREAK_SOUND_ID = ResourceLocation.fromNamespaceAndPath("aeronautics", "block.steam_vent.open");
    // Aeronautics' own GustEntity (dev.eriksonn.aeronautics.content.blocks.hot_air.gust.GustEntity, see
    // Creators-of-Aeronautics/Simulated-Project) is what its steam vents and balloon envelopes spawn for
    // exactly this "burst of air escaping in a direction" effect: client-side, it fans out three
    // correctly-oriented GustParticleData cards (their orientation is a fixed Quaternionf baked in at
    // construction, not derived from velocity - a hand-rolled single card at a static orientation just sat
    // edge-on and invisible from most angles) plus a stream of directional AirPoofParticleData, and it plays
    // its own gust sound. Its static addGust(Level, BlockPos, Direction) factory takes only vanilla types, so
    // it's resolved and invoked by reflection the same way as the other Aeronautics classes below - jar-in-
    // jar means none of it is on the compile classpath, but Aeronautics is a required dependency (see
    // neoforge.mods.toml), so it's always actually present when this runs; the null fallback only matters if
    // a future Aeronautics version renames/removes it.
    @Nullable
    private static final Method GUST_ENTITY_ADD_GUST = resolveGustEntityAddGust();

    @Nullable
    private static Method resolveGustEntityAddGust() {
        try {
            Class<?> gustEntity = Class.forName("dev.eriksonn.aeronautics.content.blocks.hot_air.gust.GustEntity");
            return gustEntity.getMethod("addGust", Level.class, BlockPos.class, Direction.class);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

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
    // The last room this fan actually found sealed, kept around purely so that if it breaks, findBreach can
    // compare against it to find the one boundary face that changed - see updateOxygenatedRoom.
    @Nullable
    private OxygenRoom.Room lastSealedRoom;
    private boolean physicallySealed;
    private int roomUpdateCooldown;
    private int oxygenDrainCooldown;
    // This fan's share of the combined capacity servicing its current room (see totalCapacityServicing),
    // cached from the last room update rather than recomputed on every drain tick - the drain interval
    // doesn't line up with the room-update interval, and the room's serviced-by set doesn't change fast
    // enough to need re-deriving it more often than the room itself is re-checked.
    private int lastServicingCapacityTotal;

    public OxygenFanBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.airCurrent = new OxygenAirCurrent(this);
    }

    @Override
    public void remove() {
        super.remove();
        if (level != null) {
            OxygenFanRegistry.unregister(level, this);
        }
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

    @Nullable
    public OxygenRoom.Room getOxygenatedRoom() {
        return oxygenatedRoom;
    }

    @Override
    public void tick() {
        super.tick();

        if (level != null && !level.isClientSide) {
            // Re-registering every tick is cheap (a hash-set add that's a no-op once already present) and
            // avoids depending on exactly when/whether a load hook fires for a block entity restored from
            // disk - simpler than getting that lifecycle exactly right.
            OxygenFanRegistry.register(level, this);

            if (oxygenatedRoom != null) {
                if (oxygenDrainCooldown-- <= 0) {
                    oxygenDrainCooldown = OXYGEN_DRAIN_INTERVAL_TICKS;
                    consumeOxygen(ownDrainShare());
                }
            } else {
                oxygenDrainCooldown = 0;
            }
        }

        if (roomUpdateCooldown-- <= 0) {
            roomUpdateCooldown = ROOM_UPDATE_INTERVAL_TICKS;
            updateOxygenatedRoom();
        }

        if (level != null && level.isClientSide && oxygenatedRoom != null) {
            spawnOxygenParticles();
        }
    }

    /**
     * Every open position touching the fan's nozzle chain - a candidate for the flood fill in
     * {@link #updateOxygenatedRoom} to start from. Not just the one straight out the last nozzle's front
     * face: a nozzle poking into a room at a corner or alcove, where only one of its <em>side</em> faces
     * actually opens onto the room and the straight-through face is blocked, still ought to supply that
     * room, so every side of every nozzle segment in the chain is offered up - {@link OxygenRoom#fill}
     * itself is what actually rejects the ones that aren't real open air (occupied by the next nozzle
     * segment, the fan, a solid wall, whatever).
     *
     * <p>A fan only contributes to a room at all if it has oxygen, is spinning, is on the Moon or in Deep
     * Space, and - per {@code create:nozzle} being how the air actually gets projected into a room - has one
     * fitted directly to its outlet; none of that holds, this is empty.
     */
    private List<BlockPos> candidateOutlets() {
        if (level == null || tank.isEmpty() || getSpeed() == 0.0F) {
            return List.of();
        }
        if (level.dimension() != ModDimensions.MOON && level.dimension() != ModDimensions.DEEP_SPACE) {
            return List.of();
        }

        Direction facing = getBlockState().getValue(EncasedFanBlock.FACING);
        BlockPos nozzle = getBlockPos().relative(facing);
        if (!(level.getBlockState(nozzle).getBlock() instanceof NozzleBlock)) {
            return List.of();
        }

        List<BlockPos> candidates = new ArrayList<>();
        while (level.getBlockState(nozzle).getBlock() instanceof NozzleBlock) {
            for (Direction direction : Direction.values()) {
                candidates.add(nozzle.relative(direction));
            }
            nozzle = nozzle.relative(facing);
        }
        return candidates;
    }

    /**
     * Floods outward from this fan's nozzle outlet to find the sealed room it's supplying, if any, then
     * checks whether every fan contributing to that same room (see {@link #totalCapacityServicing}) can
     * cover its actual size. Runs identically on both sides - the block entity's synced
     * {@code FACING}/speed/tank state is all this needs, so client and server always agree on the same room
     * without any extra networking - but the seal-broken effect and player bookkeeping only ever happen
     * once, server-side, and as soon as the break is detected rather than waiting for anything.
     */
    private void updateOxygenatedRoom() {
        OxygenRoom.Room sealedRoom = null;
        for (BlockPos candidate : candidateOutlets()) {
            sealedRoom = OxygenRoom.fill(level, candidate, ROOM_SAFETY_CEILING);
            if (sealedRoom != null) {
                break;
            }
        }

        int servicingCapacity = sealedRoom != null ? totalCapacityServicing(sealedRoom) : 0;
        oxygenatedRoom = sealedRoom != null && servicingCapacity >= sealedRoom.positions().size() ? sealedRoom : null;
        lastServicingCapacityTotal = servicingCapacity;

        boolean wasSealed = physicallySealed;
        physicallySealed = sealedRoom != null;

        if (level != null && !level.isClientSide && wasSealed && !physicallySealed) {
            OxygenRoom.Breach breach = lastSealedRoom != null ? OxygenRoom.findBreach(level, lastSealedRoom) : null;
            onSealBroken(breach);
        }

        if (sealedRoom != null) {
            lastSealedRoom = sealedRoom;
        }
    }

    /**
     * Sums the room capacity of every fan (this one included) that's eligible and actually blowing into
     * {@code room} - found by checking the blocks directly next to the room's interior for other oxygen
     * fans, rather than needing any kind of shared registry between them.
     */
    private int totalCapacityServicing(OxygenRoom.Room room) {
        Set<BlockPos> checkedNeighbors = new HashSet<>();
        int total = 0;
        for (BlockPos pos : room.positions()) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (!checkedNeighbors.add(neighbor)) {
                    continue;
                }
                if (level.getBlockEntity(neighbor) instanceof OxygenFanBlockEntity fan) {
                    if (fan.candidateOutlets().stream().anyMatch(room::contains)) {
                        total += OxygenRoom.maxSizeForSpeed(fan.getSpeed(), ROOM_BLOCKS_PER_RPM, MIN_ROOM_BLOCKS, MAX_ROOM_BLOCKS);
                    }
                }
            }
        }
        return total;
    }

    /** @param breach where and which way the room actually broke open (see {@link OxygenRoom#findBreach}), or {@code null} to fall back to this fan's own position/facing. */
    private void onSealBroken(@Nullable OxygenRoom.Breach breach) {
        // Several fans in one shared room all notice the same break on the same update cycle - only the
        // first to get here actually announces it, so it doesn't turn into a wall of simultaneous sounds.
        if (!OxygenFanRegistry.tryAnnounceSealBreak(level, level.getGameTime())) {
            return;
        }

        BlockPos pos = breach != null ? breach.pos() : getBlockPos();
        Direction direction = breach != null ? breach.direction() : getBlockState().getValue(EncasedFanBlock.FACING);

        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(SEAL_BREAK_SOUND_ID);
        level.playSound(null, pos, sound != null ? sound : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (level instanceof ServerLevel serverLevel) {
            spawnSealBreakGust(serverLevel, pos, direction);
        }
    }

    /**
     * Delegates the actual burst to Aeronautics' own {@code GustEntity.addGust} - see the field javadoc
     * above for why a hand-rolled version of this kept looking wrong (static orientation, ignored velocity).
     * Falls back to a plain directional puff of vanilla particles only if that method can't be found.
     */
    private void spawnSealBreakGust(ServerLevel serverLevel, BlockPos pos, Direction direction) {
        if (GUST_ENTITY_ADD_GUST != null) {
            try {
                GUST_ENTITY_ADD_GUST.invoke(null, serverLevel, pos, direction);
                return;
            } catch (ReflectiveOperationException ignored) {
                // Fall through to the vanilla fallback below.
            }
        }

        double stepX = direction.getStepX();
        double stepY = direction.getStepY();
        double stepZ = direction.getStepZ();

        double originX = pos.getX() + 0.5 + stepX * 0.55;
        double originY = pos.getY() + 0.5 + stepY * 0.55;
        double originZ = pos.getZ() + 0.5 + stepZ * 0.55;

        // Zeroing spread/jitter along the breach's own axis (the 1 - |step| factor) keeps particles flat
        // against the opening and moving straight out through it, instead of also smearing them forward and
        // back through the wall/floor on either side.
        double spreadX = SEAL_BREAK_PARTICLE_SPREAD * (1.0 - Math.abs(stepX));
        double spreadY = SEAL_BREAK_PARTICLE_SPREAD * (1.0 - Math.abs(stepY));
        double spreadZ = SEAL_BREAK_PARTICLE_SPREAD * (1.0 - Math.abs(stepZ));

        for (int i = 0; i < SEAL_BREAK_PARTICLE_COUNT; i++) {
            double x = originX + (serverLevel.random.nextDouble() - 0.5) * spreadX;
            double y = originY + (serverLevel.random.nextDouble() - 0.5) * spreadY;
            double z = originZ + (serverLevel.random.nextDouble() - 0.5) * spreadZ;

            double outward = SEAL_BREAK_PARTICLE_SPEED * (0.6 + serverLevel.random.nextDouble() * 0.8);
            double vx = stepX * outward + (serverLevel.random.nextDouble() - 0.5) * SEAL_BREAK_PARTICLE_JITTER * (1.0 - Math.abs(stepX));
            double vy = stepY * outward + (serverLevel.random.nextDouble() - 0.5) * SEAL_BREAK_PARTICLE_JITTER * (1.0 - Math.abs(stepY));
            double vz = stepZ * outward + (serverLevel.random.nextDouble() - 0.5) * SEAL_BREAK_PARTICLE_JITTER * (1.0 - Math.abs(stepZ));

            serverLevel.sendParticles(ParticleTypes.POOF, x, y, z, 0, vx, vy, vz, 1.0);
        }
    }

    /** @return this fan's share of its currently-oxygenated room's per-interval oxygen cost, proportional to its own capacity among every fan servicing that room. */
    private int ownDrainShare() {
        int roomSize = oxygenatedRoom.positions().size();
        int ownCapacity = OxygenRoom.maxSizeForSpeed(getSpeed(), ROOM_BLOCKS_PER_RPM, MIN_ROOM_BLOCKS, MAX_ROOM_BLOCKS);
        double share = lastServicingCapacityTotal > 0 ? (double) ownCapacity / lastServicingCapacityTotal : 1.0;
        return (int) Math.max(1, Math.round(roomSize * OXYGEN_DRAIN_MB_PER_ROOM_BLOCK * share));
    }

    private void consumeOxygen(int amountMb) {
        FluidStack current = tank.getFluid();
        if (current.isEmpty()) {
            return;
        }
        FluidStack shrunk = current.copy();
        shrunk.shrink(amountMb);
        tank.setFluid(shrunk);
    }

    private void spawnOxygenParticles() {
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            BlockPos pos = oxygenatedRoom.randomPosition(level.random);
            double x = pos.getX() + level.random.nextDouble();
            double y = pos.getY() + level.random.nextDouble();
            double z = pos.getZ() + level.random.nextDouble();
            level.addParticle(ModParticleTypes.OXYGEN_MIST.get(), x, y, z, 0.0, 0.01, 0.0);
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
