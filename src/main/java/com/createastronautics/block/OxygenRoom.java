package com.createastronautics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A flood fill through open space, used to find the enclosed room (if any) an oxygen fan is blowing into -
 * the same idea as how a hot air balloon envelope is grown outward and treated as "torn open" once it
 * exceeds its expected size: fill outward through non-solid blocks up to {@code safetyCeiling}, and if
 * there's an opening to the wider world, the fill will blow straight through it and hit that ceiling almost
 * immediately rather than ever completing, since a real opening leads to effectively unbounded space.
 * Completing under the ceiling is what "sealed" means here - whether it's actually being kept oxygenated is
 * a separate question of whether enough fans are contributing to it, see {@link #maxSizeForSpeed}.
 *
 * Doors, trapdoors, and fence gates are the one case where the block's own collision shape isn't the right
 * answer: closed, they should seal the room like any other solid block, but open, the door panel itself
 * still has a sliver of collision hugging one side of the block - which would otherwise make the fill treat
 * an open doorway as a wall. All 3 share the same vanilla {@code OPEN} property, so that's checked directly
 * instead of the shape.
 */
public final class OxygenRoom {
    private OxygenRoom() {
    }

    /** @return the enclosed room's blocks, or {@code null} if {@code start} isn't open or the space isn't sealed within {@code safetyCeiling} blocks. */
    @Nullable
    public static Room fill(Level level, BlockPos start, int safetyCeiling) {
        if (!isPassable(level, start)) {
            return null;
        }

        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> order = new ArrayList<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();

        BlockPos startPos = start.immutable();
        visited.add(startPos);
        order.add(startPos);
        frontier.add(startPos);

        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.poll();
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (visited.contains(neighbor)) {
                    continue;
                }
                if (!level.isLoaded(neighbor)) {
                    // Unloaded means we can't tell whether it's sealed - fail safe rather than assume it is.
                    return null;
                }
                if (!isPassable(level, neighbor)) {
                    continue;
                }
                if (visited.size() >= safetyCeiling) {
                    return null;
                }

                BlockPos neighborPos = neighbor.immutable();
                visited.add(neighborPos);
                order.add(neighborPos);
                frontier.add(neighborPos);
            }
        }

        return new Room(visited, order);
    }

    private static boolean isPassable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.OPEN)) {
            return state.getValue(BlockStateProperties.OPEN);
        }
        return state.getCollisionShape(level, pos).isEmpty();
    }

    /** @return how many blocks of room a single fan spinning at {@code speed} can pull its weight for. */
    public static int maxSizeForSpeed(float speed, float blocksPerRpm, int minBlocks, int maxBlocks) {
        return Mth.clamp(Math.round(Math.abs(speed) * blocksPerRpm), minBlocks, maxBlocks);
    }

    /** The sealed set of blocks a room occupies. */
    public record Room(Set<BlockPos> positions, List<BlockPos> order) {
        public boolean contains(BlockPos pos) {
            return positions.contains(pos);
        }

        public BlockPos randomPosition(RandomSource random) {
            return order.get(random.nextInt(order.size()));
        }
    }
}
