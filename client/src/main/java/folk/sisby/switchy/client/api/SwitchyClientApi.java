package folk.sisby.switchy.client.api;

import com.mojang.brigadier.StringReader;
import folk.sisby.switchy.api.SwitchyFeedback;
import folk.sisby.switchy.api.exception.InvalidWordException;
import folk.sisby.switchy.api.module.presets.SwitchyClientPresets;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import folk.sisby.switchy.client.SwitchyClient;
import folk.sisby.switchy.client.util.SwitchyFiles;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
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
@SuppressWarnings("deprecation")
public class SwitchyClientApi {
	/**
	 * Map of listeners for API calls waiting for client preset returns.
	 */
	@ApiStatus.Internal
	public static final Map<Integer, BiConsumer<SwitchyFeedback, SwitchyClientPresets>> API_RESPONSE_LISTENERS = new HashMap<>();
	/**
	 * Map of listeners for API calls waiting for exported preset NBT returns.
	 */
	@ApiStatus.Internal
	public static final Map<Integer, BiConsumer<SwitchyFeedback, NbtCompound>> API_EXPORT_LISTENERS = new HashMap<>();
	private static int nextId = 0;
	private static int nextExportId = 0;

	private static PacketByteBuf createSwitchyByteBuf(BiConsumer<SwitchyFeedback, SwitchyClientPresets> listener) {
		int id = nextId++;
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(id);
		API_RESPONSE_LISTENERS.put(id, listener);
		return buf;
	}

	private static PacketByteBuf createSwitchyExportByteBuf(BiConsumer<SwitchyFeedback, NbtCompound> listener) {
		int id = nextExportId++;
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(id);
		API_EXPORT_LISTENERS.put(id, listener);
		return buf;
	}

	/**
	 * Gets whether the server has switchy installed.
	 *
	 * @return true if connected to a server with Switchy installed, false otherwise.
	 */
	public static boolean isSwitchyServer() {
		return ClientPlayNetworking.canSend(C2S_REQUEST_CLIENT_PRESETS);
	}

	/**
	 * Gets the export folder used by Switchy Client's import/export functionality.
	 *
	 * @return the export folder file.
	 */
	public static File getExportFolder() {
		return new File(SwitchyClient.EXPORT_PATH);
	}

	/**
	 * Gets the list of .dat files in the export folder.
	 *
	 * @return the .dat files currently in the export folder.
	 */
	public static List<File> getImportableFiles() {
		return SwitchyFiles.filesWithExtension(getExportFolder(), "dat");
	}

	/**
	 * Requests presets from the server in a client-compatible format.
	 *
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#switchCurrentPreset(net.minecraft.server.network.ServerPlayerEntity, String)
	 */
	public static void getClientPresets(BiConsumer<SwitchyFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_REQUEST_CLIENT_PRESETS))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = createSwitchyByteBuf(responseCallback);
		ClientPlayNetworking.send(C2S_REQUEST_CLIENT_PRESETS, buf);
	}

	/**
	 * Switches to the specified preset on the server.
	 *
	 * @param name             the case-insensitive name of a preset.
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#switchCurrentPreset(net.minecraft.server.network.ServerPlayerEntity, String)
	 */
	public static void switchCurrentPreset(String name, BiConsumer<SwitchyFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_SWITCH))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = createSwitchyByteBuf(responseCallback);
		buf.writeString(name);
		ClientPlayNetworking.send(C2S_SWITCH, buf);
	}

	/**
	 * Creates a new preset on the server.
	 *
	 * @param name             the case-insensitive name of a preset.
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @throws InvalidWordException          when the specified preset name is not a word ({@link StringReader#isAllowedInUnquotedString(char)}).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#newPreset(String)
	 */
	public static void newPreset(String name, BiConsumer<SwitchyFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException, InvalidWordException {
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
	 * @param name             the case-insensitive name of a preset.
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#deletePreset(String)
	 */
	public static void deletePreset(String name, BiConsumer<SwitchyFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_PRESETS_DELETE))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = createSwitchyByteBuf(responseCallback);
		buf.writeString(name);
		ClientPlayNetworking.send(C2S_PRESETS_DELETE, buf);
	}

	/**
	 * Changes the name of the specified preset on the server.
	 *
	 * @param name             the case-insensitive name of a preset.
	 * @param newName          the new name for the specified preset. a single word matching {@code azAZ09_-.+}.
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#renamePreset(String, String)
	 */
	public static void renamePreset(String name, String newName, BiConsumer<SwitchyFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
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
	 * @param id               a module identifier.
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#disableModule(Identifier)
	 */
	public static void disableModule(Identifier id, BiConsumer<SwitchyFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_PRESETS_MODULE_DISABLE))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		PacketByteBuf buf = createSwitchyByteBuf(responseCallback);
		buf.writeString(id.toString());
		ClientPlayNetworking.send(C2S_PRESETS_MODULE_DISABLE, buf);
	}

	/**
	 * Enables the specified module on the server.
	 *
	 * @param id               a module identifier.
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#enableModule(Identifier)
	 */
	public static void enableModule(Identifier id, BiConsumer<SwitchyFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
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

	private static void doImport(NbtCompound presetsNbt, Collection<Identifier> excludeModules, Collection<Identifier> includeModules, @Nullable String command, BiConsumer<SwitchyFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(command != null ? C2S_IMPORT_CONFIRM : C2S_IMPORT))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		writeModuleSpecifiers(presetsNbt, excludeModules, includeModules);
		if (command != null) presetsNbt.putString(KEY_IMPORT_COMMAND, command);
		PacketByteBuf buf = createSwitchyByteBuf(responseCallback);
		ClientPlayNetworking.send(command != null ? C2S_IMPORT_CONFIRM : C2S_IMPORT, buf.writeNbt(presetsNbt));
	}

	/**
	 * Import the provided Presets NBT without confirmation.
	 *
	 * @param presetsNbt       the NBT of presets and modules to import.
	 * @param excludeModules   A collection of modules to not import from the NBT, even if they are allowed.
	 * @param includeModules   A collection of modules to always import from the NBT, even if they require operator.
	 * @param responseCallback the callback for the response from the server.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#importFromOther(ServerPlayerEntity, SwitchyPresets)
	 */
	public static void importPresets(NbtCompound presetsNbt, Collection<Identifier> excludeModules, Collection<Identifier> includeModules, BiConsumer<SwitchyFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		doImport(presetsNbt, excludeModules, includeModules, null, responseCallback);
	}

	/**
	 * Import the provided Presets NBT with chat-based confirmation.
	 * Must run twice with the same confirmation command to complete.
	 *
	 * @param presetsNbt          the NBT of presets and modules to import.
	 * @param excludeModules      A collection of modules to not import from the NBT, even if they are allowed.
	 * @param includeModules      A collection of modules to always import from the NBT, even if they require operator.
	 * @param responseCallback    the callback for the response from the server.
	 * @param confirmationCommand the command to prompt the player to re-enter to confirm.
	 * @throws UnsupportedOperationException when the channel ID is not recognized by the server (Switchy Client is not installed).
	 * @see folk.sisby.switchy.api.presets.SwitchyPresets#importFromOther(ServerPlayerEntity, SwitchyPresets)
	 */
	public static void importPresets(NbtCompound presetsNbt, Collection<Identifier> excludeModules, Collection<Identifier> includeModules, String confirmationCommand, BiConsumer<SwitchyFeedback, SwitchyClientPresets> responseCallback) throws UnsupportedOperationException {
		doImport(presetsNbt, excludeModules, includeModules, confirmationCommand, responseCallback);
	}

	/**
	 * Export the player's presets to NBT.
	 *
	 * @param excludeModules   A collection of modules to not export to the NBT, if they exist.
	 * @param responseCallback the callback for the response from the server.
	 * @see folk.sisby.switchy.client.SwitchyClientReceivers
	 */
	public static void exportPresets(Collection<Identifier> excludeModules, BiConsumer<SwitchyFeedback, NbtCompound> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_REQUEST_PRESETS))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		NbtCompound nbt = new NbtCompound();
		writeModuleSpecifiers(nbt, excludeModules, List.of());
		PacketByteBuf buf = createSwitchyExportByteBuf(responseCallback);
		ClientPlayNetworking.send(C2S_REQUEST_PRESETS, buf.writeNbt(nbt));
	}

	/**
	 * Export the player's presets to a file.
	 *
	 * @param excludeModules   A collection of modules to not export to the NBT, if they exist.
	 * @param filename         the name of the file in the export folder to write to, without extension.
	 *                         If null, file is automatically named based on the current time and whether in singleplayer.
	 * @param responseCallback the callback for the response from the server.
	 * @see folk.sisby.switchy.client.SwitchyClientReceivers
	 */
	public static void exportPresetsToFile(Collection<Identifier> excludeModules, @Nullable String filename, BiConsumer<SwitchyFeedback, File> responseCallback) throws UnsupportedOperationException {
		if (!ClientPlayNetworking.canSend(C2S_REQUEST_PRESETS))
			throw new UnsupportedOperationException("Server does not have Switchy Client installed");
		NbtCompound nbt = new NbtCompound();
		writeModuleSpecifiers(nbt, excludeModules, List.of());
		PacketByteBuf buf = createSwitchyExportByteBuf((f, n) -> responseCallback.accept(f, SwitchyFiles.exportNbtToFile(filename != null ? filename : (MinecraftClient.getInstance().isInSingleplayer() ? "Singleplayer_" : "Multiplayer_") + new SimpleDateFormat("MMM-dd_HH-mm-ss").format(new java.util.Date()), n, f.messages()::add)));
		ClientPlayNetworking.send(C2S_REQUEST_PRESETS, buf.writeNbt(nbt));
	}
}
