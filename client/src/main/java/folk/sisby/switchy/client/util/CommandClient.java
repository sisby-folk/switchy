package folk.sisby.switchy.client.util;

import com.mojang.brigadier.context.CommandContext;
import folk.sisby.switchy.util.Command.PentaConsumer;
import folk.sisby.switchy.util.Command.QuadConsumer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Pair;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.command.api.client.QuiltClientCommandSource;

import static folk.sisby.switchy.Switchy.LOGGER;
import static folk.sisby.switchy.util.Feedback.tellInvalid;

public class CommandClient {

	public static <V, V2, V3> int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context,
												   PentaConsumer<CommandContext<QuiltClientCommandSource>, ClientPlayerEntity, V, V2, V3> executeFunction,
												   @Nullable Pair<String, Class<V>> argument,
												   @Nullable Pair<String, Class<V2>> argument2,
												   @Nullable Pair<String, Class<V3>> argument3
	) {
		ClientPlayerEntity player = context.getSource().getPlayer();
		try {
			executeFunction.accept(
					context,
					player,
					(argument != null ? context.getArgument(argument.getLeft(), argument.getRight()) : null),
					(argument2 != null ? context.getArgument(argument2.getLeft(), argument2.getRight()) : null),
					(argument3 != null ? context.getArgument(argument3.getLeft(), argument3.getRight()) : null)
			);
			return 1;
		} catch (Exception e) {
			tellInvalid(player, "commands.switchy.fail");
			LOGGER.error("[Switchy] Error while executing command: {}", context.getInput(), e);
			return 0;
		}
	}

	public static <V, V2> int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context, QuadConsumer<CommandContext<QuiltClientCommandSource>, ClientPlayerEntity, V, V2> executeFunction, @Nullable Pair<String, Class<V>> argument, @Nullable Pair<String, Class<V2>> argument2) {
		return unwrapAndExecute(context, (ctx, player, arg, arg2, ignored) -> executeFunction.accept(ctx, player, arg, arg2), argument, argument2, null);
	}

	public static <V> int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context, TriConsumer<CommandContext<QuiltClientCommandSource>, ClientPlayerEntity, V> executeFunction, @Nullable Pair<String, Class<V>> argument) {
		return unwrapAndExecute(context, (ctx, player, arg, ignored) -> executeFunction.accept(ctx, player, arg), argument, null);
	}

	public static int unwrapAndExecute(CommandContext<QuiltClientCommandSource> context, BiConsumer<CommandContext<QuiltClientCommandSource>, ClientPlayerEntity> executeFunction) {
		return unwrapAndExecute(context, (ctx, player, ignored) -> executeFunction.accept(ctx, player), null);
	}
}
