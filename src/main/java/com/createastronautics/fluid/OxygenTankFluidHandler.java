package com.createastronautics.fluid;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

/** Backs the brass space suit chestplate's tank - only accepts/releases oxygen, unlike a generic fluid item. */
public class OxygenTankFluidHandler extends FluidHandlerItemStack {
    public OxygenTankFluidHandler(ItemStack container, int capacityMb) {
        super(ModDataComponents.OXYGEN_CONTENT, container, capacityMb);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return isOxygen(stack);
    }

    @Override
    public boolean canFillFluidType(FluidStack fluid) {
        return isOxygen(fluid);
    }

    @Override
    public boolean canDrainFluidType(FluidStack fluid) {
        return isOxygen(fluid);
    }

    private static boolean isOxygen(FluidStack stack) {
        return stack.getFluid().isSame(ModFluids.OXYGEN.get());
    }
}
