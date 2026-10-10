package com.createastronautics.space.fixer.create;

import com.createastronautics.space.fixer.BlockEntityAccessor;
import com.createastronautics.space.fixer.BlockEntityRegistry;
import net.minecraft.nbt.CompoundTag;

/**
 * Resets a bearing's running/angle state on move rather than trying to preserve it - covers Create's
 * mechanical/windmill/clockwork bearings and Aeronautics' propeller bearings, which all share this shape.
 *
 * Ported from Create-Cosmonautics' "Dimensional Sable" module (dev.egg.registries.blockentities.create) -
 * https://github.com/CosmonauticsTeam/Create-Cosmonautics - GPLv3-licensed; see LICENSE.
 */
public class BearingFixer extends BlockEntityAccessor {
    @Override
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {
        if (info.tag().contains("Running")) {
            info.tag().putBoolean("Running", false);
        }
        if (info.tag().contains("Angle")) {
            info.tag().putFloat("Angle", 0);
        }
        return info.tag();
    }
}
