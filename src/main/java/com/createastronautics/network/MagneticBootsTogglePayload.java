package com.createastronautics.network;

import com.createastronautics.CreateAstronautics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client asks the server to flip magnetic boots on/off; carries no data since the server always re-resolves
 * the target surface itself from the player's own (server-side) position and look direction rather than
 * trusting anything the client reports.
 */
public record MagneticBootsTogglePayload() implements CustomPacketPayload {
    public static final Type<MagneticBootsTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "magnetic_boots_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagneticBootsTogglePayload> STREAM_CODEC =
            StreamCodec.unit(new MagneticBootsTogglePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
