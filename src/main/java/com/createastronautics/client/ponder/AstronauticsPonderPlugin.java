package com.createastronautics.client.ponder;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.block.ModBlocks;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** Registers this mod's ponder scenes with Create's Ponder - see {@code CreateAstronauticsClient#clientSetup}. */
public class AstronauticsPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return CreateAstronautics.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(ModBlocks.OXYGEN_FAN.getId())
                .addStoryBoard("oxygen_fan", OxygenFanPonder::sealedRoom);
        helper.forComponents(ModBlocks.REDSTONE_REGULATOR.getId())
                .addStoryBoard("redstone_regulator", RedstoneRegulatorPonder::signalStrength);
        helper.forComponents(ModBlocks.SOLID_ROCKET_BOOSTER.getId())
                .addStoryBoard("solid_rocket_booster", SolidRocketBoosterPonder::liftoff);
        helper.forComponents(ModBlocks.OXYGEN_DETECTOR.getId())
                .addStoryBoard("oxygen_detector", OxygenDetectorPonder::detectingOxygen);
    }
}
