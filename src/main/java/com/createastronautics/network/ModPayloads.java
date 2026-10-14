package com.createastronautics.network;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.magnetic.MagneticBootsNetworkHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = CreateAstronautics.MODID)
public class ModPayloads {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(MagneticBootsTogglePayload.TYPE, MagneticBootsTogglePayload.STREAM_CODEC,
                MagneticBootsNetworkHandler::handleToggle);
        registrar.playToClient(SeamlessTransitionPayload.TYPE, SeamlessTransitionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientHandlers.seamlessTransition(payload)));
    }

    // Kept in a nested class so the client-only SeamlessTransition is never loaded on a dedicated server -
    // the lambda above only resolves this class when a payload actually arrives, which only happens client-side.
    private static final class ClientHandlers {
        private static void seamlessTransition(SeamlessTransitionPayload payload) {
            com.createastronautics.client.SeamlessTransition.handle(payload.active());
        }
    }
}
