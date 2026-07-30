package com.createastronautics;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class ModDimensions {
    public static final ResourceKey<Level> MOON = key("moon");
    public static final ResourceKey<Level> DEEP_SPACE = key("deep_space");

    private static ResourceKey<Level> key(String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, path));
    }
}
