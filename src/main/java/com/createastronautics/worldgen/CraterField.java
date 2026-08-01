package com.createastronautics.worldgen;

import net.minecraft.util.RandomSource;

/**
 * Shared crater math used by both {@link CraterFieldDensityFunction} (the terrain height contribution) and
 * {@link CraterSurfaceCondition} (whether a column falls inside a crater's footprint at all), so the two
 * always agree on where craters actually are.
 *
 * <p>Craters are scattered on a jittered grid: the world is divided into cells, each of which may or may
 * not contain a crater (deterministically, from a hash of its cell coordinates), with a randomized center
 * offset, radius and depth. The radius is also perturbed per-angle by a few overlapping sine harmonics, so
 * craters read as irregular, weathered blobs instead of perfect circles.</p>
 */
final class CraterField {
    static final int CELL_SIZE = 40;
    static final double PRESENCE_CHANCE = 0.3;
    static final int MIN_RADIUS = 7;
    static final int MAX_RADIUS = 26;
    static final double MAX_DEPTH_RATIO = 0.32;
    static final double RIM_HEIGHT_RATIO = 0.12;
    static final double RIM_WIDTH_RATIO = 0.3;

    private CraterField() {
    }

    /** Signed height offset in blocks - negative inside the bowl, positive over the raised rim, else 0. */
    static double strongestContribution(int x, int z) {
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

    /** Whether the column falls within any crater's bowl or raised rim. */
    static boolean isInCrater(int x, int z) {
        return strongestContribution(x, z) != 0.0;
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

        // A handful of overlapping sine harmonics with random phase, so the crater's outline bulges and
        // dents rather than tracing a perfect circle - real impact craters are never perfectly round.
        double phase1 = random.nextDouble() * Math.PI * 2.0;
        double phase2 = random.nextDouble() * Math.PI * 2.0;
        double phase3 = random.nextDouble() * Math.PI * 2.0;

        double dx = x - centerX;
        double dz = z - centerZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double theta = Math.atan2(dz, dx);
        double wobble = 0.12 * Math.cos(3.0 * theta + phase1)
                + 0.08 * Math.cos(5.0 * theta + phase2)
                + 0.06 * Math.cos(2.0 * theta + phase3);
        double effectiveRadius = radius * (1.0 + wobble);
        double rimWidth = Math.max(2.0, effectiveRadius * RIM_WIDTH_RATIO);

        if (dist <= effectiveRadius) {
            double t = dist / effectiveRadius;
            return -maxDepth * (1.0 - t * t);
        } else if (dist <= effectiveRadius + rimWidth) {
            double t = (dist - effectiveRadius) / rimWidth;
            return rimHeight * Math.sin(Math.PI * (1.0 - t));
        }
        return 0.0;
    }

    private static long hashCell(int cellX, int cellZ) {
        long h = 374761393L + (long) cellX * 668265263L + (long) cellZ * 2246822519L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return h ^ (h >>> 16);
    }
}
