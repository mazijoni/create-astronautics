package com.createastronautics.fluid;

import com.createastronautics.CreateAstronautics;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModFluidTypes {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CreateAstronautics.MODID);

    // Density <= 0 marks the fluid as lighter than air, which is what makes Create's fluid tanks
    // (FluidTankRenderer/FluidTankBlockEntity) fill from the top down instead of the usual bottom-up.
    public static final DeferredHolder<FluidType, FluidType> OXYGEN = FLUID_TYPES.register("oxygen",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.createastronautics.oxygen")
                    .density(-1)
                    .viscosity(100)
                    .temperature(300)
                    .lightLevel(0)
                    .canSwim(false)
                    .canDrown(false)
                    .canExtinguish(false)
                    .canConvertToSource(false)));
}
