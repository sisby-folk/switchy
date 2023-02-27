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
	/**
	 * @param context  the command context to execute with
	 * @param executor a function executing a command method, utilizing the provided context and player objects
	 * @return an integer representing the command outcome. 1 for executed, 0 for exceptions.
	 * @see folk.sisby.switchy.client.SwitchyClientCommands
	 */
	public static int executeClient(CommandContext<QuiltClientCommandSource> context, SwitchyClientCommandExecutor executor) {
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

	/**
	 * A simple representation of a Switchy Client command method
	 */
	public interface SwitchyClientCommandExecutor {
		/**
		 * @param context the command context
		 * @param player  the client player
		 */
		void execute(CommandContext<QuiltClientCommandSource> context, ClientPlayerEntity player);
	}
}
