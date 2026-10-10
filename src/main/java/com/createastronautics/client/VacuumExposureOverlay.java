package com.createastronautics.client;

import com.createastronautics.ModDimensions;
import com.createastronautics.PlayerEnvironmentHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Shows vanilla's own powder-snow frost vignette ({@code textures/misc/powder_snow_outline.png}, the same
 * asset and blit vanilla's {@code Gui} uses for {@code player.getTicksFrozen() > 0}) whenever the player is
 * actively suffocating in a vacuum - purely a visual echo of that danger cue, not a real freeze: it doesn't
 * touch {@code Entity#setTicksFrozen}, so none of vanilla's actual freezing side effects (movement slowdown,
 * periodic freeze damage) come along for the ride, only the on-screen warning.
 *
 * <p>Driven by the player's own air supply rather than recomputing "is this player exposed" independently -
 * {@link com.createastronautics.PlayerEnvironmentHandler#onLivingBreathe} already makes air supply drop
 * immediately on unprotected vacuum exposure (skipping the normal ~15s grace countdown), and air supply is
 * ordinary synced entity data the client already has, the same way vanilla's own air bubble HUD works. The
 * dimension check keeps this from also firing for perfectly ordinary underwater drowning back on Earth.</p>
 *
 * <p>Suppressed entirely while wearing the full suit, even with an empty tank: the suit still insulates
 * against the cold, so running out of oxygen while wearing it is a suffocation problem (still deals the same
 * damage) rather than a "the vacuum is freezing you" one - the frost cue is reserved for actually being
 * exposed with no suit at all.</p>
 */
public class VacuumExposureOverlay implements LayeredDraw.Layer {
    public static final VacuumExposureOverlay INSTANCE = new VacuumExposureOverlay();

    private static final ResourceLocation POWDER_SNOW_OUTLINE = ResourceLocation.withDefaultNamespace("textures/misc/powder_snow_outline.png");

    private VacuumExposureOverlay() {
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }

        boolean noAtmosphere = player.level().dimension() == ModDimensions.MOON || player.level().dimension() == ModDimensions.DEEP_SPACE;
        int airSupply = player.getAirSupply();
        if (!noAtmosphere || airSupply >= player.getMaxAirSupply() || PlayerEnvironmentHandler.isWearingFullSpaceSuit(player)) {
            return;
        }

        float intensity = 1.0F - Math.max(airSupply, 0) / (float) player.getMaxAirSupply();
        renderOverlay(guiGraphics, intensity);
    }

    private static void renderOverlay(GuiGraphics guiGraphics, float alpha) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.blit(POWDER_SNOW_OUTLINE, 0, 0, -90, 0.0F, 0.0F, guiGraphics.guiWidth(), guiGraphics.guiHeight(), guiGraphics.guiWidth(), guiGraphics.guiHeight());
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
