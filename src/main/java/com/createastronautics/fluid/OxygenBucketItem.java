package com.createastronautics.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Unlike a vanilla bucket, this never places a source block in the world (Create tanks/pipes are filled
 * directly through the fluid handler capability by {@code FluidHelper}, which runs before this item's own
 * {@code use} logic would, so world placement is never needed for that to work).
 */
public class OxygenBucketItem extends BucketItem {
    public OxygenBucketItem(Fluid content, Properties properties) {
        super(content, properties);
    }

    @Override
    public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult hitResult, @Nullable ItemStack container) {
        return false;
    }
}
