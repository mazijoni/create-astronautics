package com.createastronautics.client;

import com.createastronautics.CreateAstronautics;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Constructing a {@link PartialModel} registers its model location into Flywheel's own global registry, which
 * Flywheel then bakes alongside everything else the first time models are loaded - but only if the location
 * is registered before that happens. {@link #register()} exists purely so {@link CreateAstronauticsClient}
 * can force this class to load (and its field below to run) early enough, from its constructor, the same way
 * Northstar-Redux's own NorthstarPartialModels.register() does - see THIRD_PARTY_NOTICES.md.
 */
public class BrassSpaceSuitPartialModels {
    public static final PartialModel BRASS_SPACE_SUIT_HELMET =
            PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "entities/armor/brass_space_suit_helmet"));

    public static void register() {
    }
}
