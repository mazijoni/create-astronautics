package com.createastronautics.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Holds the Redstone Regulator's one setting - the signal strength it puts out - as a Create value box on
 * top of the block: hold right-click on it for a single slider from 0 to 15, same as setting the time on
 * Create's own Pulse Repeater. The block state does the actual redstone work; this only pushes the chosen
 * value into it (see {@link RedstoneRegulatorBlock#setSignal}).
 */
public class RedstoneRegulatorBlockEntity extends SmartBlockEntity {
    private ScrollValueBehaviour signal;

    public RedstoneRegulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        signal = new SignalBehaviour(this).between(0, RedstoneRegulatorBlock.MAX_SIGNAL).withCallback(this::onSignalChanged);
        // Set directly rather than through setValue, which would try to sync a block entity that isn't in
        // the world yet.
        signal.value = RedstoneRegulatorBlock.MAX_SIGNAL;
        behaviours.add(signal);
    }

    private void onSignalChanged(int newSignal) {
        if (level != null && !level.isClientSide) {
            RedstoneRegulatorBlock.setSignal(level, worldPosition, newSignal);
        }
    }

    /** The slider itself: one row, a number per step. */
    private static class SignalBehaviour extends ScrollValueBehaviour {
        SignalBehaviour(SmartBlockEntity blockEntity) {
            super(Component.translatable("createastronautics.redstone_regulator.signal_strength"), blockEntity, new ValueSlot());
        }

        @Override
        public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
            return new ValueSettingsBoard(label, RedstoneRegulatorBlock.MAX_SIGNAL, 5,
                    List.of(Component.translatable("createastronautics.redstone_regulator.signal_row")),
                    new ValueSettingsFormatter(ValueSettings::format));
        }
    }

    /** Where the value box sits: flat on the raised square in the middle of the block. Same placement as Create's BrassDiodeScrollSlot. */
    private static class ValueSlot extends ValueBoxTransform {
        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            return VecHelper.voxelSpace(8, 2.6f, 8);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            float yRot = AngleHelper.horizontalAngle(state.getValue(BlockStateProperties.HORIZONTAL_FACING)) + 180;
            TransformStack.of(ms)
                    .rotateYDegrees(yRot)
                    .rotateXDegrees(90);
        }

        @Override
        public int getOverrideColor() {
            return 0x592424;
        }
    }
}
