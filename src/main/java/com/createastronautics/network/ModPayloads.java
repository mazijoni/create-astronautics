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
    }
}
