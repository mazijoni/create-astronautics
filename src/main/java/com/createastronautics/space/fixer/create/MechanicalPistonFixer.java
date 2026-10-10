package com.createastronautics.space.fixer.create;

import com.createastronautics.space.fixer.BlockEntityAccessor;
import com.createastronautics.space.fixer.BlockEntityRegistry;
import net.minecraft.nbt.CompoundTag;

// Ported from Create-Cosmonautics' "Dimensional Sable" module (dev.egg.registries.blockentities.create) -
// https://github.com/CosmonauticsTeam/Create-Cosmonautics - GPLv3-licensed; see LICENSE.
public class MechanicalPistonFixer extends BlockEntityAccessor {
    @Override
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {
        if (info.tag().contains("Running")) {
            info.tag().putBoolean("Running", false);
        }
        if (info.tag().contains("Offset")) {
            info.tag().putFloat("Offset", 0);
        }
        return info.tag();
    }
}
