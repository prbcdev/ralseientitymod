package dev.ralsei.network;

import dev.ralsei.Deltarune;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DialogueOpenFollowConfirmPayload(int entityId, boolean starting) implements CustomPacketPayload {
    public static final Type<DialogueOpenFollowConfirmPayload> TYPE = new Type<>(Deltarune.id("dialogue_open_follow_confirm"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueOpenFollowConfirmPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DialogueOpenFollowConfirmPayload::entityId,
            ByteBufCodecs.BOOL, DialogueOpenFollowConfirmPayload::starting,
            DialogueOpenFollowConfirmPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}