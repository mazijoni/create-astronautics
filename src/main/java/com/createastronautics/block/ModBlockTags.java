package com.createastronautics.block;

import com.createastronautics.CreateAstronautics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Data-driven overrides for {@link OxygenRoom}'s sealing check, so exceptions to "does this block's real
 * shape fill this face" are a datapack edit away instead of a Java {@code instanceof}/property check for
 * every special case - the same approach Northstar-Redux's own sealer uses ({@code NorthstarBlockTags}'
 * {@code BLOCKS_AIR}/{@code AIR_PASSES_THROUGH}), rather than each mod maintaining its own hardcoded list of
 * "blocks that lie about their shape."
 */
public final class ModBlockTags {
    private ModBlockTags() {
    }

    /** Always seals every face regardless of its real collision shape - e.g. doors/trapdoors/gates, which are only ever actually built flush against one face at a time. */
    public static final TagKey<Block> BLOCKS_AIR = create("blocks_air");
    /** Never seals any face, even where its real collision shape would otherwise read as full - reserved for a future block whose shape lies the other way. */
    public static final TagKey<Block> AIR_PASSES_THROUGH = create("air_passes_through");

    private static TagKey<Block> create(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, path));
    }
}
