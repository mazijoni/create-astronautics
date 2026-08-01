package com.createastronautics.worldgen;

import com.createastronautics.CreateAstronautics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, CreateAstronautics.MODID);

    public static final DeferredHolder<Feature<?>, CraterMaterialFeature> CRATER_MATERIAL =
            FEATURES.register("crater_material", () -> new CraterMaterialFeature(NoneFeatureConfiguration.CODEC));
}
