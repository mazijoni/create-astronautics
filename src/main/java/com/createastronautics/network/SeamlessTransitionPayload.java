package com.createastronautics.network;

import com.createastronautics.CreateAstronautics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server tells the client a ship-borne dimension change is about to happen ({@code active = true}) or has
 * finished ({@code false}), so the client can skip the usual "Loading terrain..." screen in between - see
 * {@link com.createastronautics.client.SeamlessTransition}.
 */
public record SeamlessTransitionPayload(boolean active) implements CustomPacketPayload {
    public static final Type<SeamlessTransitionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateAstronautics.MODID, "seamless_transition"));

    public static final StreamCodec<FriendlyByteBuf, SeamlessTransitionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.active),
            buf -> new SeamlessTransitionPayload(buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
