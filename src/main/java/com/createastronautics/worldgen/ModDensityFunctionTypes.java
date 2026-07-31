package com.createastronautics.worldgen;

import com.createastronautics.CreateAstronautics;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDensityFunctionTypes {
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, CreateAstronautics.MODID);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<CraterFieldDensityFunction>> CRATER_FIELD =
            DENSITY_FUNCTION_TYPES.register("crater_field", () -> CraterFieldDensityFunction.CODEC);
}
