package com.createastronautics;

import com.createastronautics.item.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDrownEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Handles the "no atmosphere" survival aspects of the Moon and Deep Space: reduced/zero gravity, fall
 * damage scaled to match, and suffocating (same as being underwater, minus the bubble particles that make
 * no sense in a vacuum) unless the player is wearing the complete space suit.
 */
@EventBusSubscriber(modid = CreateAstronautics.MODID)
public class PlayerEnvironmentHandler {
    private static final ResourceLocation GRAVITY_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "planetary_gravity");
    private static final ResourceLocation FALL_DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "planetary_fall_damage");

    // Fraction subtracted from normal gravity (and, to match, fall damage): -1.0 = weightless/no fall damage,
    // -5.0/6.0 = 1/6 gravity, like the real Moon.
    private static final double DEEP_SPACE_GRAVITY_FACTOR = -1.0;
    private static final double MOON_GRAVITY_FACTOR = -5.0 / 6.0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        Double factor = gravityFactorFor(player.level().dimension());

        applyOrClear(player.getAttribute(Attributes.GRAVITY), GRAVITY_MODIFIER_ID, factor);
        applyOrClear(player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER), FALL_DAMAGE_MODIFIER_ID, factor);
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

        boolean noAtmosphere = gravityFactorFor(player.level().dimension()) != null;
        if (noAtmosphere && !isWearingFullSpaceSuit(player)) {
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
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.IRON_SPACE_SUIT_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.IRON_SPACE_SUIT_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.IRON_SPACE_SUIT_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.IRON_SPACE_SUIT_BOOTS.get());
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
