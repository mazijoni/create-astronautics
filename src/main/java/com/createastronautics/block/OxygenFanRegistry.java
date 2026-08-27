package com.createastronautics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Every currently-loaded oxygen fan, grouped by dimension, so "is this position breathable" can be answered
 * by checking every active fan's sealed room directly, right now, against the position actually asked about
 * - the same idea as Northstar-Redux's {@code SealingProvider}/{@code NorthstarOxygen.hasOxygen(pos)}, which
 * resolves a query by asking every registered sealer whether it covers that exact position, rather than
 * remembering which players were recently inside one. That makes it correct the instant someone crosses a
 * doorway, and correct for however many players are in however many rooms across however many fans, since
 * nothing here is keyed to a specific player at all.
 */
public final class OxygenFanRegistry {
    // Several fans sharing one room all detect a seal breaking on the same update cycle, and each playing
    // its own sound/particle burst independently would spam both, so only the first to report it per
    // dimension within this window actually announces it.
    private static final long SEAL_BREAK_ANNOUNCE_COOLDOWN_TICKS = 20;

    private static final Map<ResourceKey<Level>, Set<OxygenFanBlockEntity>> FANS_BY_DIMENSION = new HashMap<>();
    private static final Map<ResourceKey<Level>, Long> LAST_SEAL_BREAK_ANNOUNCEMENT = new HashMap<>();

    private OxygenFanRegistry() {
    }

    public static void register(Level level, OxygenFanBlockEntity fan) {
        FANS_BY_DIMENSION.computeIfAbsent(level.dimension(), key -> Collections.newSetFromMap(new WeakHashMap<>())).add(fan);
    }

    public static void unregister(Level level, OxygenFanBlockEntity fan) {
        Set<OxygenFanBlockEntity> fans = FANS_BY_DIMENSION.get(level.dimension());
        if (fans != null) {
            fans.remove(fan);
        }
    }

    /** @return whether the caller should go ahead and play the seal-broken sound/particles - false means another fan already announced one too recently. */
    public static boolean tryAnnounceSealBreak(Level level, long currentTick) {
        Long last = LAST_SEAL_BREAK_ANNOUNCEMENT.get(level.dimension());
        if (last != null && currentTick - last < SEAL_BREAK_ANNOUNCE_COOLDOWN_TICKS) {
            return false;
        }
        LAST_SEAL_BREAK_ANNOUNCEMENT.put(level.dimension(), currentTick);
        return true;
    }

    public static boolean isOxygenated(Level level, BlockPos pos) {
        Set<OxygenFanBlockEntity> fans = FANS_BY_DIMENSION.get(level.dimension());
        if (fans == null) {
            return false;
        }
        for (OxygenFanBlockEntity fan : fans) {
            OxygenRoom.Room room = fan.getOxygenatedRoom();
            if (room != null && room.contains(pos)) {
                return true;
            }
        }
        return false;
    }
}
