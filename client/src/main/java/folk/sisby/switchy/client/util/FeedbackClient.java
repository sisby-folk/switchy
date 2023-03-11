package folk.sisby.switchy.client.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static folk.sisby.switchy.util.Feedback.literal;

/**
 * Utilities for styling text and sending translatable feedback on the client.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class FeedbackClient {

	/**
	 * Sends feedback to the player.
	 * Feedback is branded with Switchy Client.
	 *
	 * @param player the player to send the messages to.
	 * @param text   the text to send.
	 */
	public static void sendClientMessage(ClientPlayerEntity player, Text text) {
		player.sendMessage(literal("[Switchy Client] ").setStyle(Style.EMPTY.withColor(Formatting.AQUA)).append(text), false);
	}
}
