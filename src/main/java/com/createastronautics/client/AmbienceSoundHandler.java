package com.createastronautics.client;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** No air means no medium for sound to travel through: muffled on the Moon, and completely silent in Deep Space. */
@EventBusSubscriber(modid = CreateAstronautics.MODID, value = Dist.CLIENT)
public class AmbienceSoundHandler {
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || event.getSound() == null) {
            return;
        }

        boolean deepSpace = level.dimension() == ModDimensions.DEEP_SPACE;
        boolean moon = level.dimension() == ModDimensions.MOON;
        if (!deepSpace && !moon) {
            return;
        }

        // There's no water here, so the "drowning" hurt sound makes no sense - drop it entirely either way.
        if (event.getSound().getLocation().getPath().contains("drown")) {
            event.setSound(null);
            return;
        }

        if (deepSpace) {
            event.setSound(null);
        } else {
            event.setSound(new MuffledSoundInstance(event.getSound(), 0.55F, 0.85F));
        }
    }
}
