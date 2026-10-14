package com.createastronautics.item;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.block.ModBlocks;
import com.createastronautics.fluid.ModFluids;
import com.createastronautics.fluid.OxygenBucketItem;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import software.bernie.geckolib.model.GeoModel;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateAstronautics.MODID);

    // Crystallite was called astrium up to 0.2.0 - keeps items already sitting in existing worlds' inventories.
    static {
        ITEMS.addAlias(CreateAstronautics.id("moon_astrium_ore"), CreateAstronautics.id("moon_crystallite_ore"));
        ITEMS.addAlias(CreateAstronautics.id("moon_slate_astrium_ore"), CreateAstronautics.id("moon_slate_crystallite_ore"));
        ITEMS.addAlias(CreateAstronautics.id("astrium"), CreateAstronautics.id("crystallite"));
    }

    public static final DeferredItem<BlockItem> MOON_DUST = ITEMS.register("moon_dust",
            () -> new BlockItem(ModBlocks.MOON_DUST.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> DEAD_GRASS = ITEMS.register("dead_grass",
            () -> new BlockItem(ModBlocks.DEAD_GRASS.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> SOLID_ROCKET_BOOSTER =ITEMS.register("solid_rocket_booster",
            () -> new BlockItem(ModBlocks.SOLID_ROCKET_BOOSTER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> OXYGEN_FAN = ITEMS.register("oxygen_fan",
            () -> new BlockItem(ModBlocks.OXYGEN_FAN.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> REDSTONE_REGULATOR = ITEMS.register("redstone_regulator",
            () -> new BlockItem(ModBlocks.REDSTONE_REGULATOR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MOONSTONE_BLOCK = ITEMS.register("moonstone_block",
            () -> new BlockItem(ModBlocks.MOONSTONE_BLOCK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> POINTED_MOONSTONE = ITEMS.register("pointed_moonstone",
            () -> new BlockItem(ModBlocks.POINTED_MOONSTONE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> LUNAR_STONE = ITEMS.register("lunar_stone",
            () -> new BlockItem(ModBlocks.LUNAR_STONE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> LUNAR_COBBLESTONE = ITEMS.register("lunar_cobblestone",
            () -> new BlockItem(ModBlocks.LUNAR_COBBLESTONE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MOON_SLATE = ITEMS.register("moon_slate",
            () -> new BlockItem(ModBlocks.MOON_SLATE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MOON_COBBLED_SLATE = ITEMS.register("moon_cobbled_slate",
            () -> new BlockItem(ModBlocks.MOON_COBBLED_SLATE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> LUNAR_GRAVEL = ITEMS.register("lunar_gravel",
            () -> new BlockItem(ModBlocks.LUNAR_GRAVEL.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> LUNAR_ANDESITE = ITEMS.register("lunar_andesite",
            () -> new BlockItem(ModBlocks.LUNAR_ANDESITE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MOON_COAL_ORE = ITEMS.register("moon_coal_ore",
            () -> new BlockItem(ModBlocks.MOON_COAL_ORE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MOON_SLATE_COAL_ORE = ITEMS.register("moon_slate_coal_ore",
            () -> new BlockItem(ModBlocks.MOON_SLATE_COAL_ORE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MOON_IRON_ORE = ITEMS.register("moon_iron_ore",
            () -> new BlockItem(ModBlocks.MOON_IRON_ORE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MOON_SLATE_IRON_ORE = ITEMS.register("moon_slate_iron_ore",
            () -> new BlockItem(ModBlocks.MOON_SLATE_IRON_ORE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MOON_CRYSTALLITE_ORE = ITEMS.register("moon_crystallite_ore",
            () -> new BlockItem(ModBlocks.MOON_CRYSTALLITE_ORE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MOON_SLATE_CRYSTALLITE_ORE = ITEMS.register("moon_slate_crystallite_ore",
            () -> new BlockItem(ModBlocks.MOON_SLATE_CRYSTALLITE_ORE.get(), new Item.Properties()));

    // Raw material dropped by moon_crystallite_ore/moon_slate_crystallite_ore - no further processing chain yet.
    public static final DeferredItem<Item> CRYSTALLITE = ITEMS.register("crystallite",
            () -> new Item(new Item.Properties()));

    // A 40% chance from crushing coal (see recipe/crushing/coal.json) - the carbon that, mixed with iron and
    // quartz under superheat, makes titanium ingots.
    public static final DeferredItem<Item> CARBON_DUST = ITEMS.register("carbon_dust",
            () -> new Item(new Item.Properties()));

    // Superheated mixing of 2 iron ingots, 1 quartz and 1 carbon dust (see recipe/mixing/titanium_ingot.json).
    public static final DeferredItem<Item> TITANIUM_INGOT = ITEMS.register("titanium_ingot",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<BlockItem> TITANIUM_CASING = ITEMS.register("titanium_casing",
            () -> new BlockItem(ModBlocks.TITANIUM_CASING.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> OXYGEN_DETECTOR = ITEMS.register("oxygen_detector",
            () -> new BlockItem(ModBlocks.OXYGEN_DETECTOR.get(), new Item.Properties()));

    // Matches vanilla's own torch item registration: attaches to the underside of a block by default,
    // falling back to a wall mount.
    public static final DeferredItem<StandingAndWallBlockItem> BURNT_TORCH = ITEMS.register("burnt_torch",
            () -> new StandingAndWallBlockItem(ModBlocks.BURNT_TORCH.get(), ModBlocks.BURNT_WALL_TORCH.get(), new Item.Properties(), Direction.DOWN));

    public static final DeferredItem<OxygenBucketItem> OXYGEN_BUCKET = ITEMS.register("oxygen_bucket",
            () -> new OxygenBucketItem(ModFluids.OXYGEN.get(), new Item.Properties()
                    .craftRemainder(Items.BUCKET)
                    .stacksTo(1)));

    public static final DeferredItem<BrassSpaceSuitArmorItem> BRASS_SPACE_SUIT_HELMET = ITEMS.register("brass_space_suit_helmet",
            () -> new BrassSpaceSuitArmorItem(ModArmorMaterials.BRASS_SPACE_SUIT, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15)), spaceSuitModel()));

    // 30 minutes of air at 1 bucket (1000 mB) = 5 minutes -> 6 buckets.
    public static final int OXYGEN_TANK_CAPACITY_MB = 6000;

    public static final DeferredItem<BrassSpaceSuitArmorItem> BRASS_SPACE_SUIT_CHESTPLATE = ITEMS.register("brass_space_suit_chestplate",
            () -> new BrassSpaceSuitArmorItem(ModArmorMaterials.BRASS_SPACE_SUIT, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(15)), spaceSuitModel(), OXYGEN_TANK_CAPACITY_MB));

    public static final DeferredItem<BrassSpaceSuitArmorItem> BRASS_SPACE_SUIT_LEGGINGS = ITEMS.register("brass_space_suit_leggings",
            () -> new BrassSpaceSuitArmorItem(ModArmorMaterials.BRASS_SPACE_SUIT, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(15)), spaceSuitModel()));

    public static final DeferredItem<BrassSpaceSuitArmorItem> BRASS_SPACE_SUIT_BOOTS = ITEMS.register("brass_space_suit_boots",
            () -> new BrassSpaceSuitArmorItem(ModArmorMaterials.BRASS_SPACE_SUIT, ArmorItem.Type.BOOTS, new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(15)), spaceSuitModel()));

    // A fresh supplier per item so each equipment slot gets its own GeoModel instance, matching GeckoLib's
    // expectation that a GeoModel isn't shared across unrelated GeoAnimatable instances.
    private static java.util.function.Supplier<GeoModel<BrassSpaceSuitArmorItem>> spaceSuitModel() {
        return BrassSpaceSuitArmorModel::new;
    }
}
