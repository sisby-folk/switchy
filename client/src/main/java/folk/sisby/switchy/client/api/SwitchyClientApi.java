package folk.sisby.switchy.client.api;

import com.mojang.brigadier.StringReader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import static folk.sisby.switchy.SwitchyClientServerNetworking.*;
import static folk.sisby.switchy.SwitchyClientServerNetworking.C2S_PRESETS_MODULE_ENABLE;

/**
 * Client-side API for interacting with the client player's presets on the server
 *
 * @author Sisby folk
 * @since 1.9.1
 */
@SuppressWarnings("unused")
public class SwitchyClientApi {
	/**
	 * Switches to the specified preset on the server.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#switchCurrentPreset(net.minecraft.server.network.ServerPlayerEntity, String)
	 */
	public static void switchCurrentPreset(String name) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_SWITCH))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(name);
		ClientPlayNetworking.send(C2S_SWITCH, buf);
	}

	/**
	 * Creates a new preset on the server.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @throws IllegalArgumentException when the specified preset name is not a word ({@link StringReader#isAllowedInUnquotedString(char)})
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#newPreset(String)
	 */
	public static void newPreset(String name) throws UnsupportedOperationException, IllegalArgumentException {
		if (!ClientPlayNetworking.canSend(C2S_PRESETS_NEW))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		if (!name.chars().mapToObj(i -> (char) i).allMatch(StringReader::isAllowedInUnquotedString))
			throw new IllegalArgumentException("Specified preset name is not a word");
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(name);
		ClientPlayNetworking.send(C2S_PRESETS_NEW, buf);
	}

	/**
	 * Switches to the specified preset on the server.
	 * Lossy.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#deletePreset(String)
	 */
	public static void deletePreset(String name) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_PRESETS_DELETE))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(name);
		ClientPlayNetworking.send(C2S_PRESETS_DELETE, buf);
	}

	/**
	 * Changes the name of the specified preset on the server.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @param newName the new name for the specified preset. a single word matching {@code azAZ09_-.+}.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#renamePreset(String, String)
	 */
	public static void renamePreset(String name, String newName) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_PRESETS_RENAME))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(name);
		buf.writeString(newName);
		ClientPlayNetworking.send(C2S_PRESETS_RENAME, buf);
	}

	/**
	 * Disables the specified module on the server.
	 * Lossy.
	 *
	 * @param id a module identifier.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#disableModule(Identifier)
	 */
	public static void disableModule(Identifier id) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_PRESETS_MODULE_DISABLE))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(id.toString());
		ClientPlayNetworking.send(C2S_PRESETS_MODULE_DISABLE, buf);
	}

	/**
	 * Enables the specified module on the server.
	 *
	 * @param id a module identifier.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#enableModule(Identifier)
	 */
	public static void enableModule(Identifier id) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_PRESETS_MODULE_ENABLE))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(id.toString());
		ClientPlayNetworking.send(C2S_PRESETS_MODULE_ENABLE, buf);
	}
}
