package com.createastronautics.block;

import com.createastronautics.CreateAstronautics;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
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

    // Same properties as vanilla's own dripstone_block - a plain solid stone-tier block.
    public static final DeferredBlock<Block> MOONSTONE_BLOCK = BLOCKS.register("moonstone_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .sound(SoundType.DRIPSTONE_BLOCK)
                    .strength(1.5F)));

    public static final DeferredBlock<PointedMoonstoneBlock> POINTED_MOONSTONE = BLOCKS.register("pointed_moonstone",
            () -> new PointedMoonstoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .sound(SoundType.DRIPSTONE_BLOCK)
                    .strength(1.5F)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    // The Moon's own base rock, replacing plain minecraft:stone in its terrain - same properties and the
    // same silk-touch-else-cobblestone drop rule as vanilla stone (see loot_table/blocks/lunar_stone.json).
    public static final DeferredBlock<Block> LUNAR_STONE = BLOCKS.register("lunar_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.STONE)
                    .strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> LUNAR_COBBLESTONE = BLOCKS.register("lunar_cobblestone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.STONE)
                    .strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops()));

    // RotatedPillarBlock is the same plain vanilla class minecraft:deepslate itself uses - no custom
    // behaviour needed, just the "axis" property and column texture split (see its blockstate/model).
    public static final DeferredBlock<RotatedPillarBlock> MOON_SLATE = BLOCKS.register("moon_slate",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .sound(SoundType.DEEPSLATE)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> MOON_COBBLED_SLATE = BLOCKS.register("moon_cobbled_slate",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .sound(SoundType.DEEPSLATE)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()));

    // Falls and has a chance to drop flint instead of itself, same as vanilla gravel - the drop chance is
    // purely loot-table-driven (see loot_table/blocks/lunar_gravel.json), not block-class behaviour.
    public static final DeferredBlock<LunarGravelBlock> LUNAR_GRAVEL = BLOCKS.register("lunar_gravel",
            () -> new LunarGravelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.GRAVEL)
                    .strength(0.6F)));

    // Unlike the other lunar_* variants, this deliberately does NOT behave like its overworld counterpart:
    // it's raw moon rock that only becomes usable minecraft:andesite once washed in a Create basin under a
    // water wheel (see data/createastronautics/recipe/splashing/lunar_andesite.json) - so it always drops
    // itself, with no loot table trickery standing in for the real andesite.
    public static final DeferredBlock<Block> LUNAR_ANDESITE = BLOCKS.register("lunar_andesite",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.STONE)
                    .strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops()));

    // Coal ore, matching vanilla coal_ore/deepslate_coal_ore exactly (same hardness, same 0-2 XP drop).
    public static final DeferredBlock<DropExperienceBlock> MOON_COAL_ORE = BLOCKS.register("moon_coal_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 2), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.STONE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<DropExperienceBlock> MOON_SLATE_COAL_ORE = BLOCKS.register("moon_slate_coal_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 2), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .sound(SoundType.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops()));

    // Iron ore, requiring a stone pickaxe like vanilla iron_ore (see tags/block/needs_stone_tool.json) -
    // drops raw_iron via loot_table/blocks/moon_iron_ore.json, no block class needed since the raw-material
    // drop is entirely loot-table-driven, same as vanilla's own iron_ore.
    public static final DeferredBlock<Block> MOON_IRON_ORE = BLOCKS.register("moon_iron_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.STONE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> MOON_SLATE_IRON_ORE = BLOCKS.register("moon_slate_iron_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .sound(SoundType.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops()));

    // Astrium requires an iron pickaxe (see tags/block/needs_iron_tool.json) and always drops exactly one
    // raw astrium per ore, with no fortune bonus roll (see loot_table/blocks/moon_astrium_ore.json). XP
    // drop matches vanilla diamond_ore, and it glows like a light source befitting a rare crystal.
    public static final DeferredBlock<DropExperienceBlock> MOON_ASTRIUM_ORE = BLOCKS.register("moon_astrium_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.STONE)
                    .strength(3.0F, 3.0F)
                    .lightLevel(state -> 10)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<DropExperienceBlock> MOON_SLATE_ASTRIUM_ORE = BLOCKS.register("moon_slate_astrium_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .sound(SoundType.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .lightLevel(state -> 10)
                    .requiresCorrectToolForDrops()));
}
