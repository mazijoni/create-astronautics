package com.createastronautics.block;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks which players are currently standing inside some oxygen fan's sealed, oxygenated room, so
 * {@link com.createastronautics.PlayerEnvironmentHandler} doesn't need a level-wide registry of every fan to
 * ask "is this player somewhere safe right now" - each fan just marks the players it finds inside its own
 * room (see {@link OxygenFanBlockEntity}) when it recomputes that room, and this remembers it for a few
 * seconds afterwards so a player isn't punished for standing in a room between two of the fan's checks.
 */
public final class OxygenRoomTracker {
    private static final long GRACE_TICKS = 60;

    private static final Map<UUID, Long> OXYGENATED_UNTIL = new HashMap<>();

    private OxygenRoomTracker() {
    }

    public static void markOxygenated(Player player, long currentTick) {
        OXYGENATED_UNTIL.put(player.getUUID(), currentTick + GRACE_TICKS);
    }

    public static boolean isOxygenated(Player player, long currentTick) {
        Long until = OXYGENATED_UNTIL.get(player.getUUID());
        return until != null && until >= currentTick;
    }
}
