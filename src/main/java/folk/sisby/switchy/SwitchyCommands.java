package folk.sisby.switchy;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import folk.sisby.switchy.api.ModuleImportable;
import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.api.SwitchySwitchEvent;
import folk.sisby.switchy.presets.SwitchyPreset;
import folk.sisby.switchy.presets.SwitchyPresets;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.command.api.CommandRegistrationCallback;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.ServerPlayConnectionEvents;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static folk.sisby.switchy.Switchy.*;
import static folk.sisby.switchy.api.PlayerPresets.*;
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
										.requires(source -> {
											ServerPlayerEntity player = serverPlayerOrNull(source);
											return player != null && ServerPlayNetworking.canSend(player, S2C_EXPORT);
										})
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
		ServerPlayNetworking.registerGlobalReceiver(C2S_IMPORT, (server, player, handler, buf, sender) -> importPresets(player, buf.readNbt()));
	}

	public static void InitializeEvents() {
		ServerPlayConnectionEvents.JOIN.register((spn, ps, s) -> {
			ServerPlayerEntity player = spn.getPlayer();
			SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
			SwitchySwitchEvent switchEvent = new SwitchySwitchEvent(
					spn.getPlayer().getUuid(), presets != null ? presets.getCurrentPreset().presetName : "", null, presets != null ? presets.getEnabledModuleNames() : new ArrayList<>()
			);
			SwitchyEvents.fireSwitch(switchEvent);
			if (ServerPlayNetworking.canSend(player, S2C_SWITCH)) {
				ps.sendPacket(S2C_SWITCH, PacketByteBufs.create().writeNbt(switchEvent.toNbt()));
			}
		});
	}

	private static CompletableFuture<Suggestions> suggestPresets(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, boolean allowCurrent) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		CommandSource.suggestMatching(getPlayerPresetNames(player).stream().filter((s) -> allowCurrent || !Objects.equals(s, getPlayerCurrentPresetName(player))), builder);
		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestModules(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, boolean enabled) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		CommandSource.suggestIdentifiers(getPlayerPresetModules(player).entrySet().stream().filter(e -> e.getValue() == enabled).map(Map.Entry::getKey), builder);
		return builder.buildFuture();
	}

	private static @Nullable ServerPlayerEntity serverPlayerOrNull(ServerCommandSource source) {
		try {
			return source.getPlayer();
		} catch (CommandSyntaxException e) {
			return null;
		}
	}

	private static <V, V2> int unwrapAndExecute(CommandContext<ServerCommandSource> context, Function4<ServerPlayerEntity, SwitchyPresets, V, V2, Integer> executeFunction, @Nullable Pair<String, Class<V>> argument, @Nullable Pair<String, Class<V2>> argument2) {
		int result = 0;

		ServerPlayerEntity player = serverPlayerOrNull(context.getSource());
		if (player == null) {
			LOGGER.error("Switchy: Command wasn't called by a player! (this shouldn't happen!)");
			return result;
		}

		// Get context and execute
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		result = executeFunction.apply(
				player,
				presets,
				(argument != null ? context.getArgument(argument.getLeft(), argument.getRight()) : null),
				(argument2 != null ? context.getArgument(argument2.getLeft(), argument2.getRight()) : null)
		);
		// Record previous command (for confirmations)
		last_command.put(player.getUuid(), context.getInput());

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

	private static int setPreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName) {
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

		if (!last_command.getOrDefault(player.getUuid(), "").equalsIgnoreCase(command("switchy delete " + presetName))) {
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

		if (!last_command.getOrDefault(player.getUuid(), "").equalsIgnoreCase(command("switchy module disable " + moduleId))) {
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

	private static void importPresets(ServerPlayerEntity player, @Nullable NbtCompound presetNbt) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();

		// Parse Preset NBT //

		if (presetNbt == null || !presetNbt.contains("command", NbtElement.STRING_TYPE)) {
			tellInvalid(player, "commands.switchy.import.fail.parse");
			return;
		}

		SwitchyPresets importedPresets;
		try {
			importedPresets = SwitchyPresets.fromNbt(presetNbt, null);
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

		importedPresets.modules.forEach((moduleId, enabled) -> {
			if (enabled && (!presets.modules.containsKey(moduleId) || !presets.modules.get(moduleId) || excludeModules.contains(moduleId) || getImportable(moduleId) == ModuleImportable.NEVER || (!opModules.contains(moduleId) && getImportable(moduleId) == ModuleImportable.OPERATOR))) {
				importedPresets.disableModule(moduleId);
			}
		});

		String command = presetNbt.getString("command");

		// Perform pre-import command feedback //

		if (!opModules.isEmpty() && player.hasPermissionLevel(2)) {
			tellWarn(player, "commands.switchy.import.fail.permission", getIdText(opModules));
			return;
		}

		// Print info and stop if confirmation is required.
		if (!last_command.getOrDefault(player.getUuid(), "").equalsIgnoreCase(command)) {
			tellWarn(player, "commands.switchy.import.warn", literal(String.valueOf(importedPresets.getPresetNames().size())), literal(String.valueOf(importedPresets.getEnabledModules().size())));
			tellWarn(player, "commands.switchy.import.warn.collision");
			tellWarn(player, "commands.switchy.list.presets", getHighlightedListText(importedPresets.getPresetNames(), Map.of(presets.getCurrentPreset().presetName::equalsIgnoreCase, Formatting.STRIKETHROUGH, presets.getPresetNames()::contains, Formatting.DARK_RED)));
			tellWarn(player, "commands.switchy.list.modules", importedPresets.getEnabledModuleText());
			sendMessage(player, translatableWithArgs("commands.switchy.import.confirmation", FORMAT_INVALID, literal("/" + command)));
			last_command.put(player.getUuid(), command);
			return;
		}

		// Import
		presets.importFromOther(importedPresets);
		tellSuccess(player, "commands.switchy.import.success", literal(String.valueOf(importedPresets.getPresetNames().size())));
	}
}
