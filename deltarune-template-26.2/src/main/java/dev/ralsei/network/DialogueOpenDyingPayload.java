package dev.ralsei.network;

import dev.ralsei.Deltarune;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DialogueOpenDyingPayload(int entityId) implements CustomPacketPayload {
    public static final Type<DialogueOpenDyingPayload> TYPE = new Type<>(Deltarune.id("dialogue_open_dying"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueOpenDyingPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DialogueOpenDyingPayload::entityId,
            DialogueOpenDyingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}