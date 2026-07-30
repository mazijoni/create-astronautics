package com.createastronautics;

import com.createastronautics.item.ModArmorMaterials;
import com.createastronautics.item.ModItems;
import com.createastronautics.worldgen.feature.ModFeatures;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(CreateAstronautics.MODID)
public class CreateAstronautics {
    public static final String MODID = "createastronautics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateAstronautics(IEventBus modEventBus, ModContainer modContainer) {
        ModFeatures.FEATURES.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.IRON_SPACE_SUIT_HELMET);
            event.accept(ModItems.IRON_SPACE_SUIT_CHESTPLATE);
            event.accept(ModItems.IRON_SPACE_SUIT_LEGGINGS);
            event.accept(ModItems.IRON_SPACE_SUIT_BOOTS);
        }
    }
}
