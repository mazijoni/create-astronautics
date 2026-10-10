package com.createastronautics.space.fixer;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;
import java.util.UUID;

// Ported from Create-Cosmonautics' "Dimensional Sable" module (dev.egg.registries) -
// https://github.com/CosmonauticsTeam/Create-Cosmonautics - GPLv3-licensed; see LICENSE.
public class UUIDBlockEntityAccessor extends BlockEntityAccessor {
    private final Set<String> uuidTags;

    public UUIDBlockEntityAccessor(final Set<String> tags) {
        uuidTags = tags;
    }

    @Override
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {
        for (String tag : uuidTags) {
            if (info.tag().hasUUID(tag)) {
                var newSubLevel = info.moveInfo().oldToNewSubLevelMap().get(info.oldSubLevelID());
                if (newSubLevel != null) {
                    UUID goalSubLevel = newSubLevel.first();
                    info.tag().putUUID(tag, goalSubLevel);
                }
            }
        }
        return info.tag();
    }
}
