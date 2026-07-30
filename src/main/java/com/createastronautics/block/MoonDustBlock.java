package com.createastronautics.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Fine, powdery lunar regolith. Behaves just like sand: falls with gravity, same footstep/dig sounds. */
public class MoonDustBlock extends FallingBlock {
    public static final MapCodec<MoonDustBlock> CODEC = simpleCodec(MoonDustBlock::new);

    public MoonDustBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }
}
