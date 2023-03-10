package folk.sisby.switchy;

import folk.sisby.switchy.api.*;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import folk.sisby.switchy.presets.SwitchyPresetsImpl;
import folk.sisby.switchy.util.PresetConverter;
import folk.sisby.switchy.util.SwitchyCommand;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static folk.sisby.switchy.api.module.SwitchyModuleRegistry.getEditable;
import static folk.sisby.switchy.util.Feedback.*;

/**
 * Server-side network handling for client interactions with Switchy.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class SwitchyClientServerNetworking {
	// Data Requests
	/**
	 * Request serialized presets for exporting.
	 */
	public static final Identifier C2S_REQUEST_PRESETS = new Identifier(Switchy.ID, "c2s_presets");
	/**
	 * Request client-compatible serialized presets for client addon use.
	 */
	public static final Identifier C2S_REQUEST_CLIENT_PRESETS = new Identifier(Switchy.ID, "c2s_client_presets");

	// Actions
	/**
	 * Send serialized presets to import.
	 * Must be sent twice to finalize - outputs confirmation text in chat.
	 */
	public static final Identifier C2S_IMPORT_CONFIRM = new Identifier(Switchy.ID, "c2s_import_confirm");
	/**
	 * Send serialized presets to import.
	 */
	public static final Identifier C2S_IMPORT = new Identifier(Switchy.ID, "c2s_import");
	/**
	 * Send switch action with preset name.
	 */
	public static final Identifier C2S_SWITCH = new Identifier(Switchy.ID, "c2s_switch");
	/**
	 * Send new preset action with preset name.
	 */
	public static final Identifier C2S_PRESETS_NEW = new Identifier(Switchy.ID, "c2s_presets_new");
	/**
	 * Send delete preset action with preset name.
	 */
	public static final Identifier C2S_PRESETS_DELETE = new Identifier(Switchy.ID, "c2s_presets_delete");
	/**
	 * Send rename preset action with preset name and new name.
	 */
	public static final Identifier C2S_PRESETS_RENAME = new Identifier(Switchy.ID, "c2s_presets_rename");
	/**
	 * Send disable module action with module ID.
	 */
	public static final Identifier C2S_PRESETS_MODULE_DISABLE = new Identifier(Switchy.ID, "c2s_presets_module_disable");
	/**
	 * Send enable module action with module ID.
	 */
	public static final Identifier C2S_PRESETS_MODULE_ENABLE = new Identifier(Switchy.ID, "c2s_presets_module_enable");

	// Responses
	/**
	 * Serialized presets for exporting.
	 */
	public static final Identifier S2C_PRESETS = new Identifier(Switchy.ID, "s2c_presets");
	/**
	 * Client-compatible serialized presets for previewing.
	 */
	public static final Identifier S2C_CLIENT_PRESETS = new Identifier(Switchy.ID, "s2c_client_presets");

	// Events
	/**
	 * @see SwitchyEvents.Switch
	 */
	public static final Identifier S2C_EVENT_SWITCH = new Identifier(Switchy.ID, "s2c_event_switch");

	// NBT Keys
	/**
	 * The NBT key where the command is stored in import NBT.
	 */
	public static final String KEY_IMPORT_COMMAND = "command";
	/**
	 * The NBT key where the explicitly excluded module IDs are stored in import NBT.
	 */
	public static final String KEY_IMPORT_EXCLUDE = "excludeModules";
	/**
	 * The NBT key where the explicitly included module IDs are stored in import NBT.
	 */
	public static final String KEY_IMPORT_INCLUDE = "includeModules";

	/**
	 * Register server-side receivers for Switchy Client.
	 */
	public static void InitializeReceivers() {
		// Data Requests
		ServerPlayNetworking.registerGlobalReceiver(C2S_REQUEST_PRESETS, (server, player, handler, buf, sender) -> withFeedback(player, buf, (pl, pr, f) -> SwitchyFeedbackStatus.SUCCESS, (pl, pr, oBuf) -> sendPresets(pl, pr, oBuf, buf.readNbt())));
		ServerPlayNetworking.registerGlobalReceiver(C2S_REQUEST_CLIENT_PRESETS, (server, player, handler, buf, sender) -> withFeedback(player, buf, (pl, pr, f) -> SwitchyFeedbackStatus.SUCCESS, SwitchyClientServerNetworking::sendClientPresets));
		// Actions
		ServerPlayNetworking.registerGlobalReceiver(C2S_IMPORT_CONFIRM, (server, player, handler, buf, sender) -> withFeedback(player, buf, (pl, pr, f) -> importPresets(pl, pr, f, buf.readNbt()), SwitchyClientServerNetworking::sendClientPresets));
		ServerPlayNetworking.registerGlobalReceiver(C2S_IMPORT, (server, player, handler, buf, sender) -> withFeedback(player, buf, (pl, pr, f) -> instantImportPresets(pl, pr, f, buf.readNbt()), SwitchyClientServerNetworking::sendClientPresets));
		ServerPlayNetworking.registerGlobalReceiver(C2S_SWITCH, (server, player, handler, buf, sender) -> withFeedback(player, buf, (pl, pr, f) -> SwitchyApi.switchPreset(pl, pr, f, buf.readString()), SwitchyClientServerNetworking::sendClientPresets));
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_NEW, (server, player, handler, buf, sender) -> withFeedback(player, buf, (pl, pr, f) -> SwitchyApi.newPreset(pr, f, buf.readString()), SwitchyClientServerNetworking::sendClientPresets));
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_DELETE, (server, player, handler, buf, sender) -> withFeedback(player, buf, (pl, pr, f) -> {
			String name = buf.readString();
			SwitchyCommands.HISTORY.put(pl.getUuid(), command("switchy delete " + name));
			return SwitchyApi.deletePreset(pl, pr, f, name);
		}, SwitchyClientServerNetworking::sendClientPresets));
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_RENAME, (server, player, handler, buf, sender) -> withFeedback(player, buf, (pl, pr, f) -> SwitchyApi.renamePreset(pr, f, buf.readString(), buf.readString()), SwitchyClientServerNetworking::sendClientPresets));
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_MODULE_DISABLE, (server, player, handler, buf, sender) -> withFeedback(player, buf, (pl, pr, f) -> {
			String id = buf.readString();
			SwitchyCommands.HISTORY.put(pl.getUuid(), command("switchy module disable " + id));
			return SwitchyApi.disableModule(pl, pr, f, Identifier.tryParse(id));
		}, SwitchyClientServerNetworking::sendClientPresets));
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_MODULE_ENABLE, (server, player, handler, buf, sender) -> withFeedback(player, buf, (pl, pr, f) -> SwitchyApi.enableModule(pl, pr, f, Identifier.tryParse(buf.readString())), SwitchyClientServerNetworking::sendClientPresets));
	}

	/**
	 * Set up "relays" that pass switchy events to the client.
	 */
	public static void InitializeRelays() {
		SwitchyEvents.SWITCH.register((player, event) -> ServerPlayNetworking.send(player, S2C_EVENT_SWITCH, PacketByteBufs.create().writeNbt(event.toNbt())));
	}

	private static void withFeedback(ServerPlayerEntity player, PacketByteBuf buf, SwitchyCommand.SwitchyServerCommandExecutor executor, TriConsumer<ServerPlayerEntity, SwitchyPresets, PacketByteBuf> feedbackMethod) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		int listener = -1;
		SwitchyFeedbackStatus status = SwitchyFeedbackStatus.FAIL;
		List<Text> feedback = new ArrayList<>();
		try {
			listener = buf.readInt();
			status = executor.execute(player, presets, feedback::add);
		} catch (Exception ignored) {
		}
		PacketByteBuf outBuf = PacketByteBufs.create();
		outBuf.writeInt(listener);
		outBuf.writeNbt(new SwitchyFeedback(status, feedback).toNbt());
		feedbackMethod.accept(player, presets, outBuf);
	}

	private static void sendClientPresets(ServerPlayerEntity player, SwitchyPresets presets, PacketByteBuf outBuf) {
		presets.saveCurrentPreset(player);
		outBuf.writeNbt(PresetConverter.presetsToNbt(player, presets));
		ServerPlayNetworking.send(player, S2C_CLIENT_PRESETS, outBuf);
	}

	private static void sendPresets(ServerPlayerEntity player, SwitchyPresets presets, PacketByteBuf outBuf, @Nullable NbtCompound nbt) {
		if (nbt != null) {
			NbtList excludes = nbt.getList(KEY_IMPORT_EXCLUDE, NbtElement.STRING_TYPE);
			if (excludes.isEmpty()) {
				ServerPlayNetworking.send(player, S2C_PRESETS, outBuf.writeNbt(presets.toNbt()));
			} else {
				SwitchyPresets exportPresets = new SwitchyPresetsImpl(false);
				exportPresets.fillFromNbt(presets.toNbt());
				excludes.forEach(e -> {
					Identifier id = Identifier.tryParse(e.asString());
					if (id != null && exportPresets.containsModule(id) && exportPresets.isModuleEnabled(id))
						exportPresets.disableModule(id);
				});
				ServerPlayNetworking.send(player, S2C_PRESETS, outBuf.writeNbt(exportPresets.toNbt()));
			}
		}
	}

	private static SwitchyFeedbackStatus instantImportPresets(ServerPlayerEntity player, SwitchyPresets presets, Consumer<Text> feedback, @Nullable NbtCompound presetNbt) {
		SwitchyCommands.HISTORY.put(player.getUuid(), "INSTANT IMPORT");
		if (presetNbt == null) {
			feedback.accept(invalid("commands.switchy_client.import.fail.parse"));
			return SwitchyFeedbackStatus.FAIL;
		}
		presetNbt.putString(KEY_IMPORT_COMMAND, "INSTANT IMPORT");
		return importPresets(player, presets, feedback, presetNbt);
	}

	private static SwitchyFeedbackStatus importPresets(ServerPlayerEntity player, SwitchyPresets presets, Consumer<Text> feedback, @Nullable NbtCompound presetNbt) {

		// Parse Preset NBT //

		if (presetNbt == null || !presetNbt.contains(KEY_IMPORT_COMMAND, NbtElement.STRING_TYPE)) {
			feedback.accept(invalid("commands.switchy_client.import.fail.parse"));
			return SwitchyFeedbackStatus.INVALID;
		}

		SwitchyPresetsImpl importedPresets;
		try {
			importedPresets = new SwitchyPresetsImpl(false);
			importedPresets.fillFromNbt(presetNbt);
		} catch (Exception e) {
			invalid("commands.switchy_client.import.fail.construct");
			return SwitchyFeedbackStatus.FAIL;
		}

		// Parse & Apply Additional Arguments //

		List<Identifier> excludeModules;
		List<Identifier> includeModules;
		try {
			excludeModules = presetNbt.getList(KEY_IMPORT_EXCLUDE, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::new).toList();
			includeModules = presetNbt.getList(KEY_IMPORT_INCLUDE, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::new).toList();
		} catch (InvalidIdentifierException e) {
			feedback.accept(invalid("commands.switchy_client.import.fail.parse"));
			return SwitchyFeedbackStatus.FAIL;
		}

		if (!player.hasPermissionLevel(2) && includeModules.stream().anyMatch(id -> getEditable(id) == SwitchyModuleEditable.OPERATOR)) {
			feedback.accept(warn("commands.switchy_client.import.fail.permission", getIdListText(includeModules)));
			return SwitchyFeedbackStatus.INVALID;
		}

		importedPresets.getModules().forEach((id, enabled) -> {
			if (enabled && (!presets.isModuleEnabled(id) || excludeModules.contains(id) || getEditable(id) == SwitchyModuleEditable.NEVER || (getEditable(id) == SwitchyModuleEditable.OPERATOR && !includeModules.contains(id)))) {
				importedPresets.disableModule(id);
			}
		});

		String command = presetNbt.getString(KEY_IMPORT_COMMAND);

		return SwitchyApi.confirmAndImportPresets(player, importedPresets.getPresets(), importedPresets.getEnabledModules(), command, feedback);
	}
}
