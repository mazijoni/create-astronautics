package com.createastronautics.magnetic;

import com.createastronautics.ModDimensions;
import com.createastronautics.item.ModItems;
import com.createastronautics.network.MagneticBootsTogglePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-authoritative magnetic boots toggling: the client only ever asks "flip it," the server decides
 * whether that's allowed (boots worn + Deep Space). Deliberately manual-only - nothing anywhere else in the
 * mod ever calls {@link #setActive} on its own, so the only way this state changes is the player pressing
 * the toggle key again. See {@link MagneticBootsEffectHandler} for what "active" actually does each tick.
 */
public class MagneticBootsNetworkHandler {
    public static void handleToggle(MagneticBootsTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                setActive(player, !isActive(player));
            }
        });
    }

    public static boolean isActive(Player player) {
        return player.getData(ModAttachments.MAGNETIC_BOOTS_ACTIVE);
    }

    public static void setActive(ServerPlayer player, boolean active) {
        player.setData(ModAttachments.MAGNETIC_BOOTS_ACTIVE, active);
        player.displayClientMessage(active
                ? Component.translatable("hud.createastronautics.magnetic_boots.on").withStyle(ChatFormatting.GREEN)
                : Component.translatable("hud.createastronautics.magnetic_boots.off").withStyle(ChatFormatting.RED), true);
    }

    public static boolean canActivate(Player player) {
        return player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BRASS_SPACE_SUIT_BOOTS.get())
                && player.level().dimension() == ModDimensions.DEEP_SPACE;
    }
}
