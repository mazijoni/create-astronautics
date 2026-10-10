package com.createastronautics;

import com.createastronautics.item.ModItems;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Create Astronautics doesn't register a creative tab of its own - Aeronautics doesn't either. Both pool
 * into the shared "Create Simulated" tab (registry id {@code simulated:group}) that the underlying
 * Simulated framework owns, organized into labelled sections (Simulated itself, Aeronautics, Offroad, ...)
 * rather than separate tabs. Our own items join that tab as an "Astronautics" section
 * (assets/createastronautics/simulated/sections/astronautics.json) the same way, by registering directly
 * into {@link SimulatedRegistrate}'s static bookkeeping rather than through its {@code Registrate} builder
 * pipeline - our items are already registered via a plain {@code DeferredRegister}, so there's nothing to
 * gain from routing registration itself through Simulated's Registrate subclass.
 */
public class CreateAstronauticsSection {
    private static final ResourceLocation SECTION_ID = ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "astronautics");

    public static void register() {
        addItem(ModItems.MOON_DUST.get());
        addItem(ModItems.BURNT_TORCH.get());
        addItem(ModItems.OXYGEN_FAN.get());
        addItem(ModItems.OXYGEN_BUCKET.get());
        addItem(ModItems.MOONSTONE_BLOCK.get());
        addItem(ModItems.POINTED_MOONSTONE.get());
        addItem(ModItems.LUNAR_STONE.get());
        addItem(ModItems.LUNAR_COBBLESTONE.get());
        addItem(ModItems.MOON_SLATE.get());
        addItem(ModItems.MOON_COBBLED_SLATE.get());
        addItem(ModItems.LUNAR_GRAVEL.get());
        addItem(ModItems.LUNAR_ANDESITE.get());
        addItem(ModItems.MOON_COAL_ORE.get());
        addItem(ModItems.MOON_SLATE_COAL_ORE.get());
        addItem(ModItems.MOON_IRON_ORE.get());
        addItem(ModItems.MOON_SLATE_IRON_ORE.get());
        addItem(ModItems.MOON_ASTRIUM_ORE.get());
        addItem(ModItems.MOON_SLATE_ASTRIUM_ORE.get());
        addItem(ModItems.ASTRIUM.get());
        addItem(ModItems.BRASS_SPACE_SUIT_HELMET.get());
        addItem(ModItems.BRASS_SPACE_SUIT_CHESTPLATE.get());
        addItem(ModItems.BRASS_SPACE_SUIT_LEGGINGS.get());
        addItem(ModItems.BRASS_SPACE_SUIT_BOOTS.get());
        addItem(ModItems.SOLID_ROCKET_BOOSTER.get());
    }

    private static void addItem(Item item) {
        SimulatedRegistrate.TAB_ITEMS.add(() -> item);
        SimulatedRegistrate.ITEM_TO_SECTION.put(BuiltInRegistries.ITEM.getKey(item), SECTION_ID);
    }
}
