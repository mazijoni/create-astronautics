package com.createastronautics.client;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.client.sky.DeepSpaceSpecialEffects;
import com.createastronautics.client.sky.MoonSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

@Mod(value = CreateAstronautics.MODID, dist = Dist.CLIENT)
public class CreateAstronauticsClient {
    public CreateAstronauticsClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerDimensionSpecialEffects);
    }

    private void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "moon"), new MoonSpecialEffects());
        event.register(ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "deep_space"), new DeepSpaceSpecialEffects());
    }
}
