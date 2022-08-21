package folk.sisby.switchy;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.command.api.CommandRegistrationCallback;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SwitchyCommands {
	private static String last_command = "";

	public static void InitializeCommands() {
		CommandRegistrationCallback.EVENT.register(
				(dispatcher, buildContext, environment) -> dispatcher.register(
						literal("switchy")
								.then(literal("help")
										.executes((c) -> unwrapAndExecute(c, SwitchyCommands::displayHelp)))
								.then(literal("list")
										.executes((c) -> unwrapAndExecute(c, SwitchyCommands::listPresets)))
								.then(literal("new")
										.then(argument("preset", StringArgumentType.word())
												.executes((c) -> unwrapAndExecute(c, SwitchyCommands::newPreset, new Pair<>("preset", String.class)))))
								.then(literal("set")
										.then(argument("preset", StringArgumentType.word())
												.suggests((c, b) -> suggestPresets(c, b, false))
												.executes((c) -> unwrapAndExecute(c, SwitchyCommands::setPreset, new Pair<>("preset", String.class)))))
								.then(literal("delete")
										.then(argument("preset", StringArgumentType.word())
												.suggests((c, b) -> suggestPresets(c, b, true))
												.executes((c) -> unwrapAndExecute(c, SwitchyCommands::deletePreset, new Pair<>("preset", String.class)))))
								.then(literal("rename")
										.then(argument("preset", StringArgumentType.word())
												.suggests((c, b) -> suggestPresets(c, b, true))
												.then(argument("name", StringArgumentType.word())
													.executes((c) -> unwrapAndExecute(c, SwitchyCommands::renamePreset, new Pair<>("preset", String.class), new Pair<>("name", String.class))))))
								.then(literal("module")
										.then(literal("enable")
												.then(argument("module", IdentifierArgumentType.identifier())
														.suggests((c, b) -> suggestModules(c, b, false))
														.executes((c) -> unwrapAndExecute(c, SwitchyCommands::enableModule, new Pair<>("module", Identifier.class)))))
										.then(literal("disable")
												.then(argument("module", IdentifierArgumentType.identifier())
														.suggests((c, b) -> suggestModules(c, b, true))
														.executes((c) -> unwrapAndExecute(c, SwitchyCommands::disableModule, new Pair<>("module", Identifier.class)))))
								)));

		// switchy set alias
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> dispatcher.register(
				literal("switch")
						.then(argument("preset", StringArgumentType.word())
								.suggests((c, b) -> suggestPresets(c, b, false))
								.executes((c) -> unwrapAndExecute(c, SwitchyCommands::setPreset, new Pair<>("preset", String.class)))))
		);
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
				((SwitchyPlayer) player).switchy$setPresets(SwitchyPresets.fromNbt(player, new NbtCompound()));
			}
			SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
			result = executeFunction.apply(
					player,
					presets,
					(argument != null ? context.getArgument(argument.getLeft(), argument.getRight()) : null),
					(argument2 != null ? context.getArgument(argument2.getLeft(), argument2.getRight()) : null)
			);
		} catch (CommandSyntaxException e) {
			Switchy.LOGGER.error("Switchy: Command wasn't called by a player! (this shouldn't happen!)");
		}

		// Record previous command (for confirmations)
		last_command = context.getInput();
		return result;
	}

	private static <V> int unwrapAndExecute(CommandContext<ServerCommandSource> context, Function3<ServerPlayerEntity, SwitchyPresets, V, Integer> executeFunction,  @Nullable Pair<String, Class<V>> argument) {
		return unwrapAndExecute(context, (player, preset, arg, ignored2) -> executeFunction.apply(player, preset, arg), argument, null);
	}

	private static int unwrapAndExecute(CommandContext<ServerCommandSource> context, BiFunction<ServerPlayerEntity, SwitchyPresets, Integer> executeFunction) {
		return unwrapAndExecute(context, (player, preset, ignored, ignored2) -> executeFunction.apply(player, preset), null, null);
	}

	private static int displayHelp(ServerPlayerEntity player, SwitchyPresets presets) {
		tellInfo(player, "/switchy new [name]", " - create a new preset");
		tellInfo(player, "/switchy set [name]", " - switches to specified preset");
		tellInfo(player, "/switch [name]", " - alias of above");
		tellInfo(player, "/switchy delete [name]", " - delete a preset permanently");
		tellInfo(player, "/switchy rename [name] [name]", " - rename a preset");
		tellInfo(player, "/switchy list", " - list presets and show current");
		tellInfo(player, "/switchy module enable [name]", " - enable a module for you");
		tellInfo(player, "/switchy module disable [name]", " - disable a module for you");
		return 6;
	}

	private static int listPresets(ServerPlayerEntity player, SwitchyPresets presets) {
		tellInfo(player, "Presets: ", Objects.toString(presets, "[]"));
		tellInfo(player, "Current Preset: ", presets != null ? Objects.toString(presets.getCurrentPreset(), "<None>") : "<None>");
		return 1;
	}

	private static int newPreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName) {
		if (presets.containsPreset(presetName)) {
			tellInvalid(player, "That preset already exists! Try ", "/switchy set " + presetName);
			return 0;
		}

		presets.addPreset(new SwitchyPreset(presetName, presets.getModuleToggles()));
		tellSuccess(player, "Created ", presetName);
		return 1 + setPreset(player, presets, presetName);
	}

	private static int setPreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName) {
		if (!presets.containsPreset(presetName)) {
			tellInvalid(player, "That preset doesn't exist! Try ", "/switchy list");
			return 0;
		}

		String oldPresetName = Objects.toString(presets.getCurrentPreset(), "<None>");
		presets.setCurrentPreset(presetName, true);
		tellChanged(player, "Switched", oldPresetName, presetName);
		return 1;
	}

	private static int renamePreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName, String newName) {
		if (!presets.containsPreset(presetName) || presets.containsPreset(newName)) {
			tellInvalid(player, "That preset " + (presets.containsPreset(newName) ? "already exists" : "doesn't exist") + "! Try ", "/switchy list");
			return 0;
		}

		presets.renamePreset(presetName, newName);
		tellChanged(player, "Renamed preset ", presetName, newName);
		return 1;
	}

	private static int deletePreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName) {
		if (!presets.getPresetNames().contains(presetName)) {
			tellInvalid(player, "That preset doesn't exist! Try ", "/switchy list");
			return 0;
		}

		if (!last_command.equalsIgnoreCase("/switchy delete " + presetName)) {
			tellWarn(player, "WARNING: Preset data from enabled modules will be deleted.");
			tellWarn(player, "Modules: ", presets.getModuleToggles().entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).map(Identifier::getPath).toList().toString());
			tellInvalid(player, "Confirm using ", "/switchy delete " + presetName);
			return 0;
		} else {
			presets.deletePreset(presetName);
			tellSuccess(player, "Deleted ", presetName);
			return 1;
		}
	}

	private static int disableModule(ServerPlayerEntity player, SwitchyPresets presets, Identifier moduleId) {
		if (!presets.getModuleToggles().containsKey(moduleId) || !presets.getModuleToggles().get(moduleId)) {
			tellInvalid(player, "Module " + (presets.getModuleToggles().containsKey(moduleId) ? "doesn't exist" : "is already disabled") + ": ", moduleId.toString());
			return 0;
		}

		if (!last_command.equalsIgnoreCase("/switchy module disable " + moduleId)) {
			tellWarn(player, Switchy.COMPAT_REGISTRY.get(moduleId).get().getDisableConfirmation());
			tellInvalid(player, "Confirm using ", "/switchy module disable " + moduleId);
			return 0;
		} else {
			presets.disableModule(moduleId);
			tellSuccess(player, "Disabled ", moduleId.toString());
			return 1;
		}
	}

	private static int enableModule(ServerPlayerEntity player, SwitchyPresets presets, Identifier moduleId) {
		if (!presets.getModuleToggles().containsKey(moduleId) || presets.getModuleToggles().get(moduleId)) {
			tellInvalid(player, "Module " + (presets.getModuleToggles().containsKey(moduleId) ? "doesn't exist" : "is already enabled") + ": ", moduleId.toString());
			return 0;
		}

		presets.enableModule(moduleId);
		tellSuccess(player, "Enabled ", moduleId.toString());
		return 1;
	}

	private static void sendMessage(ServerPlayerEntity player, Text text) {
		player.sendMessage(new LiteralText("[Switchy] ").setStyle(Style.EMPTY.withColor(Formatting.AQUA)).append(text), false);
	}

	private static void tellChanged(ServerPlayerEntity player, String action, String oldLiteral, String newLiteral) {
		sendMessage(player,
				new LiteralText("")
						.append(new LiteralText(action + " from ").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
						.append(new LiteralText(oldLiteral).setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
						.append(new LiteralText(" to ").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
						.append(new LiteralText(newLiteral).setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
		);
	}

	private static void tell(ServerPlayerEntity player, String literal, Style style, String literal2, Style style2) {
		sendMessage(player,
				new LiteralText("")
						.append(new LiteralText(literal).setStyle(style))
						.append(new LiteralText(literal2).setStyle(style2))
		);
	}

	private static void tell(ServerPlayerEntity player, String literal, Style style) {
		tell(player, literal, style, "", Style.EMPTY);
	}

	private static void tellSuccess(ServerPlayerEntity player, String literal, String literal2) {
		tell(player, literal, Style.EMPTY.withColor(Formatting.GREEN), literal2, Style.EMPTY.withColor(Formatting.WHITE).withItalic(true));
	}

	private static void tellSuccess(ServerPlayerEntity player, String literal) {
		tell(player, literal, Style.EMPTY.withColor(Formatting.GREEN));
	}

	private static void tellInvalid(ServerPlayerEntity player, String literal, String literal2) {
		tell(player, literal, Style.EMPTY.withColor(Formatting.YELLOW), literal2, Style.EMPTY.withColor(Formatting.WHITE).withItalic(true));
	}

	private static void tellInvalid(ServerPlayerEntity player, String literal) {
		tell(player, literal, Style.EMPTY.withColor(Formatting.YELLOW));
	}

	private static void tellInfo(ServerPlayerEntity player, String literal, String literal2) {
		tell(player, literal, Style.EMPTY.withColor(Formatting.GRAY).withItalic(true), literal2, Style.EMPTY.withColor(Formatting.WHITE));
	}

	private static void tellInfo(ServerPlayerEntity player, String literal) {
		tell(player, literal, Style.EMPTY.withColor(Formatting.GRAY));
	}

	private static void tellWarn(ServerPlayerEntity player, String literal, String literal2) {
		tell(player, literal, Style.EMPTY.withColor(Formatting.GOLD), literal2, Style.EMPTY.withColor(Formatting.GRAY));
	}

	private static void tellWarn(ServerPlayerEntity player, String literal) {
		tell(player, literal, Style.EMPTY.withColor(Formatting.GOLD));
	}

}
