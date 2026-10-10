package com.createastronautics.space.fixer;

import com.createastronautics.CreateAstronautics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Set;

// Ported from Create-Cosmonautics' "Dimensional Sable" module (dev.egg.registries) -
// https://github.com/CosmonauticsTeam/Create-Cosmonautics - GPLv3-licensed; see LICENSE.
public class PosBlockEntityAccessor extends BlockEntityAccessor {
    private final Set<String> positionTags;

    public PosBlockEntityAccessor(final Set<String> tags) {
        positionTags = tags;
    }

    @Override
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {
        for (String tag : positionTags) {
            if (info.tag().contains(tag)) {
                var list = info.tag().getIntArray(tag);
                BlockPos pos = new BlockPos(list[0], list[1], list[2]);
                pos = pos.offset(info.moveInfo().oldToNewSubLevelMap().get(info.oldSubLevelID()).second());
                info.tag().putIntArray(tag, new int[]{pos.getX(), pos.getY(), pos.getZ()});
                CreateAstronautics.LOGGER.info("[ship-move] pos-fixed {} on {}: -> {}", tag, info.tag().getString("id"), pos);
            } else {
                CreateAstronautics.LOGGER.info("[ship-move] pos-fixer looked for tag={} on {} - not present as int-array (actual type={})",
                        tag, info.tag().getString("id"), tagTypeIfPresentUnderDifferentType(info.tag(), tag));
            }
        }
        return info.tag();
    }

    /** Debug helper: reports what NBT type a same-named tag actually has, if it's not the int-array we expect. */
    private static String tagTypeIfPresentUnderDifferentType(CompoundTag tag, String key) {
        for (byte type = 0; type < 13; type++) {
            if (tag.contains(key, type)) {
                return "TAG_" + type;
            }
        }
        return Tag.TAG_END + "/absent";
    }
}
