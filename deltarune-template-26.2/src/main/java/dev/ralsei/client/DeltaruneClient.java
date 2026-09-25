package dev.ralsei.client;

import dev.ralsei.client.gui.RalseiDialogueScreen;
import dev.ralsei.client.render.RalseiEntityRenderer;
import dev.ralsei.entity.ModEntityTypes;
import dev.ralsei.entity.RalseiEntity;
import dev.ralsei.entity.dialogue.DialogueMessage;
import dev.ralsei.entity.dialogue.DialoguePool;
import dev.ralsei.network.*;
import dev.ralsei.sound.ModSounds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;

public class DeltaruneClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntityTypes.RALSEI, RalseiEntityRenderer::new);

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(entity instanceof RalseiEntity ralsei)) {
                return InteractionResult.PASS;
            }

            if (world.isClientSide()) {
                if (!ralsei.isTalking() && !player.isCrouching()) {
                    DialogueMessage[] messages;
                    if (ralsei.isSleeping()) {
                        messages = DialoguePool.randomWakeUp();
                    } else if (ralsei.isSinging()) {
                        messages = DialoguePool.randomCaughtSinging();
                    } else {
                        messages = DialoguePool.randomGreeting();
                    }
                    Minecraft.getInstance().gui.setScreen(new RalseiDialogueScreen(ralsei.getId(), messages));
                }
            } else if (player instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.isCrouching()) {
                    RalseiEntity.FollowPrompt prompt = ralsei.requestFollowToggle(serverPlayer);
                    if (prompt == RalseiEntity.FollowPrompt.START) {
                        ServerPlayNetworking.send(serverPlayer, new DialogueOpenFollowConfirmPayload(ralsei.getId(), true));
                    } else if (prompt == RalseiEntity.FollowPrompt.STOP) {
                        ServerPlayNetworking.send(serverPlayer, new DialogueOpenFollowConfirmPayload(ralsei.getId(), false));
                    }
                } else if (ralsei.isInBoredomTask()) {
                    if (!ralsei.interruptBoredomTask(serverPlayer)) {
                        ServerPlayNetworking.send(serverPlayer, new DialogueForceClosePayload());
                    }
                } else if (!ralsei.beginTalking(serverPlayer)) {
                    ServerPlayNetworking.send(serverPlayer, new DialogueForceClosePayload());
                }
            }

            return InteractionResult.SUCCESS;
        });

        ClientPlayNetworking.registerGlobalReceiver(DialogueOpenFollowConfirmPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                DialogueMessage[] messages = payload.starting()
                        ? DialoguePool.FOLLOW_START_MESSAGES
                        : DialoguePool.FOLLOW_STOP_MESSAGES;
                Minecraft.getInstance().gui.setScreen(new RalseiDialogueScreen(payload.entityId(), messages));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(DialogueForceClosePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (Minecraft.getInstance().gui.screen() instanceof RalseiDialogueScreen) {
                    Minecraft.getInstance().gui.setScreen(null);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(StopLullabyPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft.getInstance().getSoundManager().stop(ModSounds.RALSEI_LULLABY.location(), SoundSource.NEUTRAL);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DialogueOpenChatResponsePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                DialogueMessage[] response = DialoguePool.randomForChatKey(payload.key());
                DialogueMessage[] messages = switch (payload.interruption()) {
                    case "sleep" -> DialoguePool.concat(DialoguePool.randomWakeUp(), response);
                    case "sing" -> DialoguePool.concat(DialoguePool.randomCaughtSinging(), response);
                    default -> response;
                };
                Minecraft.getInstance().gui.setScreen(new RalseiDialogueScreen(payload.entityId(), messages));
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DialogueOpenDyingPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft.getInstance().gui.setScreen(new RalseiDialogueScreen(payload.entityId(), DialoguePool.randomDying()));
            });
        });

    }
}