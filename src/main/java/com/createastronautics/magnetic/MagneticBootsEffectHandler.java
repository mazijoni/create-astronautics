package com.createastronautics.magnetic;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.SupportSurface;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * What magnetic boots actually do each tick, ported from Create-Cosmonautics' own
 * {@code AnchorBootsItem#accelerateDescentNearBlock}: a small constant downward nudge whenever there's a
 * block within reach directly below (see {@link SupportSurface}), so the player settles onto (and stays on)
 * whatever's underneath them in zero-g instead of drifting - not a reorientation mechanic, just "gravity,
 * but only close to a surface."
 * <p>
 * Gated on {@link MagneticBootsNetworkHandler#isActive}, which - unlike the block-proximity check below -
 * only ever changes from an explicit player toggle, never automatically.
 */
@EventBusSubscriber(modid = CreateAstronautics.MODID)
public class MagneticBootsEffectHandler {
    private static final double PULL_ACCEL = 0.05;
    private static final double SUPPORT_TOLERANCE = 1.0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (!MagneticBootsNetworkHandler.isActive(player) || !MagneticBootsNetworkHandler.canActivate(player)) {
            return;
        }
        if (player.getAbilities().flying) {
            return;
        }
        if (!SupportSurface.isNear(player, SUPPORT_TOLERANCE)) {
            return;
        }

        player.setDeltaMovement(player.getDeltaMovement().add(0.0, -PULL_ACCEL, 0.0));
    }
}
