package com.createastronautics.client.sky;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.config.Config;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The parts of the climb to space that the Overworld's sky dome itself doesn't cover:
 * <ul>
 *     <li>Darkens the fog colour in step with {@link RealisticOverworldEffects#spaceFade} - the fog colour
 *     is also what the screen is cleared to, so it's what shows everywhere below the sky dome, and fading
 *     the dome alone would leave the lower half of the view daytime-blue.</li>
 *     <li>Fades the Earth in beneath the camera once it's far above anything the game still renders, so the
 *     ground doesn't just dissolve into empty fog colour on the way up.</li>
 * </ul>
 */
@EventBusSubscriber(modid = CreateAstronautics.MODID, value = Dist.CLIENT)
public final class SpaceFadeFogHandler {
    // Fractions of the transition height between which the Earth fades in - Y=400 (just above the build
    // limit) to Y=1000 by default.
    private static final double EARTH_FADE_START = 0.02;
    private static final double EARTH_FADE_END = 0.05;
    private static final int OVERWORLD_SEA_LEVEL = 63;

    private SpaceFadeFogHandler() {}

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (event.getCamera().getEntity().level().dimension() != Level.OVERWORLD) {
            return;
        }

        float remaining = 1.0F - RealisticOverworldEffects.spaceFade(event.getCamera().getPosition().y);
        if (remaining < 1.0F) {
            event.setRed(event.getRed() * remaining);
            event.setGreen(event.getGreen() * remaining);
            event.setBlue(event.getBlue() * remaining);
        }
    }

    // Drawn right after the sky rather than from RealisticOverworldEffects#renderSky, so it doesn't depend
    // on that having taken over from vanilla's sky - terrain and clouds still draw over it afterwards.
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY
                || event.getCamera().getEntity().level().dimension() != Level.OVERWORLD) {
            return;
        }

        double cameraY = event.getCamera().getPosition().y;
        double top = Config.SPACE_TRANSITION_HEIGHT.get();
        float alpha = (float) Mth.clamp(
                (cameraY - top * EARTH_FADE_START) / (top * (EARTH_FADE_END - EARTH_FADE_START)), 0.0, 1.0);
        if (alpha <= 0.0F) {
            return;
        }

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(event.getModelViewMatrix());
        EarthRenderer.render(poseStack, cameraY - OVERWORLD_SEA_LEVEL, alpha);
    }
}
