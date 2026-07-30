package com.createastronautics.item;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.block.ModBlocks;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateAstronautics.MODID);

    public static final DeferredItem<BlockItem> MOON_DUST = ITEMS.register("moon_dust",
            () -> new BlockItem(ModBlocks.MOON_DUST.get(), new Item.Properties()));

    public static final DeferredItem<ArmorItem> IRON_SPACE_SUIT_HELMET = ITEMS.register("iron_space_suit_helmet",
            () -> new ArmorItem(ModArmorMaterials.IRON_SPACE_SUIT, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15))));

    public static final DeferredItem<ArmorItem> IRON_SPACE_SUIT_CHESTPLATE = ITEMS.register("iron_space_suit_chestplate",
            () -> new ArmorItem(ModArmorMaterials.IRON_SPACE_SUIT, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(15))));

    public static final DeferredItem<ArmorItem> IRON_SPACE_SUIT_LEGGINGS = ITEMS.register("iron_space_suit_leggings",
            () -> new ArmorItem(ModArmorMaterials.IRON_SPACE_SUIT, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(15))));

    public static final DeferredItem<ArmorItem> IRON_SPACE_SUIT_BOOTS = ITEMS.register("iron_space_suit_boots",
            () -> new ArmorItem(ModArmorMaterials.IRON_SPACE_SUIT, ArmorItem.Type.BOOTS, new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(15))));
}
