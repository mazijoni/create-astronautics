package com.createastronautics.item;

import com.createastronautics.CreateAstronautics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, CreateAstronautics.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BRASS_SPACE_SUIT = ARMOR_MATERIALS.register("brass_space_suit", () -> {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 2);
        defense.put(ArmorItem.Type.LEGGINGS, 5);
        defense.put(ArmorItem.Type.CHESTPLATE, 6);
        defense.put(ArmorItem.Type.HELMET, 2);
        defense.put(ArmorItem.Type.BODY, 6);

        return new ArmorMaterial(
                defense,
                9,
                SoundEvents.ARMOR_EQUIP_IRON,
                () -> Ingredient.of(Items.IRON_INGOT),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "brass_space_suit"))),
                0.0F,
                0.0F);
    });
}
