package com.createastronautics.worldgen;

import com.createastronautics.block.ModBlocks;
import com.createastronautics.block.PointedMoonstoneBlock;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Turns scattered pockets of the caves/rilles carved by {@code createastronautics:moon_tunnel_cave}/
 * {@code createastronautics:lunar_rille} into moonstone grottoes, the same two-layer way vanilla dripstone
 * caves work: a crust of {@link ModBlocks#MOONSTONE_BLOCK} first grows over bare rock next to cave air, and
 * {@link PointedMoonstoneBlock} spikes only ever grow from that crust, never straight off plain stone -
 * so finding a spike always means finding the block it's named after.
 *
 * <p>Only a minority of the map gets moonstone at all: {@link #isMoonstoneZone} gates the whole feature on a
 * coarse per-region coin flip (regions are {@link #ZONE_SIZE_CHUNKS} chunks square) so it reads as pockets
 * of a distinct grotto biome-within-a-biome rather than dusting every single cave in the dimension.</p>
 *
 * <p>Clusters are placed as ready-formed 1-3 segment stacks rather than grown over time, since {@link
 * PointedMoonstoneBlock} deliberately doesn't simulate vanilla dripstone's growth (see its class doc).</p>
 */
public class MoonstoneClusterFeature extends Feature<NoneFeatureConfiguration> {
    private static final int ZONE_SIZE_CHUNKS = 4;
    private static final double ZONE_CHANCE = 0.3;

    // Same idea as the moon_rock_variant noise the surface rule uses for andesite/gravel patches: a
    // coherent 3D field thresholded, so nearby blocks agree and the crust grows in clumped deposits rather
    // than an independent per-block coin flip (which just reads as random speckling).
    private static final int CRUST_NOISE_FIRST_OCTAVE = -3;
    private static final double CRUST_NOISE_THRESHOLD = 0.35;
    private static final double SPIKE_CHANCE = 0.85;
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 110;

    public MoonstoneClusterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        if (!isMoonstoneZone(level, origin)) {
            return false;
        }

        boolean changedAny = growCrust(level, origin);
        changedAny |= growSpikes(level, origin, random);
        return changedAny;
    }

    /** @return whether {@code origin}'s chunk falls in one of the sparse regions moonstone generates in at all. */
    private static boolean isMoonstoneZone(WorldGenLevel level, BlockPos origin) {
        int cellX = Math.floorDiv(origin.getX() >> 4, ZONE_SIZE_CHUNKS);
        int cellZ = Math.floorDiv(origin.getZ() >> 4, ZONE_SIZE_CHUNKS);
        long hash = level.getSeed() + (long) cellX * 341873128712L + (long) cellZ * 132897987541L;
        return RandomSource.create(hash).nextFloat() < ZONE_CHANCE;
    }

    /** Grows a crust of {@link ModBlocks#MOONSTONE_BLOCK} over bare rock next to cave air, in clumped
     * deposits shaped by a coherent noise field rather than scattered one block at a time. */
    private static boolean growCrust(WorldGenLevel level, BlockPos origin) {
        NormalNoise clusterNoise = NormalNoise.create(RandomSource.create(level.getSeed() ^ 0x53656c4e69746579L), CRUST_NOISE_FIRST_OCTAVE, 1.0, 1.0);
        boolean changedAny = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                for (int y = MIN_Y + 1; y < MAX_Y; y++) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        continue;
                    }
                    if (clusterNoise.getValue(pos.getX(), pos.getY(), pos.getZ()) <= CRUST_NOISE_THRESHOLD) {
                        continue;
                    }

                    if (isNaturalRock(level, pos.above())) {
                        level.setBlock(pos.above(), ModBlocks.MOONSTONE_BLOCK.get().defaultBlockState(), 2);
                        changedAny = true;
                    }
                    if (isNaturalRock(level, pos.below())) {
                        level.setBlock(pos.below(), ModBlocks.MOONSTONE_BLOCK.get().defaultBlockState(), 2);
                        changedAny = true;
                    }
                }
            }
        }
        return changedAny;
    }

    /** Grows spikes only from {@link ModBlocks#MOONSTONE_BLOCK} crust, never bare rock. */
    private static boolean growSpikes(WorldGenLevel level, BlockPos origin, RandomSource random) {
        boolean changedAny = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                for (int y = MIN_Y + 1; y < MAX_Y; y++) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        continue;
                    }

                    if (random.nextDouble() < SPIKE_CHANCE && level.getBlockState(pos.above()).is(ModBlocks.MOONSTONE_BLOCK.get())) {
                        changedAny |= placeSpike(level, random, pos.immutable(), Direction.DOWN);
                    } else if (random.nextDouble() < SPIKE_CHANCE && level.getBlockState(pos.below()).is(ModBlocks.MOONSTONE_BLOCK.get())) {
                        changedAny |= placeSpike(level, random, pos.immutable(), Direction.UP);
                    }
                }
            }
        }
        return changedAny;
    }

    private static boolean isNaturalRock(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(ModBlocks.LUNAR_STONE.get()) || state.is(ModBlocks.MOON_SLATE.get())
                || state.is(ModBlocks.LUNAR_ANDESITE.get()) || state.is(ModBlocks.LUNAR_GRAVEL.get());
    }

    /**
     * Builds a spike of 1-3 segments extending from {@code start} (already known to sit against moonstone
     * crust on the opposite side) further into the air pocket in {@code tipDirection}, bailing out without
     * placing anything if the pocket isn't tall/deep enough for the length rolled.
     */
    private static boolean placeSpike(WorldGenLevel level, RandomSource random, BlockPos start, Direction tipDirection) {
        int length = 1;
        double roll = random.nextDouble();
        if (roll > 0.95) {
            length = 4;
        } else if (roll > 0.85) {
            length = 3;
        } else if (roll > 0.6) {
            length = 2;
        }

        BlockPos[] positions = new BlockPos[length];
        for (int i = 0; i < length; i++) {
            positions[i] = start.relative(tipDirection, i);
            if (!level.getBlockState(positions[i]).isAir()) {
                return false;
            }
        }

        for (int i = 0; i < length; i++) {
            // Mirrors PointedMoonstoneBlock's own calculateDripstoneThickness: the tip is always TIP, the
            // segment right before it always tapers through FRUSTUM (never straight from BASE/MIDDLE to
            // TIP), the wall-attached end is BASE, and only anything left over in between is MIDDLE.
            DripstoneThickness thickness;
            if (i == length - 1) {
                thickness = DripstoneThickness.TIP;
            } else if (i == length - 2) {
                thickness = DripstoneThickness.FRUSTUM;
            } else if (i == 0) {
                thickness = DripstoneThickness.BASE;
            } else {
                thickness = DripstoneThickness.MIDDLE;
            }

            BlockState state = ModBlocks.POINTED_MOONSTONE.get().defaultBlockState()
                    .setValue(PointedMoonstoneBlock.TIP_DIRECTION, tipDirection)
                    .setValue(PointedMoonstoneBlock.THICKNESS, thickness);
            level.setBlock(positions[i], state, 2);
        }
        return true;
    }
}
