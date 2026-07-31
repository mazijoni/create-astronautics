package com.createastronautics.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Scatters bowl-shaped impact craters directly into the terrain height, instead of carving them in
 * afterwards as a placed feature. Feature decoration can only safely touch blocks within about one chunk
 * of wherever it was placed, which caps crater size; baking the shape into the density function itself
 * removes that limit entirely, since every column is sampled independently from the same deterministic
 * per-cell noise regardless of chunk boundaries.
 *
 * <p>Craters are scattered on a jittered grid: the world is divided into cells, each of which may or may
 * not contain a crater (deterministically, from a hash of its cell coordinates), with a randomized center
 * offset, radius and depth. The output is a signed height offset in blocks - negative inside the bowl,
 * positive over the raised rim just outside it - meant to be scaled and added into the base terrain
 * density.</p>
 */
public class CraterFieldDensityFunction implements DensityFunction.SimpleFunction {
    public static final MapCodec<CraterFieldDensityFunction> CODEC = MapCodec.unit(CraterFieldDensityFunction::new);
    public static final KeyDispatchDataCodec<CraterFieldDensityFunction> DATA_CODEC = KeyDispatchDataCodec.of(CODEC);

    private static final int CELL_SIZE = 40;
    private static final double PRESENCE_CHANCE = 0.3;
    private static final int MIN_RADIUS = 7;
    private static final int MAX_RADIUS = 26;
    private static final double MAX_DEPTH_RATIO = 0.32;
    private static final double RIM_HEIGHT_RATIO = 0.12;
    private static final double RIM_WIDTH_RATIO = 0.3;

    private static final double MAX_VALUE = MAX_RADIUS * RIM_HEIGHT_RATIO;
    private static final double MIN_VALUE = -(MAX_RADIUS * MAX_DEPTH_RATIO);

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();

        int cellX = Math.floorDiv(x, CELL_SIZE);
        int cellZ = Math.floorDiv(z, CELL_SIZE);

        double strongest = 0.0;
        for (int dcx = -1; dcx <= 1; dcx++) {
            for (int dcz = -1; dcz <= 1; dcz++) {
                double contribution = craterContributionAt(cellX + dcx, cellZ + dcz, x, z);
                if (Math.abs(contribution) > Math.abs(strongest)) {
                    strongest = contribution;
                }
            }
        }
        return strongest;
    }

    private static double craterContributionAt(int cellX, int cellZ, int x, int z) {
        RandomSource random = RandomSource.create(hashCell(cellX, cellZ));
        if (random.nextDouble() >= PRESENCE_CHANCE) {
            return 0.0;
        }

        double centerX = (cellX + 0.2 + random.nextDouble() * 0.6) * CELL_SIZE;
        double centerZ = (cellZ + 0.2 + random.nextDouble() * 0.6) * CELL_SIZE;
        int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
        double maxDepth = radius * MAX_DEPTH_RATIO;
        double rimHeight = radius * RIM_HEIGHT_RATIO;
        double rimWidth = Math.max(2.0, radius * RIM_WIDTH_RATIO);

        double dx = x - centerX;
        double dz = z - centerZ;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist <= radius) {
            double t = dist / radius;
            return -maxDepth * (1.0 - t * t);
        } else if (dist <= radius + rimWidth) {
            double t = (dist - radius) / rimWidth;
            return rimHeight * Math.sin(Math.PI * (1.0 - t));
        }
        return 0.0;
    }

    private static long hashCell(int cellX, int cellZ) {
        long h = 374761393L + (long) cellX * 668265263L + (long) cellZ * 2246822519L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return h ^ (h >>> 16);
    }

    @Override
    public double minValue() {
        return MIN_VALUE;
    }

    @Override
    public double maxValue() {
        return MAX_VALUE;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return DATA_CODEC;
    }
}
