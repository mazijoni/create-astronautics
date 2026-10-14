package com.createastronautics.space.mixin.gravity;

import com.createastronautics.ModDimensions;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Makes dropped items, arrows, falling blocks and every other non-living entity fall at the local gravity
// (see ModDimensions#gravityScale). Same hook as Northstar-Redux's EntityGravityMixin
// (com.lightning.northstar.mixin.gravity) - https://github.com/Astronauts-of-Create/Northstar-Redux -
// MIT-licensed.
@Mixin(Entity.class)
public abstract class EntityGravityMixin {
    @ModifyReturnValue(method = "getGravity", at = @At("RETURN"))
    private double createastronautics$scaleGravity(double original) {
        Entity self = (Entity) (Object) this;
        // Living entities read theirs from the gravity attribute, which is where players' is already set.
        if (self instanceof LivingEntity) {
            return original;
        }
        return original * ModDimensions.gravityScale(self.level().dimension());
    }
}
