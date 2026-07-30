package com.createastronautics.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Config for a single impact crater: a parabolic bowl dug into the terrain, lined with an inner
 * (deepest), mid-wall and rim material, plus a low raised ejecta rim just outside the bowl edge.
 */
public record CraterConfiguration(IntProvider radius, float depthRatio, float rimHeight, BlockState floorState,
        BlockState wallState, BlockState rimState) implements FeatureConfiguration {
    public static final Codec<CraterConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.codec(1, 48).fieldOf("radius").forGetter(CraterConfiguration::radius),
            Codec.FLOAT.fieldOf("depth_ratio").forGetter(CraterConfiguration::depthRatio),
            Codec.FLOAT.fieldOf("rim_height").forGetter(CraterConfiguration::rimHeight),
            BlockState.CODEC.fieldOf("floor_state").forGetter(CraterConfiguration::floorState),
            BlockState.CODEC.fieldOf("wall_state").forGetter(CraterConfiguration::wallState),
            BlockState.CODEC.fieldOf("rim_state").forGetter(CraterConfiguration::rimState))
            .apply(instance, CraterConfiguration::new));
}
