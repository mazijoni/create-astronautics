package com.createastronautics;

import com.createastronautics.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The creative tab every planet's terrain and building blocks live in, kept apart from the machines and
 * gear in the shared Create Simulated tab (see {@link CreateAstronauticsSection}). Laid out the way that
 * tab is: one titled banner per category with its blocks underneath - here, one category per world you can
 * stand on.
 *
 * Simulated's section system only works inside its own tab, so this one does the same thing itself. A
 * creative tab is just a flat list of stacks shown nine to a row, so the layout is built out of empty
 * stacks: a full empty row where each banner goes, and enough to finish off each category's last row so the
 * next one starts on a fresh line (see {@link #layout}; a tab can't normally hold empty stacks, which is
 * what {@code CreativeModeTabMixin} is for). The banners are then drawn over those empty rows by
 * {@code CreativeModeInventoryScreenMixin}, using {@link #bannerRow} - sprite, title plate, two-tone text
 * and all positioned exactly as Simulated draws its own, so the two tabs look like one set.
 */
public final class PlanetBlocksTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateAstronautics.MODID);

    public static final int COLUMNS = 9;

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("planet_blocks",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createastronautics.planet_blocks"))
                    .icon(() -> new ItemStack(ModItems.LUNAR_STONE.get()))
                    .displayItems((parameters, output) -> {
                        for (Planet planet : Planet.values()) {
                            planet.items().forEach(output::accept);
                        }
                    })
                    .build());

    private PlanetBlocksTab() {}

    /**
     * Every solid world there is to walk on, in the order they're listed in the tab: the one with blocks
     * first, then the rest of the inner solar system outwards from the Sun. A planet with nothing in it yet
     * still gets its banner, over an empty row, so it's obvious where its blocks will go.
     */
    public enum Planet {
        MOON("moon", () -> List.of(
                ModItems.MOON_DUST.get(),
                ModItems.DEAD_GRASS.get(),
                ModItems.LUNAR_GRAVEL.get(),
                ModItems.LUNAR_STONE.get(),
                ModItems.LUNAR_COBBLESTONE.get(),
                ModItems.LUNAR_ANDESITE.get(),
                ModItems.MOON_SLATE.get(),
                ModItems.MOON_COBBLED_SLATE.get(),
                ModItems.MOONSTONE_BLOCK.get(),
                ModItems.POINTED_MOONSTONE.get(),
                ModItems.MOON_COAL_ORE.get(),
                ModItems.MOON_SLATE_COAL_ORE.get(),
                ModItems.MOON_IRON_ORE.get(),
                ModItems.MOON_SLATE_IRON_ORE.get(),
                ModItems.MOON_CRYSTALLITE_ORE.get(),
                ModItems.MOON_SLATE_CRYSTALLITE_ORE.get())),
        MERCURY("mercury", List::of),
        VENUS("venus", List::of),
        MARS("mars", List::of);

        private final String name;
        private final ResourceLocation bannerSprite;
        private final Supplier<List<Item>> items;

        Planet(String name, Supplier<List<Item>> items) {
            this.name = name;
            this.bannerSprite = CreateAstronautics.id("planet_banner/" + name);
            this.items = items;
        }

        public Component title() {
            return Component.translatable("createastronautics.planet_blocks." + name);
        }

        /**
         * The banner's background, as a GUI sprite: {@code textures/gui/sprites/planet_banner/<name>.png},
         * 162x18 per frame. Its {@code .mcmeta} already declares it an animation, so stacking more frames
         * under the first is all it takes to animate it - it then plays while the banner is hovered, like
         * the Simulated tab's own.
         */
        public ResourceLocation bannerSprite() {
            return bannerSprite;
        }

        public List<Item> items() {
            return items.get();
        }

        private int itemRows() {
            // An empty category still takes up one (empty) row under its banner.
            return Math.max(1, (items().size() + COLUMNS - 1) / COLUMNS);
        }
    }

    /** @return the tab's contents as they're actually shown: banner rows and row padding included, as empty stacks. */
    public static List<ItemStack> layout() {
        List<ItemStack> stacks = new ArrayList<>();
        for (Planet planet : Planet.values()) {
            for (int i = 0; i < COLUMNS; i++) {
                stacks.add(ItemStack.EMPTY);
            }
            for (Item item : planet.items()) {
                stacks.add(new ItemStack(item));
            }
            while (stacks.size() % COLUMNS != 0) {
                stacks.add(ItemStack.EMPTY);
            }
            if (planet.items().isEmpty()) {
                for (int i = 0; i < COLUMNS; i++) {
                    stacks.add(ItemStack.EMPTY);
                }
            }
        }
        return stacks;
    }

    /** @return which row of {@link #layout} this planet's banner sits on. */
    public static int bannerRow(Planet planet) {
        int row = 0;
        for (Planet earlier : Planet.values()) {
            if (earlier == planet) {
                break;
            }
            row += 1 + earlier.itemRows();
        }
        return row;
    }

    public static boolean is(CreativeModeTab tab) {
        return TAB.isBound() && TAB.get() == tab;
    }
}
