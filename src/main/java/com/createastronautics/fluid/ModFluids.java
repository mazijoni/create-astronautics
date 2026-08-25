package com.createastronautics.fluid;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, CreateAstronautics.MODID);

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OXYGEN = FLUIDS.register("oxygen",
            () -> new BaseFlowingFluid.Source(oxygenProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OXYGEN_FLOWING = FLUIDS.register("oxygen_flowing",
            () -> new BaseFlowingFluid.Flowing(oxygenProperties()));

    // Deliberately has no .block(...) registered: without an associated LiquidBlock, the fluid can never
    // form a world-placed block (BaseFlowingFluid#createLegacyBlock falls back to Blocks.AIR), so it can't
    // be placed like water. It still works in Create's tanks/pipes, which fill via the fluid handler
    // capability rather than by placing a world fluid block.
    private static BaseFlowingFluid.Properties oxygenProperties() {
        return new BaseFlowingFluid.Properties(ModFluidTypes.OXYGEN, OXYGEN, OXYGEN_FLOWING)
                .bucket(ModItems.OXYGEN_BUCKET)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .explosionResistance(1);
    }
}
