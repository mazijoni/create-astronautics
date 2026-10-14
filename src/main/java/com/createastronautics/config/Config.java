package com.createastronautics.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // No public reference exists for how Sable force units relate to ship mass, so this is a tuning knob
    // to be adjusted by playtesting rather than a calibrated figure.
    public static final ModConfigSpec.DoubleValue SOLID_ROCKET_BOOSTER_THRUST = BUILDER
            .comment("Thrust applied to a Sable ship by a fully-fueled, unobstructed Solid Rocket Booster.")
            .defineInRange("solidRocketBoosterThrust", 700.0, 0.0, Double.MAX_VALUE);

    public static final ModConfigSpec.IntValue SOLID_ROCKET_BOOSTER_FUEL_BLOCK_BURN_TICKS = BUILDER
            .comment("How long, in ticks, a Solid Rocket Booster burns for on each fuel block it uses up.")
            .defineInRange("solidRocketBoosterFuelBlockBurnTicks", 200, 1, 1_000_000);

    public static final ModConfigSpec.DoubleValue SPACE_TRANSITION_HEIGHT = BUILDER
            .comment("Overworld Y level above which a Sable ship (and everyone on it) is moved into Deep Space.")
            .defineInRange("spaceTransitionHeight", 20000.0, 400.0, 1.0e7);

    public static final ModConfigSpec.DoubleValue DEEP_SPACE_EXIT_HEIGHT = BUILDER
            .comment("Deep Space Y level below which a Sable ship (and everyone on it) falls back into the Overworld.")
            .defineInRange("deepSpaceExitHeight", 0.0, -1.0e7, 1.0e7);

    // Keeps a freshly-arrived ship clear of the boundary it just crossed, so it can't immediately bounce
    // straight back the other way.
    public static final ModConfigSpec.DoubleValue TRANSITION_SAFE_OFFSET = BUILDER
            .comment("How far past the boundary a ship arrives after changing dimension, on either side.")
            .defineInRange("transitionSafeOffset", 1000.0, 16.0, 1.0e6);

    public static final ModConfigSpec.DoubleValue EARTH_RADIUS = BUILDER
            .comment("Half-size, in blocks, of the Earth seen below you in Deep Space.")
            .defineInRange("earthRadius", 100000.0, 1000.0, 1.0e8);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
