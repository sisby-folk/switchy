package folk.sisby.switchy;

import com.mojang.brigadier.arguments.StringArgumentType;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.presets.SwitchyPreset;
import folk.sisby.switchy.presets.SwitchyPresets;
import folk.sisby.switchy.util.Command;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.quiltmc.qsl.command.api.CommandRegistrationCallback;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;

import java.util.*;
import java.util.function.Predicate;

import static folk.sisby.switchy.Switchy.LOGGER;
import static folk.sisby.switchy.Switchy.MODULE_INFO;
import static folk.sisby.switchy.SwitchyNetworking.S2C_EXPORT;
import static folk.sisby.switchy.util.Command.*;
import static folk.sisby.switchy.util.Feedback.*;

public class SwitchyCommands {
	public static final Map<UUID, String> history = new HashMap<>();

	public static void InitializeCommands() {
		CommandRegistrationCallback.EVENT.register(
				(dispatcher, buildContext, environment) -> dispatcher.register(
						CommandManager.literal("switchy")
								.then(CommandManager.literal("help")
										.executes((c) -> Command.unwrapAndExecute(c, history, SwitchyCommands::displayHelp)))
								.then(CommandManager.literal("list")
										.executes((c) -> unwrapAndExecute(c, history, SwitchyCommands::listPresets)))
								.then(CommandManager.literal("new")
										.then(CommandManager.argument("preset", StringArgumentType.word())
												.executes((c) -> unwrapAndExecute(c, history, SwitchyCommands::newPreset, new Pair<>("preset", String.class)))))
								.then(CommandManager.literal("set")
										.then(CommandManager.argument("preset", StringArgumentType.word())
												.suggests((c, b) -> suggestPresets(c, b, false))
												.executes((c) -> unwrapAndExecute(c, history, SwitchyCommands::setPreset, new Pair<>("preset", String.class)))))
								.then(CommandManager.literal("delete")
										.then(CommandManager.argument("preset", StringArgumentType.word())
												.suggests((c, b) -> suggestPresets(c, b, false))
												.executes((c) -> unwrapAndExecute(c, history, SwitchyCommands::deletePreset, new Pair<>("preset", String.class)))))
								.then(CommandManager.literal("rename")
										.then(CommandManager.argument("preset", StringArgumentType.word())
												.suggests((c, b) -> suggestPresets(c, b, true))
												.then(CommandManager.argument("name", StringArgumentType.word())
														.executes((c) -> unwrapAndExecute(c, history, SwitchyCommands::renamePreset, new Pair<>("preset", String.class), new Pair<>("name", String.class))))))
								.then(CommandManager.literal("module")
										.then(CommandManager.literal("enable")
												.then(CommandManager.argument("module", IdentifierArgumentType.identifier())
														.suggests((c, b) -> suggestModules(c, b, false))
														.executes((c) -> unwrapAndExecute(c, history, SwitchyCommands::enableModule, new Pair<>("module", Identifier.class)))))
										.then(CommandManager.literal("disable")
												.then(CommandManager.argument("module", IdentifierArgumentType.identifier())
														.suggests((c, b) -> suggestModules(c, b, true))
														.executes((c) -> unwrapAndExecute(c, history, SwitchyCommands::disableModule, new Pair<>("module", Identifier.class))))))
								.then(CommandManager.literal("export")
										.requires(source -> {
											ServerPlayerEntity player = serverPlayerOrNull(source);
											return player != null && ServerPlayNetworking.canSend(player, S2C_EXPORT);
										})
										.executes((c) -> unwrapAndExecute(c, history, SwitchyCommands::exportPresets)))
				));

		// switchy set alias
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> dispatcher.register(
				CommandManager.literal("switch")
						.then(CommandManager.argument("preset", StringArgumentType.word())
								.suggests((c, b) -> suggestPresets(c, b, false))
								.executes((c) -> unwrapAndExecute(c, history, SwitchyCommands::setPreset, new Pair<>("preset", String.class)))))
		);
	}

	private static int displayHelp(ServerPlayerEntity player, SwitchyPresets presets) {
		tellHelp(player, "commands.switchy.help.help", "commands.switchy.help.command");
		tellHelp(player, "commands.switchy.list.help", "commands.switchy.list.command");
		tellHelp(player, "commands.switchy.new.help", "commands.switchy.new.command", "commands.switchy.help.placeholder.preset");
		tellHelp(player, "commands.switchy.set.help", "commands.switchy.set.command", "commands.switchy.help.placeholder.preset");
		tellHelp(player, "commands.switch.help", "commands.switch.command", "commands.switchy.help.placeholder.preset");
		tellHelp(player, "commands.switchy.delete.help", "commands.switchy.delete.command", "commands.switchy.help.placeholder.preset");
		tellHelp(player, "commands.switchy.rename.help", "commands.switchy.rename.command", "commands.switchy.help.placeholder.preset", "commands.switchy.help.placeholder.preset");
		tellHelp(player, "commands.switchy.module.enable.help", "commands.switchy.module.enable.command", "commands.switchy.help.placeholder.module");
		tellHelp(player, "commands.switchy.module.disable.help", "commands.switchy.module.disable.command", "commands.switchy.help.placeholder.module");
		tellHelp(player, "commands.switchy.export.help", "commands.switchy.export.command");
		tellHelp(player, "commands.switchy.import.help", "commands.switchy.import.command", "commands.switchy.help.placeholder.file");
		return 11;
	}

	private static int exportPresets(ServerPlayerEntity player, SwitchyPresets presets) {
		try {
			presets.saveCurrentPreset(player);
			PacketByteBuf presetsBuf = PacketByteBufs.create().writeNbt(presets.toNbt());
			ServerPlayNetworking.send(player, S2C_EXPORT, presetsBuf);
			return 1;
		} catch (Exception ex) {
			LOGGER.error(ex.toString());
			LOGGER.error(ex.getMessage());
			sendMessage(player, translatableWithArgs("commands.switchy.export.fail", FORMAT_INVALID));
			return 0;
		}
	}

	private static int listPresets(ServerPlayerEntity player, SwitchyPresets presets) {
		sendMessage(player, translatableWithArgs("commands.switchy.list.presets", FORMAT_INFO, literal(presets.toString())));
		sendMessage(player, translatableWithArgs("commands.switchy.list.modules", FORMAT_INFO, presets.getEnabledModuleText()));
		sendMessage(player, translatableWithArgs("commands.switchy.list.current", FORMAT_INFO, literal(presets.getCurrentPreset().toString())));
		return 1;
	}

	private static int newPreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName) {
		try {
			presets.addPreset(new SwitchyPreset(presetName, presets.modules));
			tellSuccess(player, "commands.switchy.new.success", literal(presetName));
			return 1 + setPreset(player, presets, presetName);
		} catch (IllegalStateException ignored) {
			tellInvalidTry(player, "commands.switchy.new.fail.exists", "commands.switchy.set.command", literal(presetName));
			return 0;
		}
	}

	public static int setPreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName) {
		String oldPresetName = presets.getCurrentPreset().toString();
		try {
			String newPresetName = presets.switchCurrentPreset(player, presetName);
			LOGGER.info("[Switchy] Player switch: '" + oldPresetName + "' -> '" + newPresetName + "' [" + player.getGameProfile().getName() + "]");
			tellSuccess(player, "commands.switchy.set.success", literal(oldPresetName), literal(newPresetName));
			return 1;
		} catch (IllegalArgumentException ignored) {
			tellInvalidTry(player, "commands.switchy.set.fail.missing", "commands.switchy.list.command");
			return 0;
		} catch (IllegalStateException ignored) {
			tellInvalidTry(player, "commands.switchy.set.fail.current", "commands.switchy.list.command");
			return 0;
		}
	}

	private static int renamePreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName, String newName) {
		try {
			presets.renamePreset(presetName, newName);
			tellSuccess(player, "commands.switchy.rename.success", literal(presetName), literal(newName));
			return 1;
		} catch (IllegalArgumentException ignored) {
			tellInvalidTry(player, "commands.switchy.rename.fail.missing", "commands.switchy.list.command");
			return 0;
		} catch (IllegalStateException ignored) {
			tellInvalidTry(player, "commands.switchy.rename.fail.exists", "commands.switchy.list.command");
			return 0;
		}
	}

	private static int deletePreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName) {
		if (!presets.getPresetNames().contains(presetName)) {
			tellInvalidTry(player, "commands.switchy.delete.fail.missing", "commands.switchy.list.command");
			return 0;
		}
		if (presetName.equalsIgnoreCase(Objects.toString(presets.getCurrentPreset(), null))) {
			tellInvalidTry(player, "commands.switchy.delete.fail.current", "commands.switchy.rename.command", literal(""), literal(""));
			return 0;
		}

		if (!history.getOrDefault(player.getUuid(), "").equalsIgnoreCase(command("switchy delete " + presetName))) {
			tellWarn(player, "commands.switchy.delete.warn");
			tellWarn(player, "commands.switchy.list.modules", presets.getEnabledModuleText());
			tellInvalidTry(player, "commands.switchy.delete.confirmation", "commands.switchy.delete.command", literal(presetName));
			return 0;
		} else {
			presets.deletePreset(presetName); // Unsure if we can rectify having both confirmation and throw-errors
			tellSuccess(player, "commands.switchy.delete.success", literal(presetName));
			return 1;
		}
	}

	private static int disableModule(ServerPlayerEntity player, SwitchyPresets presets, Identifier moduleId) {
		if (!presets.getEnabledModules().contains(moduleId)) {
			tellInvalid(player, "commands.switchy.module.disable.fail." + (presets.modules.containsKey(moduleId) ? "disabled" : "missing"), literal(moduleId.toString()));
			return 0;
		}

		if (!history.getOrDefault(player.getUuid(), "").equalsIgnoreCase(command("switchy module disable " + moduleId))) {
			sendMessage(player, MODULE_INFO.get(moduleId).disableConfirmation().setStyle(FORMAT_WARN.getLeft()));
			tellInvalidTry(player, "commands.switchy.module.disable.confirmation", "commands.switchy.module.disable.command", literal(moduleId.toString()));
			return 0;
		} else {
			presets.disableModule(moduleId); // Unsure if we can rectify having both confirmation and throw-errors
			tellSuccess(player, "commands.switchy.module.disable.success", literal(moduleId.toString()));
			return 1;
		}
	}

	private static int enableModule(ServerPlayerEntity player, SwitchyPresets presets, Identifier moduleId) {
		try {
			presets.enableModule(moduleId);
		} catch (IllegalArgumentException ignored) {
			tellInvalid(player, "commands.switchy.module.enable.fail.missing", literal(moduleId.toString()));
			return 0;
		} catch (IllegalStateException ignored) {
			tellInvalid(player, "commands.switchy.module.enable.fail.enabled", literal(moduleId.toString()));
			return 0;
		}
		tellSuccess(player, "commands.switchy.module.enable.success", literal(moduleId.toString()));
		return 1;
	}

	public static boolean confirmAndImportPresets(ServerPlayerEntity player, Map<String, SwitchyPreset> importedPresets, List<Identifier> modules, String command) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();

		// Print info and stop if confirmation is required.
		if (!history.getOrDefault(player.getUuid(), "").equalsIgnoreCase(command(command))) {
			tellWarn(player, "commands.switchy.import.warn.info", literal(String.valueOf(importedPresets.size())), literal(String.valueOf(modules.size())));
			tellWarn(player, "commands.switchy.list.presets", getHighlightedListText(importedPresets.keySet().stream().sorted().toList(), List.of(new Pair<>(presets.getPresetNames()::contains, Formatting.DARK_RED))));
			tellWarn(player, "commands.switchy.import.warn.collision");
			tellWarn(player, "commands.switchy.list.modules", getIdText(modules));
			sendMessage(player, translatableWithArgs("commands.switchy.import.confirmation", FORMAT_INVALID, literal("/" + command)));
			history.put(player.getUuid(), command(command));
			return false;
		}

		// Import
		presets.importFromOther(player, importedPresets);
		tellSuccess(player, "commands.switchy.import.success", literal(String.valueOf(importedPresets.keySet().stream().filter(Predicate.not(presets.getPresetNames()::contains)).toList().size())), literal(String.valueOf(importedPresets.keySet().stream().filter(presets.getPresetNames()::contains).toList().size())));
		return true;
	}
}
