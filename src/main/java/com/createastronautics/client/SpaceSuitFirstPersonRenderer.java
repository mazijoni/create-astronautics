package com.createastronautics.client;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Replaces the vanilla arm/sleeve with the brass space suit's sleeve texture in first-person view when the
 * chestplate is worn. Adapted from Northstar-Redux's {@code SpaceSuitFirstPersonRenderer}, see
 * {@code THIRD_PARTY_NOTICES.md}.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class SpaceSuitFirstPersonRenderer {
    private static final ResourceLocation ARM_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "textures/armor/brass_space_suit_arm.png");

    private static boolean active = false;

    @SubscribeEvent
    public static void onClientTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ItemStack chestplate = mc.player == null ? null : mc.player.getInventory().getArmor(2);
        active = chestplate != null && chestplate.is(ModItems.BRASS_SPACE_SUIT_CHESTPLATE.get());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerHand(RenderArmEvent event) {
        if (!active) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        MultiBufferSource buffer = event.getMultiBufferSource();
        if (!(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer pr)) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = pr.getModel();
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        ModelPart armPart = event.getArm() == HumanoidArm.LEFT ? model.leftSleeve : model.rightSleeve;
        armPart.xRot = 0.0F;
        // SpaceSuitLayerVisibility hides this same ModelPart (shared with the third-person model) while the
        // chestplate is worn, and ModelPart#render is a no-op when invisible - force it back on since we're
        // deliberately rendering it here as the first-person hand replacement.
        armPart.visible = true;
        armPart.render(event.getPoseStack(), buffer.getBuffer(RenderType.entitySolid(ARM_TEXTURE)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        event.setCanceled(true);
    }
}
