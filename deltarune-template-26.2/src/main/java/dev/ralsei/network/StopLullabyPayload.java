package dev.ralsei.network;

import dev.ralsei.Deltarune;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StopLullabyPayload(int entityId) implements CustomPacketPayload {
    public static final Type<StopLullabyPayload> TYPE = new Type<>(Deltarune.id("stop_lullaby"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StopLullabyPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StopLullabyPayload::entityId,
            StopLullabyPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}