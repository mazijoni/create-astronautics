package com.createastronautics.client;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.block.ModBlocks;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.decoration.encasing.CasingConnectivity;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.fluids.PipeAttachmentModel;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogCTBehaviour;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.model.ModelSwapper;
import net.createmod.catnip.data.Couple;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Map;
import java.util.function.Function;

/**
 * The client half of Titanium Casing and the blocks encased in it: the same connected-texture models and casing
 * connectivity Create's own Registrate setup gives its andesite/brass/copper casings and encased blocks (see its
 * {@code BuilderTransformers#casing}/{@code #encasedShaft}/{@code #encasedCogwheel} and {@code AllBlocks
 * #ENCASED_FLUID_PIPE}), wired up by hand since we don't register through Registrate.
 */
public final class TitaniumCasingModels {
    // An 8x8 omnidirectional sheet like Create's own casings - its top-left tile is the plain casing.
    private static final CTSpriteShiftEntry CASING = CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
            CreateAstronautics.id("block/titanium_casing"), CreateAstronautics.id("block/titanium_casing_connected"));

    // The small encased cogwheel's sides join up with each other, vertically and horizontally, from one 2x2
    // sheet - which doubles as the large encased cogwheel's (unconnected) side texture, same as Create's.
    private static final CTSpriteShiftEntry COGWHEEL_SIDE = CTSpriteShifter.getCT(AllCTTypes.VERTICAL,
            CreateAstronautics.id("block/titanium_encased_cogwheel_side"), CreateAstronautics.id("block/titanium_encased_cogwheel_side_connected"));
    private static final CTSpriteShiftEntry COGWHEEL_OTHERSIDE = CTSpriteShifter.getCT(AllCTTypes.HORIZONTAL,
            CreateAstronautics.id("block/titanium_encased_cogwheel_side"), CreateAstronautics.id("block/titanium_encased_cogwheel_side_connected"));

    private TitaniumCasingModels() {
    }

    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        swap(models, ModBlocks.TITANIUM_CASING.get(), model -> new CTModel(model, new EncasedCTBehaviour(CASING)));
        swap(models, ModBlocks.TITANIUM_ENCASED_SHAFT.get(), model -> new CTModel(model, new EncasedCTBehaviour(CASING)));
        swap(models, ModBlocks.TITANIUM_ENCASED_COGWHEEL.get(),
                model -> new CTModel(model, new EncasedCogCTBehaviour(CASING, Couple.create(COGWHEEL_SIDE, COGWHEEL_OTHERSIDE))));
        swap(models, ModBlocks.TITANIUM_ENCASED_LARGE_COGWHEEL.get(), model -> new CTModel(model, new EncasedCogCTBehaviour(CASING)));
        // Also wrapped for pipe attachments (drains, brackets, ...), outermost - the order Create's own applies.
        swap(models, ModBlocks.TITANIUM_ENCASED_FLUID_PIPE.get(),
                model -> PipeAttachmentModel.withAO(new CTModel(model, new EncasedCTBehaviour(CASING))));
    }

    /**
     * Lets neighbouring casings and encased blocks blend into each other across the faces that are plain
     * casing - but not across a face a shaft or pipe pokes out of. Call once blocks exist (client setup).
     */
    public static void registerConnectivity() {
        CasingConnectivity connectivity = CreateClient.CASING_CONNECTIVITY;
        connectivity.makeCasing(ModBlocks.TITANIUM_CASING.get(), CASING);
        connectivity.make(ModBlocks.TITANIUM_ENCASED_SHAFT.get(), CASING,
                (state, face) -> face.getAxis() != state.getValue(EncasedShaftBlock.AXIS));
        for (Block cogwheel : new Block[]{ModBlocks.TITANIUM_ENCASED_COGWHEEL.get(), ModBlocks.TITANIUM_ENCASED_LARGE_COGWHEEL.get()}) {
            connectivity.make(cogwheel, CASING, (state, face) -> face.getAxis() == state.getValue(EncasedCogwheelBlock.AXIS)
                    && !state.getValue(face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? EncasedCogwheelBlock.TOP_SHAFT : EncasedCogwheelBlock.BOTTOM_SHAFT));
        }
        connectivity.make(ModBlocks.TITANIUM_ENCASED_FLUID_PIPE.get(), CASING,
                (state, face) -> !state.getValue(EncasedPipeBlock.FACING_TO_PROPERTY_MAP.get(face)));

        // Same layer Create gives its own encased cogwheels - the cog shows through cutout gaps in the casing.
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.TITANIUM_ENCASED_COGWHEEL.get(), ChunkRenderTypeSet.of(RenderType.cutoutMipped()));
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.TITANIUM_ENCASED_LARGE_COGWHEEL.get(), ChunkRenderTypeSet.of(RenderType.cutoutMipped()));
    }

    private static void swap(Map<ModelResourceLocation, BakedModel> models, Block block, Function<BakedModel, BakedModel> wrapper) {
        ModelSwapper.swapModels(models, ModelSwapper.getAllBlockStateModelLocations(block), wrapper);
    }
}
