package com.createastronautics.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * The faint drifting mist that marks a sealed room's breathable air. Reuses vanilla's own
 * {@code minecraft:cloud} sprite sheet (the {@code minecraft:generic_*} particle textures), but stays much
 * fainter than a real cloud puff - peak alpha of {@value #PEAK_ALPHA}, fading in and back out over its life
 * instead of popping in and out at full opacity - so it reads as "there's air here" rather than smoke.
 */
public class OxygenMistParticle extends TextureSheetParticle {
    private static final float PEAK_ALPHA = 0.18F;
    private static final float FADE_FRACTION = 0.3F;

    protected OxygenMistParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet spriteSet) {
        super(level, x, y, z, dx, dy, dz);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.98F;
        this.quadSize = 0.15F + this.random.nextFloat() * 0.1F;
        this.lifetime = 40 + this.random.nextInt(20);
        // Overrides the fairly large random jitter Particle's own constructor already applied to dx/dy/dz -
        // fine for a generic burst particle, far too jittery for a slow drifting mist.
        this.xd = dx + (this.random.nextFloat() * 2.0F - 1.0F) * 0.01F;
        this.yd = dy;
        this.zd = dz + (this.random.nextFloat() * 2.0F - 1.0F) * 0.01F;
        this.setSprite(spriteSet.get(this.random));
        this.setColor(1.0F, 1.0F, 1.0F);
        this.setAlpha(0.0F);
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

        this.move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.zd *= this.friction;

        float lifeFraction = (float) this.age / (float) this.lifetime;
        float fade = lifeFraction < FADE_FRACTION ? lifeFraction / FADE_FRACTION
                : lifeFraction > 1.0F - FADE_FRACTION ? (1.0F - lifeFraction) / FADE_FRACTION
                : 1.0F;
        this.setAlpha(Mth.clamp(fade, 0.0F, 1.0F) * PEAK_ALPHA);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                        double x, double y, double z, double dx, double dy, double dz) {
            return new OxygenMistParticle(level, x, y, z, dx, dy, dz, spriteSet);
        }
    }
}
