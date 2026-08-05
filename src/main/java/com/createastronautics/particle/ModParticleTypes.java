package com.createastronautics.particle;

import com.createastronautics.CreateAstronautics;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, CreateAstronautics.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ROCKET_PLUME = PARTICLE_TYPES.register("rocket_plume",
            () -> new SimpleParticleType(false));
}
