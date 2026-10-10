package com.createastronautics.space.fixer;

import com.createastronautics.space.Pair;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

/**
 * A block entity's saved NBT can reference positions or sub-level UUIDs belonging to OTHER blocks (Create's
 * kinetic "Source" pointer, a Simulated swivel bearing's linked plate, a rope's block attachments, ...).
 * Those references go stale the instant a ship is rebuilt somewhere else, since the referenced blocks moved
 * too. A "fixer" registered here gets a chance to rewrite such references during
 * {@link com.createastronautics.space.SubLevelTemplate#load} - see {@link #modifyNBT}.
 *
 * Ported from Create-Cosmonautics' "Dimensional Sable" module (dev.egg.registries) -
 * https://github.com/CosmonauticsTeam/Create-Cosmonautics - GPLv3-licensed; see LICENSE.
 */
public class BlockEntityRegistry {
    private static final HashMap<String, Vector<BlockEntityAccessor>> blockEntityFixerMap = new HashMap<>();

    public record BlockEntityInfo(CompoundTag tag, UUID oldSubLevelID, MoveInfo moveInfo) {}

    public record MoveInfo(HashMap<UUID, Pair<UUID, Vec3i>> oldToNewSubLevelMap, Vector3d translationOffset,
                            ServerLevel oldDimension, ServerLevel newDimension) {}

    /** No-op passthrough for any block entity type with no fixer registered. */
    public static CompoundTag modifyNBT(final BlockEntityInfo info) {
        String id = info.tag.getString("id");
        Vector<BlockEntityAccessor> accessors = blockEntityFixerMap.get(id);
        if (accessors != null) {
            for (BlockEntityAccessor accessor : accessors) {
                accessor.modifyNBT(info);
            }
        }
        return info.tag();
    }

    public static void publishCompoundPosFixer(final String namespace, final Set<String> blockEntities, final Set<String> tags) {
        for (String blockEntity : blockEntities) {
            publishPosFixer(namespace, blockEntity, tags);
        }
    }

    public static void publishCompoundUUIDFixer(final String namespace, final Set<String> blockEntities, final Set<String> tags) {
        for (String blockEntity : blockEntities) {
            publishUUIDFixer(namespace, blockEntity, tags);
        }
    }

    /** For fixers that need more than a simple position/UUID rewrite. */
    public static void publishCustomFixer(final String namespace, final String blockEntityLocation, final BlockEntityAccessor blockEntityAccessor) {
        final String location = namespace + ":" + blockEntityLocation;
        blockEntityFixerMap.putIfAbsent(location, new Vector<>());
        blockEntityFixerMap.get(location).add(blockEntityAccessor);
    }

    /** For a block entity that stores one or more absolute BlockPos int-array tags needing the same offset applied. */
    public static void publishPosFixer(final String namespace, final String blockEntityLocation, final Set<String> tags) {
        publishCustomFixer(namespace, blockEntityLocation, new PosBlockEntityAccessor(tags));
    }

    /** For a block entity that references another sub-level by UUID plus a position within it. */
    public static void publishConnectionFixer(final String namespace, final String blockEntityLocation, final String subLevelTag, final String positionTag) {
        publishCustomFixer(namespace, blockEntityLocation, new ConnectionBlockEntityAccessor(subLevelTag, positionTag));
    }

    /** For a block entity that just needs its own sub-level UUID reference (old -> new) rewritten. */
    public static void publishUUIDFixer(final String namespace, final String blockEntityLocation, final Set<String> tags) {
        publishCustomFixer(namespace, blockEntityLocation, new UUIDBlockEntityAccessor(tags));
    }
}
