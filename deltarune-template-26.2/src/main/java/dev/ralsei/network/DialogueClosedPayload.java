package dev.ralsei.network;

import dev.ralsei.Deltarune;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DialogueClosedPayload(int entityId) implements CustomPacketPayload {
    public static final Type<DialogueClosedPayload> TYPE = new Type<>(Deltarune.id("dialogue_closed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueClosedPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DialogueClosedPayload::entityId,
            DialogueClosedPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}