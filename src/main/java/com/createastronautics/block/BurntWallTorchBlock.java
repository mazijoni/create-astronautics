package com.createastronautics.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** The wall-mounted counterpart of {@link BurntTorchBlock} - see there for why it has no light or particles. */
public class BurntWallTorchBlock extends WallTorchBlock {
    // WallTorchBlock.codec() (unlike most vanilla blocks) returns the exact, non-wildcarded
    // MapCodec<WallTorchBlock>, so subclasses can't narrow it further - typed as WallTorchBlock here even
    // though the factory always produces a BurntWallTorchBlock.
    public static final MapCodec<WallTorchBlock> CODEC = simpleCodec(BurntWallTorchBlock::new);

    public BurntWallTorchBlock(BlockBehaviour.Properties properties) {
        super(ParticleTypes.SMOKE, properties);
    }

    @Override
    public MapCodec<WallTorchBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Burnt out - no flame or smoke particles.
    }
}
