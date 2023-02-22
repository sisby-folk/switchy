package folk.sisby.switchy.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.presets.SwitchyPresets;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static folk.sisby.switchy.Switchy.LOGGER;
import static folk.sisby.switchy.api.SwitchyPresetAPI.*;
import static folk.sisby.switchy.util.Feedback.tellInvalid;

public class Command {
	public interface QuadConsumer<A1, A2, A3, A4> {
		void accept(A1 a1, A2 a2, A3 a3, A4 a4);
	}

	public interface PentaConsumer<A1, A2, A3, A4, A5> {
		void accept(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5);
	}


	public static CompletableFuture<Suggestions> suggestPresets(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, boolean allowCurrent) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		CommandSource.suggestMatching(getPlayerPresetNames(player).stream().filter((s) -> allowCurrent || !Objects.equals(s, getPlayerCurrentPresetName(player))), builder);
		return builder.buildFuture();
	}

	public static CompletableFuture<Suggestions> suggestModules(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, boolean enabled) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		CommandSource.suggestIdentifiers(getPlayerModules(player).entrySet().stream().filter(e -> e.getValue() == enabled).map(Map.Entry::getKey), builder);
		return builder.buildFuture();
	}

	public static @Nullable ServerPlayerEntity serverPlayerOrNull(ServerCommandSource source) {
		try {
			return source.getPlayer();
		} catch (CommandSyntaxException e) {
			return null;
		}
	}

	public static <EventType> void consumeEventPacket(PacketByteBuf buf, Function<NbtCompound, EventType> parser, Consumer<EventType> eventHandler) {
		NbtCompound eventNbt = buf.readNbt();
		if (eventNbt != null) {
			eventHandler.accept(parser.apply(eventNbt));
		}
	}

	public static <V, V2> int unwrapAndExecute(CommandContext<ServerCommandSource> context, QuadConsumer<ServerPlayerEntity, SwitchyPresets, V, V2> executeFunction, @Nullable Pair<String, Class<V>> argument, @Nullable Pair<String, Class<V2>> argument2) {
		ServerPlayerEntity player = serverPlayerOrNull(context.getSource());
		if (player == null) {
			LOGGER.error("[Switchy] Commands cannot be invoked by a non-player");
			return 0;
		}

		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
		try {
			executeFunction.accept(
					player,
					presets,
					(argument != null ? context.getArgument(argument.getLeft(), argument.getRight()) : null),
					(argument2 != null ? context.getArgument(argument2.getLeft(), argument2.getRight()) : null)
			);
			return 1;
		} catch (Exception e) {
			tellInvalid(player, "commands.switchy.fail");
			LOGGER.error("[Switchy] Error while executing command: {}", context.getInput(), e);
			return 0;
		}
	}

	public static <V> int unwrapAndExecute(CommandContext<ServerCommandSource> context, TriConsumer<ServerPlayerEntity, SwitchyPresets, V> executeFunction, @Nullable Pair<String, Class<V>> argument) {
		return unwrapAndExecute(context, (player, preset, arg, ignored) -> executeFunction.accept(player, preset, arg), argument, null);
	}

	public static int unwrapAndExecute(CommandContext<ServerCommandSource> context, BiConsumer<ServerPlayerEntity, SwitchyPresets> executeFunction) {
		return unwrapAndExecute(context, (player, preset, ignored) -> executeFunction.accept(player, preset), null);
	}
}
