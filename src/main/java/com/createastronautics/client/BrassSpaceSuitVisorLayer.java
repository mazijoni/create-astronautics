package com.createastronautics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.createastronautics.item.BrassSpaceSuitArmorItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders the helmet's {@code armorHead} bone translucently instead of through the opaque, cutout pass every
 * other suit piece uses (see {@link BrassSpaceSuitArmorItem}), so the glass dome's alpha channel actually
 * blends with whatever is behind it.
 * <p>
 * {@link GeoRenderer#renderRecursively} always draws a bone's cubes through the normal opaque pass first and
 * only afterwards calls a layer's {@link #renderForBone} - the two are additive, not a replacement. Since
 * {@code armorCutoutNoCull} has no blending (a pixel is either fully opaque or fully discarded, regardless of
 * its actual alpha), letting that first pass draw the visor at all would paint it fully solid before we ever
 * got to add glass blending on top. So {@link #preRender} hides the bone before that pass runs - {@code
 * GeoRenderer#renderCubesOfBone} skips hidden bones - and {@link #renderForBone} is the only thing that ever
 * draws it, un-hiding it just long enough to redraw its cubes on {@link RenderType#entityTranslucent}.
 * <p>
 * Every other suit piece (body/legs/boots) keeps its own separate opaque renderer untouched (see
 * {@link BrassSpaceSuitArmorItem}), so this translucent pass is scoped to just the helmet - it doesn't bring
 * back the deferred-render/depth-write timing problems translucent RenderTypes cause for Essential's
 * clothing cosmetics or the inventory paperdoll when the *entire* armor renderer is made translucent.
 */
public class BrassSpaceSuitVisorLayer extends GeoRenderLayer<BrassSpaceSuitArmorItem> {
    private static final String VISOR_BONE = "armorHead";

    public BrassSpaceSuitVisorLayer(GeoRenderer<BrassSpaceSuitArmorItem> renderer) {
        super(renderer);
    }

    @Override
    public void preRender(PoseStack poseStack, BrassSpaceSuitArmorItem animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                           MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        bakedModel.getBone(VISOR_BONE).ifPresent(bone -> bone.setHidden(true));
    }

    @Override
    public void renderForBone(PoseStack poseStack, BrassSpaceSuitArmorItem animatable, GeoBone bone, RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!VISOR_BONE.equals(bone.getName())) {
            return;
        }

        GeoRenderer<BrassSpaceSuitArmorItem> renderer = getRenderer();
        RenderType translucent = RenderType.entityTranslucent(getTextureResource(animatable));
        VertexConsumer translucentBuffer = bufferSource.getBuffer(translucent);
        int colour = renderer.getRenderColor(animatable, partialTick, packedLight).argbInt();

        // preRender hid this bone so the opaque cutout pass above would skip it - un-hide it just for this
        // draw (renderCubesOfBone is a no-op on hidden bones), then restore the hidden flag afterwards so
        // nothing else that inspects it later this frame sees a stale value.
        bone.setHidden(false);
        renderer.renderCubesOfBone(poseStack, bone, translucentBuffer, packedLight, packedOverlay, colour);
        bone.setHidden(true);
    }
}
