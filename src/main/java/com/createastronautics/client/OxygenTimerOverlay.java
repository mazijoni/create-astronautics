package com.createastronautics.client;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.fluid.ModDataComponents;
import com.createastronautics.item.ModItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * Shows the brass space suit chestplate's remaining oxygen as a MM:SS countdown, drawn over
 * {@link #BAR_TEXTURE} sitting immediately to the right of the hotbar, vertically centered on it - not
 * above it (that's vanilla's own air bubble/food row) but beside it, in the empty space past the hotbar's
 * right edge.
 */
public class OxygenTimerOverlay implements LayeredDraw.Layer {
    public static final OxygenTimerOverlay INSTANCE = new OxygenTimerOverlay();

    private static final ResourceLocation BAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "textures/gui/oxygen_bar.png");
    private static final int BAR_WIDTH = 51;
    private static final int BAR_HEIGHT = 24;
    // Matches the hotbar's own half-width (182px wide, centered) and height, to sit right beside it.
    private static final int HOTBAR_HALF_WIDTH = 91;
    private static final int HOTBAR_HEIGHT = 22;
    private static final int GAP_BESIDE_HOTBAR = 4;
    // Nudges just the text off dead-center within the bar - the bar texture itself is untouched.
    private static final int TEXT_OFFSET_X = 2;
    private static final int TEXT_OFFSET_Y = 2;

    // Matches PlayerEnvironmentHandler's drain rate: 1 bucket (1000 mB) lasts 5 minutes (300 seconds).
    private static final float SECONDS_PER_MB = 300.0F / 1000.0F;
    private static final int WARNING_THRESHOLD_SECONDS = 60;
    private static final int NORMAL_COLOR = 0x55DFFF;
    private static final int WARNING_COLOR = 0xFF5555;

    private OxygenTimerOverlay() {
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(ModItems.BRASS_SPACE_SUIT_CHESTPLATE.get())) {
            return;
        }

        int amountMb = chest.getOrDefault(ModDataComponents.OXYGEN_CONTENT.get(), SimpleFluidContent.EMPTY).getAmount();
        int totalSeconds = Math.round(amountMb * SECONDS_PER_MB);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String text = String.format("%02d:%02d", minutes, seconds);

        int barX = guiGraphics.guiWidth() / 2 + HOTBAR_HALF_WIDTH + GAP_BESIDE_HOTBAR;
        int barY = guiGraphics.guiHeight() - HOTBAR_HEIGHT / 2 - BAR_HEIGHT / 2;
        guiGraphics.blit(BAR_TEXTURE, barX, barY, 0, 0.0F, 0.0F, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

        Font font = minecraft.font;
        int textX = barX + (BAR_WIDTH - font.width(text)) / 2 + TEXT_OFFSET_X;
        int textY = barY + (BAR_HEIGHT - font.lineHeight) / 2 + TEXT_OFFSET_Y;
        guiGraphics.drawString(font, text, textX, textY, totalSeconds <= WARNING_THRESHOLD_SECONDS ? WARNING_COLOR : NORMAL_COLOR);
    }
}
