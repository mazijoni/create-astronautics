package com.createastronautics.client;

import com.createastronautics.CreateAstronautics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client half of the ship-borne dimension change: while active, the "Loading terrain..." screen vanilla
 * shows on every dimension change is suppressed (see {@code MinecraftMixin}), so flying a rocket across the
 * Overworld/Deep Space boundary reads as one continuous flight instead of a teleport.
 *
 * Runs on a timer rather than purely on the server's start/end packets, so a lost or never-sent "end" can't
 * leave loading screens suppressed forever.
 */
@EventBusSubscriber(modid = CreateAstronautics.MODID, value = Dist.CLIENT)
public final class SeamlessTransition {
    private static final int DURATION_TICKS = 100;

    private static int ticksRemaining;

    private SeamlessTransition() {}

    public static void handle(boolean active) {
        ticksRemaining = active ? DURATION_TICKS : 0;
    }

    public static boolean shouldSuppress(Screen screen) {
        return ticksRemaining > 0 && screen instanceof ReceivingLevelScreen;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }
}
