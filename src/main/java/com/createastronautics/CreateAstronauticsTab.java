package com.createastronautics;

import com.createastronautics.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CreateAstronauticsTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateAstronautics.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATE_ASTRONAUTICS_TAB = CREATIVE_MODE_TABS.register("create_astronautics",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createastronautics"))
                    .icon(() -> ModItems.BRASS_SPACE_SUIT_HELMET.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.MOON_DUST.get());
                        output.accept(ModItems.SOLID_ROCKET_BOOSTER.get());
                        output.accept(ModItems.BRASS_SPACE_SUIT_HELMET.get());
                        output.accept(ModItems.BRASS_SPACE_SUIT_CHESTPLATE.get());
                        output.accept(ModItems.BRASS_SPACE_SUIT_LEGGINGS.get());
                        output.accept(ModItems.BRASS_SPACE_SUIT_BOOTS.get());
                    })
                    .build());
}
