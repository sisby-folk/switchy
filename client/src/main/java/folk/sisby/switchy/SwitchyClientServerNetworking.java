package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import folk.sisby.switchy.presets.SwitchyPresetsImpl;
import folk.sisby.switchy.util.PresetConverter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
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
	 * Request displayable serialized presets for previewing.
	 */
	public static final Identifier C2S_REQUEST_DISPLAY_PRESETS = new Identifier(Switchy.ID, "c2s_display_presets");

	// Actions
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
	 * Displayable serialized presets for previewing.
	 */
	public static final Identifier S2C_DISPLAY_PRESETS = new Identifier(Switchy.ID, "s2c_display_presets");

	// Events
	/**
	 * @see SwitchyEvents.Switch
	 */
	public static final Identifier S2C_EVENT_SWITCH = new Identifier(Switchy.ID, "s2c_event_switch");

	/**
	 * Register server-side receivers for Switchy Client.
	 */
	public static void InitializeReceivers() {
		// Data Requests
		ServerPlayNetworking.registerGlobalReceiver(C2S_REQUEST_PRESETS, (server, player, handler, buf, sender) -> sendPresets(player));
		ServerPlayNetworking.registerGlobalReceiver(C2S_REQUEST_DISPLAY_PRESETS, (server, player, handler, buf, sender) -> sendDisplayPresets(player));
		// Actions
		ServerPlayNetworking.registerGlobalReceiver(C2S_IMPORT, (server, player, handler, buf, sender) -> importPresets(player, buf.readNbt()));
		ServerPlayNetworking.registerGlobalReceiver(C2S_SWITCH, (server, player, handler, buf, sender) -> {
			SwitchyCommands.switchPreset(player, ((SwitchyPlayer) player).switchy$getPresets(), buf.readString());
			sendDisplayPresets(player);
		});
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_NEW, (server, player, handler, buf, sender) -> {
			SwitchyCommands.newPreset(player, ((SwitchyPlayer) player).switchy$getPresets(), buf.readString());
			sendDisplayPresets(player);
		});
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_DELETE, (server, player, handler, buf, sender) -> {
			String name = buf.readString();
			SwitchyCommands.HISTORY.put(player.getUuid(), command("switchy delete " + name));
			SwitchyCommands.deletePreset(player, ((SwitchyPlayer) player).switchy$getPresets(), name);
			sendDisplayPresets(player);
		});
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_RENAME, (server, player, handler, buf, sender) -> {
			SwitchyCommands.renamePreset(player, ((SwitchyPlayer) player).switchy$getPresets(), buf.readString(), buf.readString());
			sendDisplayPresets(player);
		});
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_MODULE_DISABLE, (server, player, handler, buf, sender) -> {
			String id = buf.readString();
			SwitchyCommands.HISTORY.put(player.getUuid(), command("switchy module disable " + id));
			SwitchyCommands.disableModule(player, ((SwitchyPlayer) player).switchy$getPresets(), Identifier.tryParse(id));
			sendDisplayPresets(player);
		});
		ServerPlayNetworking.registerGlobalReceiver(C2S_PRESETS_MODULE_ENABLE, (server, player, handler, buf, sender) -> {
			SwitchyCommands.enableModule(player, ((SwitchyPlayer) player).switchy$getPresets(), Identifier.tryParse(buf.readString()));
			sendDisplayPresets(player);
		});
	}

	/**
	 * Set up "relays" that pass switchy events to the client.
	 */
	public static void InitializeRelays() {
		SwitchyEvents.SWITCH.register((player, event) -> ServerPlayNetworking.send(player, S2C_EVENT_SWITCH, PacketByteBufs.create().writeNbt(event.toNbt())));
	}

	private static void sendDisplayPresets(ServerPlayerEntity player) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		presets.saveCurrentPreset(player);
		PacketByteBuf displayPresetsBuf = PacketByteBufs.create().writeNbt(PresetConverter.presetsToNbt(presets));
		ServerPlayNetworking.send(player, S2C_DISPLAY_PRESETS, displayPresetsBuf);
	}

	private static void sendPresets(ServerPlayerEntity player) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		try {
			presets.saveCurrentPreset(player);
			PacketByteBuf presetsBuf = PacketByteBufs.create().writeNbt(presets.toNbt());
			ServerPlayNetworking.send(player, S2C_PRESETS, presetsBuf);
		} catch (Exception ex) {
			LOGGER.error(ex.toString());
			LOGGER.error(ex.getMessage());
			sendMessage(player, translatableWithArgs("commands.switchy.export.fail", FORMAT_INVALID));
		}
	}

	private static void importPresets(ServerPlayerEntity player, @Nullable NbtCompound presetNbt) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();

		// Parse Preset NBT //

		if (presetNbt == null || !presetNbt.contains("command", NbtElement.STRING_TYPE)) {
			tellInvalid(player, "commands.switchy.import.fail.parse");
			return;
		}

		SwitchyPresetsImpl importedPresets;
		try {
			importedPresets = new SwitchyPresetsImpl(false);
			importedPresets.fillFromNbt(presetNbt);
		} catch (Exception e) {
			tellInvalid(player, "commands.switchy.import.fail.construct");
			return;
		}

		// Parse & Apply Additional Arguments //

		List<Identifier> excludeModules;
		List<Identifier> opModules;
		try {
			excludeModules = presetNbt.getList("excludeModules", NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::new).toList();
			opModules = presetNbt.getList("opModules", NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::new).toList();
		} catch (InvalidIdentifierException e) {
			tellInvalid(player, "commands.switchy.import.fail.parse");
			return;
		}

		if (!opModules.isEmpty() && player.hasPermissionLevel(2)) {
			tellWarn(player, "commands.switchy.import.fail.permission", getIdListText(opModules));
			return;
		}

		importedPresets.getModules().forEach((id, enabled) -> {
			if (enabled && (!presets.containsModule(id) || !presets.isModuleEnabled(id) || excludeModules.contains(id) || getEditable(id) == SwitchyModuleEditable.NEVER || (getEditable(id) == SwitchyModuleEditable.OPERATOR && !opModules.contains(id)))) {
				importedPresets.disableModule(id);
			}
		});

		String command = presetNbt.getString("command");

		SwitchyCommands.confirmAndImportPresets(player, importedPresets.getPresets(), importedPresets.getEnabledModules(), command);
	}
}
