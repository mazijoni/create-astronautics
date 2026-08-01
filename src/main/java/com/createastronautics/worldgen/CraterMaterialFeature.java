package com.createastronautics.worldgen;

import com.createastronautics.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Bakes the "impact site" look into craters after the normal surface rules have run: wherever a column
 * falls inside a crater (per {@link CraterField}, the same math the terrain density function itself uses),
 * the moon dust surface rules would otherwise have placed is mostly stripped away to bare stone/gravel/
 * andesite, since an impact would blast off the loose regolith rather than leave it undisturbed.
 *
 * <p>This runs as a normal top-layer-modification feature rather than a surface rule, because building a
 * custom {@code SurfaceRules.ConditionSource} isn't possible without reflection - {@code SurfaceRules
 * .Condition} and {@code Context} are package-private in vanilla. A feature only ever touches blocks within
 * its own chunk, and {@link CraterField} is itself already chunk-independent (a fixed 3x3 cell lookup), so
 * this is exactly as safe as reading the crater density function directly.</p>
 */
public class CraterMaterialFeature extends Feature<NoneFeatureConfiguration> {
    private static final double ROCK_CHANCE = 0.8;

    public CraterMaterialFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        boolean changedAny = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (!CraterField.isInCrater(x, z)) {
                    continue;
                }

                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                pos.set(x, y, z);
                if (!level.getBlockState(pos).is(ModBlocks.MOON_DUST.get())) {
                    continue;
                }

                if (random.nextDouble() < ROCK_CHANCE) {
                    level.setBlock(pos, pickRock(random), 2);
                    changedAny = true;
                }
            }
        }
        return changedAny;
    }

    private static BlockState pickRock(RandomSource random) {
        double roll = random.nextDouble();
        if (roll < 1.0 / 3.0) {
            return Blocks.ANDESITE.defaultBlockState();
        } else if (roll < 2.0 / 3.0) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        return Blocks.STONE.defaultBlockState();
    }
}
