package com.createastronautics.block;

import com.createastronautics.CreateAstronautics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateAstronautics.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolidRocketBoosterBlockEntity>> SOLID_ROCKET_BOOSTER = BLOCK_ENTITIES.register("solid_rocket_booster",
            () -> BlockEntityType.Builder.of(SolidRocketBoosterBlockEntity::new, ModBlocks.SOLID_ROCKET_BOOSTER.get()).build(null));
}
