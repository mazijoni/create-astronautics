package com.createastronautics.block;

import com.createastronautics.CreateAstronautics;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
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

    // Crystallite was called astrium up to 0.2.0 - keeps ore already generated in existing worlds.
    static {
        BLOCKS.addAlias(CreateAstronautics.id("moon_astrium_ore"), CreateAstronautics.id("moon_crystallite_ore"));
        BLOCKS.addAlias(CreateAstronautics.id("moon_slate_astrium_ore"), CreateAstronautics.id("moon_slate_crystallite_ore"));
    }

    public static final DeferredBlock<MoonDustBlock> MOON_DUST = BLOCKS.register("moon_dust",
            () -> new MoonDustBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.SAND)
                    .strength(0.5F)));

    // What grass_block turns into out in a vacuum (see PlayerEnvironmentHandler#extinguishIfNeeded). Same
    // hardness, sound and silk-touch-else-dirt drop as grass_block (see loot_table/blocks/dead_grass.json), and
    // still soil plants can be put on (it's in #minecraft:dirt) - but a plain block, so it never spreads and
    // bone meal does nothing to it.
    public static final DeferredBlock<Block> DEAD_GRASS = BLOCKS.register("dead_grass",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .sound(SoundType.GRASS)
                    .strength(0.6F)));

    public static final DeferredBlock<SolidRocketBoosterBlock> SOLID_ROCKET_BOOSTER = BLOCKS.register("solid_rocket_booster",
            () -> new SolidRocketBoosterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .strength(3.5F)
                    // Needs an iron pickaxe or better to drop (see tags/block/needs_iron_tool.json).
                    .requiresCorrectToolForDrops()
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

    // Matches Create's own pulse repeater block properties (instabreak, pops off when pushed).
    public static final DeferredBlock<RedstoneRegulatorBlock> REDSTONE_REGULATOR = BLOCKS.register("redstone_regulator",
            () -> new RedstoneRegulatorBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.DESTROY)));

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

    // Same properties as vanilla's own dripstone_block - a plain solid stone-tier block that only drops to a
    // pickaxe.
    public static final DeferredBlock<Block> MOONSTONE_BLOCK = BLOCKS.register("moonstone_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .sound(SoundType.DRIPSTONE_BLOCK)
                    .strength(1.5F, 1.0F)
                    .requiresCorrectToolForDrops()));

    // Same hardness/blast resistance and sound as vanilla's own pointed_dripstone, which drops to any tool.
    public static final DeferredBlock<PointedMoonstoneBlock> POINTED_MOONSTONE = BLOCKS.register("pointed_moonstone",
            () -> new PointedMoonstoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .sound(SoundType.POINTED_DRIPSTONE)
                    .strength(1.5F, 3.0F)
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

    // Crystallite requires a diamond pickaxe (see tags/block/needs_diamond_tool.json) and always drops exactly one
    // raw crystallite per ore, with no fortune bonus roll (see loot_table/blocks/moon_crystallite_ore.json). XP
    // drop matches vanilla diamond_ore, and it glows like a light source befitting a rare crystal.
    public static final DeferredBlock<DropExperienceBlock> MOON_CRYSTALLITE_ORE = BLOCKS.register("moon_crystallite_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.STONE)
                    .strength(3.0F, 3.0F)
                    .lightLevel(state -> 10)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<DropExperienceBlock> MOON_SLATE_CRYSTALLITE_ORE = BLOCKS.register("moon_slate_crystallite_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .sound(SoundType.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .lightLevel(state -> 10)
                    .requiresCorrectToolForDrops()));

    // Create's own casing block class (connected textures, see TitaniumCasingModels), so a wrench does nothing
    // to it and sneak-wrenching picks it up, like Create's casings - but unlike their wooden-core ones it's solid
    // metal, so it takes an iron pickaxe to mine (see tags/block/needs_iron_tool.json) and has iron_block's
    // hardness and sound.
    public static final DeferredBlock<CasingBlock> TITANIUM_CASING = BLOCKS.register("titanium_casing",
            () -> new CasingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()));

    // Same properties as vanilla's own observer - and like it, it never conducts the signal it puts out. Its
    // indicator glows dimly either way: red with no oxygen, light blue with it, under a colored-lighting mod
    // (see assets/createastronautics/light/emitters.json).
    public static final DeferredBlock<OxygenDetectorBlock> OXYGEN_DETECTOR = BLOCKS.register("oxygen_detector",
            () -> new OxygenDetectorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.STONE)
                    .strength(3.0F)
                    .lightLevel(state -> 6)
                    .requiresCorrectToolForDrops()
                    .isRedstoneConductor((state, level, pos) -> false)));

    // Titanium versions of Create's own encased shaft, cogwheels and fluid pipe: right-click a shaft, cogwheel,
    // large cogwheel or fluid pipe with Titanium Casing to encase it (see TitaniumEncasing). Built from Create's
    // own block classes, so they behave exactly like the andesite/brass/copper ones - including dropping only
    // what was encased, not the casing.
    public static final DeferredBlock<EncasedShaftBlock> TITANIUM_ENCASED_SHAFT = BLOCKS.register("titanium_encased_shaft",
            () -> new EncasedShaftBlock(encasedProperties(), TITANIUM_CASING::get));

    public static final DeferredBlock<EncasedCogwheelBlock> TITANIUM_ENCASED_COGWHEEL = BLOCKS.register("titanium_encased_cogwheel",
            () -> new EncasedCogwheelBlock(encasedProperties(), false, TITANIUM_CASING::get));

    public static final DeferredBlock<EncasedCogwheelBlock> TITANIUM_ENCASED_LARGE_COGWHEEL = BLOCKS.register("titanium_encased_large_cogwheel",
            () -> new EncasedCogwheelBlock(encasedProperties(), true, TITANIUM_CASING::get));

    public static final DeferredBlock<EncasedPipeBlock> TITANIUM_ENCASED_FLUID_PIPE = BLOCKS.register("titanium_encased_fluid_pipe",
            () -> new EncasedPipeBlock(encasedProperties(), TITANIUM_CASING::get));

    // Create's own encased blocks start from andesite's hardness with no occlusion (see its
    // BuilderTransformers#encasedBase) - only the look and sound here are titanium's.
    private static BlockBehaviour.Properties encasedProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(1.5F, 6.0F)
                .noOcclusion();
    }
}
