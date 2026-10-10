package com.createastronautics.advancement;

import com.createastronautics.CreateAstronautics;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCriteriaTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES =
            DeferredRegister.create(Registries.TRIGGER_TYPE, CreateAstronautics.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, OxygenatedRoomTrigger> OXYGENATED_ROOM =
            TRIGGER_TYPES.register("oxygenated_room", OxygenatedRoomTrigger::new);
}
