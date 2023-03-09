package folk.sisby.switchy.client.api;

import com.mojang.brigadier.StringReader;
import folk.sisby.switchy.api.exception.InvalidWordException;
import folk.sisby.switchy.api.module.presets.SwitchyClientPresets;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static folk.sisby.switchy.SwitchyClientServerNetworking.*;

/**
 * Client-side API for interacting with the client player's presets on the server.
 *
 * @author Sisby folk
 * @since 1.9.1
 */
@SuppressWarnings("unused")
public class SwitchyClientApi {
	/**
	 * Map of listeners for API calls waiting for client preset returns.
	 */
	@ApiStatus.Internal
	public static final Map<Integer, BiConsumer<SwitchyRequestFeedback, SwitchyClientPresets>> API_RESPONSE_LISTENERS = new HashMap<>();
	public static final Map<Integer, BiConsumer<SwitchyRequestFeedback, NbtCompound>> API_EXPORT_LISTENERS = new HashMap<>();
	private static int nextId = 0;
	private static int nextExportId = 0;

	private static PacketByteBuf createSwitchyByteBuf(BiConsumer<SwitchyRequestFeedback, SwitchyClientPresets> listener) {
		int id = nextId++;
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(id);
		API_RESPONSE_LISTENERS.put(id, listener);
		return buf;
	}

	private static PacketByteBuf createSwitchyExportByteBuf(BiConsumer<SwitchyRequestFeedback, NbtCompound> listener) {
		int id = nextExportId++;
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(id);
		API_EXPORT_LISTENERS.put(id, listener);
		return buf;
	}

	/**
	 * Requests presets from the server in a client-compatible format.
	 *
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#switchCurrentPreset(net.minecraft.server.network.ServerPlayerEntity, String)
	 */
	public static void getClientPresets(BiConsumer<SwitchyRequestFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_REQUEST_CLIENT_PRESETS))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = createSwitchyByteBuf(responseCallback);
		ClientPlayNetworking.send(C2S_REQUEST_CLIENT_PRESETS, buf);
	}

	/**
	 * Switches to the specified preset on the server.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#switchCurrentPreset(net.minecraft.server.network.ServerPlayerEntity, String)
	 */
	public static void switchCurrentPreset(String name, BiConsumer<SwitchyRequestFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_SWITCH))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = createSwitchyByteBuf(responseCallback);
		buf.writeString(name);
		ClientPlayNetworking.send(C2S_SWITCH, buf);
	}

	/**
	 * Creates a new preset on the server.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @throws InvalidWordException when the specified preset name is not a word ({@link StringReader#isAllowedInUnquotedString(char)}).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#newPreset(String)
	 */
	public static void newPreset(String name, BiConsumer<SwitchyRequestFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException, InvalidWordException {
		if (!ClientPlayNetworking.canSend(C2S_PRESETS_NEW))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		if (!name.chars().mapToObj(i -> (char) i).allMatch(StringReader::isAllowedInUnquotedString))
			throw new InvalidWordException();
		PacketByteBuf buf = createSwitchyByteBuf(responseCallback);
		buf.writeString(name);
		ClientPlayNetworking.send(C2S_PRESETS_NEW, buf);
	}

	/**
	 * Switches to the specified preset on the server.
	 * Lossy.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#deletePreset(String)
	 */
	public static void deletePreset(String name, BiConsumer<SwitchyRequestFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_PRESETS_DELETE))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = createSwitchyByteBuf(responseCallback);
		buf.writeString(name);
		ClientPlayNetworking.send(C2S_PRESETS_DELETE, buf);
	}

	/**
	 * Changes the name of the specified preset on the server.
	 *
	 * @param name the case-insensitive name of a preset.
	 * @param newName the new name for the specified preset. a single word matching {@code azAZ09_-.+}.
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#renamePreset(String, String)
	 */
	public static void renamePreset(String name, String newName, BiConsumer<SwitchyRequestFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_PRESETS_RENAME))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = createSwitchyByteBuf(responseCallback);
		buf.writeString(name);
		buf.writeString(newName);
		ClientPlayNetworking.send(C2S_PRESETS_RENAME, buf);
	}

	/**
	 * Disables the specified module on the server.
	 * Lossy.
	 *
	 * @param id a module identifier.
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#disableModule(Identifier)
	 */
	public static void disableModule(Identifier id, BiConsumer<SwitchyRequestFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_PRESETS_MODULE_DISABLE))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = createSwitchyByteBuf(responseCallback);
		buf.writeString(id.toString());
		ClientPlayNetworking.send(C2S_PRESETS_MODULE_DISABLE, buf);
	}

	/**
	 * Enables the specified module on the server.
	 *
	 * @param id a module identifier.
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#enableModule(Identifier)
	 */
	public static void enableModule(Identifier id, BiConsumer<SwitchyRequestFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_PRESETS_MODULE_ENABLE))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = createSwitchyByteBuf(responseCallback);
		buf.writeString(id.toString());
		ClientPlayNetworking.send(C2S_PRESETS_MODULE_ENABLE, buf);
	}

	private static void writeModuleSpecifiers(NbtCompound presetsNbt, Collection<Identifier> excludeModules, Collection<Identifier> includeModules) {
		if (!excludeModules.isEmpty()) {
			NbtList excludeModulesNbt = new NbtList();
			excludeModules.stream().map(Identifier::toString).map(NbtString::of).forEach(excludeModulesNbt::add);
			presetsNbt.put(KEY_IMPORT_EXCLUDE, excludeModulesNbt);
		}
		if (!includeModules.isEmpty()) {
			NbtList includeModulesNbt = new NbtList();
			includeModules.stream().map(Identifier::toString).map(NbtString::of).forEach(includeModulesNbt::add);
			presetsNbt.put(KEY_IMPORT_INCLUDE, includeModulesNbt);
		}
	}

	private static void doImport(NbtCompound presetsNbt, Collection<Identifier> excludeModules, Collection<Identifier> includeModules, @Nullable String command, BiConsumer<SwitchyRequestFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(command != null ? C2S_IMPORT_CONFIRM : C2S_IMPORT))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		writeModuleSpecifiers(presetsNbt, excludeModules, includeModules);
		if (command != null) presetsNbt.putString(KEY_IMPORT_COMMAND, command);
		PacketByteBuf buf = createSwitchyByteBuf(responseCallback);
		ClientPlayNetworking.send(command != null ? C2S_IMPORT_CONFIRM : C2S_IMPORT, buf.writeNbt(presetsNbt));
	}

	/**
	 * Import the provided Presets NBT without confirmation.
	 * @param presetsNbt the NBT of presets and modules to import.
	 * @param excludeModules A collection of modules to not import from the NBT, even if they are allowed.
	 * @param includeModules A collection of modules to always import from the NBT, even if they require operator.
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#importFromOther(ServerPlayerEntity, SwitchyPresets)
	 */
	public static void importPresets(NbtCompound presetsNbt, Collection<Identifier> excludeModules, Collection<Identifier> includeModules, BiConsumer<SwitchyRequestFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		doImport(presetsNbt, excludeModules, includeModules, null, responseCallback);
	}

	/**
	 * Import the provided Presets NBT with chat-based confirmation.
	 * Must run twice with the same confirmation command to complete.
	 * @param presetsNbt the NBT of presets and modules to import.
	 * @param excludeModules A collection of modules to not import from the NBT, even if they are allowed.
	 * @param includeModules A collection of modules to always import from the NBT, even if they require operator.
	 * @param responseCallback the callback for the response from the server.
	 * @param confirmationCommand the command to prompt the player to re-enter to confirm.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#importFromOther(ServerPlayerEntity, SwitchyPresets)
	 */
	public static void importPresets(NbtCompound presetsNbt, Collection<Identifier> excludeModules, Collection<Identifier> includeModules, String confirmationCommand, BiConsumer<SwitchyRequestFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		doImport(presetsNbt, excludeModules, includeModules, confirmationCommand, responseCallback);
	}

	/**
	 * Export the player's presets to a file.
	 * @param excludeModules A collection of modules to not export to the NBT, if they exist.
	 * @param responseCallback the callback for the response from the server.
	 * @see folk.sisby.switchy.client.SwitchyClientReceivers
	 */
	public static void exportPresets(Collection<Identifier> excludeModules, BiConsumer<SwitchyRequestFeedback, NbtCompound> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_REQUEST_PRESETS))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		NbtCompound nbt = new NbtCompound();
		writeModuleSpecifiers(nbt, excludeModules, List.of());
		PacketByteBuf buf = createSwitchyExportByteBuf(responseCallback);
		ClientPlayNetworking.send(C2S_REQUEST_PRESETS, buf.writeNbt(nbt));
	}
}
