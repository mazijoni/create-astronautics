package com.createastronautics.client;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.block.ModBlockEntities;
import com.createastronautics.block.ModBlocks;
import com.createastronautics.client.particle.RocketPlumeParticle;
import com.createastronautics.client.sky.DeepSpaceSpecialEffects;
import com.createastronautics.client.sky.MoonSpecialEffects;
import com.createastronautics.client.sky.RealisticOverworldEffects;
import com.createastronautics.fluid.ModFluidTypes;
import com.createastronautics.particle.ModParticleTypes;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
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
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@Mod(value = CreateAstronautics.MODID, dist = Dist.CLIENT)
public class CreateAstronauticsClient {
    public CreateAstronauticsClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerDimensionSpecialEffects);
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerParticleProviders);
        modEventBus.addListener(this::registerKeyMappings);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerClientExtensions);
        modEventBus.addListener(this::registerGuiLayers);
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
        event.registerBlockEntityRenderer(ModBlockEntities.OXYGEN_FAN.get(), OxygenFanRenderer::new);
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
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SOLID_ROCKET_BOOSTER.get(), ChunkRenderTypeSet.of(RenderType.cutoutMipped()));
            // The fan blade/lattice geometry has cutout gaps, same reasoning as the smart chute above.
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.OXYGEN_FAN.get(), ChunkRenderTypeSet.of(RenderType.cutoutMipped()));
            // Matches vanilla's own torch registration (ItemBlockRenderTypes) - without this the model's
            // transparent background renders as solid black instead of see-through.
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BURNT_TORCH.get(), ChunkRenderTypeSet.of(RenderType.cutout()));
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BURNT_WALL_TORCH.get(), ChunkRenderTypeSet.of(RenderType.cutout()));
            // Registers the Flywheel-backed render path for the spinning shaft/propeller - without this,
            // the block silently renders as a bare casing whenever Flywheel visualization is active, since
            // OxygenFanRenderer (the classic BlockEntityRenderer) intentionally defers to this instead.
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.OXYGEN_FAN.get())
                    .factory(OxygenFanVisual::new)
                    .apply();
        });
    }

    private void registerClientExtensions(RegisterClientExtensionsEvent event) {
        ResourceLocation still = ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "fluid/oxygen_still");
        ResourceLocation flowing = ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "fluid/oxygen_flow");
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowing;
            }
        }, ModFluidTypes.OXYGEN.get());
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "oxygen_timer"), OxygenTimerOverlay.INSTANCE);
    }
}
