package com.createastronautics.space.mixin.tab;

import com.createastronautics.PlanetBlocksTab;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

// Swaps the planet blocks tab's displayed contents for the sectioned layout once the tab has been built the
// normal way. It has to be done here: the layout is spaced out with empty stacks, and the normal way of
// filling a tab refuses those. The search tab's own copy of the items is left alone. Same approach as
// Simulated's CreativeModeTabMixin (dev.simulated_team.simulated.mixin.creative_tab_sections).
@Mixin(CreativeModeTab.class)
public class CreativeModeTabMixin {
    @Shadow
    private Collection<ItemStack> displayItems;

    @Inject(method = "buildContents", at = @At("TAIL"))
    private void createastronautics$layOutPlanetBlocks(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        if (PlanetBlocksTab.is((CreativeModeTab) (Object) this)) {
            displayItems = PlanetBlocksTab.layout();
        }
    }
}
