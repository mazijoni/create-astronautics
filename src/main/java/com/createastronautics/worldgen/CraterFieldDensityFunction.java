package com.createastronautics.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
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

    private static final double MAX_VALUE = CraterField.MAX_RADIUS * CraterField.RIM_HEIGHT_RATIO;
    private static final double MIN_VALUE = -(CraterField.MAX_RADIUS * CraterField.MAX_DEPTH_RATIO);

    @Override
    public double compute(FunctionContext context) {
        return CraterField.strongestContribution(context.blockX(), context.blockZ());
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
