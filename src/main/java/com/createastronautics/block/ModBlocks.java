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
}
