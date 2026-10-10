package com.createastronautics.client;

import com.createastronautics.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.mixin.accessor.EntityRenderDispatcherAccessor;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the brass space suit helmet's glass dome as its own translucent overlay, drawn with Create's own
 * baked-mesh utilities instead of a second GeckoLib render pass - so the underlying GeckoLib helmet (see
 * {@link com.createastronautics.item.BrassSpaceSuitArmorItem}) can stay a single, uniformly opaque piece like
 * every other suit part. Adapted from Northstar-Redux's SpaceSuitLayerRenderer, see
 * THIRD_PARTY_NOTICES.md - a plain {@link RenderLayer} added onto every living entity renderer (the same way
 * vanilla adds its own armor/elytra/name-tag layers), entirely independent of GeckoLib's bone/RenderType
 * system, which sidesteps whatever render-order/buffer-flush quirks a GeckoLib-internal translucent pass ran
 * into.
 */
public class BrassSpaceSuitHelmetLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public BrassSpaceSuitHelmetLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack ms, MultiBufferSource buffer, int light, T entity,
                        float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                        float netHeadYaw, float headPitch) {
        if (entity.getPose() == Pose.SLEEPING || !entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BRASS_SPACE_SUIT_HELMET.get())) {
            return;
        }

        M entityModel = getParentModel();
        if (!(entityModel instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        BlockState air = Blocks.AIR.defaultBlockState();
        SuperByteBuffer helmet = CachedBuffers.partial(BrassSpaceSuitPartialModels.BRASS_SPACE_SUIT_HELMET, air);

        ms.pushPose();

        if (humanoidModel.young) {
            ms.scale(0.75F, 0.75F, 0.75F);
            ms.translate(0, 1, 0);
        }

        humanoidModel.head.translateAndRotate(ms);
        // 1.45 nudged down a quarter pixel (1/16 block per pixel) to sit flush against the helmet - the
        // later Y-flip (scale(-1,-1,1) below) means increasing this value moves the visor down on screen.
        ms.translate(0.5, 1.465625, -0.5);
        ms.scale(-1, -1, 1);

        helmet.disableDiffuse()
                .light(light)
                .renderInto(ms, buffer.getBuffer(Sheets.translucentCullBlockSheet()));

        ms.popPose();
    }

    public static void registerOnAll(EntityRenderDispatcher renderManager) {
        for (EntityRenderer<? extends Player> renderer : renderManager.getSkinMap().values()) {
            registerOn(renderer);
        }
        for (EntityRenderer<?> renderer : ((EntityRenderDispatcherAccessor) renderManager).create$getRenderers().values()) {
            registerOn(renderer);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerOn(EntityRenderer<?> entityRenderer) {
        if (!(entityRenderer instanceof LivingEntityRenderer<?, ?> livingRenderer)) {
            return;
        }
        if (!(livingRenderer.getModel() instanceof HumanoidModel)) {
            return;
        }
        BrassSpaceSuitHelmetLayer<?, ?> layer = new BrassSpaceSuitHelmetLayer<>(livingRenderer);
        livingRenderer.addLayer((RenderLayer) layer);
    }
}
