package folk.sisby.switchy.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.SwitchyPlayer;
import folk.sisby.switchy.SwitchyPreset;
import folk.sisby.switchy.SwitchyPresets;
import folk.sisby.switchy.api.ModuleImportable;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.command.api.CommandRegistrationCallback;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static folk.sisby.switchy.Switchy.C2S_IMPORT;
import static folk.sisby.switchy.util.Feedback.*;

public class SwitchyCommands {
	private static final Map<UUID, String> last_command = new HashMap<>();

	public static void InitializeCommands() {
		CommandRegistrationCallback.EVENT.register(
				(dispatcher, buildContext, environment) -> dispatcher.register(
						CommandManager.literal("switchy")
								.then(CommandManager.literal("help")
										.executes((c) -> unwrapAndExecute(c, SwitchyCommands::displayHelp)))
								.then(CommandManager.literal("list")
										.executes((c) -> unwrapAndExecute(c, SwitchyCommands::listPresets)))
								.then(CommandManager.literal("new")
										.then(CommandManager.argument("preset", StringArgumentType.word())
												.executes((c) -> unwrapAndExecute(c, SwitchyCommands::newPreset, new Pair<>("preset", String.class)))))
								.then(CommandManager.literal("set")
										.then(CommandManager.argument("preset", StringArgumentType.word())
												.suggests((c, b) -> suggestPresets(c, b, false))
												.executes((c) -> unwrapAndExecute(c, SwitchyCommands::setPreset, new Pair<>("preset", String.class)))))
								.then(CommandManager.literal("delete")
										.then(CommandManager.argument("preset", StringArgumentType.word())
												.suggests((c, b) -> suggestPresets(c, b, false))
												.executes((c) -> unwrapAndExecute(c, SwitchyCommands::deletePreset, new Pair<>("preset", String.class)))))
								.then(CommandManager.literal("rename")
										.then(CommandManager.argument("preset", StringArgumentType.word())
												.suggests((c, b) -> suggestPresets(c, b, true))
												.then(CommandManager.argument("name", StringArgumentType.word())
														.executes((c) -> unwrapAndExecute(c, SwitchyCommands::renamePreset, new Pair<>("preset", String.class), new Pair<>("name", String.class))))))
								.then(CommandManager.literal("module")
										.then(CommandManager.literal("enable")
												.then(CommandManager.argument("module", IdentifierArgumentType.identifier())
														.suggests((c, b) -> suggestModules(c, b, false))
														.executes((c) -> unwrapAndExecute(c, SwitchyCommands::enableModule, new Pair<>("module", Identifier.class)))))
										.then(CommandManager.literal("disable")
												.then(CommandManager.argument("module", IdentifierArgumentType.identifier())
														.suggests((c, b) -> suggestModules(c, b, true))
														.executes((c) -> unwrapAndExecute(c, SwitchyCommands::disableModule, new Pair<>("module", Identifier.class))))))
								.then(CommandManager.literal("export")
										.executes((c) -> unwrapAndExecute(c, SwitchyCommands::exportPresets)))
				));

		// switchy set alias
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> dispatcher.register(
				CommandManager.literal("switch")
						.then(CommandManager.argument("preset", StringArgumentType.word())
								.suggests((c, b) -> suggestPresets(c, b, false))
								.executes((c) -> unwrapAndExecute(c, SwitchyCommands::setPreset, new Pair<>("preset", String.class)))))
		);
	}

	public static void InitializeReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(C2S_IMPORT, (server, player, handler, buf, sender) -> {
			// Parse as NBT
			NbtCompound presetNbt = buf.readNbt();
			if (presetNbt == null || !presetNbt.contains("filename")) {
				tellInvalid(player, "commands.switchy.import.fail.parse");
				return;
			}
			String filename = presetNbt.getString("filename");

			// Parse Identifier Flags
			List<Identifier> addModules = new ArrayList<>();
			List<Identifier> removeModules = new ArrayList<>();
			try {
				presetNbt.getList("addModules", NbtElement.STRING_TYPE).forEach(
						e -> addModules.add(new Identifier(e.asString()))
				);
				presetNbt.getList("removeModules", NbtElement.STRING_TYPE).forEach(
						e -> removeModules.add(new Identifier(e.asString()))
				);
			} catch (InvalidIdentifierException e) {
				tellInvalid(player, "commands.switchy.import.fail.parse");
				return;
			}

			// Construct presets from NBT
			SwitchyPresets importedPresets;
			try {
				importedPresets = SwitchyPresets.fromNbt(presetNbt, null);
			} catch (Exception e) {
				tellInvalid(player, "commands.switchy.import.fail.construct");
				return;
			}

			// Get player presets
			if (((SwitchyPlayer) player).switchy$getPresets() == null) {
				((SwitchyPlayer) player).switchy$setPresets(SwitchyPresets.fromNbt(new NbtCompound(), player));
			}
			SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();

			// Generate Importable List
			Map<Identifier, ModuleImportable> configuredImportable = Switchy.COMPAT_REGISTRY.entrySet().stream().collect(Collectors.toMap(
					Map.Entry::getKey,
					entry -> Switchy.IMPORTABLE_CONFIGURABLE.contains(entry.getValue().get().getImportable()) ? Switchy.CONFIG.moduleImportable.get(entry.getKey().toString()) : entry.getValue().get().getImportable()
			));

			// Warn user about any disallowed flags

			// Generate list of modules being imported
			List<Identifier> modules = Switchy.COMPAT_REGISTRY.keySet().stream().filter(
					(key) -> presets.getModuleToggles().containsKey(key) && importedPresets.getModuleToggles().containsKey(key)
							&& (((configuredImportable.get(key) == ModuleImportable.ALLOWED || configuredImportable.get(key) == ModuleImportable.ALWAYS_ALLOWED) && !removeModules.contains(key)) ||
							(configuredImportable.get(key) == ModuleImportable.OPERATOR && player.hasPermissionLevel(2) && addModules.contains(key)))
			).toList();

			// Print and check command confirmation
			if (!last_command.getOrDefault(player.getUuid(), "").equalsIgnoreCase("/switchy_client import " + filename)) {
				tellWarn(player, "commands.switchy.import.warn", literal(String.valueOf(importedPresets.getPresetNames().size())), literal(String.valueOf(modules.size())));
				tellWarn(player, "commands.switchy.list.presets", literal(importedPresets.getPresetNames().toString()));
				tellWarn(player, "commands.switchy.list.modules", literal(modules.toString()));
				tellInvalidTry(player, "commands.switchy.import.confirmation", "commands.switchy.import.command", literal(filename));
				last_command.put(player.getUuid(), "/switchy_client import " + filename);
				return;
			}

			// Actually attempt to import
			if (presets.importFromOther(importedPresets, modules)) {
				tellSuccess(player, "commands.switchy.import.success", literal(String.valueOf(importedPresets.getPresetNames().size())));
			} else {
				String collisionPresets = presets.getPresetNames().stream()
						.filter(importedPresets.getPresetNames()::contains)
						.collect(Collectors.joining(", "));
				tellInvalid(player, "commands.switchy.import.fail.collision", literal(collisionPresets));
			}

		});
	}

	private static CompletableFuture<Suggestions> suggestPresets(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, boolean allowCurrent) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		String remaining = builder.getRemainingLowerCase();

		SwitchyPresets ps;
		if ((ps = ((SwitchyPlayer) player).switchy$getPresets()) != null) {
			ps.getPresetNames().stream()
					.filter((s) -> allowCurrent || !Objects.equals(s, Objects.toString(ps.getCurrentPreset())))
					.filter((s) -> s.toLowerCase(Locale.ROOT).startsWith(remaining))
					.forEach(builder::suggest);
		}

		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestModules(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, boolean enabled) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		String remaining = builder.getRemainingLowerCase();

		if (player instanceof SwitchyPlayer switchyPlayer && switchyPlayer.switchy$getPresets() != null) {
			Switchy.COMPAT_REGISTRY.keySet().stream()
					.filter(id -> enabled == switchyPlayer.switchy$getPresets().getModuleToggles().get(id))
					.filter((id) -> id.getPath().toLowerCase(Locale.ROOT).startsWith(remaining) || id.toString().toLowerCase(Locale.ROOT).startsWith(remaining))
					.map(Identifier::toString)
					.forEach(builder::suggest);
		}

		return builder.buildFuture();
	}

	private static <V, V2> int unwrapAndExecute(CommandContext<ServerCommandSource> context, Function4<ServerPlayerEntity, SwitchyPresets, V, V2, Integer> executeFunction, @Nullable Pair<String, Class<V>> argument, @Nullable Pair<String, Class<V2>> argument2) {
		int result = 0;

		// Get context and execute
		try {
			ServerPlayerEntity player = context.getSource().getPlayer();
			if (((SwitchyPlayer) player).switchy$getPresets() == null) {
				((SwitchyPlayer) player).switchy$setPresets(SwitchyPresets.fromNbt(new NbtCompound(), player));
			}
			SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
			result = executeFunction.apply(
					player,
					presets,
					(argument != null ? context.getArgument(argument.getLeft(), argument.getRight()) : null),
					(argument2 != null ? context.getArgument(argument2.getLeft(), argument2.getRight()) : null)
			);
			// Record previous command (for confirmations)
			last_command.put(player.getUuid(), context.getInput());
		} catch (CommandSyntaxException e) {
			Switchy.LOGGER.error("Switchy: Command wasn't called by a player! (this shouldn't happen!)");
		}

		return result;
	}

	private static <V> int unwrapAndExecute(CommandContext<ServerCommandSource> context, Function3<ServerPlayerEntity, SwitchyPresets, V, Integer> executeFunction, @Nullable Pair<String, Class<V>> argument) {
		return unwrapAndExecute(context, (player, preset, arg, ignored2) -> executeFunction.apply(player, preset, arg), argument, null);
	}

	private static int unwrapAndExecute(CommandContext<ServerCommandSource> context, BiFunction<ServerPlayerEntity, SwitchyPresets, Integer> executeFunction) {
		return unwrapAndExecute(context, (player, preset, ignored, ignored2) -> executeFunction.apply(player, preset), null, null);
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
			ServerPlayNetworking.send(player, Switchy.S2C_EXPORT, presetsBuf);
			return 1;
		} catch (Exception ex) {
			Switchy.LOGGER.error(ex.toString());
			Switchy.LOGGER.error(ex.getMessage());
			sendMessage(player, translatableWithArgs("commands.switchy.export.fail", FORMAT_INVALID));
			return 0;
		}
	}

	private static int listPresets(ServerPlayerEntity player, SwitchyPresets presets) {
		sendMessage(player, translatableWithArgs("commands.switchy.list.presets", FORMAT_INFO, literal(Objects.toString(presets, "[]"))));
		sendMessage(player, translatableWithArgs("commands.switchy.list.current", FORMAT_INFO, literal(presets != null ? Objects.toString(presets.getCurrentPreset(), "<None>") : "<None>")));
		return 1;
	}

	private static int newPreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName) {
		if (presets.containsPreset(presetName)) {
			tellInvalidTry(player, "commands.switchy.new.fail.exists", "commands.switchy.set.command", literal(presetName));
			return 0;
		}

		presets.addPreset(new SwitchyPreset(presetName, presets.getModuleToggles()));
		tellSuccess(player, "commands.switchy.new.success", literal(presetName));
		return 1 + setPreset(player, presets, presetName);
	}

	private static int setPreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName) {
		if (!presets.containsPreset(presetName)) {
			tellInvalidTry(player, "commands.switchy.set.fail.missing", "commands.switchy.list.command");
			return 0;
		}
		if (presetName.equalsIgnoreCase(Objects.toString(presets.getCurrentPreset(), null))) {
			tellInvalidTry(player, "commands.switchy.set.fail.current", "commands.switchy.list.command");
			return 0;
		}

		String oldPresetName = Objects.toString(presets.getCurrentPreset(), "<None>");
		presets.setCurrentPreset(player, presetName, true);
		Switchy.LOGGER.info("[Switchy] Player switch: '" + oldPresetName + "' -> '" + presetName + "' [" + player.getGameProfile().getName() + "]");
		tellSuccess(player, "commands.switchy.set.success", literal(oldPresetName), literal(presetName));
		return 1;
	}

	private static int renamePreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName, String newName) {
		if (!presets.containsPreset(presetName) || presets.containsPreset(newName)) {
			tellInvalidTry(player, "commands.switchy.rename.fail." + (presets.containsPreset(newName) ? "exists" : "missing"), "commands.switchy.list.command");
			return 0;
		}

		presets.renamePreset(presetName, newName);
		tellSuccess(player, "commands.switchy.rename.success", literal(presetName), literal(newName));
		return 1;
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

		if (!last_command.getOrDefault(player.getUuid(), "").equalsIgnoreCase("/switchy delete " + presetName)) {
			tellWarn(player, "commands.switchy.delete.warn");
			tellWarn(player, "commands.switchy.list.modules", literal(presets.getModuleToggles().entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).map(Identifier::getPath).toList().toString()));
			tellInvalidTry(player, "commands.switchy.delete.confirmation", "commands.switchy.delete.command", literal(presetName));
			return 0;
		} else {
			presets.deletePreset(presetName);
			tellSuccess(player, "commands.switchy.delete.success", literal(presetName));
			return 1;
		}
	}

	private static int disableModule(ServerPlayerEntity player, SwitchyPresets presets, Identifier moduleId) {
		if (!presets.getModuleToggles().containsKey(moduleId) || !presets.getModuleToggles().get(moduleId)) {
			tellInvalid(player, "commands.switchy.module.disable.fail." + (presets.getModuleToggles().containsKey(moduleId) ? "disabled" : "missing"), literal(moduleId.toString()));
			return 0;
		}

		if (!last_command.getOrDefault(player.getUuid(), "").equalsIgnoreCase("/switchy module disable " + moduleId)) {
			sendMessage(player, Switchy.COMPAT_REGISTRY.get(moduleId).get().getDisableConfirmation().setStyle(FORMAT_WARN.getLeft()));
			tellInvalidTry(player, "commands.switchy.module.disable.confirmation", "commands.switchy.module.disable.command", literal(moduleId.toString()));
			return 0;
		} else {
			presets.disableModule(moduleId);
			tellSuccess(player, "commands.switchy.module.disable.success", literal(moduleId.toString()));
			return 1;
		}
	}

	private static int enableModule(ServerPlayerEntity player, SwitchyPresets presets, Identifier moduleId) {
		if (!presets.getModuleToggles().containsKey(moduleId) || presets.getModuleToggles().get(moduleId)) {
			tellInvalid(player, "commands.switchy.module.enable.fail." + (presets.getModuleToggles().containsKey(moduleId) ? "enabled" : "missing"), literal(moduleId.toString()));
			return 0;
		}

		presets.enableModule(moduleId);
		tellSuccess(player, "commands.switchy.module.enable.success", literal(moduleId.toString()));
		return 1;
	}
}
