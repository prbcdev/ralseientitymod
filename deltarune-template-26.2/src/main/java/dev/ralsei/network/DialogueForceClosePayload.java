package dev.ralsei.network;

import dev.ralsei.Deltarune;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DialogueForceClosePayload() implements CustomPacketPayload {
    public static final Type<DialogueForceClosePayload> TYPE = new Type<>(Deltarune.id("dialogue_force_close"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueForceClosePayload> CODEC =
            StreamCodec.unit(new DialogueForceClosePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}