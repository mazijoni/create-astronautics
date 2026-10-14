package com.createastronautics.client;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.block.RedstoneRegulatorBlock;
import com.createastronautics.block.RedstoneRegulatorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Colours the ring around the Redstone Regulator's centre square by how strong a signal it's putting out,
 * the same way Create colours its Pulse Repeater by how far through its delay it is (BrassDiodeRenderer):
 * the block's own model is left alone, and a second, slightly larger box - {@link #INDICATOR} - is drawn
 * over it in a flat colour. Only that box's rim shows around the raised centre square, which is the ring.
 *
 * Unlike Create's version this has no Flywheel visual alongside it, so it always draws here.
 */
public class RedstoneRegulatorRenderer extends SafeBlockEntityRenderer<RedstoneRegulatorBlockEntity> {
    public static final PartialModel INDICATOR =
            PartialModel.of(CreateAstronautics.id("block/redstone_regulator/indicator"));

    // Create's own diode indicator colours: unlit and fully lit.
    private static final int COLOR_OFF = 0x2C0300;
    private static final int COLOR_FULL = 0xCD0000;

    public RedstoneRegulatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Forces {@link #INDICATOR} to register before models are baked - see BrassSpaceSuitPartialModels. */
    public static void registerPartialModels() {
    }

    @Override
    protected void renderSafe(RedstoneRegulatorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        float strength = RedstoneRegulatorBlock.currentOutput(be.getBlockState()) / (float) RedstoneRegulatorBlock.MAX_SIGNAL;
        CachedBuffers.partial(INDICATOR, be.getBlockState())
                .color(Color.mixColors(COLOR_OFF, COLOR_FULL, strength))
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }
}
