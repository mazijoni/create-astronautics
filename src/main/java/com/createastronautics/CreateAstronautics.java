package com.createastronautics;

import com.createastronautics.advancement.ModCriteriaTriggers;
import com.createastronautics.block.ModBlockEntities;
import com.createastronautics.block.ModBlocks;
import com.createastronautics.block.OxygenDetectorDisplaySource;
import com.createastronautics.block.TitaniumEncasing;
import com.createastronautics.config.Config;
import com.createastronautics.fluid.ModDataComponents;
import com.createastronautics.fluid.ModFluidTypes;
import com.createastronautics.fluid.ModFluids;
import com.createastronautics.fluid.OxygenTankFluidHandler;
import com.createastronautics.item.ModArmorMaterials;
import com.createastronautics.item.ModItems;
import com.createastronautics.magnetic.ModAttachments;
import com.createastronautics.particle.ModParticleTypes;
import com.createastronautics.space.ShipMoveFixers;
import com.createastronautics.space.SpaceTransitionHandler;
import com.createastronautics.worldgen.ModDensityFunctionTypes;
import com.createastronautics.worldgen.ModFeatures;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import org.slf4j.Logger;

@Mod(CreateAstronautics.MODID)
public class CreateAstronautics {
    public static final String MODID = "createastronautics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public CreateAstronautics(IEventBus modEventBus, ModContainer modContainer) {
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModFluidTypes.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        PlanetBlocksTab.TABS.register(modEventBus);
        ModDensityFunctionTypes.DENSITY_FUNCTION_TYPES.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModCriteriaTriggers.TRIGGER_TYPES.register(modEventBus);
        ModParticleTypes.PARTICLE_TYPES.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        OxygenDetectorDisplaySource.DISPLAY_SOURCES.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(OxygenDetectorDisplaySource::attach));
        modEventBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(TitaniumEncasing::registerVariants));
        modEventBus.addListener(TitaniumEncasing::addToBlockEntityTypes);
        modEventBus.addListener((FMLCommonSetupEvent event) -> CreateAstronauticsSection.register());
        modEventBus.addListener((FMLCommonSetupEvent event) -> ShipMoveFixers.register());
        modEventBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(SpaceTransitionHandler::init));
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // NeoForge only auto-wires this capability for items whose class is exactly BucketItem (see
        // CapabilityHooks#registerFallbackVanillaProviders), so our BucketItem subclass needs it registered
        // explicitly to be fillable/drainable by Create's tanks, pipes, spouts, etc.
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new FluidBucketWrapper(stack),
                ModItems.OXYGEN_BUCKET.get());
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new OxygenTankFluidHandler(stack, ModItems.OXYGEN_TANK_CAPACITY_MB),
                ModItems.BRASS_SPACE_SUIT_CHESTPLATE.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.OXYGEN_FAN.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler(side));
    }
}
