package com.createastronautics.space.mixin.gravity;

import com.createastronautics.ModDimensions;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BubblePopParticle;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.CherryParticle;
import net.minecraft.client.particle.DripParticle;
import net.minecraft.client.particle.FallingDustParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SquidInkParticle;
import net.minecraft.client.particle.WakeParticle;
import net.minecraft.client.particle.WaterDropParticle;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

// Makes particles fall at the local gravity (see ModDimensions#gravityScale). Ported from Northstar-Redux's
// ParticleGravityMixin (com.lightning.northstar.mixin.gravity) -
// https://github.com/Astronauts-of-Create/Northstar-Redux - MIT-licensed.
@Mixin(Particle.class)
public class ParticleGravityMixin {
    @Shadow
    @Final
    protected ClientLevel level;

    @ModifyExpressionValue(
            method = {
                    "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)V",
                    "setPower"
            },
            at = @At(
                    value = "CONSTANT",
                    args = "doubleValue=0.10000000149011612"
            )
    )
    private double createastronautics$modifyInitialVelocity(double original) {
        // Particles start with a small upward kick that gravity is meant to pull back down again - with no
        // gravity at all, that kick alone sends every particle drifting upwards forever.
        return ModDimensions.gravityScale(level.dimension()) == 0.0F ? 0.0 : original;
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/particle/Particle;gravity:F",
                    opcode = Opcodes.GETFIELD
            )
    )
    private float createastronautics$modifyGravity(float original) {
        return original * ModDimensions.gravityScale(level.dimension());
    }

    @Mixin(FallingDustParticle.class)
    public abstract static class FallingDustParticleMixin extends Particle {
        protected FallingDustParticleMixin(ClientLevel level, double x, double y, double z) {
            super(level, x, y, z);
        }

        @ModifyExpressionValue(method = "tick", at = @At(value = "CONSTANT", args = "doubleValue=0.003000000026077032"))
        private double createastronautics$modifyGravity(double constant) {
            return constant * ModDimensions.gravityScale(level.dimension());
        }
    }

    @Mixin(SquidInkParticle.class)
    public abstract static class SquidInkParticleMixin extends Particle {
        protected SquidInkParticleMixin(ClientLevel level, double x, double y, double z) {
            super(level, x, y, z);
        }

        @ModifyExpressionValue(method = "tick", at = @At(value = "CONSTANT", args = "doubleValue=0.007400000002235174"))
        private double createastronautics$modifyGravity(double constant) {
            return constant * ModDimensions.gravityScale(level.dimension());
        }
    }

    // The rest override tick() and read the gravity field through their own class rather than through
    // Particle, which makes each one a different injection target as far as the bytecode is concerned.

    @Mixin(BubblePopParticle.class)
    public abstract static class BubblePopParticleMixin extends Particle {
        protected BubblePopParticleMixin(ClientLevel level, double x, double y, double z) {
            super(level, x, y, z);
        }

        @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/BubblePopParticle;gravity:F", opcode = Opcodes.GETFIELD))
        private float createastronautics$modifyGravity(float original) {
            return original * ModDimensions.gravityScale(level.dimension());
        }
    }

    @Mixin(CampfireSmokeParticle.class)
    public abstract static class CampfireSmokeParticleMixin extends Particle {
        protected CampfireSmokeParticleMixin(ClientLevel level, double x, double y, double z) {
            super(level, x, y, z);
        }

        @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/CampfireSmokeParticle;gravity:F", opcode = Opcodes.GETFIELD))
        private float createastronautics$modifyGravity(float original) {
            return original * ModDimensions.gravityScale(level.dimension());
        }
    }

    @Mixin(CherryParticle.class)
    public abstract static class CherryParticleMixin extends Particle {
        protected CherryParticleMixin(ClientLevel level, double x, double y, double z) {
            super(level, x, y, z);
        }

        @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/CherryParticle;gravity:F", opcode = Opcodes.GETFIELD))
        private float createastronautics$modifyGravity(float original) {
            return original * ModDimensions.gravityScale(level.dimension());
        }
    }

    @Mixin(DripParticle.class)
    public abstract static class DripParticleMixin extends Particle {
        protected DripParticleMixin(ClientLevel level, double x, double y, double z) {
            super(level, x, y, z);
        }

        @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/DripParticle;gravity:F", opcode = Opcodes.GETFIELD))
        private float createastronautics$modifyGravity(float original) {
            return original * ModDimensions.gravityScale(level.dimension());
        }
    }

    @Mixin(WakeParticle.class)
    public abstract static class WakeParticleMixin extends Particle {
        protected WakeParticleMixin(ClientLevel level, double x, double y, double z) {
            super(level, x, y, z);
        }

        @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/WakeParticle;gravity:F", opcode = Opcodes.GETFIELD))
        private float createastronautics$modifyGravity(float original) {
            return original * ModDimensions.gravityScale(level.dimension());
        }
    }

    @Mixin(WaterDropParticle.class)
    public abstract static class WaterDropParticleMixin extends Particle {
        protected WaterDropParticleMixin(ClientLevel level, double x, double y, double z) {
            super(level, x, y, z);
        }

        @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/WaterDropParticle;gravity:F", opcode = Opcodes.GETFIELD))
        private float createastronautics$modifyGravity(float original) {
            return original * ModDimensions.gravityScale(level.dimension());
        }
    }
}
