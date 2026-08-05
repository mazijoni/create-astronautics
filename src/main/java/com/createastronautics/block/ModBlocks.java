package com.createastronautics.block;

import com.createastronautics.CreateAstronautics;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateAstronautics.MODID);

    public static final DeferredBlock<MoonDustBlock> MOON_DUST = BLOCKS.register("moon_dust",
            () -> new MoonDustBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.SAND)
                    .strength(0.5F)));

    public static final DeferredBlock<SolidRocketBoosterBlock> SOLID_ROCKET_BOOSTER = BLOCKS.register("solid_rocket_booster",
            () -> new SolidRocketBoosterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .strength(3.5F)
                    // Matches Create's own smart chute: its model doesn't fill the block, and its texture has
                    // cutout gaps you can see through, so it must not behave like a solid full cube.
                    .noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)));
}
