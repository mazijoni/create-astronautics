package com.createastronautics.block;

import com.createastronautics.config.Config;
import com.createastronautics.particle.ModParticleTypes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Burns the solid fuel stacked on its intake side and, while lit, both stays "powered" for redstone
 * purposes and pushes on any Sable ship it's mounted to - see {@link #getScaledThrust()} for how that push
 * is computed.
 *
 * Fuel isn't put <em>into</em> the booster: it's built onto it, as blocks from the
 * {@link ModBlockTags#SOLID_ROCKET_FUEL} tag (blocks of coal) anywhere in the 3x3 column reaching
 * {@link #FUEL_STACK_HEIGHT} blocks out from the intake face. The booster eats that stack one block at a
 * time from the far end back towards itself (see {@link #findNextFuelBlock}), each block burning for
 * {@link Config#SOLID_ROCKET_BOOSTER_FUEL_BLOCK_BURN_TICKS}. Boosters sitting next to each other reach into
 * overlapping columns and simply share whatever is there.
 *
 * The exhaust is also hot: whatever solid block the nozzle is pointed at from close range is cooked into
 * magma (see {@link #scanExhaust}, {@link #heatTarget}), unless the exhaust has somewhere to go - blocks in
 * {@link ModBlockTags#EXHAUST_PASSES_THROUGH} (bars, grates) let it straight through, which is what a
 * launch pad should be made of.
 */
public class SolidRocketBoosterBlockEntity extends SmartBlockEntity
        implements IHaveGoggleInformation, BlockEntitySubLevelPropellerActor, BlockEntityPropeller {
    public static final int FUEL_STACK_HEIGHT = 4;
    private static final int FUEL_STACK_RADIUS = 1; // 3x3

    private static final int EXHAUST_SCAN_INTERVAL = 10;
    private static final int EXHAUST_SCAN_LENGTH = 5; // blocks of clearance in front of the nozzle for 100% efficiency
    // How long a block has to sit in the exhaust before it turns to magma, per block of distance from the
    // nozzle - one second right up against it, five at the far end of the plume.
    private static final int HEAT_TICKS_PER_BLOCK_OF_DISTANCE = 20;
    private static final int FUEL_COUNT_INTERVAL = 20;
    private static final int BURN_SYNC_INTERVAL = 20; // keeps the goggle countdown live instead of only updating on fuel changes

    private int burnTicksRemaining;
    private int burnTicksTotal;
    // 1 = fully clear nozzle, 0 = completely blocked. Recomputed periodically in tick(), see scanExhaust().
    private float efficiency = 1.0f;
    // How many fuel blocks are stacked within reach - counted server-side, synced for the goggle readout.
    private int fuelBlocks;

    // The block currently being cooked by the exhaust, how far from the nozzle it is, and for how long.
    @Nullable
    private BlockPos heatTarget;
    private int heatTargetDistance;
    private int heatTicks;

    public SolidRocketBoosterBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.SOLID_ROCKET_BOOSTER.get(), pos, state);
    }

    public SolidRocketBoosterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        boolean wasBurning = isBurning();
        boolean powered = getBlockState().getValue(SolidRocketBoosterBlock.POWERED);

        if (powered) {
            if (burnTicksRemaining <= 0) {
                startBurningNextFuel();
            }
            if (burnTicksRemaining > 0) {
                burnTicksRemaining--;
            }
        }
        // Losing redstone signal mid-burn pauses the countdown rather than resetting it (so the fuel isn't
        // wasted and burning picks back up where it left off) - but exhaust must still stop immediately,
        // so everything below requires POWERED explicitly rather than relying on isBurning() alone, which
        // stays true the whole time the countdown sits paused.
        boolean firing = powered && isBurning();

        if (level.getGameTime() % EXHAUST_SCAN_INTERVAL == 0) {
            scanExhaust(firing);
        }
        if (level.getGameTime() % FUEL_COUNT_INTERVAL == 0) {
            updateFuelCount();
        }

        // A fully blocked nozzle has nowhere for the particle to go but into whatever's obstructing it, so
        // it's skipped entirely rather than spawning particles that immediately embed in a block.
        if (firing && efficiency > 0f) {
            spawnExhaustEffects();
        }

        // Only push a sync packet on a state change or on a fixed interval while burning, instead of every
        // tick, so the goggle countdown stays live without spamming updates.
        if (isBurning() != wasBurning || (firing && level.getGameTime() % BURN_SYNC_INTERVAL == 0)) {
            setChanged();
            sendData();
        }
    }

    // --- Fuel ---

    private void startBurningNextFuel() {
        BlockPos fuelPos = findNextFuelBlock();
        if (fuelPos == null) {
            return;
        }

        BlockState fuel = level.getBlockState(fuelPos);
        level.removeBlock(fuelPos, false);
        // The same puff of block particles and break sound as mining it, minus the drop.
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, fuelPos, Block.getId(fuel));

        burnTicksTotal = Config.SOLID_ROCKET_BOOSTER_FUEL_BLOCK_BURN_TICKS.get();
        burnTicksRemaining = burnTicksTotal;
        updateFuelCount();
    }

    /**
     * The fuel stack is used up from the far end inwards - the layer furthest from the intake first, so a
     * stack built upwards on an upright rocket burns down from the top.
     */
    @Nullable
    private BlockPos findNextFuelBlock() {
        Direction intake = getBlockState().getValue(SolidRocketBoosterBlock.FACING).getOpposite();
        for (int layer = FUEL_STACK_HEIGHT; layer >= 1; layer--) {
            for (BlockPos pos : fuelLayer(intake, layer)) {
                if (level.getBlockState(pos).is(ModBlockTags.SOLID_ROCKET_FUEL)) {
                    return pos.immutable();
                }
            }
        }
        return null;
    }

    private void updateFuelCount() {
        Direction intake = getBlockState().getValue(SolidRocketBoosterBlock.FACING).getOpposite();
        int count = 0;
        for (int layer = 1; layer <= FUEL_STACK_HEIGHT; layer++) {
            for (BlockPos pos : fuelLayer(intake, layer)) {
                if (level.getBlockState(pos).is(ModBlockTags.SOLID_ROCKET_FUEL)) {
                    count++;
                }
            }
        }

        if (count != fuelBlocks) {
            fuelBlocks = count;
            setChanged();
            sendData();
        }
    }

    /** The 3x3 slice of the fuel column {@code layer} blocks out from the intake face. */
    private Iterable<BlockPos> fuelLayer(Direction intake, int layer) {
        BlockPos centre = getBlockPos().relative(intake, layer);
        return switch (intake.getAxis()) {
            case X -> BlockPos.betweenClosed(centre.offset(0, -FUEL_STACK_RADIUS, -FUEL_STACK_RADIUS), centre.offset(0, FUEL_STACK_RADIUS, FUEL_STACK_RADIUS));
            case Y -> BlockPos.betweenClosed(centre.offset(-FUEL_STACK_RADIUS, 0, -FUEL_STACK_RADIUS), centre.offset(FUEL_STACK_RADIUS, 0, FUEL_STACK_RADIUS));
            case Z -> BlockPos.betweenClosed(centre.offset(-FUEL_STACK_RADIUS, -FUEL_STACK_RADIUS, 0), centre.offset(FUEL_STACK_RADIUS, FUEL_STACK_RADIUS, 0));
        };
    }

    private boolean isBurning() {
        return burnTicksRemaining > 0;
    }

    // --- Exhaust: obstruction and heat ---

    /**
     * Follows the exhaust out from the nozzle to the first thing it hits. How far it got before that sets
     * {@link #efficiency} (a plume can't skip past a wall to the clear air beyond it), and the thing it hit
     * is what gets heated.
     *
     * The exhaust is followed twice: once through the booster's own grid - the rest of the ship it's built
     * into, or just the world if it isn't on one - and, on a ship, once more through the world the ship is
     * flying over, since a ship's blocks and the ground under it aren't in the same place as far as block
     * positions go. The ship's own blocks are hit first, so the world only matters if those are clear.
     */
    private void scanExhaust(boolean firing) {
        Direction nozzle = getBlockState().getValue(SolidRocketBoosterBlock.FACING);
        Vec3 centre = Vec3.atCenterOf(getBlockPos());
        Vec3 step = Vec3.atLowerCornerOf(nozzle.getNormal());

        BlockPos hit = null;
        int clear = EXHAUST_SCAN_LENGTH;
        for (int i = 1; i <= EXHAUST_SCAN_LENGTH; i++) {
            BlockPos pos = getBlockPos().relative(nozzle, i);
            if (blocksExhaust(pos)) {
                hit = pos;
                clear = i - 1;
                break;
            }
        }

        if (hit == null) {
            Vec3 worldCentre = Sable.HELPER.projectOutOfSubLevel(level, centre);
            if (!BlockPos.containing(worldCentre).equals(getBlockPos())) {
                Vec3 worldStep = Sable.HELPER.projectOutOfSubLevel(level, centre.add(step)).subtract(worldCentre);
                for (int i = 1; i <= EXHAUST_SCAN_LENGTH; i++) {
                    BlockPos pos = BlockPos.containing(worldCentre.add(worldStep.scale(i)));
                    if (blocksExhaust(pos)) {
                        hit = pos;
                        clear = i - 1;
                        break;
                    }
                }
            }
        }

        float newEfficiency = (float) clear / EXHAUST_SCAN_LENGTH;
        if (Math.abs(newEfficiency - efficiency) > 0.001f) {
            efficiency = newEfficiency;
            setChanged();
            sendData();
        }

        heat(firing ? hit : null, clear + 1);
    }

    private boolean blocksExhaust(BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !state.is(ModBlockTags.EXHAUST_PASSES_THROUGH) && !state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * Keeps cooking {@code target} (or starts over, if it's a different block than last time), and turns it
     * to magma once it's been in the exhaust long enough for how far away it is.
     */
    private void heat(@Nullable BlockPos target, int distance) {
        if (target == null || !canTurnToMagma(target)) {
            heatTarget = null;
            heatTicks = 0;
            return;
        }

        if (!target.equals(heatTarget)) {
            heatTarget = target;
            heatTargetDistance = distance;
            heatTicks = 0;
            return;
        }

        heatTicks += EXHAUST_SCAN_INTERVAL;
        if (heatTicks < HEAT_TICKS_PER_BLOCK_OF_DISTANCE * heatTargetDistance) {
            return;
        }

        level.setBlockAndUpdate(target, Blocks.MAGMA_BLOCK.defaultBlockState());
        level.playSound(null, target, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 0.8f + level.random.nextFloat() * 0.3f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LAVA, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                    8, 0.4, 0.4, 0.4, 0.0);
        }
        heatTarget = null;
        heatTicks = 0;
    }

    private boolean canTurnToMagma(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.is(Blocks.MAGMA_BLOCK)
                && !state.is(ModBlockTags.EXHAUST_PROOF)
                && !state.hasBlockEntity()
                && state.getDestroySpeed(level, pos) >= 0.0f; // bedrock and the like
    }

    /**
     * The plume-then-smoke {@link ModParticleTypes#ROCKET_PLUME} sprite (see {@link com.createastronautics
     * .client.particle.RocketPlumeParticle}) streaming from the nozzle, plus a periodic burn crackle.
     */
    private void spawnExhaustEffects() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Direction nozzle = getBlockState().getValue(SolidRocketBoosterBlock.FACING);
        Vec3 normal = Vec3.atLowerCornerOf(nozzle.getNormal());
        Vec3 exhaustPos = Vec3.atCenterOf(getBlockPos()).add(normal.scale(0.6));
        Vec3 velocity = normal.scale(0.35);

        for (int i = 0; i < 3; i++) {
            double jitterX = exhaustPos.x + (level.random.nextDouble() - 0.5) * 0.2;
            double jitterY = exhaustPos.y + (level.random.nextDouble() - 0.5) * 0.2;
            double jitterZ = exhaustPos.z + (level.random.nextDouble() - 0.5) * 0.2;
            // count=0 makes the server send xd/yd/zd straight through as the particle's spawn velocity,
            // instead of treating them as a random position-jitter radius the way count>0 would.
            serverLevel.sendParticles(ModParticleTypes.ROCKET_PLUME.get(), jitterX, jitterY, jitterZ, 0,
                    velocity.x, velocity.y, velocity.z, 1.0);
        }

        if (level.getGameTime() % 10 == 0) {
            level.playSound(null, getBlockPos(), SoundEvents.BLAZE_BURN, SoundSource.BLOCKS,
                    1.0f, 0.8f + level.random.nextFloat() * 0.2f);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("BurnTicksRemaining", burnTicksRemaining);
        tag.putInt("BurnTicksTotal", burnTicksTotal);
        tag.putFloat("Efficiency", efficiency);
        if (clientPacket) {
            tag.putInt("FuelBlocks", fuelBlocks);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        burnTicksRemaining = tag.getInt("BurnTicksRemaining");
        burnTicksTotal = tag.getInt("BurnTicksTotal");
        efficiency = tag.contains("Efficiency") ? tag.getFloat("Efficiency") : 1.0f;
        if (clientPacket) {
            fuelBlocks = tag.getInt("FuelBlocks");
        }
    }

    // --- Engineer's goggles overlay - same layout as Create Propulsion: Simulated's thruster stats card ---

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // CreateLang.translate(key) always looks up "create." + key (CreateLang.builder() hardcodes
        // Create's own mod id as the LangBuilder namespace) - fine for Create's own keys, but it silently
        // resolves to a key that doesn't exist for ours, so our own keys go through Component.translatable
        // directly instead, same as Create Propulsion: Simulated does for its own goggle text.
        CreateLang.builder()
                .add(Component.translatable("gui.goggles.solid_rocket_booster.title"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        CreateLang.builder()
                .add(Component.translatable("gui.goggles.solid_rocket_booster.status"))
                .text(": ")
                .add(getGoggleStatus())
                .forGoggles(tooltip);

        int efficiencyPercent = Math.round(efficiency * 100f);
        CreateLang.builder()
                .add(Component.translatable("gui.goggles.solid_rocket_booster.efficiency"))
                .text(": ")
                .add(CreateLang.number(efficiencyPercent))
                .text("%")
                .style(efficiencyColor(efficiencyPercent))
                .forGoggles(tooltip);

        CreateLang.builder()
                .add(Component.translatable("gui.goggles.solid_rocket_booster.thrust_output"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        CreateLang.builder()
                .text("  ")
                .add(Component.translatable("gui.goggles.solid_rocket_booster.thrust_label").withStyle(ChatFormatting.GRAY))
                .add(Component.literal(String.format(Locale.ROOT, "%.2f", getThrust())).withStyle(ChatFormatting.AQUA))
                .add(Component.literal(" pN").withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);

        CreateLang.builder()
                .add(Component.translatable("gui.goggles.solid_rocket_booster.fuel_label"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        CreateLang.builder()
                .text("  ")
                .add(Component.literal(String.valueOf(fuelBlocks)).withStyle(fuelBlocks > 0 ? ChatFormatting.AQUA : ChatFormatting.RED))
                .add(Component.translatable("gui.goggles.solid_rocket_booster.fuel_blocks").withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);

        // What's burning now plus everything still stacked up - an upper bound when boosters share a stack.
        int totalBurnTicks = burnTicksRemaining + fuelBlocks * Config.SOLID_ROCKET_BOOSTER_FUEL_BLOCK_BURN_TICKS.get();
        if (totalBurnTicks > 0) {
            CreateLang.builder()
                    .text("  ")
                    .add(Component.literal(formatBurnTime(totalBurnTicks)).withStyle(ChatFormatting.AQUA))
                    .add(Component.translatable("gui.goggles.solid_rocket_booster.burn_time").withStyle(ChatFormatting.GRAY))
                    .forGoggles(tooltip);
        }

        return true;
    }

    private Component getGoggleStatus() {
        if (!getBlockState().getValue(SolidRocketBoosterBlock.POWERED)) {
            return Component.translatable("gui.goggles.solid_rocket_booster.status.not_powered").withStyle(ChatFormatting.GOLD);
        }
        if (isBurning()) {
            return Component.translatable("gui.goggles.solid_rocket_booster.status.working").withStyle(ChatFormatting.GREEN);
        }
        return Component.translatable("gui.goggles.solid_rocket_booster.status.no_fuel").withStyle(ChatFormatting.RED);
    }

    private static ChatFormatting efficiencyColor(int percent) {
        if (percent >= 100) {
            return ChatFormatting.GREEN;
        }
        if (percent >= 50) {
            return ChatFormatting.GOLD;
        }
        return ChatFormatting.RED;
    }

    /** "1h 2m 3s" style, matching Create Propulsion: Simulated's own burn-time readout. */
    private static String formatBurnTime(int ticks) {
        int totalSeconds = ticks / 20;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        StringBuilder text = new StringBuilder();
        if (hours > 0) {
            text.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            text.append(minutes).append("m ");
        }
        text.append(seconds).append('s');
        return text.toString();
    }

    // --- Sable propulsion: pushes whatever ship this block is mounted to while lit ---

    @Override
    public BlockEntityPropeller getPropeller() {
        return this;
    }

    @Override
    public Direction getBlockDirection() {
        return getBlockState().getValue(SolidRocketBoosterBlock.FACING);
    }

    @Override
    public double getAirflow() {
        return isActive() ? Config.SOLID_ROCKET_BOOSTER_THRUST.get() * efficiency : 0.0;
    }

    @Override
    public double getThrust() {
        return isActive() ? Config.SOLID_ROCKET_BOOSTER_THRUST.get() * efficiency : 0.0;
    }

    @Override
    public boolean isActive() {
        return getBlockState().getValue(SolidRocketBoosterBlock.POWERED) && isBurning();
    }

    @Override
    public double getScaledThrust() {
        // BlockEntityPropeller's default scales thrust by relative airflow, which models a fan/propeller
        // losing bite as the ship catches up to it. A solid rocket booster carries its own oxidizer and
        // doesn't need surrounding air at all, so it keeps pushing at full strength regardless of ship speed.
        return -getThrust();
    }
}
