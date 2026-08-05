package com.createastronautics.client;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.network.MagneticBootsTogglePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Purely relays "the player pressed the toggle key" to the server - see {@link com.createastronautics
 * .magnetic.MagneticBootsNetworkHandler} for where gear/dimension/surface are actually resolved. No local
 * prediction: the small round-trip is preferable to camera/movement code briefly disagreeing with whatever
 * the server ends up deciding.
 */
@EventBusSubscriber(modid = CreateAstronautics.MODID, value = Dist.CLIENT)
public class MagneticBootsInputHandler {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        while (ModKeyMappings.TOGGLE_MAGNETIC_BOOTS.consumeClick()) {
            PacketDistributor.sendToServer(new MagneticBootsTogglePayload());
        }
    }
}
