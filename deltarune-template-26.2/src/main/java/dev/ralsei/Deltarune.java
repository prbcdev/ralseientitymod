package dev.ralsei;

import dev.ralsei.chat.RalseiChatListener;
import dev.ralsei.entity.ModEntityTypes;
import dev.ralsei.entity.RalseiEntity;
import dev.ralsei.item.ModCreativeTabs;
import dev.ralsei.item.ModItems;
import dev.ralsei.network.*;
import dev.ralsei.sound.ModSounds;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Deltarune implements ModInitializer {
	public static final String MOD_ID = "deltarune";

	//outputs info, warnings & errors to console + creates log files w/ all relevant info
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		//runs immediately on start-up, please keep this section neatly organized with line spacers & comments

		// ── entity related entries i.e. mob AI, behavior & miscellaneous mechanics ──
		ModEntityTypes.registerModEntityTypes();
		ModEntityTypes.registerAttributes();

		// ── item registries & groups ──
		ModItems.registerModItems();

		// ── creative menu item group hooks + logic ──
		ModCreativeTabs.registerModCreativeTabs();

		// ── custom sfx ──
		ModSounds.registerModSounds();

		// ── networking payloads ──
		PayloadTypeRegistry.clientboundPlay().register(StopLullabyPayload.TYPE, StopLullabyPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DialogueClosedPayload.TYPE, DialogueClosedPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(DialogueForceClosePayload.TYPE, DialogueForceClosePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(DialogueOpenFollowConfirmPayload.TYPE, DialogueOpenFollowConfirmPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(DialogueOpenChatResponsePayload.TYPE, DialogueOpenChatResponsePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(DialogueOpenDyingPayload.TYPE, DialogueOpenDyingPayload.CODEC);
		RalseiChatListener.register();

		ServerPlayNetworking.registerGlobalReceiver(DialogueClosedPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				var entity = context.player().level().getEntity(payload.entityId());
				if (entity instanceof RalseiEntity ralsei) {
					ralsei.endTalking(context.player()); // was context.player().getUUID()
				}
			});
		});

		LOGGER.info("ralsei: hi");
	}
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
