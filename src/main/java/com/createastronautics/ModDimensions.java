package com.createastronautics;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class ModDimensions {
    // Shorter namespace than the mod id, purely for nicer "/execute in astronautics:moon run ..." commands -
    // every other registry these dimensions refer to (biome, noise settings, dimension type) stays under the
    // mod's own namespace.
    private static final String NAMESPACE = "astronautics";

    public static final ResourceKey<Level> MOON = key("moon");
    public static final ResourceKey<Level> DEEP_SPACE = key("deep_space");

    /**
     * How strong gravity is in a dimension relative to the Overworld's: 0 in Deep Space, 1/6 on the Moon, 1
     * everywhere else. Applies to everything that isn't a living creature - dropped items, arrows, falling
     * blocks, particles. Players get theirs through an attribute modifier instead (see
     * {@link PlayerEnvironmentHandler}), since magnetic boots have to be able to switch it back on.
     */
    public static float gravityScale(ResourceKey<Level> dimension) {
        if (dimension == DEEP_SPACE) {
            return 0.0F;
        }
        if (dimension == MOON) {
            return 1.0F / 6.0F;
        }
        return 1.0F;
    }

    private static ResourceKey<Level> key(String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(NAMESPACE, path));
    }
}
