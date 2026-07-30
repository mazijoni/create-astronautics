package com.createastronautics.client;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/** Wraps another sound to play it quieter and lower-pitched, for the "no atmosphere to carry sound properly" feel. */
public class MuffledSoundInstance implements SoundInstance {
    private final SoundInstance delegate;
    private final float volumeMultiplier;
    private final float pitchMultiplier;

    public MuffledSoundInstance(SoundInstance delegate, float volumeMultiplier, float pitchMultiplier) {
        this.delegate = delegate;
        this.volumeMultiplier = volumeMultiplier;
        this.pitchMultiplier = pitchMultiplier;
    }

    @Override
    public ResourceLocation getLocation() {
        return delegate.getLocation();
    }

    @Override
    @Nullable
    public WeighedSoundEvents resolve(SoundManager soundManager) {
        return delegate.resolve(soundManager);
    }

    @Override
    public Sound getSound() {
        return delegate.getSound();
    }

    @Override
    public SoundSource getSource() {
        return delegate.getSource();
    }

    @Override
    public boolean isLooping() {
        return delegate.isLooping();
    }

    @Override
    public boolean isRelative() {
        return delegate.isRelative();
    }

    @Override
    public int getDelay() {
        return delegate.getDelay();
    }

    @Override
    public float getVolume() {
        return delegate.getVolume() * volumeMultiplier;
    }

    @Override
    public float getPitch() {
        return delegate.getPitch() * pitchMultiplier;
    }

    @Override
    public double getX() {
        return delegate.getX();
    }

    @Override
    public double getY() {
        return delegate.getY();
    }

    @Override
    public double getZ() {
        return delegate.getZ();
    }

    @Override
    public Attenuation getAttenuation() {
        return delegate.getAttenuation();
    }

    @Override
    public boolean canStartSilent() {
        return delegate.canStartSilent();
    }

    @Override
    public boolean canPlaySound() {
        return delegate.canPlaySound();
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
        return delegate.getStream(soundBuffers, sound, looping);
    }
}
