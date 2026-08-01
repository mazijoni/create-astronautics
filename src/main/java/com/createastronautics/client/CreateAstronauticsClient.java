package com.createastronautics.client;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.client.sky.DeepSpaceSpecialEffects;
import com.createastronautics.client.sky.MoonSpecialEffects;
import com.createastronautics.client.sky.RealisticOverworldEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
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
        // Replaces vanilla's own Overworld sky effects (same registry key) to add gradient night-sky shading
        // and a more varied star field - daytime, dawn/dusk, and rain are untouched, see the class javadoc.
        event.register(BuiltinDimensionTypes.OVERWORLD_EFFECTS, new RealisticOverworldEffects());
    }
}
