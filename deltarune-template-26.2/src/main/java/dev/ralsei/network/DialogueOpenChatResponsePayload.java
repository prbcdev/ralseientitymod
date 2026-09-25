package dev.ralsei.network;

import dev.ralsei.Deltarune;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DialogueOpenChatResponsePayload(int entityId, String key, String interruption) implements CustomPacketPayload {
    public static final Type<DialogueOpenChatResponsePayload> TYPE = new Type<>(Deltarune.id("dialogue_open_chat_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueOpenChatResponsePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DialogueOpenChatResponsePayload::entityId,
            ByteBufCodecs.STRING_UTF8, DialogueOpenChatResponsePayload::key,
            ByteBufCodecs.STRING_UTF8, DialogueOpenChatResponsePayload::interruption,
            DialogueOpenChatResponsePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}