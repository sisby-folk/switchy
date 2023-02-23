package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.presets.SwitchyPresets;
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

public class SwitchyClientServerNetworking {
	// Client API
	public static final Identifier C2S_REQUEST_PRESETS = new Identifier(Switchy.ID, "c2s_export");
	public static final Identifier C2S_REQUEST_DISPLAY_PRESETS = new Identifier(Switchy.ID, "c2s_display_presets");
	public static final Identifier C2S_IMPORT = new Identifier(Switchy.ID, "c2s_import");
	public static final Identifier C2S_SWITCH = new Identifier(Switchy.ID, "c2s_switch");

	// Server Responses
	public static final Identifier S2C_PRESETS = new Identifier(Switchy.ID, "s2c_export");
	public static final Identifier S2C_DISPLAY_PRESETS = new Identifier(Switchy.ID, "s2c_display_presets");

	// Relayed Events
	public static final Identifier S2C_EVENT_SWITCH = new Identifier(Switchy.ID, "s2c_event_switch");

	public static void InitializeReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(C2S_REQUEST_PRESETS, (server, player, handler, buf, sender) -> sendPresets(player));
		ServerPlayNetworking.registerGlobalReceiver(C2S_REQUEST_DISPLAY_PRESETS, (server, player, handler, buf, sender) -> sendDisplayPresets(player));
		ServerPlayNetworking.registerGlobalReceiver(C2S_IMPORT, (server, player, handler, buf, sender) -> importPresets(player, buf.readNbt()));
		ServerPlayNetworking.registerGlobalReceiver(C2S_SWITCH, (server, player, handler, buf, sender) -> SwitchyCommands.setPreset(player, ((SwitchyPlayer) player).switchy$getPresets(), buf.readString()));
	}

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

		SwitchyPresets importedPresets;
		try {
			importedPresets = new SwitchyPresets(false);
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
