package folk.sisby.switchy.util;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import folk.sisby.switchy.api.SwitchyFeedbackStatus;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.api.presets.SwitchyPreset;
import folk.sisby.switchy.api.presets.SwitchyPresetData;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

import static folk.sisby.switchy.Switchy.LOGGER;
import static folk.sisby.switchy.util.Feedback.invalid;
import static folk.sisby.switchy.util.Feedback.sendMessage;

/**
 * Utilities for registering and executing server commands.
 *
 * @author Sisby folk
 * @since 1.8.13
 */
public class SwitchyCommand {
	private static CompletableFuture<Suggestions> suggestPresets(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, BiPredicate<SwitchyPresets, SwitchyPreset> suggestionPredicate) throws CommandSyntaxException {
		SwitchyPresets presets = ((SwitchyPlayer) context.getSource().getPlayer()).switchy$getPresets();
		CommandSource.suggestMatching(presets.getPresets().values().stream().filter(p -> suggestionPredicate.test(presets, p)).map(SwitchyPresetData::getName), builder);
		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestModules(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, BiPredicate<SwitchyPresets, Identifier> suggestionPredicate) throws CommandSyntaxException {
		SwitchyPresets presets = ((SwitchyPlayer) context.getSource().getPlayer()).switchy$getPresets();
		CommandSource.suggestIdentifiers(presets.getModules().keySet().stream().filter(id -> suggestionPredicate.test(presets, id)), builder);
		return builder.buildFuture();
	}

	/**
	 * Suggests presets for the typing player.
	 *
	 * @param context      the command context to suggest with.
	 * @param builder      the suggestion builder.
	 * @param allowCurrent whether to include the player's current preset.
	 * @return the suggestion promise.
	 * @throws CommandSyntaxException when the source is not a player.
	 */
	public static CompletableFuture<Suggestions> suggestPresets(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, boolean allowCurrent) throws CommandSyntaxException {
		return suggestPresets(context, builder, allowCurrent ? (presets, preset) -> true : (presets, preset) -> !presets.getCurrentPresetName().equalsIgnoreCase(preset.getName()));
	}

	/**
	 * Creates a basic word argument called "preset" that suggests player presets.
	 *
	 * @param allowCurrent whether to include the player's current preset.
	 * @return the resultant argument builder.
	 */
	public static RequiredArgumentBuilder<ServerCommandSource, String> presetArgument(boolean allowCurrent) {
		return CommandManager.argument("preset", StringArgumentType.word()).suggests((c, b) ->SwitchyCommand.suggestPresets(c, b, allowCurrent));
	}

	/**
	 * Suggests switchy modules for the typing player.
	 *
	 * @param context the command context to suggest with.
	 * @param builder the suggestion builder.
	 * @param enabled whether to show enabled or disabled modules.
	 * @return the suggestion promise.
	 * @throws CommandSyntaxException when the source is not a player.
	 */
	public static CompletableFuture<Suggestions> suggestModules(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, Boolean enabled) throws CommandSyntaxException {
		return suggestModules(context, builder, (presets, id) -> enabled == null || presets.isModuleEnabled(id) == enabled);
	}

	/**
	 * Creates a basic Identifier argument called "module" that suggests player modules.
	 *
	 * @param enabled whether to show enabled or disabled modules.
	 * @return the resultant argument builder.
	 */
	public static RequiredArgumentBuilder<ServerCommandSource, Identifier> moduleArgument(Boolean enabled) {
		return CommandManager.argument("module", IdentifierArgumentType.identifier()).suggests((c, b) ->SwitchyCommand.suggestModules(c, b, enabled));
	}


	/**
	 * Tries to get the command source as a {@link ServerPlayerEntity}.
	 *
	 * @param source the command context to retrieve the player from.
	 * @return {@link ServerPlayerEntity} if possible, null otherwise.
	 */
	public static @Nullable ServerPlayerEntity serverPlayerOrNull(ServerCommandSource source) {
		try {
			return source.getPlayer();
		} catch (CommandSyntaxException e) {
			return null;
		}
	}

	/**
	 * Simplifies receiving serialized packets and parsing them into objects.
	 *
	 * @param buf          the received packet.
	 * @param parser       a function that parses NBT into an event object.
	 * @param eventHandler a handler for the parsed event object.
	 * @param <EventType>  the type of event.
	 */
	public static <EventType> void consumeEventPacket(PacketByteBuf buf, Function<NbtCompound, EventType> parser, Consumer<EventType> eventHandler) {
		NbtCompound eventNbt = buf.readNbt();
		if (eventNbt != null) {
			eventHandler.accept(parser.apply(eventNbt));
		}
	}

	/**
	 * Executes a given Switchy command method using context.
	 * Catches and logs errors, and unwraps the presets object from context.
	 *
	 * @param context  the command context to execute with.
	 * @param executor a function executing a command method, utilizing the provided player and presets objects.
	 * @return an integer representing the command outcome. 1 for executed, 0 for exceptions.
	 * @see folk.sisby.switchy.SwitchyCommands
	 */
	public static int execute(CommandContext<ServerCommandSource> context, SwitchyServerCommandExecutor executor) {
		ServerPlayerEntity player = serverPlayerOrNull(context.getSource());
		if (player == null) {
			LOGGER.error("[Switchy] Commands cannot be invoked by a non-player");
			return 0;
		}

		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		try {
			executor.execute(player, presets, t -> sendMessage(player, t));
			return 1;
		} catch (Exception e) {
			invalid("commands.switchy.fail");
			LOGGER.error("[Switchy] Error while executing command: {}", context.getInput(), e);
			return 0;
		}
	}

	/**
	 * A simple representation of a Switchy command method.
	 */
	public interface SwitchyServerCommandExecutor {
		/**
		 * @param player  the relevant player.
		 * @param presets the relevant player's presets.
		 * @param feedback a consumer for text feedback.
		 */
		SwitchyFeedbackStatus execute(ServerPlayerEntity player, SwitchyPresets presets, Consumer<Text> feedback);
	}
}
