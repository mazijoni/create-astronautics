package com.createastronautics.space.mixin;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Ported from Create-Cosmonautics' "Dimensional Sable" module (dev.egg), itself credited there as
// "Copyright (c) 2025 KyanBirb (sable touys)" - https://github.com/CosmonauticsTeam/Create-Cosmonautics
// GPLv3-licensed; see LICENSE.
@Mixin(LevelPlot.class)
public interface LevelPlotAccessor {

    @Accessor(value = "container", remap = false)
    SubLevelContainer createastronautics$getContainer();

    @Accessor(value = "logSize", remap = false)
    int createastronautics$getLogSize();

    @Accessor(value = "biome", remap = false)
    ResourceKey<Biome> createastronautics$getBiome();

    @Accessor(value = "expandPlotIfNecessary", remap = false)
    boolean createastronautics$getExpandPlotIfNecessary();

    @Accessor(value = "expandPlotIfNecessary", remap = false)
    void createastronautics$setExpandPlotIfNecessary(boolean expandPlotIfNecessary);
}
