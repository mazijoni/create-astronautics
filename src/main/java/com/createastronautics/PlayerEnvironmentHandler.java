package com.createastronautics;

import com.createastronautics.fluid.ModDataComponents;
import com.createastronautics.item.ModItems;
import com.createastronautics.magnetic.MagneticBootsNetworkHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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

        // There's no atmosphere for weather to happen in. Vanilla still advances the weather cycle for any
        // dimension with skylight (ServerLevel#advanceWeatherCycle only gates on that, nothing else) and
        // decrements clearWeatherTime toward zero every tick regardless of what we do - reacting only once
        // rain/thunder had already flipped on left a real gap where it could start before we caught it, and
        // depending on whatever clearWeatherTime a save already had, might not get caught at all until it
        // cycled back around. Pinning the clear-weather timer back up every single tick, unconditionally,
        // means it can never reach zero in the first place.
        level.setWeatherParameters(1_000_000, 0, false, false);
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
        if (noAtmosphere && !(isWearingFullSpaceSuit(player) && hasOxygenRemaining(player))) {
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
