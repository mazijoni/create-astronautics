package com.createastronautics.client;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.item.ModItems;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Hides the player skin's cosmetic overlay layers (hat, jacket, pant legs) that would otherwise clip
 * through the brass space suit's own geometry - vanilla never does this automatically, since it has no
 * concept of "this armor fully covers that body part".
 *
 * Runs at {@link EventPriority#LOWEST} so it applies after vanilla's own {@code setModelProperties} (which
 * runs before this event fires at all) and after any other mod's {@code RenderPlayerEvent.Pre} listener that
 * might also touch these same flags (e.g. Essentials' clothing layer), so ours is the value that sticks.
 */
@EventBusSubscriber(modid = CreateAstronautics.MODID, value = Dist.CLIENT)
public class SpaceSuitLayerVisibility {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!(event.getRenderer().getModel() instanceof PlayerModel<?> model)) {
            return;
        }

        boolean helmet = isBrassSuitPiece(player.getItemBySlot(EquipmentSlot.HEAD), ModItems.BRASS_SPACE_SUIT_HELMET.get());
        boolean chestplate = isBrassSuitPiece(player.getItemBySlot(EquipmentSlot.CHEST), ModItems.BRASS_SPACE_SUIT_CHESTPLATE.get());
        boolean leggings = isBrassSuitPiece(player.getItemBySlot(EquipmentSlot.LEGS), ModItems.BRASS_SPACE_SUIT_LEGGINGS.get());
        boolean boots = isBrassSuitPiece(player.getItemBySlot(EquipmentSlot.FEET), ModItems.BRASS_SPACE_SUIT_BOOTS.get());

        model.hat.visible = !helmet;
        model.jacket.visible = !chestplate;
        model.leftSleeve.visible = !chestplate;
        model.rightSleeve.visible = !chestplate;
        boolean pantsVisible = !leggings && !boots;
        model.leftPants.visible = pantsVisible;
        model.rightPants.visible = pantsVisible;
    }

    private static boolean isBrassSuitPiece(ItemStack stack, Item expected) {
        return stack.is(expected);
    }
}
