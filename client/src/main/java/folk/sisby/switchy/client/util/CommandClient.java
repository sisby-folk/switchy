package folk.sisby.switchy.client.util;

import com.mojang.brigadier.context.CommandContext;
import folk.sisby.switchy.util.Feedback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;

import static folk.sisby.switchy.Switchy.LOGGER;
import static folk.sisby.switchy.client.util.FeedbackClient.sendClientMessage;

/**
 * Utilities for registering and executing client commands.
 *
 * @author Sisby folk
 * @since 1.8.13
 */
public class CommandClient {
	/**
	 * Executes a given Switchy Client command method using context.
	 * Catches and logs errors, and unwraps player from context.
	 *
	 * @param context  the command context to execute with.
	 * @param executor a function executing a command method, utilizing the provided context and player objects.
	 * @return an integer representing the command outcome. 1 for executed, 0 for exceptions.
	 * @see folk.sisby.switchy.client.SwitchyClientCommands
	 */
	public static int executeClient(CommandContext<FabricClientCommandSource> context, SwitchyClientCommandExecutor executor) {
		ClientPlayerEntity player = context.getSource().getPlayer();
		try {
			executor.execute(context.getInput(), player);
			return 1;
		} catch (Exception e) {
			sendClientMessage(player, Feedback.invalid("commands.switchy_client.fail"));
			LOGGER.error("[Switchy Client] Error while executing command: {}", context.getInput(), e);
			return 0;
		}
	}

	/**
	 * A simple representation of a Switchy Client command method.
	 */
	public interface SwitchyClientCommandExecutor {
		/**
		 * @param command the input of the command executed.
		 * @param player  the client player.
		 */
		void execute(String command, ClientPlayerEntity player);
	}
}
