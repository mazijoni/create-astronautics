package com.createastronautics;

import com.createastronautics.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Two tabs rather than one: {@link #CREATE_ASTRONAUTICS_TAB} keeps its original position and holds the
 * world-decoration items (moon dust, the burnt torch) that make sense sitting wherever a "Create
 * Astronautics" tab naturally lands, while {@link #ASTRONAUTICS_TAB} holds the actual gear (the oxygen fan,
 * the space suit, the booster) and is chained directly onto Aeronautics' own tab via {@code withTabsBefore}
 * - the same mechanism Create itself uses to keep its own tabs (palettes, base, ...) adjacent to one another
 * rather than scattered wherever they'd otherwise sort.
 *
 * Aeronautics doesn't register a creative tab of its own, though - decompiling it shows its items (and its
 * sibling addons') all pool into one shared tab owned by the underlying "Simulated" framework it and Create
 * Aeronautics are built on: registry id {@code simulated:group}, titled "Create Simulated" (not
 * "Aeronautics" - see {@code itemGroup.simulated.group} in that mod's own lang file). That class isn't on
 * the compile classpath (jar-in-jar, same as the other Aeronautics classes referenced elsewhere in this
 * mod), so the key is built by hand from the same namespace/path rather than importing it directly.
 */
public class CreateAstronauticsTab {
    private static final ResourceKey<CreativeModeTab> AERONAUTICS_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("simulated", "group"));

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateAstronautics.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATE_ASTRONAUTICS_TAB = CREATIVE_MODE_TABS.register("create_astronautics",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createastronautics"))
                    .icon(() -> ModItems.MOON_DUST.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.MOON_DUST.get());
                        output.accept(ModItems.BURNT_TORCH.get());
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ASTRONAUTICS_TAB = CREATIVE_MODE_TABS.register("astronautics",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createastronautics.astronautics"))
                    .withTabsBefore(AERONAUTICS_TAB_KEY)
                    .icon(() -> ModItems.BRASS_SPACE_SUIT_HELMET.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.OXYGEN_FAN.get());
                        output.accept(ModItems.OXYGEN_BUCKET.get());
                        output.accept(ModItems.BRASS_SPACE_SUIT_HELMET.get());
                        output.accept(ModItems.BRASS_SPACE_SUIT_CHESTPLATE.get());
                        output.accept(ModItems.BRASS_SPACE_SUIT_LEGGINGS.get());
                        output.accept(ModItems.BRASS_SPACE_SUIT_BOOTS.get());
                        output.accept(ModItems.SOLID_ROCKET_BOOSTER.get());
                    })
                    .build());
}
