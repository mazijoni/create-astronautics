package com.createastronautics.block;

import com.createastronautics.CreateAstronautics;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
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

    // Matches Create's own encased_fan block properties (stone-tier, podzol map color).
    public static final DeferredBlock<OxygenFanBlock> OXYGEN_FAN = BLOCKS.register("oxygen_fan",
            () -> new OxygenFanBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .sound(SoundType.STONE)
                    .strength(1.25F, 4.2F)
                    .requiresCorrectToolForDrops()));

    // Same properties as vanilla's own torch, minus the light level - it's burnt out.
    public static final DeferredBlock<BurntTorchBlock> BURNT_TORCH = BLOCKS.register("burnt_torch",
            () -> new BurntTorchBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.WOOD)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<BurntWallTorchBlock> BURNT_WALL_TORCH = BLOCKS.register("burnt_wall_torch",
            () -> new BurntWallTorchBlock(BlockBehaviour.Properties.ofFullCopy(BURNT_TORCH.get())
                    .dropsLike(BURNT_TORCH.get())));
}
