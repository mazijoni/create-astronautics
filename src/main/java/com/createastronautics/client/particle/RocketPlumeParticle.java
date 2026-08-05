package com.createastronautics.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * The same plume-then-smoke animated sprite sequence as Create Propulsion: Simulated's solid-fuel thruster
 * exhaust (github.com/Propulsion-Team/create-propulsion-simulated, MIT licensed, Copyright (c) 2025 Sergey
 * Feduk) - the plume/smoke textures and frame-count constants are carried over from that mod's
 * {@code PlumeParticle}, though the motion here is a lighter self-managed collision instead of that mod's
 * bespoke collision-reflection solver and Sable sub-level velocity inheritance (see
 * {@link #resolveCollisionAndMove()}).
 */
public class RocketPlumeParticle extends SimpleAnimatedParticle {
    private static final int PLUME_FRAME_COUNT = 6;
    private static final int SMOKE_FRAME_COUNT = 7;
    private static final int TOTAL_FRAME_COUNT = PLUME_FRAME_COUNT + SMOKE_FRAME_COUNT;
    private static final int SMOKE_TRANSITION_AGE = 20;

    private final SpriteSet spriteSet;
    private boolean smoking;
    private boolean hasCollided;
    private float baseQuadSize;

    protected RocketPlumeParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet spriteSet) {
        super(level, x, y, z, spriteSet, 0.0F);
        this.spriteSet = spriteSet;
        // Vanilla's own block collision (Particle#move with hasPhysics=true) permanently freezes a
        // particle's movement the very first time an axis gets blocked - fine for embers settling into
        // ash, wrong for an exhaust plume that should keep billowing after it hits a surface. Collision is
        // instead handled by hand in resolveCollisionAndMove().
        this.hasPhysics = false;
        this.quadSize *= 2.0F;
        this.baseQuadSize = this.quadSize;
        this.lifetime = 40 + this.random.nextInt(5);
        this.friction = 0.99F;
        this.xd = dx + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
        this.yd = dy + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
        this.zd = dz + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
        pickSprite();
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        resolveCollisionAndMove();

        if (!this.smoking && this.age >= SMOKE_TRANSITION_AGE) {
            this.smoking = true;
            this.baseQuadSize *= 1.2F;
            this.friction = 0.96F;
        }
        if (this.smoking) {
            this.yd += 0.02F;
        }

        float percent = (float) this.age / (float) this.lifetime;
        this.quadSize = this.smoking
                ? this.baseQuadSize - percent * 2.0F + 2.5F
                : this.baseQuadSize + Mth.sqrt(percent) * 2.0F;

        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;

        pickSprite();
        this.setAlpha(Mth.clamp(this.age / 2.0F, 0.0F, 1.0F));
    }

    /**
     * Per-axis block collision check, then either a one-time "spread out across the surface" kick on the
     * first impact (floor/ceiling scatters sideways, a wall bounces back) or - on every tick after that -
     * simply zeroing whichever axis is currently blocked so the particle slides along the surface instead
     * of tunnelling through it.
     */
    private void resolveCollisionAndMove() {
        boolean blockedX = this.xd != 0 && isSolid(this.x + this.xd, this.y, this.z);
        boolean blockedY = this.yd != 0 && isSolid(this.x, this.y + this.yd, this.z);
        boolean blockedZ = this.zd != 0 && isSolid(this.x, this.y, this.z + this.zd);

        if (!blockedX && !blockedY && !blockedZ) {
            this.move(this.xd, this.yd, this.zd);
            return;
        }

        if (!this.hasCollided) {
            this.hasCollided = true;
            float spreadSpeed = (float) Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd) * 0.6F;
            if (blockedY) {
                float angle = this.random.nextFloat() * ((float) Math.PI * 2.0F);
                this.xd = Mth.cos(angle) * spreadSpeed;
                this.zd = Mth.sin(angle) * spreadSpeed;
                this.yd = 0.0;
            } else {
                this.yd = Math.abs(this.yd) * 0.5 + 0.01;
                if (blockedX) {
                    this.xd = -this.xd * 0.3;
                }
                if (blockedZ) {
                    this.zd = -this.zd * 0.3;
                }
            }
        } else {
            if (blockedX) this.xd = 0.0;
            if (blockedY) this.yd = 0.0;
            if (blockedZ) this.zd = 0.0;
        }

        this.move(this.xd, this.yd, this.zd);
    }

    private boolean isSolid(double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        VoxelShape shape = this.level.getBlockState(pos).getCollisionShape(this.level, pos);
        return !shape.isEmpty();
    }

    private void pickSprite() {
        int frame = this.smoking
                ? PLUME_FRAME_COUNT + Mth.clamp((this.age - SMOKE_TRANSITION_AGE) * SMOKE_FRAME_COUNT / Math.max(1, this.lifetime - SMOKE_TRANSITION_AGE), 0, SMOKE_FRAME_COUNT - 1)
                : Mth.clamp(this.age * PLUME_FRAME_COUNT / SMOKE_TRANSITION_AGE, 0, PLUME_FRAME_COUNT - 1);
        this.setSprite(this.spriteSet.get(frame, TOTAL_FRAME_COUNT));
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                        double x, double y, double z, double dx, double dy, double dz) {
            return new RocketPlumeParticle(level, x, y, z, dx, dy, dz, spriteSet);
        }
    }
}
