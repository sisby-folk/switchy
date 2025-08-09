package dev.sisby.switchy;

import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.sisby.switchy.data.SwitchyComponentTypes;
import dev.sisby.switchy.data.SwitchyPlayerData;
import dev.sisby.switchy.data.SwitchyProfile;
import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class SwitchyCommands {
	private static int list(String input, ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback) {
		try {
			data.updateCurrent(player);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		feedback.accept(prefix()
			.append(Text.literal("%s".formatted(data.profiles().size())).formatted(Formatting.WHITE))
			.append(Text.literal(" profiles available. ").formatted(Formatting.GRAY))
			.append(clickable("new", "/switchy switch ", false))
		);
		for (SwitchyProfile profile : data.profiles().values()) {
			if (profile.id().equals(data.current())) continue;
			feedback.accept(indent()
				.append(clickable("switch", "/switchy switch %s".formatted(profile.id()), true))
				.append(Text.of(" "))
				.append(profile.getOrGetDefault(SwitchyComponentTypes.NAME, p -> Text.of(p.id())).copy().setStyle(Style.EMPTY
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(profile.components().toString())))
				))
			);
		}
		feedback.accept(indent()
			.append(Text.literal("Current: ").formatted(Formatting.GRAY))
			.append(data.getCurrentProfile().getOrGetDefault(SwitchyComponentTypes.NAME, p -> Text.of(p.id())).copy().setStyle(Style.EMPTY
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(data.getCurrentProfile().components().toString())))
			))
		);
		return data.profiles().size();
	}

	private static int switchProfile(ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback, String profileId) {
		SwitchyProfile currentProfile = data.getCurrentProfile();
		SwitchyProfile nextProfile;
		try {
			nextProfile = data.switchOrCreateProfile(profileId, player);
		} catch (Exception e) {
			feedback.accept(prefix()
				.append("Error while switching: ").formatted(Formatting.RED)
				.append(Objects.requireNonNullElse(e.getMessage(), "???")).formatted(Formatting.GRAY)
				.append(" See server logs for more info.").formatted(Formatting.RED)
			);
			Switchy.LOGGER.error("[Switchy] Error while switching to {} for player {}", profileId, player.getGameProfile().getName(), e);
			return 0;
		}
		feedback.accept(prefix()
			.append(Text.literal("Switched from ").formatted(Formatting.GREEN))
			.append(currentProfile.getOrGetDefault(SwitchyComponentTypes.NAME, p -> Text.of(p.id())))
			.append(Text.literal(" to ").formatted(Formatting.GREEN))
			.append(nextProfile.getOrGetDefault(SwitchyComponentTypes.NAME, p -> Text.of(p.id())))
			.append(Text.literal("! ").formatted(Formatting.GREEN))
			.append(clickable("list", "/switchy", true))
		);
		return 1;
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registries, CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(
			CommandManager.literal("switchy")
				.then(CommandManager.literal("switch")
					.then(CommandManager.argument("profile", StringArgumentType.word())
						.suggests((c, b) -> CommandSource.suggestMatching((Iterable<String>) map(c, (i, p, d, f) -> Sets.difference(d.profiles().keySet(), Set.of(d.current())) , false), b))
						.executes(c -> execute(c, (i, p, d, f) -> switchProfile(p, d, f, c.getArgument("profile", String.class).toLowerCase())))
					)
				)
				.executes(c -> execute(c, SwitchyCommands::list))
		);
	}

	public static MutableText prefix() {
		return Text.literal("").append(Text.literal("[Switchy] ").formatted(Formatting.DARK_PURPLE));
	}

	public static MutableText indent() {
		return Text.literal("").append(Text.literal("|| ").formatted(Formatting.DARK_PURPLE));
	}

	public static MutableText clickable(String name, String command, boolean instant) {
		return Text.literal("")
			.append(Text.literal("<").formatted(Formatting.GRAY))
			.append(Text.literal(name).setStyle(Style.EMPTY
				.withFormatting(Formatting.AQUA)
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(command + (instant ? "" : "...")).formatted(Formatting.AQUA)))
				.withClickEvent(new ClickEvent(instant ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, command))
			))
			.append(Text.literal(">").formatted(Formatting.GRAY));
	}

	public static <T> T map(CommandContext<ServerCommandSource> context, SurveyorCommandExecutor<T> executor, boolean feedback) {
		ServerPlayerEntity player;
		try {
			player = context.getSource().getPlayerOrThrow();
		} catch (CommandSyntaxException e) {
			if (feedback) Switchy.LOGGER.error("[Switchy] Commands cannot be invoked by a non-player");
			return null;
		}

		SwitchyPlayerData data = ((SwitchyPlayer) player).switchy$playerData();
		try {
			return executor.execute(context.getInput(), player, data, t -> context.getSource().sendFeedback(() -> t, false));
		} catch (Exception e) {
			if (feedback) context.getSource().sendFeedback(() -> prefix().append(Text.literal("Command \"/%s...\" failed! Check log for details.".formatted(context.getInput().substring(0, Math.min(context.getInput().length(), 20)))).formatted(Formatting.RED)), false);
			if (feedback) Switchy.LOGGER.error("[Switchy] Error while executing command: {}", context.getInput(), e);
			return null;
		}
	}

	public static int execute(CommandContext<ServerCommandSource> context, SurveyorCommandExecutor<Integer> executor) {
		return Objects.requireNonNullElse(map(context, executor, true), 0);
	}

	public interface SurveyorCommandExecutor<T> {
		T execute(String input, ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback);
	}
}
