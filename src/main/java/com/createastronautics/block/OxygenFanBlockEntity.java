package com.createastronautics.block;

import com.createastronautics.ModDimensions;
import com.createastronautics.fluid.ModFluids;
import com.createastronautics.particle.ModParticleTypes;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.NozzleBlock;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

/**
 * Adds a small oxygen-only fluid buffer to the otherwise-untouched {@link EncasedFanBlockEntity}, exposed on
 * exactly one face - wherever {@link OxygenFanBlock#pipeWorldDirection} places the pipe port for the
 * block's current {@code FACING} and {@link OxygenFanBlock#PIPE_ROTATION}. The tank only ever accepts
 * fluid, never gives it back out through a pipe - see the {@code drain} overrides below.
 *
 * On the Moon and in Deep Space, a running fan with a {@code create:nozzle} on its outlet keeps whatever
 * closed space that nozzle sits in breathable, as long as it has oxygen to do it with. Which blocks that
 * space covers is worked out by its {@link OxygenSealer}, the same way Northstar-Redux's Oxygen Sealer does
 * it; the oxygen bookkeeping in {@link #tick} follows Northstar's {@code OxygenSealerBlockEntity} too - a
 * small constant cost per sealed block, plus a cost per creature actually breathing the air (see
 * {@link #drainOxygen}). Several fans blowing into the same space pool their capacity and split that cost
 * (see {@link #contributedCapacity}, {@link #ownShare}).
 *
 * The sealer runs on both sides from state that's synced anyway (facing, speed, the blocks around it), so
 * the client can draw mist, leak particles and the goggle readout without any room data being sent to it.
 * Whether the room is actually being supplied ({@link #active}) is decided on the server alone and synced.
 * {@link OxygenFanRegistry} is how everything else asks whether a position is breathable. The fan never
 * pushes anything itself (see {@link OxygenAirCurrent}), and neither does the nozzle - see
 * {@link #getAirCurrent}.
 */
public class OxygenFanBlockEntity extends EncasedFanBlockEntity {
    public static final int TANK_CAPACITY_MB = 1000;

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
    // Oxygen cost per tick: a little for every sealed block just to keep the room pressurised (so a closet
    // costs far less than a cathedral), plus more for every creature breathing in it. A 100-block room
    // with one person in it gets through a full tank in about 80 seconds.
    private static final float PASSIVE_DRAIN_MB_PER_BLOCK = 0.0015F;
    public static final float BREATHING_DRAIN_MB = 0.05F;
    // Tank level and usage only matter to the client for the goggle readout, so they're synced at most this
    // often rather than every time a millibucket is used.
    private static final int SYNC_INTERVAL_TICKS = 10;

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
            syncPending = true;
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

    private final OxygenSealer sealer = new OxygenSealer() {
        @Override
        protected int extensionCapacity(Level level, BlockPos pos) {
            return contributedCapacity(level, pos);
        }
    };
    private final BlockPos.MutableBlockPos particlePos = new BlockPos.MutableBlockPos();

    // Whether the sealed room is actually being kept breathable right now - decided server-side, synced.
    private boolean active;
    // This fan's current oxygen use in mB/tick - server-side, synced for the goggle readout.
    private float drain;
    private float pendingDrain;
    private float breathingDrain;
    private boolean syncPending;
    private int syncCooldown;

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

    /** @return whether {@code pos} is inside the room this fan is currently keeping breathable. */
    public boolean isSupplying(BlockPos pos) {
        return active && sealer.contains(pos);
    }

    /** Called for every creature breathing this fan's air, once per tick each - see {@link #BREATHING_DRAIN_MB}. */
    public void drainOxygen(float amountMb) {
        breathingDrain += amountMb;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        boolean server = !level.isClientSide;
        if (server) {
            // Re-registering every tick is cheap (a hash-set add that's a no-op once already present) and
            // avoids depending on exactly when/whether a load hook fires for a block entity restored from
            // disk - simpler than getting that lifecycle exactly right.
            OxygenFanRegistry.register(level, this);
            syncIfPending();
        }

        BlockPos origin = sealOrigin();
        if (origin == null) {
            sealer.reset();
            setActive(false);
            return;
        }

        boolean wasSealed = sealer.isSealed();
        boolean completed = sealer.processSeal(level, origin, ownCapacity());
        if (server && completed && wasSealed && sealer.hasLeak()) {
            onSealBroken();
        }

        if (!sealer.isSealed()) {
            // Only worth pointing out where the air is getting out if there's any air to lose - and a room
            // that's merely too big has no hole to point at.
            if (sealer.hasLeak() && !tank.isEmpty()) {
                sealer.renderLeakPath(level);
            }
            setActive(false);
            return;
        }

        if (server) {
            consumeOxygen();
        } else if (active) {
            spawnOxygenParticles();
        }
    }

    /**
     * Where the sealer's flood starts: the nozzle on the fan's outlet. The air is taken to come out of
     * every open side of the nozzle, not just straight ahead, so one poking into a corner or alcove still
     * supplies whatever room it actually touches.
     *
     * @return {@code null} if this fan can't be pressurising anything at all right now - it isn't spinning,
     * has no nozzle fitted, or is somewhere the air is already breathable.
     */
    @Nullable
    private BlockPos sealOrigin() {
        if (getSpeed() == 0.0F || !isInVacuum()) {
            return null;
        }

        BlockPos nozzle = getBlockPos().relative(getBlockState().getValue(EncasedFanBlock.FACING));
        return hasNozzle() ? nozzle : null;
    }

    private boolean isInVacuum() {
        return level.dimension() == ModDimensions.MOON || level.dimension() == ModDimensions.DEEP_SPACE;
    }

    private boolean hasNozzle() {
        BlockPos nozzle = getBlockPos().relative(getBlockState().getValue(EncasedFanBlock.FACING));
        return level.getBlockState(nozzle).getBlock() instanceof NozzleBlock;
    }

    private int ownCapacity() {
        return Mth.clamp(Math.round(Math.abs(getSpeed()) * ROOM_BLOCKS_PER_RPM), MIN_ROOM_BLOCKS, MAX_ROOM_BLOCKS);
    }

    /**
     * The capacity another fan adds to this one's, if {@code pos} - somewhere this fan's flood just reached
     * - is the nozzle of a different fan that's running and has oxygen of its own.
     */
    private int contributedCapacity(Level level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof NozzleBlock)) {
            return 0;
        }

        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof OxygenFanBlockEntity fan && fan != this
                    && fan.getBlockState().getValue(EncasedFanBlock.FACING) == direction.getOpposite()
                    && fan.getSpeed() != 0.0F && !fan.tank.isEmpty()) {
                return fan.ownCapacity();
            }
        }
        return 0;
    }

    /** @return the fraction of the room's oxygen cost this fan pays - its own capacity out of everything servicing the room. */
    private float ownShare() {
        int own = ownCapacity();
        return (float) own / (own + sealer.getExtraVolume());
    }

    /**
     * Costs accumulate as a fraction and are taken from the tank in whole millibuckets; the room only
     * counts as supplied while the tank is keeping up with what's owed.
     */
    private void consumeOxygen() {
        if (tank.isEmpty()) {
            setActive(false);
            return;
        }

        int drainable = Math.min((int) pendingDrain, tank.getFluidAmount());
        if (drainable > 0) {
            FluidStack remaining = tank.getFluid().copy();
            remaining.shrink(drainable);
            tank.setFluid(remaining);
            pendingDrain -= drainable;
            setChanged();
            syncPending = true;
        }

        setActive(pendingDrain < 1.0F);
        if (!active) {
            return;
        }

        float newDrain = (sealer.getSealedBlockCount() * PASSIVE_DRAIN_MB_PER_BLOCK + breathingDrain) * ownShare();
        if (Math.abs(newDrain - drain) > 0.0005F) {
            drain = newDrain;
            syncPending = true;
        }
        pendingDrain += newDrain;
        breathingDrain = 0.0F;
    }

    private void setActive(boolean nowActive) {
        if (active == nowActive || level == null || level.isClientSide) {
            return;
        }
        active = nowActive;
        if (!nowActive) {
            drain = 0.0F;
        }
        setChanged();
        sendData();
    }

    private void syncIfPending() {
        if (syncCooldown > 0) {
            syncCooldown--;
        } else if (syncPending) {
            syncPending = false;
            syncCooldown = SYNC_INTERVAL_TICKS;
            sendData();
        }
    }

    private void onSealBroken() {
        // Several fans in one shared room all notice the same break on the same update cycle - only the
        // first to get here actually announces it, so it doesn't turn into a wall of simultaneous sounds.
        if (!OxygenFanRegistry.tryAnnounceSealBreak(level, level.getGameTime())) {
            return;
        }

        BlockPos breachPos = sealer.getBreachPos();
        Direction breachDirection = sealer.getBreachDirection();
        BlockPos pos = breachPos != null ? breachPos : getBlockPos();
        Direction direction = breachDirection != null ? breachDirection : getBlockState().getValue(EncasedFanBlock.FACING);

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

    private void spawnOxygenParticles() {
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            sealer.randomSealedPosition(level.random, particlePos);
            double x = particlePos.getX() + level.random.nextDouble();
            double y = particlePos.getY() + level.random.nextDouble();
            double z = particlePos.getZ() + level.random.nextDouble();
            level.addParticle(ModParticleTypes.OXYGEN_MIST.get(), x, y, z, 0.0, 0.01, 0.0);
        }
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("OxygenTank", tank.writeToNBT(registries, new CompoundTag()));
        if (clientPacket) {
            tag.putBoolean("OxygenActive", active);
            tag.putFloat("OxygenDrain", drain);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("OxygenTank")) {
            tank.readFromNBT(registries, tag.getCompound("OxygenTank"));
        }
        if (clientPacket) {
            active = tag.getBoolean("OxygenActive");
            drain = tag.getFloat("OxygenDrain");
        }
    }

    // --- Engineer's goggles overlay ---

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        CreateLang.builder()
                .add(Component.translatable("gui.goggles.oxygen_fan.title"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        CreateLang.builder()
                .add(Component.translatable("gui.goggles.oxygen_fan.status"))
                .text(": ")
                .add(goggleStatus())
                .forGoggles(tooltip);

        boolean canSeal = getSpeed() != 0.0F && isInVacuum() && hasNozzle();
        int ownCapacity = ownCapacity();
        int totalCapacity = ownCapacity + sealer.getExtraVolume();

        if (canSeal) {
            // A closed room has a size worth showing against the capacity whether or not it fits; with a
            // leak there's no room to measure, so only the capacity is shown.
            boolean enclosed = sealer.isSealed() || sealer.isTooBig();
            CreateLang.builder()
                    .add(Component.translatable(sealer.isSealed() ? "gui.goggles.oxygen_fan.room_size"
                            : sealer.isTooBig() ? "gui.goggles.oxygen_fan.room_too_big_size"
                            : "gui.goggles.oxygen_fan.max_room_size"))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            MutableComponent size = enclosed
                    ? Component.literal(String.valueOf(sealer.getEnclosedBlockCount())).withStyle(sealer.isTooBig() ? ChatFormatting.RED : ChatFormatting.AQUA)
                            .append(Component.literal(" / " + totalCapacity).withStyle(ChatFormatting.DARK_GRAY))
                    : Component.literal(String.valueOf(totalCapacity)).withStyle(ChatFormatting.AQUA);
            CreateLang.builder()
                    .add(size)
                    .add(Component.translatable("gui.goggles.oxygen_fan.blocks").withStyle(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        }

        if (active) {
            CreateLang.builder()
                    .add(Component.translatable("gui.goggles.oxygen_fan.usage"))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            CreateLang.builder()
                    .add(Component.literal(String.format(Locale.ROOT, "%.2f", drain * 20.0F)).withStyle(ChatFormatting.GOLD))
                    .add(Component.translatable("gui.goggles.oxygen_fan.mb_per_second").withStyle(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
            if (drain > 0.0F) {
                CreateLang.builder()
                        .add(Component.literal(formatDuration(Math.round(tank.getFluidAmount() / drain))).withStyle(ChatFormatting.GOLD))
                        .add(Component.translatable("gui.goggles.oxygen_fan.remaining").withStyle(ChatFormatting.GRAY))
                        .forGoggles(tooltip, 1);
            }
        }

        CreateLang.builder()
                .add(Component.translatable("gui.goggles.oxygen_fan.tank"))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
        CreateLang.builder()
                .add(Component.literal(String.valueOf(tank.getFluidAmount())).withStyle(tank.isEmpty() ? ChatFormatting.RED : ChatFormatting.AQUA))
                .add(Component.literal(" / " + TANK_CAPACITY_MB + " mB").withStyle(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);

        if (isPlayerSneaking && canSeal) {
            CreateLang.builder()
                    .add(Component.literal(String.valueOf(ownCapacity)).withStyle(ChatFormatting.AQUA))
                    .add(Component.translatable("gui.goggles.oxygen_fan.capacity_speed").withStyle(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
            if (sealer.getExtraVolume() > 0) {
                CreateLang.builder()
                        .add(Component.literal(String.valueOf(sealer.getExtraVolume())).withStyle(ChatFormatting.AQUA))
                        .add(Component.translatable("gui.goggles.oxygen_fan.capacity_extra").withStyle(ChatFormatting.GRAY))
                        .forGoggles(tooltip, 1);
            }
            if (sealer.isSealInProgress()) {
                CreateLang.builder()
                        .add(Component.translatable("gui.goggles.oxygen_fan.checking").withStyle(ChatFormatting.GRAY))
                        .add(Component.literal(Math.round(sealer.getProgress(ownCapacity) * 100.0F) + "%").withStyle(ChatFormatting.AQUA))
                        .forGoggles(tooltip);
            }
        }

        return true;
    }

    private Component goggleStatus() {
        if (!isInVacuum()) {
            return Component.translatable("gui.goggles.oxygen_fan.status.breathable").withStyle(ChatFormatting.GRAY);
        }
        if (getSpeed() == 0.0F) {
            return Component.translatable("gui.goggles.oxygen_fan.status.not_spinning").withStyle(ChatFormatting.GOLD);
        }
        if (!hasNozzle()) {
            return Component.translatable("gui.goggles.oxygen_fan.status.no_nozzle").withStyle(ChatFormatting.GOLD);
        }
        if (sealer.hasLeak()) {
            return Component.translatable("gui.goggles.oxygen_fan.status.leak").withStyle(ChatFormatting.RED);
        }
        if (sealer.isTooBig()) {
            return Component.translatable("gui.goggles.oxygen_fan.status.too_big").withStyle(ChatFormatting.RED);
        }
        if (!sealer.isSealed()) {
            return Component.translatable("gui.goggles.oxygen_fan.status.checking").withStyle(ChatFormatting.GRAY);
        }
        if (!active) {
            return Component.translatable("gui.goggles.oxygen_fan.status.no_oxygen").withStyle(ChatFormatting.RED);
        }
        return Component.translatable("gui.goggles.oxygen_fan.status.working").withStyle(ChatFormatting.GREEN);
    }

    private static String formatDuration(int ticks) {
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
    }
}
