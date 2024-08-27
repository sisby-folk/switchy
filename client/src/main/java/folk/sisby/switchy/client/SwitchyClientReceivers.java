package folk.sisby.switchy.client;

import folk.sisby.switchy.SwitchyClientServerNetworking;
import folk.sisby.switchy.api.SwitchyFeedback;
import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import folk.sisby.switchy.api.module.presets.SwitchyClientPresets;
import folk.sisby.switchy.client.api.SwitchyClientEvents;
import folk.sisby.switchy.packet.S2CExportPresets;
import folk.sisby.switchy.packet.S2CPreviewPresets;
import folk.sisby.switchy.packet.S2CSwitchEvent;
import folk.sisby.switchy.presets.SwitchyClientPresetsImpl;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.function.BiConsumer;

import static folk.sisby.switchy.client.api.SwitchyClientApi.API_EXPORT_LISTENERS;
import static folk.sisby.switchy.client.api.SwitchyClientApi.API_RESPONSE_LISTENERS;

/**
 * Client-side network handling for client interactions with Switchy.
 *
 * @author Sisby folk
 * @since 1.9.1
 */
public class SwitchyClientReceivers {
	/**
	 * Register client-side receivers for Switchy Client.
	 */
	public static void InitializeReceivers() {
		SwitchyClientServerNetworking.touch();
		ClientPlayNetworking.registerGlobalReceiver(S2CExportPresets.ID, (packet, context) -> handleExportNbt(packet));
		ClientPlayNetworking.registerGlobalReceiver(S2CSwitchEvent.ID, (packet, context) -> SwitchyClientEvents.SWITCH.invoker().onSwitch(SwitchySwitchEvent.fromNbt(packet.eventNbt())));
		ClientPlayNetworking.registerGlobalReceiver(S2CPreviewPresets.ID, (packet, context) -> handleClientPresets(packet));
		ClientPlayConnectionEvents.DISCONNECT.register(SwitchyClientReceivers::onPlayDisconnect);
	}

	private static void handleClientPresets(S2CPreviewPresets packet) {
		int id = packet.listener();
		BiConsumer<SwitchyFeedback, SwitchyClientPresets> listener = API_RESPONSE_LISTENERS.remove(id);
		if (listener != null) {
			NbtCompound feedbackNbt = packet.feedbackNbt();
			if (feedbackNbt != null) {
				SwitchyFeedback feedback = SwitchyFeedback.fromNbt(feedbackNbt);
				NbtCompound presetsNbt = packet.presetsNbt();
				if (presetsNbt != null) {
					SwitchyClientPresets presets = new SwitchyClientPresetsImpl(new HashMap<>(), 0);
					presets.fillFromNbt(presetsNbt);
					listener.accept(feedback, presets);
				}
			}
		}
	}

	private static void handleExportNbt(S2CExportPresets packet) {
		int id = packet.listener();
		BiConsumer<SwitchyFeedback, NbtCompound> listener = API_EXPORT_LISTENERS.remove(id);
		if (listener != null) {
			NbtCompound feedbackNbt = packet.feedbackNbt();
			if (feedbackNbt != null) {
				SwitchyFeedback feedback = SwitchyFeedback.fromNbt(feedbackNbt);
				NbtCompound presetsNbt = packet.presetsNbt();
				if (presetsNbt != null) {
					listener.accept(feedback, presetsNbt);
				}
			}
		}
	}

	public static void onPlayDisconnect(ClientPlayNetworkHandler handler, MinecraftClient client) {
		SwitchySwitchEvent event = SwitchyClientEvents.PREVIOUS_SWITCH_EVENT;
		if (event != null) {
			SwitchyClientEvents.SWITCH.invoker().onSwitch(new SwitchySwitchEvent(event.player(), null, event.currentPreset(), event.enabledModules()));
			SwitchyClientEvents.PREVIOUS_SWITCH_EVENT = null;
		}
	}
}
