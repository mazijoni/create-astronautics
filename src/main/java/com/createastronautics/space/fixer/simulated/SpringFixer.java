package com.createastronautics.space.fixer.simulated;

import com.createastronautics.space.fixer.BlockEntityAccessor;
import com.createastronautics.space.fixer.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

// Ported from Create-Cosmonautics' "Dimensional Sable" module (dev.egg.registries.blockentities.simulated) -
// https://github.com/CosmonauticsTeam/Create-Cosmonautics - GPLv3-licensed; see LICENSE.
public class SpringFixer extends BlockEntityAccessor {
    @Override
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {
        // "Goal" is a packed long BlockPos rather than an int array, so this doesn't go through the generic
        // connection fixer the way most sub-level-referencing block entities do.
        if (info.tag().hasUUID("GoalSubLevel")) {
            var newGoal = info.moveInfo().oldToNewSubLevelMap().get(info.tag().getUUID("GoalSubLevel"));
            if (newGoal != null) {
                UUID goalSubLevel = newGoal.first();
                Vec3i offset = newGoal.second();
                info.tag().putUUID("GoalSubLevel", goalSubLevel);

                if (info.tag().contains("Goal")) {
                    BlockPos pos = BlockPos.of(info.tag().getLong("Goal"));
                    pos = pos.offset(offset);
                    info.tag().putLong("Goal", pos.asLong());
                }
            }
        }
        return info.tag();
    }
}
