package folk.sisby.switchy.client;

import folk.sisby.switchy.api.events.SwitchySwitchEvent;
import folk.sisby.switchy.client.api.SwitchyClientEvents;
import folk.sisby.switchy.client.screen.SwitchScreen;
import folk.sisby.switchy.api.module.presets.SwitchyDisplayPresets;
import folk.sisby.switchy.presets.SwitchyDisplayPresetsImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import static folk.sisby.switchy.SwitchyClientServerNetworking.*;
import static folk.sisby.switchy.client.util.FeedbackClient.tellInvalid;
import static folk.sisby.switchy.client.util.FeedbackClient.tellSuccess;
import static folk.sisby.switchy.util.Command.consumeEventPacket;
import static folk.sisby.switchy.util.Feedback.literal;

/**
 * @author Sisby folk
 * @since 1.9.1
 * Client-side network handling for client interactions with Switchy
 */
public class SwitchyClientNetworking {
	public static void InitializeReceivers() {
		ClientPlayNetworking.registerGlobalReceiver(S2C_PRESETS, (client, handler, buf, sender) -> exportPresets(client, buf));
		ClientPlayNetworking.registerGlobalReceiver(S2C_EVENT_SWITCH, (client, handler, buf, sender) -> consumeEventPacket(buf, SwitchySwitchEvent::fromNbt, SwitchyClientEvents.SWITCH.invoker()::onSwitch));
		ClientPlayNetworking.registerGlobalReceiver(S2C_DISPLAY_PRESETS, (client, handler, buf, sender) -> displayPresets(client, buf.readNbt()));
	}

	public static void switchCurrentPreset(String name) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(name);
		ClientPlayNetworking.send(C2S_SWITCH, buf);
	}

	private static void exportPresets(MinecraftClient client, PacketByteBuf buf) {
		NbtCompound presetNbt = buf.readNbt();
		if (presetNbt != null) {
			String filename = (client.isInSingleplayer() ? "Singleplayer_" : "Multiplayer_") + new SimpleDateFormat("MMM-dd_HH-mm-ss").format(new java.util.Date());
			File exportFile = new File(SwitchyClient.EXPORT_PATH + "/" + filename + ".dat");
			boolean ignored = exportFile.getParentFile().mkdirs();
			try {
				NbtIo.writeCompressed(presetNbt, exportFile);
				if (client.player != null) {
					tellSuccess(client.player, "commands.switchy_client.export.success", literal("config/switchy/" + filename + ".dat"));
				}
			} catch (IOException e) {
				SwitchyClient.LOGGER.error("IO error when copying default configuration", e);
				if (client.player != null) {
					tellInvalid(client.player, "commands.switchy_client.export.fail");
				}
			}
		}
	}

	private static void displayPresets(MinecraftClient client, @Nullable NbtCompound displayPresetsNbt) {
		if (displayPresetsNbt != null) {
			SwitchyDisplayPresets displayPresets = new SwitchyDisplayPresetsImpl();
			displayPresets.fillFromNbt(displayPresetsNbt);
			client.execute(() ->
					client.setScreen(new SwitchScreen(displayPresets))
			);
		}
	}
}
