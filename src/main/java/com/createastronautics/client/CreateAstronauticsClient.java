package com.createastronautics.client;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.block.ModBlockEntities;
import com.createastronautics.block.ModBlocks;
import com.createastronautics.client.particle.RocketPlumeParticle;
import com.createastronautics.client.sky.DeepSpaceSpecialEffects;
import com.createastronautics.client.sky.MoonSpecialEffects;
import com.createastronautics.client.sky.RealisticOverworldEffects;
import com.createastronautics.particle.ModParticleTypes;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@Mod(value = CreateAstronautics.MODID, dist = Dist.CLIENT)
public class CreateAstronauticsClient {
    public CreateAstronauticsClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerDimensionSpecialEffects);
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerParticleProviders);
        modEventBus.addListener(this::registerKeyMappings);
        modEventBus.addListener(this::clientSetup);
    }

    private void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "moon"), new MoonSpecialEffects());
        event.register(ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "deep_space"), new DeepSpaceSpecialEffects());
        // Replaces vanilla's own Overworld sky effects (same registry key) to add gradient night-sky shading
        // and a more varied star field - daytime, dawn/dusk, and rain are untouched, see the class javadoc.
        event.register(BuiltinDimensionTypes.OVERWORLD_EFFECTS, new RealisticOverworldEffects());
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SOLID_ROCKET_BOOSTER.get(), SmartBlockEntityRenderer::new);
    }

    private void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticleTypes.ROCKET_PLUME.get(), RocketPlumeParticle.Factory::new);
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.TOGGLE_MAGNETIC_BOOTS);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        // Matches Create's own smart chute: its model has cutout gaps in the texture that need to render
        // as transparent instead of solid black.
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(ModBlocks.SOLID_ROCKET_BOOSTER.get(), ChunkRenderTypeSet.of(RenderType.cutoutMipped())));
    }
}
