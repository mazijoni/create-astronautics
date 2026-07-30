package com.createastronautics;

import com.createastronautics.worldgen.feature.ModFeatures;
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
        ModFeatures.FEATURES.register(modEventBus);
    }
}
