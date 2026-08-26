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

    public static final DeferredItem<BlockItem> MOON_DUST = ITEMS.register("moon_dust",
            () -> new BlockItem(ModBlocks.MOON_DUST.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> SOLID_ROCKET_BOOSTER = ITEMS.register("solid_rocket_booster",
            () -> new BlockItem(ModBlocks.SOLID_ROCKET_BOOSTER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> OXYGEN_FAN = ITEMS.register("oxygen_fan",
            () -> new BlockItem(ModBlocks.OXYGEN_FAN.get(), new Item.Properties()));

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
