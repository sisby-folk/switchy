package folk.sisby.switchy.client;

import folk.sisby.switchy.api.SwitchyFeedback;
import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import folk.sisby.switchy.api.module.presets.SwitchyClientPresets;
import folk.sisby.switchy.client.api.SwitchyClientEvents;
import folk.sisby.switchy.presets.SwitchyClientPresetsImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import org.quiltmc.qsl.networking.api.client.ClientPlayConnectionEvents;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.util.HashMap;
import java.util.function.BiConsumer;

import static folk.sisby.switchy.SwitchyClientServerNetworking.*;
import static folk.sisby.switchy.client.api.SwitchyClientApi.API_EXPORT_LISTENERS;
import static folk.sisby.switchy.client.api.SwitchyClientApi.API_RESPONSE_LISTENERS;
import static folk.sisby.switchy.util.SwitchyCommand.consumeEventPacket;

/**
 * Client-side network handling for client interactions with Switchy.
 *
 * @author Sisby folk
 * @since 1.9.1
 */
public class SwitchyClientReceivers implements ClientPlayConnectionEvents.Disconnect {
	/**
	 * Register client-side receivers for Switchy Client.
	 */
	public static void InitializeReceivers() {
		ClientPlayNetworking.registerGlobalReceiver(S2C_PRESETS, (client, handler, buf, sender) -> handleExportNbt(buf));
		ClientPlayNetworking.registerGlobalReceiver(S2C_EVENT_SWITCH, (client, handler, buf, sender) -> consumeEventPacket(buf, SwitchySwitchEvent::fromNbt, SwitchyClientEvents.SWITCH.invoker()::onSwitch));
		ClientPlayNetworking.registerGlobalReceiver(S2C_CLIENT_PRESETS, (client, handler, buf, sender) -> handleClientPresets(buf));
	}

	private static void handleClientPresets(PacketByteBuf buf) {
		int id = buf.readInt();
		BiConsumer<SwitchyFeedback, SwitchyClientPresets> listener = API_RESPONSE_LISTENERS.remove(id);
		if (listener != null) {
			NbtCompound feedbackNbt = buf.readNbt();
			if (feedbackNbt != null) {
				SwitchyFeedback feedback = SwitchyFeedback.fromNbt(feedbackNbt);
				NbtCompound presetsNbt = buf.readNbt();
				if (presetsNbt != null) {
					SwitchyClientPresets presets = new SwitchyClientPresetsImpl(new HashMap<>(), 0);
					presets.fillFromNbt(presetsNbt);
					listener.accept(feedback, presets);
				}
			}
		}
	}

	private static void handleExportNbt(PacketByteBuf buf) {
		int id = buf.readInt();
		BiConsumer<SwitchyFeedback, NbtCompound> listener = API_EXPORT_LISTENERS.remove(id);
		if (listener != null) {
			NbtCompound feedbackNbt = buf.readNbt();
			if (feedbackNbt != null) {
				SwitchyFeedback feedback = SwitchyFeedback.fromNbt(feedbackNbt);
				NbtCompound presetsNbt = buf.readNbt();
				if (presetsNbt != null) {
					listener.accept(feedback, presetsNbt);
				}
			}
		}
	}

	@Override
	public void onPlayDisconnect(ClientPlayNetworkHandler handler, MinecraftClient client) {
		SwitchySwitchEvent event = SwitchyClientEvents.PREVIOUS_SWITCH_EVENT;
		if (event != null) {
			SwitchyClientEvents.SWITCH.invoker().onSwitch(new SwitchySwitchEvent(event.player(), null, event.currentPreset(), event.enabledModules()));
			SwitchyClientEvents.PREVIOUS_SWITCH_EVENT = null;
		}
	}
}
