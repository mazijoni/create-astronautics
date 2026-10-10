package com.createastronautics.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Plain vanilla {@link FallingBlock} behaviour (falls, chance to drop flint instead of itself - the drop
 * chance is loot-table-driven, see loot_table/blocks/lunar_gravel.json) under a different texture. A
 * concrete subclass only exists because {@code FallingBlock} itself is abstract, requiring a {@link #codec()}.
 */
public class LunarGravelBlock extends FallingBlock {
    public static final MapCodec<LunarGravelBlock> CODEC = simpleCodec(LunarGravelBlock::new);

    public LunarGravelBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }
}
