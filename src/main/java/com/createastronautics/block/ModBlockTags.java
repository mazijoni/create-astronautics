package com.createastronautics.block;

import com.createastronautics.CreateAstronautics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Data-driven overrides for {@link OxygenSealer}'s sealing check, so exceptions to "does this block's real
 * shape fill this face" are a datapack edit away instead of a Java {@code instanceof}/property check for
 * every special case - the same approach Northstar-Redux's own sealer uses ({@code NorthstarBlockTags}'
 * {@code BLOCKS_AIR}/{@code AIR_PASSES_THROUGH}), rather than each mod maintaining its own hardcoded list of
 * "blocks that lie about their shape."
 */
public final class ModBlockTags {
    private ModBlockTags() {
    }

    /** Never lets air out through any face regardless of its real shape - e.g. doors/trapdoors/gates, which are only ever actually built flush against one face at a time. See {@link OxygenSealer#isFaceOccluded}. */
    public static final TagKey<Block> BLOCKS_AIR = create("blocks_air");
    /** Never seals any face, even where its real collision shape would otherwise read as full - reserved for a future block whose shape lies the other way. */
    public static final TagKey<Block> AIR_PASSES_THROUGH = create("air_passes_through");

    /** What a Solid Rocket Booster burns when it's stacked on its intake - see {@link SolidRocketBoosterBlockEntity}. */
    public static final TagKey<Block> SOLID_ROCKET_FUEL = create("solid_rocket_fuel");
    /** Open enough that rocket exhaust goes straight through: doesn't block the nozzle, and doesn't get cooked into magma. */
    public static final TagKey<Block> EXHAUST_PASSES_THROUGH = create("exhaust_passes_through");
    /** Blocks the nozzle like any other solid block, but can take the heat without turning to magma. */
    public static final TagKey<Block> EXHAUST_PROOF = create("exhaust_proof");

    private static TagKey<Block> create(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, path));
    }
}
