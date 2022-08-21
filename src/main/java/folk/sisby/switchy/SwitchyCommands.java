package folk.sisby.switchy;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Function3;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
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

	private static <V> int unwrapAndExecute(CommandContext<ServerCommandSource> context, Function3<ServerPlayerEntity, SwitchyPresets, V, Integer> executeFunction, @Nullable Pair<String, Class<V>> argument) {
		int result = 0;

		// Get context and execute
		try {
			ServerPlayerEntity player = context.getSource().getPlayer();
			if (((SwitchyPlayer) player).switchy$getPresets() == null) {
				((SwitchyPlayer) player).switchy$setPresets(SwitchyPresets.fromNbt(player, new NbtCompound()));
			}
			SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
			result = executeFunction.apply(player, presets, argument != null ? context.getArgument(argument.getLeft(), argument.getRight()) : null);
		} catch (CommandSyntaxException e) {
			Switchy.LOGGER.error("Switchy: Command wasn't called by a player! (this shouldn't happen!)");
		}

		// Record previous command (for confirmations)
		last_command = context.getInput();
		return result;
	}

	private static int unwrapAndExecute(CommandContext<ServerCommandSource> context, BiFunction<ServerPlayerEntity, SwitchyPresets, Integer> executeFunction) {
		return unwrapAndExecute(context, (player, preset, ignored) -> executeFunction.apply(player, preset), null);
	}

	private static int displayHelp(ServerPlayerEntity player, SwitchyPresets presets) {
		inform(player, "Commands: new, set, delete, list");
		inform(player, "/switchy new {name} - create a new preset");
		inform(player, "/switch {name} OR /switchy set {name} - saves current preset and swaps to specified");
		inform(player, "/switchy delete {name} - delete a preset");
		inform(player, "/switchy list - list all created presets");
		inform(player, "/switchy module enable/disable {name} - toggle compat modules");
		return 6;
	}

	private static int listPresets(ServerPlayerEntity player, SwitchyPresets presets) {
		player.sendMessage(new LiteralText("Presets: ").append(new LiteralText(Objects.toString(presets, "[]"))), false);
		player.sendMessage(new LiteralText("Current Preset: ").append(new LiteralText(presets != null ? Objects.toString(presets.getCurrentPreset(), "<None>") : "<None>")), false);
		return 1;
	}

	private static int newPreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName) {
		if (presets.containsPreset(presetName)) {
			informWithName(player, "That preset already exists! - try ", "/switchy set " + presetName);
			return 0;
		}

		presets.addPreset(new SwitchyPreset(presetName, presets.getModuleToggles()));
		informWithName(player, "Successfully added preset ", presetName);
		return 1 + setPreset(player, presets, presetName);
	}

	private static int setPreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName) {
		if (!presets.containsPreset(presetName)) {
			informWithName(player, "That preset doesn't exist! - try ", "/switchy list");
			return 0;
		}

		String oldPresetName = Objects.toString(presets.getCurrentPreset(), "<None>");
		presets.setCurrentPreset(presetName, true);
		informSwitch(player, oldPresetName, presetName);
		return 1;
	}

	private static int deletePreset(ServerPlayerEntity player, SwitchyPresets presets, String presetName) {
		if (!presets.getPresetNames().contains(presetName)) {
			informWithName(player, "That preset doesn't exist! Try", "/switchy list");
			return 0;
		}

		if (!last_command.equalsIgnoreCase("/switchy delete " + presetName)) {
			warn(player, "WARNING: Deleting a preset will permanently delete its data for the following modules:");
			informWithName(player, "", presets.getModuleToggles().entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).map(Identifier::getPath).toList().toString());
			informWithName(player, "To Confirm, please enter ", "/switchy delete " + presetName);
			return 0;
		} else {
			presets.deletePreset(presetName);
			informWithName(player, "Preset deleted: ", presetName);
			return 1;
		}
	}

	private static int disableModule(ServerPlayerEntity player, SwitchyPresets presets, Identifier moduleId) {
		if (!presets.getModuleToggles().containsKey(moduleId) || !presets.getModuleToggles().get(moduleId)) {
			informWithName(player, "Module doesn't exist or is already disabled! - ", moduleId.toString());
			return 0;
		}

		if (!last_command.equalsIgnoreCase("/switchy module disable " + moduleId)) {
			inform(player, "Disabling a module will delete its data from all your presets");
			warn(player, Switchy.COMPAT_REGISTRY.get(moduleId).get().getDisableConfirmation());
			informWithName(player, "To Confirm, please enter ", "/switchy module disable " + moduleId);
			return 0;
		} else {
			presets.disableModule(moduleId);
			informWithName(player, "Successfully disabled module ", moduleId.toString());
			return 1;
		}
	}

	private static int enableModule(ServerPlayerEntity player, SwitchyPresets presets, Identifier moduleId) {
		if (!presets.getModuleToggles().containsKey(moduleId) || presets.getModuleToggles().get(moduleId)) {
			informWithName(player, "Module doesn't exist or is already enabled! - ", moduleId.toString());
			return 0;
		}

		presets.enableModule(moduleId);
		informWithName(player, "Successfully enabled module ", moduleId.toString());
		return 1;
	}

	private static void informSwitch(ServerPlayerEntity player, String oldPreset, String newPreset) {
		player.sendMessage(new LiteralText("Switched from ")
						.setStyle(Style.EMPTY.withColor(Formatting.YELLOW))
						.append(new LiteralText(oldPreset).setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
						.append(new LiteralText(" to "))
						.append(new LiteralText(newPreset).setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
				, false);
	}

	private static void informWithName(ServerPlayerEntity player, String literal, String name) {
		player.sendMessage(new LiteralText(literal)
						.setStyle(Style.EMPTY.withColor(Formatting.YELLOW))
						.append(new LiteralText(name).setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
				, false);
	}

	private static void inform(ServerPlayerEntity player, String literal) {
		player.sendMessage(new LiteralText(literal)
						.setStyle(Style.EMPTY.withColor(Formatting.YELLOW))
				, false);
	}

	private static void warn(ServerPlayerEntity player, String literal) {
		player.sendMessage(new LiteralText(literal)
						.setStyle(Style.EMPTY.withColor(Formatting.GOLD))
				, false);
	}

}
