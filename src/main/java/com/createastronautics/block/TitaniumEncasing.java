package com.createastronautics.block;

import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;

/**
 * Hooks the titanium encased blocks (see {@link ModBlocks#TITANIUM_ENCASED_SHAFT} and friends) into Create the
 * way its own Registrate setup does for the andesite/brass/copper ones: each becomes an encased variant of the
 * block it wraps, so right-clicking that block with Titanium Casing turns it into this one, and each joins the
 * block entity type its Create counterpart already uses, so it spins/flows and renders exactly like them.
 *
 * Create's blocks and block entity types are looked up by id rather than through its {@code AllBlocks}/
 * {@code AllBlockEntityTypes} entries - those are Registrate types, which aren't on our compile classpath.
 */
public final class TitaniumEncasing {
    private TitaniumEncasing() {
    }

    /** Must run after block registration has finished - called from common setup. */
    public static void registerVariants() {
        EncasingRegistry.addVariant((ShaftBlock) createBlock("shaft"), ModBlocks.TITANIUM_ENCASED_SHAFT.get());
        EncasingRegistry.addVariant((CogWheelBlock) createBlock("cogwheel"), ModBlocks.TITANIUM_ENCASED_COGWHEEL.get());
        EncasingRegistry.addVariant((CogWheelBlock) createBlock("large_cogwheel"), ModBlocks.TITANIUM_ENCASED_LARGE_COGWHEEL.get());
        EncasingRegistry.addVariant((FluidPipeBlock) createBlock("fluid_pipe"), ModBlocks.TITANIUM_ENCASED_FLUID_PIPE.get());
    }

    public static void addToBlockEntityTypes(BlockEntityTypeAddBlocksEvent event) {
        event.modify(createBlockEntityType("encased_shaft"), ModBlocks.TITANIUM_ENCASED_SHAFT.get());
        event.modify(createBlockEntityType("encased_cogwheel"), ModBlocks.TITANIUM_ENCASED_COGWHEEL.get());
        event.modify(createBlockEntityType("encased_large_cogwheel"), ModBlocks.TITANIUM_ENCASED_LARGE_COGWHEEL.get());
        event.modify(createBlockEntityType("encased_fluid_pipe"), ModBlocks.TITANIUM_ENCASED_FLUID_PIPE.get());
    }

    private static Block createBlock(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", path));
    }

    private static ResourceKey<BlockEntityType<?>> createBlockEntityType(String path) {
        return ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("create", path));
    }
}
