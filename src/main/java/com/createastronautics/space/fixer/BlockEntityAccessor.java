package com.createastronautics.space.fixer;

import net.minecraft.nbt.CompoundTag;

// Ported from Create-Cosmonautics' "Dimensional Sable" module (dev.egg.registries) -
// https://github.com/CosmonauticsTeam/Create-Cosmonautics - GPLv3-licensed; see LICENSE.
public class BlockEntityAccessor {
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {
        return info.tag();
    }
}
