package com.createastronautics.block;

import com.createastronautics.config.Config;
import com.createastronautics.particle.ModParticleTypes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Locale;

/**
 * Burns items dropped into its intake face (whatever counts as furnace fuel) and, while lit, both stays
 * "powered" for redstone purposes and pushes on any Sable ship it's mounted to - see
 * {@link #getScaledThrust()} for how that push is computed.
 */
public class SolidRocketBoosterBlockEntity extends SmartBlockEntity
        implements IHaveGoggleInformation, BlockEntitySubLevelPropellerActor, BlockEntityPropeller {
    private static final int ADJACENT_PULL_INTERVAL = 8; // matches vanilla hopper's own cooldown
    private static final int OBSTRUCTION_SCAN_INTERVAL = 10;
    private static final int OBSTRUCTION_SCAN_LENGTH = 5; // blocks of clearance in front of the nozzle for 100% efficiency
    private static final int BURN_SYNC_INTERVAL = 20; // keeps the goggle countdown live instead of only updating on fuel changes
    private static final int FUEL_STACK_LIMIT = 64;

    private final ItemStackHandler fuelInventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            sendData();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidFuel(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return FUEL_STACK_LIMIT;
        }
    };

    private int burnTicksRemaining;
    private int burnTicksTotal;
    // 1 = fully clear nozzle, 0 = completely blocked. Recomputed periodically in tick(), see updateObstruction().
    private float efficiency = 1.0f;

    public SolidRocketBoosterBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.SOLID_ROCKET_BOOSTER.get(), pos, state);
    }

    public SolidRocketBoosterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // No filter slot - any furnace fuel is accepted.
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        if (level.getGameTime() % ADJACENT_PULL_INTERVAL == 0) {
            tryPullFuelFromAdjacentInventory();
        }
        if (level.getGameTime() % OBSTRUCTION_SCAN_INTERVAL == 0) {
            updateObstruction();
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
        // so both effects below require POWERED explicitly rather than relying on isBurning() alone, which
        // stays true the whole time the countdown sits paused.

        // A fully blocked nozzle has nowhere for the particle to go but into whatever's obstructing it, so
        // it's skipped entirely rather than spawning particles that immediately embed in a block.
        if (powered && isBurning() && efficiency > 0f) {
            spawnExhaustEffects();
        }

        // Only push a sync packet on a state change or on a fixed interval while burning, instead of every
        // tick, so the goggle countdown stays live without spamming updates.
        if (isBurning() != wasBurning || (powered && isBurning() && level.getGameTime() % BURN_SYNC_INTERVAL == 0)) {
            setChanged();
            sendData();
        }
    }

    /**
     * Scans outward from the nozzle along {@link SolidRocketBoosterBlock#FACING}, stopping at the first
     * obstruction (matching a real exhaust plume, which can't skip past a wall to the clear air beyond it).
     * {@link #efficiency} is the fraction of {@link #OBSTRUCTION_SCAN_LENGTH} that came back clear, and
     * scales both displayed/actual thrust and whether exhaust particles spawn at all.
     */
    private void updateObstruction() {
        Direction nozzle = getBlockState().getValue(SolidRocketBoosterBlock.FACING);
        BlockPos cursor = getBlockPos();
        int clear = 0;
        for (int i = 0; i < OBSTRUCTION_SCAN_LENGTH; i++) {
            cursor = cursor.relative(nozzle);
            if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) {
                break;
            }
            clear++;
        }

        float newEfficiency = (float) clear / OBSTRUCTION_SCAN_LENGTH;
        if (Math.abs(newEfficiency - efficiency) > 0.001f) {
            efficiency = newEfficiency;
            setChanged();
            sendData();
        }
    }

    private void startBurningNextFuel() {
        ItemStack fuel = fuelInventory.getStackInSlot(0);
        if (fuel.isEmpty()) {
            return;
        }

        int burnTime = fuel.getBurnTime(RecipeType.SMELTING);
        if (burnTime <= 0) {
            return;
        }

        fuelInventory.extractItem(0, 1, false);
        burnTicksTotal = burnTime;
        burnTicksRemaining = burnTime;
    }

    /** Hopper-style pull: pulls one valid fuel item from whatever inventory (Item Vault included) sits against the intake face. */
    private void tryPullFuelFromAdjacentInventory() {
        if (!fuelInventory.getStackInSlot(0).isEmpty()) {
            return;
        }

        Direction intake = getBlockState().getValue(SolidRocketBoosterBlock.FACING).getOpposite();
        BlockPos neighborPos = getBlockPos().relative(intake);
        IItemHandler neighborHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, intake.getOpposite());
        if (neighborHandler == null) {
            return;
        }

        for (int slot = 0; slot < neighborHandler.getSlots(); slot++) {
            ItemStack candidate = neighborHandler.extractItem(slot, 1, true);
            if (candidate.isEmpty() || !isValidFuel(candidate)) {
                continue;
            }
            if (!fuelInventory.insertItem(0, candidate, true).isEmpty()) {
                continue;
            }

            ItemStack extracted = neighborHandler.extractItem(slot, 1, false);
            fuelInventory.insertItem(0, extracted, false);
            return;
        }
    }

    private boolean isValidFuel(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    private boolean isBurning() {
        return burnTicksRemaining > 0;
    }

    /** Lets a player feed fuel by right-clicking the intake face directly, no hopper required. */
    public ItemInteractionResult insertFuelFromPlayer(Player player, InteractionHand hand, ItemStack stack) {
        if (stack.isEmpty() || !isValidFuel(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        ItemStack remainder = fuelInventory.insertItem(0, stack.copy(), false);
        if (remainder.getCount() == stack.getCount()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        player.setItemInHand(hand, remainder);
        return ItemInteractionResult.SUCCESS;
    }

    /**
     * Lets a player empty-hand right-click the intake face to take back whatever fuel is queued but not
     * yet burning. Safe to hand out the entire slot: {@link #startBurningNextFuel()} extracts a fuel item
     * from this handler the instant it starts combusting, so anything still sitting in the slot is by
     * definition not the piece currently being consumed.
     */
    public boolean extractUnusedFuel(Player player) {
        ItemStack queued = fuelInventory.getStackInSlot(0);
        if (queued.isEmpty()) {
            return false;
        }

        ItemStack extracted = fuelInventory.extractItem(0, queued.getCount(), false);
        if (!player.getInventory().add(extracted)) {
            player.drop(extracted, false);
        }
        return true;
    }

    /** Only exposed on the intake face - the side opposite the nozzle/{@link SolidRocketBoosterBlock#FACING}. */
    public IItemHandler getFuelHandler(Direction side) {
        Direction intake = getBlockState().getValue(SolidRocketBoosterBlock.FACING).getOpposite();
        return side == null || side == intake ? fuelInventory : null;
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
        tag.put("FuelInventory", fuelInventory.serializeNBT(registries));
        tag.putInt("BurnTicksRemaining", burnTicksRemaining);
        tag.putInt("BurnTicksTotal", burnTicksTotal);
        tag.putFloat("Efficiency", efficiency);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        fuelInventory.deserializeNBT(registries, tag.getCompound("FuelInventory"));
        burnTicksRemaining = tag.getInt("BurnTicksRemaining");
        burnTicksTotal = tag.getInt("BurnTicksTotal");
        efficiency = tag.contains("Efficiency") ? tag.getFloat("Efficiency") : 1.0f;
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

        ItemStack queuedFuel = fuelInventory.getStackInSlot(0);
        if (isBurning()) {
            CreateLang.builder()
                    .text("  ")
                    .add(Component.literal(formatBurnTime(burnTicksRemaining)).withStyle(ChatFormatting.AQUA))
                    .add(Component.translatable("gui.goggles.solid_rocket_booster.burn_time").withStyle(ChatFormatting.GRAY))
                    .forGoggles(tooltip);
        }
        if (!queuedFuel.isEmpty()) {
            CreateLang.builder()
                    .text("  ")
                    .add(queuedFuel.getHoverName().copy().withStyle(ChatFormatting.GRAY))
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
