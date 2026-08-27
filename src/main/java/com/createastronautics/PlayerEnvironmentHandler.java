package com.createastronautics;

import com.createastronautics.block.ModBlocks;
import com.createastronautics.block.OxygenFanRegistry;
import com.createastronautics.fluid.ModDataComponents;
import com.createastronautics.item.ModItems;
import com.createastronautics.magnetic.MagneticBootsNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDrownEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * Handles the "no atmosphere" survival aspects of the Moon and Deep Space: reduced/zero gravity, fall
 * damage scaled to match, and suffocating (same as being underwater, minus the bubble particles that make
 * no sense in a vacuum) unless the player is wearing the complete space suit AND its chestplate still has
 * oxygen in its tank - the tank drains while both of those conditions hold (see {@link #onPlayerTick}), at
 * a fixed rate of 1 bucket per 5 real-time minutes, independent of the tank's capacity.
 *
 * It also handles the vacuum's effect on fire and open liquids (see {@link #extinguishNearby}): water
 * flash-freezes into ice, lava cools into obsidian, open flame is snuffed out instantly, and regular
 * torches burn out - soul torches, campfires, and candles keep going, on the theory that they don't need
 * ambient air the way an open flame or a burning splinter does. A fire-extinguishing sound plays once per
 * player per sweep if anything actually went out, rather than once per block - a single sweep can catch
 * several torches/campfires at once.
 *
 * All of that is suspended for a player standing inside a sealed room an oxygen fan is supplying - checked
 * live against the player's actual current position via {@link OxygenFanRegistry} (no per-player caching,
 * so it's correct instantly and for any number of players) - since that's meant to be a real breathable
 * pocket of air: no suffocation, no suit oxygen drain, and nothing in it freezes or burns out.
 */
@EventBusSubscriber(modid = CreateAstronautics.MODID)
public class PlayerEnvironmentHandler {
    private static final ResourceLocation GRAVITY_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "planetary_gravity");
    private static final ResourceLocation FALL_DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "planetary_fall_damage");

    // Fraction subtracted from normal gravity: -1.0 = weightless, -5.0/6.0 = 1/6 gravity, like the real Moon.
    private static final double DEEP_SPACE_GRAVITY_FACTOR = -1.0;
    private static final double MOON_GRAVITY_FACTOR = -5.0 / 6.0;

    // Fall damage is cancelled outright anywhere gravity is reduced, rather than merely scaled down to
    // match the gravity factor - a fall that's only survivable because gravity is 1/6 normal still shouldn't
    // hurt at 1/6 normal damage.
    private static final double FALL_DAMAGE_CANCEL_FACTOR = -1.0;

    // How close to a floor/ship deck counts as "standing on it" for restoring normal gravity in Deep Space -
    // tighter than the magnetic boots' own reach, since this is passive ambient gravity, not an active pull.
    private static final double GROUNDED_GRAVITY_TOLERANCE = 0.5;

    // 1 bucket (1000 mB) lasts 5 real-time minutes (6000 ticks) -> 1 mB every 6 ticks, regardless of the
    // chestplate's total capacity.
    private static final int OXYGEN_DRAIN_INTERVAL_TICKS = 6;
    private static final int OXYGEN_DRAIN_AMOUNT_MB = 1;

    // Extinguishing/freezing is swept near each player rather than across the whole level, since there's no
    // cheap way to enumerate every fire/water/lava/torch position in loaded chunks - this bounds the cost to
    // a modest volume, checked a few times a second rather than every tick.
    private static final int EXTINGUISH_INTERVAL_TICKS = 10;
    private static final int EXTINGUISH_HORIZONTAL_RADIUS = 12;
    private static final int EXTINGUISH_VERTICAL_RADIUS = 8;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        Double factor = appliedGravityFactor(player);

        applyOrClear(player.getAttribute(Attributes.GRAVITY), GRAVITY_MODIFIER_ID, factor);
        applyOrClear(player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER), FALL_DAMAGE_MODIFIER_ID,
                factor == null ? null : FALL_DAMAGE_CANCEL_FACTOR);

        boolean noAtmosphere = gravityFactorFor(player.level().dimension()) != null;
        if (noAtmosphere && isWearingFullSpaceSuit(player) && !player.isCreative() && !player.isSpectator()
                && !OxygenFanRegistry.isOxygenated(player.level(), player.blockPosition())
                && player.tickCount % OXYGEN_DRAIN_INTERVAL_TICKS == 0) {
            drainOxygen(player, OXYGEN_DRAIN_AMOUNT_MB);
        }
    }

    /**
     * The gravity attribute modifier to actually apply - unlike {@link #gravityFactorFor}, this restores
     * normal gravity in Deep Space whenever the magnetic boots toggle is on (see
     * {@link MagneticBootsNetworkHandler#isActive}) and there's a floor or ship deck within reach (see
     * {@link SupportSurface}). With the boots off, this does nothing extra at all - just the plain
     * weightlessness below, same as if magnetic boots didn't exist. The Moon keeps its constant 1/6 gravity
     * regardless - it already has ambient gravity everywhere, floor or not, and isn't tied to the boots.
     */
    private static Double appliedGravityFactor(Player player) {
        if (player.level().dimension() == ModDimensions.DEEP_SPACE) {
            boolean magnetActive = MagneticBootsNetworkHandler.isActive(player) && MagneticBootsNetworkHandler.canActivate(player);
            boolean grounded = magnetActive && SupportSurface.isNear(player, GROUNDED_GRAVITY_TOLERANCE);
            return grounded ? null : DEEP_SPACE_GRAVITY_FACTOR;
        }
        return gravityFactorFor(player.level().dimension());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || gravityFactorFor(level.dimension()) == null) {
            return;
        }

        extinguishNearby(level);
    }

    /**
     * There's no atmosphere for weather to happen in - but rain/thunder isn't actually per-dimension state
     * to begin with: every non-Overworld level's weather data is a {@code DerivedLevelData} that reads
     * straight through to the Overworld's (see {@code ServerLevel#serverLevelData}), and every one of its
     * setters (raining, thundering, the timers, {@code ServerLevel#setWeatherParameters}, all of it) is a
     * silent no-op. So {@code isRaining()}/{@code isThundering()} always report exactly the Overworld's
     * current weather on the Moon and in Deep Space too, and there's no way to independently clear that -
     * every actual weather effect has to be blocked some other way instead:
     * <ul>
     *     <li>The falling-snow/rain visual ({@code LevelRenderer#renderSnowAndRain}/{@code tickRain}) gates
     *     per-position on {@code Biome#hasPrecipitation}, which the Moon and Deep Space biomes set false.</li>
     *     <li>Snow/ice actually forming on the ground ({@code ServerLevel#tickPrecipitation}) ignores
     *     {@code hasPrecipitation} entirely and only checks {@code Biome#shouldSnow}/{@code shouldFreeze} -
     *     "cold enough" (Mojira MC-248212, a real vanilla gap) - so the Moon's biome temperature is raised
     *     just past that threshold instead, same as Deep Space's already-warm {@code minecraft:the_void}.</li>
     *     <li>Lightning has no biome gate at all (real deserts get struck too), so it's cancelled here.</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LightningBolt && gravityFactorFor(event.getLevel().dimension()) != null) {
            event.setCanceled(true);
        }
    }

    /**
     * Sweeps a box around each player for anything the vacuum wouldn't allow: open flame and regular
     * torches are snuffed out (torches leave behind {@link ModBlocks#BURNT_TORCH}/{@code BURNT_WALL_TORCH}
     * rather than just vanishing), regular campfires and candles are blown out (just flipped unlit, same as
     * vanilla's own "put out with water/shovel" behaviour - the block stays, only the flame/light/particles
     * stop), and standing water/lava flash-freezes/cools since there's nothing to hold it as a liquid. Soul
     * torches and soul campfires are left alone, on the theory that soul fire doesn't need ambient air the
     * way a normal flame does.
     */
    private static void extinguishNearby(ServerLevel level) {
        if (level.getGameTime() % EXTINGUISH_INTERVAL_TICKS != 0) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (Player player : level.players()) {
            BlockPos center = player.blockPosition();
            boolean extinguishedAnything = false;

            for (int dx = -EXTINGUISH_HORIZONTAL_RADIUS; dx <= EXTINGUISH_HORIZONTAL_RADIUS; dx++) {
                for (int dy = -EXTINGUISH_VERTICAL_RADIUS; dy <= EXTINGUISH_VERTICAL_RADIUS; dy++) {
                    for (int dz = -EXTINGUISH_HORIZONTAL_RADIUS; dz <= EXTINGUISH_HORIZONTAL_RADIUS; dz++) {
                        pos.setWithOffset(center, dx, dy, dz);
                        // Checked per swept position, not just the player's own - otherwise stepping just
                        // outside a sealed room re-enables the sweep and its wide radius reaches straight
                        // back into the still-oxygenated room behind them.
                        if (OxygenFanRegistry.isOxygenated(level, pos)) {
                            continue;
                        }
                        if (extinguishIfNeeded(level, pos)) {
                            extinguishedAnything = true;
                        }
                    }
                }
            }

            // One sound per player per sweep rather than one per extinguished block - a sweep can catch
            // several torches/campfires at once (e.g. the moment someone first steps into a vacuum area),
            // and playing a separate sound for each would spam the same way multiple fans announcing the
            // same seal break did.
            if (extinguishedAnything) {
                level.playSound(null, center, SoundEvents.FIRE_EXTINGUISH, SoundSource.AMBIENT, 1.0F, 1.0F);
            }
        }
    }

    private static boolean extinguishIfNeeded(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.FIRE) {
            level.removeBlock(pos, false);
            return true;
        } else if (block == Blocks.TORCH) {
            level.setBlock(pos, ModBlocks.BURNT_TORCH.get().defaultBlockState(), Block.UPDATE_ALL);
            return true;
        } else if (block == Blocks.WALL_TORCH) {
            Direction facing = state.getValue(WallTorchBlock.FACING);
            level.setBlock(pos, ModBlocks.BURNT_WALL_TORCH.get().defaultBlockState().setValue(WallTorchBlock.FACING, facing), Block.UPDATE_ALL);
            return true;
        } else if (block == Blocks.CAMPFIRE && state.getValue(BlockStateProperties.LIT)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
            return true;
        } else if (block instanceof AbstractCandleBlock && state.getValue(BlockStateProperties.LIT)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
            return true;
        } else if (block == Blocks.WATER) {
            level.setBlock(pos, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
        } else if (block == Blocks.LAVA) {
            level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
        }
        return false;
    }

    private static void applyOrClear(AttributeInstance attribute, ResourceLocation modifierId, Double factor) {
        if (attribute == null) {
            return;
        }
        if (factor == null) {
            attribute.removeModifier(modifierId);
        } else {
            attribute.addOrUpdateTransientModifier(new AttributeModifier(modifierId, factor, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    @SubscribeEvent
    public static void onLivingBreathe(LivingBreatheEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Creative/spectator players aren't actually in danger, so let their air supply regenerate normally
        // instead of endlessly ticking down.
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        boolean noAtmosphere = gravityFactorFor(player.level().dimension()) != null;
        if (noAtmosphere && !(isWearingFullSpaceSuit(player) && hasOxygenRemaining(player))
                && !OxygenFanRegistry.isOxygenated(player.level(), BlockPos.containing(player.getEyePosition()))) {
            event.setCanBreathe(false);
        }
    }

    @SubscribeEvent
    public static void onLivingDrown(LivingDrownEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // No water means no bubbles floating up when suffocating - keep the damage, drop the particles.
        if (gravityFactorFor(player.level().dimension()) != null) {
            event.setBubbleCount(0);
        }
    }

    private static boolean isWearingFullSpaceSuit(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BRASS_SPACE_SUIT_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BRASS_SPACE_SUIT_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.BRASS_SPACE_SUIT_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BRASS_SPACE_SUIT_BOOTS.get());
    }

    private static boolean hasOxygenRemaining(Player player) {
        return !oxygenContent(player.getItemBySlot(EquipmentSlot.CHEST)).isEmpty();
    }

    private static void drainOxygen(Player player, int amountMb) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        FluidStack fluid = oxygenContent(chest).copy();
        if (fluid.isEmpty()) {
            return;
        }

        fluid.shrink(amountMb);
        if (fluid.isEmpty()) {
            chest.remove(ModDataComponents.OXYGEN_CONTENT.get());
        } else {
            chest.set(ModDataComponents.OXYGEN_CONTENT.get(), SimpleFluidContent.copyOf(fluid));
        }
    }

    private static FluidStack oxygenContent(ItemStack chest) {
        return chest.getOrDefault(ModDataComponents.OXYGEN_CONTENT.get(), SimpleFluidContent.EMPTY).copy();
    }

    private static Double gravityFactorFor(ResourceKey<Level> dimension) {
        if (dimension == ModDimensions.DEEP_SPACE) {
            return DEEP_SPACE_GRAVITY_FACTOR;
        } else if (dimension == ModDimensions.MOON) {
            return MOON_GRAVITY_FACTOR;
        }
        return null;
    }
}
