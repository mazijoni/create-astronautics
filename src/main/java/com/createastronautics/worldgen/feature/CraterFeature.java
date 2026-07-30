package com.createastronautics.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Carves a lunar-style impact crater into the terrain: a parabolic bowl whose walls expose
 * progressively deeper material (rim = stone, walls = tuff, floor = deepslate) instead of the
 * surface's concrete powder shell, plus a low raised rim of ejecta material just outside the bowl.
 *
 * The crater outline is perturbed by a couple of summed sine harmonics so it reads as an irregular
 * (but still roughly circular) impact scar rather than a perfect circle.
 */
public class CraterFeature extends Feature<CraterConfiguration> {
    public CraterFeature(Codec<CraterConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CraterConfiguration> context) {
        var level = context.level();
        var origin = context.origin();
        RandomSource random = context.random();
        CraterConfiguration config = context.config();

        int radius = config.radius().sample(random);
        if (radius < 1) {
            return false;
        }

        double depthRatio = config.depthRatio();
        double maxDepth = Math.max(1.0, radius * depthRatio);
        // Capped so that even large craters stay within the chunk generation region that's safe to write to
        // (feature generation may only reliably touch blocks close to the chunk currently being decorated).
        double rimWidth = Math.min(3.0, Math.max(2.0, radius * 0.25));
        double rimHeight = config.rimHeight();

        // Irregular-but-round outline: sum of two randomized sine harmonics around the angle.
        double jitterPhase1 = random.nextDouble() * Math.PI * 2.0;
        double jitterPhase2 = random.nextDouble() * Math.PI * 2.0;
        double jitterAmp1 = 0.05 + random.nextDouble() * 0.07;
        double jitterAmp2 = 0.03 + random.nextDouble() * 0.05;

        int extent = (int) Math.ceil(radius * 1.2 + rimWidth) + 1;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        boolean placedAny = false;
        for (int dx = -extent; dx <= extent; dx++) {
            for (int dz = -extent; dz <= extent; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;

                double angle = Math.atan2(dz, dx);
                double jitter = 1.0 + jitterAmp1 * Math.sin(angle * 3.0 + jitterPhase1)
                        + jitterAmp2 * Math.sin(angle * 5.0 + jitterPhase2);
                double effectiveRadius = radius * jitter;
                double dist = Math.sqrt((double) dx * dx + (double) dz * dz);

                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;

                if (dist <= effectiveRadius) {
                    double t = dist / effectiveRadius; // 0 = center, 1 = bowl edge
                    double depth = maxDepth * (1.0 - t * t);
                    int newSurfaceY = surfaceY - (int) Math.round(depth);

                    // Clear out the old surface shell down to the new (lower) crater floor.
                    for (int y = surfaceY; y > newSurfaceY; y--) {
                        mutable.set(x, y, z);
                        setBlock(level, mutable, Blocks.AIR.defaultBlockState());
                    }

                    // Line the exposed floor/wall with material depending on how deep into the bowl we are.
                    BlockState liningState;
                    if (t < 0.35) {
                        liningState = config.floorState();
                    } else if (t < 0.75) {
                        liningState = config.wallState();
                    } else {
                        liningState = config.rimState();
                    }

                    int liningDepth = 3;
                    for (int y = newSurfaceY; y > newSurfaceY - liningDepth; y--) {
                        mutable.set(x, y, z);
                        setBlock(level, mutable, liningState);
                    }
                    placedAny = true;
                } else if (dist <= effectiveRadius + rimWidth) {
                    // Raised ejecta rim just outside the bowl, tapering smoothly to nothing.
                    double t = (dist - effectiveRadius) / rimWidth; // 0 at bowl edge, 1 at outer edge
                    double bump = rimHeight * Math.sin(Math.PI * (1.0 - t));
                    int addBlocks = (int) Math.round(bump);
                    if (addBlocks > 0) {
                        for (int y = surfaceY + 1; y <= surfaceY + addBlocks; y++) {
                            mutable.set(x, y, z);
                            setBlock(level, mutable, config.rimState());
                        }
                        placedAny = true;
                    }
                }
            }
        }

        return placedAny;
    }
}
