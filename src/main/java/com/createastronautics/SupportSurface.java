package com.createastronautics;

import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * "Is there a floor (or ship deck) within reach below this entity" - shared between
 * {@link PlayerEnvironmentHandler} (restoring normal gravity while grounded in Deep Space) and
 * {@link com.createastronautics.magnetic.MagneticBootsEffectHandler} (the boots' own active pull). Routed
 * through {@link Sable#HELPER}'s {@code projectOutOfSubLevel} so this reads correctly while standing on a
 * Sable ship deck, not just plain world terrain: it resolves the ship-local block position to its current
 * apparent world Y.
 */
public final class SupportSurface {
    private SupportSurface() {}

    public static boolean isNear(Entity entity, double tolerance) {
        BlockPos supportingPos = entity.getBlockPosBelowThatAffectsMyMovement();
        if (entity.level().getBlockState(supportingPos).isAir()) {
            return false;
        }

        Vec3 projected = Sable.HELPER.projectOutOfSubLevel(entity.level(), supportingPos.getBottomCenter());
        double supportY = projected.y + 1.0;
        return entity.getY() - supportY <= tolerance;
    }
}
