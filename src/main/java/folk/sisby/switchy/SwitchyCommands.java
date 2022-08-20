package folk.sisby.switchy;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.quiltmc.qsl.command.api.CommandRegistrationCallback;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SwitchyCommands {
	public static void InitializeCommands() {
		CommandRegistrationCallback.EVENT.register(
				(dispatcher, buildContext, environment) -> dispatcher.register(
						literal("switchy")
								.then(literal("help")
										.executes(SwitchyCommands::executeHelp))
								.then(literal("list")
										.executes(SwitchyCommands::executeList))
								.then(literal("new")
										.then(argument("preset", StringArgumentType.word())
												.executes(SwitchyCommands::executeNew)))
								.then(literal("set")
										.then(argument("preset", StringArgumentType.word())
												.suggests(SwitchyCommands::suggestOtherPresets)
												.executes(SwitchyCommands::executeSet)))
								.then(literal("delete")
										.then(argument("preset", StringArgumentType.word())
												.suggests(SwitchyCommands::suggestPresets)
												.executes(SwitchyCommands::executeDelete)))
								.then(literal("module")
										.then(literal("allow")
												.then(argument("module", IdentifierArgumentType.identifier())
														.suggests(SwitchyCommands::suggestDenied)
														.executes(SwitchyCommands::executeAllow)))
										.then(literal("deny")
												.then(argument("module", IdentifierArgumentType.identifier())
														.suggests(SwitchyCommands::suggestAllowed)
														.executes(SwitchyCommands::executeDeny)))
								)));

		// switchy set alias
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> dispatcher.register(
				literal("switch")
						.then(argument("preset", StringArgumentType.word())
								.suggests(SwitchyCommands::suggestOtherPresets)
								.executes(SwitchyCommands::executeSet)))
		);
	}

	private static CompletableFuture<Suggestions> suggestPresets(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		String remaining = builder.getRemainingLowerCase();

		SwitchyPresets ps;
		if ((ps = ((SwitchyPlayer) player).switchy$getPresets()) != null) {
			ps.getPresetNames().stream()
					.filter((s) -> s.toLowerCase(Locale.ROOT).startsWith(remaining))
					.forEach(builder::suggest);
		}

		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestOtherPresets(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		String remaining = builder.getRemainingLowerCase();

		SwitchyPresets ps;
		if ((ps = ((SwitchyPlayer) player).switchy$getPresets()) != null) {
			ps.getPresetNames().stream()
					.filter((s) -> !Objects.equals(s, Objects.toString(ps.getCurrentPreset())))
					.filter((s) -> s.toLowerCase(Locale.ROOT).startsWith(remaining))
					.forEach(builder::suggest);
		}

		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestModules(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, boolean allowed) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		String remaining = builder.getRemainingLowerCase();

		if (player instanceof SwitchyPlayer switchyPlayer && switchyPlayer.switchy$getPresets() != null) {
			Switchy.COMPAT_REGISTRY.keySet().stream()
					.filter(id -> allowed == switchyPlayer.switchy$getPresets().getModuleToggles().get(id))
					.filter((id) -> id.getPath().toLowerCase(Locale.ROOT).startsWith(remaining) || id.toString().toLowerCase(Locale.ROOT).startsWith(remaining))
					.map(Identifier::toString)
					.forEach(builder::suggest);
		}

		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestAllowed(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
		return suggestModules(context, builder, true);
	}

	private static CompletableFuture<Suggestions> suggestDenied(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
		return suggestModules(context, builder, false);
	}

	private static int executeHelp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();

		inform(player, "Commands: new, set, delete, list");
		inform(player, "/switchy new {name} - create a new preset");
		inform(player, "/switch {name} OR /switchy set {name} - saves current preset and swaps to specified");
		inform(player, "/switchy delete {name} - delete a preset");
		inform(player, "/switchy list - list all created presets");
		inform(player, "/switchy module allow/deny {name} - toggle compat modules [WARNING: Data will be lost]");
		return 1;
	}

	private static int executeList(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();

		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		player.sendMessage(new LiteralText("Presets: ").append(new LiteralText(Objects.toString(presets, "[]"))), false);
		player.sendMessage(new LiteralText("Current Preset: ").append(new LiteralText(presets != null ? Objects.toString(presets.getCurrentPreset(), "<None>") : "<None>")), false);
		return 1;
	}

	private static int executeNew(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		String presetName = context.getArgument("preset", String.class);
		SwitchyPresets presets = validateSwitchyPlayer(player).switchy$getPresets();

		if (presets.addPreset(new SwitchyPreset(presetName, presets.getModuleToggles()))) {
			informWithPreset(player, "Successfully added preset ", presetName);
			return 1 + executeSet(context);
		} else {
			informWithPreset(player, "That preset already exists! - try /switchy set ", presetName);
			return 0;
		}
	}


	private static int executeSet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		String presetName = context.getArgument("preset", String.class);
		SwitchyPresets presets = validateSwitchyPlayer(player).switchy$getPresets();

		String oldPresetName = Objects.toString(presets.getCurrentPreset(), "<None>");
		if (presets.setCurrentPreset(presetName, true)) {
			informSwitch(player, oldPresetName, presetName);
			return 1;
		} else {
			inform(player, "That preset doesn't exist! /switchy list");
			return 0;
		}
	}

	private static int executeDelete(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		String presetName = context.getArgument("preset", String.class);
		SwitchyPresets presets = validateSwitchyPlayer(player).switchy$getPresets();

		if (presets.deletePreset(presetName)) {
			informWithPreset(player, "Preset deleted: ", presetName);
			return 1;
		} else {
			inform(player, "That preset doesn't exist! /switchy list");
			return 0;
		}
	}

	private static int executeDeny(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		Identifier moduleId = context.getArgument("module", Identifier.class);
		SwitchyPresets presets = validateSwitchyPlayer(player).switchy$getPresets();

		if (presets.denyModule(moduleId)) {
			informWithPreset(player, "Successfully denied module ", moduleId.toString());
			return 1;
		} else {
			informWithPreset(player, "Module doesn't exist or is already denied - ", moduleId.toString());
			return 0;
		}
	}

	private static int executeAllow(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		Identifier moduleId = context.getArgument("module", Identifier.class);
		SwitchyPresets presets = validateSwitchyPlayer(player).switchy$getPresets();

		if (presets.allowModule(moduleId)) {
			informWithPreset(player, "Successfully allowed module ", moduleId.toString());
			return 1;
		} else {
			informWithPreset(player, "Module doesn't exist or is already allowed! - ", moduleId.toString());
			return 0;
		}
	}


	private static SwitchyPlayer validateSwitchyPlayer(ServerPlayerEntity player) {
		if (((SwitchyPlayer) player).switchy$getPresets() == null) {
			((SwitchyPlayer) player).switchy$setPresets(SwitchyPresets.fromNbt(player, new NbtCompound()));
		}
		return (SwitchyPlayer) player;
	}

	private static void informSwitch(ServerPlayerEntity player, String oldPreset, String newPreset) {
		player.sendMessage(new LiteralText("You've switched from ")
						.setStyle(Style.EMPTY.withColor(Formatting.YELLOW))
						.append(new LiteralText(oldPreset).setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
						.append(new LiteralText(" to "))
						.append(new LiteralText(newPreset).setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
				, false);
	}

	private static void informWithPreset(ServerPlayerEntity player, String literal, String preset) {
		player.sendMessage(new LiteralText(literal)
						.setStyle(Style.EMPTY.withColor(Formatting.YELLOW))
						.append(new LiteralText(preset).setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
				, false);
	}

	private static void inform(ServerPlayerEntity player, String literal) {
		player.sendMessage(new LiteralText(literal)
						.setStyle(Style.EMPTY.withColor(Formatting.YELLOW))
				, false);
	}

}
