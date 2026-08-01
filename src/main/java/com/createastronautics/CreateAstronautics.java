package com.createastronautics;

import com.createastronautics.block.ModBlocks;
import com.createastronautics.item.ModArmorMaterials;
import com.createastronautics.item.ModItems;
import com.createastronautics.worldgen.ModDensityFunctionTypes;
import com.createastronautics.worldgen.ModFeatures;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CreateAstronautics.MODID)
public class CreateAstronautics {
    public static final String MODID = "createastronautics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateAstronautics(IEventBus modEventBus, ModContainer modContainer) {
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModDensityFunctionTypes.DENSITY_FUNCTION_TYPES.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        CreateAstronauticsTab.CREATIVE_MODE_TABS.register(modEventBus);
    }
}
