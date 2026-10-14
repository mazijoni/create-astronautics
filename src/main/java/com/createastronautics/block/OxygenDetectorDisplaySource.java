package com.createastronautics.block;

import com.createastronautics.CreateAstronautics;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Lets a Create Display Link read an {@link OxygenDetectorBlock}: one line, "Oxygen ✔" or "Oxygen ✘", for
 * whether the detector is currently sensing oxygen - labelable like Create's own single-line sources (e.g.
 * "Airlock: Oxygen ✔").
 */
public class OxygenDetectorDisplaySource extends SingleLineDisplaySource {
    public static final DeferredRegister<DisplaySource> DISPLAY_SOURCES = DeferredRegister.create(CreateRegistries.DISPLAY_SOURCE, CreateAstronautics.MODID);

    public static final DeferredHolder<DisplaySource, OxygenDetectorDisplaySource> OXYGEN_STATUS = DISPLAY_SOURCES.register("oxygen_status",
            OxygenDetectorDisplaySource::new);

    /** Attaches this source to the detector block - called once blocks exist (common setup). */
    public static void attach() {
        DisplaySource.BY_BLOCK.add(ModBlocks.OXYGEN_DETECTOR.get(), OXYGEN_STATUS.get());
    }

    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        BlockState state = context.level().getBlockState(context.getSourcePos());
        if (!state.is(ModBlocks.OXYGEN_DETECTOR.get())) {
            return EMPTY_LINE;
        }
        return statusLine(state.getValue(OxygenDetectorBlock.DETECTING));
    }

    /**
     * "Oxygen" followed by a green check mark or a red cross - the colours show wherever the display target
     * renders text styling (signs, lecterns, ...); a display board shows the symbols in its own colour. Also
     * what the detector's ponder scene puts on its display board.
     */
    public static MutableComponent statusLine(boolean detecting) {
        return Component.translatable("createastronautics.display_source.oxygen_status.label").append(" ")
                .append(detecting
                        ? Component.literal("\u2714").withStyle(ChatFormatting.GREEN) // heavy check mark
                        : Component.literal("\u2718").withStyle(ChatFormatting.RED)); // heavy ballot X
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }

    // Create's default is 100 ticks - a status readout that lags 5 seconds behind an airlock opening is too slow.
    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
