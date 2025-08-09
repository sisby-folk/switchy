package dev.sisby.switchy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.sisby.switchy.data.SwitchyComponentTypes;
import dev.sisby.switchy.data.SwitchyPlayerData;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.function.Consumer;

public class SwitchyCommands {
	private static int switchProfile(String input, ServerPlayerEntity player, SwitchyPlayerData data, Consumer<Text> feedback, String profileId) {
		feedback.accept(prefix()
			.append(Text.literal("Switched from ").formatted(Formatting.GREEN))
			.append(data.getCurrentProfile().getOrGetDefault(SwitchyComponentTypes.NAME, p -> Text.of(p.id())))
			.append(Text.literal(" to ").formatted(Formatting.GREEN))
			.append(data.switchOrCreateProfile(profileId, player).getOrGetDefault(SwitchyComponentTypes.NAME, p -> Text.of(p.id())))
			.append(Text.literal("!").formatted(Formatting.GREEN))
		);
		return 1;
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registries, CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(
			CommandManager.literal("switchy")
				.then(CommandManager.literal("switch")
					.then(CommandManager.argument("profile", StringArgumentType.word())
						.suggests((c, b) -> CommandSource.suggestMatching((Iterable<String>) map(c, (i, p, d, f) -> d.profiles().keySet(), false), b))
						.executes(c -> execute(c, (i, p, d, f) -> switchProfile(i, p, d, f, c.getArgument("profile", String.class).toLowerCase())))
					)
				)
		);
	}

	public static MutableText prefix() {
		return Text.literal("").append(Text.literal("[Switchy] ").formatted(Formatting.DARK_PURPLE));
	}

	public static MutableText indent() {
		return Text.literal("").append(Text.literal("|| ").formatted(Formatting.DARK_PURPLE));
	}

	public static <T> T map(CommandContext<ServerCommandSource> context, SurveyorCommandExecutor<T> executor, boolean feedback) {
		ServerPlayerEntity player;
		try {
			player = context.getSource().getPlayerOrThrow();
		} catch (CommandSyntaxException e) {
			if (feedback) Switchy.LOGGER.error("[Switchy] Commands cannot be invoked by a non-player");
			return null;
		}

		SwitchyPlayerData data = player.getAttachedOrCreate(Switchy.PLAYER_DATA);
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
