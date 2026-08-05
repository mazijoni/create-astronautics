package com.createastronautics.client;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** No air means no medium for sound to travel through: muffled on the Moon, and completely silent in Deep Space. */
@EventBusSubscriber(modid = CreateAstronautics.MODID, value = Dist.CLIENT)
public class AmbienceSoundHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbienceSoundHandler.class);

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

        // Inventory/UI feedback (crafting clicks, item pickup, armor equip, eating, XP, menu sounds) isn't
        // carried through open air the way ambient/world sounds are - it's heard directly rather than
        // propagating across a vacuum, so it's exempt from the "no atmosphere" muffling/silencing below.
        // MASTER is menu/UI-only sounds; PLAYERS covers the rest of that list (plus footsteps, which a
        // suited astronaut would still feel/hear through their own boots regardless of open air).
        SoundSource source = event.getSound().getSource();
        if (source == SoundSource.MASTER || source == SoundSource.PLAYERS) {
            return;
        }

        // The vanilla drowning hurt sound is a watery gurgle that makes no sense in a vacuum - swap it for
        // the plain hurt sound so the player still gets an audible damage cue, just without the bubbling.
        // That cue matters too much to lose, so it plays even in Deep Space, unlike every other sound.
        // Any failure building the replacement must never escape this handler - an uncaught exception here
        // runs on the same thread that's processing the incoming sound packet, and can tear down the
        // connection instead of just losing one sound effect.
        if (event.getSound().getLocation().getPath().contains("drown")) {
            try {
                SoundInstance hurtCue = plainHurtSound(event.getSound());
                event.setSound(moon ? new MuffledSoundInstance(hurtCue, 0.55F, 0.85F) : hurtCue);
            } catch (Exception e) {
                LOGGER.warn("Failed to swap drowning sound for a plain hurt sound, silencing it instead", e);
                event.setSound(null);
            }
            return;
        }

        if (deepSpace) {
            event.setSound(null);
        } else {
            event.setSound(new MuffledSoundInstance(event.getSound(), 0.55F, 0.85F));
        }
    }

    private static SoundInstance plainHurtSound(SoundInstance original) {
        return new SimpleSoundInstance(
                SoundEvents.PLAYER_HURT,
                original.getSource(),
                original.getVolume(),
                original.getPitch(),
                SoundInstance.createUnseededRandom(),
                original.getX(),
                original.getY(),
                original.getZ()
        );
    }
}
