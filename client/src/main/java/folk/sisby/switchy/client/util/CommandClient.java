package folk.sisby.switchy.client.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.command.api.client.QuiltClientCommandSource;

import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

public class CommandClient {

	public static <V, V2, V3> int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context, Map<UUID, String> history,
												   Function5<CommandContext<QuiltClientCommandSource>, ClientPlayerEntity, V, V2, V3, Integer> executeFunction,
												   @Nullable Pair<String, Class<V>> argument,
												   @Nullable Pair<String, Class<V2>> argument2,
												   @Nullable Pair<String, Class<V3>> argument3
	) {
		// Get context and execute
		ClientPlayerEntity player = context.getSource().getPlayer();
		int result = executeFunction.apply(
				context,
				player,
				(argument != null ? context.getArgument(argument.getLeft(), argument.getRight()) : null),
				(argument2 != null ? context.getArgument(argument2.getLeft(), argument2.getRight()) : null),
				(argument3 != null ? context.getArgument(argument3.getLeft(), argument3.getRight()) : null)
		);
		// Record previous command (for confirmations)
		history.put(player.getUuid(),context.getInput());
		return result;
	}

	public static <V, V2> int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context, Map<UUID, String> history, Function4<CommandContext<QuiltClientCommandSource>, ClientPlayerEntity, V, V2, Integer> executeFunction, @Nullable Pair<String, Class<V>> argument, @Nullable Pair<String, Class<V2>> argument2) {
		return unwrapAndExecute(context, history, (ctx, player, arg, arg2, ignored) -> executeFunction.apply(ctx, player, arg, arg2), argument, argument2, null);
	}

	public static <V> int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context, Map<UUID, String> history, Function3<CommandContext<QuiltClientCommandSource>, ClientPlayerEntity, V, Integer> executeFunction, @Nullable Pair<String, Class<V>> argument) {
		return unwrapAndExecute(context, history, (ctx, player, arg, ignored) -> executeFunction.apply(ctx, player, arg), argument, null);
	}

	public static int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context, Map<UUID, String> history, BiFunction<CommandContext<QuiltClientCommandSource>, ClientPlayerEntity, Integer> executeFunction) {
		return unwrapAndExecute(context, history, (ctx, player, ignored) -> executeFunction.apply(ctx, player), null);
	}
}