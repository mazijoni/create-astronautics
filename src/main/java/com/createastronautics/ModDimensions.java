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

    private static ResourceKey<Level> key(String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(NAMESPACE, path));
    }
}
