package com.createastronautics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
 * exceeds its expected size (see Create Aeronautics' balloon envelope graph, or Northstar-Redux's
 * {@code ProgressiveBlockSealer}, both of which this is a simplified version of): fill outward through open
 * faces up to {@code safetyCeiling}, and if there's an opening to the wider world, the fill will blow
 * straight through it and hit that ceiling almost immediately rather than ever completing, since a real
 * opening leads to effectively unbounded space. Completing under the ceiling is what "sealed" means here -
 * whether it's actually being kept oxygenated is a separate question of whether enough fans are
 * contributing to it, see {@link #maxSizeForSpeed}.
 *
 * Sealing is checked per face (does either side of the shared face fully occlude it, matching vanilla's own
 * {@link Block#isFaceFull}) rather than per block, so partial shapes like slabs and stairs seal correctly
 * instead of an "any collision at all blocks everything" all-or-nothing check. That default is overridden by
 * {@link ModBlockTags#BLOCKS_AIR}/{@link ModBlockTags#AIR_PASSES_THROUGH} for the blocks whose real shape
 * doesn't reflect whether they actually seal - see {@link #occludesFace} - which currently covers closed
 * doors/trapdoors/gates and Create's own fluid tank. An open door/trapdoor/gate is always treated as fully
 * open regardless of the sliver of collision the door panel itself still occupies, ahead of any tag -
 * without that, opening a door wouldn't register as a breach at all.
 *
 * A closed door/trapdoor/gate on the room's boundary is also added to the room's own position set once the
 * fill completes, the same way a block can be waterlogged - it's still a wall (nothing flows through it,
 * and it's never queued to expand the fill any further), but the position it occupies is oxygen-logged too,
 * so it still shows mist and still counts as "inside" for queries instead of reading as a hole punched out
 * of the room.
 */
public final class OxygenRoom {
    private OxygenRoom() {
    }

    /** @return the enclosed room's blocks, or {@code null} if {@code start} isn't open or the space isn't sealed within {@code safetyCeiling} blocks. */
    @Nullable
    public static Room fill(Level level, BlockPos start, int safetyCeiling) {
        if (occludesFace(level, start, Direction.UP) && occludesFace(level, start, Direction.DOWN)
                && occludesFace(level, start, Direction.NORTH) && occludesFace(level, start, Direction.SOUTH)
                && occludesFace(level, start, Direction.EAST) && occludesFace(level, start, Direction.WEST)) {
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
                if (isFaceSealed(level, pos, neighbor, direction)) {
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

        Set<BlockPos> doorMembers = new HashSet<>();
        for (BlockPos pos : List.copyOf(order)) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (visited.contains(neighbor) || !isClosedDoorLikeBlock(level, neighbor)) {
                    continue;
                }
                BlockPos neighborPos = neighbor.immutable();
                visited.add(neighborPos);
                order.add(neighborPos);
                doorMembers.add(neighborPos);
            }
        }

        return new Room(visited, order, doorMembers);
    }

    /**
     * Finds where a previously-sealed room actually broke open, by re-checking each of its boundary faces
     * (a room member next to a position that wasn't a genuine interior-air part of the room) for one that's
     * no longer sealed. A fresh, unbounded flood fill from the outlet can't answer this on its own - once
     * there's a real opening it wanders off into the newly-unbounded space and could end up reporting a
     * position dozens of blocks away in a fairly arbitrary direction, nowhere near the actual hole - so this
     * instead compares against the room's own last-known shape to find the one specific face that changed.
     *
     * Skipping only {@link Room#isInteriorAir} neighbors (not every member) matters for a door/trapdoor on
     * the boundary: it's included in the room's own positions the same way a closed one always was, so a
     * plain "is this neighbor part of the room" skip would also skip testing the one face that actually
     * broke - the room member itself never looks different from the room's perspective, only the door's
     * live block state does.
     *
     * @return the breached position and the outward-facing direction the opening faces, or {@code null} if
     * no single boundary face change explains it (e.g. the room was restructured some other way) - callers
     * should fall back to a sensible default in that case.
     */
    @Nullable
    public static Breach findBreach(Level level, Room previousRoom) {
        for (BlockPos pos : previousRoom.order()) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (previousRoom.isInteriorAir(neighbor)) {
                    continue;
                }
                if (level.isLoaded(neighbor) && !isFaceSealed(level, pos, neighbor, direction)) {
                    return new Breach(pos, direction);
                }
            }
        }
        return null;
    }

    private static boolean isFaceSealed(Level level, BlockPos from, BlockPos to, Direction direction) {
        return occludesFace(level, from, direction) || occludesFace(level, to, direction.getOpposite());
    }

    private static boolean occludesFace(Level level, BlockPos pos, Direction direction) {
        BlockState state = level.getBlockState(pos);
        // An open door/trapdoor/gate never seals, full stop, ahead of anything else below - vanilla's own
        // "labeled closed" (the OPEN property being false) is the only thing this trusts on its own; being
        // labeled open is always taken at face value, but being labeled closed is NOT by itself taken to
        // mean the opening is actually sealed (see ModBlockTags.BLOCKS_AIR below) - a door/trapdoor's own
        // collision shape only ever touches one face (a 3px sliver flush against the top or bottom of its
        // block for a trapdoor), so whether "closed" really seals a given direction isn't something the
        // door's own state can answer, and needs the explicit tag instead.
        if (state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN)) {
            return false;
        }
        // Deliberate, data-driven overrides for blocks whose real collision shape doesn't reflect whether
        // they actually seal - the same architecture Northstar-Redux's sealer uses (NorthstarBlockTags'
        // BLOCKS_AIR/AIR_PASSES_THROUGH), rather than a per-block-type instanceof/property check in Java for
        // every exception. BLOCKS_AIR covers closed doors/trapdoors/gates (see above) and Create's own
        // fluid tank, which reuses the same empty-CollisionContext shape campfires use to let smoke rise
        // through them - that makes its real collision shape report a gap on the bottom face that has
        // nothing to do with whether air can actually pass through a tank wall.
        if (state.is(ModBlockTags.BLOCKS_AIR)) {
            return true;
        }
        if (state.is(ModBlockTags.AIR_PASSES_THROUGH)) {
            return false;
        }
        return Block.isFaceFull(state.getCollisionShape(level, pos), direction);
    }

    private static boolean isClosedDoorLikeBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.hasProperty(BlockStateProperties.OPEN) && !state.getValue(BlockStateProperties.OPEN);
    }

    /** @return how many blocks of room a single fan spinning at {@code speed} can pull its weight for. */
    public static int maxSizeForSpeed(float speed, float blocksPerRpm, int minBlocks, int maxBlocks) {
        return Mth.clamp(Math.round(Math.abs(speed) * blocksPerRpm), minBlocks, maxBlocks);
    }

    /** Where a room breached: {@code pos} is the room member the opening is at, {@code direction} the face it opens through. */
    public record Breach(BlockPos pos, Direction direction) {
    }

    /**
     * The sealed set of blocks a room occupies. {@code doorMembers} is the subset of {@code positions} that
     * are closed doors/trapdoors/gates rather than genuine interior air - see {@link #isInteriorAir}.
     */
    public record Room(Set<BlockPos> positions, List<BlockPos> order, Set<BlockPos> doorMembers) {
        public boolean contains(BlockPos pos) {
            return positions.contains(pos);
        }

        /** @return whether {@code pos} was actual open interior space in this room, as opposed to a closed door/trapdoor/gate on its boundary. */
        private boolean isInteriorAir(BlockPos pos) {
            return positions.contains(pos) && !doorMembers.contains(pos);
        }

        public BlockPos randomPosition(RandomSource random) {
            return order.get(random.nextInt(order.size()));
        }
    }
}
