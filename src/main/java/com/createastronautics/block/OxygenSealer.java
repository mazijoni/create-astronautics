package com.createastronautics.block;

import com.createastronautics.particle.ModParticleTypes;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the enclosed space an oxygen fan is blowing into, by flooding outward through open block faces from
 * its nozzle a bounded number of blocks per tick. The fan can keep {@code maximumSealed} blocks breathable.
 * If the flood finds more open space than that it keeps going anyway, up to {@link #LEAK_PROBE_BLOCKS}, to
 * tell the two possible reasons apart: if it runs out of space first, the room is closed but simply
 * {@linkplain #isTooBig() too big} for the fan; if it's still going at that point, the space is open to
 * the vacuum somewhere - a {@linkplain #hasLeak() leak} - and the route the flood took to get out is kept
 * as the {@linkplain #renderLeakPath leak path} so the hole can actually be found.
 *
 * The flood is re-run from scratch every {@link #CHECK_DELAY_TICKS}; the previous result stays in force
 * until the new one completes, so a room doesn't flicker unsealed while it's being re-checked.
 *
 * Ported from Northstar-Redux's {@code ProgressiveBlockSealer} (com.lightning.northstar.world.sealer) -
 * https://github.com/Astronauts-of-Create/Northstar-Redux - MIT-licensed. Differences: the too-big/leak
 * distinction above, another oxygen fan's nozzle found inside the space adds that fan's capacity
 * (Northstar's {@code SealerExtensionSource}, see {@link #extensionCapacity}), and the spot where a
 * previously sealed space broke open is recorded (see {@link #getBreachPos}).
 */
public class OxygenSealer {
    private static final long START_MARKER = Long.MIN_VALUE;

    private static final int CHECK_DELAY_TICKS = 10;
    private static final int CHECKED_BLOCKS_PER_TICK = 1024;
    // How much open space counts as "this is just the outside" rather than a very large room.
    private static final int LEAK_PROBE_BLOCKS = 4096;
    private static final int LEAK_PARTICLES_PER_TICK = 3;
    private static final double LEAK_PARTICLE_SPEED = 0.12;

    private final MutableBlockPos tempPos1 = new MutableBlockPos();
    private final MutableBlockPos tempPos2 = new MutableBlockPos();

    // Position -> the position the flood reached it from, so the route to any point can be walked back.
    private final Long2LongMap visited = new Long2LongOpenHashMap();
    private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
    private int extraCheckedVolume;
    private long lastChecked = START_MARKER;

    private final LongArrayList leakPath = new LongArrayList();
    private final LongSet sealedBlocks = new LongOpenHashSet();
    // Same contents as sealedBlocks, kept as an array purely so a random member can be picked cheaply.
    private long[] sealedArray = new long[0];
    private boolean hasLeak;
    private boolean tooBig;
    private int enclosedBlockCount;
    private int extraVolume;
    @Nullable
    private BlockPos breachPos;
    @Nullable
    private Direction breachDirection;

    private int checkCooldown;

    public OxygenSealer() {
        visited.defaultReturnValue(START_MARKER);
    }

    /** @return whether a flood just completed and the sealed set (or leak) was updated. */
    public boolean processSeal(Level level, BlockPos origin, int maximumSealed) {
        if (isSealInProgress()) {
            return updateSeal(level, maximumSealed);
        }

        if (--checkCooldown <= 0) {
            checkCooldown = CHECK_DELAY_TICKS;
            beginSeal(origin);
        }

        return false;
    }

    private void beginSeal(BlockPos origin) {
        visited.clear();
        queue.clear();
        extraCheckedVolume = 0;
        lastChecked = origin.asLong();

        visited.put(origin.asLong(), START_MARKER);
        queue.enqueue(origin.asLong());
    }

    /** @return true if the flood is complete, false if it's still in progress. */
    private boolean updateSeal(Level level, int maximumSealed) {
        maximumSealed += extraCheckedVolume;
        int probeLimit = Math.max(maximumSealed, LEAK_PROBE_BLOCKS);

        int checked = 0;
        while (!queue.isEmpty() && checked++ < CHECKED_BLOCKS_PER_TICK && visited.size() <= probeLimit) {
            long parent = queue.dequeueLong();
            tempPos1.set(parent);

            for (Direction direction : Direction.values()) {
                tempPos2.setWithOffset(tempPos1, direction);

                long packed = tempPos2.asLong();
                if (visited.containsKey(packed)) {
                    continue;
                }
                if (isAirOccluded(level, tempPos1, tempPos2, direction)) {
                    continue;
                }

                visited.put(packed, parent);

                int extension = extensionCapacity(level, tempPos2);
                extraCheckedVolume += extension;
                maximumSealed += extension;
                probeLimit = Math.max(maximumSealed, LEAK_PROBE_BLOCKS);

                queue.enqueue(packed);
                lastChecked = packed;
            }
        }

        if (!queue.isEmpty() && visited.size() <= probeLimit) {
            return false;
        }

        onSealComplete(maximumSealed);
        return true;
    }

    private void onSealComplete(int maximumSealed) {
        boolean wasSealed = isSealed();
        // Still finding open space when the probe gave up means it's the outside; running out of space
        // first means a closed room, which then either fits the fan's capacity or doesn't.
        hasLeak = !queue.isEmpty();
        tooBig = !hasLeak && visited.size() > maximumSealed;
        enclosedBlockCount = hasLeak ? 0 : visited.size();
        leakPath.clear();
        breachPos = null;
        breachDirection = null;

        if (tooBig) {
            sealedBlocks.clear();
            sealedArray = new long[0];
        } else if (hasLeak) {
            long pos = lastChecked;
            while (pos != START_MARKER) {
                leakPath.add(pos);
                pos = visited.get(pos);
            }

            if (wasSealed) {
                findBreach();
            }

            sealedBlocks.clear();
            sealedArray = new long[0];
        } else {
            sealedBlocks.clear();
            sealedBlocks.addAll(visited.keySet());
            sealedArray = sealedBlocks.toLongArray();
        }

        extraVolume = extraCheckedVolume;

        queue.clear();
        visited.clear();
    }

    /**
     * The leak path runs from somewhere outside back to the nozzle, so walking it from the nozzle end, the
     * first step that leaves what used to be the sealed space is where the wall gave way.
     */
    private void findBreach() {
        for (int i = leakPath.size() - 1; i > 0; i--) {
            long inside = leakPath.getLong(i);
            long outside = leakPath.getLong(i - 1);
            if (sealedBlocks.contains(inside) && !sealedBlocks.contains(outside)) {
                breachPos = BlockPos.of(inside);
                BlockPos step = BlockPos.of(outside).subtract(breachPos);
                breachDirection = Direction.fromDelta(step.getX(), step.getY(), step.getZ());
                return;
            }
        }
    }

    /** Forgets everything - for when the fan stops being able to supply a room at all. */
    public void reset() {
        visited.clear();
        queue.clear();
        leakPath.clear();
        sealedBlocks.clear();
        sealedArray = new long[0];
        hasLeak = false;
        tooBig = false;
        enclosedBlockCount = 0;
        extraVolume = 0;
        extraCheckedVolume = 0;
        breachPos = null;
        breachDirection = null;
        checkCooldown = 0;
    }

    /** Extra capacity contributed by whatever is at a position the flood just reached - nothing by default. */
    protected int extensionCapacity(Level level, BlockPos pos) {
        return 0;
    }

    public static boolean isAirOccluded(BlockGetter level, BlockPos from, BlockPos to, Direction direction) {
        return isFaceOccluded(level, from, direction, true) || isFaceOccluded(level, to, direction.getOpposite(), false);
    }

    /**
     * A face stops air only if the block's own shape actually covers all of it, same as Northstar. That's
     * what makes doors and trapdoors behave: the panel is a thin slab against one face of its block, so it
     * seals that one face and nothing else - a closed door seals a doorway it stands across, an open one
     * has swung its panel round to a side face and doesn't, and a trapdoor lying flat in a hole through a
     * wall does nothing at all. Being "closed" is never taken on its own to mean sealed.
     *
     * @param source whether air would be leaving {@code pos} through this face rather than entering it. A
     *               {@link ModBlockTags#BLOCKS_AIR} block only stops air on its way <em>out</em> - air can
     *               still get into whatever part of its block space its shape leaves open.
     */
    public static boolean isFaceOccluded(BlockGetter level, BlockPos pos, Direction direction, boolean source) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof OxygenFanBlock) {
            return true;
        }

        if (source && state.is(ModBlockTags.BLOCKS_AIR)) {
            return true;
        }
        return Block.isFaceFull(state.getShape(level, pos), direction) && !state.is(ModBlockTags.AIR_PASSES_THROUGH);
    }

    /** Client-side: a trickle of the same faint mist a sealed room shows, drifting along the route the air is escaping by. */
    public void renderLeakPath(Level level) {
        if (!hasLeak || leakPath.size() < 2 || !level.isClientSide) {
            return;
        }

        RandomSource random = level.random;
        for (int n = 0; n < LEAK_PARTICLES_PER_TICK; n++) {
            int i = 1 + random.nextInt(leakPath.size() - 1);
            tempPos1.set(leakPath.getLong(i));
            tempPos2.set(leakPath.getLong(i - 1));

            level.addParticle(ModParticleTypes.OXYGEN_MIST.get(),
                    tempPos1.getX() + 0.3 + random.nextFloat() * 0.4,
                    tempPos1.getY() + 0.3 + random.nextFloat() * 0.4,
                    tempPos1.getZ() + 0.3 + random.nextFloat() * 0.4,
                    (tempPos2.getX() - tempPos1.getX()) * LEAK_PARTICLE_SPEED,
                    (tempPos2.getY() - tempPos1.getY()) * LEAK_PARTICLE_SPEED,
                    (tempPos2.getZ() - tempPos1.getZ()) * LEAK_PARTICLE_SPEED);
        }
    }

    public boolean isSealInProgress() {
        return !queue.isEmpty();
    }

    /** @return whether a flood has completed and found a closed space the fan can fill - false for a leak, a room that's too big, and before the first flood has finished. */
    public boolean isSealed() {
        return !hasLeak && !tooBig && !sealedBlocks.isEmpty();
    }

    /** @return whether the space is open to the outside. */
    public boolean hasLeak() {
        return hasLeak;
    }

    /** @return whether the space is closed, but bigger than the fan(s) blowing into it can fill. */
    public boolean isTooBig() {
        return tooBig;
    }

    /** @return the size of the closed space the last flood found, whether or not the fan can fill it - 0 if it was open to the outside. */
    public int getEnclosedBlockCount() {
        return enclosedBlockCount;
    }

    public boolean contains(BlockPos pos) {
        return sealedBlocks.contains(pos.asLong());
    }

    public int getSealedBlockCount() {
        return sealedBlocks.size();
    }

    /** @return the capacity other fans blowing into the same space added to this one's own, as of the last completed flood. */
    public int getExtraVolume() {
        return extraVolume;
    }

    /** @return how far along the current flood is, 0-1. */
    public float getProgress(int maximumSealed) {
        int target = maximumSealed + extraCheckedVolume;
        return target <= 0 ? 0.0F : Math.min(1.0F, (float) visited.size() / target);
    }

    public int getCheckCooldown() {
        return checkCooldown;
    }

    /** Only valid while {@link #isSealed()}. */
    public void randomSealedPosition(RandomSource random, MutableBlockPos out) {
        out.set(sealedArray[random.nextInt(sealedArray.length)]);
    }

    /** @return the last block inside a previously sealed space before the air got out, if the latest flood found it broken - {@code null} otherwise. */
    @Nullable
    public BlockPos getBreachPos() {
        return breachPos;
    }

    @Nullable
    public Direction getBreachDirection() {
        return breachDirection;
    }
}
