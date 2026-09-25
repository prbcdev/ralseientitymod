package dev.ralsei.chat;

import dev.ralsei.entity.RalseiEntity;
import dev.ralsei.entity.dialogue.DialoguePool;
import dev.ralsei.network.DialogueOpenChatResponsePayload;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

public final class RalseiChatListener {

    private RalseiChatListener() {}

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register(RalseiChatListener::onChatMessage);
    }

    private static void onChatMessage(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound boundChatType) {
        Set<String> words = tokenize(message.decoratedContent().getString());
        if (!DialoguePool.mentionsRalsei(words)) {
            return;
        }

        RalseiEntity target = findNearestRalsei(sender);
        String interruption = target.beginTalkingWithInterruption(sender);
        if (interruption == null) {
            return; // out of range, or already talking to someone else — ignore silently
        }

        ServerPlayNetworking.send(sender, new DialogueOpenChatResponsePayload(target.getId(), DialoguePool.matchChatKey(words), interruption));
    }

    private static Set<String> tokenize(String content) {
        return Set.of(content.toLowerCase(Locale.ROOT).split("[^a-z0-9']+"));
    }

    private static @Nullable RalseiEntity findNearestRalsei(ServerPlayer sender) {
        double range = Math.sqrt(RalseiEntity.MAX_TALK_DISTANCE_SQ);
        RalseiEntity nearest = null;
        double nearestDistSq = RalseiEntity.MAX_TALK_DISTANCE_SQ;
        for (RalseiEntity ralsei : sender.level().getEntitiesOfClass(RalseiEntity.class, sender.getBoundingBox().inflate(range))) {
            double distSq = ralsei.distanceToSqr(sender);
            if (distSq <= nearestDistSq) {
                nearest = ralsei;
                nearestDistSq = distSq;
            }
        }
        return nearest;
    }
}