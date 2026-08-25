package com.createastronautics.client;

import com.createastronautics.fluid.ModDataComponents;
import com.createastronautics.item.ModItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/** Shows the brass space suit chestplate's remaining oxygen as a MM:SS countdown while it's worn. */
public class OxygenTimerOverlay implements LayeredDraw.Layer {
    public static final OxygenTimerOverlay INSTANCE = new OxygenTimerOverlay();

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

        Font font = minecraft.font;
        int x = guiGraphics.guiWidth() - font.width(text) - 8;
        int y = 8;
        guiGraphics.drawString(font, text, x, y, totalSeconds <= WARNING_THRESHOLD_SECONDS ? WARNING_COLOR : NORMAL_COLOR);
    }
}
