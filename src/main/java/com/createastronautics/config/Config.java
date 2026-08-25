package com.createastronautics.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // No public reference exists for how Sable force units relate to ship mass, so this is a tuning knob
    // to be adjusted by playtesting rather than a calibrated figure.
    public static final ModConfigSpec.DoubleValue SOLID_ROCKET_BOOSTER_THRUST = BUILDER
            .comment("Thrust applied to a Sable ship by a fully-fueled, unobstructed Solid Rocket Booster.")
            .defineInRange("solidRocketBoosterThrust", 70.0, 0.0, Double.MAX_VALUE);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
