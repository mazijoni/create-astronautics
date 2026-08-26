package com.createastronautics.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A standing torch that's burnt out in the vacuum of space - same shape as a regular torch, but dark: no
 * light (see its zero {@code lightLevel} in {@link ModBlocks}) and no flame/smoke particles. Vanilla
 * torches convert to this in the Moon and Deep Space dimensions, see
 * {@link com.createastronautics.PlayerEnvironmentHandler}.
 */
public class BurntTorchBlock extends TorchBlock {
    public static final MapCodec<BurntTorchBlock> CODEC = simpleCodec(BurntTorchBlock::new);

    public BurntTorchBlock(BlockBehaviour.Properties properties) {
        super(ParticleTypes.SMOKE, properties);
    }

    @Override
    public MapCodec<? extends TorchBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Burnt out - no flame or smoke particles.
    }
}
