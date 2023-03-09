package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import folk.sisby.switchy.client.api.SwitchyFeedbackStatus;
import folk.sisby.switchy.client.api.SwitchyRequestFeedback;
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
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;

import java.util.List;

import static folk.sisby.switchy.Switchy.LOGGER;
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
		ServerPlayNetworking.registerGlobalReceiver(C2S_REQUEST_PRESETS, (server, player, handler, buf, sender) -> sendPresetsWithFeedback(player, buf));
		ServerPlayNetworking.registerGlobalReceiver(C2S_REQUEST_CLIENT_PRESETS, (server, player, handler, buf, sender) -> withFeedback(player, buf, (p, ps) -> {}));
		// Actions
		ServerPlayNetworking.registerGlobalReceiver(C2S_IMPORT_CONFIRM, (server, player, handler, buf, sender) -> withFeedback(player, buf, (p, ps) -> importPresets(p, buf.readNbt())));
		ServerPlayNetworking.registerGlobalReceiver(C2S_IMPORT, (server, player, handler, buf, sender) -> withFeedback(player, buf, (p, ps) -> instantImportPresets(player, buf.readNbt())));
		ServerPlayNetworking.registerGlobalReceiver(C2S_SWITCH, (server, player, handler, buf, sender) -> withFeedback(player, buf, (p, ps) -> SwitchyCommands.switchPreset(p, ps, buf.readString())));
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_NEW, (server, player, handler, buf, sender) -> withFeedback(player, buf, (p, ps) -> SwitchyCommands.newPreset(p, ps, buf.readString())));
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_DELETE, (server, player, handler, buf, sender) -> withFeedback(player, buf, (p, ps) -> {
			String name = buf.readString();
			SwitchyCommands.HISTORY.put(player.getUuid(), command("switchy delete " + name));
			SwitchyCommands.deletePreset(player, ((SwitchyPlayer) player).switchy$getPresets(), name);
		}));
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_RENAME, (server, player, handler, buf, sender) -> withFeedback(player, buf, (p, ps) -> SwitchyCommands.renamePreset(p, ps, buf.readString(), buf.readString())));
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_MODULE_DISABLE, (server, player, handler, buf, sender) -> withFeedback(player, buf, (p, ps) -> {
			String id = buf.readString();
			SwitchyCommands.HISTORY.put(player.getUuid(), command("switchy module disable " + id));
			SwitchyCommands.disableModule(player, ((SwitchyPlayer) player).switchy$getPresets(), Identifier.tryParse(id));
		}));
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_MODULE_ENABLE, (server, player, handler, buf, sender) -> withFeedback(player, buf, (p, ps) -> SwitchyCommands.enableModule(player, ((SwitchyPlayer) player).switchy$getPresets(), Identifier.tryParse(buf.readString()))));
	}

	/**
	 * Set up "relays" that pass switchy events to the client.
	 */
	public static void InitializeRelays() {
		SwitchyEvents.SWITCH.register((player, event) -> ServerPlayNetworking.send(player, S2C_EVENT_SWITCH, PacketByteBufs.create().writeNbt(event.toNbt())));
	}

	private static void withFeedback(ServerPlayerEntity player, PacketByteBuf buf, SwitchyCommand.SwitchyServerCommandExecutor executor) {
		int listener = -1;
		try {
			listener = buf.readInt();
			executor.execute(player, ((SwitchyPlayer) player).switchy$getPresets());
			sendClientPresets(player, listener, new SwitchyRequestFeedback(SwitchyFeedbackStatus.SUCCESS, List.of(Text.literal("EXAMPLE MESSAGE"))));
		} catch (Exception ex) {
			sendClientPresets(player, listener, new SwitchyRequestFeedback(SwitchyFeedbackStatus.FAILURE, List.of(Text.literal(ex.getMessage()))));
		}
	}

	private static void sendClientPresets(ServerPlayerEntity player, int listener, SwitchyRequestFeedback feedback) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		presets.saveCurrentPreset(player);
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(listener);
		buf.writeNbt(feedback.toNbt());
		buf.writeNbt(PresetConverter.presetsToNbt(player, presets));
		ServerPlayNetworking.send(player, S2C_CLIENT_PRESETS, buf);
	}

	private static void sendPresetsWithFeedback(ServerPlayerEntity player, PacketByteBuf buf) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		int listener = -1;
		PacketByteBuf outBuf = PacketByteBufs.create();
		try {
			listener = buf.readInt();
			outBuf.writeInt(listener);
			presets.saveCurrentPreset(player);
			NbtCompound nbt = buf.readNbt();
			if (nbt != null) {
				NbtList excludes = nbt.getList(KEY_IMPORT_EXCLUDE, NbtElement.STRING_TYPE);
				if (excludes.isEmpty()) {
					outBuf.writeNbt(new SwitchyRequestFeedback(SwitchyFeedbackStatus.SUCCESS, List.of(Text.literal("Exported"))).toNbt()); // TODO: Real text.
					ServerPlayNetworking.send(player, S2C_PRESETS, outBuf.writeNbt(presets.toNbt()));
				} else {
					SwitchyPresets exportPresets = new SwitchyPresetsImpl(false);
					exportPresets.fillFromNbt(presets.toNbt());
					excludes.forEach(e -> {
						Identifier id = Identifier.tryParse(e.asString());
						if (id != null && exportPresets.containsModule(id) && exportPresets.isModuleEnabled(id))
							exportPresets.disableModule(id);
					});
					outBuf.writeNbt(new SwitchyRequestFeedback(SwitchyFeedbackStatus.SUCCESS, List.of(Text.literal("Exported"))).toNbt()); // TODO: Real text.
					ServerPlayNetworking.send(player, S2C_PRESETS, outBuf.writeNbt(exportPresets.toNbt()));
				}
			}
		} catch (Exception ex) {
			LOGGER.error("Saving to file failed!", ex);
			if (listener == -1) outBuf.writeInt(listener);
			outBuf.writeNbt(new SwitchyRequestFeedback(SwitchyFeedbackStatus.FAILURE, List.of(Text.literal("Didn't Export"))).toNbt()); // TODO: Real text.
			sendMessage(player, translatableWithArgs("commands.switchy_client.export.fail", FORMAT_INVALID));
			ServerPlayNetworking.send(player, S2C_PRESETS, outBuf.writeNbt(new NbtCompound()));
		}
	}

	private static void instantImportPresets(ServerPlayerEntity player, @Nullable NbtCompound presetNbt) {
		SwitchyCommands.HISTORY.put(player.getUuid(), "INSTANT IMPORT");
		if (presetNbt == null) {
			tellInvalid(player, "commands.switchy_client.import.fail.parse");
			return;
		}
		presetNbt.putString(KEY_IMPORT_COMMAND, "INSTANT IMPORT");
		importPresets(player, presetNbt);
	}

	private static void importPresets(ServerPlayerEntity player, @Nullable NbtCompound presetNbt) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();

		// Parse Preset NBT //

		if (presetNbt == null || !presetNbt.contains(KEY_IMPORT_COMMAND, NbtElement.STRING_TYPE)) {
			tellInvalid(player, "commands.switchy_client.import.fail.parse");
			return;
		}

		SwitchyPresetsImpl importedPresets;
		try {
			importedPresets = new SwitchyPresetsImpl(false);
			importedPresets.fillFromNbt(presetNbt);
		} catch (Exception e) {
			tellInvalid(player, "commands.switchy_client.import.fail.construct");
			return;
		}

		// Parse & Apply Additional Arguments //

		List<Identifier> excludeModules;
		List<Identifier> includeModules;
		try {
			excludeModules = presetNbt.getList(KEY_IMPORT_EXCLUDE, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::new).toList();
			includeModules = presetNbt.getList(KEY_IMPORT_INCLUDE, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::new).toList();
		} catch (InvalidIdentifierException e) {
			tellInvalid(player, "commands.switchy_client.import.fail.parse");
			return;
		}

		if (!player.hasPermissionLevel(2) && includeModules.stream().anyMatch(id -> getEditable(id) == SwitchyModuleEditable.OPERATOR)) {
			tellWarn(player, "commands.switchy_client.import.fail.permission", getIdListText(includeModules));
			return;
		}

		importedPresets.getModules().forEach((id, enabled) -> {
			if (enabled && (!presets.isModuleEnabled(id) || excludeModules.contains(id) || getEditable(id) == SwitchyModuleEditable.NEVER || (getEditable(id) == SwitchyModuleEditable.OPERATOR && !includeModules.contains(id)))) {
				importedPresets.disableModule(id);
			}
		});

		String command = presetNbt.getString(KEY_IMPORT_COMMAND);

		SwitchyCommands.confirmAndImportPresets(player, importedPresets.getPresets(), importedPresets.getEnabledModules(), command);
	}
}
