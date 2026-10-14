package com.createastronautics.space.mixin.tab;

import com.createastronautics.PlanetBlocksTab;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Draws each planet's banner over the empty row left for it in the planet blocks tab (see PlanetBlocksTab),
// scrolling with the items. Everything about how a banner is drawn - the sprite filling the row and playing
// its animation while hovered, the dark plate behind the title at the left, the two-tone title text, and
// every offset - is a copy of Simulated's own SimulatedCreativeTab#renderBanners, and the text and the
// hover animation go through Simulated's own code, so the banners match the Create Simulated tab's.
@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {
    // The item grid: 5 rows of 18px slots, starting 8px in and 17px down from the window's corner.
    private static final int VISIBLE_ROWS = 5;
    private static final int ROW_HEIGHT = 18;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 17;
    private static final int BANNER_WIDTH = PlanetBlocksTab.COLUMNS * ROW_HEIGHT;

    // Title colours, the same ones this mod's section in the Create Simulated tab uses
    // (assets/createastronautics/simulated/sections/astronautics.json). The lower half of the lettering is
    // drawn 20% darker than the upper, which is what Simulated does when a section gives only one colour.
    private static final int TITLE_PLATE_COLOR = 0xAA1A1A3A;
    private static final int TITLE_COLOR = 0xFFE0D9FF;
    private static final int TITLE_LOWER_COLOR = 0xFFB3AECC;

    @Shadow
    private static CreativeModeTab selectedTab;

    @Shadow
    private float scrollOffs;

    @Inject(method = "render", at = @At("TAIL"))
    private void createastronautics$renderPlanetBanners(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!PlanetBlocksTab.is(selectedTab)) {
            return;
        }

        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
        // Same arithmetic as ItemPickerMenu#getRowIndexForScroll, which isn't accessible from here.
        int totalRows = (screen.getMenu().items.size() + PlanetBlocksTab.COLUMNS - 1) / PlanetBlocksTab.COLUMNS;
        int firstVisibleRow = Math.max((int) (scrollOffs * (totalRows - VISIBLE_ROWS) + 0.5), 0);

        int left = screen.getGuiLeft() + GRID_X;
        int top = screen.getGuiTop() + GRID_Y;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        // Depth-tested so an item tooltip already drawn over this spot stays on top.
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        pose.translate(left, top, 0.0F);

        for (PlanetBlocksTab.Planet planet : PlanetBlocksTab.Planet.values()) {
            int row = PlanetBlocksTab.bannerRow(planet) - firstVisibleRow;
            if (row < 0 || row >= VISIBLE_ROWS) {
                continue;
            }
            int y = row * ROW_HEIGHT;

            ResourceLocation sprite = planet.bannerSprite();
            boolean hovered = mouseX >= left && mouseX <= left + BANNER_WIDTH && mouseY >= top + y && mouseY <= top + y + ROW_HEIGHT;
            SimulatedCreativeTab.setPlaying(sprite, hovered);
            graphics.blitSprite(sprite, 0, y, BANNER_WIDTH, ROW_HEIGHT);

            Component title = planet.title();
            int titleWidth = Minecraft.getInstance().font.width(title);
            graphics.fill(2, y + 2, titleWidth + 8, y + ROW_HEIGHT - 2, TITLE_PLATE_COLOR);
            SimulatedCreativeTab.drawAuraText(graphics, title, TITLE_LOWER_COLOR, TITLE_COLOR, 5, y + 5);
        }

        pose.popPose();
        RenderSystem.disableDepthTest();
    }
}
