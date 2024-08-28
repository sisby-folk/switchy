package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyApi;
import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.SwitchyFeedback;
import folk.sisby.switchy.api.SwitchyFeedbackStatus;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.api.module.SwitchyModuleEditable;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import folk.sisby.switchy.packet.C2SDeletePreset;
import folk.sisby.switchy.packet.C2SDisableModule;
import folk.sisby.switchy.packet.C2SEnableModule;
import folk.sisby.switchy.packet.C2SExportPresets;
import folk.sisby.switchy.packet.C2SImportPresets;
import folk.sisby.switchy.packet.C2SNewPreset;
import folk.sisby.switchy.packet.C2SPreviewPresets;
import folk.sisby.switchy.packet.C2SRenamePreset;
import folk.sisby.switchy.packet.C2SSwitchPreset;
import folk.sisby.switchy.packet.S2CExportPresets;
import folk.sisby.switchy.packet.S2CPreviewPresets;
import folk.sisby.switchy.packet.S2CSwitchEvent;
import folk.sisby.switchy.presets.SwitchyPresetsImpl;
import folk.sisby.switchy.util.PresetConverter;
import folk.sisby.switchy.util.SwitchyCommand;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static folk.sisby.switchy.api.module.SwitchyModuleRegistry.getEditable;
import static folk.sisby.switchy.util.Feedback.command;
import static folk.sisby.switchy.util.Feedback.getIdListText;
import static folk.sisby.switchy.util.Feedback.invalid;
import static folk.sisby.switchy.util.Feedback.warn;

/**
 * Server-side network handling for client interactions with Switchy.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class SwitchyClientServerNetworking {
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

	static {
		PayloadTypeRegistry.playC2S().register(C2SDeletePreset.ID, C2SDeletePreset.CODEC);
		PayloadTypeRegistry.playC2S().register(C2SDisableModule.ID, C2SDisableModule.CODEC);
		PayloadTypeRegistry.playC2S().register(C2SEnableModule.ID, C2SEnableModule.CODEC);
		PayloadTypeRegistry.playC2S().register(C2SExportPresets.ID, C2SExportPresets.CODEC);
		PayloadTypeRegistry.playC2S().register(C2SImportPresets.ID, C2SImportPresets.CODEC);
		PayloadTypeRegistry.playC2S().register(C2SNewPreset.ID, C2SNewPreset.CODEC);
		PayloadTypeRegistry.playC2S().register(C2SPreviewPresets.ID, C2SPreviewPresets.CODEC);
		PayloadTypeRegistry.playC2S().register(C2SRenamePreset.ID, C2SRenamePreset.CODEC);
		PayloadTypeRegistry.playC2S().register(C2SSwitchPreset.ID, C2SSwitchPreset.CODEC);
		PayloadTypeRegistry.playS2C().register(S2CExportPresets.ID, S2CExportPresets.CODEC);
		PayloadTypeRegistry.playS2C().register(S2CPreviewPresets.ID, S2CPreviewPresets.CODEC);
		PayloadTypeRegistry.playS2C().register(S2CSwitchEvent.ID, S2CSwitchEvent.CODEC);
	}

	/**
	 * Register server-side receivers for Switchy Client.
	 */
	public static void InitializeReceivers() {
		// Data Requests
		ServerPlayNetworking.registerGlobalReceiver(C2SExportPresets.ID, (packet, context) -> withFeedback(context, (pl, pr, f) -> SwitchyFeedbackStatus.SUCCESS, (pl, pr, f) -> sendPresets(packet.listener(), pl, pr, f, packet.presetsNbt())));
		ServerPlayNetworking.registerGlobalReceiver(C2SPreviewPresets.ID, (packet, context) -> withFeedback(context, (pl, pr, f) -> SwitchyFeedbackStatus.SUCCESS, (pl, pr, f) -> sendClientPresets(packet.listener(), pl, pr, f)));
		// Actions
		ServerPlayNetworking.registerGlobalReceiver(C2SImportPresets.ID, (packet, context) -> withFeedback(context, (pl, pr, f) -> packet.confirm() ? importPresets(pl, pr, f, packet.presetsNbt()) : instantImportPresets(pl, pr, f, packet.presetsNbt()), (pl, pr, f) -> sendClientPresets(packet.listener(), pl, pr, f)));
		ServerPlayNetworking.registerGlobalReceiver(C2SSwitchPreset.ID, (packet, context) -> withFeedback(context, (pl, pr, f) -> SwitchyApi.switchPreset(pl, pr, f, packet.name()), (pl, pr, f) -> sendClientPresets(packet.listener(), pl, pr, f)));
		ServerPlayNetworking.registerGlobalReceiver(C2SNewPreset.ID, (packet, context) -> withFeedback(context, (pl, pr, f) -> SwitchyApi.newPreset(pr, f, packet.name()), (pl, pr, f) -> sendClientPresets(packet.listener(), pl, pr, f)));
		ServerPlayNetworking.registerGlobalReceiver(C2SDeletePreset.ID, (packet, context) -> withFeedback(context, (pl, pr, f) -> {
			String name = packet.name();
			SwitchyCommands.HISTORY.put(pl.getUuid(), command("switchy delete " + name));
			return SwitchyApi.deletePreset(pl, pr, f, name);
		}, (pl, pr, f) -> sendClientPresets(packet.listener(), pl, pr, f)));
		ServerPlayNetworking.registerGlobalReceiver(C2SRenamePreset.ID, (packet, context) -> withFeedback(context, (pl, pr, f) -> SwitchyApi.renamePreset(pr, f, packet.oldName(), packet.newName()), (pl, pr, f) -> sendClientPresets(packet.listener(), pl, pr, f)));
		ServerPlayNetworking.registerGlobalReceiver(C2SDisableModule.ID, (packet, context) -> withFeedback(context, (pl, pr, f) -> {
			Identifier id = packet.id();
			SwitchyCommands.HISTORY.put(pl.getUuid(), command("switchy module disable " + id.toString()));
			return SwitchyApi.disableModule(pl, pr, f, id);
		}, (pl, pr, f) -> sendClientPresets(packet.listener(), pl, pr, f)));
		ServerPlayNetworking.registerGlobalReceiver(C2SEnableModule.ID, (packet, context) -> withFeedback(context, (pl, pr, f) -> SwitchyApi.enableModule(pl, pr, f, packet.id()), (pl, pr, f) -> sendClientPresets(packet.listener(), pl, pr, f)));
	}

	/**
	 * Set up "relays" that pass switchy events to the client.
	 */
	public static void InitializeRelays() {
		SwitchyEvents.SWITCH.register((player, event) -> ServerPlayNetworking.send(player, new S2CSwitchEvent(event.toNbt())));
	}

	private static void withFeedback(ServerPlayNetworking.Context context, SwitchyCommand.SwitchyServerCommandExecutor executor, TriConsumer<ServerPlayerEntity, SwitchyPresets, SwitchyFeedback> feedbackMethod) {
		ServerPlayerEntity player = context.player();
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		SwitchyFeedbackStatus status = SwitchyFeedbackStatus.FAIL;
		List<Text> feedback = new ArrayList<>();
		try {
			status = executor.execute(player, presets, feedback::add);
		} catch (Exception ignored) {
		}
		feedbackMethod.accept(player, presets, new SwitchyFeedback(status, feedback));
	}

	private static void sendClientPresets(int listener, ServerPlayerEntity player, SwitchyPresets presets, SwitchyFeedback feedback) {
		presets.saveCurrentPreset(player);
		NbtCompound presetsNbt = PresetConverter.presetsToNbt(player, presets);
		ServerPlayNetworking.send(player, new S2CPreviewPresets(listener, feedback.toNbt(player), presetsNbt));
	}

	private static void sendPresets(int listener, ServerPlayerEntity player, SwitchyPresets presets, SwitchyFeedback feedback, @Nullable NbtCompound nbt) {
		if (nbt != null) {
			NbtList excludes = nbt.getList(KEY_IMPORT_EXCLUDE, NbtElement.STRING_TYPE);
			if (excludes.isEmpty()) {
				ServerPlayNetworking.send(player, new S2CExportPresets(listener, feedback.toNbt(player), presets.toNbt()));
			} else {
				SwitchyPresets exportPresets = new SwitchyPresetsImpl(false);
				exportPresets.fillFromNbt(presets.toNbt());
				excludes.forEach(e -> {
					Identifier id = Identifier.tryParse(e.asString());
					if (id != null && exportPresets.containsModule(id) && exportPresets.isModuleEnabled(id))
						exportPresets.disableModule(id);
				});
				ServerPlayNetworking.send(player, new S2CExportPresets(listener, feedback.toNbt(player), exportPresets.toNbt()));
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
			excludeModules = presetNbt.getList(KEY_IMPORT_EXCLUDE, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::tryParse).toList();
			includeModules = presetNbt.getList(KEY_IMPORT_INCLUDE, NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::tryParse).toList();
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

	public static void touch() {
	}
}
