package folk.sisby.switchy.client.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.network.ClientPlayerEntity;
import org.quiltmc.qsl.command.api.client.QuiltClientCommandSource;

import static folk.sisby.switchy.Switchy.LOGGER;
import static folk.sisby.switchy.client.util.FeedbackClient.tellInvalid;

/**
 * @author Sisby folk
 * @since 1.8.13
 * Utilities for registering and executing client commands
 */
public class CommandClient {
	public interface SwitchyClientCommandExecutor { void execute(CommandContext<QuiltClientCommandSource> context, ClientPlayerEntity player); }

	public static int executeClient(CommandContext<QuiltClientCommandSource> context, SwitchyClientCommandExecutor executor)
	{
		ClientPlayerEntity player = context.getSource().getPlayer();
		try {
			executor.execute(context, player);
			return 1;
		} catch (Exception e) {
			tellInvalid(player, "commands.switchy.fail");
			LOGGER.error("[Switchy] Error while executing command: {}", context.getInput(), e);
			return 0;
		}
	}
}
